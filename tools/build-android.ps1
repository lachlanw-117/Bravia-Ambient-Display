param(
    [string]$Tools = "$PSScriptRoot/../.tools/android",
    [string]$SdkRoot = '',
    [string]$JavaHome = '',
    [switch]$Unsigned
)
$ErrorActionPreference = 'Stop'
$Tools = [IO.Path]::GetFullPath($Tools)
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    if (!(Test-Path -LiteralPath "$Tools/jdk" -PathType Container)) { throw "Missing portable JDK: $Tools/jdk. Supply -JavaHome with a JDK 21 directory." }
    $jdkDirectory = Get-ChildItem -LiteralPath "$Tools/jdk" -Directory | Select-Object -First 1
    if (!$jdkDirectory) { throw "No JDK directory found in $Tools/jdk. Supply -JavaHome." }
    $JavaHome = $jdkDirectory.FullName
}
$jdk = [IO.Path]::GetFullPath($JavaHome)
if ([string]::IsNullOrWhiteSpace($SdkRoot)) { $SdkRoot = Join-Path $Tools 'sdk' }
$sdk = [IO.Path]::GetFullPath($SdkRoot)
foreach ($binary in @('java.exe','javac.exe','jar.exe')) {
    if (!(Test-Path -LiteralPath "$jdk/bin/$binary" -PathType Leaf)) { throw "Missing JDK 21 executable: $jdk/bin/$binary" }
}
$compilerVersion = & "$jdk/bin/javac.exe" -version
if ($LASTEXITCODE -or $compilerVersion -notmatch '^javac 21(?:\.|\s|$)') { throw "JDK 21 required at $jdk; found: $compilerVersion" }
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk/bin;$env:PATH"
$project = [IO.Path]::GetFullPath("$PSScriptRoot/../android")
$artifact = if ($Unsigned) { 'wright-ambient-unsigned.apk' } else { 'wright-ambient-debug.apk' }
$output = [IO.Path]::GetFullPath("$PSScriptRoot/../build/android/$artifact")
# Fresh intermediates prevent removed/renamed Java classes leaking into a later APK.
# Preserve prior runs privately for inspection; this script performs no cleanup deletion.
$build = Join-Path (Split-Path $output) ('work-' + [Guid]::NewGuid().ToString('N'))
$bt = "$sdk/build-tools/35.0.0"
$platform = "$sdk/platforms/android-35/android.jar"
if (!(Test-Path -LiteralPath $platform -PathType Leaf)) { throw "Missing Android API 35: $platform. Install it or supply -SdkRoot." }
foreach ($binary in @('aapt2.exe','d8.bat','zipalign.exe')) {
    if (!(Test-Path -LiteralPath "$bt/$binary" -PathType Leaf)) { throw "Missing Android build-tools 35.0.0 executable: $bt/$binary" }
}
Write-Output "Build prerequisites: $compilerVersion; Android API 35; build-tools 35.0.0; unsigned=$Unsigned"
New-Item -ItemType Directory -Force "$build/classes","$build/generated","$build/dex","$build/test" | Out-Null
function Check([string]$Step) { if ($LASTEXITCODE) { throw "$Step failed ($LASTEXITCODE)" } }
& "$jdk/bin/javac.exe" -d "$build/test" "$project/src/au/wright/ambient/Policy.java" "$project/src/au/wright/ambient/PhotoPresentation.java" "$project/test/PolicyTest.java" "$project/test/PhotoPresentationTest.java" "$project/src/au/wright/ambient/WeatherPresentation.java" "$project/test/WeatherPresentationTest.java" "$project/src/au/wright/ambient/NoticePresentation.java" "$project/test/NoticePresentationTest.java"
Check 'JVM test compile'
& "$jdk/bin/java.exe" -cp "$build/test" PolicyTest
Check 'JVM tests'
& "$jdk/bin/java.exe" -cp "$build/test" PhotoPresentationTest
Check 'Photo presentation tests'
& "$jdk/bin/java.exe" -cp "$build/test" WeatherPresentationTest
Check 'Weather presentation tests'
& "$jdk/bin/java.exe" -cp "$build/test" NoticePresentationTest
Check 'Notice presentation tests'
& "$bt/aapt2.exe" compile --dir "$project/res" -o "$build/resources.zip"
Check 'Resource compile'
& "$bt/aapt2.exe" link -I $platform --manifest "$project/AndroidManifest.xml" --java "$build/generated" --version-code 1 --version-name 0.1.0 -o "$build/resources.apk" "$build/resources.zip"
Check 'Resource link'
$sources = @(Get-ChildItem "$project/src","$build/generated" -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& "$jdk/bin/javac.exe" --release 8 -classpath $platform -d "$build/classes" $sources
Check 'Android Java compile'
& "$jdk/bin/jar.exe" cf "$build/classes.jar" -C "$build/classes" .
Check 'Java archive'
& "$bt/d8.bat" --lib $platform --min-api 26 --output "$build/dex" "$build/classes.jar"
Check 'DEX compile'
Copy-Item -LiteralPath "$build/resources.apk" -Destination "$build/unsigned.apk" -Force
& "$jdk/bin/jar.exe" uf "$build/unsigned.apk" -C "$build/dex" classes.dex
Check 'APK assembly'
& "$bt/zipalign.exe" -f 4 "$build/unsigned.apk" "$build/aligned.apk"
Check 'APK alignment'
& "$bt/zipalign.exe" -c 4 "$build/aligned.apk"
Check 'APK alignment verification'
if ($Unsigned) {
    Copy-Item -LiteralPath "$build/aligned.apk" -Destination $output -Force
    Write-Output "Unsigned APK ready locally (not installable until signed): $output"
    return
}
# Local debug signing material only; ignored by Git. Never used as a production identity.
$key = Join-Path $Tools 'ambient-debug.keystore'
if (!(Test-Path $key)) {
    & "$jdk/bin/keytool.exe" -genkeypair -keystore $key -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Local Development,C=AU'
    Check 'Debug key generation'
}
& "$bt/apksigner.bat" sign --ks $key --ks-pass pass:android --out $output "$build/aligned.apk"
Check 'Debug signing'
& "$bt/apksigner.bat" verify $output
Check 'APK verification'
Write-Output "APK ready locally: $output"

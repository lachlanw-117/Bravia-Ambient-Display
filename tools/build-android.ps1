param([string]$Tools = "$PSScriptRoot/../.tools/android")
$ErrorActionPreference = 'Stop'
$Tools = [IO.Path]::GetFullPath($Tools)
$jdk = (Get-ChildItem "$Tools/jdk" -Directory | Select-Object -First 1).FullName
$sdk = Join-Path $Tools 'sdk'
$env:JAVA_HOME = $jdk
$env:PATH = "$jdk/bin;$env:PATH"
$project = [IO.Path]::GetFullPath("$PSScriptRoot/../android")
$output = [IO.Path]::GetFullPath("$PSScriptRoot/../build/android/wright-ambient-debug.apk")
# Fresh intermediates prevent removed/renamed Java classes leaking into a later APK.
# Preserve prior runs privately for inspection; this script performs no cleanup deletion.
$build = Join-Path (Split-Path $output) ('work-' + [Guid]::NewGuid().ToString('N'))
$bt = "$sdk/build-tools/35.0.0"
$platform = "$sdk/platforms/android-35/android.jar"
if (!(Test-Path $platform)) { throw 'Install Android API 35 using bootstrap-windows.ps1 first' }
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

param([string]$Tools = "$PSScriptRoot/../.tools/android")
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$Tools = [IO.Path]::GetFullPath($Tools)
New-Item -ItemType Directory -Force -Path $Tools | Out-Null
# Portable tools only. No PATH or registry changes, device connections or installs.
$asset = (Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse')[0].binary.package
$jdkZip = Join-Path $Tools 'jdk.zip'
if (!(Test-Path "$Tools/jdk")) {
    Invoke-WebRequest $asset.link -OutFile $jdkZip -UseBasicParsing
    if ((Get-FileHash $jdkZip -Algorithm SHA256).Hash -ne $asset.checksum) { throw 'JDK checksum mismatch' }
    Expand-Archive $jdkZip -DestinationPath "$Tools/jdk" -Force
}
$jdk = (Get-ChildItem "$Tools/jdk" -Directory | Select-Object -First 1).FullName
$env:JAVA_HOME = $jdk
$sdk = Join-Path $Tools 'sdk'
if (!(Test-Path "$sdk/cmdline-tools/latest/bin/sdkmanager.bat")) {
    $sdkZip = Join-Path $Tools 'cmdline-tools.zip'
    Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip' -OutFile $sdkZip -UseBasicParsing
    if ((Get-FileHash $sdkZip -Algorithm SHA256).Hash -ne '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a') { throw 'SDK checksum mismatch' }
    Expand-Archive $sdkZip -DestinationPath "$Tools/unpacked" -Force
    New-Item -ItemType Directory -Force "$sdk/cmdline-tools" | Out-Null
    Move-Item -LiteralPath "$Tools/unpacked/cmdline-tools" -Destination "$sdk/cmdline-tools/latest"
}
# sdkmanager prompts for the Android SDK licence; review it in the terminal.
& "$sdk/cmdline-tools/latest/bin/sdkmanager.bat" "--sdk_root=$sdk" 'platforms;android-35' 'build-tools;35.0.0'
if ($LASTEXITCODE) { throw 'SDK installation failed or licence not accepted' }
if (!(Test-Path "$sdk/platforms/android-35/android.jar")) { throw 'SDK licence not accepted or platform installation incomplete' }
Write-Output "Portable JDK: $jdk"
Write-Output "Android SDK: $sdk"

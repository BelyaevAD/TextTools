# Install TextTools into %LOCALAPPDATA%\Scripts\TextTools and start it.
# Run from this directory:
#   powershell -ExecutionPolicy Bypass -File .\install.ps1

$ErrorActionPreference = 'Stop'

$srcDir = $PSScriptRoot
$src = Join-Path $srcDir 'TextTools.ahk'
$dstDir = Join-Path $env:LOCALAPPDATA 'Scripts\TextTools'
$dst = Join-Path $dstDir 'TextTools.ahk'
$ahk = Join-Path $env:LOCALAPPDATA 'Programs\AutoHotkey\v2\AutoHotkey64.exe'

if (-not (Test-Path $ahk)) { $ahk = 'C:\Program Files\AutoHotkey\v2\AutoHotkey64.exe' }
if (-not (Test-Path $ahk)) {
    throw "AutoHotkey v2 not found. Install: winget install -e --id AutoHotkey.AutoHotkey"
}
if (-not (Test-Path $src)) { throw "Source not found: $src" }
if (-not (Test-Path (Join-Path $srcDir 'Lib'))) { throw "Lib folder missing next to TextTools.ahk" }

$valLog = Join-Path $env:TEMP 'texttools-validate.log'
$p = Start-Process -FilePath $ahk -ArgumentList '/ErrorStdOut', '/validate', "`"$src`"" `
    -Wait -PassThru -NoNewWindow -RedirectStandardError $valLog
if ($p.ExitCode -ne 0) {
    Get-Content $valLog -ErrorAction SilentlyContinue | Write-Host
    throw "Validation failed (exit $($p.ExitCode)) — install aborted"
}

Get-CimInstance Win32_Process -Filter "Name = 'AutoHotkey64.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like '*TextTools.ahk*' } |
    ForEach-Object {
        Write-Host "Stopping previous instance (PID $($_.ProcessId))"
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
Start-Sleep -Milliseconds 400

if (-not (Test-Path $dstDir)) { New-Item -ItemType Directory -Path $dstDir | Out-Null }
$dstLib = Join-Path $dstDir 'Lib'
if (Test-Path $dstLib) { Remove-Item $dstLib -Recurse -Force }
Copy-Item $src $dst -Force
Copy-Item (Join-Path $srcDir 'Lib') $dstDir -Recurse -Force

$dstLang = Join-Path $dstDir 'lang'
if (-not (Test-Path $dstLang)) { New-Item -ItemType Directory -Path $dstLang | Out-Null }
Get-ChildItem $dstLang -File -Filter '*.ini' -ErrorAction SilentlyContinue | Remove-Item -Force
$srcLang = Join-Path $srcDir 'lang'
if (Test-Path $srcLang) {
    Get-ChildItem $srcLang -File -Filter '*.ini' | Copy-Item -Destination $dstLang -Force
}
$userLang = Join-Path $dstLang 'user'
if (-not (Test-Path $userLang)) { New-Item -ItemType Directory -Path $userLang | Out-Null }
Write-Host "Installed: $dst (+ Lib, lang; lang\user kept)"

$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
$runVal = "`"$ahk`" `"$dst`""
Set-ItemProperty -Path $runKey -Name TextTools -Value $runVal
Write-Host "Autostart registered (HKCU Run\TextTools)"

Start-Process -FilePath $ahk -ArgumentList "`"$dst`"" -WorkingDirectory $dstDir
Start-Sleep -Seconds 2

$alive = Get-CimInstance Win32_Process -Filter "Name = 'AutoHotkey64.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like '*TextTools.ahk*' }
if ($alive) {
    Write-Host "TextTools started (PID $($alive.ProcessId -join ', '))"
} else {
    throw "Process did not start — see $dstDir\TextTools.log"
}

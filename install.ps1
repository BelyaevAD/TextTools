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
    -Wait -PassThru -NoNewWindow -RedirectStandardOutput $valLog
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
foreach ($sub in @('Lib', 'lang')) {
    $dstSub = Join-Path $dstDir $sub
    if (Test-Path $dstSub) { Remove-Item $dstSub -Recurse -Force }
}
Copy-Item $src $dst -Force
Copy-Item (Join-Path $srcDir 'Lib') $dstDir -Recurse -Force
if (Test-Path (Join-Path $srcDir 'lang')) {
    Copy-Item (Join-Path $srcDir 'lang') $dstDir -Recurse -Force
}
Write-Host "Installed: $dst (+ Lib, lang)"

$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
$runVal = "`"$ahk`" `"$dst`""
Set-ItemProperty -Path $runKey -Name TextTools -Value $runVal
Write-Host "Autostart registered (HKCU Run\TextTools)"

Start-Process -FilePath $ahk -ArgumentList "`"$dst`"" -WorkingDirectory $dstDir
Start-Sleep -Seconds 2

$alive = Get-Process -Name 'AutoHotkey64' -ErrorAction SilentlyContinue
if ($alive) {
    Write-Host "TextTools started (PID $($alive.Id -join ', '))"
} else {
    throw "Process did not start — see $dstDir\TextTools.log"
}

# Local TextTools test runner. Do not redirect AHK stdout/stderr — GUI hangs on pipes.
# Usage (repo root):
#   powershell -ExecutionPolicy Bypass -File .\run-tests.ps1
#   powershell -ExecutionPolicy Bypass -File .\run-tests.ps1 -Gui

param([switch]$Gui)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$ahk = Join-Path $env:LOCALAPPDATA 'Programs\AutoHotkey\v2\AutoHotkey64.exe'
if (-not (Test-Path $ahk)) { $ahk = 'C:\Program Files\AutoHotkey\v2\AutoHotkey64.exe' }
if (-not (Test-Path $ahk)) { throw "AutoHotkey v2 not found" }

function Stop-AhkMatching([string]$needle) {
    Get-CimInstance Win32_Process -Filter "Name = 'AutoHotkey64.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like $needle } |
        ForEach-Object {
            Write-Host "Stopping leftover AHK PID $($_.ProcessId)"
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
}

function Invoke-AhkValidate([string]$file) {
    $errLog = Join-Path $env:TEMP ("tt_val_" + [IO.Path]::GetFileNameWithoutExtension($file) + ".log")
    if (Test-Path $errLog) { Remove-Item $errLog -Force }
    $p = Start-Process -FilePath $ahk -ArgumentList '/ErrorStdOut', '/validate', $file `
        -Wait -PassThru -NoNewWindow -WorkingDirectory $root -RedirectStandardError $errLog
    if (Test-Path $errLog) { Get-Content $errLog -ErrorAction SilentlyContinue | Write-Host }
    if ($p.ExitCode -ne 0) { throw "validate failed: $file exit $($p.ExitCode)" }
    Write-Host "OK validate $file"
}

function Invoke-AhkScript([string]$file, [string]$report, [int]$timeoutSec) {
    Stop-AhkMatching "*$file*"
    if (Test-Path $report) { Remove-Item $report -Force }
    $p = Start-Process -FilePath $ahk -ArgumentList $file -PassThru -WorkingDirectory $root
    $ok = $p.WaitForExit($timeoutSec * 1000)
    if (-not $ok) {
        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
        if (Test-Path $report) { Write-Host "---- partial report ----"; Get-Content $report | Write-Host }
        throw "TIMEOUT ${timeoutSec}s: $file"
    }
    if (Test-Path $report) { Get-Content $report | Write-Host }
    if ($p.ExitCode -ne 0) { throw "$file failed exit $($p.ExitCode)" }
    Write-Host "OK $file"
}

Write-Host "Static checks"
node (Join-Path $root 'tools\check-shadowing.mjs')
node (Join-Path $root 'tools\check-cyrillic.mjs')

Write-Host "Validate"
foreach ($f in @('TextTools.ahk', 'tests-logic.ahk', 'tests-editor-open.ahk', 'tests-usage-and-source.ahk', 'capture-screenshots.ahk')) {
    Invoke-AhkValidate $f
}

Write-Host "Logic tests (CI gate)"
Invoke-AhkScript 'tests-logic.ahk' (Join-Path $env:TEMP 'tt_logic_report.txt') 60

if ($Gui) {
    Write-Host "GUI tests"
    Invoke-AhkScript 'tests-editor-open.ahk' (Join-Path $env:TEMP 'tt_eo_report.txt') 90
    Invoke-AhkScript 'tests-usage-and-source.ahk' (Join-Path $env:TEMP 'tt_us_report.txt') 120
}

Write-Host "ALL CHECKS PASSED"

# Remove the installed copy and autostart. Does not uninstall AutoHotkey.
#   powershell -ExecutionPolicy Bypass -File .\uninstall.ps1

$ErrorActionPreference = 'Stop'
$dstDir = Join-Path $env:LOCALAPPDATA 'Scripts\TextTools'

Get-CimInstance Win32_Process -Filter "Name = 'AutoHotkey64.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like '*TextTools.ahk*' } |
    ForEach-Object {
        Write-Host "Stopping TextTools (PID $($_.ProcessId))"
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
Start-Sleep -Milliseconds 400

$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
if (Get-ItemProperty -Path $runKey -Name TextTools -ErrorAction SilentlyContinue) {
    Remove-ItemProperty -Path $runKey -Name TextTools
    Write-Host "Autostart removed"
}

if (Test-Path $dstDir) {
    Remove-Item $dstDir -Recurse -Force
    Write-Host "Removed $dstDir"
} else {
    Write-Host "Nothing installed at $dstDir"
}

#Requires -Version 5.1
param(
    [Parameter(Mandatory = $true)][Int64]$Hwnd,
    [Parameter(Mandatory = $true)][string]$Path
)

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class TtShot {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hwnd, IntPtr hdcBlt, uint nFlags);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
  public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
}
"@

$dir = Split-Path -Parent $Path
if ($dir -and -not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$h = [IntPtr]$Hwnd
[void][TtShot]::ShowWindow($h, 9) # SW_RESTORE
[void][TtShot]::SetForegroundWindow($h)
Start-Sleep -Milliseconds 180

$rect = New-Object TtShot+RECT
if (-not [TtShot]::GetWindowRect($h, [ref]$rect)) {
    throw "GetWindowRect failed for hwnd=$Hwnd"
}
$w = [Math]::Max(1, $rect.Right - $rect.Left)
$hgt = [Math]::Max(1, $rect.Bottom - $rect.Top)

$bmp = New-Object System.Drawing.Bitmap $w, $hgt
$g = [System.Drawing.Graphics]::FromImage($bmp)
try {
    $hdc = $g.GetHdc()
    try {
        # PW_RENDERFULLCONTENT = 2
        $ok = [TtShot]::PrintWindow($h, $hdc, 2)
        if (-not $ok) {
            $ok = [TtShot]::PrintWindow($h, $hdc, 0)
        }
        if (-not $ok) {
            throw "PrintWindow failed for hwnd=$Hwnd"
        }
    }
    finally {
        $g.ReleaseHdc($hdc)
    }
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}
finally {
    $g.Dispose()
    $bmp.Dispose()
}

Write-Output $Path

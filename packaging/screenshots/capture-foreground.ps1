# Save the TextTools window, then blur its outer 5 pixels. Start this before opening a surface that closes when focus leaves.
param(
    [Parameter(Mandatory = $true)][string]$Out,
    [string]$Match = "",
    [switch]$Popup,
    [string]$Gate = "",
    [int]$Wait = 25
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public static class Shot {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }
    public delegate bool EnumProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc lp, IntPtr l);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassName(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int attr, out RECT r, int size);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
    public static readonly List<IntPtr> Hwnds = new List<IntPtr>();
    static bool Collect(IntPtr h, IntPtr l) {
        if (IsWindowVisible(h)) Hwnds.Add(h);
        return true;
    }
    public static void CollectVisible() {
        Hwnds.Clear();
        EnumWindows(Collect, IntPtr.Zero);
    }
    public static string Text(IntPtr h) {
        var sb = new StringBuilder(512);
        GetWindowText(h, sb, sb.Capacity);
        return sb.ToString();
    }
    public static string ClassName(IntPtr h) {
        var sb = new StringBuilder(256);
        GetClassName(h, sb, sb.Capacity);
        return sb.ToString();
    }
    public static uint PidOf(IntPtr h) {
        uint pid;
        GetWindowThreadProcessId(h, out pid);
        return pid;
    }
    public static RECT BoundsOf(IntPtr h) {
        RECT r;
        int hr = DwmGetWindowAttribute(h, 9, out r, Marshal.SizeOf(typeof(RECT)));
        if (hr != 0 || r.Right <= r.Left || r.Bottom <= r.Top) {
            if (!GetWindowRect(h, out r) || r.Right <= r.Left || r.Bottom <= r.Top) {
                throw new InvalidOperationException("no window bounds");
            }
        }
        return r;
    }
}
"@
[Shot]::SetProcessDPIAware() | Out-Null

function Get-TextToolsPids {
    $ids = New-Object System.Collections.Generic.List[int]
    Get-Process -Name TextTools -ErrorAction SilentlyContinue | ForEach-Object { $ids.Add($_.Id) }
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | ForEach-Object {
        if ($_.CommandLine -match 'texttools') { $ids.Add([int]$_.ProcessId) }
    }
    return $ids
}

function Find-Target($pids) {
    if ($pids.Count -eq 0) { return $null }
    [Shot]::CollectVisible()
    foreach ($h in [Shot]::Hwnds) {
        $wpid = [int][Shot]::PidOf($h)
        if (-not $pids.Contains($wpid)) { continue }
        $title = [Shot]::Text($h)
        $cls = [Shot]::ClassName($h)
        if ($title -and -not $script:seen.Contains($title)) { [void]$script:seen.Add($title) }
        if ($Popup) {
            if ($cls -eq '#32768') { return @{ Hwnd = $h; Title = $title } }
            continue
        }
        if (-not $Match) {
            if ($title.StartsWith('TextTools')) { return @{ Hwnd = $h; Title = $title } }
            continue
        }
        if ($title -eq $Match) { return @{ Hwnd = $h; Title = $title } }
    }
    return $null
}

$seen = New-Object System.Collections.Generic.List[string]
$deadline = (Get-Date).AddSeconds($Wait)
$nextPid = [datetime]::MinValue
$pids = New-Object System.Collections.Generic.List[int]
$hit = $null
while ((Get-Date) -lt $deadline) {
    if ((Get-Date) -ge $nextPid) {
        $pids = Get-TextToolsPids
        $nextPid = (Get-Date).AddSeconds(1)
    }
    if ($Gate -and -not (Test-Path -LiteralPath $Gate)) {
        Start-Sleep -Milliseconds 100
        continue
    }
    $hit = Find-Target $pids
    if ($null -ne $hit) { break }
    Start-Sleep -Milliseconds 100
}
if ($null -eq $hit) {
    [Console]::Error.WriteLine("no TextTools window within ${Wait}s. seen: $($seen -join ' | ')")
    exit 3
}

$rect = [Shot]::BoundsOf($hit.Hwnd)
$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path -LiteralPath $dir)) {
    New-Item -ItemType Directory -Path $dir | Out-Null
}
$bmp = New-Object System.Drawing.Bitmap $width, $height
$graphics = [System.Drawing.Graphics]::FromImage($bmp)
$graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, (New-Object System.Drawing.Size($width, $height)))
$graphics.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

$java = "java"
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path -LiteralPath $candidate) { $java = $candidate }
}
& $java (Join-Path $PSScriptRoot "BlurBorder.java") $Out
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "captured: $Out title: $($hit.Title)"

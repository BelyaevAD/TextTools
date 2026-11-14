# WiX light links one culture per invocation. jpackage passes every culture in
# one -cultures list, and light then stops with LGHT0100. Build the base MSI,
# build the other cultures, and embed them as language transforms.
$ErrorActionPreference = 'Stop'
$shimDir = (Resolve-Path (Split-Path -Parent $MyInvocation.MyCommand.Path)).Path
$argFile = Join-Path $shimDir 'light-args.txt'
$lightArgs = @($args)
if (Test-Path -LiteralPath $argFile) {
    $lightArgs = @(Get-Content -LiteralPath $argFile -Encoding UTF8)
    Remove-Item -LiteralPath $argFile -Force
}
$beside = Join-Path $shimDir 'light-wix.exe'
$realLight = $null
if (Test-Path -LiteralPath $beside) { $realLight = $beside }
if (-not $realLight) {
    foreach ($dir in ($env:PATH -split ';')) {
    if ([string]::IsNullOrWhiteSpace($dir)) { continue }
    $dirPath = $dir.Trim().Trim('"')
    try { $dirPath = (Resolve-Path -LiteralPath $dirPath).Path } catch { continue }
    if ($dirPath -eq $shimDir) { continue }
    $candidate = Join-Path $dirPath 'light.exe'
    if (Test-Path -LiteralPath $candidate) { $realLight = $candidate; break }
    }
}
if (-not $realLight) { Write-Error 'WiX light.exe is not on PATH'; exit 1 }
$torch = Join-Path (Split-Path -Parent $realLight) 'torch.exe'

$cultureArg = @($lightArgs | Where-Object { $_ -like '-cultures:*' } | Select-Object -First 1)
if ($cultureArg.Count -eq 0 -or $cultureArg[0] -notlike '*;*') {
    & $realLight @lightArgs
    exit $LASTEXITCODE
}

$loc = New-Object System.Collections.Generic.List[string]
$rest = New-Object System.Collections.Generic.List[string]
$out = $null
for ($i = 0; $i -lt $lightArgs.Count; $i++) {
    $arg = [string]$lightArgs[$i]
    if ($arg -eq '-loc') { $i++; [void]$loc.Add([string]$lightArgs[$i]); continue }
    if ($arg -eq '-out' -or $arg -eq '-o') { $i++; $out = [string]$lightArgs[$i]; continue }
    if ($arg -like '-cultures:*') { continue }
    [void]$rest.Add($arg)
}
if (-not $out) { Write-Error 'light -out is missing'; exit 1 }

$cultures = $cultureArg[0].Substring(10).Split(';') | Where-Object { $_ }
$base = if ($cultures -contains 'en-us') { 'en-us' } else { $cultures[0] }
$ordered = @($base) + @($cultures | Where-Object { $_ -ne $base })
$lcidOf = @{ 'en-us' = 1033; 'de-de' = 1031; 'ja-jp' = 1041; 'zh-cn' = 2052; 'ru-ru' = 1049 }
foreach ($culture in $ordered) {
    if (-not $lcidOf.ContainsKey($culture)) { Write-Error "No LCID for $culture"; exit 1 }
}

$byCulture = @{}
foreach ($file in $loc) {
    $text = [IO.File]::ReadAllText($file)
    $culture = ''
    if ($text -match 'Culture="([^"]+)"') { $culture = $Matches[1].ToLowerInvariant() }
    if (-not $byCulture.ContainsKey($culture)) { $byCulture[$culture] = @() }
    $byCulture[$culture] += $file
}

$work = Join-Path ([IO.Path]::GetTempPath()) ("tt-msi-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $work | Out-Null
try {
$built = @{}
foreach ($culture in $ordered) {
    $msi = if ($culture -eq $base) { $out } else { Join-Path $work "$culture.msi" }
    $one = New-Object System.Collections.Generic.List[string]
    $one.AddRange($rest)
    $files = @()
    if ($byCulture.ContainsKey($culture)) { $files += $byCulture[$culture] }
    if ($byCulture.ContainsKey('')) { $files += $byCulture[''] }
    foreach ($file in $files) { [void]$one.Add('-loc'); [void]$one.Add($file) }
    [void]$one.Add("-cultures:$culture")
    [void]$one.Add('-out')
    [void]$one.Add($msi)
    & $realLight @one
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $built[$culture] = $msi
}

$transforms = @()
foreach ($culture in $ordered) {
    if ($culture -eq $base) { continue }
    $lcid = $lcidOf[$culture]
    $mst = Join-Path $work "$lcid.mst"
    & $torch -nologo -p -t language $built[$base] $built[$culture] -out $mst
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $transforms += [pscustomobject]@{ Lcid = $lcid; Path = (Resolve-Path -LiteralPath $mst).Path }
}

$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.OpenDatabase($built[$base], 1)
$view = $db.OpenView('SELECT `Name`,`Data` FROM `_Storages`')
$view.Execute()
foreach ($transform in $transforms) {
    $record = $installer.CreateRecord(2)
    $record.StringData(1) = "$($transform.Lcid)"
    $record.SetStream(2, $transform.Path)
    $view.Modify(1, $record)
}
$view.Close()
$summary = $db.SummaryInformation(1)
$template = [string]$summary.Property(7)
$platform = ($template -split ';')[0]
$ids = @($ordered | ForEach-Object { $lcidOf[$_] })
$summary.Property(7) = "$platform;" + ($ids -join ',')
$summary.Persist()
$db.Commit()
$exitCode = 0
} finally {
    # Runs on every exit path (exit inside try included); release COM handles first so nothing locks the folder.
    $view = $null; $summary = $null; $db = $null; $installer = $null
    [GC]::Collect(); [GC]::WaitForPendingFinalizers()
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
exit $exitCode

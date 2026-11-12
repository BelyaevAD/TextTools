# Append a catalog row when the app gains a window, an editor page, or a settings tab. Existing rows stay.
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$catalog = Join-Path $repo "docs\screenshots\catalog.yaml"
$kotlin = Join-Path $repo "app\src\main\kotlin"
$settingsXml = Join-Path $repo "app\src\main\composeResources\values\settings.xml"
$editor = Join-Path $kotlin "dev\belyaevad\texttools\app\ui\editor"

$known = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($m in [regex]::Matches([System.IO.File]::ReadAllText($catalog), '(?m)^    source: (\S+)\s*$')) {
    [void]$known.Add($m.Groups[1].Value)
}

function To-Id([string]$name) {
    return ([regex]::Replace($name, '([a-z0-9])([A-Z])', '$1-$2')).ToLower()
}

function Add-Surface([string]$source, [string]$id, [string]$match, [string]$open) {
    if ($known.Contains($source)) { return }
    $file = "docs/assets/screenshots/en/$id.png"
    $nl = "`r`n"
    $block = @(
        "",
        "  - id: $id",
        "    file: $file",
        "    source: $source",
        "    match: `"$match`"",
        "    readme: false",
        "    gate: true",
        "    open: `"$open`""
    ) -join $nl
    [System.IO.File]::AppendAllText($catalog, $block + $nl)
    [void]$known.Add($source)
    Write-Output "added $id"
}

$editorHasPages = $false
Get-ChildItem -LiteralPath $editor -Filter *.kt | ForEach-Object {
    $pageFile = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($m in [regex]::Matches($pageFile, '(?m)^(?:private |internal |public )?fun (\w+Page)\(')) {
        if ($m.Groups[1].Value -ne 'editorPage') { $script:editorHasPages = $true }
    }
}

$skipWindow = @('SettingsWindow', 'LastResultWindow')
Get-ChildItem -LiteralPath $kotlin -Recurse -Filter *.kt | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    $found = [regex]::Matches($content, '(?m)^(?:private |internal |public )?fun (\w+Window)\(')
    $bodies = @{}
    foreach ($m in $found) {
        $name = $m.Groups[1].Value
        $rest = $content.Substring($m.Index)
        $nextFun = [regex]::Match($rest.Substring(1), '(?m)^(?:private |internal |public )?fun ')
        $slice = if ($nextFun.Success) { $rest.Substring(0, $nextFun.Index + 1) } else { $rest }
        $nl = $slice.IndexOf("`n")
        $bodies[$name] = if ($nl -ge 0) { $slice.Substring($nl + 1) } else { "" }
    }
    foreach ($name in @($bodies.Keys)) {
        if ($skipWindow -contains $name) { continue }
        if ($editorHasPages -and $name -eq 'editorWindow') { continue }
        $calledBySibling = $false
        foreach ($other in @($bodies.Keys)) {
            if ($other -eq $name) { continue }
            if ($bodies[$other] -match "(^|[^A-Za-z0-9_])$([regex]::Escape($name))\(") { $calledBySibling = $true }
        }
        if ($calledBySibling) { continue }
        if ($bodies[$name] -notmatch '(^|[^A-Za-z0-9_])\w*Window\(') { continue }
        Add-Surface $name (To-Id $name) "" "Open $name from the tray (History, Statistics, Action editor, Last result, or Settings). Do not click Clear, Exit, or Restart, and do not type a key, a proxy password, or a DSN. After a capture, set match to the exact title the script prints."
    }
}

$xml = [System.IO.File]::ReadAllText($settingsXml)
foreach ($m in [regex]::Matches($xml, 'name="(settings_tab_[^"]+)">([^<]+)')) {
    $source = $m.Groups[1].Value
    $label = $m.Groups[2].Value.Trim()
    Add-Surface $source ("settings-" + ($source -replace '^settings_tab_', '')) "TextTools - settings" "Tray, Settings, tab $label. Leave the key, proxy password, and DSN empty."
}

Get-ChildItem -LiteralPath $editor -Filter *.kt | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    foreach ($m in [regex]::Matches($content, '(?m)^(?:private |internal |public )?fun (\w+Page)\(')) {
        $name = $m.Groups[1].Value
        if ($name -eq 'editorPage') { continue }
        Add-Surface $name (To-Id $name) "TextTools - action editor" "Tray, Action editor, then the page drawn by $name. Do not type a key, a proxy password, or a DSN."
    }
}

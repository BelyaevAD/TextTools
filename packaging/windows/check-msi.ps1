# Fails (exit 1) when the MSI lacks the JpKeepUser* custom actions from main.wxs,
# which means jpackage used its stock template and an upgrade would lose user data.
param([Parameter(Mandatory = $true)][string]$Msi)
$ErrorActionPreference = 'Stop'
$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.OpenDatabase((Resolve-Path -LiteralPath $Msi).Path, 0)
$view = $db.OpenView('SELECT `Action` FROM `CustomAction`')
$view.Execute()
$found = @()
while ($record = $view.Fetch()) {
    $name = $record.StringData(1)
    if ($name -like 'JpKeepUser*') { $found += $name }
}
$view.Close()
if ($found.Count -eq 0) {
    [Console]::Error.WriteLine("MSI $Msi has no JpKeepUser* custom actions: built from the stock jpackage template.")
    exit 1
}
Write-Host "check-msi: $($found.Count) JpKeepUser* actions in $Msi"
exit 0

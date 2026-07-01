param(
    [string]$AppHome = '',
    [string]$TaskName = 'TechWatch Weekly Collection',
    [string]$At = '08:00'
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($AppHome)) {
    $AppHome = if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'runtime')) { $PSScriptRoot } else { Split-Path $PSScriptRoot -Parent }
}
$appHome = (Resolve-Path -LiteralPath $AppHome).Path
$cli = Join-Path $appHome 'techwatch-cli.exe'
if (-not (Test-Path -LiteralPath $cli)) { throw "CLI launcher not found: $cli" }
$action = New-ScheduledTaskAction -Execute $cli -Argument 'run-weekly' -WorkingDirectory $appHome
$trigger = New-ScheduledTaskTrigger -Weekly -WeeksInterval 1 -DaysOfWeek Monday -At $At
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 2)
Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Description 'Collect the previous completed week and update the report.' -Force | Out-Null
Write-Host "Registered scheduled task: $TaskName (Monday $At)"

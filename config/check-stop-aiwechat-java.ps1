param(
    [switch]$Stop,
    [switch]$IncludeMavenLauncher
)

$ErrorActionPreference = "Stop"

function Get-JavaProcessEntries {
    $jpsOutput = & jps -lv 2>$null
    if (-not $jpsOutput) {
        throw "jps is not available or returned no Java processes."
    }

    $entries = @()
    foreach ($line in $jpsOutput) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $parts = $line -split '\s+', 2
        if ($parts.Count -lt 1) {
            continue
        }

        $processId = 0
        if (-not [int]::TryParse($parts[0], [ref]$processId)) {
            continue
        }

        $commandLine = if ($parts.Count -gt 1) { $parts[1] } else { "" }

        $entries += [PSCustomObject]@{
            ProcessId   = $processId
            CommandLine = $commandLine
        }
    }

    return $entries
}

function Test-AiwechatProcess {
    param(
        [string]$CommandLine
    )

    if ([string]::IsNullOrWhiteSpace($CommandLine)) {
        return $false
    }

    if ($CommandLine -match 'nacos-server\.jar') {
        return $false
    }

    return $CommandLine -match 'com\.aiwechat\.auth\.AuthServiceApplication' `
        -or $CommandLine -match 'com\.aiwechat\.gateway\.ApiGatewayApplication' `
        -or $CommandLine -match 'com\.aiwechat\.AiwechatApplication' `
        -or $CommandLine -match 'D:\\vis\\aiwechat\\aiwechat'
}

function Test-AiwechatMavenLauncher {
    param(
        [string]$CommandLine
    )

    if ([string]::IsNullOrWhiteSpace($CommandLine)) {
        return $false
    }

    return $CommandLine -match 'org\.codehaus\.plexus\.classworlds\.launcher\.Launcher' `
        -and $CommandLine -match 'D:\\vis\\aiwechat\\aiwechat'
}

$javaEntries = Get-JavaProcessEntries
$targets = @()

foreach ($entry in $javaEntries) {
    if (Test-AiwechatProcess -CommandLine $entry.CommandLine) {
        $targets += [PSCustomObject]@{
            ProcessId   = $entry.ProcessId
            Kind        = "service"
            CommandLine = $entry.CommandLine
        }
        continue
    }

    if ($IncludeMavenLauncher -and (Test-AiwechatMavenLauncher -CommandLine $entry.CommandLine)) {
        $targets += [PSCustomObject]@{
            ProcessId   = $entry.ProcessId
            Kind        = "maven-launcher"
            CommandLine = $entry.CommandLine
        }
    }
}

if ($targets.Count -eq 0) {
    Write-Host "No aiwechat Java residual processes found."
    exit 0
}

Write-Host "Detected aiwechat Java processes:"
$targets |
    Sort-Object Kind, ProcessId |
    Format-Table -AutoSize ProcessId, Kind, CommandLine

if (-not $Stop) {
    Write-Host ""
    Write-Host "Run with -Stop to terminate these processes."
    if (-not $IncludeMavenLauncher) {
        Write-Host "Add -IncludeMavenLauncher to also target aiwechat Maven launcher processes."
    }
    exit 0
}

foreach ($target in $targets | Sort-Object Kind, ProcessId -Descending) {
    try {
        Stop-Process -Id $target.ProcessId -Force -ErrorAction Stop
        Write-Host ("Stopped PID {0} ({1})" -f $target.ProcessId, $target.Kind)
    } catch {
        Write-Warning ("Failed to stop PID {0}: {1}" -f $target.ProcessId, $_.Exception.Message)
    }
}

Write-Host "aiwechat residual Java process cleanup completed."

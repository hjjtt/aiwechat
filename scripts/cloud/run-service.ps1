param(
    [Parameter(Mandatory = $true)]
    [string]$Module,
    [string]$EnvFile = ".env",
    [string]$Profile = "dev",
    [string]$MvnCommand = "mvn"
)

$ErrorActionPreference = "Stop"

function Read-DotEnv($Path) {
    $map = @{}
    foreach ($rawLine in Get-Content $Path) {
        $line = $rawLine.Trim()
        if (-not $line -or $line.StartsWith("#")) { continue }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $map[$key] = $value
    }
    return $map
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$backendDir = Join-Path $repoRoot "backend"

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}

if (-not (Test-Path $EnvFile)) {
    throw ".env file not found: $EnvFile"
}

$vars = Read-DotEnv $EnvFile
foreach ($entry in $vars.GetEnumerator()) {
    Set-Item -Path ("Env:" + $entry.Key) -Value ([string]$entry.Value)
}

$env:SPRING_PROFILES_ACTIVE = $Profile
$env:NACOS_SERVER_ADDR = if ($env:NACOS_SERVER_ADDR) { $env:NACOS_SERVER_ADDR } else { "127.0.0.1:8848" }
$env:NACOS_GROUP = if ($env:NACOS_GROUP) { $env:NACOS_GROUP } else { "AIWECHAT_GROUP" }

Set-Location $backendDir
Write-Host "Starting module '$Module' with profile '$Profile'..."
& $MvnCommand -pl $Module spring-boot:run

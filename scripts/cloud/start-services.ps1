param(
    [string]$EnvFile = ".env",
    [string]$Profile = "dev",
    [string]$MvnCommand = "mvn",
    [switch]$SkipPublish
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
$publishScript = Join-Path $repoRoot "scripts\nacos\publish-config.ps1"
$runScript = Join-Path $scriptDir "run-service.ps1"

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}

if (-not (Test-Path $EnvFile)) {
    throw ".env file not found: $EnvFile"
}

if (-not (Test-Path $publishScript)) {
    throw "publish-config.ps1 not found: $publishScript"
}

if (-not (Test-Path $runScript)) {
    throw "run-service.ps1 not found: $runScript"
}

$vars = Read-DotEnv $EnvFile
$nacosServerAddr = if ($vars.ContainsKey("NACOS_SERVER_ADDR") -and -not [string]::IsNullOrWhiteSpace($vars["NACOS_SERVER_ADDR"])) { [string]$vars["NACOS_SERVER_ADDR"] } else { "127.0.0.1:8848" }
$nacosServer = if ($nacosServerAddr.StartsWith("http://") -or $nacosServerAddr.StartsWith("https://")) { $nacosServerAddr } else { "http://$nacosServerAddr" }
$nacosGroup = if ($vars.ContainsKey("NACOS_GROUP") -and -not [string]::IsNullOrWhiteSpace($vars["NACOS_GROUP"])) { [string]$vars["NACOS_GROUP"] } else { "AIWECHAT_GROUP" }
$nacosNamespace = if ($vars.ContainsKey("NACOS_NAMESPACE")) { [string]$vars["NACOS_NAMESPACE"] } else { "" }
$nacosUsername = if ($vars.ContainsKey("NACOS_USERNAME") -and -not [string]::IsNullOrWhiteSpace($vars["NACOS_USERNAME"])) { [string]$vars["NACOS_USERNAME"] } else { "nacos" }
$nacosPassword = if ($vars.ContainsKey("NACOS_PASSWORD") -and -not [string]::IsNullOrWhiteSpace($vars["NACOS_PASSWORD"])) { [string]$vars["NACOS_PASSWORD"] } else { "nacos" }

try {
    $health = Invoke-WebRequest -Uri "$nacosServer/nacos" -UseBasicParsing -TimeoutSec 5
    if ($health.StatusCode -lt 200 -or $health.StatusCode -ge 500) {
        throw "Nacos health check returned status $($health.StatusCode)"
    }
} catch {
    throw "Nacos is not reachable at $nacosServer/nacos. Start Nacos first."
}

if (-not $SkipPublish) {
    & powershell -ExecutionPolicy Bypass -File $publishScript -EnvFile $EnvFile -NacosServer $nacosServer -Group $nacosGroup -Namespace $nacosNamespace -Username $nacosUsername -Password $nacosPassword
}

$modules = @("auth-service", "api-gateway", "order-service", "menu-service", "ai-chat-service", "knowledge-service", "admin-service")
foreach ($module in $modules) {
    Start-Process -FilePath "powershell" -WorkingDirectory $backendDir -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $runScript,
        "-Module", $module,
        "-EnvFile", $EnvFile,
        "-Profile", $Profile,
        "-MvnCommand", $MvnCommand
    )
    Write-Host "Started $module in a new terminal window."
}

Write-Host "All service start commands were issued."

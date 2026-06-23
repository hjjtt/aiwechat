param(
    [string]$EnvFile = ".env",
    [string]$NacosServer = "http://127.0.0.1:8848",
    [string]$Group = "AIWECHAT_GROUP",
    [string]$Namespace = "",
    [string]$Username = "nacos",
    [string]$Password = "123456",
    [switch]$DryRun,
    [switch]$CheckOnly
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

function Render-Template($Path, $Vars) {
    $content = Get-Content $Path -Raw
    foreach ($entry in $Vars.GetEnumerator()) {
        $content = $content.Replace('${' + $entry.Key + '}', [string]$entry.Value)
    }
    return $content
}

function Ensure-RequiredKeys($Vars, $Keys) {
    $missing = @()
    foreach ($key in $Keys) {
        if (-not $Vars.ContainsKey($key) -or [string]::IsNullOrWhiteSpace([string]$Vars[$key])) {
            $missing += $key
        }
    }
    if ($missing.Count -gt 0) {
        throw "Missing required .env keys: $($missing -join ', ')"
    }
}

function Test-UnresolvedPlaceholders($Content, $DataId) {
    $matches = [regex]::Matches($Content, '\$\{[A-Z0-9_]+\}')
    if ($matches.Count -gt 0) {
        $tokens = $matches | ForEach-Object { $_.Value } | Select-Object -Unique
        throw "Template '$DataId' still has unresolved placeholders: $($tokens -join ', ')"
    }
}

function New-NacosHeaders($Vars, $Token) {
    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers['accessToken'] = $Token
    }

    if ($Vars.ContainsKey('NACOS_AUTH_IDENTITY_KEY') -and $Vars.ContainsKey('NACOS_AUTH_IDENTITY_VALUE')) {
        $idKey = [string]$Vars['NACOS_AUTH_IDENTITY_KEY']
        $idValue = [string]$Vars['NACOS_AUTH_IDENTITY_VALUE']
        if (-not [string]::IsNullOrWhiteSpace($idKey) -and -not [string]::IsNullOrWhiteSpace($idValue)) {
            $headers[$idKey] = $idValue
        }
    }

    return $headers
}

function Get-NacosToken($BaseUrl, $Username, $Password, $Vars) {
    $customToken = ""
    if ($Vars.ContainsKey('NACOS_AUTH_TOKEN')) {
        $customToken = [string]$Vars['NACOS_AUTH_TOKEN']
    }
    if (-not [string]::IsNullOrWhiteSpace($customToken) -and $customToken -ne 'change_me_nacos_token') {
        return $customToken
    }

    try {
        $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/nacos/v1/auth/users/login" -ContentType "application/x-www-form-urlencoded" -Body @{ username = $Username; password = $Password }
        if ($resp.accessToken) { return $resp.accessToken }
        if ($resp.data -and $resp.data.accessToken) { return $resp.data.accessToken }
    } catch {
        throw "Failed to login Nacos at $BaseUrl. Error: $($_.Exception.Message)"
    }

    return ""
}

function Publish-Config($BaseUrl, $Headers, $Namespace, $Group, $DataId, $Content, $DryRunMode) {
    if ($DryRunMode) {
        Write-Host "[DryRun] Prepared $DataId (group=$Group, namespace=$Namespace)"
        return
    }

    $body = @{
        tenant = $Namespace
        dataId = $DataId
        group = $Group
        type = "yaml"
        content = $Content
    }

    $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/nacos/v1/cs/configs" -Headers $Headers -ContentType "application/x-www-form-urlencoded" -Body $body
    if ($resp -ne $true -and $resp -ne "true") {
        throw "Publishing failed for $DataId. Response: $resp"
    }
    Write-Host "Published $DataId"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")
$tplDir = Join-Path $repoRoot "scripts\nacos\templates"

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $candidate = Join-Path $repoRoot $EnvFile
    if (Test-Path $candidate) {
        $EnvFile = $candidate
    } else {
        $EnvFile = Join-Path (Get-Location) $EnvFile
    }
}

if (-not (Test-Path $EnvFile)) {
    throw ".env file not found: $EnvFile"
}

$templates = @(
    @{ DataId = "shared-config.yaml"; Path = (Join-Path $tplDir "shared-config.yaml.tpl") },
    @{ DataId = "auth-service.yaml"; Path = (Join-Path $tplDir "auth-service.yaml.tpl") },
    @{ DataId = "api-gateway.yaml"; Path = (Join-Path $tplDir "api-gateway.yaml.tpl") },
    @{ DataId = "order-service.yaml"; Path = (Join-Path $tplDir "order-service.yaml.tpl") },
    @{ DataId = "menu-service.yaml"; Path = (Join-Path $tplDir "menu-service.yaml.tpl") },
    @{ DataId = "ai-chat-service.yaml"; Path = (Join-Path $tplDir "ai-chat-service.yaml.tpl") },
    @{ DataId = "knowledge-service.yaml"; Path = (Join-Path $tplDir "knowledge-service.yaml.tpl") },
    @{ DataId = "admin-service.yaml"; Path = (Join-Path $tplDir "admin-service.yaml.tpl") }
)

foreach ($item in $templates) {
    if (-not (Test-Path $item.Path)) {
        throw "Template not found: $($item.Path)"
    }
}

$vars = Read-DotEnv $EnvFile
$vars["NACOS_SERVER_ADDR"] = ($NacosServer -replace '^https?://', '')
$vars["NACOS_NAMESPACE"] = $Namespace
$vars["NACOS_USERNAME"] = $Username
$vars["NACOS_PASSWORD"] = $Password
$vars["NACOS_GROUP"] = $Group

if ([string]::IsNullOrWhiteSpace($Namespace)) {
    Write-Warning "NACOS_NAMESPACE is empty. Configs will be published to the public namespace."
}

$defaults = @{
    "WECHAT_SESSION_HOST" = "https://api.weixin.qq.com/sns/jscode2session"
    "AI_BASE_URL" = "https://api-inference.modelscope.cn/v1"
    "AI_MODEL" = "qwen/Qwen3-1.7B"
    "AI_TEMPERATURE" = "0.7"
    "AI_MODEL_INDEX" = "0"
    "TOKEN_EXPIRE_HOURS" = "72"
    "CHAT_HISTORY_LIMIT" = "10"
    "KNOWLEDGE_SYNC_ENABLED" = "true"
    "KNOWLEDGE_SYNC_CRON" = "0 0 3 * * ?"
    "AMAP_GEOCODE_URL" = "https://restapi.amap.com/v3/geocode/geo"
    "AMAP_REVERSE_GEOCODE_URL" = "https://restapi.amap.com/v3/geocode/regeo"
    "AMAP_PLACE_SEARCH_URL" = "https://restapi.amap.com/v3/place/text"
    "AMAP_NEARBY_SEARCH_URL" = "https://restapi.amap.com/v3/place/nearby"
    "RATE_LIMIT_ENABLED" = "true"
    "RATE_LIMIT_REQUESTS_PER_MINUTE" = "60"
    "RATE_LIMIT_WINDOW_SECONDS" = "60"
    "VECTORSTORE_PATH" = "./uploaded/vectorstore.json"
    "AUTH_SERVICE_URI" = "http://localhost:9091"
    "AUTH_SERVICE_URL" = "http://localhost:9091"
    "ADMIN_AUTH_SECRET" = "please_change_admin_secret"
    "ADMIN_AUTH_USERNAME" = "admin"
    "ADMIN_AUTH_PASSWORD" = "admin123"
    "EMBEDDING_MODEL" = "Alibaba-NLP/gte-large-zh"
}

foreach ($key in $defaults.Keys) {
    if (-not $vars.ContainsKey($key) -or [string]::IsNullOrWhiteSpace([string]$vars[$key])) {
        $vars[$key] = $defaults[$key]
    }
}

$required = @(
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD",
    "WECHAT_APP_ID",
    "WECHAT_APP_SECRET",
    "MODELSCOPE_API_KEY",
    "AMAP_API_KEY"
)
Ensure-RequiredKeys -Vars $vars -Keys $required

$rendered = @()
foreach ($item in $templates) {
    $content = Render-Template -Path $item.Path -Vars $vars
    Test-UnresolvedPlaceholders -Content $content -DataId $item.DataId
    $rendered += @{
        DataId = $item.DataId
        Content = $content
    }
}

$token = Get-NacosToken -BaseUrl $NacosServer -Username $Username -Password $Password -Vars $vars
$headers = New-NacosHeaders -Vars $vars -Token $token

if ($CheckOnly) {
    Write-Host "Check completed:"
    Write-Host "- .env loaded: $EnvFile"
    Write-Host "- Nacos reachable: $NacosServer"
    Write-Host "- Group: $Group"
    Write-Host "- Namespace: $Namespace"
    Write-Host "- Templates: $($templates.Count)"
    Write-Host "No unresolved placeholders. Ready to publish."
    exit 0
}

foreach ($item in $rendered) {
    Publish-Config -BaseUrl $NacosServer -Headers $headers -Namespace $Namespace -Group $Group -DataId $item.DataId -Content $item.Content -DryRunMode:$DryRun
}

Write-Host "All configs processed."

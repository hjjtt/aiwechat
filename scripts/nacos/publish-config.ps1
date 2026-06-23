param(
    [string]$EnvFile = ".env",
    [string]$NacosServer = "http://127.0.0.1:8848",
    [string]$Group = "AIWECHAT_GROUP",
    [string]$Namespace = "",
    [string]$Username = "nacos",
    [string]$Password = "nacos"
)

$ErrorActionPreference = "Stop"

function Read-DotEnv($Path) {
    $map = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
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

function Get-NacosToken($BaseUrl, $Username, $Password) {
    try {
        $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/nacos/v1/auth/users/login" -ContentType "application/x-www-form-urlencoded" -Body @{ username = $Username; password = $Password }
        if ($resp.accessToken) { return $resp.accessToken }
        if ($resp.data -and $resp.data.accessToken) { return $resp.data.accessToken }
    } catch {
        Write-Host "Nacos auth login skipped (auth may be disabled): $($_.Exception.Message)"
    }
    return ""
}

function Publish-Config($BaseUrl, $Token, $Namespace, $Group, $DataId, $Content) {
    $headers = @{}
    if ($Token) { $headers['accessToken'] = $Token }

    $body = @{
        tenant = $Namespace
        dataId = $DataId
        group = $Group
        type = 'yaml'
        content = $Content
    }

    Invoke-RestMethod -Method Post -Uri "$BaseUrl/nacos/v1/cs/configs" -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $body | Out-Null
    Write-Host "Published $DataId"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..\..")

if (-not [System.IO.Path]::IsPathRooted($EnvFile)) {
    $EnvFile = Join-Path $repoRoot $EnvFile
}

if (-not (Test-Path $EnvFile)) {
    throw ".env file not found: $EnvFile"
}

$vars = Read-DotEnv $EnvFile
$vars['NACOS_SERVER_ADDR'] = ($NacosServer -replace '^https?://', '')
$vars['NACOS_NAMESPACE'] = $Namespace
$vars['NACOS_USERNAME'] = $Username
$vars['NACOS_PASSWORD'] = $Password

$defaults = @{
    'WECHAT_SESSION_HOST' = 'https://api.weixin.qq.com/sns/jscode2session'
    'AI_BASE_URL' = 'https://api-inference.modelscope.cn/v1'
    'AI_MODEL' = 'qwen/Qwen3-1.7B'
    'AI_TEMPERATURE' = '0.7'
    'AI_MODEL_INDEX' = '0'
    'TOKEN_EXPIRE_HOURS' = '72'
    'CHAT_HISTORY_LIMIT' = '10'
    'KNOWLEDGE_SYNC_ENABLED' = 'true'
    'KNOWLEDGE_SYNC_CRON' = '0 0 3 * * ?'
    'AMAP_GEOCODE_URL' = 'https://restapi.amap.com/v3/geocode/geo'
    'AMAP_REVERSE_GEOCODE_URL' = 'https://restapi.amap.com/v3/geocode/regeo'
    'AMAP_PLACE_SEARCH_URL' = 'https://restapi.amap.com/v3/place/text'
    'AMAP_NEARBY_SEARCH_URL' = 'https://restapi.amap.com/v3/place/nearby'
    'RATE_LIMIT_ENABLED' = 'true'
    'RATE_LIMIT_REQUESTS_PER_MINUTE' = '60'
    'RATE_LIMIT_WINDOW_SECONDS' = '60'
    'VECTORSTORE_PATH' = './uploaded/vectorstore.json'
    'AUTH_SERVICE_URL' = 'http://localhost:9091'
    'ADMIN_AUTH_SECRET' = 'change-me'
    'ADMIN_AUTH_USERNAME' = 'admin'
    'ADMIN_AUTH_PASSWORD' = 'admin123'
    'NACOS_GROUP' = 'AIWECHAT_GROUP'
    'EMBEDDING_API_KEY' = 'change_me_embedding_api_key'
    'EMBEDDING_BASE_URL' = 'https://open.bigmodel.cn/api/paas/v4'
    'EMBEDDING_MODEL' = 'embedding-3'
}

foreach ($key in $defaults.Keys) {
    if (-not $vars.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($vars[$key])) {
        $vars[$key] = $defaults[$key]
    }
}

$token = Get-NacosToken -BaseUrl $NacosServer -Username $Username -Password $Password

$tplDir = Join-Path $scriptDir "templates"
$templates = @(
    @{ DataId = 'shared-config.yaml'; Path = (Join-Path $tplDir 'shared-config.yaml.tpl') },
    @{ DataId = 'auth-service.yaml'; Path = (Join-Path $tplDir 'auth-service.yaml.tpl') },
    @{ DataId = 'api-gateway.yaml'; Path = (Join-Path $tplDir 'api-gateway.yaml.tpl') },
    @{ DataId = 'order-service.yaml'; Path = (Join-Path $tplDir 'order-service.yaml.tpl') },
    @{ DataId = 'menu-service.yaml'; Path = (Join-Path $tplDir 'menu-service.yaml.tpl') },
    @{ DataId = 'ai-chat-service.yaml'; Path = (Join-Path $tplDir 'ai-chat-service.yaml.tpl') },
    @{ DataId = 'knowledge-service.yaml'; Path = (Join-Path $tplDir 'knowledge-service.yaml.tpl') },
    @{ DataId = 'admin-service.yaml'; Path = (Join-Path $tplDir 'admin-service.yaml.tpl') }
)

foreach ($item in $templates) {
    $content = Render-Template -Path $item.Path -Vars $vars
    Publish-Config -BaseUrl $NacosServer -Token $token -Namespace $Namespace -Group $Group -DataId $item.DataId -Content $content
}

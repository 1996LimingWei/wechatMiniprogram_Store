$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Require-File([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "缺少文件：$Path"
    }
}

function Assert-Contains([string]$Path, [string]$Pattern, [string]$Message) {
    $content = Get-Content $Path -Raw -Encoding utf8
    if ($content -notmatch $Pattern) {
        throw $Message
    }
}

function Assert-NotContains([string]$Path, [string]$Pattern, [string]$Message) {
    $content = Get-Content $Path -Raw -Encoding utf8
    if ($content -match $Pattern) {
        throw $Message
    }
}

$envDevelopment = Join-Path $root "shop-admin/.env.development"
$envStaging = Join-Path $root "shop-admin/.env.staging"
$envProduction = Join-Path $root "shop-admin/.env.production"
$http = Join-Path $root "shop-admin/src/utils/http/index.ts"
$login = Join-Path $root "shop-admin/src/views/login/index.vue"
$error403 = Join-Path $root "shop-admin/src/views/error/403.vue"
$error404 = Join-Path $root "shop-admin/src/views/error/404.vue"
$error500 = Join-Path $root "shop-admin/src/views/error/500.vue"
$packageJson = Join-Path $root "shop-admin/package.json"

foreach ($file in @($envDevelopment, $envStaging, $envProduction, $http, $login, $error403, $error404, $error500, $packageJson)) {
    Require-File $file
}

foreach ($file in @($envDevelopment, $envStaging, $envProduction)) {
    Assert-Contains $file 'VITE_ADMIN_API_BASE_URL' "$file 必须声明 VITE_ADMIN_API_BASE_URL"
}

Assert-NotContains $envStaging 'localhost|127\.0\.0\.1|VITE_PROXY_TARGET' "管理后台预发布环境不得包含本地代理目标"
Assert-NotContains $envProduction 'localhost|127\.0\.0\.1|VITE_PROXY_TARGET' "管理后台生产环境不得包含本地代理目标"

Assert-Contains $http 'VITE_ADMIN_API_BASE_URL' "管理后台 HTTP 客户端必须读取 VITE_ADMIN_API_BASE_URL"
Assert-Contains $http 'baseURL:\s*adminApiBaseUrl' "管理后台 Axios 必须通过 baseURL 使用环境注入地址"
Assert-Contains $http 'safeErrorMessage' "管理后台错误提示必须经过脱敏过滤"
foreach ($keyword in @("/admin-api", "/app-api", "exception", "stack", "trace", "sql", "jdbc", "token", "authorization", "password", "secret")) {
    Assert-Contains $http ([regex]::Escape($keyword)) "管理后台错误脱敏必须覆盖：$keyword"
}

Assert-NotContains $login 'username:\s*"admin"|password:\s*"admin123"|体验|测试|Mock' "管理后台登录页不得预填默认账号或展示测试文案"
Assert-Contains $login 'v1\.0 客户交付版' "管理后台登录页必须展示正式版本信息"
Assert-Contains $error403 '当前账号无权访问该功能' "403 页面必须使用客户可接受的正式文案"
Assert-Contains $error404 '页面不存在或已被调整' "404 页面必须使用客户可接受的正式文案"
Assert-Contains $error500 '服务暂时不可用，请稍后再试' "500 页面必须使用客户可接受的正式文案"

Assert-Contains $packageJson '"lint:check"' "管理后台必须提供只检查不改写的 lint:check 脚本"
Assert-Contains $packageJson '"lint:eslint:check"' "管理后台必须提供 eslint 检查脚本"
Assert-Contains $packageJson '"lint:prettier:check"' "管理后台必须提供 prettier 检查脚本"
Assert-Contains $packageJson '"lint:stylelint:check"' "管理后台必须提供 stylelint 检查脚本"

Write-Host "管理后台生产就绪校验通过。"

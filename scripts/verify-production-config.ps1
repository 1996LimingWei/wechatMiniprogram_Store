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

$prod = Join-Path $root "shop-backend/shop-server/src/main/resources/application-prod.yml"
$staging = Join-Path $root "shop-backend/shop-server/src/main/resources/application-staging.yml"
$adminProduction = Join-Path $root "shop-admin/.env.production"
$adminStaging = Join-Path $root "shop-admin/.env.staging"
$miniappEnv = Join-Path $root "shop-miniapp/config/env.js"

Require-File $prod
Require-File $staging
Require-File $adminProduction
Require-File $adminStaging
Require-File $miniappEnv

foreach ($file in @($prod, $staging)) {
    Assert-Contains $file 'mock-enabled:\s*false' "$file 必须关闭微信登录 Mock"
    Assert-Contains $file 'mock-endpoints-enabled:\s*false' "$file 必须关闭商品 Mock 端点"
    Assert-Contains $file 'mock-actions-enabled:\s*false' "$file 必须关闭交易 Mock 操作"
    Assert-Contains $file 'provider:\s*\$\{TRADE_REFUND_PROVIDER:wechat\}' "$file 退款 Provider 默认必须为 wechat"
    Assert-Contains $file 'provider:\s*\$\{TRADE_LOGISTICS_PROVIDER:kuaidi100\}' "$file 物流 Provider 默认必须为 kuaidi100"
    Assert-Contains $file 'provider:\s*\$\{MATERIAL_STORAGE_PROVIDER:mounted\}' "$file 素材存储生产默认必须为 mounted"
    Assert-Contains $file 'public-base-url:\s*\$\{MATERIAL_PUBLIC_BASE_URL:\}' "$file 素材公开访问地址必须由环境变量注入"
    Assert-Contains $file 'allowed-origin-patterns:\s*\$\{WEB_CORS_ALLOWED_ORIGIN_PATTERNS:\}' "$file CORS 必须由环境变量注入"
    Assert-Contains $file 'external-base-url:\s*\$\{APP_EXTERNAL_BASE_URL:\}' "$file 必须配置外部访问地址"
    Assert-Contains $file 'refund-notify-url:\s*\$\{WECHAT_PAY_REFUND_NOTIFY_URL:\}' "$file 必须配置微信退款回调地址"
    Assert-NotContains $file 'localhost|127\.0\.0\.1|admin123|password:\s*root' "$file 不得包含本地地址、默认密码或 root 密码"
}

Assert-NotContains $adminProduction 'localhost|127\.0\.0\.1|VITE_PROXY_TARGET' "管理后台生产环境不得包含本地代理目标"
Assert-NotContains $adminStaging 'localhost|127\.0\.0\.1|VITE_PROXY_TARGET' "管理后台预发布环境不得包含本地代理目标"
Assert-Contains $miniappEnv '正式环境 API 地址必须使用 HTTPS' "小程序正式环境必须校验 HTTPS API"
Assert-Contains $miniappEnv '正式环境未配置 VUE_APP_API_BASE_URL' "小程序正式环境必须要求注入 API 地址"

Write-Host "生产/预发布配置静态校验通过。"

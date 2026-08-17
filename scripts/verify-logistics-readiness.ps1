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

$service = Join-Path $root "shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/TradeLogisticsService.java"
$provider = Join-Path $root "shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/provider/Kuaidi100TradeLogisticsProvider.java"
$prod = Join-Path $root "shop-backend/shop-server/src/main/resources/application-prod.yml"
$staging = Join-Path $root "shop-backend/shop-server/src/main/resources/application-staging.yml"
$adminOrder = Join-Path $root "shop-admin/src/views/order/index.vue"
$miniappDetail = Join-Path $root "shop-miniapp/pages/ucenter/orderDetail/orderDetail.vue"
$miniappList = Join-Path $root "shop-miniapp/pages/ucenter/order/order.vue"

foreach ($file in @($service, $provider, $prod, $staging, $adminOrder, $miniappDetail, $miniappList)) {
    Require-File $file
}

foreach ($file in @($prod, $staging)) {
    Assert-Contains $file 'provider:\s*\$\{TRADE_LOGISTICS_PROVIDER:kuaidi100\}' "$file 生产/预发布物流 Provider 默认必须为 kuaidi100"
    Assert-Contains $file 'customer:\s*\$\{KUAIDI100_CUSTOMER:\}' "$file 必须由环境变量注入快递100 customer"
    Assert-Contains $file 'key:\s*\$\{KUAIDI100_KEY:\}' "$file 必须由环境变量注入快递100 key"
}

$expectedCodes = @("shunfeng", "zhongtong", "yuantong", "yunda", "jtexpress", "shentong", "jd", "ems")
foreach ($code in $expectedCodes) {
    Assert-Contains $service ([regex]::Escape($code)) "后端物流公司编码缺少 $code"
    Assert-Contains $adminOrder ([regex]::Escape($code)) "后台物流公司下拉缺少 $code"
}

Assert-Contains $service '物流公司与编码不匹配' "后台发货必须校验物流公司与编码一致"
Assert-Contains $service 'minusMinutes\(30\)' "物流查询缓存必须具备 30 分钟新鲜度"
Assert-Contains $service '物流轨迹暂时不可用' "物流查询失败必须返回友好提示"
Assert-Contains $service '物流服务暂时不可用，当前显示最近缓存' "物流查询失败必须可回退最近缓存"
Assert-Contains $provider 'https://poll\.kuaidi100\.com/poll/query\.do' "快递100正式查询地址必须存在"
Assert-Contains $provider 'validateConfiguration' "快递100 Provider 必须校验账号配置"
Assert-Contains $provider 'param\.put\("phone"' "快递100 查询必须传入收件手机号以支持隐私面单查询"
Assert-Contains $adminOrder 'logistics\.queryMessage' "后台物流详情必须展示查询失败提示"
Assert-Contains $adminOrder 'maxlength="32"' "后台物流单号输入长度必须与后端一致"
Assert-Contains $miniappDetail 'logistics\.traces' "小程序订单详情必须展示物流轨迹"
Assert-Contains $miniappDetail 'logistics\.queryMessage' "小程序订单详情必须展示物流失败提示"
Assert-Contains $miniappList 'formatLogisticsModal' "小程序订单列表物流弹窗必须展示最新轨迹或失败提示"

Write-Host "真实物流验收代码准备校验通过。"

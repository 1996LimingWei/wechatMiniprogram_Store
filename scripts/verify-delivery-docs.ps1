$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Require-File([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "缺少交付文档：$Path"
    }
}

function Assert-Contains([string]$Path, [string]$Pattern, [string]$Message) {
    $content = Get-Content $Path -Raw -Encoding utf8
    if ($content -notmatch $Pattern) {
        throw $Message
    }
}

$guide = Join-Path $root "docs/delivery/v1.0-customer-delivery-guide.md"
$report = Join-Path $root "docs/delivery/v1.0-final-acceptance-report.md"
$miniapp = Join-Path $root "docs/acceptance/v1.0-miniapp-review-checklist.md"
$payRefund = Join-Path $root "docs/acceptance/v1.0-wechat-pay-refund-acceptance.md"
$logistics = Join-Path $root "docs/acceptance/v1.0-logistics-acceptance.md"

foreach ($file in @($guide, $report, $miniapp, $payRefund, $logistics)) {
    Require-File $file
}

$guideChecks = @(
    "部署文档",
    "回滚方案",
    "运维巡检",
    "数据库备份恢复",
    "管理后台使用手册",
    "商品导入模板说明",
    "发货导入模板说明",
    "微信支付配置说明",
    "快递 100 配置说明",
    "小程序提审资料清单",
    "客户验收清单",
    "CI 执行记录",
    "已知限制",
    "v1.0 发布说明",
    "WECHAT_PAY_REFUND_NOTIFY_URL",
    "KUAIDI100_CUSTOMER"
)

foreach ($item in $guideChecks) {
    Assert-Contains $guide ([regex]::Escape($item)) "客户交付手册缺少：$item"
}

$reportChecks = @(
    "版本信息",
    "环境验收",
    "用户交易链路",
    "商品运营链路",
    "订单履约链路",
    "售后退款链路",
    "财务对账链路",
    "安全与权限",
    "监控与运维",
    "CI 与门禁",
    "已知限制确认",
    "验收结论"
)

foreach ($item in $reportChecks) {
    Assert-Contains $report ([regex]::Escape($item)) "最终验收报告缺少：$item"
}

Assert-Contains $payRefund '退款重复通知幂等' "支付退款验收记录必须覆盖退款通知幂等"
Assert-Contains $logistics '查询失败回退缓存' "物流验收记录必须覆盖失败缓存回退"
Assert-Contains $miniapp '体验版' "小程序提审清单必须覆盖体验版"

Write-Host "客户交付文档校验通过。"

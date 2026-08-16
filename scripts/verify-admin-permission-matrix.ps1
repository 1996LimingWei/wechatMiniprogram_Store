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

function Assert-TextContains([string]$Content, [string]$Pattern, [string]$Message) {
    if ($Content -notmatch $Pattern) {
        throw $Message
    }
}

$adminFilter = Join-Path $root "shop-backend/shop-framework/shop-starter-security/src/main/java/com/shop/framework/security/AdminSecurityFilter.java"
$authService = Join-Path $root "shop-backend/shop-module-system/src/main/java/com/shop/module/system/service/AdminAuthService.java"
$rbacBase = Join-Path $root "sql/migrations/V20260813_04__rbac_trade_scope_and_audit_reference.sql"
$rbacV1 = Join-Path $root "sql/migrations/V20260816_09__rbac_v1_permission_matrix.sql"
$routerUtils = Join-Path $root "shop-admin/src/router/utils.ts"
$routerIndex = Join-Path $root "shop-admin/src/router/index.ts"
$authUtils = Join-Path $root "shop-admin/src/utils/auth.ts"
$orderView = Join-Path $root "shop-admin/src/views/order/index.vue"
$afterSaleView = Join-Path $root "shop-admin/src/views/after-sale/index.vue"
$paymentView = Join-Path $root "shop-admin/src/views/payment/index.vue"
$refundView = Join-Path $root "shop-admin/src/views/refund/index.vue"
$reconcileView = Join-Path $root "shop-admin/src/views/reconcile/index.vue"
$rbacMigrations = (Get-ChildItem (Join-Path $root "sql/migrations") -Filter "*.sql" |
    ForEach-Object { Get-Content $_.FullName -Raw -Encoding utf8 }) -join "`n"

foreach ($file in @($adminFilter, $authService, $rbacBase, $rbacV1, $routerUtils, $routerIndex, $authUtils, $orderView, $afterSaleView, $paymentView, $refundView, $reconcileView)) {
    Require-File $file
}

Assert-Contains $adminFilter 'sys_role_permission' "后端管理端过滤器必须读取角色权限关联表"
Assert-Contains $adminFilter 'pathMatcher\.match\(rule\.pathPattern\(\), uri\)' "后端管理端过滤器必须按 pathPattern 匹配接口"
Assert-Contains $adminFilter 'method\.equalsIgnoreCase\(rule\.httpMethod\(\)\)' "后端管理端过滤器必须按 HTTP Method 匹配权限"
Assert-Contains $adminFilter 'CommonResult\.error\(403,\s*"没有该管理操作权限"\)' "后端管理端过滤器无权限必须返回 403"
Assert-Contains $authService 'result\.put\("permissions", permissions\)' "管理后台登录资料必须返回权限码"

foreach ($keyword in @(
    "READONLY",
    "ORDER_CUSTOMER_SERVICE",
    "AFTER_SALE_REVIEWER",
    "FINANCE",
    "PRODUCT_OPERATOR",
    "trade:order-export",
    "trade:order-batch-ship",
    "trade:order-delivery-note",
    "trade:order-picking-list",
    "trade:reconcile-export",
    "trade:payment-sync",
    "trade:refund-sync",
    "system:admin-user",
    "system:role"
)) {
    Assert-TextContains $rbacMigrations ([regex]::Escape($keyword)) "RBAC 迁移必须覆盖：$keyword"
}

Assert-Contains $rbacBase 'trade:after-sale-process' "基础 RBAC 迁移必须覆盖售后处理权限"
Assert-Contains $rbacV1 "r\.code = 'READONLY'" "只读角色必须有权限收紧规则"
Assert-Contains $rbacV1 "\(p\.http_method <> 'GET'" "只读角色必须剥离非 GET 写权限"
Assert-Contains $rbacV1 "r\.code = 'FINANCE'" "财务角色必须有权限收紧规则"
Assert-Contains $rbacV1 "trade:order-ship" "财务角色必须剥离发货权限"
Assert-Contains $rbacV1 "PRODUCT_OPERATOR" "商品运营角色必须有权限收紧规则"
Assert-Contains $rbacV1 "trade:reconcile-export" "商品运营必须剥离财务对账导出权限"

Assert-Contains $routerUtils 'requiredPermissions' "前端动态路由必须支持 permissions 元信息"
Assert-Contains $routerIndex 'to\.meta\?\.permissions' "前端路由守卫必须校验页面权限"
Assert-Contains $authUtils 'hasAnyPerms' "前端必须提供任一权限按钮判断"

foreach ($keyword in @(
    "trade:order-ship",
    "trade:logistics-read",
    "trade:order-export",
    "trade:order-batch-ship",
    "trade:order-delivery-note",
    "trade:order-picking-list"
)) {
    Assert-Contains $orderView ([regex]::Escape($keyword)) "订单页按钮必须按权限控制：$keyword"
}

Assert-Contains $afterSaleView 'trade:after-sale-process' "售后页处理按钮必须按权限控制"
Assert-Contains $paymentView 'trade:payment-sync' "支付页同步按钮必须按权限控制"
Assert-Contains $paymentView 'trade:payment-handle' "支付异常处理按钮必须按权限控制"
Assert-Contains $refundView 'trade:refund-sync' "退款页同步按钮必须按权限控制"
Assert-Contains $refundView 'trade:refund-handle' "退款异常处理按钮必须按权限控制"
Assert-Contains $reconcileView 'trade:reconcile-trigger' "对账页生成按钮必须按权限控制"
Assert-Contains $reconcileView 'trade:reconcile-export' "对账页导出按钮必须按权限控制"
Assert-Contains $reconcileView 'trade:reconcile-handle' "对账页处理按钮必须按权限控制"

Write-Host "管理员权限矩阵校验通过。"

param(
    [string]$BaseUrl = "http://localhost:8085",
    [string]$MysqlContainer = "shop-mysql",
    [string]$MysqlDatabase = "shop",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin123"
)

$ErrorActionPreference = "Stop"
$env:NO_PROXY = "localhost,127.0.0.1"
$env:no_proxy = "localhost,127.0.0.1"

function Invoke-Sql([string]$Sql) {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $result = & docker exec $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" `
        --default-character-set=utf8mb4 $MysqlDatabase -N -B -e $Sql 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    if ($exitCode -ne 0) {
        throw "SQL 执行失败"
    }
    return $result
}

function Assert-Zero([string]$Name, [string]$Sql) {
    $count = [long](Invoke-Sql $Sql)
    if ($count -ne 0) {
        throw "对账失败：$Name，异常记录数=$count"
    }
    Write-Host "[通过] $Name"
}

function Get-AppProductIds {
    $ids = [System.Collections.Generic.HashSet[long]]::new()
    $page = 1
    do {
        $response = Invoke-RestMethod -Uri "$BaseUrl/app-api/goods/list?page=$page&size=100" -Method Get
        if ($response.code -ne 0) {
            throw "小程序商品列表返回失败：$($response.msg)"
        }
        foreach ($item in $response.data.goodsList.records) {
            [void]$ids.Add([long]$item.id)
        }
        $pages = [int]$response.data.goodsList.pages
        $page++
    } while ($page -le $pages)
    return $ids
}

function Get-AdminOnSaleProductIds {
    $loginBody = @{ username = $AdminUsername; password = $AdminPassword } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Uri "$BaseUrl/admin-api/auth/login" -Method Post `
        -ContentType "application/json" -Body $loginBody
    if ($login.code -ne 0 -or -not $login.data.token) {
        throw "管理后台登录失败"
    }
    $headers = @{ Authorization = "Bearer $($login.data.token)" }
    $ids = [System.Collections.Generic.HashSet[long]]::new()
    $page = 1
    do {
        $response = Invoke-RestMethod -Uri "$BaseUrl/admin-api/product/spu/page?pageNo=$page&pageSize=100&status=1" `
            -Method Get -Headers $headers
        if ($response.code -ne 0) {
            throw "管理后台商品列表返回失败：$($response.msg)"
        }
        foreach ($item in $response.data.list) {
            [void]$ids.Add([long]$item.id)
        }
        $page++
    } while ($ids.Count -lt [long]$response.data.total)
    return $ids
}

Assert-Zero "SPU 库存等于有效 SKU 库存汇总" @"
SELECT COUNT(*) FROM product_spu p
WHERE p.deleted=0 AND p.stock<>(
  SELECT COALESCE(SUM(s.stock),0) FROM product_sku s WHERE s.spu_id=p.id AND s.deleted=0
)
"@

Assert-Zero "SPU 售价等于最低有效 SKU 售价" @"
SELECT COUNT(*) FROM product_spu p
WHERE p.deleted=0 AND p.price<>(
  SELECT COALESCE(MIN(s.price),0) FROM product_sku s WHERE s.spu_id=p.id AND s.deleted=0
)
"@

Assert-Zero "上架商品均存在有效 SKU" @"
SELECT COUNT(*) FROM product_spu p
WHERE p.deleted=0 AND p.status=1 AND NOT EXISTS (
  SELECT 1 FROM product_sku s WHERE s.spu_id=p.id AND s.deleted=0 AND s.price>0 AND s.stock>=0
)
"@

Assert-Zero "购物车不存在孤儿规格或非法数量" @"
SELECT COUNT(*) FROM trade_cart c
LEFT JOIN product_sku s ON s.id=c.sku_id AND s.spu_id=c.spu_id AND s.deleted=0
WHERE c.deleted=0 AND (s.id IS NULL OR c.count<1 OR c.count>99 OR c.price<=0)
"@

Assert-Zero "订单金额公式一致" @"
SELECT COUNT(*) FROM trade_order o
WHERE o.deleted=0 AND (
  o.goods_price<0 OR o.freight_price<0 OR o.coupon_price<0
  OR o.order_price<>o.goods_price+o.freight_price-o.coupon_price
  OR o.actual_price<>o.order_price OR o.actual_price<=0
)
"@

Assert-Zero "订单明细小计一致" @"
SELECT COUNT(*) FROM trade_order_item oi
WHERE oi.deleted=0 AND (oi.price<=0 OR oi.count<1 OR oi.count>99 OR oi.total_price<>oi.price*oi.count)
"@

Assert-Zero "支付单金额与订单实付金额一致" @"
SELECT COUNT(*) FROM pay_order p
JOIN trade_order o ON o.id=p.order_id AND o.deleted=0
WHERE p.deleted=0 AND p.amount<>o.actual_price
"@

Assert-Zero "订单与支付状态组合合法" @"
SELECT COUNT(*) FROM trade_order o WHERE o.deleted=0 AND NOT (
  (o.status=0 AND o.pay_status=0) OR
  (o.status IN (1,2,3) AND o.pay_status=1) OR
  (o.status=4 AND o.pay_status=0) OR
  (o.status=5 AND o.pay_status IN (1,2))
)
"@

Assert-Zero "退款成功状态在售后、订单和支付单之间一致" @"
SELECT COUNT(*) FROM trade_after_sale a
JOIN trade_order o ON o.id=a.order_id AND o.deleted=0
LEFT JOIN pay_order p ON p.order_id=o.id AND p.deleted=0
WHERE a.deleted=0 AND a.status=1 AND (o.pay_status<>2 OR p.status<>3)
"@

Assert-Zero "商品销量等于未退款已支付订单数量" @"
SELECT COUNT(*) FROM product_spu p
WHERE p.deleted=0 AND p.sales_count<>(
  SELECT COALESCE(SUM(oi.count),0)
  FROM trade_order_item oi
  JOIN trade_order o ON o.id=oi.order_id AND o.deleted=0
  WHERE oi.spu_id=p.id AND oi.deleted=0 AND o.pay_status=1
)
"@

$appIds = Get-AppProductIds
$adminIds = Get-AdminOnSaleProductIds
$missingInAdmin = @($appIds | Where-Object { -not $adminIds.Contains($_) })
$missingInApp = @($adminIds | Where-Object { -not $appIds.Contains($_) })
if ($missingInAdmin.Count -gt 0 -or $missingInApp.Count -gt 0) {
    throw "前后台商品不一致：前台独有=$($missingInAdmin -join ','), 后台上架但前台缺失=$($missingInApp -join ',')"
}
Write-Host "[通过] 小程序商品与后台上架商品 ID 完全一致，共 $($appIds.Count) 个"
Write-Host "商品与交易全链路数据对账通过"

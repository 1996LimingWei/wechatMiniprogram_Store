param(
    [string]$BaseUrl = "http://localhost:8085",
    [string]$MysqlContainer = "shop-mysql",
    [string]$MysqlDatabase = "shop",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root",
    [int]$TimeoutWaitSeconds = 90
)

$ErrorActionPreference = "Stop"
$env:NO_PROXY = "localhost,127.0.0.1"
$env:no_proxy = "localhost,127.0.0.1"
$ProductId = 9000260726
$SkuId = 9000260726001
$ProductName = "交易验收测试商品"
$RunCodePrefix = "trade_acceptance_"

function Write-Step([string]$Message) {
    Write-Host "[交易验收] $Message"
}

function Assert-True([object]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw "验收失败：$Message"
    }
}

function Invoke-Sql([string]$Sql) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $result = & docker exec $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $MysqlDatabase -N -B -e $Sql 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0) {
        throw "SQL 执行失败：$Sql"
    }
    return $result
}

function Invoke-Api([string]$Path, [object]$Body = @{}, [string]$Token = "") {
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $json = $Body | ConvertTo-Json -Depth 8 -Compress
    $response = Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Headers $headers -ContentType "application/json" -Body $json
    Assert-True ($response.code -eq 0) "$Path 返回失败：$($response.msg)"
    return $response.data
}

function Invoke-GetApi([string]$Path, [string]$Token = "") {
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $response = Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Get -Headers $headers
    Assert-True ($response.code -eq 0) "$Path 返回失败：$($response.msg)"
    return $response.data
}

function Assert-HttpDenied([string]$Path, [string]$Token = "", [string]$Method = "Post") {
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    try {
        $request = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers }
        if ($Method -eq "Post") {
            $request.ContentType = "application/json"
            $request.Body = "{}"
        }
        $response = Invoke-RestMethod @request
        if ($response.code -eq 401 -or $response.code -eq 403) {
            return
        }
        throw "验收失败：$Path 应拒绝访问"
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            Assert-True ($statusCode -eq 401 -or $statusCode -eq 403) "$Path 应返回 401/403，实际为 $statusCode"
            return
        }
        throw
    }
}

function Assert-ApiRejected([string]$Path, [object]$Body, [string]$Token) {
    $headers = @{ Authorization = "Bearer $Token" }
    $json = $Body | ConvertTo-Json -Depth 8 -Compress
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Headers $headers `
            -ContentType "application/json" -Body $json
        Assert-True ($response.code -ne 0) "$Path 应拒绝当前操作"
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            Assert-True ($statusCode -ge 400) "$Path 应返回 HTTP 错误状态"
            return
        }
        throw
    }
}

function Cleanup-TestData {
    Write-Step "清理历史验收测试数据"
    $sql = @"
DELETE FROM trade_order_log WHERE order_id IN (
  SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
);
DELETE FROM trade_after_sale_item WHERE after_sale_id IN (
  SELECT id FROM trade_after_sale WHERE order_id IN (
    SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
  )
);
DELETE FROM trade_after_sale WHERE order_id IN (
  SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
);
DELETE FROM trade_order_logistics WHERE order_id IN (
  SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
);
DELETE FROM pay_order WHERE order_id IN (
  SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
);
DELETE FROM trade_order_item WHERE order_id IN (
  SELECT id FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%')
);
DELETE FROM trade_order WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%');
DELETE FROM trade_cart WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%');
DELETE FROM member_address WHERE user_id IN (SELECT id FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%');
DELETE FROM member_user WHERE openid LIKE 'dev_openid_$RunCodePrefix%';
DELETE FROM product_stock_log WHERE spu_id = $ProductId OR sku_id = $SkuId;
DELETE FROM product_sku WHERE id = $SkuId AND spu_id = $ProductId;
DELETE FROM product_spu WHERE id = $ProductId AND name = '$ProductName';
"@
    Invoke-Sql $sql | Out-Null
}

function Seed-TestProduct {
    Write-Step "准备验收测试商品"
    $sql = @"
INSERT INTO product_spu
  (id, category_id, name, keyword, introduction, description, pic_url, slider_pic_urls, type, price, market_price, stock, sales_count, sort, status)
VALUES
  ($ProductId, 1, '$ProductName', 'trade acceptance', '交易验收专用商品', '交易验收专用商品', 'https://example.com/trade-acceptance.png', '[]', 1, 9900, 12900, 30, 0, 0, 1);
INSERT INTO product_sku
  (id, spu_id, properties, price, market_price, stock, pic_url)
VALUES
  ($SkuId, $ProductId, JSON_ARRAY(JSON_OBJECT('id', 1, 'name', 'Spec', 'valueId', 1, 'valueName', 'Standard')), 9900, 12900, 30, 'https://example.com/trade-acceptance.png');
"@
    Invoke-Sql $sql | Out-Null
}

function New-TestUser([string]$Scene) {
    $code = "$RunCodePrefix$Scene`_$(Get-Date -Format 'yyyyMMddHHmmssfff')"
    $data = Invoke-Api "/app-api/auth/LoginByMa" @{ code = $code; privacyAccepted = $true }
    Assert-True ($data.token) "登录未返回 token"
    return @{
        Token = [string]$data.token
        UserId = [long]$data.userId
    }
}

function Save-TestAddress([string]$Token, [long]$UserId) {
    Invoke-Api "/app-api/address/save" @{
        userName = "交易验收"
        telNumber = "13800138000"
        provinceId = 44
        cityId = 4403
        districtId = 440305
        provinceName = "广东省"
        cityName = "深圳市"
        countyName = "南山区"
        detailInfo = "企业交付验收地址 1 号"
        isDefault = 1
    } $Token | Out-Null
    $addressId = Invoke-Sql "SELECT id FROM member_address WHERE user_id = $UserId ORDER BY id DESC LIMIT 1;"
    Assert-True ($addressId) "保存地址后未查到地址 ID"
    return [long]$addressId
}

function New-PaidOrder([string]$Scene) {
    $user = New-TestUser $Scene
    $addressId = Save-TestAddress $user.Token $user.UserId
    Invoke-Api "/app-api/cart/add" @{ goodsId = $ProductId; productId = $SkuId; number = 1 } $user.Token | Out-Null
    $requestId = "PS$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())$([guid]::NewGuid().ToString('N').Substring(0,8))"
    $submit = Invoke-Api "/app-api/order/submit" @{ addressId = $addressId; requestId = $requestId } $user.Token
    $orderId = [long]$submit.orderInfo.id
    Invoke-Api "/app-api/pay/prepay" @{ orderId = $orderId } $user.Token | Out-Null
    $amounts = Invoke-Sql "SELECT CONCAT(p.amount, ',', o.actual_price) FROM pay_order p JOIN trade_order o ON o.id=p.order_id WHERE p.order_id=$orderId;"
    $amountParts = $amounts.Split(',')
    Assert-True ($amountParts[0] -eq $amountParts[1]) "支付单金额应与订单实付金额一致，实际为 $amounts"
    Invoke-Api "/app-api/pay/mock-success" @{ orderId = $orderId } $user.Token | Out-Null
    Assert-Order $orderId 1 1 "支付成功后订单应为待发货且已支付"
    return @{
        Token = $user.Token
        UserId = $user.UserId
        OrderId = $orderId
    }
}

function New-UnpaidOrder([string]$Scene) {
    $user = New-TestUser $Scene
    $addressId = Save-TestAddress $user.Token $user.UserId
    Invoke-Api "/app-api/cart/add" @{ goodsId = $ProductId; productId = $SkuId; number = 1 } $user.Token | Out-Null
    $requestId = "PS$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())$([guid]::NewGuid().ToString('N').Substring(0,8))"
    $submit = Invoke-Api "/app-api/order/submit" @{ addressId = $addressId; requestId = $requestId } $user.Token
    $orderId = [long]$submit.orderInfo.id
    Assert-Order $orderId 0 0 "提交订单后应为待付款且未支付"
    return @{
        Token = $user.Token
        UserId = $user.UserId
        OrderId = $orderId
    }
}

function Assert-Order([long]$OrderId, [int]$Status, [int]$PayStatus, [string]$Message) {
    $row = Invoke-Sql "SELECT CONCAT(status, ',', pay_status) FROM trade_order WHERE id = $OrderId;"
    Assert-True ($row -eq "$Status,$PayStatus") "$Message，实际为 $row"
}

function Assert-AfterSale([long]$OrderId, [int]$Status, [string]$Message) {
    $row = Invoke-Sql "SELECT status FROM trade_after_sale WHERE order_id = $OrderId ORDER BY id DESC LIMIT 1;"
    Assert-True ($row -eq "$Status") "$Message，实际售后状态为 $row"
}

function Wait-RefundCompleted([long]$OrderId) {
    $deadline = (Get-Date).AddSeconds($TimeoutWaitSeconds)
    do {
        $row = Invoke-Sql "SELECT CONCAT(o.status, ',', o.pay_status, ',', a.status, ',', p.status) FROM trade_order o JOIN trade_after_sale a ON a.order_id=o.id JOIN pay_order p ON p.order_id=o.id WHERE o.id=$OrderId ORDER BY a.id DESC LIMIT 1;"
        if ($row -eq "5,2,1,3") {
            return
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "验收失败：退款未在 $TimeoutWaitSeconds 秒内完成，订单/支付/售后/支付单状态为 $row"
}

function Assert-LogExists([long]$OrderId, [string]$Action) {
    $count = Invoke-Sql "SELECT COUNT(*) FROM trade_order_log WHERE order_id = $OrderId AND action = '$Action';"
    Assert-True ([int]$count -ge 1) "订单 $OrderId 缺少日志 $Action"
}

try {
    Invoke-RestMethod -Uri "$BaseUrl/app-api/product/category/list" -Method Get | Out-Null
    Cleanup-TestData
    Seed-TestProduct

    Write-Step "验收：管理端鉴权边界"
    Assert-HttpDenied "/admin-api/trade/order/list?page=1&size=10" "" "Get"
    $memberForAuth = New-TestUser "member_auth"
    Assert-HttpDenied "/admin-api/trade/order/list?page=1&size=10" $memberForAuth.Token "Get"
    $adminLogin = Invoke-Api "/admin-api/auth/login" @{ username = "admin"; password = "admin123" }
    $adminToken = [string]$adminLogin.token
    Assert-True ($adminToken) "管理员登录未返回 token"

    Write-Step "验收：购物车重复加入与删除"
    $cartUser = New-TestUser "cart_repeat"
    Invoke-Api "/app-api/cart/add" @{ goodsId = $ProductId; productId = $SkuId; number = 1 } $cartUser.Token | Out-Null
    Invoke-Api "/app-api/cart/delete" @{ productIds = "$SkuId" } $cartUser.Token | Out-Null
    Invoke-Api "/app-api/cart/add" @{ goodsId = $ProductId; productId = $SkuId; number = 1 } $cartUser.Token | Out-Null
    Invoke-Api "/app-api/cart/delete" @{ productIds = "$SkuId" } $cartUser.Token | Out-Null
    Assert-True ([int](Invoke-Sql "SELECT COUNT(*) FROM trade_cart WHERE user_id = $($cartUser.UserId) AND sku_id = $SkuId;") -eq 0) "购物车物理删除后仍有遗留记录"

    Write-Step "验收：无售后申请不能直接退款"
    $noApplyFlow = New-PaidOrder "no_apply_refund"
    Assert-ApiRejected "/admin-api/trade/after-sale/approve" @{ afterSaleId = 999999999 } $adminToken
    Assert-Order $noApplyFlow.OrderId 1 1 "无售后申请时订单状态不应变化"

    Write-Step "验收：下单、支付、管理端发货、确认收货"
    $shipFlow = New-PaidOrder "ship"
    $adminList = Invoke-GetApi "/admin-api/trade/order/list?page=1&size=10&orderId=$($shipFlow.OrderId)" $adminToken
    Assert-True ([int]$adminList.total -eq 1) "管理端订单列表未查到测试订单"
    $adminDetail = Invoke-GetApi "/admin-api/trade/order/detail?orderId=$($shipFlow.OrderId)" $adminToken
    Assert-True ($adminDetail.orderInfo.id -eq $shipFlow.OrderId) "管理端订单详情订单 ID 不匹配"
    Invoke-Api "/admin-api/trade/order/ship" @{
        orderId = $shipFlow.OrderId
        logisticsCompany = "shunfeng"
        logisticsCode = "shunfeng"
        logisticsNo = "SFTRADE$($shipFlow.OrderId)"
    } $adminToken | Out-Null
    Assert-Order $shipFlow.OrderId 2 1 "管理端发货后订单应为待收货"
    Assert-LogExists $shipFlow.OrderId "SHIP_ORDER"
    $logistics = Invoke-Api "/app-api/order/logistics" @{ orderId = $shipFlow.OrderId } $shipFlow.Token
    Assert-True ($logistics.hasLogistics -eq $true) "小程序端未查到物流信息"
    Invoke-Api "/app-api/order/confirmOrder" @{ orderId = $shipFlow.OrderId } $shipFlow.Token | Out-Null
    Assert-Order $shipFlow.OrderId 3 1 "确认收货后订单应为已完成"
    Assert-LogExists $shipFlow.OrderId "CONFIRM_RECEIPT"

    Write-Step "验收：管理端同意售后"
    $approveFlow = New-PaidOrder "approve"
    Invoke-Api "/app-api/order/refund/apply" @{ orderId = $approveFlow.OrderId; reason = "验收同意退款" } $approveFlow.Token | Out-Null
    Assert-Order $approveFlow.OrderId 5 1 "申请售后后订单应为退款中"
    $stockBeforeRefund = [int](Invoke-Sql "SELECT stock FROM product_sku WHERE id = $SkuId;")
    $afterSaleList = Invoke-GetApi "/admin-api/trade/after-sale/list?page=1&size=10&status=0&orderId=$($approveFlow.OrderId)" $adminToken
    Assert-True ([int]$afterSaleList.total -eq 1) "管理端售后列表未查到处理中售后单"
    $approveAfterSaleId = [long]$afterSaleList.list[0].id
    Invoke-Api "/admin-api/trade/after-sale/approve" @{ afterSaleId = $approveAfterSaleId } $adminToken | Out-Null
    Wait-RefundCompleted $approveFlow.OrderId
    Assert-Order $approveFlow.OrderId 5 2 "同意退款后订单支付状态应为已退款"
    Assert-AfterSale $approveFlow.OrderId 1 "同意退款后售后单应为已退款"
    Assert-LogExists $approveFlow.OrderId "REFUND_SUCCESS"
    Assert-True ([int](Invoke-Sql "SELECT status FROM pay_order WHERE order_id = $($approveFlow.OrderId);") -eq 3) "退款后支付单应为已退款"
    Assert-True ([int](Invoke-Sql "SELECT stock FROM product_sku WHERE id = $SkuId;") -eq ($stockBeforeRefund + 1)) "待发货退款后 SKU 库存未回补"
    $refundQuery = Invoke-Api "/app-api/pay/query" @{ orderId = $approveFlow.OrderId } $approveFlow.Token
    Assert-True ($refundQuery.orderStatus -eq "refunded") "退款后支付查询未返回 refunded"

    Write-Step "验收：管理端拒绝售后"
    $rejectFlow = New-PaidOrder "reject"
    Invoke-Api "/app-api/order/refund/apply" @{ orderId = $rejectFlow.OrderId; reason = "验收拒绝退款" } $rejectFlow.Token | Out-Null
    $rejectAfterSaleList = Invoke-GetApi "/admin-api/trade/after-sale/list?page=1&size=10&status=0&orderId=$($rejectFlow.OrderId)" $adminToken
    $rejectAfterSaleId = [long]$rejectAfterSaleList.list[0].id
    Invoke-Api "/admin-api/trade/after-sale/reject" @{ afterSaleId = $rejectAfterSaleId; rejectReason = "验收拒绝原因" } $adminToken | Out-Null
    Assert-Order $rejectFlow.OrderId 1 1 "拒绝售后后订单应恢复到待发货"
    Assert-AfterSale $rejectFlow.OrderId 2 "拒绝售后后售后单应为已拒绝"
    $rejectReasonLength = Invoke-Sql "SELECT CHAR_LENGTH(reject_reason) FROM trade_after_sale WHERE order_id = $($rejectFlow.OrderId) ORDER BY id DESC LIMIT 1;"
    Assert-True ([int]$rejectReasonLength -gt 0) "拒绝原因未保存"
    Assert-LogExists $rejectFlow.OrderId "REJECT_AFTER_SALE"

    Write-Step "验收：用户撤销售后"
    $cancelRefundFlow = New-PaidOrder "cancel_refund"
    Invoke-Api "/app-api/order/refund/apply" @{ orderId = $cancelRefundFlow.OrderId; reason = "验收撤销售后" } $cancelRefundFlow.Token | Out-Null
    Invoke-Api "/app-api/order/refund/cancel" @{ orderId = $cancelRefundFlow.OrderId } $cancelRefundFlow.Token | Out-Null
    Assert-Order $cancelRefundFlow.OrderId 1 1 "撤销售后后订单应恢复到待发货"
    Assert-AfterSale $cancelRefundFlow.OrderId 3 "撤销售后后售后单应为已撤销"
    Assert-LogExists $cancelRefundFlow.OrderId "CANCEL_AFTER_SALE"

    Write-Step "验收：待付款超时关闭与库存回补"
    $stockBeforeTimeout = [int](Invoke-Sql "SELECT stock FROM product_sku WHERE id = $SkuId;")
    $timeoutFlow = New-UnpaidOrder "timeout"
    $stockAfterSubmit = [int](Invoke-Sql "SELECT stock FROM product_sku WHERE id = $SkuId;")
    Assert-True ($stockAfterSubmit -eq ($stockBeforeTimeout - 1)) "待付款订单创建后库存未扣减"
    Invoke-Sql "UPDATE trade_order SET expire_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id = $($timeoutFlow.OrderId);" | Out-Null
    $deadline = (Get-Date).AddSeconds($TimeoutWaitSeconds)
    do {
        Start-Sleep -Seconds 5
        $currentStatus = Invoke-Sql "SELECT status FROM trade_order WHERE id = $($timeoutFlow.OrderId);"
        if ($currentStatus -eq "4") {
            break
        }
    } while ((Get-Date) -lt $deadline)
    Assert-Order $timeoutFlow.OrderId 4 0 "超时任务应关闭待付款订单"
    $stockAfterTimeout = [int](Invoke-Sql "SELECT stock FROM product_sku WHERE id = $SkuId;")
    Assert-True ($stockAfterTimeout -eq $stockBeforeTimeout) "超时关闭后库存未回补"
    Assert-LogExists $timeoutFlow.OrderId "SYSTEM_CLOSE"

    Write-Step "验收通过，开始清理测试数据"
    Cleanup-TestData
    Write-Host "交易闭环自动验收通过"
} catch {
    try {
        Cleanup-TestData
    } catch {
        Write-Warning "测试数据清理失败，请手动检查 $MysqlContainer.$MysqlDatabase"
    }
    Write-Error $_
    exit 1
}

param(
    [string]$BaseUrl = "http://127.0.0.1:8085",
    [string]$MysqlContainer = "shop-mysql",
    [string]$RedisContainer = "shop-redis"
)

$ErrorActionPreference = "Stop"
$ProductId = 240001
$LoginCode = "issue25_product_flow"
$CommentText = "issue25-product-comment"
$Token = $null
$UserId = $null

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-Api([string]$Path, [hashtable]$Body = @{}, [string]$AuthToken = $null, [switch]$Json) {
    $headers = @{}
    if ($AuthToken) { $headers["Authorization"] = "Bearer $AuthToken" }
    $parameters = @{
        Uri = "$BaseUrl$Path"
        Method = "Post"
        Headers = $headers
        TimeoutSec = 10
    }
    if ($Json) {
        $parameters["ContentType"] = "application/json"
        $parameters["Body"] = $Body | ConvertTo-Json
    } else {
        $parameters["Body"] = $Body
    }
    return Invoke-RestMethod @parameters
}

function Invoke-Sql([string]$Sql) {
    $result = docker exec $MysqlContainer mysql -N -B -uroot -proot shop -e $Sql
    if ($LASTEXITCODE -ne 0) { throw "数据库验收命令执行失败" }
    return $result
}

function Wait-Backend {
    foreach ($attempt in 1..30) {
        try {
            if ((Invoke-Api "/app-api/goods/count").code -eq 0) { return }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "后端未在规定时间内就绪"
}

try {
    Wait-Backend

    $banner = Invoke-Api "/app-api/index/banner"
    $channel = Invoke-Api "/app-api/index/channel"
    $categoryFloor = Invoke-Api "/app-api/index/category"
    $catalog = Invoke-Api "/app-api/catalog/index"
    Assert-True (@($banner.data.banner).Count -gt 0) "首页 Banner 为空"
    Assert-True (@($channel.data.channel).Count -gt 0) "首页频道为空"
    Assert-True (@($categoryFloor.data.categoryList).Count -gt 0) "首页分类楼层为空"
    Assert-True (@($catalog.data.categoryList).Count -gt 0) "正式分类为空"

    $list = Invoke-Api "/app-api/goods/list" @{ categoryId = 240101; page = 1; size = 20 }
    Assert-True ($list.data.goodsList.total -eq 3) "一级分类商品聚合异常"
    $suggestions = Invoke-Api "/app-api/search/helper" @{ keyword = "玫瑰" }
    Assert-True (@($suggestions.data).Count -ge 1) "正式搜索联想为空"
    $emptySearch = Invoke-Api "/app-api/search/helper" @{ keyword = "issue25-no-result" }
    Assert-True (@($emptySearch.data).Count -eq 0) "无结果搜索未返回空态"

    $detail = Invoke-Api "/app-api/goods/detail" @{ id = $ProductId }
    Assert-True (@($detail.data.specificationList).Count -eq 2) "商品规格维度异常"
    Assert-True (@($detail.data.productList).Count -eq 4) "商品 SKU 矩阵异常"
    Assert-True (@($detail.data.productList | Where-Object available -eq $true).Count -eq 3) "可售 SKU 数量异常"
    Assert-True (@($detail.data.productList | Where-Object available -eq $false).Count -eq 1) "缺货 SKU 数量异常"

    $anonymousCollect = Invoke-Api "/app-api/collect/list"
    Assert-True ($anonymousCollect.code -eq 401) "未登录收藏接口未受保护"

    $login = Invoke-Api "/app-api/auth/LoginByMa" @{ code = $LoginCode } -Json
    Assert-True ($login.code -eq 0) "Mock 微信登录失败"
    $Token = $login.data.token
    $UserId = [long]$login.data.userId

    $collect = Invoke-Api "/app-api/collect/addordelete" @{ typeId = 0; valueId = $ProductId } $Token
    Assert-True ($collect.code -eq 0 -and $collect.data.type -eq "add") "收藏商品失败"
    $collectList = Invoke-Api "/app-api/collect/list" @{} $Token
    Assert-True (@($collectList.data.valueId) -contains $ProductId) "收藏列表缺少目标商品"

    $footprint = Invoke-Api "/app-api/footprint/record" @{ goodsId = $ProductId } $Token
    Assert-True ($footprint.code -eq 0) "记录足迹失败"
    $footprintList = Invoke-Api "/app-api/footprint/list" @{} $Token
    Assert-True (@($footprintList.data.data | ForEach-Object { $_.goodsId }) -contains $ProductId) "足迹列表缺少目标商品"

    $beforeCount = (Invoke-Api "/app-api/comment/count" @{ valueId = $ProductId }).data.allCount
    $post = Invoke-Api "/app-api/comment/post" @{ typeId = 0; valueId = $ProductId; content = $CommentText } $Token
    Assert-True ($post.code -eq 0) "发表评论失败"
    $comments = Invoke-Api "/app-api/comment/list" @{ typeId = 0; valueId = $ProductId; page = 1; size = 20 }
    $afterCount = (Invoke-Api "/app-api/comment/count" @{ valueId = $ProductId }).data.allCount
    Assert-True (@($comments.data.records.content) -contains $CommentText) "评论列表缺少新评论"
    Assert-True ($afterCount -eq ($beforeCount + 1)) "评论数量未正确增加"

    Write-Output "MINIAPP_PRODUCT_FLOW_ACCEPTANCE_OK"
} finally {
    if ($UserId) {
        Invoke-Sql "DELETE FROM product_comment WHERE user_id=$UserId AND content='$CommentText'; DELETE FROM member_collect WHERE user_id=$UserId; DELETE FROM member_footprint WHERE user_id=$UserId; DELETE FROM member_user WHERE id=$UserId;" | Out-Null
    }
    if ($Token) {
        docker exec $RedisContainer redis-cli DEL "shop:token:$Token" | Out-Null
    }
}

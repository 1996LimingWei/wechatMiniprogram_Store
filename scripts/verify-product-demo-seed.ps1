param(
    [string]$BaseUrl = "http://127.0.0.1:8086",
    [string]$Database = "shop",
    [string]$MysqlContainer = "shop-mysql"
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-Api([string]$Path, [hashtable]$Body = @{}) {
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Body $Body -TimeoutSec 10
}

function Invoke-Sql([string]$Sql) {
    $result = docker exec $MysqlContainer mysql -N -B -uroot -proot $Database -e $Sql
    if ($LASTEXITCODE -ne 0) { throw "SQL 执行失败" }
    return $result
}

function ConvertFrom-Utf8Mojibake([string]$Value) {
    if ($Value -match '[\p{IsCJKUnifiedIdeographs}]') { return $Value }
    return [System.Text.Encoding]::UTF8.GetString([System.Text.Encoding]::GetEncoding(28591).GetBytes($Value))
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

& "$PSScriptRoot/migrate-db.ps1" -Database $Database -MysqlContainer $MysqlContainer
if ($LASTEXITCODE -ne 0) { throw "演示种子迁移失败" }
$signatureBefore = Invoke-Sql "SELECT CONCAT((SELECT COUNT(*) FROM product_category WHERE id BETWEEN 240101 AND 240131),':',(SELECT COUNT(*) FROM product_spu WHERE id BETWEEN 240001 AND 240006),':',(SELECT COUNT(*) FROM product_sku WHERE id BETWEEN 240011 AND 240061),':',(SELECT COUNT(*) FROM content_banner WHERE id BETWEEN 240201 AND 240202),':',(SELECT COUNT(*) FROM product_comment WHERE id=240401));"
& "$PSScriptRoot/migrate-db.ps1" -Database $Database -MysqlContainer $MysqlContainer
if ($LASTEXITCODE -ne 0) { throw "重复迁移失败" }
$signatureAfter = Invoke-Sql "SELECT CONCAT((SELECT COUNT(*) FROM product_category WHERE id BETWEEN 240101 AND 240131),':',(SELECT COUNT(*) FROM product_spu WHERE id BETWEEN 240001 AND 240006),':',(SELECT COUNT(*) FROM product_sku WHERE id BETWEEN 240011 AND 240061),':',(SELECT COUNT(*) FROM content_banner WHERE id BETWEEN 240201 AND 240202),':',(SELECT COUNT(*) FROM product_comment WHERE id=240401));"
Assert-True ($signatureBefore -eq "7:6:10:2:1" -and $signatureAfter -eq $signatureBefore) "演示种子数量或迁移幂等性异常"

Wait-Backend
$banner = Invoke-Api "/app-api/index/banner"
$channel = Invoke-Api "/app-api/index/channel"
$brand = Invoke-Api "/app-api/index/brand"
$topic = Invoke-Api "/app-api/index/topic"
Assert-True (@($banner.data.banner.id) -contains 240201) "首页缺少演示 Banner"
Assert-True (@($channel.data.channel.id) -contains 240211) "首页缺少演示频道"
Assert-True (@($brand.data.brandList.id) -contains 240221) "首页缺少演示品牌"
Assert-True (@($topic.data.topicList.id) -contains 240231) "首页缺少演示专题"

$categoryList = Invoke-Api "/app-api/goods/list" @{ categoryId = 240101; page = 1; size = 20 }
Assert-True ($categoryList.data.goodsList.total -eq 3) "一级分类未聚合演示子分类商品"
$search = Invoke-Api "/app-api/search/helper" @{ keyword = "玫瑰" }
Assert-True (@($search.data | ForEach-Object { ConvertFrom-Utf8Mojibake $_ }) -contains "玫瑰阿胶糕礼盒") "搜索联想缺少多规格商品"
$hidden = Invoke-Api "/app-api/search/helper" @{ keyword = "桂圆红枣" }
Assert-True (@($hidden.data).Count -eq 0) "下架商品不应出现在搜索联想"

$detail = Invoke-Api "/app-api/goods/detail" @{ id = 240001 }
Assert-True (@($detail.data.specificationList).Count -eq 2) "多规格维度数量异常"
Assert-True (@($detail.data.productList).Count -eq 4) "SKU 矩阵数量异常"
Assert-True (@($detail.data.productList | Where-Object available -eq $true).Count -eq 3) "可售组合数量异常"
Assert-True (@($detail.data.productList | Where-Object available -eq $false).Count -eq 1) "缺货组合数量异常"
Assert-True (@($detail.data.productList.retailPrice | Select-Object -Unique).Count -eq 4) "SKU 差异价格异常"
Assert-True (@($detail.data.productList.picUrl | Select-Object -Unique).Count -eq 2) "SKU 差异图片异常"
Assert-True ($detail.data.comment.count -ge 1) "演示评论缺失"

$soldOut = Invoke-Api "/app-api/goods/detail" @{ id = 240002 }
Assert-True (@($soldOut.data.productList | Where-Object available -eq $true).Count -eq 0) "全部缺货商品出现可售 SKU"

Write-Output "PRODUCT_DEMO_SEED_ACCEPTANCE_OK"

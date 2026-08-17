param(
    [string]$BaseUrl = "http://127.0.0.1:8086"
)

$ErrorActionPreference = "Stop"
$ProductId = 9900001501
$SkuIds = @(9900001511, 9900001512, 9900001513, 9900001514)

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-Sql([string]$Sql) {
    $output = docker exec shop-mysql mysql -N -uroot -proot shop -e $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed"
    }
    return $output
}

function Invoke-Api([string]$Path, [hashtable]$Body = @{}) {
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Body $Body -TimeoutSec 10
}

function Wait-Backend {
    foreach ($attempt in 1..30) {
        try {
            $response = Invoke-Api "/app-api/goods/detail" @{ id = $ProductId }
            if ($response.code -eq 0) {
                return $response
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Backend did not become ready"
}

try {
    Invoke-Sql "DELETE FROM product_sku WHERE id IN ($($SkuIds -join ',')); DELETE FROM product_spu WHERE id=$ProductId; INSERT INTO product_spu(id,category_id,name,keyword,introduction,description,pic_url,price,market_price,stock,sales_count,sort,status,deleted) VALUES ($ProductId,5,'issue15-product','issue15','sku acceptance','','https://example.com/spu.png',1200,1600,8,0,1,1,b'0'); INSERT INTO product_sku(id,spu_id,properties,price,market_price,stock,pic_url) VALUES (9900001511,$ProductId,JSON_ARRAY(JSON_OBJECT('id',10,'name','Size','valueId',101,'valueName','Small'),JSON_OBJECT('id',20,'name','Color','valueId',201,'valueName','Red')),1200,1600,3,'https://example.com/red-small.png'),(9900001512,$ProductId,JSON_ARRAY(JSON_OBJECT('id',10,'name','Size','valueId',101,'valueName','Small'),JSON_OBJECT('id',20,'name','Color','valueId',202,'valueName','Blue')),1300,1700,0,''),(9900001513,$ProductId,JSON_ARRAY(JSON_OBJECT('id',10,'name','Size','valueId',102,'valueName','Large'),JSON_OBJECT('id',20,'name','Color','valueId',202,'valueName','Blue')),1500,1900,5,'https://example.com/blue-large.png'),(9900001514,$ProductId,'not-json',9999,9999,1,'');" | Out-Null

    $response = Wait-Backend
    Assert-True ($response.code -eq 0) "Detail request failed"
    Assert-True (@($response.data.specificationList).Count -eq 2) "Specification dimension count invalid"
    Assert-True ($response.data.specificationList[0].specificationId -eq 10) "Specification order invalid"
    Assert-True ((@($response.data.specificationList[0].valueList.id) -join ',') -eq '101,102') "Size values invalid"
    Assert-True ((@($response.data.specificationList[1].valueList.id) -join ',') -eq '201,202') "Color values invalid"
    Assert-True ((@($response.data.productList.id) -join ',') -eq '9900001514,9900001511,9900001512,9900001513') "SKU order invalid"

    $inStock = $response.data.productList | Where-Object id -eq 9900001511
    Assert-True ($inStock.available -eq $true -and $inStock.stock -eq 3) "In-stock SKU availability invalid"
    Assert-True ($inStock.goodsSpecificationIds -eq '101_201') "Compatibility field invalid"
    Assert-True ($inStock.retailPrice -eq '12.00' -and $inStock.picUrl -eq 'https://example.com/red-small.png') "SKU price or image invalid"

    $soldOut = $response.data.productList | Where-Object id -eq 9900001512
    Assert-True ($soldOut.available -eq $false -and $soldOut.goodsNumber -eq 0) "Sold-out SKU availability invalid"

    $malformed = $response.data.productList | Where-Object id -eq 9900001514
    Assert-True (@($malformed.properties).Count -eq 0 -and @($malformed.specificationValueIds).Count -eq 0) "Malformed SKU fallback invalid"

    Write-Output "PRODUCT_SKU_ACCEPTANCE_OK"
} finally {
    Invoke-Sql "DELETE FROM product_sku WHERE id IN ($($SkuIds -join ',')); DELETE FROM product_spu WHERE id=$ProductId;" | Out-Null
}

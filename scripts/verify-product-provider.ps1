param(
    [string]$MockBaseUrl = "http://127.0.0.1:8086",
    [string]$DatabaseBaseUrl = "http://127.0.0.1:8087",
    [string]$ProdBaseUrl = "http://127.0.0.1:8088"
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Invoke-Api([string]$BaseUrl, [string]$Path, [hashtable]$Body = @{}) {
    return Invoke-RestMethod -Uri "$BaseUrl$Path" -Method Post -Body $Body -TimeoutSec 10
}

function Wait-Backend([string]$BaseUrl) {
    foreach ($attempt in 1..30) {
        try {
            $response = Invoke-Api $BaseUrl "/app-api/goods/count"
            if ($response.code -eq 0) { return }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Backend did not become ready: $BaseUrl"
}

Wait-Backend $MockBaseUrl
Wait-Backend $DatabaseBaseUrl
Wait-Backend $ProdBaseUrl

$mockCount = Invoke-Api $MockBaseUrl "/app-api/goods/count"
Assert-True ($mockCount.data.goodsCount -eq 13) "Mock provider was not selected"
$mockList = Invoke-Api $MockBaseUrl "/app-api/goods/list" @{ categoryId = 1; page = 1; size = 10 }
Assert-True ($mockList.data.goodsList.total -eq 5) "Mock category filter invalid"
$mockDetail = Invoke-Api $MockBaseUrl "/app-api/goods/detail" @{ id = 1 }
Assert-True ($mockDetail.data.productList[0].id -eq 1001) "Mock SKU contract invalid"
Assert-True ($mockDetail.data.productList[0].available -eq $true) "Mock availability invalid"
$mockCompatibility = Invoke-Api $MockBaseUrl "/app-api/mock/goods/count"
Assert-True ($mockCompatibility.code -eq 0) "Development Mock compatibility endpoint unavailable"

$databaseCount = Invoke-Api $DatabaseBaseUrl "/app-api/goods/count"
Assert-True ($databaseCount.data.goodsCount -gt 0) "Database provider was not selected"
$databaseList = Invoke-Api $DatabaseBaseUrl "/app-api/goods/list" @{ page = 1; size = 1 }
$databaseId = $databaseList.data.goodsList.records[0].id
$databaseDetail = Invoke-Api $DatabaseBaseUrl "/app-api/goods/detail" @{ id = $databaseId }
Assert-True (@($databaseDetail.data.productList).Count -gt 0) "Database detail contract invalid"
$brandList = Invoke-Api $DatabaseBaseUrl "/app-api/brand/list"
$topicList = Invoke-Api $DatabaseBaseUrl "/app-api/topic/list"
$hotBanner = Invoke-Api $DatabaseBaseUrl "/app-api/goods/hot"
Assert-True ($brandList.code -eq 0 -and $topicList.code -eq 0 -and $hotBanner.code -eq 0) "Formal content endpoints invalid"

$prodMock = Invoke-Api $ProdBaseUrl "/app-api/mock/goods/count"
Assert-True ($prodMock.code -eq 403) "Production profile exposed Mock compatibility endpoint"
$prodFormal = Invoke-Api $ProdBaseUrl "/app-api/goods/count"
Assert-True ($prodFormal.code -eq 0 -and $prodFormal.data.goodsCount -gt 0) "Production formal endpoint invalid"

Write-Output "PRODUCT_PROVIDER_ACCEPTANCE_OK"

param(
    [string]$BaseUrl = "http://127.0.0.1:8085"
)

$ErrorActionPreference = "Stop"
$ProductId = 9900002201
$HiddenProductId = 9900002202
$CodeA = "issue22_user_a"
$CodeB = "issue22_user_b"
$TokenA = $null
$TokenB = $null

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

function Invoke-Api([string]$Path, [hashtable]$Body = @{}, [string]$Token = $null, [switch]$Json) {
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $params = @{
        Uri = "$BaseUrl$Path"
        Method = "Post"
        Headers = $headers
        TimeoutSec = 10
    }
    if ($Json) {
        $params["ContentType"] = "application/json"
        $params["Body"] = $Body | ConvertTo-Json
    } else {
        $params["Body"] = $Body
    }
    return Invoke-RestMethod @params
}

function Wait-Backend {
    foreach ($attempt in 1..30) {
        try {
            $response = Invoke-Api "/app-api/search/index"
            if ($response.code -eq 0) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "Backend did not become ready"
}

try {
    Invoke-Sql "DELETE FROM product_spu WHERE id IN ($ProductId,$HiddenProductId); INSERT INTO product_spu(id,category_id,name,keyword,introduction,description,pic_url,price,market_price,stock,sales_count,sort,status,deleted) VALUES ($ProductId,5,'issue22-active-product','issue22','search acceptance','','https://example.com/active.png',100,100,10,1,1,1,b'0'),($HiddenProductId,5,'issue22-hidden-product','hidden22','search acceptance','','https://example.com/hidden.png',100,100,10,1,1,0,b'0');" | Out-Null

    Wait-Backend

    $anonymous = Invoke-Api "/app-api/search/index"
    Assert-True ($anonymous.code -eq 0) "Anonymous search index failed"
    Assert-True (@($anonymous.data.historyKeywordList).Count -eq 0) "Anonymous history must be empty"
    Assert-True (@($anonymous.data.hotKeywordList).Count -gt 0) "Hot keywords must come from products"

    $activeSuggestions = Invoke-Api "/app-api/search/helper" @{ keyword = "issue22" }
    Assert-True (@($activeSuggestions.data) -contains "issue22-active-product") "Active product suggestion missing"
    $hiddenSuggestions = Invoke-Api "/app-api/search/helper" @{ keyword = "hidden22" }
    Assert-True (@($hiddenSuggestions.data).Count -eq 0) "Inactive product must not be suggested"
    $blankSuggestions = Invoke-Api "/app-api/search/helper" @{ keyword = "" }
    Assert-True (@($blankSuggestions.data).Count -eq 0) "Blank keyword must return empty suggestions"

    $loginA = Invoke-Api "/app-api/auth/LoginByMa" @{ code = $CodeA } -Json
    $loginB = Invoke-Api "/app-api/auth/LoginByMa" @{ code = $CodeB } -Json
    Assert-True ($loginA.code -eq 0 -and $loginB.code -eq 0) "Mock login failed"
    $TokenA = $loginA.data.token
    $TokenB = $loginB.data.token

    Invoke-Api "/app-api/goods/list" @{ keyword = "issue22"; page = 1; size = 10 } $TokenA | Out-Null
    Invoke-Api "/app-api/goods/list" @{ keyword = "issue22"; page = 1; size = 10 } $TokenA | Out-Null
    $historyCount = Invoke-Sql "SELECT COUNT(*) FROM product_search_history h JOIN member_user u ON u.id=h.user_id WHERE u.openid='dev_openid_$CodeA' AND h.keyword='issue22' AND h.deleted=b'0';"
    Assert-True ([int]$historyCount -eq 1) "Repeated keyword must be idempotent"

    $indexA = Invoke-Api "/app-api/search/index" @{} $TokenA
    $indexB = Invoke-Api "/app-api/search/index" @{} $TokenB
    Assert-True (@($indexA.data.historyKeywordList) -contains "issue22") "User A history missing"
    Assert-True (@($indexB.data.historyKeywordList).Count -eq 0) "User B must not see user A history"

    Invoke-Api "/app-api/goods/list" @{ keyword = "hidden22"; page = 1; size = 10 } $TokenB | Out-Null
    $beforeClearB = Invoke-Api "/app-api/search/index" @{} $TokenB
    Assert-True (@($beforeClearB.data.historyKeywordList) -contains "hidden22") "User B history missing"

    $clearA = Invoke-Api "/app-api/search/clearhistory" @{} $TokenA
    Assert-True ($clearA.data.cleared -eq 1) "User A clear count invalid"
    $afterClearA = Invoke-Api "/app-api/search/index" @{} $TokenA
    $afterClearB = Invoke-Api "/app-api/search/index" @{} $TokenB
    Assert-True (@($afterClearA.data.historyKeywordList).Count -eq 0) "User A history was not cleared"
    Assert-True (@($afterClearB.data.historyKeywordList) -contains "hidden22") "User A clear affected user B"

    Write-Output "PRODUCT_SEARCH_ACCEPTANCE_OK"
} finally {
    if ($TokenA) {
        try { Invoke-Api "/app-api/auth/logout" @{} $TokenA | Out-Null } catch {}
    }
    if ($TokenB) {
        try { Invoke-Api "/app-api/auth/logout" @{} $TokenB | Out-Null } catch {}
    }
    Invoke-Sql "DELETE h FROM product_search_history h JOIN member_user u ON u.id=h.user_id WHERE u.openid IN ('dev_openid_$CodeA','dev_openid_$CodeB'); DELETE FROM member_user WHERE openid IN ('dev_openid_$CodeA','dev_openid_$CodeB'); DELETE FROM product_spu WHERE id IN ($ProductId,$HiddenProductId);" | Out-Null
}

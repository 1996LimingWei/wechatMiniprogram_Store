param(
    [string]$MiniappRoot = (Join-Path (Split-Path -Parent $PSScriptRoot) "shop-miniapp")
)

$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw "小程序提审准备校验失败：$Message"
    }
}

function Read-JsonFile([string]$Path) {
    Assert-True (Test-Path $Path) "文件不存在：$Path"
    return Get-Content $Path -Raw -Encoding utf8 | ConvertFrom-Json
}

$pagesJsonPath = Join-Path $MiniappRoot "pages.json"
$manifestPath = Join-Path $MiniappRoot "manifest.json"
$projectConfigPath = Join-Path $MiniappRoot "project.config.json"
$pagesJson = Read-JsonFile $pagesJsonPath
$manifest = Read-JsonFile $manifestPath
$projectConfig = Read-JsonFile $projectConfigPath

$pagePaths = @($pagesJson.pages | ForEach-Object { $_.path })
$requiredPages = @(
    "pages/legal/privacy/privacy",
    "pages/legal/agreement/agreement",
    "pages/legal/afterSale/afterSale",
    "pages/ucenter/settings/settings"
)
foreach ($page in $requiredPages) {
    Assert-True ($pagePaths -contains $page) "缺少页面注册：$page"
    Assert-True (Test-Path (Join-Path $MiniappRoot ($page + ".vue"))) "缺少页面文件：$page.vue"
}

Assert-True ($manifest.'mp-weixin'.'__usePrivacyCheck__' -eq $true) "manifest.json 必须开启微信隐私检查"
Assert-True (-not [string]::IsNullOrWhiteSpace($manifest.'mp-weixin'.appid)) "manifest.json 必须配置小程序 AppID"
Assert-True ($projectConfig.setting.urlCheck -eq $true) "project.config.json 必须开启合法域名校验"
Assert-True (-not [string]::IsNullOrWhiteSpace($projectConfig.appid)) "project.config.json 必须配置小程序 AppID"

$settingsText = Get-Content (Join-Path $MiniappRoot "pages/ucenter/settings/settings.vue") -Raw -Encoding utf8
$ucenterText = Get-Content (Join-Path $MiniappRoot "pages/ucenter/index/index.vue") -Raw -Encoding utf8
$utilText = Get-Content (Join-Path $MiniappRoot "utils/util.js") -Raw -Encoding utf8

Assert-True ($settingsText.Contains('open-type="contact"') -or $ucenterText.Contains('open-type="contact"')) "必须存在微信在线客服入口"
Assert-True ($settingsText.Contains('/pages/legal/afterSale/afterSale') -and $ucenterText.Contains('/pages/legal/afterSale/afterSale')) "售后政策必须在账号设置和我的页面可达"
Assert-True ($utilText.Contains("requestPayment:mock-disabled")) "生产环境必须阻止模拟支付"
Assert-True (-not $utilText.Contains("开发环境模拟支付")) "正式包不得包含模拟支付提示文案"

$registeredVueFiles = @($pagePaths | ForEach-Object { Join-Path $MiniappRoot ($_ + ".vue") })
$scanFiles = $registeredVueFiles + @(
    (Join-Path $MiniappRoot "App.vue"),
    (Join-Path $MiniappRoot "main.js"),
    (Join-Path $MiniappRoot "utils/util.js"),
    (Join-Path $MiniappRoot "components/uParse/src/wxParse.vue")
)
$bannedPatterns = @(
    "console\.log",
    "敬请期待",
    "开发中",
    "开发环境模拟支付",
    "当前为体验模式"
)
foreach ($file in $scanFiles) {
    $text = Get-Content $file -Raw -Encoding utf8
    foreach ($pattern in $bannedPatterns) {
        if ($text -match $pattern) {
            throw "小程序提审准备校验失败：$file 包含不应进入提审包的内容：$pattern"
        }
    }
}

Write-Host "小程序提审准备校验通过：页面入口、隐私配置、客服入口、模拟支付防线和调试输出均符合要求。"

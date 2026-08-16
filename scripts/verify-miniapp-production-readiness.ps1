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

function Assert-NotContains([string]$Path, [string]$Pattern, [string]$Message) {
    $content = Get-Content $Path -Raw -Encoding utf8
    if ($content -match $Pattern) {
        throw $Message
    }
}

$env = Join-Path $root "shop-miniapp/config/env.js"
$util = Join-Path $root "shop-miniapp/utils/util.js"
$app = Join-Path $root "shop-miniapp/App.vue"
$goldCard = Join-Path $root "shop-miniapp/pages/ucenter/goldCard/goldCard.vue"
$member = Join-Path $root "shop-miniapp/pages/ucenter/member/member.vue"

foreach ($file in @($env, $util, $app, $goldCard, $member)) {
    Require-File $file
}

Assert-Contains $env 'VUE_APP_ENV|UNI_APP_ENV' "小程序必须支持环境名区分"
Assert-Contains $env 'VUE_APP_STAGING_API_BASE_URL' "小程序必须支持体验版/staging API 地址"
Assert-Contains $env 'VUE_APP_PROD_API_BASE_URL' "小程序必须支持正式版 API 地址"
Assert-Contains $env '体验版和正式环境 API 地址必须使用 HTTPS' "小程序体验版和正式版必须强制 HTTPS"
Assert-Contains $util 'safeMessage' "小程序错误提示必须经过脱敏过滤"
foreach ($keyword in @("/admin-api", "/app-api", "exception", "stack", "trace", "sql", "jdbc", "token", "authorization", "password", "secret")) {
    Assert-Contains $util ([regex]::Escape($keyword)) "小程序错误脱敏必须覆盖：$keyword"
}

Assert-NotContains $app 'console\.(log|debug|error|warn)' "App.vue 不得输出调试日志"
Assert-NotContains $goldCard 'console\.(log|debug|error|warn)|体验模式|测试|Mock' "黄金会员页不得保留调试输出或测试文案"
Assert-NotContains $member 'console\.(log|debug|error|warn)' "会员中心页不得输出调试日志"

$sourceFiles = Get-ChildItem -Path (Join-Path $root "shop-miniapp") -Recurse -Include *.vue,*.js |
    Where-Object {
        $_.FullName -notmatch '\\node_modules\\|\\unpackage\\|\\components\\uParse\\|\\static\\'
    }
foreach ($file in $sourceFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding utf8
    if ($content -match 'console\.(log|debug)') {
        throw "小程序源码不得保留 console.log/debug：$($file.FullName)"
    }
}

Write-Host "小程序生产就绪校验通过。"

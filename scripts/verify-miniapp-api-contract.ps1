$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$miniappRoot = Join-Path $root "shop-miniapp"
$apiFile = Join-Path $miniappRoot "utils\api.js"
$pagesFile = Join-Path $miniappRoot "pages.json"
$backendRoot = Join-Path $root "shop-backend"

function Normalize-Path([string]$Path) {
    return "/" + $Path.Trim().Trim("/")
}

$apiDeclarations = @{}
foreach ($match in [regex]::Matches((Get-Content $apiFile -Raw -Encoding utf8), "(?m)^\s*(?<name>[A-Za-z][A-Za-z0-9_]*)\s*:\s*'(?<path>[^']+)'") ) {
    $apiDeclarations[$match.Groups["name"].Value] = $match.Groups["path"].Value
}

$pageConfig = Get-Content $pagesFile -Raw -Encoding utf8 | ConvertFrom-Json
$usedApiNames = [System.Collections.Generic.HashSet[string]]::new()
foreach ($page in $pageConfig.pages) {
    $pageFile = Join-Path $miniappRoot ($page.path.Replace("/", "\") + ".vue")
    if (-not (Test-Path $pageFile)) {
        throw "已注册页面不存在：$($page.path)"
    }
    $content = Get-Content $pageFile -Raw -Encoding utf8
    foreach ($match in [regex]::Matches($content, "api\.(?<name>(?!js\b)[A-Za-z][A-Za-z0-9_]*)")) {
        [void]$usedApiNames.Add($match.Groups["name"].Value)
    }
}

$backendRoutes = [System.Collections.Generic.HashSet[string]]::new()
Get-ChildItem $backendRoot -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding utf8
    $paths = [regex]::Matches($content, '@(?:Get|Post|Put|Delete|Request)Mapping\("(?<path>[^"]+)"\)') |
        ForEach-Object { $_.Groups["path"].Value }
    $prefixes = @($paths | Where-Object { $_ -like "/app-api*" })
    foreach ($path in $prefixes) {
        [void]$backendRoutes.Add((Normalize-Path $path))
    }
    foreach ($prefix in $prefixes) {
        foreach ($path in $paths | Where-Object { $_.StartsWith("/") -and -not $_.StartsWith("/app-api") }) {
            [void]$backendRoutes.Add((Normalize-Path ($prefix.TrimEnd("/") + $path)))
        }
    }
}

$missingDeclarations = @()
$missingRoutes = @()
foreach ($name in $usedApiNames | Sort-Object) {
    if (-not $apiDeclarations.ContainsKey($name)) {
        $missingDeclarations += $name
        continue
    }
    $route = Normalize-Path ("/app-api/" + $apiDeclarations[$name])
    if (-not $backendRoutes.Contains($route)) {
        $missingRoutes += "$name -> $route"
    }
}

if ($missingDeclarations.Count -gt 0) {
    throw "小程序存在未声明 API：$($missingDeclarations -join ', ')"
}
if ($missingRoutes.Count -gt 0) {
    throw "小程序 API 缺少后端路由：$($missingRoutes -join '; ')"
}

Write-Host "小程序 API 契约校验通过：$($usedApiNames.Count) 个已使用 API，$($backendRoutes.Count) 条后端 app-api 路由。"

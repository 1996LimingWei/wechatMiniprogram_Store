param(
    [string]$BaselineFile = (Join-Path $PSScriptRoot "api-contract-baseline.json"),
    [switch]$Strict
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $root "shop-backend"

if (-not (Test-Path $backendRoot)) {
    throw "后端目录不存在：$backendRoot"
}

$counts = [ordered]@{
    requestMapping = 0
    mapSignature = 0
    doSignature = 0
}

$details = New-Object System.Collections.Generic.List[string]
$controllers = Get-ChildItem $backendRoot -Recurse -Filter "*Controller.java"
foreach ($file in $controllers) {
    $relativePath = Resolve-Path -Relative $file.FullName
    $lineNumber = 0
    foreach ($line in Get-Content $file.FullName -Encoding utf8) {
        $lineNumber += 1
        $trimmed = $line.Trim()
        if ($trimmed -match '@\s*RequestMapping\(') {
            $counts.requestMapping += 1
            $details.Add("requestMapping|${relativePath}:$lineNumber|$trimmed")
        }
        if ($trimmed -match 'public\s+.*CommonResult<.*Map<' -or
            $trimmed -match '@Request(?:Body|Param)[^\r\n]*Map<') {
            $counts.mapSignature += 1
            $details.Add("mapSignature|${relativePath}:$lineNumber|$trimmed")
        }
        if ($trimmed -match 'public\s+.*CommonResult<.*\b[A-Za-z0-9]+DO\b' -or
            $trimmed -match '@RequestBody[^\r\n]*\b[A-Za-z0-9]+DO\b') {
            $counts.doSignature += 1
            $details.Add("doSignature|${relativePath}:$lineNumber|$trimmed")
        }
    }
}

if ($Strict) {
    $violations = @()
    foreach ($key in $counts.Keys) {
        if ($counts[$key] -gt 0) {
            $violations += "$key=$($counts[$key])"
        }
    }
    if ($violations.Count -gt 0) {
        throw "严格 API 契约校验失败，仍存在历史债务：$($violations -join ', ')"
    }
}

if (-not (Test-Path $BaselineFile)) {
    throw "API 契约基线文件不存在：$BaselineFile"
}

$baseline = Get-Content $BaselineFile -Raw -Encoding utf8 | ConvertFrom-Json
$exceeded = @()
foreach ($key in $counts.Keys) {
    $limit = [int]$baseline.$key
    if ($counts[$key] -gt $limit) {
        $exceeded += "$key 当前 $($counts[$key])，基线 $limit"
    }
}

if ($exceeded.Count -gt 0) {
    $sample = ($details | Select-Object -First 20) -join "`n"
    throw "API 契约债务增加：$($exceeded -join '; ')`n前 20 条明细：`n$sample"
}

Write-Host "后端 API 契约基线校验通过：RequestMapping=$($counts.requestMapping)，Map签名=$($counts.mapSignature)，DO签名=$($counts.doSignature)。"

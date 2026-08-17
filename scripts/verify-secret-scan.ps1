param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"

$excludedDirectories = @(
    ".git",
    "node_modules",
    "dist",
    "target",
    "unpackage",
    ".docker-data",
    ".runtime-logs",
    "logs"
)

$allowedExtensions = @(
    ".java", ".yml", ".yaml", ".properties", ".xml", ".json", ".js", ".ts", ".vue",
    ".ps1", ".md", ".sql", ".env", ".example", ".template", ".conf", ".dockerignore",
    ".gitignore", ".gitattributes", ".txt"
)

function Test-ExcludedPath([string]$Path) {
    $normalized = $Path.Replace("\", "/")
    foreach ($directory in $excludedDirectories) {
        if ($normalized -match "(^|/)$([regex]::Escape($directory))(/|$)") {
            return $true
        }
    }
    return $false
}

$errors = New-Object System.Collections.Generic.List[string]
$disallowedFileNamePatterns = @(
    '(^|[\\/])application-local\.ya?ml$',
    '(^|[\\/])\.env\.prod$',
    '(^|[\\/])\.env\.production\.local$',
    '(^|[\\/])\.env\.staging\.local$',
    '\.(pem|p12|pfx|jks)$'
)

Get-ChildItem $Root -Recurse -File -Force | Where-Object { -not (Test-ExcludedPath $_.FullName) } | ForEach-Object {
    $relativePath = Resolve-Path -Relative $_.FullName
    $normalized = $relativePath.Replace("\", "/")
    foreach ($pattern in $disallowedFileNamePatterns) {
        if ($normalized -match $pattern) {
            $errors.Add("禁止提交高风险文件：$relativePath")
        }
    }

    $extension = $_.Extension.ToLowerInvariant()
    if ($allowedExtensions -notcontains $extension -and $_.Name -notmatch '^\.env(\..+)?$') {
        return
    }

    $lineNumber = 0
    foreach ($line in Get-Content $_.FullName -Encoding utf8 -ErrorAction SilentlyContinue) {
        $lineNumber += 1
        if ($line -match '^\s*-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----') {
            $errors.Add("发现私钥内容：${relativePath}:$lineNumber")
        }
        if ($line -match 'WECHAT_PAY_API_V3_KEY\s*[:=]\s*["'']?[A-Za-z0-9]{32}["'']?\s*$') {
            $errors.Add("疑似微信支付 API v3 Key：${relativePath}:$lineNumber")
        }
        if ($line -match 'WX_MA_SECRET\s*[:=]\s*["'']?(?!\$\{)(?!<)[A-Za-z0-9_-]{16,}["'']?\s*$') {
            $errors.Add("疑似微信小程序 Secret：${relativePath}:$lineNumber")
        }
        if ($line -match 'KUAIDI100_KEY\s*[:=]\s*["'']?(?!\$\{)(?!<)[A-Za-z0-9_-]{16,}["'']?\s*$') {
            $errors.Add("疑似快递 100 Key：${relativePath}:$lineNumber")
        }
    }
}

if ($errors.Count -gt 0) {
    throw "Secret 扫描失败：`n$($errors -join "`n")"
}

Write-Host "Secret 扫描通过，未发现私钥、证书或高风险真实密钥。"

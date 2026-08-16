param(
    [switch]$RunOnlineAudit
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if (-not $RunOnlineAudit) {
    Write-Host "依赖漏洞扫描为重门禁：交付前请执行 scripts/verify-dependency-audit.ps1 -RunOnlineAudit。"
    exit 0
}

Push-Location (Join-Path $root "shop-admin")
try {
    corepack pnpm audit --audit-level high
} finally {
    Pop-Location
}

Push-Location (Join-Path $root "shop-backend")
try {
    mvn -B -DskipTests org.owasp:dependency-check-maven:12.1.8:check "-DfailBuildOnCVSS=7"
} finally {
    Pop-Location
}

Write-Host "依赖漏洞扫描通过。"

param(
    [switch]$SkipBackendTests,
    [switch]$SkipAdminBuild,
    [switch]$SkipDbMigration
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "verify-secret-scan.ps1")
& (Join-Path $PSScriptRoot "verify-production-config.ps1")
& (Join-Path $PSScriptRoot "verify-miniapp-api-contract.ps1")
& (Join-Path $PSScriptRoot "verify-backend-api-contract.ps1")

if (-not $SkipBackendTests) {
    Push-Location (Join-Path $root "shop-backend")
    try {
        mvn -B test
    } finally {
        Pop-Location
    }
}

if (-not $SkipAdminBuild) {
    Push-Location (Join-Path $root "shop-admin")
    try {
        corepack pnpm install --frozen-lockfile
        corepack pnpm typecheck
        corepack pnpm build
    } finally {
        Pop-Location
    }
}

if (-not $SkipDbMigration) {
    & (Join-Path $PSScriptRoot "verify-db-migration.ps1")
}

Write-Host "v1.0 基础 CI 门禁通过。"


param(
    [switch]$SkipBackendTests,
    [switch]$SkipAdminBuild,
    [switch]$SkipDbMigration,
    [switch]$RunTradeFlow,
    [switch]$RunCommerceConsistency,
    [switch]$RunAdminLint,
    [switch]$RunDependencyAudit
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot "verify-secret-scan.ps1")
& (Join-Path $PSScriptRoot "verify-production-config.ps1")
& (Join-Path $PSScriptRoot "verify-admin-production-readiness.ps1")
& (Join-Path $PSScriptRoot "verify-admin-permission-matrix.ps1")
& (Join-Path $PSScriptRoot "verify-miniapp-api-contract.ps1")
& (Join-Path $PSScriptRoot "verify-miniapp-production-readiness.ps1")
& (Join-Path $PSScriptRoot "verify-miniapp-review-readiness.ps1")
& (Join-Path $PSScriptRoot "verify-logistics-readiness.ps1")
& (Join-Path $PSScriptRoot "verify-delivery-docs.ps1")
& (Join-Path $PSScriptRoot "verify-backend-api-contract.ps1")
& (Join-Path $PSScriptRoot "verify-dependency-audit.ps1") -RunOnlineAudit:$RunDependencyAudit

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
        if ($RunAdminLint) {
            corepack pnpm lint:check
        }
    } finally {
        Pop-Location
    }
}

if (-not $SkipDbMigration) {
    & (Join-Path $PSScriptRoot "verify-db-migration.ps1")
}

if ($RunTradeFlow) {
    & (Join-Path $PSScriptRoot "verify-trade-flow.ps1")
}

if ($RunCommerceConsistency) {
    & (Join-Path $PSScriptRoot "verify-commerce-consistency.ps1")
}

Write-Host "v1.0 基础 CI 门禁通过。"

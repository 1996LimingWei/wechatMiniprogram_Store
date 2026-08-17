param(
    [string]$EnvFile = ".env.prod",
    [string]$ComposeFile = "docker-compose.prod.yml",
    [string]$Service = "mysql"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $root $EnvFile
if (-not (Test-Path $envPath)) {
    throw "环境变量文件不存在：$envPath"
}

$values = @{}
foreach ($line in Get-Content $envPath -Encoding utf8) {
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith("#") -or $trimmed -notmatch "=") {
        continue
    }
    $parts = $trimmed.Split("=", 2)
    $values[$parts[0]] = $parts[1]
}

$database = $values["MYSQL_DATABASE"]
$password = $values["MYSQL_ROOT_PASSWORD"]
if (-not $database -or -not $password) {
    throw "环境变量文件必须包含 MYSQL_DATABASE 和 MYSQL_ROOT_PASSWORD"
}

$backupDir = Join-Path $root "deploy/backups/mysql"
New-Item -ItemType Directory -Force $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$backupFile = Join-Path $backupDir "$database-$timestamp.sql"

$composePath = Join-Path $root $ComposeFile
docker compose --env-file $envPath -f $composePath exec -T $Service sh -c "mysqldump -uroot -p`"$password`" --single-transaction --routines --triggers --default-character-set=utf8mb4 `"$database`"" > $backupFile
if ($LASTEXITCODE -ne 0) {
    throw "数据库备份失败"
}

Write-Host "数据库备份完成：$backupFile"


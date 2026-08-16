param(
    [Parameter(Mandatory = $true)]
    [string]$Database,
    [string]$MysqlContainer = "shop-mysql",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root"
)

$ErrorActionPreference = "Stop"
$MigrationDirectory = Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "sql") "migrations"

function Invoke-MysqlQuery {
    param(
        [string]$Sql
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $result = & docker exec $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $Database -N -B -e $Sql 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0) {
        throw "SQL 执行失败：$Sql"
    }
    return $result
}

function Invoke-MigrationSql {
    param(
        [string]$Sql,
        [string]$MigrationName
    )

    $temporaryFile = New-TemporaryFile
    $containerFile = "/tmp/shop-migration-$([guid]::NewGuid().ToString('N')).sql"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        [System.IO.File]::WriteAllText($temporaryFile.FullName, $Sql, [System.Text.UTF8Encoding]::new($false))
        & docker cp $temporaryFile.FullName "${MysqlContainer}:$containerFile" | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "迁移文件复制失败：$MigrationName"
        }
        $mysqlCommand = 'mysql --default-character-set=utf8mb4 -u"$1" -p"$2" "$3" < "$4"'
        & docker exec $MysqlContainer sh -c $mysqlCommand migration $MysqlUser $MysqlPassword $Database $containerFile 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "迁移执行失败：$MigrationName"
        }
    } finally {
        Remove-Item -LiteralPath $temporaryFile.FullName -Force -ErrorAction SilentlyContinue
        & docker exec $MysqlContainer rm -f $containerFile 2>$null | Out-Null
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

if (-not (Test-Path -PathType Container $MigrationDirectory)) {
    throw "迁移目录不存在：$MigrationDirectory"
}

Invoke-MysqlQuery @'
CREATE TABLE IF NOT EXISTS schema_migration_history (
  version varchar(32) NOT NULL,
  description varchar(128) NOT NULL,
  checksum varchar(64) NOT NULL,
  installed_on datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB COMMENT='数据库迁移历史';
'@ | Out-Null

$migrations = Get-ChildItem -Path $MigrationDirectory -File -Filter "V*__*.sql" | Sort-Object Name
foreach ($migration in $migrations) {
    if ($migration.Name -notmatch "^V(?<version>[0-9_]+)__(?<description>.+)\.sql$") {
        throw "迁移文件名不符合版本规则：$($migration.Name)"
    }

    $version = $Matches.version
    $description = $Matches.description
    $sql = (Get-Content -Path $migration.FullName -Raw -Encoding utf8) -replace "`r`n", "`n"
    $checksumBytes = [System.Text.Encoding]::UTF8.GetBytes($sql)
    $checksum = [System.BitConverter]::ToString(
        [System.Security.Cryptography.SHA256]::Create().ComputeHash($checksumBytes)
    ).Replace('-', '').ToLowerInvariant()
    $recordedChecksum = Invoke-MysqlQuery "SELECT checksum FROM schema_migration_history WHERE version = '$version';"
    if ($recordedChecksum) {
        if ($recordedChecksum -ne $checksum) {
            throw "迁移文件校验和已变化：$($migration.Name)"
        }
        Write-Host "跳过已执行迁移：$($migration.Name)"
        continue
    }

    Invoke-MigrationSql -Sql $sql -MigrationName $migration.Name
    Invoke-MysqlQuery "INSERT INTO schema_migration_history(version, description, checksum) VALUES ('$version', '$description', '$checksum');" | Out-Null
    Write-Host "已执行迁移：$($migration.Name)"
}

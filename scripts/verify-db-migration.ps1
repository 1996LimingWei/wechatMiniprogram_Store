param(
    [string]$MysqlContainer = "shop-mysql",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root"
)

$ErrorActionPreference = "Stop"
$TestDatabase = "shop_migration_verify_$(Get-Date -Format 'yyyyMMddHHmmssfff')"
$DatabaseCreated = $false

function Invoke-Mysql {
    param(
        [string]$Database,
        [string]$Sql
    )
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $result = & docker exec $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $Database -N -B -e $Sql 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0) {
        throw "SQL 执行失败：$Sql"
    }
    return $result
}

function Assert-Equal {
    param(
        [object]$Actual,
        [object]$Expected,
        [string]$Message
    )
    if ([string]$Actual -ne [string]$Expected) {
        throw "验收失败：$Message；实际值=$Actual，期望值=$Expected"
    }
}

try {
    $deadline = (Get-Date).AddSeconds(60)
    do {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & docker exec $MysqlContainer mysqladmin "-u$MysqlUser" "-p$MysqlPassword" ping --silent 2>$null | Out-Null
        $pingExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference
        if ($pingExitCode -eq 0) { break }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    if ($pingExitCode -ne 0) {
        throw "MySQL 容器未在 60 秒内就绪：$MysqlContainer"
    }

    Invoke-Mysql 'mysql' "CREATE DATABASE $TestDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    $DatabaseCreated = $true
    Invoke-Mysql $TestDatabase "CREATE TABLE trade_order (id bigint NOT NULL AUTO_INCREMENT, order_sn varchar(32) NOT NULL, user_id bigint NOT NULL, status tinyint NOT NULL DEFAULT 0, pay_status tinyint NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_order_sn (order_sn), KEY idx_user_id (user_id), KEY idx_status (status)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE pay_order (id bigint NOT NULL AUTO_INCREMENT, pay_sn varchar(32) NOT NULL, order_id bigint NOT NULL, user_id bigint NOT NULL, amount int NOT NULL, channel varchar(32) NOT NULL DEFAULT 'mock', status tinyint NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_pay_sn (pay_sn), KEY idx_order_id (order_id)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE content_banner (id bigint NOT NULL AUTO_INCREMENT, title varchar(128) NOT NULL, pic_url varchar(512) NOT NULL, url varchar(512) DEFAULT '', sort int NOT NULL DEFAULT 0, status tinyint NOT NULL DEFAULT 1, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB;" | Out-Null

    & "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "迁移脚本应执行成功"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_order_log';") 1 "应创建订单日志表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale';") 1 "应创建售后表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'expire_time';") 1 "应新增订单过期时间字段"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_expire_status';") 3 "订单过期索引应包含状态、支付状态和过期时间三列"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'before_order_status';") 1 "售后表应保留订单原状态"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'cancel_time';") 1 "售后表应支持撤销时间"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COLUMN_COMMENT LIKE '%3=%' FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND column_name = 'status';") 1 "支付状态定义应包含退款状态"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260727_01';") 1 "应记录已执行迁移"

    & "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "重复执行迁移应成功"
    $migrationCount = (Get-ChildItem (Join-Path $PSScriptRoot "..\sql\migrations") -File -Filter "V*__*.sql").Count
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history;") $migrationCount "重复执行不得重复记录迁移"
}
finally {
    if ($DatabaseCreated) {
        Invoke-Mysql "mysql" ("DROP DATABASE IF EXISTS {0};" -f $TestDatabase) | Out-Null
    }
}

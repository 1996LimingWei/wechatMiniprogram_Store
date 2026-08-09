param(
    [string]$MysqlContainer = "shop-mysql",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root"
)

$ErrorActionPreference = "Stop"
$TestDatabase = "shop_order_query_verify_$(Get-Date -Format 'yyyyMMddHHmmssfff')"
$MigrationFile = Join-Path $PSScriptRoot "..\sql\migrations\V20260731_01__trade_order_query_indexes.sql"
$ExpectedMigrationCount = (Get-ChildItem -Path (Join-Path $PSScriptRoot '..\sql\migrations') -File -Filter 'V*__*.sql').Count
$OrderSelectColumns = "id, order_sn, user_id, status, pay_status, mobile, create_time"
$DatabaseCreated = $false

function Write-Step([string]$Message) {
    Write-Host "[订单查询验收] $Message"
}

function Invoke-Mysql {
    param(
        [string]$Database,
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

function Invoke-MigrationFile {
    param(
        [string]$Database,
        [string]$Path
    )

    $sql = Get-Content -Path $Path -Raw -Encoding utf8
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $sql | & docker exec -i $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $Database 2>$null
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($exitCode -ne 0) {
        throw "迁移重复执行失败：$(Split-Path -Path $Path -Leaf)"
    }
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

function Assert-True {
    param(
        [object]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "验收失败：$Message"
    }
}

function Get-IndexColumns([string]$IndexName) {
    return Invoke-Mysql $TestDatabase @"
SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index)
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'trade_order'
  AND index_name = '$IndexName';
"@
}

function Get-ExplainPlan([string]$Sql) {
    $row = [string](Invoke-Mysql $TestDatabase "EXPLAIN $Sql")
    $columns = $row -split "`t"
    if ($columns.Count -lt 12) {
        throw "无法解析 EXPLAIN 结果：$row"
    }
    return [pscustomobject]@{
        Type = $columns[4]
        PossibleKeys = $columns[5]
        Key = $columns[6]
        Rows = $columns[9]
        Extra = $columns[11]
    }
}

function Assert-ExplainKey {
    param(
        [string]$Scene,
        [string]$Sql,
        [string]$ExpectedKey
    )

    $plan = Get-ExplainPlan $Sql
    Write-Host ("[订单查询验收] {0}：key={1}, type={2}, rows={3}, Extra={4}" -f
        $Scene, $plan.Key, $plan.Type, $plan.Rows, $plan.Extra)
    Assert-Equal $plan.Key $ExpectedKey "$Scene 应命中索引 $ExpectedKey"
    Assert-True ($plan.Type -ne "ALL") "$Scene 不应执行全表扫描"
}

function Assert-ExplainCandidate {
    param(
        [string]$Scene,
        [string]$Sql,
        [string]$RequiredPossibleKey,
        [string[]]$AllowedSelectedKeys
    )

    $plan = Get-ExplainPlan $Sql
    Write-Host ("[订单查询验收] {0}：possible_keys={1}, key={2}, type={3}, rows={4}, Extra={5}" -f
        $Scene, $plan.PossibleKeys, $plan.Key, $plan.Type, $plan.Rows, $plan.Extra)
    $possibleKeys = @($plan.PossibleKeys -split ",")
    Assert-True ($possibleKeys -contains $RequiredPossibleKey) "$Scene 的候选索引应包含 $RequiredPossibleKey"
    Assert-True ($AllowedSelectedKeys -contains $plan.Key) `
        "$Scene 的实际索引应为以下之一：$($AllowedSelectedKeys -join ', ')"
    Assert-True ($plan.Type -ne "ALL") "$Scene 不应执行全表扫描"
}

try {
    Write-Step "检查 Docker 与 MySQL 容器"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker info --format "{{.ServerVersion}}" *> $null
    $dockerExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($dockerExitCode -ne 0) {
        throw "Docker 服务不可用，请启动 Docker Desktop 后重试"
    }

    Write-Step "创建隔离数据库与迁移基线"
    Invoke-Mysql "mysql" "CREATE DATABASE $TestDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
    $DatabaseCreated = $true
    Invoke-Mysql $TestDatabase @'
CREATE TABLE trade_order (
  id bigint NOT NULL AUTO_INCREMENT,
  order_sn varchar(32) NOT NULL,
  user_id bigint NOT NULL,
  status tinyint NOT NULL DEFAULT 0,
  pay_status tinyint NOT NULL DEFAULT 0,
  mobile varchar(20) DEFAULT '',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_sn (order_sn),
  KEY idx_user_id (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB;

CREATE TABLE pay_order (
  id bigint NOT NULL AUTO_INCREMENT,
  pay_sn varchar(32) NOT NULL,
  order_id bigint NOT NULL,
  user_id bigint NOT NULL,
  amount int NOT NULL,
  channel varchar(32) NOT NULL DEFAULT 'mock',
  status tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_sn (pay_sn),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB;

CREATE TABLE content_banner (
  id bigint NOT NULL AUTO_INCREMENT,
  title varchar(128) NOT NULL,
  pic_url varchar(512) NOT NULL,
  url varchar(512) DEFAULT '',
  sort int NOT NULL DEFAULT 0,
  status tinyint NOT NULL DEFAULT 1,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id)
) ENGINE=InnoDB;
'@ | Out-Null

    & "$PSScriptRoot/migrate-db.ps1" `
        -Database $TestDatabase `
        -MysqlContainer $MysqlContainer `
        -MysqlUser $MysqlUser `
        -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "全量迁移应执行成功"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260730_03';") 1 "支付状态机迁移应先存在"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260731_01';") 1 "订单查询索引迁移应被记录"

    Write-Step "验证索引列顺序"
    $expectedIndexes = [ordered]@{
        idx_create_time_id = "create_time,id"
        idx_user_create_time_id = "user_id,create_time,id"
        idx_mobile_create_time_id = "mobile,create_time,id"
        idx_status_pay_create_time_id = "status,pay_status,create_time,id"
        idx_pay_status_create_time_id = "pay_status,create_time,id"
    }
    foreach ($index in $expectedIndexes.GetEnumerator()) {
        Assert-Equal (Get-IndexColumns $index.Key) $index.Value "索引 $($index.Key) 的列顺序应正确"
    }
    Assert-Equal (Invoke-Mysql $TestDatabase @"
SELECT COUNT(DISTINCT index_name)
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'trade_order'
  AND index_name IN ('idx_create_time_id', 'idx_user_create_time_id', 'idx_mobile_create_time_id',
                     'idx_status_pay_create_time_id', 'idx_pay_status_create_time_id');
"@) $expectedIndexes.Count "应只存在五个约定的查询索引"

    Write-Step "验证迁移重复执行安全"
    Invoke-MigrationFile -Database $TestDatabase -Path $MigrationFile
    foreach ($index in $expectedIndexes.GetEnumerator()) {
        Assert-Equal (Get-IndexColumns $index.Key) $index.Value "重复执行后索引 $($index.Key) 不得变化"
    }
    & "$PSScriptRoot/migrate-db.ps1" `
        -Database $TestDatabase `
        -MysqlContainer $MysqlContainer `
        -MysqlUser $MysqlUser `
        -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "迁移管理脚本重复执行应成功"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history;") $ExpectedMigrationCount "重复执行不得新增迁移记录"

    Write-Step "准备代表性订单数据"
    Invoke-Mysql $TestDatabase @'
CREATE TABLE verify_digit (n tinyint NOT NULL PRIMARY KEY);
INSERT INTO verify_digit(n) VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

INSERT INTO trade_order
  (order_sn, user_id, status, pay_status, mobile, create_time, update_time, deleted)
SELECT
  CONCAT('Q', LPAD(sequence_no, 31, '0')),
  MOD(sequence_no, 200) + 1,
  MOD(sequence_no, 6),
  MOD(sequence_no, 3),
  CONCAT('13', MOD(sequence_no, 10), LPAD(MOD(sequence_no, 100000000), 8, '0')),
  TIMESTAMP('2026-01-01 00:00:00') + INTERVAL sequence_no SECOND,
  TIMESTAMP('2026-01-01 00:00:00') + INTERVAL sequence_no SECOND,
  b'0'
FROM (
  SELECT
    ones.n
      + tens.n * 10
      + hundreds.n * 100
      + thousands.n * 1000
      + ten_thousands.n * 10000 AS sequence_no
  FROM verify_digit ones
  CROSS JOIN verify_digit tens
  CROSS JOIN verify_digit hundreds
  CROSS JOIN verify_digit thousands
  CROSS JOIN verify_digit ten_thousands
) sequence_source
WHERE sequence_no < 20000;

ANALYZE TABLE trade_order;
'@ | Out-Null
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM trade_order;") 20000 "应准备两万条代表性订单"

    Write-Step "验证关键查询计划"
    Assert-ExplainKey "无筛选倒序列表" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_create_time_id"
    Assert-ExplainKey "创建时间范围" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND create_time >= '2026-01-01 02:00:00' AND create_time < '2026-01-01 03:00:00' ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_create_time_id"
    Assert-ExplainKey "用户订单列表" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND user_id = 42 ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_user_create_time_id"
    Assert-ExplainCandidate "用户订单分页计数" `
        "SELECT COUNT(*) FROM trade_order WHERE deleted = b'0' AND user_id = 42;" `
        "idx_user_create_time_id" `
        @("idx_user_create_time_id", "idx_user_id")
    Assert-ExplainKey "手机号前缀查询" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND mobile LIKE '1380000001%' ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_mobile_create_time_id"
    Assert-ExplainCandidate "订单与支付状态组合查询" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND status = 1 AND pay_status = 1 ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_status_pay_create_time_id" `
        @("idx_status_pay_create_time_id", "idx_expire_status", "idx_create_time_id")
    Assert-ExplainCandidate "支付状态查询" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND pay_status = 2 ORDER BY create_time DESC, id DESC LIMIT 20;" `
        "idx_pay_status_create_time_id" `
        @("idx_pay_status_create_time_id", "idx_create_time_id")
    Assert-ExplainKey "订单号精确查询" `
        "SELECT $OrderSelectColumns FROM trade_order WHERE deleted = b'0' AND order_sn = 'Q0000000000000000000000000000042' LIMIT 1;" `
        "uk_order_sn"

    Write-Host "订单查询迁移与查询计划验收通过"
}
finally {
    if ($DatabaseCreated) {
        Invoke-Mysql "mysql" "DROP DATABASE IF EXISTS $TestDatabase;" | Out-Null
    }
}

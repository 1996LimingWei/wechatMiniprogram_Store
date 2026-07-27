# 数据库增量迁移实施计划

> **面向执行 Agent：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，逐项执行并勾选本计划中的复选框。

**目标：** 为项目 MySQL 建立可重复执行、可追踪的增量迁移能力，并把旧版 `shop` 数据库安全升级到交易闭环 P0 所需结构。

**架构：** 迁移 SQL 存放在 `sql/migrations/`，PowerShell 执行器按版本顺序读取文件，在目标库的 `schema_migration_history` 中记录版本、校验和和执行时间。执行器通过项目已有的 Docker MySQL 容器运行 `mysql` 客户端；每个迁移在写入历史记录前必须完整成功。迁移 SQL 使用 `information_schema` 和临时存储过程检测表、列和索引，确保目标库已部分升级时仍可安全执行。

**技术栈：** MySQL 8、PowerShell 7、Docker、项目 `shop-mysql` 容器。

---

## 子 Issue 拆分与顺序

1. `chore: 建立数据库增量迁移机制并补齐交易表结构`（本计划）
2. `feat: 合并并验收商品分类与列表真实接口`（现有 `feat/backend-product-real-api` 分支完成迁移验收后处理）
3. `feat: 实现商品详情、SKU 库存与交易快照对接`
4. `test: 补充真实商品链路与数据库迁移验收`

本计划只交付子 Issue 1；不修改商品接口、交易业务逻辑或小程序页面。

## 文件结构

| 文件 | 责任 |
| --- | --- |
| `sql/migrations/V20260727_01__trade_p0_schema.sql` | 从 2026-07-24 基线升级到交易 P0：订单超时字段与索引、订单日志表、售后表及支付状态注释。 |
| `scripts/migrate-db.ps1` | 创建迁移历史表、计算校验和、按版本执行 SQL、在成功后记录版本。 |
| `scripts/verify-db-migration.ps1` | 在名称唯一的临时数据库中构造旧版基线、执行迁移两次并断言表、列、索引与历史记录。 |
| `README.md` | 说明 JDK 25 前置条件、迁移命令、目标容器和禁止重跑初始化脚本的边界。 |
| `docs/superpowers/status.md` | 记录迁移机制完成、验证结果和后续商品分支集成入口。 |

### Task 1：编写迁移验收脚本（先失败）

**文件：**

- 新建：`scripts/verify-db-migration.ps1`
- 新建：`sql/migrations/V20260727_01__trade_p0_schema.sql`（空文件，仅供脚本定位）

- [x] **步骤 1：定义隔离数据库与断言工具。**

在 `scripts/verify-db-migration.ps1` 写入参数、Docker MySQL 调用和断言函数。测试库名称使用时间戳，禁止指向 `shop`：

```powershell
param(
    [string]$MysqlContainer = 'shop-mysql',
    [string]$MysqlUser = 'root',
    [string]$MysqlPassword = 'root'
)

$ErrorActionPreference = 'Stop'
$TestDatabase = "shop_migration_verify_$(Get-Date -Format 'yyyyMMddHHmmssfff')"

function Invoke-Mysql([string]$Database, [string]$Sql) {
    $result = & docker exec $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $Database -N -B -e $Sql 2>$null
    if ($LASTEXITCODE -ne 0) { throw "SQL 执行失败：$Sql" }
    return $result
}

function Invoke-MigrationSql([string]$Database, [string]$Sql) {
    $Sql | & docker exec -i $MysqlContainer mysql "-u$MysqlUser" "-p$MysqlPassword" $Database
    if ($LASTEXITCODE -ne 0) { throw '迁移 SQL 执行失败' }
}

function Assert-Equal([object]$Actual, [object]$Expected, [string]$Message) {
    if ([string]$Actual -ne [string]$Expected) {
        throw "验收失败：$Message；实际值=$Actual，期望值=$Expected"
    }
}
```

- [x] **步骤 2：构造 2026-07-24 的最小旧版交易基线。**

脚本创建 `$TestDatabase`，并只建立迁移涉及的旧版对象；`trade_order` 不含 `expire_time`、`close_time`、`close_reason` 和 `idx_expire_status`，也不建立 `trade_order_log`、`trade_after_sale`。基线 DDL 必须与 `git diff 1c1718f fff6be5 -- sql/init.sql` 中的旧版列定义一致：

```powershell
Invoke-Mysql 'mysql' "CREATE DATABASE `$TestDatabase` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" | Out-Null
Invoke-Mysql $TestDatabase @'
CREATE TABLE trade_order (
  id bigint NOT NULL AUTO_INCREMENT,
  order_sn varchar(32) NOT NULL,
  user_id bigint NOT NULL,
  status tinyint NOT NULL DEFAULT 0,
  pay_status tinyint NOT NULL DEFAULT 0,
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
'@ | Out-Null
```

将后续迁移调用和断言包裹在 `try/finally` 中；仅在 `finally` 删除本脚本创建的精确测试库：

```powershell
try {
    # 迁移调用与断言写在此处
}
finally {
    Invoke-Mysql 'mysql' ("DROP DATABASE IF EXISTS {0};" -f $TestDatabase) | Out-Null
}
```

- [x] **步骤 3：调用尚未实现的迁移脚本并确认其失败。**

```powershell
& "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
```

预期：失败，提示找不到 `scripts/migrate-db.ps1`；这证明验收脚本会实际调用迁移入口，而不是只检查静态 SQL。

- [x] **步骤 4：提交失败验收脚本。**

```powershell
git add scripts/verify-db-migration.ps1 sql/migrations/V20260727_01__trade_p0_schema.sql
git commit -m "test: 添加数据库迁移失败验收"
```

### Task 2：实现可追踪的迁移执行器

**文件：**

- 新建：`scripts/migrate-db.ps1`
- 修改：`scripts/verify-db-migration.ps1`

- [x] **步骤 1：在验收脚本中定义成功后的断言。**

在迁移调用后加入以下断言；在执行器实现前运行会失败：

```powershell
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_order_log';") 1 '应创建订单日志表'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale';") 1 '应创建售后表'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'expire_time';") 1 '应新增订单过期时间字段'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_expire_status';") 1 '应新增订单过期索引'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260727_01';") 1 '应记录已执行迁移'
```

- [x] **步骤 2：实现迁移历史表、文件发现和校验和校验。**

`scripts/migrate-db.ps1` 必须支持 `-Database`、`-MysqlContainer`、`-MysqlUser`、`-MysqlPassword` 参数；禁止默认清库或创建数据库。核心逻辑如下：

```powershell
$MigrationDirectory = Join-Path $PSScriptRoot '..\sql\migrations'
$migrations = Get-ChildItem $MigrationDirectory -File -Filter 'V*__*.sql' | Sort-Object Name

Invoke-Mysql $Database @'
CREATE TABLE IF NOT EXISTS schema_migration_history (
  version varchar(32) NOT NULL,
  description varchar(128) NOT NULL,
  checksum varchar(64) NOT NULL,
  installed_on datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (version)
) ENGINE=InnoDB COMMENT='数据库迁移历史';
'@ | Out-Null

foreach ($migration in $migrations) {
    if ($migration.Name -notmatch '^V(?<version>[0-9_]+)__(?<description>.+)\.sql$') {
        throw "迁移文件名不符合版本规则：$($migration.Name)"
    }
    $version = $Matches.version
    $checksum = (Get-FileHash $migration.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $record = Invoke-Mysql $Database "SELECT checksum FROM schema_migration_history WHERE version = '$version';"
    if ($record) {
        if ($record -ne $checksum) { throw "迁移文件校验和已变化：$($migration.Name)" }
        continue
    }
    $sql = Get-Content $migration.FullName -Raw -Encoding utf8
    Invoke-MigrationSql $Database $sql
    Invoke-Mysql $Database "INSERT INTO schema_migration_history(version, description, checksum) VALUES ('$version', '$($Matches.description)', '$checksum');" | Out-Null
}
```

- [x] **步骤 3：执行验收脚本，确认表、字段、索引与历史记录均存在。**

运行：

```powershell
.\scripts\verify-db-migration.ps1
```

预期：在临时数据库中完成第一轮迁移，所有断言通过。

- [x] **步骤 4：为重复执行增加断言并再次执行。**

在验收脚本第二次调用 `migrate-db.ps1`，并断言历史表只有一行：

```powershell
& "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
Assert-Equal (Invoke-Mysql $TestDatabase 'SELECT COUNT(*) FROM schema_migration_history;') 1 '重复执行不得重复记录迁移'
```

预期：第二轮执行跳过已记录版本，不重复修改结构。

- [x] **步骤 5：提交迁移执行器。**

```powershell
git add scripts/migrate-db.ps1 scripts/verify-db-migration.ps1
git commit -m "feat: 建立数据库增量迁移执行器"
```

### Task 3：编写交易 P0 结构迁移

**文件：**

- 修改：`sql/migrations/V20260727_01__trade_p0_schema.sql`
- 修改：`scripts/verify-db-migration.ps1`

- [x] **步骤 1：让验收脚本增加售后表字段与支付状态断言。**

```powershell
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'before_order_status';") 1 '售后表应保留订单原状态'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'cancel_time';") 1 '售后表应支持撤销时间'
Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COLUMN_COMMENT FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND column_name = 'status';") '支付状态 0=待支付 1=已支付 2=已关闭 3=已退款' '支付状态定义应包含退款'
```

- [x] **步骤 2：实现 `V20260727_01__trade_p0_schema.sql`。**

迁移中创建 `trade_order_log`、`trade_after_sale` 时使用 `CREATE TABLE IF NOT EXISTS`，列、索引使用临时存储过程检测 `information_schema` 后再执行。例如：

```sql
DELIMITER //
CREATE PROCEDURE migration_add_column_if_missing(
  IN table_name_arg varchar(64), IN column_name_arg varchar(64), IN definition_arg text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = table_name_arg AND column_name = column_name_arg
  ) THEN
    SET @migration_sql = CONCAT('ALTER TABLE `', table_name_arg, '` ADD COLUMN `', column_name_arg, '` ', definition_arg);
    PREPARE migration_stmt FROM @migration_sql;
    EXECUTE migration_stmt;
    DEALLOCATE PREPARE migration_stmt;
  END IF;
END //
DELIMITER ;

CALL migration_add_column_if_missing('trade_order', 'expire_time', 'datetime DEFAULT NULL COMMENT ''待付款超时关闭时间''');
CALL migration_add_column_if_missing('trade_order', 'close_time', 'datetime DEFAULT NULL COMMENT ''订单关闭时间''');
CALL migration_add_column_if_missing('trade_order', 'close_reason', 'varchar(128) DEFAULT '''' COMMENT ''订单关闭原因''');
DROP PROCEDURE migration_add_column_if_missing;
```

同一迁移必须：

- 创建 `idx_expire_status(status, pay_status, expire_time)`，且仅在缺失时创建；
- 以 `ALTER TABLE ... MODIFY COLUMN` 更新 `trade_order.status` 和 `pay_order.status` 的状态注释；
- 使用与 `sql/init.sql` 相同的列、默认值、唯一索引和普通索引定义创建两个新表；
- 不删除、重命名或重置任何现有数据。

- [x] **步骤 3：运行完整迁移验收并检查输出。**

运行：

```powershell
.\scripts\verify-db-migration.ps1
```

预期：首次运行创建所需对象，第二次运行只跳过已记录版本；临时库的清理仅删除脚本刚创建的 `$TestDatabase`。

- [x] **步骤 4：提交交易 P0 迁移。**

```powershell
git add sql/migrations/V20260727_01__trade_p0_schema.sql scripts/verify-db-migration.ps1
git commit -m "feat: 补齐交易 P0 数据库迁移"
```

### Task 4：补充开发文档并验证真实本地库

**文件：**

- 修改：`README.md`
- 修改：`docs/superpowers/status.md`
- 修改：`scripts/verify-db-migration.ps1`

- [x] **步骤 1：在 README 本地开发章节补充固定命令。**

```powershell
docker start shop-mysql shop-redis
.\scripts\migrate-db.ps1 -Database shop
```

文档必须注明：该命令仅升级已有库；全新数据库才使用 `sql/init.sql`；运行后端前必须使用 JDK 25。

- [x] **步骤 2：备份并迁移本地 `shop` 库。**

先创建带时间戳的 SQL 备份文件，再执行迁移；备份与迁移目标均须明确为 `shop`：

```powershell
$backupFile = "sql\backups\shop-before-migration-$(Get-Date -Format 'yyyyMMddHHmmss').sql"
New-Item -ItemType Directory -Force 'sql\backups' | Out-Null
docker exec shop-mysql mysqldump -uroot -proot shop | Set-Content -Encoding utf8 $backupFile
.\scripts\migrate-db.ps1 -Database shop
```

执行前确认 `sql/backups/` 已加入 `.gitignore`，防止备份及敏感数据被提交。

- [x] **步骤 3：对真实本地库执行只读结构断言。**

```powershell
docker exec shop-mysql mysql -uroot -proot shop -N -B -e "SHOW TABLES LIKE 'trade_after_sale'; SHOW TABLES LIKE 'trade_order_log';"
docker exec shop-mysql mysql -uroot -proot shop -N -B -e "SHOW INDEX FROM trade_order WHERE Key_name = 'idx_expire_status';"
```

预期：输出两个表名和 `idx_expire_status` 索引；不执行删除或重置语句。

- [x] **步骤 4：在 JDK 25 环境完成构建验证。**

```powershell
cd shop-backend
mvn clean install -DskipTests
```

预期：11 个 Maven 模块全部 `SUCCESS`。若环境仍使用 JDK 17，停止并先配置 JDK 25，不得把 JDK 版本回退为 17。

- [x] **步骤 5：更新状态并提交文档。**

```powershell
git add .gitignore README.md docs/superpowers/status.md scripts sql/migrations
git commit -m "docs: 补充数据库迁移使用说明"
```

## 计划自检

- 规格中的“可追踪、可重复、旧库安全升级”分别由 Task 2、Task 3、Task 4 覆盖。
- 交易 P0 缺失表、字段、索引由 Task 3 的 SQL 与验收断言覆盖。
- 商品真实接口、SKU 和交易快照不在本计划范围，保留给后续子 Issue，避免与现有 `feat/backend-product-real-api` 工作树冲突。
- 所有创建、修改、测试与提交步骤均给出明确路径与命令；不含未完成步骤。

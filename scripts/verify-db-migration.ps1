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
    Invoke-Mysql $TestDatabase "CREATE TABLE trade_order (id bigint NOT NULL AUTO_INCREMENT, order_sn varchar(32) NOT NULL, user_id bigint NOT NULL, status tinyint NOT NULL DEFAULT 0, pay_status tinyint NOT NULL DEFAULT 0, mobile varchar(20) DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id), UNIQUE KEY uk_order_sn (order_sn), KEY idx_user_id (user_id), KEY idx_status (status)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE pay_order (id bigint NOT NULL AUTO_INCREMENT, pay_sn varchar(32) NOT NULL, order_id bigint NOT NULL, user_id bigint NOT NULL, amount int NOT NULL, channel varchar(32) NOT NULL DEFAULT 'mock', status tinyint NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_pay_sn (pay_sn), KEY idx_order_id (order_id)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE content_banner (id bigint NOT NULL AUTO_INCREMENT, title varchar(128) NOT NULL, pic_url varchar(512) NOT NULL, url varchar(512) DEFAULT '', sort int NOT NULL DEFAULT 0, status tinyint NOT NULL DEFAULT 1, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE product_category (id bigint NOT NULL AUTO_INCREMENT, parent_id bigint NOT NULL DEFAULT 0, name varchar(64) NOT NULL, icon varchar(512) DEFAULT '', sort int NOT NULL DEFAULT 0, status tinyint NOT NULL DEFAULT 1, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE product_spu (id bigint NOT NULL AUTO_INCREMENT, category_id bigint NOT NULL, name varchar(128) NOT NULL, keyword varchar(256) DEFAULT '', introduction varchar(256) DEFAULT '', description text, pic_url varchar(512) NOT NULL, slider_pic_urls varchar(2048) DEFAULT '[]', video_url varchar(512) DEFAULT '', type tinyint NOT NULL DEFAULT 1, price int NOT NULL, market_price int DEFAULT NULL, stock int NOT NULL DEFAULT 0, sales_count int NOT NULL DEFAULT 0, sort int NOT NULL DEFAULT 0, status tinyint NOT NULL DEFAULT 0, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id), KEY idx_category (category_id), KEY idx_status (status)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE product_sku (id bigint NOT NULL AUTO_INCREMENT, spu_id bigint NOT NULL, properties varchar(512) DEFAULT '[]', price int NOT NULL, market_price int DEFAULT NULL, stock int NOT NULL DEFAULT 0, pic_url varchar(512) DEFAULT '', weight double DEFAULT NULL, volume double DEFAULT NULL, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id), KEY idx_spu_id (spu_id)) ENGINE=InnoDB;" | Out-Null
    Invoke-Mysql $TestDatabase "CREATE TABLE member_user (id bigint NOT NULL AUTO_INCREMENT, openid varchar(64) DEFAULT NULL, unionid varchar(64) DEFAULT NULL, session_key varchar(128) DEFAULT NULL, mobile varchar(20) DEFAULT NULL, nickname varchar(64) DEFAULT '', avatar varchar(512) DEFAULT '', status tinyint NOT NULL DEFAULT 1, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted bit(1) NOT NULL DEFAULT b'0', PRIMARY KEY (id), UNIQUE KEY uk_openid (openid)) ENGINE=InnoDB;" | Out-Null

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
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'pay_order' AND index_name = 'uk_order_id';") 1 "支付单应按订单唯一约束"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260730_03';") 1 "应记录支付状态机迁移"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260731_01';") 1 "应记录订单查询索引迁移"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_create_time_id';") "create_time,id" "创建时间索引列顺序应正确"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_user_create_time_id';") "user_id,create_time,id" "用户订单索引列顺序应正确"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_mobile_create_time_id';") "mobile,create_time,id" "手机号查询索引列顺序应正确"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_status_pay_create_time_id';") "status,pay_status,create_time,id" "订单状态查询索引列顺序应正确"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_pay_status_create_time_id';") "pay_status,create_time,id" "支付状态查询索引列顺序应正确"

    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260801_02';") 1 "应记录演示种子迁移"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_category WHERE id BETWEEN 240101 AND 240131;") 7 "应写入完整演示分类"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_spu WHERE id BETWEEN 240001 AND 240006;") 6 "应写入演示商品"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_sku WHERE id BETWEEN 240011 AND 240061;") 10 "应写入演示 SKU"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_spu p LEFT JOIN product_category c ON c.id=p.category_id WHERE p.id BETWEEN 240001 AND 240006 AND c.id IS NULL;") 0 "演示商品不得存在孤儿分类"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_sku s LEFT JOIN product_spu p ON p.id=s.spu_id WHERE s.id BETWEEN 240011 AND 240061 AND p.id IS NULL;") 0 "演示 SKU 不得存在孤儿商品"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_comment c LEFT JOIN member_user u ON u.id=c.user_id LEFT JOIN product_spu p ON p.id=c.spu_id WHERE c.id=240401 AND (u.id IS NULL OR p.id IS NULL);") 0 "演示评论关联必须完整"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_category c WHERE c.id BETWEEN 240111 AND 240131 AND c.parent_id<>0 AND NOT EXISTS (SELECT 1 FROM product_spu p WHERE p.category_id=c.id AND p.status=1 AND p.deleted=b'0');") 0 "每个演示二级分类都应有可售商品"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT CONCAT(COUNT(*),':',SUM(stock=0),':',SUM(stock>0),':',COUNT(DISTINCT price),':',COUNT(DISTINCT pic_url)) FROM product_sku WHERE spu_id=240001;") "4:1:3:4:2" "二维多规格商品应覆盖缺货、价格和图片差异"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT CONCAT(COUNT(*),':',SUM(stock=0)) FROM product_sku WHERE spu_id=240002;") "2:2" "全部缺货商品应无可售 SKU"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT CONCAT((SELECT COUNT(*) FROM content_banner WHERE id BETWEEN 240201 AND 240202),':',(SELECT COUNT(*) FROM content_channel WHERE id BETWEEN 240211 AND 240213),':',(SELECT COUNT(*) FROM content_brand WHERE id BETWEEN 240221 AND 240222),':',(SELECT COUNT(*) FROM content_topic WHERE id BETWEEN 240231 AND 240232),':',(SELECT COUNT(*) FROM product_comment WHERE id=240401));") "2:3:2:2:1" "首页内容与评论种子应完整"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT HEX(name) FROM product_spu WHERE id=240001;") "E78EABE791B0E998BFE883B6E7B395E7A4BCE79B92" "演示商品中文名称应以 UTF-8 正确写入"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT HEX(title) FROM content_banner WHERE id=240201;") "E78EABE791B0E998BFE883B6E7B395E5A49AE8A784E6A0BCE7A4BCE79B92" "首页内容中文标题应以 UTF-8 正确写入"

    $seedSignature = Invoke-Mysql $TestDatabase "SELECT CONCAT((SELECT COUNT(*) FROM product_category WHERE id BETWEEN 240101 AND 240131),':',(SELECT COUNT(*) FROM product_spu WHERE id BETWEEN 240001 AND 240006),':',(SELECT COUNT(*) FROM product_sku WHERE id BETWEEN 240011 AND 240061),':',(SELECT COUNT(*) FROM content_banner WHERE id BETWEEN 240201 AND 240202),':',(SELECT COUNT(*) FROM product_comment WHERE id=240401));"
    Invoke-Mysql $TestDatabase "DELETE FROM schema_migration_history WHERE version='20260801_02';" | Out-Null
    & "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "强制重放演示种子迁移应成功"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT CONCAT((SELECT COUNT(*) FROM product_category WHERE id BETWEEN 240101 AND 240131),':',(SELECT COUNT(*) FROM product_spu WHERE id BETWEEN 240001 AND 240006),':',(SELECT COUNT(*) FROM product_sku WHERE id BETWEEN 240011 AND 240061),':',(SELECT COUNT(*) FROM content_banner WHERE id BETWEEN 240201 AND 240202),':',(SELECT COUNT(*) FROM product_comment WHERE id=240401));") $seedSignature "强制重放不得产生重复种子"

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

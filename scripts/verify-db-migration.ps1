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

function Initialize-TestDatabase([string]$Database) {
    $temporaryFile = New-TemporaryFile
    $containerFile = "/tmp/shop-init-$Database.sql"
    try {
        Get-Content (Join-Path $PSScriptRoot "..\sql\init.sql") -Encoding utf8 |
            Select-Object -Skip 7 |
            Set-Content $temporaryFile -Encoding utf8
        & docker cp $temporaryFile.FullName "${MysqlContainer}:$containerFile" | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "复制初始化 SQL 到 MySQL 容器失败"
        }
        & docker exec $MysqlContainer sh -c "mysql -u$MysqlUser -p$MysqlPassword --default-character-set=utf8mb4 $Database < $containerFile" 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "完整初始化 SQL 执行失败"
        }
    } finally {
        Remove-Item -LiteralPath $temporaryFile.FullName -Force -ErrorAction SilentlyContinue
        & docker exec $MysqlContainer rm -f $containerFile 2>$null | Out-Null
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
    Initialize-TestDatabase $TestDatabase

    & "$PSScriptRoot/migrate-db.ps1" -Database $TestDatabase -MysqlContainer $MysqlContainer -MysqlUser $MysqlUser -MysqlPassword $MysqlPassword
    Assert-Equal $LASTEXITCODE 0 "迁移脚本应执行成功"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_order_log';") 1 "应创建订单日志表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale';") 1 "应创建售后表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'expire_time';") 1 "应新增订单过期时间字段"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND index_name = 'idx_expire_status';") 3 "订单过期索引应包含状态、支付状态和过期时间三列"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'before_order_status';") 1 "售后表应保留订单原状态"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name = 'cancel_time';") 1 "售后表应支持撤销时间"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale_item';") 1 "应创建售后商品明细表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'product_stock_log';") 1 "应创建商品库存流水表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log';") 1 "应创建后台操作审计表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'pay_notify_failure_log';") 1 "应创建支付通知失败审计表"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'member_address' AND index_name = 'uk_member_address_default_user';") 1 "每个用户只能有一个默认地址"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_order_logistics' AND column_name IN ('logistics_code','last_query_time','traces_json','query_message');") 4 "物流表应具备生产查询和缓存字段"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_permission' AND column_name = 'http_method';") 1 "RBAC 权限应区分 HTTP 方法"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_comment' AND column_name = 'order_id';") 1 "商品评价应关联已完成订单"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM product_sku sku LEFT JOIN (SELECT sku_id,SUM(change_quantity) ledger_stock FROM product_stock_log GROUP BY sku_id) l ON l.sku_id=sku.id WHERE sku.deleted=b'0' AND sku.stock<>COALESCE(l.ledger_stock,0);") 0 "SKU 库存应可由库存流水完整重算"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260811_14';") 1 "应执行最新企业加固迁移"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM schema_migration_history WHERE version = '20260812_01';") 1 "应执行退款可靠性与订单完成时间迁移"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_order' AND column_name = 'finish_time';") 1 "订单表应记录不可变完成时间"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM trade_order WHERE status = 3 AND finish_time IS NULL;") 0 "历史已完成订单应回填完成时间"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND column_name IN ('refund_attempt_count','refund_last_attempt_time','refund_next_attempt_time','refund_claim_until','refund_last_error');") 5 "售后表应包含完整退款任务状态"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'trade_after_sale' AND index_name = 'idx_refund_retry';") "status,refund_next_attempt_time,refund_claim_until,refund_attempt_count,id" "退款待执行索引列顺序应正确"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT CHAR_LENGTH(password) FROM sys_admin_user WHERE username = 'admin' AND deleted = b'0';") 60 "默认管理员密码应使用完整 BCrypt 哈希"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_admin_user' AND column_name = 'avatar';") 1 "管理员资料结构应包含头像字段"
    Assert-Equal (Invoke-Mysql $TestDatabase "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'member_user' AND column_name = 'member_level';") 1 "会员表结构应包含会员等级"
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

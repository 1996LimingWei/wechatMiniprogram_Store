param([Parameter(Mandatory = $true)][string]$MysqlContainer)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
if ($env:GITHUB_ACTIONS -ne 'true' -or -not $env:RUNNER_TEMP) {
    throw '仅允许 GitHub 临时运行器执行交易演练，禁止连接客户数据库'
}
$root = Split-Path -Parent $PSScriptRoot
$process = $null
try {
    # 此工作库属于一次性 CI 容器，与客户及正式配置隔离。
    & docker cp (Join-Path $root 'sql/init.sql') "${MysqlContainer}:/tmp/ci-init.sql"
    & docker exec $MysqlContainer sh -c 'MYSQL_PWD=root mysql -uroot --default-character-set=utf8mb4 < /tmp/ci-init.sql'
    & "$PSScriptRoot/migrate-db.ps1" -Database shop -MysqlContainer $MysqlContainer
    if ($LASTEXITCODE -ne 0) { throw '交易演练初始化失败' }
    # 历史演示种子中的销量没有对应实付订单，不能用作一致性验收基线。
    # 仅清理本次 GitHub 临时容器中新建的库，保留系统权限与展示配置。
    $keep = @('sys_role', 'sys_permission', 'sys_role_permission', 'sys_admin_user',
        'sys_admin_user_role', 'schema_migration_history', 'shop_display_config')
    $tables = & docker exec $MysqlContainer sh -c 'MYSQL_PWD=root mysql -uroot shop -N -B -e "SHOW TABLES"'
    foreach ($table in $tables) {
        if ($table -notmatch '^[A-Za-z0-9_]+$') { throw '非预期表名' }
        if ($table -notin $keep) {
            & docker exec $MysqlContainer sh -c 'MYSQL_PWD=root mysql -uroot shop -e "DELETE FROM $1"' cleanup $table
        }
    }
    $jar = Get-ChildItem (Join-Path $root 'shop-backend/shop-server/target') -Filter '*.jar' | Select-Object -First 1
    if (-not $jar) { throw '缺少本次构建后端产物' }
    $outputLog = Join-Path $env:RUNNER_TEMP 'trade-runtime.log'
    $errorLog = Join-Path $env:RUNNER_TEMP 'trade-runtime.err'
    $process = Start-Process java -ArgumentList @('-jar', $jar.FullName, '--spring.profiles.active=dev', '--app.delivery-mode=commerce') `
        -RedirectStandardOutput $outputLog -RedirectStandardError $errorLog -PassThru
    $deadline = (Get-Date).AddSeconds(120)
    $ready = $false
    do {
        if ($process.HasExited) { throw '交易演练后端提前退出' }
        try {
            $response = Invoke-RestMethod 'http://127.0.0.1:8085/actuator/health' -TimeoutSec 3
            $ready = $response.status -eq 'UP'
        } catch { $ready = $false }
        if (-not $ready) { Start-Sleep -Seconds 2 }
    } while (-not $ready -and (Get-Date) -lt $deadline)
    if (-not $ready) { throw '交易演练后端未按时就绪' }
    & "$PSScriptRoot/verify-trade-flow.ps1" -MysqlContainer $MysqlContainer
    & "$PSScriptRoot/verify-commerce-consistency.ps1" -MysqlContainer $MysqlContainer
    Write-Host '交易 HTTP 流程与数据库一致性演练通过；本轮为隔离 Mock，不代表真实微信验收。'
} catch {
    if ($outputLog -and (Test-Path $outputLog)) { Get-Content $outputLog -Tail 100 }
    if ($errorLog -and (Test-Path $errorLog)) { Get-Content $errorLog -Tail 40 }
    throw
} finally {
    if ($process -and -not $process.HasExited) { Stop-Process -Id $process.Id -Force }
}

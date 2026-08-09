# Issue #17：订单搜索与数据库分页性能补强实施计划

> 对应规格：[订单搜索与数据库分页性能补强设计](../specs/2026-07-31-order-query-pagination-design.md)
> 对应 Issue：[GitHub #17](https://github.com/QtImM/wechatMiniprogram_Store/issues/17)
> 建议分支：`feat/order-query-pagination`
> 并行边界：不得修改 Issue #15 的 `shop-module-product/**` 和 `shop-miniapp/pages/goods/goods.vue`。

## 基线与完成定义

管理端订单列表已经使用数据库分页，并已支持订单号、用户 ID、手机号、订单状态和支付状态。本计划不得重新实现这些已完成能力。

完成本计划意味着：

- 补齐创建时间范围、可索引搜索语义、稳定排序和页大小保护。
- 用户端订单列表改为数据库分页。
- 管理端当前页关联数据改为批量读取。
- 只落地经 `EXPLAIN` 验证有效的查询索引。
- 与 Issue #15 文件级零交集。

## 文件范围

| 类别 | 计划修改/新增文件 |
| --- | --- |
| 管理端入口 | `shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/controller/admin/AdminTradeOrderController.java` |
| 用户端入口 | `shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/controller/AppOrderController.java` |
| 查询服务 | `TradeOrderService.java` 的列表查询区域；如抽出只读职责，则新增 `TradeOrderQueryService.java`，不得重构状态转换方法 |
| 批量装配 | 在 `shop-module-trade/service` 下新增只读装配类，直接批量读取订单商品、物流和售后 Mapper |
| 数据访问 | 复用现有 `TradeOrderMapper`、`TradeOrderItemMapper`、`TradeOrderLogisticsMapper`、`TradeAfterSaleMapper`，非必要不新增 XML SQL |
| 数据库 | 新增 `sql/migrations/V20260731_01__trade_order_query_indexes.sql`，同步 `sql/init.sql` |
| 测试 | 新增独立的订单查询服务测试和响应契约测试，不复用 Issue #14 的支付状态机测试文件 |
| 验收 | 新增 `scripts/verify-order-query.ps1`；同步扩展 `scripts/verify-db-migration.ps1` 的最小基线与查询索引断言 |
| 文档 | 本计划、对应规格和 `docs/superpowers/status.md` |

## 任务

- [x] 任务 1：同步安全基线并固化现有契约
  - PR #16 未合并时，以其 head 分支为安全基线并创建堆叠 PR；合并后再 rebase 或改回 `main`。
  - 确认迁移顺序为 `V20260730_03` 后执行 `V20260731_01`。
  - 为当前管理端和用户端列表响应补契约测试，记录改造前字段、金额格式、操作按钮、物流和售后空结构。
  - 只在订单查询区域解决 rebase 差异，不触碰支付、关闭、退款和库存代码。

- [x] 任务 2：补齐管理端查询契约
  - 新增 `createTimeStart` 和 `createTimeEnd`，按 `[start, end)` 构建条件。
  - 使用严格的 `yyyy-MM-dd HH:mm:ss` 解析；格式非法或 `start >= end` 时抛出 400 级业务错误。
  - `orderSn` 改为精确匹配；`mobile` 改为右侧通配的前缀匹配。
  - 保留 `orderId`、`userId`、`status`、`payStatus` 的精确匹配。
  - 页码归一到至少 1，页大小归一到 1～100。
  - 排序固定为 `create_time DESC, id DESC`。
  - 测试：覆盖各单条件、组合条件、时间边界、非法时间、相同创建时间稳定排序和超大页请求。

- [x] 任务 3：将用户端列表改为数据库分页
  - 保留现有 `showType` 到状态条件的映射。
  - 将 `selectList + subList` 替换为 MyBatis-Plus `selectPage`。
  - 使用与管理端一致的 `create_time DESC, id DESC` 排序和页大小上限。
  - 保持现有 `list`、`page`、`total` 响应字段及订单展示结构。
  - 测试：确认只查询当前页、总数正确、空页正确、退款状态筛选不回退为内存分页。

- [x] 任务 4：消除管理端列表 N+1 查询
  - 主订单分页完成后收集当前页 `orderIds`；空页直接返回。
  - 分别批量查询 `trade_order_item`、`trade_order_logistics`、`trade_after_sale`。
  - 商品按订单分组；物流和售后按 `update_time DESC, id DESC` 选择每单最新记录。
  - 将批量数据装配回现有列表响应，保持原始订单排序和 JSON 字段。
  - 测试：用多订单、多商品、多条物流/售后历史验证分组和最新记录选择；断言关联查询次数为常数，不随页内订单数增加。

- [x] 任务 5：用代表性数据确定并落地索引
  - 在隔离数据库准备覆盖无筛选、用户、手机号、状态、支付状态和时间范围的代表性订单数据。
  - 对规格中的候选索引逐一执行 `EXPLAIN`，记录 `key`、`type`、`rows` 和 `Extra`。
  - 只将有明确收益的索引写入 `V20260731_01__trade_order_query_indexes.sql`，不得重复创建 `uk_order_sn`、`idx_user_id`、`idx_status`。
  - 迁移使用“索引不存在时才创建”的安全方式，并同步 `sql/init.sql`。
  - 在 `scripts/verify-order-query.ps1` 断言迁移版本、索引列顺序、重复迁移幂等和主要查询计划。

- [x] 任务 6：执行回归验证
  - 执行 `cd shop-backend && mvn test -pl shop-module-trade -am`。
  - 执行 `cd shop-backend && mvn clean install -DskipTests`。
  - 在隔离 MySQL 8.0 环境执行 `powershell -ExecutionPolicy Bypass -File scripts/verify-order-query.ps1`；不得连接现有业务库。
  - 对比管理端和用户端列表改造前后的响应字段，确认没有接口契约回归。

- [x] 任务 7：更新交付记录
  - 在 `status.md` 记录最终新增索引、测试命令、查询计划和结果。
  - 提交信息使用中文，例如：`feat: 补强订单搜索与数据库分页`。
  - 推送前确认 `git diff --name-only origin/main...HEAD` 不包含 Issue #15 的任何文件。

## 实施验收结果

- 查询职责已抽到 `TradeOrderQueryService`，管理端与用户端都使用数据库分页，并统一按 `create_time DESC, id DESC` 排序。
- 当前页商品、物流和售后由 `TradeOrderListAssembler` 各批量查询一次；空页不会查询关联表。
- JDK 25 下执行 `mvn test -pl shop-module-trade -am`，商品模块 7 项、交易模块 18 项测试通过。
- JDK 25 下执行 `mvn clean install -DskipTests`，11 个 Maven 模块构建通过。
- 在一次性 MySQL 8.0.31 实例执行通用迁移验收和订单查询专项验收，五个版本首次执行、重复执行与校验和检查均通过。
- 两万条订单数据下，创建时间、用户、手机号前缀、状态+支付状态、支付状态和订单号查询分别命中约定索引，均未发生全表扫描。

## 风险与协作规则

- Issue #15 正在独立 worktree 中修改商品模块和商品详情页；本分支不得进入该 worktree，也不得复制其未提交变更。
- PR #16 与本 Issue 都可能触及 `TradeOrderService.java` 和 `sql/init.sql`，但业务区域不同。合并顺序固定为 PR #16 在前，本分支 rebase 后再合并。
- `V20260731_01` 不能先部署到共享数据库后再补跑 `V20260730_03`。
- 不为追求“索引齐全”一次性添加所有候选索引；没有代表性查询和 `EXPLAIN` 证据的索引不落地。
- 不引入游标分页、搜索引擎、全文索引或新的 ORM 依赖。
- 本 Issue 不顺带修复订单详情、售后列表分页或库存边界，它们应保持独立 Issue。

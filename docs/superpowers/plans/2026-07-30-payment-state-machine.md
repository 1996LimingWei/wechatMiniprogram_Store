# Issue #14：支付状态机与异常幂等性实施计划

> 对应规格：[支付状态机与异常幂等性设计](../specs/2026-07-30-payment-state-machine-design.md)
> 建议分支：`feat/payment-state-machine`
> 并行边界：不修改 `shop-module-product`、`shop-miniapp`、首页内容、收藏、足迹、评论及其迁移文件。

## 文件范围

| 类别 | 计划修改/新增文件 |
| --- | --- |
| 支付服务 | `shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/PayOrderService.java` |
| 订单协作 | `shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/TradeOrderService.java` |
| 日志与售后协作 | `TradeOrderLogService.java`、`TradeAfterSaleService.java`（仅支付状态转换调用点） |
| 数据库 | 新增 `sql/migrations/V20260730_03__pay_order_state_machine.sql`；同步 `sql/init.sql` 状态注释 |
| 测试 | 新增或扩展 `shop-module-trade` 的支付状态机服务测试 |
| 文档 | 本计划、对应规格和 `docs/superpowers/status.md` |

不得修改 Issue #13 的文件范围：`shop-module-product/**`、`shop-miniapp/**`、首页/用户互动迁移及其测试。

## 任务

- [x] 任务 1：固化状态契约与迁移
  - 新增 `V20260730_03__pay_order_state_machine.sql`，仅通过可重复执行的增量 SQL 补齐支付状态注释、必要索引或约束；不得修改既有迁移文件。`V20260730_02` 已被主干的用户互动迁移占用。
  - 在状态常量或受限的转换方法中统一支付单 0/1/2/3 的含义，消除订单与支付单状态混用。
  - 验证：连续执行两次 `./scripts/migrate-db.ps1 -Database shop` 不重复变更。

- [x] 任务 2：收敛预支付与支付成功转换
  - `prepay` 仅创建或复用待支付支付单，并校验订单归属、订单可支付状态和金额。
  - 将支付成功处理收敛为单一事务：校验支付单金额与订单实付金额后，原子更新支付单和订单。
  - 对已支付回调实现幂等成功返回；对已关闭、已退款或订单不可支付的回调明确拒绝。
  - 验证：重复回调不重复更新订单，也不重复写入 `PAY_SUCCESS` 日志。

- [x] 任务 3：联动关闭与退款状态
  - 在用户取消和超时关闭未支付订单的现有事务中关闭对应待支付支付单。
  - 在现有退款完成路径中将支付单转换为“已退款”，并保证重复退款通知幂等。
  - 验证：关闭后的支付成功请求不能把订单恢复为待发货；退款后支付单为“已退款”。

- [x] 任务 4：补齐状态矩阵测试与回归验证
  - 覆盖正常支付、重复支付成功、订单关闭后回调、金额不一致、支付与超时关闭竞争、重复退款六类场景。
  - 断言订单、支付单、订单日志的最终状态与日志数量。
  - 执行 `cd shop-backend && mvn test -pl shop-module-trade -am`，再执行 `mvn clean install -DskipTests`。

- [x] 任务 5：更新交付记录
  - 在 `status.md` 记录迁移版本、测试命令及结果。
  - 提交信息使用中文，例如：`feat: 收敛支付状态机与回调幂等`。

## 风险与协作规则

- 订单状态转换仅通过已有交易服务入口完成，避免支付服务绕过订单日志或库存回补逻辑。
- 若与交易侧其他分支同时调整 `TradeOrderService.java`，以新增窄方法和小提交降低合并冲突；不得将 Issue #13 的变更带入本分支。
- 微信支付 V3 回调接入前，Mock 成功接口继续作为同一状态机的测试适配器，不新增前端功能。
- 本机 Docker 守护进程不可用，因此隔离数据库迁移脚本未在本次本地执行；脚本已按主干迁移文件数断言历史记录，合并后应在具备 Docker 的 CI 或开发环境执行一次。

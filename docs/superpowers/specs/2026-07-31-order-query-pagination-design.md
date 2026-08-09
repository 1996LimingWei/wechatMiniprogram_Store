# 订单搜索与数据库分页性能补强设计

> 日期：2026-07-31
> 类型：交易 P1 设计规格
> 对应 Issue：[GitHub #17](https://github.com/QtImM/wechatMiniprogram_Store/issues/17)
> 编号说明：GitHub 的 #16 已被支付状态机 PR 占用，因此用户口头所称“下一项 Issue 16”顺延为仓库 Issue #17。

## 1. 背景与目标

交易 P0 已提供管理端订单列表，并完成了部分搜索和数据库分页。本 Issue 不重复已有能力，只补齐当前仍会影响大数据量查询稳定性的差量。

| 能力 | 当前状态 | 本 Issue 的处理 |
| --- | --- | --- |
| 管理端数据库分页 | 已使用 MyBatis-Plus `selectPage` | 保留，不重复搭建分页框架 |
| 订单号、用户、手机号、订单状态、支付状态筛选 | 已支持 | 收敛查询语义，确保可命中索引 |
| 创建时间范围 | 未支持 | 新增闭开区间筛选 |
| 跨页稳定排序 | 仅按创建时间倒序 | 增加 ID 倒序作为唯一兜底 |
| 页大小保护 | 无上限 | 限制为 1～100 |
| 用户端订单分页 | 先查全量再内存截取 | 改为数据库分页 |
| 管理端关联数据装配 | 每张订单分别查询商品、物流、售后 | 改为按当前页订单 ID 批量读取 |
| 查询索引 | 只有基础单列索引和超时任务索引 | 根据查询矩阵和 `EXPLAIN` 补强 |

目标是在不改变现有接口路径和响应字段的前提下，使管理端与用户端订单列表具备稳定、有限、可解释的数据库查询行为。

## 2. 查询契约

管理端继续使用 `/admin-api/trade/order/list`，兼容现有请求体和查询参数传参方式。

| 参数 | 语义 | 规则 |
| --- | --- | --- |
| `page` | 页码 | 默认 1，小于 1 时归一为 1 |
| `size` | 页大小 | 默认 10，归一到 1～100 |
| `orderId` | 订单主键 | 精确匹配 |
| `orderSn` | 订单号 | 精确匹配，不再使用包含匹配 |
| `userId` | 用户 ID | 精确匹配 |
| `mobile` | 收货手机号 | 仅允许前缀匹配，不添加前导通配符 |
| `status` | 订单状态 | 精确匹配 |
| `payStatus` | 支付状态 | 精确匹配 |
| `createTimeStart` | 创建时间下界 | 包含，格式为 `yyyy-MM-dd HH:mm:ss` |
| `createTimeEnd` | 创建时间上界 | 不包含，格式为 `yyyy-MM-dd HH:mm:ss` |

时间参数为空时不参与过滤；格式非法或 `createTimeStart >= createTimeEnd` 时返回 400 级业务错误，不静默忽略。

本 Issue 不继续支持订单号和手机号的任意位置包含搜索。若未来必须按手机号后四位搜索，应新增专用规范化字段或搜索能力，不应通过 `%关键词%` 绕过索引。

## 3. 分页与排序

管理端和用户端列表都必须先在 `trade_order` 上完成数据库筛选、总数统计和分页，再装配当前页关联数据。

统一排序为：

```sql
ORDER BY create_time DESC, id DESC
```

`id` 是相同创建时间下的唯一稳定排序键，可避免相邻分页请求出现重复或漏单。本阶段继续使用页码分页，不引入游标分页。

用户端保持当前 `showType` 到订单状态的映射和响应结构，只将 `selectList + subList` 替换为数据库分页。

## 4. 当前页批量装配

主订单分页完成后，以当前页 `orderIds` 为输入，分别批量查询：

1. `trade_order_item`
2. `trade_order_logistics`
3. `trade_after_sale`

批量结果在内存中按 `order_id` 分组。物流和售后存在多条历史记录时，按 `update_time DESC, id DESC` 取每个订单的最新一条。

一页列表的查询次数必须保持常数级：

1. 分页总数查询
2. 当前页主订单查询
3. 当前页商品明细批量查询
4. 当前页物流批量查询
5. 当前页售后批量查询

不得继续出现“每增加一张订单就额外执行三次 SQL”的行为。空页不得发起关联表查询。

现有订单列表 JSON 字段、金额格式、`handleOption`、物流和售后摘要保持兼容，不在本 Issue 内重做订单详情。

## 5. 索引与迁移

新增迁移：

`sql/migrations/V20260731_01__trade_order_query_indexes.sql`

版本选择依据：

- 主干已有 `V20260730_02__user_interaction_schema.sql`。
- PR #16 已占用 `V20260730_03__pay_order_state_machine.sql`。
- Issue #15 明确不修改迁移。
- `V20260731_01` 与以上版本均不重复。

迁移合并和部署前必须保证 `V20260730_03` 先执行，再执行 `V20260731_01`，避免 Flyway 将后补的低版本判定为 out-of-order。

已有索引继续复用，不重复创建：

- 主键 `PRIMARY(id)`
- `uk_order_sn(order_sn)`
- `idx_user_id(user_id)`
- `idx_status(status)`
- 超时任务专用 `idx_expire_status(status, pay_status, expire_time)`

组合索引如下，均已在代表性数据集上经 `EXPLAIN` 验证后保留：

| 查询路径 | 最终索引 |
| --- | --- |
| 无筛选或仅时间范围倒序 | `(create_time, id)` |
| 用户订单倒序 | `(user_id, create_time, id)` |
| 手机号前缀筛选 | `(mobile, create_time, id)` |
| 订单状态及状态+支付状态筛选 | `(status, pay_status, create_time, id)` |
| 仅支付状态筛选 | `(pay_status, create_time, id)` |

不得新增重复的订单号索引、全文索引、覆盖全部返回列的宽索引或与验收查询无关的预优化索引。

## 6. 兼容性与错误处理

- 接口路径、请求方式兼容现状。
- 已有 `orderId`、`userId`、`status`、`payStatus` 参数继续可用。
- `orderSn` 从包含匹配收敛为精确匹配，`mobile` 从包含匹配收敛为前缀匹配；这是为索引可用性做出的明确契约调整。
- 空结果返回空列表和总数 0，不抛异常。
- 关联表缺失记录时继续返回当前的空物流/空售后结构。
- 批量装配不得改变订单顺序。

## 7. 范围与并行边界

本 Issue 只涉及：

- `shop-module-trade` 的订单列表查询、批量装配和测试
- `sql/init.sql`
- 独立的 `V20260731_01__trade_order_query_indexes.sql`
- 独立订单查询验收脚本和文档

明确不涉及：

- `shop-module-product/**`
- `shop-miniapp/pages/goods/goods.vue`
- 多规格 SKU 读模型和库存可售性
- 库存扣减、回补和商品快照边界
- 支付、订单关闭、退款状态转换
- 订单详情补字段和管理后台页面
- 优惠券、微信支付、真实物流

因此本 Issue 与 Issue #15 的业务代码和文件路径均无交集，可并行实施。公共 `status.md` 由后合并分支 rebase 后追加记录。

## 8. 验收标准

- 管理端已有筛选继续可用，创建时间按闭开区间过滤。
- 相同创建时间的订单跨页查询不重复、不漏项。
- 页大小不会超过 100。
- 用户端列表使用数据库分页，不再读取用户全部订单。
- 当前页关联查询次数不随订单数线性增长，列表响应契约保持兼容。
- 代表性数据集的主要查询路径有 `EXPLAIN` 结果，新增索引均有明确命中场景。
- 新迁移首次执行成功，重复执行不会重复创建索引。
- `mvn test -pl shop-module-trade -am` 和全量跳过测试构建均通过。

## 9. 实施记录

- 实现分支以尚未合并的 `feat/payment-state-machine` 为基线，避免遗漏 `V20260730_03`，并以堆叠 PR 方式隔离本 Issue 的审查差异。
- 在一次性 MySQL 8.0.31 实例运行全部迁移与重复执行校验，`V20260730_03` 先于 `V20260731_01` 执行，历史记录和五个索引列顺序均符合预期。
- 两万条代表性订单的 `EXPLAIN` 结果中：创建时间查询命中 `idx_create_time_id`，用户列表命中 `idx_user_create_time_id`，高选择性手机号前缀命中 `idx_mobile_create_time_id`，状态组合命中 `idx_status_pay_create_time_id`，支付状态命中 `idx_pay_status_create_time_id`，订单号精确查询复用 `uk_order_sn`。
- 手机号前缀范围查询在高选择性场景会额外排序，但只扫描目标范围，不执行全表扫描；短前缀场景允许优化器选择创建时间索引后过滤。

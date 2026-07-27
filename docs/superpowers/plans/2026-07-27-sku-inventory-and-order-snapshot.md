# SKU 库存与订单商品快照实施计划

> **面向执行 Agent：** 必须使用 `superpowers:executing-plans` 逐项执行并更新复选框。

**目标：** 让订单提交、取消和超时关闭基于真实 SKU 原子库存，并保存真实商品快照。

**架构：** 商品模块新增库存服务封装 SPU/SKU 查询和条件更新；交易模块通过该服务获取快照和变更库存，订单关闭从订单项读取 SKU ID 回补。现有订单项字段已满足快照要求，因此不新增数据库迁移。

**技术栈：** Java 25、Spring Boot 3.5、MyBatis-Plus、JUnit 5、MySQL 8。

---

### Task 1：商品模块库存服务

**文件：**

- 新建：`shop-backend/shop-module-product/src/main/java/com/shop/module/product/service/ProductInventoryService.java`
- 新建：`shop-backend/shop-module-product/src/test/java/com/shop/module/product/service/ProductInventoryServiceTest.java`
- 修改：`shop-backend/shop-module-product/src/main/java/com/shop/module/product/dal/mysql/ProductSkuMapper.java`

- [x] 先编写失败测试：验证下架 SPU、SKU 归属不符和库存不足均拒绝；验证扣减只更新指定 SKU。
- [x] 运行 `mvn -pl shop-module-product -am -Dtest=ProductInventoryServiceTest test`，确认因缺少服务而失败。
- [x] 实现 `ProductInventoryService`：查询上架 SPU 与归属 SKU，解析规格 JSON，使用 `stock >= count` 条件更新扣减和按 SKU 回补。
- [x] 再次运行同一测试，确认通过。

### Task 2：交易模块切换真实 SKU 快照

**文件：**

- 修改：`shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/TradeProductService.java`
- 修改：`shop-backend/shop-module-trade/src/main/java/com/shop/module/trade/service/TradeOrderService.java`
- 新建：`shop-backend/shop-module-trade/src/test/java/com/shop/module/trade/service/TradeProductServiceTest.java`

- [ ] 先编写失败测试：交易快照必须携带真实 SKU ID、价格、图片和规格，且不得访问 `MockData`。
- [ ] 运行 `mvn -pl shop-module-trade -am -Dtest=TradeProductServiceTest test`，确认失败。
- [ ] 将 `TradeProductService` 改为委托 `ProductInventoryService`；下单扣减和订单关闭回补均按订单项 `skuId` 调用。
- [ ] 运行交易模块测试，确认通过。

### Task 3：全量验证与交付

**文件：**

- 修改：`docs/superpowers/status.md`

- [ ] 运行 `mvn clean test` 和 `mvn clean install -DskipTests`。
- [ ] 启动本地后端，验证指定 SKU 下单只影响该 SKU、取消订单回补一次、退款不回补。
- [ ] 更新状态仪表盘，记录退款等待未来退货入库流程。
- [ ] 提交中文 commit 并推送 `feat/sku-inventory-snapshot` 分支。

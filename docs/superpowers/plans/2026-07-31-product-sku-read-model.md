# Issue #15 商品多规格与库存可售性读模型实施记录

> 规格：[2026-07-31-product-sku-read-model.md](../specs/2026-07-31-product-sku-read-model.md)
> 分支：`feat/product-sku-read-model`

## 已完成任务

- [x] 解析 SKU 全部 `properties`，按规格和规格值稳定去重排序。
- [x] 输出完整 SKU 矩阵，同时保留旧字段兼容性。
- [x] 对空、非法和非整数规格 ID 安全降级，并覆盖单元测试。
- [x] 小程序按精确 SKU 更新价格、图片、库存和数量上限，禁用缺货组合。
- [x] 兼容旧接口/Mock 的 `goodsSpecificationIds`、`goodsNumber`，快捷搭配加购不再绕过当前 SKU 校验。
- [x] 执行商品模块测试与真实详情接口冒烟验证。

## 验证记录

- `cd shop-backend && mvn test -pl shop-module-product -am`：8 个测试全部通过。
- 直接 MySQL/Redis 环境启动后，`GET /app-api/goods/detail?id=1` 返回 `code=0`，并包含兼容字段、完整 SKU 标识、库存、可售状态、SKU 价格与图片字段。

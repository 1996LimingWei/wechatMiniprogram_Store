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

- Docker 全量构建通过：11 个 Maven 模块构建成功，商品模块 15 项测试、交易模块 2 项测试全部通过。
- `scripts/verify-product-sku.ps1` 在独立后端容器和真实 MySQL/Redis 环境通过，覆盖完整矩阵、稳定排序、缺货与损坏属性降级，并自动清理临时数据。
- `scripts/verify-miniapp-sku.js` 使用 HBuilderX 内置 Node 18 通过，覆盖精确维度匹配、缺货、图片回退、旧字段和空规格兼容。

## 主干同步后的收口任务

- [x] 合并最新 `main`，兼容 Issue #22 新增的商品搜索服务依赖。
- [x] 保证 SKU 矩阵不依赖数据库返回顺序，并拒绝重复维度、部分非法属性污染规格列表。
- [x] 复核小程序精确组合、缺货禁用、价格图片切换和数量上限逻辑。
- [x] 完成最新商品模块测试、Docker 全量构建与多规格详情 HTTP 验收。
- [x] 更新状态、推送并合并 PR #18，关闭 Issue #15。

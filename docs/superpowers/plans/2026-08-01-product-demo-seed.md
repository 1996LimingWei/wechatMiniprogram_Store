# Issue #24 商品内容演示种子与自动验收数据集实施计划

> 规格：[2026-08-01-product-demo-seed.md](../specs/2026-08-01-product-demo-seed.md)
> 分支：`feat/product-demo-seed`

## 实施任务

- [x] 盘点初始化 SQL、现有迁移、商品内容表和迁移验收脚本。
- [x] 新增稳定 ID、可强制重放的商品内容演示种子迁移。
- [x] 覆盖分类、上下架、搜索、多规格、部分缺货、全部缺货和评论场景。
- [x] 扩展隔离数据库迁移验收，验证关联完整与强制重放幂等。
- [x] 新增商品内容 HTTP 验收，覆盖首页、分类、搜索、详情和 SKU 可售性。
- [x] 更新本地启动与数据重建说明。
- [x] 完成商品模块测试、Docker 全量构建与真实环境验收。
- [ ] 更新状态、提交推送、创建并合并 PR，关闭 Issue #24。

## 验证命令

- `scripts/verify-db-migration.ps1`
- `mvn test -pl shop-module-product -am`
- `docker compose build --progress plain backend`
- `scripts/verify-product-demo-seed.ps1`

## 验证记录

- 商品模块及依赖测试通过：商品模块 23 项测试。
- Docker 后端镜像全量构建通过：11 个 Maven 模块，交易模块 2 项测试。
- 隔离数据库全部迁移、强制重放、重复执行和 UTF-8 中文种子断言通过。
- D 盘持久化 MySQL/Redis 的首页、分类、搜索、详情、SKU 与评论 HTTP 验收通过。

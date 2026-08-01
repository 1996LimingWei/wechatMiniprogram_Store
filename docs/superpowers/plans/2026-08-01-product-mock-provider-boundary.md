# Issue #23 商品内容 Mock Provider 与正式 API 边界实施计划

> 规格：[2026-08-01-product-mock-provider-boundary.md](../specs/2026-08-01-product-mock-provider-boundary.md)
> 分支：`feat/product-mock-provider-boundary`

## 实施任务

- [x] 盘点 `MockData`、`AppMockController`、正式商品/内容路径与现有测试。
- [x] 将商品种子迁移至独立 Fixture，解除 SKU Provider 对 Controller 包的依赖。
- [x] 建立 Mock/数据库商品目录 Provider 契约和按配置选择的路由服务。
- [x] 将正式热销、新品、品牌、专题与通用支持路径迁出 `AppMockController`。
- [x] 为 `/app-api/mock/**` 增加开发开关与生产环境强制拒绝守卫。
- [x] 补齐 Provider、响应兼容和环境守卫测试。
- [x] 完成模块测试、Docker 全量构建与 Mock/数据库双模式 HTTP 验收。
- [ ] 更新状态、提交推送、创建并合并 PR，关闭 Issue #23。

## 验证命令

- `mvn test -pl shop-module-product,shop-module-trade -am`
- `docker compose build --progress plain backend`
- `scripts/verify-product-provider.ps1`

## 验证记录

- Docker 11 模块全量构建通过，商品模块 23 项、交易模块 2 项测试全部通过。
- `scripts/verify-product-provider.ps1` 输出 `PRODUCT_PROVIDER_ACCEPTANCE_OK`。
- Mock 正式路径、数据库正式路径、正式品牌/专题接口和生产环境 Mock 拒绝均完成真实 HTTP 验收。
- 三个临时验收容器均已删除，未产生数据库临时数据。

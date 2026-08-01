# Issue #25 小程序商品正式 API 收口与端到端验收实施计划

> 规格：[2026-08-01-miniapp-product-api-acceptance.md](../specs/2026-08-01-miniapp-product-api-acceptance.md)
> 分支：`feat/miniapp-product-api-acceptance`

## 实施任务

- [x] 核对 Issue #25、前置任务和小程序商品内容调用现状。
- [x] 删除商品内容本地 Mock 请求分支与 `utils/mock.js`。
- [x] 清理商品详情硬编码业务数据和列表固定 SKU 快捷加购。
- [x] 补齐正式请求失败、空态与登录鉴权反馈。
- [x] 修正小程序 README 的真实后端运行说明。
- [x] 新增商品正式 API HTTP 回归和小程序静态边界验收。
- [x] 完成后端回归、Docker HTTP 验收和 HBuilderX 编译检查。
- [x] 更新状态、提交推送、创建并合并 PR，关闭 Issue #25。

## 验证命令

- `node scripts/verify-miniapp-product-api.js`
- `node scripts/verify-miniapp-sku.js`
- `scripts/verify-miniapp-product-flow.ps1`
- `mvn test -pl shop-module-product -am`
- HBuilderX 微信小程序编译命令

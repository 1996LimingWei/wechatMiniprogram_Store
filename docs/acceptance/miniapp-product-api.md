# 小程序商品正式 API 验收清单

> 对应 Issue：[GitHub #25](https://github.com/QtImM/wechatMiniprogram_Store/issues/25)

## 验收环境

- 项目目录与 MySQL/Redis 持久化数据均位于 D 盘。
- Docker 服务：`shop-mysql`、`shop-redis`、`shop-backend`。
- 小程序：uni-app Vue2，使用 HBuilderX 编译到微信小程序。
- 后端地址：`http://127.0.0.1:8085/app-api/`。

## 自动验收

```powershell
node .\scripts\verify-miniapp-product-api.js
node .\scripts\verify-miniapp-sku.js
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify-miniapp-product-flow.ps1
```

自动验收覆盖商品内容 Mock 边界、正式 API 定义、页面脚本语法、首页内容、分类、搜索、二维多规格、部分缺货、登录、收藏、足迹、评论发表与数据清理。

## 本次验收结果（2026-08-01）

- `verify-miniapp-product-api.js`：通过，输出 `MINIAPP_PRODUCT_API_STATIC_OK`。
- `verify-miniapp-sku.js`：通过，输出 `MINIAPP_SKU_ACCEPTANCE_OK`。
- `verify-miniapp-product-flow.ps1`：通过，输出 `MINIAPP_PRODUCT_FLOW_ACCEPTANCE_OK`。
- `mvn test -pl shop-module-product -am`：通过，商品模块及依赖共执行 23 项测试，无失败或错误。
- HBuilderX 5.06 微信小程序编译：通过；微信开发者工具成功识别 AppID `wx34175bfa441e4316` 并打开项目。
- Docker：`shop-mysql`、`shop-redis` 健康，`shop-backend` 已按当前 `main` 镜像重建并通过 HTTP 全链路验收。

微信开发者工具中的视觉截图、真机授权和人工交互仍属于发布前人工验收，不作为本次自动化脚本的伪造输出。

## HBuilderX 与微信开发者工具验收

1. 在 HBuilderX 导入 `shop-miniapp`，运行到微信开发者工具。
2. 确认首页 Banner、频道、品牌、新品、热销和分类商品正常加载；切换数据库返回的分类 Tab。
3. 进入分类页和搜索页，验证有结果、无结果、联想词与登录后的搜索历史。
4. 打开商品 `240001`，验证两维规格、四个 SKU、一个缺货组合、价格/图片切换和数量上限。
5. 未登录点击收藏或发表评价，确认出现登录引导；登录后验证收藏、足迹和评论列表更新。
6. 停止后端后重新加载首页、分类和详情，确认出现明确错误提示且不展示本地伪造商品。
7. 恢复后端，确认重新加载成功，控制台没有阻断性错误。

建议截图：首页正式内容、分类空态、SKU 缺货组合、登录引导、评论发表结果和断网错误态各一张。截图属于人工交付证据，不提交包含个人信息的图片。

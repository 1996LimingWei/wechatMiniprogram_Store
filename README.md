# 药食同源微信小程序商城

面向药食同源、滋补食品等零售场景的微信小程序商城，包含用户端小程序、Java 后端和 Vue 管理后台。目前处于 v1.0 客户验收与交付阶段。

## 核心能力

- 商品：分类、SPU/SKU、素材、库存、批量导入导出与上下架
- 交易：购物车、结算、订单、支付、发货、物流、售后与退款
- 运营：Banner、频道、品牌、专题、优惠券、满减与包邮
- 管理：订单履约、支付退款异常、日终对账、角色权限与审计
- 运维：数据库迁移、健康检查、运行监控、备份恢复与交付门禁

## 技术栈

- 后端：Java 25、Spring Boot 3.5、MyBatis-Plus、MySQL 8、Redis 7
- 小程序：uni-app、Vue 2、HBuilderX、微信开发者工具
- 管理后台：Vue 3、Vite、TypeScript、Element Plus、vue-pure-admin
- 部署：Docker Compose、Nginx

## 项目结构

```text
├── shop-backend/       Java 后端
├── shop-miniapp/       微信小程序
├── shop-admin/         管理后台
├── sql/                初始化与增量迁移
├── scripts/            构建、验收和运维脚本
├── docs/               开发、部署、验收与交付文档
└── docker-compose.yml  本地开发环境
```

## 快速启动

### 1. 启动后端依赖与 API

首次使用时复制环境变量示例并按需修改：

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

已有数据库执行增量迁移：

```powershell
.\scripts\migrate-db.ps1 -Database shop
```

健康检查：<http://127.0.0.1:8085/actuator/health>

### 2. 启动管理后台

```powershell
Set-Location shop-admin
pnpm install
pnpm dev
```

访问 <http://127.0.0.1:8848>。本地开发默认账号为 `admin`，默认密码为 `admin123`，不得用于生产环境。

### 3. 启动微信小程序

1. 使用 HBuilderX 导入 `shop-miniapp`。
2. 选择“运行 → 运行到小程序模拟器 → 微信开发者工具”。
3. 本地开发时，在微信开发者工具中关闭合法域名校验。

小程序必须使用 HBuilderX 内置编译器；Node.js 24 与当前 uni-app Vue 2 CLI 构建模式不兼容。

完整本地开发说明见 [docs/local-development.md](docs/local-development.md)。

## 本地服务

| 服务 | 地址或端口 |
|------|------------|
| 管理后台 | <http://127.0.0.1:8848> |
| 后端 API | <http://127.0.0.1:8085> |
| MySQL | `127.0.0.1:3307` |
| Redis | `127.0.0.1:6380` |

## 验收

建议依次验收：环境与登录、商品运营、用户交易、订单履约、售后退款、财务对账、权限审计、监控运维。

- [客户验收清单](docs/deployment/v1.0-acceptance-checklist.md)
- [最终验收报告](docs/delivery/v1.0-final-acceptance-report.md)
- [小程序提审清单](docs/acceptance/v1.0-miniapp-review-checklist.md)
- [微信支付退款验收](docs/acceptance/v1.0-wechat-pay-refund-acceptance.md)
- [真实物流验收](docs/acceptance/v1.0-logistics-acceptance.md)

常用验证命令：

```powershell
.\scripts\verify-db-migration.ps1
.\scripts\verify-trade-flow.ps1
.\scripts\verify-commerce-consistency.ps1
.\scripts\verify-ci.ps1
```

## 项目文档

- [项目状态](docs/superpowers/status.md)
- [开发规范](docs/conventions.md)
- [v1.0 客户交付计划](docs/superpowers/plans/v1.0%20客户交付版.md)
- [客户交付手册](docs/delivery/v1.0-customer-delivery-guide.md)
- [生产部署说明](docs/deployment/v1.0-production-deployment.md)
- [运维手册](docs/deployment/v1.0-operations.md)

## 开发约定

项目采用 Spec-Driven 工作流：先读状态，按规格和计划开发，完成后执行验证并更新状态。所有开发规范以 [docs/conventions.md](docs/conventions.md) 为准。

生产部署前必须替换默认账号密码、配置 HTTPS 与客户正式渠道资料，并关闭 Mock 支付、退款和物流能力。

## 许可证

Private

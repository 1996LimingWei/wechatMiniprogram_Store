# 陕铺微信小程序商城

面向滋补食品零售场景的三端商城：微信小程序（用户端）、Vue 管理后台、Java 后端。
当前处于 v1.0 客户验收与交付阶段。

能力覆盖商品与库存、购物车与订单、支付与售后、营销与内容运营、订单履约与财务对账、后台权限与审计。

## 客户操作教程

- [陕铺后台图文操作教程（可编辑正文）](docs/delivery/shanpu-admin-guide/操作教程.md)：登录、名称说明、商品信息、价格图片、库存、上下架及可视化装修。
- [离线阅读版](docs/delivery/shanpu-admin-guide/操作教程.html)：下载教程整个文件夹后双击打开，支持目录导航和截图原图查看。
- [教程维护说明](docs/delivery/shanpu-admin-guide/维护说明.md)：编辑、更新阅读版和转发方法。

## 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Docker Desktop | 最新版 | 运行 MySQL、Redis 与后端（启动前必须已运行） |
| Java | 25 | 仅「本地 Maven 启动后端」时需要 |
| Node.js | ≥ 22.13 | 管理后台使用，推荐按 `shop-admin/.nvmrc` 执行 `nvm use` |
| pnpm | ≥ 9 | 管理后台包管理 |
| HBuilderX | App 开发版 | 编译 uni-app 小程序 |
| 微信开发者工具 | 最新版 | 运行小程序模拟器 |

## 快速启动

### 1. 启动后端

先确认 Docker Desktop 已启动，然后在仓库根目录执行：

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

首次启动会自动创建 `shop` 数据库并执行 `sql/init.sql`。旧库需要补齐增量表结构时执行迁移脚本：

```bash
# 需要 PowerShell（macOS：brew install --cask powershell）
pwsh ./scripts/migrate-db.ps1 -Database shop
```

健康检查 <http://127.0.0.1:8085/actuator/health> 返回 `{"status":"UP"}` 即成功。

**备选：本地 Maven 启动后端**（只跑 MySQL 和 Redis 容器，编译更快）

```bash
docker compose up -d mysql redis
cd shop-backend
export JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null || echo /usr/local/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home)
mvn spring-boot:run -pl shop-server
```

### 2. 启动管理后台

```bash
cd shop-admin
pnpm install
pnpm dev
```

访问 <http://127.0.0.1:8848>，默认账号 `admin` / `admin123`（仅限本地开发，生产必须替换）。

### 3. 启动微信小程序

1. 用 HBuilderX 导入 `shop-miniapp`，选择「运行 → 运行到小程序模拟器 → 微信开发者工具」。
2. 微信开发者工具中打开「设置 → 安全设置 → 服务端口」，否则 HBuilderX 无法通过 CLI 调起模拟器。
3. 在「详情 → 本地设置」勾选「不校验合法域名」。

小程序必须使用 HBuilderX 内置编译器；系统 Node.js 24 与当前 uni-app Vue 2 CLI 构建模式不兼容。

## 服务与端口

| 服务 | 地址 | 端口 |
|------|------|------|
| 管理后台 | <http://127.0.0.1:8848> | 8848 |
| 后端 API | <http://127.0.0.1:8085> | 8085 |
| MySQL | `127.0.0.1:3307` | 3307 |
| Redis | `127.0.0.1:6380` | 6380 |

## 常见问题

- **管理后台登录提示「系统异常」**：多数是后端没起来。先确认 Docker Desktop 已运行，再执行 `docker compose up -d mysql redis`，健康检查通过后刷新页面重试。
- **HBuilderX 报 `IDE service port disabled`**：微信开发者工具未开启服务端口，见上文「启动微信小程序」第 2 步；也可手动导入编译产物 `shop-miniapp/unpackage/dist/dev/mp-weixin`。
- **小程序接口请求失败**：确认后端 8085 可访问，且微信开发者工具已关闭合法域名校验。
- **端口被占用**：3307 / 6380 / 8085 / 8848 已被占用时，先停掉旧进程或修改 `.env`（容器端口）与 `shop-admin/.env.development`（前端端口）。

## 目录结构

```text
├── shop-backend/       Java 后端（Maven 多模块）
├── shop-admin/         Vue 3 管理后台
├── shop-miniapp/       uni-app 微信小程序
├── sql/                初始化脚本与增量迁移
├── scripts/            迁移、验收与运维脚本
├── docs/               开发、部署、验收与交付文档
└── docker-compose.yml  本地开发环境
```

## 更多文档

- [本地开发说明](docs/local-development.md)
- [开发规范](docs/conventions.md)
- [生产部署说明](docs/deployment/v1.0-production-deployment.md)
- [客户验收清单](docs/deployment/v1.0-acceptance-checklist.md)
- [项目状态](docs/superpowers/status.md)

生产部署前必须替换默认账号密码、配置 HTTPS 与客户正式渠道资料，并关闭 Mock 支付、退款和物流能力。

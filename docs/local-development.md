# 本地运行

## Docker 后端环境

前置条件：安装并启动 Docker Desktop（Windows 使用 WSL 2 后端）。

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://127.0.0.1:8085/app-api/product/category/list
```

首次启动会创建 `shop` 数据库并执行 `sql/init.sql`。MySQL 与 Redis 数据保存在 Docker 命名卷中；之后重启不会重复初始化数据。

常用命令：

```powershell
docker compose logs -f backend
docker compose down
docker compose down -v  # 删除本地数据库和 Redis 数据后重新初始化
```

开发 Compose 默认启用微信登录 Mock 模式。要使用真实微信登录，请在 `.env` 中保存密钥，并在 `backend.environment` 中改为传入 `WX_MA_APPID`、`WX_MA_SECRET` 以及 `WX_MA_MOCK_ENABLED=false`；不要将密钥提交到 Git。

## 小程序

Docker 仅运行后端依赖和 API，不会编译 uni-app。安装 HBuilderX（App 开发版）和微信开发者工具后，在 HBuilderX 导入 `shop-miniapp`，运行到微信开发者工具。微信开发者工具本地设置中勾选“不校验合法域名”。

当前 `shop-miniapp/utils/util.js` 已指向 `http://127.0.0.1:8085/` 并关闭前端 Mock，因此可直接请求上述后端。

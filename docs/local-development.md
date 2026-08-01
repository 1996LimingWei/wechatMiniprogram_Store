# 本地运行

## Docker 后端环境

前置条件：安装并启动 Docker Desktop（Windows 使用 WSL 2 后端）。

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
Invoke-RestMethod http://127.0.0.1:8085/app-api/product/category/list
```

首次启动会创建 `shop` 数据库并执行 `sql/init.sql`。随后执行增量迁移，补齐最新表结构与可重复的商品内容演示种子：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\migrate-db.ps1 -Database shop
```

MySQL 与 Redis 数据绑定保存在仓库 D 盘 `.docker-data`，之后重启不会重复初始化数据。迁移脚本通过版本和校验和跳过已执行版本。

常用命令：

```powershell
docker compose logs -f backend
docker compose down
```

需要重建演示数据库时，先确认当前目录是本仓库且无需保留本地数据，再执行：

```powershell
docker compose down
Remove-Item -Recurse -Force .\.docker-data
docker compose up -d --wait mysql redis
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\migrate-db.ps1 -Database shop
docker compose up -d --build backend
```

不要使用 `docker compose down -v` 代替清理：当前项目使用 D 盘绑定目录，不是 Docker 命名卷。

开发 Compose 默认启用微信登录 Mock 模式。要使用真实微信登录，请在 `.env` 中保存密钥，并在 `backend.environment` 中改为传入 `WX_MA_APPID`、`WX_MA_SECRET` 以及 `WX_MA_MOCK_ENABLED=false`；不要将密钥提交到 Git。

## 小程序

Docker 仅运行后端依赖和 API，不会编译 uni-app。安装 HBuilderX（App 开发版）和微信开发者工具后，在 HBuilderX 导入 `shop-miniapp`，运行到微信开发者工具。微信开发者工具本地设置中勾选“不校验合法域名”。

当前 `shop-miniapp/utils/util.js` 已指向 `http://127.0.0.1:8085/` 并关闭前端 Mock，因此可直接请求上述后端。

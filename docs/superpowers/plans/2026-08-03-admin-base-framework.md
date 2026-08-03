# 2026-08-03 管理后台基座搭建

## 目标

基于 [vue-pure-admin thin 版](https://github.com/pure-admin/pure-admin-thin) 搭建 `shop-admin/` 管理后台前端，完成项目基座、API 对接层和路由骨架，为后续各业务模块并行开发打基础。

## 技术选型

| 项目 | 选择 |
|------|------|
| 框架 | vue-pure-admin thin（非国际化精简版） |
| 技术栈 | Vue 3 + Vite + Element Plus + TypeScript + Pinia + Tailwind CSS |
| 包管理 | pnpm |
| 后端对接 | Axios 拦截器统一处理 `{code: 0, msg, data}` 响应格式 |
| 认证方式 | 单 Token（JWT），无 refreshToken，请求头 `Authorization: Bearer <token>` |

## 已完成内容

### 项目初始化
- 克隆 pure-admin-thin 到 `shop-admin/`，移除 `.git` 融入主仓库
- pnpm install 安装 696 个依赖包
- TypeScript 类型检查 0 错误

### 开发代理配置
- `.env.development` 添加 `VITE_PROXY_API=/admin-api` → `VITE_PROXY_TARGET=http://localhost:8085`
- `vite.config.ts` 配置 proxy，开发环境自动代理到后端
- 修改根目录 `.gitignore`：移除 `.env` 全局忽略，避免误忽略 `shop-admin/.env`

### 认证体系简化
- `src/utils/auth.ts`：重写为单 Token 模式，移除 refreshToken/expires 复杂度
- `src/utils/http/index.ts`：
  - 请求拦截：自动附加 `Authorization: Bearer <token>`，白名单放行登录接口
  - 响应拦截：解包 `{code: 0, msg, data}`，`code !== 0` 弹 `ElMessage.error`，401 跳转登录
- `src/store/modules/user.ts`：`loginByUsername` 直接调用 `/admin-api/auth/login`
- `src/views/login/index.vue`：登录成功后直接跳转首页，移除动态路由 `initRouter` 调用
- `src/utils/sso.ts`：简化为空（项目不需要 SSO）

### API 接口骨架（`src/api/`）

| 文件 | 对接后端接口 | 状态 |
|------|-------------|------|
| `user.ts` | `POST /admin-api/auth/login` | 已有后端 |
| `product.ts` | `/admin-api/product/spu/page\|create\|update\|delete` | 已有后端 |
| `category.ts` | `/admin-api/product/category/list\|create\|update\|delete` | 已有后端 |
| `order.ts` | `/admin-api/trade/order/list\|detail\|ship` | 已有后端 |
| `afterSale.ts` | `/admin-api/trade/after-sale/list\|approve\|reject` | 已有后端 |
| `content.ts` | `/admin-api/content/{banner\|channel\|brand\|topic}/*` | **待后端补充** |
| `member.ts` | `/admin-api/member/user/page\|detail` + `/admin-api/product/comment/*` | **待后端补充** |
| `dashboard.ts` | `/admin-api/dashboard/summary\|order-trend\|order-status\|top-products` | **待后端补充** |
| `types.ts` | 221 行 TypeScript 类型定义（ProductSpu, Category, TradeOrder 等 15 个接口） | 新建 |

### 路由骨架（`src/router/modules/`）

| 路由文件 | 菜单 | 子页面 |
|----------|------|--------|
| `home.ts` | 首页 | 数据看板 |
| `product.ts` | 商品管理 | 商品列表、分类管理 |
| `trade.ts` | 交易管理 | 订单管理、售后管理 |
| `content.ts` | 内容管理 | Banner、频道、品牌、专题 |
| `member.ts` | 会员中心 | 会员列表、评论管理 |

### 清理
- 删除 `mock/` 目录全部 mock 数据
- 删除 `src/views/permission/` 演示权限页面
- 删除 `src/views/welcome/` 旧首页
- `src/api/routes.ts` 重写为返回空数组（纯静态路由，不需要后端动态路由）
- 平台标题改为"药食同源管理后台"，主题色改为莫兰迪绿 `#6b8f7a`

## 验收结果

- `pnpm dev` 启动无报错，运行在 `http://localhost:8848`
- `npx vue-tsc --noEmit` TypeScript 检查 0 错误
- 登录页面正常展示，左侧菜单可见全部 5 个一级菜单
- API 代理配置正确（后端未启动时显示 proxy error，属预期行为）

## 后续并行开发

| Issue | 负责模块 | 依赖 |
|-------|----------|------|
| #2 | 管理员登录页定制 + 基础框架主题 | #1 |
| #3 | 商品管理（分类树 + SPU 列表 + SKU 编辑） | #2 |
| #4 | 订单管理（列表 + 详情 + 发货 + 物流） | #2 |
| #5 | 售后管理（列表 + 审批/拒绝） | #2 |
| #6 | 内容运营（Banner + 频道 + 品牌 + 专题） | #2 |
| #7 | 会员中心（用户列表 + 评论管理） | #2 |
| #8 | 数据看板首页 | #3~#7 部分完成 |
| #9 | 构建优化与 Nginx 部署 | 全部完成 |

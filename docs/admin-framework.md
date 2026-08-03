# 管理后台技术概要

> shop-admin 管理后台前端的技术架构简要说明

## 项目定位

为药食同源商城小程序提供后台管理能力：商品编辑、订单处理、售后审批、内容运营、会员查看和数据看板。

## 技术栈

- **框架**：[vue-pure-admin thin](https://github.com/pure-admin/pure-admin-thin) v6.2.0
- **核心**：Vue 3 + Vite 7 + TypeScript 5.9 + Element Plus 2.11
- **状态**：Pinia | **样式**：Tailwind CSS 4 + SCSS | **请求**：Axios
- **包管理**：pnpm | **端口**：8848

## 项目结构

```
shop-admin/
├── src/
│   ├── api/              # 接口层（按业务模块拆分）
│   │   ├── user.ts       # 管理员登录
│   │   ├── product.ts    # 商品 SPU CRUD
│   │   ├── category.ts   # 分类 CRUD
│   │   ├── order.ts      # 订单列表/详情/发货
│   │   ├── afterSale.ts  # 售后审批
│   │   ├── content.ts    # Banner/频道/品牌/专题
│   │   ├── member.ts     # 会员/评论
│   │   ├── dashboard.ts  # 数据看板
│   │   └── types.ts      # 全局 TypeScript 类型
│   ├── views/            # 业务页面（各模块占位）
│   ├── router/modules/   # 静态路由（5 个菜单模块）
│   ├── store/modules/    # Pinia 状态管理
│   └── utils/http/       # Axios 封装（响应解包 + Token + 错误处理）
├── .env.development      # 开发代理：/admin-api → localhost:8085
└── vite.config.ts        # Vite 配置（proxy + 构建优化）
```

## 后端对接约定

- **响应格式**：后端统一返回 `{code: 0, msg, data}`，前端 Axios 拦截器自动解包 `data`，`code !== 0` 弹出错误提示
- **认证**：登录后获取 JWT Token，请求头自动附加 `Authorization: Bearer <token>`，401 自动跳转登录页
- **代理**：开发环境 `/admin-api/**` 代理到 `http://localhost:8085`，生产环境由 Nginx 反向代理

## 启动命令

```bash
cd shop-admin
pnpm install        # 首次安装依赖
pnpm dev            # 启动开发服务（http://localhost:8848）
pnpm build          # 生产构建（输出到 dist/）
```

## 后续 Issue 规划

Issue #2 登录定制 → #3 商品管理 → #4 订单管理 → #5 售后管理 → #6 内容运营 → #7 会员中心 → #8 数据看板 → #9 部署

详见 [plans/2026-08-03-admin-base-framework.md](superpowers/plans/2026-08-03-admin-base-framework.md)

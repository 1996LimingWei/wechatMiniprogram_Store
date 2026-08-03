# 2026-08-03 管理后台登录与框架定制（Issue #2）

## 目标

在 Issue #1 基座基础上，完成登录页品牌定制、框架主题配置、用户信息展示、退出登录和路由守卫，使管理后台具备完整可用的登录体验。

## 依赖

Issue #1（项目基座搭建与 API 对接层）

## 已完成内容

### 登录页品牌定制
- 替换模板默认 Logo 为品牌 Logo（莫兰迪绿圆底叶片图标 `public/logo.svg`）
- 登录页改为品牌 Logo + "药食同源管理后台" 标题组合，替换原模板 avatar 组件
- 登录页副标题改为"药食同源 · 管理系统"
- 登录页背景色改为莫兰迪浅绿渐变（`#f5f7f6 → #e8ede9`）
- 登录按钮使用品牌主题色（`#6b8f7a`），hover 态 `#7da38c`
- 移除不再使用的模板 avatar SVG 组件导出

### 顶栏/侧边栏配置
- `platform-config.json`：标题"药食同源管理后台"、主题色 `#6b8f7a`、Logo 显示（已在 Issue #1 完成）
- 侧边栏 `SidebarLogo.vue`：自动引用 `public/logo.svg` + 平台标题，无需改动
- 顶栏移除消息通知组件（模板占位数据不适用）和设置面板齿轮图标
- 顶栏保留：菜单搜索、全屏切换、用户下拉菜单（头像 + 昵称 + 退出）

### Auth API 完善（`src/api/auth.ts`）
- `login()`：代理 `loginApi`，调用 `POST /admin-api/auth/login`
- `getUserInfo()`：从本地存储读取当前用户信息（暂无后端接口）
- `logoutApi()`：纯前端返回成功（后续可对接后端注销接口）

### 用户信息展示与退出登录
- 顶栏右上角：管理员头像 + 昵称（登录后自动设置为"管理员"）
- 点击下拉菜单 → "退出系统"：调用 `logoutApi` → 清除 avatar/username/nickname/roles/permissions → `removeToken()` → 重置路由和标签页 → 跳转 `/login`
- 退出时同步清除 `nickname` 和 `avatar` 字段（原代码遗漏）

### 路由守卫（`src/router/index.ts`，无需修改）
- 已有完整守卫逻辑：
  - 未登录（无 Cookie 或无 userInfo）→ 非白名单路径自动跳转 `/login`
  - 已登录 → 访问 `/login` 自动重定向到当前页面
  - 页面刷新 → 自动调用 `initRouter()` 重新加载路由
  - 401 响应 → HTTP 拦截器清除 token 并跳转登录页

## 变更文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `public/logo.svg` | 替换 | 模板蓝色 Logo → 莫兰迪绿叶片品牌 Logo |
| `src/views/login/index.vue` | 修改 | 替换 avatar 为品牌 Logo + 标题，调整副标题 |
| `src/style/login.css` | 重写 | 品牌色登录按钮、渐变背景、品牌 Logo 样式 |
| `src/views/login/utils/static.ts` | 修改 | 移除不再使用的 avatar 导出 |
| `src/api/auth.ts` | 新建 | login/getUserInfo/logoutApi 三个方法 |
| `src/store/modules/user.ts` | 修改 | 退出登录改为 async，清除全部字段，调用 logoutApi |
| `src/layout/components/lay-navbar/index.vue` | 修改 | 移除通知组件和设置面板 |

## 验收标准与步骤

### 前置条件
1. 启动后端服务：`cd shop-backend && mvn spring-boot:run`（端口 8085）
2. 启动管理后台：`cd shop-admin && pnpm dev`（端口 8848/8849）

### 验收步骤

| # | 操作 | 预期结果 |
|---|------|----------|
| 1 | 浏览器访问 `http://localhost:8848` | 自动跳转到 `/login` 登录页 |
| 2 | 观察登录页 | 莫兰迪绿渐变背景、品牌 Logo + "药食同源管理后台"标题、副标题"药食同源 · 管理系统"、账号密码预填 admin/admin123 |
| 3 | 清空表单，直接点击登录 | 表单校验提示"请输入账号"和"请输入密码" |
| 4 | 输入错误密码，点击登录 | 弹出红色错误提示（后端返回的错误信息） |
| 5 | 输入 admin/admin123，点击登录 | 绿色成功提示，跳转到数据看板首页 |
| 6 | 观察登录后布局 | 左侧边栏（Logo + 5个一级菜单）、顶栏（面包屑 + 搜索 + 全屏 + 管理员头像"管理员"） |
| 7 | 点击侧边栏"商品管理" | 展开子菜单：商品列表、分类管理 |
| 8 | 点击右上角管理员头像 | 弹出下拉菜单，仅显示"退出系统" |
| 9 | 点击"退出系统" | 清除 token，跳回登录页 |
| 10 | 登录后手动访问 `/login` | 自动重定向回当前页面（不允许重复登录） |
| 11 | 退出后手动访问 `/dashboard` | 自动重定向到 `/login` |
| 12 | 登录后按 F5 刷新 | 页面正常恢复，菜单和路由不丢失 |

### 技术验证
```bash
# TypeScript 检查
cd shop-admin && npx vue-tsc --noEmit    # 0 错误

# 开发服务启动
pnpm dev                                  # 无报错，端口 8848 或 8849
```

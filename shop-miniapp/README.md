# 药食同源微信小程序商城 - 前端

基于 **uni-app (Vue2)** 开发的微信小程序商城前端，使用 HBuilderX 作为开发工具。

## 技术栈

- uni-app (Vue2)
- Vuex 状态管理
- 本地 Docker 正式 API（MySQL 商品与内容数据）

## 环境准备

### 1. 安装 HBuilderX

下载地址：https://www.dcloud.io/hbuilderx.html

> 选择 **正式版** -> **App开发版**（内置 uni-app 编译器）

### 2. 安装微信开发者工具

下载地址：https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html

安装完成后：
1. 打开微信开发者工具
2. 进入 **设置 → 安全设置**
3. 开启 **服务端口**（HBuilderX 需要通过端口自动打开开发者工具）

### 3. 注册微信小程序（可选）

- 地址：https://mp.weixin.qq.com/
- 获取 AppID 后填入 `manifest.json` 的 `mp-weixin.appid` 字段
- 不填写 AppID 也可以使用测试号进行开发调试

## 构建步骤

### 方式一：HBuilderX（推荐）

1. 打开 HBuilderX
2. 点击 **文件 → 导入 → 从本地目录导入**，选择 `shop-miniapp` 目录
3. 点击菜单 **运行 → 运行到小程序模拟器 → 微信开发者工具**
4. 首次运行需要配置微信开发者工具路径（HBuilderX 会自动提示）
5. 编译完成后自动打开微信开发者工具预览

### 方式二：仅使用微信开发者工具预览

如果已通过 HBuilderX 编译过一次：
1. 打开微信开发者工具
2. 导入项目，路径选择 `shop-miniapp/unpackage/dist/dev/mp-weixin`
3. 填入 AppID（或使用测试号）

## 项目结构

```
shop-miniapp/
├── App.vue              # 应用入口
├── main.js              # Vue 实例初始化
├── manifest.json        # uni-app 应用配置
├── pages.json           # 页面路由 + tabBar 配置
├── uni.scss             # 全局样式变量
├── vue.config.js        # webpack 构建扩展
├── pages/               # 页面目录
│   ├── index/           # 首页
│   ├── catalog/         # 分类
│   ├── cart/            # 购物车
│   ├── goods/           # 商品详情
│   ├── search/          # 搜索
│   ├── ucenter/         # 用户中心
│   ├── auth/            # 登录注册
│   ├── shopping/        # 下单结算
│   └── ...
├── components/          # 公共组件
├── common/              # 公共样式
├── static/              # 静态资源
├── store/               # Vuex 状态
└── utils/               # 工具函数
    ├── api.js           # API 接口定义
    └── util.js          # 请求封装 + 工具方法
```

## 开发模式

商品与内容页面固定调用 `utils/api.js` 定义的正式 API，不再提供本地商品 Mock 分支。默认后端地址为 `http://127.0.0.1:8085/app-api/`。

启动小程序前，在仓库根目录运行：

```powershell
docker compose up -d --wait mysql redis
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\migrate-db.ps1 -Database shop
docker compose up -d --build backend
```

开发环境中的仿微信支付只会在正式预支付接口明确返回 `mockPay=true` 时打开，并继续受后端环境守卫控制；它不影响商品浏览数据来源。

完整的首页、分类、搜索、商品详情、收藏、足迹和评论验收步骤见项目文档 `docs/acceptance/miniapp-product-api.md`。

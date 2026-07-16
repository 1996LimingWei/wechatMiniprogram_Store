# 项目状态仪表盘

> AI 每次对话开始时读取此文件，了解项目当前状态和下一步行动。

---

## 当前阶段

**阶段**: Development Roadmap（后续开发路径规划）
**计划文件**: [next-development-path.md](plans/2026-07-16-next-development-path.md)
**设计规格**: [shop-miniprogram-design.md](specs/2026-06-22-shop-miniprogram-design.md)

## 进度概览

| 任务 | 状态 | 说明 |
|------|------|------|
| Task 1: Git 仓库初始化 | ✅ 完成 | 仓库结构、.gitignore、README |
| Task 2: 父 POM + 模块骨架 | ✅ 完成 | Maven 多模块结构 |
| Task 3: shop-common 公共模块 | ✅ 完成 | CommonResult, PageResult, BaseDO |
| Task 4: shop-starter-mybatis | ✅ 完成 | MyBatis-Plus 自动配置 |
| Task 5: shop-starter-security | ✅ 完成 | JWT + Spring Security |
| Task 6: shop-starter-web | ✅ 完成 | Web 统一配置 |
| Task 7: 数据库初始化 SQL | ✅ 完成 | 核心表结构 |
| Task 8: Product 模块 CRUD | ✅ 完成 | 商品分类+SPU 接口 |
| Task 9: Server 启动入口 | ✅ 完成 | Spring Boot 主应用 |
| Task 10: 小程序骨架 | ✅ 完成 | uni-app 首页+请求封装 |

## 阻塞项

- 需安装 **HBuilderX**（https://www.dcloud.io/hbuilderx.html）才能编译小程序
- 需安装 **微信开发者工具**（https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html）
- Node.js 24 与 uni-app Vue2 CLI 构建模式不兼容，须使用 HBuilderX 内置编译器

## 2026-06-28 迁移记录

- 从开源项目 platform-wxshop（Spring Boot 2.7 + Vue2）迁移到本项目架构
- 按照 plan1-demo-foundation 完成了全部 10 个 Task 的代码创建
- 后端采用 Spring Boot 3.2 + Java 17 + MyBatis-Plus 3.5.6
- 小程序采用 uni-app + Vue 2（从开源 wx-mall 的 uni-mall 版本复制，35 页面）
- 参考开源项目的业务逻辑（商品、购物车、订单等），按新架构重构
- 已删除 platform-wxshop 目录

## 2026-06-28 小程序前端定型

- 最终确定前端使用 **uni-app Vue2** 版本（从开源 wx-mall 的 `uni-mall/` 复制）
- 删除了之前的原生微信小程序版本 `shop-miniapp-native/`
- 删除了原始开源项目 `platform-wxshop/`
- 清理了原版项目中的 `skills/`、`agent/` 等无关目录
- API 地址已修改为 `http://127.0.0.1/app-api/`，指向本地 Java 后端
- 通过 HBuilderX 编译并运行到微信开发者工具，测试通过

## 2026-06-29 后端 Mock 接口完善

- 新增 `AppIndexController`：首页 7 个接口（banner、channel、brand、topic、newGoods、hotGoods、category）
- 新增 `AppMockController`：分类、商品列表（68个）、商品详情、品牌、专题、评论、购物车、用户等 mock 接口
- 新增 `AppAuthController`：mock 登录接口
- 所有接口使用 `@RequestMapping` 兼容 POST 请求（小程序 util.request 默认 POST）
- Spring Security 已放行所有 app-api 路径（包括 brand、comment、collect、buy）
- API 返回格式统一：`{code: 0, msg: "success", data: {...}}`

## 2026-07-09 商品数据与UI优化

- 整理商品与分类的关系，解决商品货不对板的问题（图片与文字匹配，且左侧分类点击时右侧展示对应的商品列表）
- 重新设计个人中心“我的服务”与“我的订单”图标，使用极简高级的矢量 SVG 替代原先 AI 渲染的 Cartoon/Emoji 拟物图标
- 将商品数据集抽离至 MockData 共享类中，彻底消除了冗余，保证连接后端时图片跟文字同样货真对板
- 优化了首页二级分类切换的交互痛点（当点击二级分类 Tab 时，自动隐藏首页庞大组件，首屏展示精美分类 Banner 与商品列表，切回“精选”时全部还原）
- 补全了金刚位（新人礼、会员、优惠券、分销）全部点击无响应的问题，加入了极具视觉吸引力的高保真模态框动效与页面跳转链接

## 2026-07-16 后续开发路径规划

- 新增 `2026-07-16-next-development-path.md`，作为 Plan1 完成后的阶段规划入口
- 明确后续按 Phase 0-5 推进：规划落地、核心交易闭环、会员营销、用户体验、管理后台、联调上线
- 明确每完成一个阶段必须中文 commit 并 push 到 GitHub
- 已验证 GitHub 推送通道：直连失败时使用 `http.sslBackend=openssl` 与本机代理参数可完成 push 检查

## 决策记录

| 日期 | 决策 | 原因 |
|------|------|------|
| 2026-06-22 | 参考芋道架构，不直接 fork | 70% 代码不需要，保持轻量 |
| 2026-06-22 | 一级分销改为"分享奖励" | 客户需求是简单分享返利 |
| 2026-06-22 | 付费会员制（月卡/年卡） | 非免费升级体系 |
| 2026-06-22 | 不做积分、直播、拼团 | 客户明确不需要 |
| 2026-06-22 | 所有计划和文档使用中文 | 用户要求 |
| 2026-06-28 | 从开源模板迁移代码并按新架构重构 | 用户提供的开源项目作为参考基础 |
| 2026-06-28 | 先复制原生小程序快速获得 UI | 当时未考虑 uni-app 版本 |
| 2026-06-29 | 切换到 uni-app Vue2 版本 | 用户明确要求用 uni-app，原版 uni-mall 已有完整 35 页面，API 路径与 mock 后端完全匹配 |
| 2026-06-29 | 删除原生版和开源项目源码 | 代码已复制，保持项目整洁 |
| 2026-07-09 | 统一前后台 Mock 核心商品数据集 | 保证不管是前端还是后端，在 Mock 模式下都能展示完全一致的高画质真实商品图，完美呈现分类对应的商品列表 |
| 2026-07-09 | 优化二级分类首屏体验 & 补全金刚区功能 | 解决首屏冗余和点击无交互的体验缺陷，提高 Demo 呈现的高保真度和完整性 |
| 2026-07-16 | 后续阶段按“交易闭环优先”推进 | 当前项目已具备高保真 Demo，最大缺口是真实后端交易链路，先完成登录、商品、购物车、订单、支付适配，再推进会员营销和管理后台 |

## 下一步行动

Plan1 所有 Task 已完成，后续开发路径已规划。下一步：
1. 提交并推送 `2026-07-16-next-development-path.md` 与本文件更新
2. 创建 `2026-07-16-plan2-core-trade-loop.md`，细化核心交易闭环实施步骤
3. 按 Plan2 依次实现真实登录、商品真实接口、购物车、地址、订单、支付适配
4. 每完成一个阶段，执行验证、中文 commit、push 到 GitHub

---

## AI 工作流指南

```
开始新对话时：
1. 读取本文件 → 了解当前在做什么
2. 读取对应的 plan 文件 → 了解具体步骤
3. 执行任务（写代码/测试）
4. 更新本文件的进度表 → 标记完成
5. 如当前 plan 所有任务完成 → 读取 spec → 规划下一个 plan

需要新增功能/阶段时：
1. 读取 spec → 确认功能定义
2. 创建新的 plan 文件（plans/YYYY-MM-DD-planN-<名称>.md）
3. 更新本文件指向新 plan
```

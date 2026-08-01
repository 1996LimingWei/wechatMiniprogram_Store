# 项目状态仪表盘

> AI 每次对话开始时读取此文件，了解项目当前状态和下一步行动。

---

## 当前阶段

**阶段**: Development Roadmap（后续开发路径规划）
**计划文件**: [next-development-path.md](plans/2026-07-16-next-development-path.md)
**后端分工**: [backend-three-person-division.md](plans/2026-07-24-backend-three-person-division.md)
**交易剩余工作**: [trade-remaining-work.md](plans/2026-07-26-trade-remaining-work.md)
**交易审计与兜底**: [trade-audit-and-fallback.md](plans/2026-07-31-trade-audit-and-fallback.md)
**下一 Epic 规格**: [2026-08-01-trade-security-boundary-design.md](specs/2026-08-01-trade-security-boundary-design.md)
**当前实施计划**: [2026-07-31-product-sku-read-model.md](plans/2026-07-31-product-sku-read-model.md)
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
| Phase1-子阶段1: 登录与会话 | ✅ 完成 | 微信登录+Token+刷新 |
| Phase1-子阶段2: 商品真实接口 | ✅ 完成 | Issue #10：分类、列表、详情与商品种子数据已接入数据库 |
| Phase1-子阶段3: 交易闭环 MVP | ✅ P0完成 | 购物车+地址+结算+订单+Mock支付+管理端发货+售后同意/拒绝/撤销+超时关闭+订单日志+自动验收 |

## 阻塞项

- 需安装 **HBuilderX**（https://www.dcloud.io/hbuilderx.html）才能编译小程序
- 需安装 **微信开发者工具**（https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html）
- Docker Desktop、WSL 2 与 Ubuntu 24.04 已可用；Docker/Ubuntu 虚拟磁盘、项目数据库、Redis 和项目 Maven 缓存均落在 D 盘。
- Node.js 24 与 uni-app Vue2 CLI 构建模式不兼容，须使用 HBuilderX 内置编译器

## 2026-06-28 迁移记录

- 从开源项目 platform-wxshop（Spring Boot 2.7 + Vue2）迁移到本项目架构
- 按照 plan1-demo-foundation 完成了全部 10 个 Task 的代码创建
- 后端采用 Spring Boot 3.5.16 + Java 25 + MyBatis-Plus 3.5.6
- 2026-07-24：后端运行时从 Java 17 升级到 Java 25，容器镜像同步切换到 Temurin 25
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

## 2026-07-17 首页顶部视觉微调

- 将首页顶部深墨绿色块调整为更浅、更低饱和的莫兰迪浅墨绿渐变
- 顶部背景加入低透明度横向细线纸纹，避免单一纯色块观感
- 将状态栏安全区并入顶部背景内部，实现首页顶部色块通顶、不露白
- 优化左上角品牌区可读性，局部加深背景并将标题文字改为深灰绿色
- 统一优化顶部文字对比度，搜索占位与分类 Tab 改为深鼠尾草灰绿色
- 金刚区由 emoji 切换为统一风格 SVG 服务图标，整体语言更一致
- 强化首屏层级关系，缩小头图与 Banner 割裂感，并提升卡片与购买按钮的前景感
- 优化首页中段信息节奏，突出“限时特惠”主活动卡并弱化公告栏存在感
- 将“今日主推 / 热销爆款 / 新品上架”切换区调整为更明确的胶囊式层级样式

## 2026-07-17 个人中心视觉升级

- 重构个人中心顶部登录区，增强会员入口存在感与信息主次
- 将“我的服务”拆分为高频服务与更多工具两组，降低入口平均感
- 新增克制的品牌页尾信息，填补页面下半部分留白并强化页面完成度
- 收敛会员卡视觉权重，并增强“我的订单”入口的状态感提示

## 2026-07-17 分类 / 详情 / 购物车视觉统一

- 分类页搜索栏、左侧类目导航、商品卡片与加购按钮统一为浅鼠尾草系卡片语言
- 商品详情页去除 emoji 式视觉点缀，重绘会员卡、服务保障标签、药食百科卡与搭配推荐卡
- 商品详情页分隔标题与信息卡层次进一步收敛，和首页、个人中心形成一致的莫兰迪风格
- 购物车页优化空状态、包邮进度条、商品卡片与底部结算栏，减少旧版工具感并增强整页完整度

## 2026-07-17 分类页排版重构

- 将分类页右侧商品区由拥挤的双列小方卡调整为更稳定的单列横向商品卡
- 收窄左侧一级分类宽度，释放右侧内容空间，改善标题、价格与操作按钮的阅读节奏
- 为分类标题区补充说明文案，强化栏目感，避免页面中上部过于单薄

## 2026-07-23 登录与会话子阶段完成 (Phase 1 - 子阶段 1)

### 后端变更
- 新增 `shop-module-member` 模块核心代码：
  - `MemberUserDO`：会员用户实体，映射 `member_user` 表
  - `MemberUserMapper`：MyBatis-Plus Mapper
  - `WxMaProperties`：微信小程序配置属性（appid/secret/mock-enabled）
  - `WxMaService`：微信 code2session 服务（支持 Mock 模式和真实模式）
  - `MemberAuthService`：登录/注册/Token 刷新/用户信息服务
  - `AppAuthController`：真实登录接口（`/app-api/auth/**`）
- 删除 `shop-module-product` 中的旧版 Mock `AppAuthController`
- 更新 `TokenAuthenticationFilter`：将 raw token 存入 credentials，支持 logout 删除
- 更新 `sql/init.sql`：`member_user` 表新增 `session_key` 字段
- 更新 `application-dev.yml`：新增 `wx.ma` 配置项（默认 Mock 模式）

### 前端变更
- `util.js`：请求 header 从 `token: xxx` 改为 `Authorization: Bearer xxx`
- `util.js`：新增 401 自动刷新 Token 逻辑（调用 `auth/refresh-token` 后重发原请求）
- `util.js`：新增 `_handleUnauthorized` 统一未授权处理（清 token + 跳转登录）
- `btnAuth.vue`：优化登录流程，兼容新版微信 `getUserProfile` 失败时直接用 code 登录

### 接口对照
| 接口 | 说明 |
|------|------|
| `POST /app-api/auth/LoginByMa` | 微信登录（code 换 token） |
| `POST /app-api/auth/refresh-token` | 刷新 Token |
| `POST /app-api/auth/logout` | 退出登录 |
| `GET /app-api/auth/user-info` | 获取当前用户信息 |

### 开发注意事项
- 微信登录默认 Mock 模式（`wx.ma.mock-enabled: true`），联调时改为 `false` 并填写 appid/secret
- 运行数据库需执行 `ALTER TABLE member_user ADD COLUMN session_key varchar(128) DEFAULT NULL;`
- 前端 mock 模式不受影响，`useMock: true` 时仍走本地 mock 数据

## 2026-07-24 真实微信登录联调完成

- 修复前后端字段名不一致：后端返回 `nickName`/`avatarUrl` → 改为 `nickname`/`avatar`/`mobile`（与前端 ucenter 页面匹配）
- 配置多环境启动：`dev,local` 双 profile，`application-local.yml` 存放真实 AppID/Secret（已 gitignore）
- 数据库密码修正：`application-dev.yml` 密码从 `qwerr521` 改为 `root`（与 Docker 容器一致）
- 前端切换真实后端：`util.js` 的 `useMock` 改为 `false`
- 微信开发者工具 AppID 配置：`project.config.json` 填入 `wx34175bfa441e4316`
- 验证通过：后端日志确认用户登录成功，前端"我的"页面正确显示"微信用户"+"欢迎回来"
- 关键经验：Maven 多模块项目修改子模块后需先 `mvn clean install -DskipTests` 再 `spring-boot:run`

## 2026-07-24 后端完善范围评估

- 已完成当前后端代码、SQL、前端接口清单与阶段计划的整体梳理
- 结论：后端当前处于"登录真实可用 + 商品基础 CRUD + 大量 Mock 接口"阶段
- 新增正式分工文档：`docs/superpowers/plans/2026-07-24-backend-three-person-division.md`
- 建议按 3 条并行开发线推进：
  1. 商品/内容真实化：商品分类、商品列表、商品详情、首页、搜索、收藏从 MockData 切到数据库
  2. 交易闭环：购物车、地址、结算、订单、库存、Mock 支付/微信支付适配
  3. 平台基础与运营能力：鉴权收口、管理后台接口、营销会员、课程、部署与可观测性

## 2026-07-24 后端三人分工文档发布

- 已将后端三人并行开发分工整理为正式计划文档
- 文档覆盖当前后端基线、三人职责、接口替换顺序、数据表范围、验收标准和协作规则
- 文档已挂载到状态仪表盘顶部，方便团队成员从 `status.md` 进入

## 2026-07-24 GitHub README 协作入口更新

- 已在仓库 README 增加“当前协作入口”
- GitHub 首页可直接进入项目状态仪表盘、后端三人分工文档和后续开发路径规划

## 2026-07-24 交易闭环 MVP 首版实现

- 新增 `shop-module-trade` 模块，并接入父 POM 与 `shop-server`
- 新增交易核心表：`member_address`、`trade_cart`、`trade_order`、`trade_order_item`、`pay_order`、`trade_order_logistics`
- 完成小程序端真实交易接口首版：
  - 购物车：数量统计、列表、加购、直接购买、改数量、删除、选中
  - 地址：列表、详情、保存、删除、简化地区列表
  - 结算：选中商品、默认/指定地址、固定运费与满额包邮、金额汇总
  - 订单：提交、列表、详情、取消、确认收货
  - 支付：Mock 预支付、Mock 支付成功确认、支付状态查询
- 将旧 `AppMockController` 中购物车、地址、订单、支付 Mock 路径迁移到 `/app-api/mock/**`，避免与真实接口冲突
- 前端 `payOrder` 支持识别后端 `mockPay` 标记，走现有仿微信支付页，并在确认支付后回调后端完成支付
- 商品快照优先读取数据库商品，当前商品真实接口未完成时临时回退 `MockData`，保证交易链路可先跑通
- 已验证：`cd shop-backend && mvn clean install -DskipTests` 构建通过

## 2026-07-24 修复订单列表始终展示同款商品 Bug

- 发现问题：无论购买何种商品，订单列表中始终展示为“东阿阿胶糕”。
- 根因分析：
  1. `MockData.getGoodsById` 中对 `id` 进行查找时使用了 `Long.valueOf(...) == id`，包装类对比导致比对为 `false`。查找失败后触发兜底逻辑 `return GOODS_LIST.get(0)`（东阿阿胶糕）。
  2. `TradeRequestUtils` 解析请求参数时，浮点格式字符串（如 `"1.0"`）触发 `Long.parseLong` 格式异常，导致 `goodsId` 降级退回 `0`。
- 修复措施：
  - `MockData.java` 改用 `Long.parseLong` 数值比对，并动态生成未知 ID 的兜底展示，防止统一替换成东阿阿胶糕。
  - `TradeRequestUtils.java` 增强数值容错解析，支持 `Number`、`Float/Double` 字符串格式转 `Long/Integer`。
  - 重新构建 `shop-backend` 并验证成功。

## 2026-07-24 本项目独立开发数据库初始化

- 发现本机 `3306` 已被非本项目 MySQL 占用，且 `root/root` 无法访问，为避免影响已有服务未做任何重置
- 新建本项目专用 Docker MySQL 容器：`shop-mysql`
- 容器内部端口 `3306` 映射到本机 `3307`
- 已执行当前 `sql/init.sql`，确认 `shop` 数据库和交易表创建成功
- `application-dev.yml` 数据库地址已调整为 `jdbc:mysql://localhost:3307/shop`

## 2026-07-24 本项目独立 Redis 启动

- 发现本机 `6379` 已被其他项目 Redis 使用，为避免 Token 与缓存串项目，未复用该 Redis
- 新建本项目专用 Docker Redis 容器：`shop-redis`
- 容器内部端口 `6379` 映射到本机 `6380`
- `application-dev.yml` Redis 端口已调整为 `6380`

## 2026-07-24 后端开发服务启动

- 已启动后端开发服务：`http://127.0.0.1:8085`
- 当前连接本项目专用 MySQL：`shop-mysql`，本机端口 `3307`
- 当前连接本项目专用 Redis：`shop-redis`，本机端口 `6380`
- 验证通过：`/app-api/product/category/list` 返回数据库分类数据
- 验证通过：未登录访问 `/app-api/cart/index` 返回 `401 请先登录`

## 2026-07-24 购物车问题修复

- 修复不同商品加购后在购物车显示为同一商品的问题：后端购物车 SKU 标识改为按 `goodsId + productId` 组合生成，避免首页/分类快捷加购传入相同 `productId` 时发生合并
- 小程序购物车页新增商品左滑删除交互：滑动商品行可露出“删除”按钮，并调用真实 `/app-api/cart/delete` 删除单个商品
- 优化购物车全选状态：空购物车不再被误判为全选
- 已清空本次测试购物车数据，避免旧错误数据继续影响页面展示
- 已验证：`mvn clean install -DskipTests` 构建通过
- 已验证：接口连续加购商品 1 和商品 2 时，购物车分别返回 `productId=1000000001` 与 `productId=2000000001`，删除单项正常

## 2026-07-26 交易闭环 P0 补洞

- 修复立即购买和购物车结算串单风险：`/app-api/buy/add` 不再直接复用普通加购逻辑，立即购买前会取消其他已勾选购物车项，并只保留本次商品进入结算
- 修复结算页选地址返回后刷新顺序：先读取本地 `addressId`，再请求 `/app-api/cart/checkout`
- 支付预下单增加状态校验：已支付、已取消、非待付款、金额异常、支付单已完成的订单不再返回支付参数
- 订单详情页操作按钮改为严格跟随后端 `handleOption`，不可取消的订单不再展示“取消订单”
- 已验证：`cd shop-backend && mvn clean install -DskipTests` 构建通过
- 已验证：启动 `shop-mysql`、`shop-redis` 后完成接口联调，立即购买只结算本次商品，Mock 支付成功后订单变为“待发货”，已支付订单再次预支付返回 `订单已支付`
- 已验证：取消订单后再次预支付返回 `当前订单不能支付`
- 已清理本次接口测试用户、购物车、地址、订单和支付单数据

## 2026-07-26 发货与物流 MVP

- 新增 `trade_order_logistics` 对应后端实体、Mapper、Service 和小程序端接口
- 新增 `/app-api/order/mock-ship`：开发用模拟发货接口，当前没有管理后台和真实物流时可将订单从“待发货”流转到“待收货”
- 新增 `/app-api/order/logistics`：订单物流查询接口，返回物流公司、物流单号、发货时间和模拟轨迹
- 订单列表和订单详情接口补充 `logistics` 信息，并在 `handleOption` 中返回 `ship`、`logistics`、`confirm` 操作状态
- 小程序订单列表、订单详情接入“模拟发货”和“查看物流”，不再停留在“物流查询功能开发中”
- 调整确认收货规则：只有“待收货”订单可确认收货，支付后的“待发货”订单需先发货
- 已验证：`cd shop-backend && mvn clean install -DskipTests` 构建通过
- 已验证：接口联调完成“支付成功 -> 待发货 -> 模拟发货 -> 待收货 -> 查看物流 -> 确认收货 -> 已完成”
- 已清理本次接口测试用户、购物车、地址、订单、支付单和物流单数据

## 2026-07-26 售后与退款 MVP

- 新增 `trade_after_sale` 售后表，并同步本地 `shop-mysql` 数据库
- 新增售后后端实体、Mapper、Service 和小程序端接口
- 新增 `/app-api/order/refund/apply`：用户申请退款/售后，订单进入“退款中”
- 新增 `/app-api/order/refund/detail`：查询订单售后信息
- 新增 `/app-api/order/refund/mock-approve`：开发用模拟退款审核通过，订单展示“已退款”，支付状态更新为已退款
- 订单列表和订单详情接口补充 `afterSale` 信息，并在 `handleOption` 中返回 `refund`、`refundApprove`
- 小程序订单列表、订单详情接入“申请退款”和“模拟退款通过”，不再停留在“退款申请功能开发中”
- 已验证：`cd shop-backend && mvn clean install -DskipTests` 构建通过
- 已验证：待发货订单可申请退款，申请后显示“退款中”，模拟审核后显示“已退款”
- 已验证：待收货订单可申请售后，模拟审核后显示“已退款”
- 已清理本次接口测试用户、购物车、地址、订单、支付单、物流单和售后单数据

## 2026-07-26 订单超时关闭与库存释放

- 新增交易订单配置 `trade.order`，支持配置待付款超时时间、自动关闭任务开关、单次处理批量和任务间隔
- 新增待付款订单自动关闭定时任务，默认每 60 秒扫描一次超时未支付订单
- `trade_order` 新增 `expire_time`、`close_time`、`close_reason` 字段，并新增 `idx_expire_status` 索引
- 提交订单时写入待付款过期时间，默认 30 分钟后超时
- 用户主动取消和系统超时关闭统一走同一套关闭逻辑：只有 `待付款 + 未支付` 状态更新成功后才回补库存、关闭待支付支付单
- 支付成功改为条件更新，只有数据库当前仍是 `待付款 + 未支付` 的订单才能变为待发货，避免支付确认与超时任务并发覆盖状态
- 商品扣库存与回补库存改为 SQL 原子增减，降低并发超卖和库存覆盖风险
- 已同步本地 `shop-mysql` 数据库表结构
- 已验证：创建订单后库存从 10 扣到 8，手动置为过期后自动关闭，库存回补到 10，支付单变为已关闭
- 已验证：已超时关闭订单再次支付返回失败，且定时任务重复扫描不会重复回补库存
- 已清理本次接口测试用户、购物车、地址、订单、支付单和测试商品数据

## 2026-07-26 订单状态日志

- 新增 `trade_order_log` 订单操作日志表，并同步本地 `shop-mysql` 数据库
- 新增订单日志实体、Mapper 和 `TradeOrderLogService`
- 订单详情接口新增 `orderLogs`，可返回订单操作时间线
- 已接入关键状态变化日志：提交订单、支付成功、用户取消、系统超时关闭、模拟发货、确认收货、申请售后、退款完成
- 日志记录操作来源、操作人、操作动作、订单状态前后值、支付状态前后值和操作说明
- 已验证：完整流程“提交订单 -> 支付成功 -> 发货 -> 确认收货 -> 申请售后 -> 退款完成”生成 6 条连续日志
- 已验证：取消流程“提交订单 -> 用户取消”生成 2 条连续日志
- 已清理本次接口测试用户、购物车、地址、订单、支付单、物流单、售后单、订单日志和测试商品数据

## 2026-07-26 交易模块剩余工作梳理

- 新增交易模块剩余工作正式清单：`docs/superpowers/plans/2026-07-26-trade-remaining-work.md`
- 按企业交付视角区分测试阶段与正式交付要求
- 按 P0/P1/P2 梳理剩余工作、必要性、验收标准和建议顺序
- 明确下一步优先推进管理端最小订单处理接口：订单列表、订单详情、发货、售后列表、售后同意/拒绝

## 2026-07-26 交易闭环 P0 企业验收项完成

- 新增管理端订单处理接口：
  - `/admin-api/trade/order/list`
  - `/admin-api/trade/order/detail`
  - `/admin-api/trade/order/ship`
- 新增管理端售后处理接口：
  - `/admin-api/trade/after-sale/list`
  - `/admin-api/trade/after-sale/approve`
  - `/admin-api/trade/after-sale/reject`
- 新增用户撤销售后接口：`/app-api/order/refund/cancel`
- `trade_after_sale` 补齐 `before_order_status`、`reject_reason`、`reject_time`、`cancel_time` 字段，并同步本地 `shop-mysql`
- 售后状态补齐：处理中、已退款、已拒绝、已撤销；拒绝和撤销后订单会恢复到申请售后前状态
- 小程序订单列表/详情默认隐藏“模拟发货”“模拟退款通过”等开发按钮，统一由 `TradeDevActionEnabled` 控制
- 小程序订单列表/详情新增“撤销申请”，订单详情展示售后拒绝原因
- 新增交易验收文档：`docs/superpowers/plans/2026-07-26-trade-acceptance.md`
- 新增自动验收脚本：`scripts/verify-trade-flow.ps1`
- 已验证：`cd shop-backend && mvn clean install -DskipTests` 构建通过
- 已验证：`.\scripts\verify-trade-flow.ps1 -BaseUrl "http://localhost:8085"` 自动跑通下单、支付、管理端发货、确认收货、售后同意、售后拒绝、用户撤销、超时关闭与库存回补
- 已清理本次验收测试用户、购物车、地址、订单、支付单、物流单、售后单、订单日志和测试商品数据

## 2026-07-26 GitHub 交易协作汇总更新

- 已更新仓库首页 `README.md`，新增“当前已完成内容汇总”
- 汇总交易闭环 P0 已完成能力、关键接口、关键表和验收脚本
- 新增“给其他同事的补充方向”，明确商品真实信息、支付、物流、管理后台前端、营销和客户资料对交易模块的影响
- 修正 README 本地开发口径：后端端口 `8085`、MySQL 端口 `3307`、Redis 端口 `6380`

## 2026-07-27 项目同步与本地容器检查

- 已同步远端 `main` 至 `fff6be5`（交易闭环 P0 企业验收项完成）。
- 已启动项目专用 `shop-mysql`（本地端口 `3307`）与 `shop-redis`（本地端口 `6380`），并通过 MySQL/Redis 存活检查。
- 发现当前本地 `shop` 数据库尚缺 `trade_after_sale`、`trade_order_log` 等新交易表；启动新版后端前应以增量迁移方式补齐表结构，避免重跑初始化脚本覆盖已有数据。
- 本机当前仅安装 JDK 17；后端已配置 Java 25，需安装并配置 JDK 25 后才能完成本地 Maven 构建。

## 2026-07-27 商品真实接口与数据库迁移 Epic 规划

- 新增规格：`docs/superpowers/specs/2026-07-27-product-real-api-and-migration-design.md`。
- 明确下一 Epic 先建立可重复执行的增量迁移，再切换商品分类、列表、详情和 SKU 库存到数据库读取。
- 已将实施拆分为迁移机制、商品列表、商品详情与交易快照、验收四个子 Issue；待规格评审后创建实施计划。

## 2026-07-27 数据库增量迁移实施计划

- 新增计划：`docs/superpowers/plans/2026-07-27-database-incremental-migration.md`。
- 首个子 Issue 聚焦可重复执行的交易 P0 数据库迁移，不修改商品接口，避免与现有 `feat/backend-product-real-api` 独立工作树冲突。
- 计划已明确迁移历史表、迁移 SQL、PowerShell 执行器、隔离数据库验收和真实本地库升级步骤。

## 2026-07-27 数据库增量迁移完成

- 新增 `sql/migrations/V20260727_01__trade_p0_schema.sql`，补齐交易 P0 的订单超时字段与索引、订单日志表、售后表和退款支付状态定义。
- 新增 `scripts/migrate-db.ps1`：按版本与 SHA-256 校验和执行迁移，并写入 `schema_migration_history`；重复执行会安全跳过已完成版本。
- 新增 `scripts/verify-db-migration.ps1`：在隔离临时数据库中验证首次迁移、结构完整性和重复执行幂等性。
- 已备份并升级本地 `shop` 数据库，确认 `trade_after_sale`、`trade_order_log`、`idx_expire_status` 和版本 `20260727_01` 存在。
- 本机已安装 Temurin JDK 25；Spring Boot 从 3.2.5 升级至 3.5.16 以支持 Java 25，后端全量 Maven 构建及可执行 JAR 打包完成。

## 2026-07-27 商品真实接口合并与联调完成

- 已将 `feat/backend-product-real-api` 与当前 `main` 的交易迁移内容合并，冲突仅涉及 `.gitignore` 和状态文档，均保留双方有效记录。
- 商品分类、分页列表、关键词筛选和商品详情接口已接入本地 MySQL；商品详情会返回 SKU 数据。
- 在 Temurin JDK 25 环境中，Maven 全模块测试通过，商品模块 `AppProductResponseAssemblerTest` 通过；完整打包生成可执行 JAR。
- 本地联调确认 `goods/count`、`catalog/index`、`goods/list` 和 `goods/detail` 均返回 `code: 0`，列表总数为 4，关键词“阿胶”可命中商品。

## 2026-07-30 首页内容与用户互动真实化推进

- Issue #11：已实现首页 Banner、频道、品牌、专题、新品、热销和分类楼层的数据库查询、内容种子和迁移；模块测试、全量构建、Docker 迁移和接口联调均通过。
- Issue #12：已在 `feat/user-interaction-mvp` 实现收藏、浏览足迹和商品评论的真实接口、数据迁移、小程序足迹接入，以及商品详情的收藏状态和评论摘要。
- `shop-module-product` 单元测试已覆盖首页内容排序/过滤、评论响应契约和商品详情互动摘要，并通过 `mvn test -pl shop-module-product -am`。
- 草稿 PR #13 汇总以上改动；Issue #11/#12 的 Docker 数据库迁移与真实接口联调条件已满足。

## 2026-07-31 交易环节二次自查

- Java 25 容器内 Maven 构建与现有 7 个商品模块测试通过；交易模块当前没有单元测试，显示 `No tests to run`。
- 现有 `scripts/verify-trade-flow.ps1` 主流程验收通过，但未覆盖鉴权、真实 SKU、重复购买、支付单金额、退款库存和并发状态竞争。
- 已确认 P0 安全问题：`/admin-api/**` 全部匿名放行；Mock 支付成功、模拟发货、模拟退款通过仅由前端隐藏，后端没有环境开关。
- 已确认商品与库存问题：交易仍按 SPU 读取价格和扣库存，使用合成 SKU ID，商品不存在时仍回退 `MockData`；待发货退款不回补库存。
- 已在隔离数据库复现购物车软删除唯一键冲突：同一用户第二次清理同一 SKU 时触发 `uk_user_sku` 重复键。
- 已通过接口复现退款数据不一致：订单已退款后 `pay_order` 仍为已支付，库存未回补，支付查询错误显示“未支付”。
- 数据库迁移验收脚本已被后续迁移破坏：临时基线缺少 `content_banner`，且历史记录数量断言仍固定为 1。
- Docker Compose 默认 Mock 登录配置无效：Dockerfile 强制 `prod`，`application-prod.yml` 又将微信 Mock 写死为 `false`。
- 本次审计测试数据已全部清理，临时后端已删除，MySQL/Redis 已恢复为停止状态。
- 交易侧下一步应先处理安全止血和数据一致性，再继续微信支付、营销或物流扩展。

## 2026-08-01 商品与交易可交付化规划

- 新增商品与交易可交付化实施计划，按“P0 安全止血 → 真实 SKU/库存 → 交易一致性 → 真实支付上线准备”推进。
- 商品负责人优先负责真实 SKU、规格选择、商品快照和库存协作；在真实商品链路完成前，不再扩展基于 MockData 的交易功能。

## 2026-08-01 Mock 契约优先决策与规划

- 决定采用“先完整 Mock、后逐项替换真实数据源”模式；Mock 仅替代数据来源，鉴权、金额校验、库存幂等、状态机和订单日志从第一天按真实规则执行。
- 新增对应设计规格和实施计划；首个开发任务为商品/SKU Mock Provider 与库存契约，后续以同一组自动验收逐项替换数据库、物流和微信支付实现。

## 2026-08-01 Mock 契约优先首个 Issue

- 已创建 [Issue #20：建立商品 SKU Mock Provider 与库存契约](https://github.com/QtImM/wechatMiniprogram_Store/issues/20)，建议分支为 `feat/mock-sku-inventory-contract`。
- Issue 覆盖 Mock SKU/库存数据、商品与库存服务契约、交易调用切换及契约测试；不改变小程序 API，也不提前接入真实支付或物流。

## 2026-08-01 Issue #20 实现与验证

- 商品模块新增可替换 `ProductSkuProvider`：开发环境默认启用稳定 Mock SKU/库存，配置 `product.provider=database` 时切换为数据库 SKU 实现。
- 交易模块不再直接访问 `MockData` 或 SPU 库存；购物车、结算、下单、取消/超时回补统一通过 SKU 契约，订单提交会重新读取当前 SKU 快照与价格。
- 已通过 `mvn test -pl shop-module-product,shop-module-trade -am`（9 项测试）和 `mvn clean install -DskipTests`（11 个模块）。Docker 守护进程不可达，隔离数据库迁移与交易自动验收待环境恢复后补跑。

## 2026-08-01 合并 SKU Mock 契约与后续 P0 规划

- 已将 `feat/mock-sku-inventory-contract` 合并并推送至 `main`；交易链路已统一通过可替换 SKU/库存契约读取商品快照与库存。
- 已复核剩余 P0，下一阶段优先处理管理端鉴权、Mock 写操作后端环境隔离、迁移验收基线与安全回归；对应规格与实施计划已创建。

## 2026-08-01 Issue #21 交易安全边界实现

- 已创建 [Issue #21：收紧交易管理端与 Mock 写操作权限边界](https://github.com/QtImM/wechatMiniprogram_Store/issues/21)，并在 `feat/trade-security-boundary` 实现。
- 新增配置注入的最小管理员登录与 `ROLE_ADMIN`；`/admin-api/**` 仅管理员可访问，发货与售后审批日志记录管理员 ID。
- 新增 `trade.mock-actions-enabled` 守卫，生产 profile 强制拒绝 Mock 支付、发货与退款审核；开发环境通过明确配置开启。
- 已通过模块测试、全量构建、Docker HTTP 鉴权、生产环境隔离、数据库迁移与完整交易验收。

## 2026-08-01 Docker 恢复与历史 P0 闭环

- Docker Desktop 数据盘已迁移到 `D:\DockerDesktop\data`，Ubuntu 24.04 已迁移到 `D:\WSL\Ubuntu-24.04`；Compose 的 MySQL/Redis 改用仓库下 `.docker-data` 绑定目录，项目 Maven 缓存固定在 `shop-backend/.mvn/repository`，大体积开发数据不再写入 C 盘。
- 修复 Compose 开发环境配置覆盖：使用 Spring Boot 标准数据源与 Redis 环境变量，启用数据库 SKU Provider、Mock 登录、管理员认证和开发态 Mock 写操作。
- 修复迁移验收的 MySQL 就绪等待、首页内容基线、动态迁移版本断言与 PowerShell 5.1 UTF-8 兼容；三个迁移版本首次执行和重复幂等执行均通过。
- 修复购物车逻辑删除唯一键冲突，删除与下单清理均改为物理删除；修复退款支付单查询、待发货退款 SKU 库存回补、无售后申请直接退款和退款查询状态错误。
- `scripts/verify-trade-flow.ps1` 已覆盖匿名/会员/管理员权限、重复购物车删除、真实 SKU、支付金额、无申请退款拒绝、完整履约售后和超时关闭，Docker HTTP 全链路验收通过并自动清理数据。
- 首页 7 个内容接口以及收藏、足迹、评论接口已完成 Docker/MySQL 联调，Issue #11/#12 的 Docker 阻塞验收项关闭。
- Dockerfile 已移除强制生产 profile，增加 Maven 持久缓存并在镜像构建中实际执行测试；11 个模块构建成功，商品模块 9 项与交易模块 2 项测试通过，独立生产 profile 容器确认 Mock 写接口返回业务码 `403`。

## 2026-08-01 商品负责人剩余 Issue 规划

- 已复核开放 Issue，确认多规格 SKU 已由 [Issue #15](https://github.com/QtImM/wechatMiniprogram_Store/issues/15) 覆盖，商品 SKU Mock Provider 已由 [Issue #20](https://github.com/QtImM/wechatMiniprogram_Store/issues/20) 覆盖，不重复建单。
- 新建 [Issue #22：将商品搜索与搜索历史切换为真实数据](https://github.com/QtImM/wechatMiniprogram_Store/issues/22)，建议分支 `feat/product-search-history`。
- 新建 [Issue #23：收口商品内容 Mock Provider 与正式 API 边界](https://github.com/QtImM/wechatMiniprogram_Store/issues/23)，建议分支 `feat/product-mock-provider-boundary`。
- 新建 [Issue #24：完善商品内容演示种子与自动验收数据集](https://github.com/QtImM/wechatMiniprogram_Store/issues/24)，建议分支 `feat/product-demo-seed`。
- 新建 [Issue #25：收口小程序商品正式 API 并完成端到端验收](https://github.com/QtImM/wechatMiniprogram_Store/issues/25)，建议分支 `feat/miniapp-product-api-acceptance`。
- 商品负责人建议执行顺序：`#22 → #15 → #23 → #24 → #25`；每张 Issue 独立分支、测试、提交、推送与合并，避免商品查询、详情装配和前端页面产生交叉冲突。

## 2026-08-01 Issue #22 商品搜索与搜索历史真实化

- 新增迁移 `V20260801_01__product_search_history.sql` 和搜索历史 DO/Mapper，按用户与关键词唯一约束实现重复搜索幂等更新和清空后恢复。
- `/app-api/search/index`、`helper`、`clearhistory` 已从 `AppMockController` 迁移到正式搜索 Controller；热门词、默认词和联想词均读取已上架数据库商品。
- 商品关键词列表第一页会为当前会员记录规范化搜索历史；匿名用户返回空历史且不写入，管理员身份也不会混入会员历史。
- 新增 5 项搜索服务测试，商品模块 14 项测试和 Docker 11 模块全量构建通过；4 个数据库迁移版本首次与重复执行通过。
- 新增 `scripts/verify-product-search.ps1`，双用户搜索历史隔离、重复关键词、清空互不影响、空关键词、下架商品过滤和测试数据自动清理均验收通过。
- [PR #26](https://github.com/QtImM/wechatMiniprogram_Store/pull/26) 已合并到 `main`，Issue #22 已自动关闭。
- 下一项切换到 [Issue #15：商品多规格与库存可售性读模型](https://github.com/QtImM/wechatMiniprogram_Store/issues/15)。

## 2026-07-31 Issue #15 商品多规格与库存可售性读模型完成

- 新增 [规格](specs/2026-07-31-product-sku-read-model.md) 与 [实施记录](plans/2026-07-31-product-sku-read-model.md)，明确只改商品模块和商品详情页，不触及 Issue #14 的交易模块或迁移文件。
- 商品详情接口现已解析全部 SKU 规格属性，返回稳定排序的规格维度、精确 SKU 矩阵、价格、图片、库存和可售状态，并保留 `goodsSpecificationIds`、`goodsNumber` 兼容字段。
- 无效、非整数或重复维度属性会按整条 SKU 安全降级，不再污染规格列表；完整 SKU 矩阵不依赖数据库返回顺序。
- 商品详情页按“规格维度 ID + 规格值 ID”精确匹配 SKU，支持缺货组合禁用、库存数量上限和 SKU 价格/图片切换；旧接口与 Mock 字段保持兼容。
- Docker 11 模块全量构建通过，商品模块 15 项与交易模块 2 项测试通过；前端 SKU 独立脚本和真实 MySQL/Redis 多规格详情 HTTP 验收均通过，测试数据自动清理。
- [PR #18](https://github.com/QtImM/wechatMiniprogram_Store/pull/18) 等待最终推送与合并，合并后关闭 Issue #15。

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
| 2026-07-24 | 后端 userInfo 字段名与前端对齐 | 后端返回 nickName/avatarUrl，前端期望 nickname/avatar，统一为小写 |
| 2026-08-01 | 采用 Mock 契约优先、可替换数据源架构 | 先完整演示与验证流程，后续替换真实数据源时不重写前端、Controller 或核心交易规则 |
| 2026-08-01 | 最小管理员身份暂采用环境配置注入 | 先消除匿名管理端风险；完整管理员表、密码管理与后台账号管理在后续平台能力阶段实现 |

## 2026-07-24 Agent Loop Skill

- 新增项目内 `skills/agent-loop/`，用于以目标、行动、观察和调整的闭环推进多步骤任务。
- 该版本面向团队协作：执行前读取项目规则、计划和状态；完成后更新状态。
- 同时在 `C:\Users\Tim\.codex\skills\agent-loop\` 维护独立的个人全局版本；两份 skill 均引用 OpenAI 的 Codex Agent Loop 原文。

## 下一步行动

下一步按 Mock 契约优先计划实施：
1. 商品负责人完成 Issue #22 后，依次执行 `#15 → #23 → #24 → #25`。
2. 交易负责人固化支付与物流服务契约，补齐重复支付、关闭后回调和并发状态竞争测试。
3. 在现有商品/SKU 契约和验收脚本下，继续替换物流供应商与微信支付 V3。

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

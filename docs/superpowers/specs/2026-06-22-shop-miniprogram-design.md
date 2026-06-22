# 药食同源微信小程序商城 — 系统设计文档

> 基于芋道(Yudao)架构参考 + 核心代码复用，自建精简商城系统

---

## 1. 项目概述

### 1.1 产品定位

面向药食同源、农副产品、保健品销售的微信小程序商城，同时支持线上课程研学内容销售。

### 1.2 核心业务

- **实物商品**：农副产品 + 保健品 → 下单→付款→发货→物流→收货
- **虚拟商品**：课程研学 → 下单→付款→自动解锁→在线观看
- **付费会员**：月卡/年卡制，享专属折扣+优先发货+新品抢先购
- **分享奖励**：用户分享商品，好友下单后获得奖励（一级）

### 1.3 技术选型

| 层级 | 技术 | 版本 |
|------|------|------|
| 小程序前端 | uni-app + Vue 3 + TypeScript + Pinia | Vue 3.4+ |
| UI组件库 | uView Plus | 3.x |
| 后端 | Java 17 + Spring Boot 3.2 | JDK 17 |
| ORM | MyBatis-Plus | 3.5.6+ |
| 数据库 | MySQL 8.0 | |
| 缓存 | Redis | 7.x |
| 安全 | Spring Security + JWT + Redis Token | |
| 微信SDK | WxJava (weixin-java-miniapp + wechatpay) | 4.6+ |
| 管理后台 | Vue 3 + Element Plus + Vite | |
| 部署 | 微信云托管（Docker容器） | |
| 对象存储 | 腾讯云COS | |

### 1.4 参考项目

- 芋道 ruoyi-vue-pro（MIT协议）：参考架构设计、复用核心代码逻辑
- 芋道 yudao-mall-uniapp：参考前端请求封装、路由设计

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     客户端                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 微信小程序    │  │ H5商城(备用) │  │ PC管理后台   │      │
│  │ (uni-app)    │  │ (uni-app)    │  │ (Vue3+EPlus) │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                  微信云托管 (Docker)                          │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Spring Boot 单体应用                       │  │
│  │                                                       │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────────┐  │  │
│  │  │  用户    │ │  商品   │ │  交易    │ │   课程     │  │  │
│  │  │  模块    │ │  模块   │ │  模块    │ │   模块     │  │  │
│  │  └─────────┘ └─────────┘ └─────────┘ └───────────┘  │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────────┐  │  │
│  │  │  营销    │ │  支付   │ │  系统    │ │   内容     │  │  │
│  │  │  模块    │ │  模块   │ │  模块    │ │   模块     │  │  │
│  │  └─────────┘ └─────────┘ └─────────┘ └───────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐      │
│  │ MySQL 8  │  │  Redis   │  │  腾讯云COS(图片/视频) │      │
│  └──────────┘  └──────────┘  └──────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 后端模块划分

```
shop-backend/
├── pom.xml                          # 父POM
├── shop-dependencies/               # BOM版本管理
├── shop-framework/                  # 基础框架层
│   ├── shop-common/                 #   通用工具、常量、异常
│   ├── shop-starter-web/            #   Web配置、统一响应、全局异常
│   ├── shop-starter-security/       #   认证鉴权(JWT+Redis)
│   ├── shop-starter-mybatis/        #   MyBatis-Plus配置、基础DO/Mapper
│   ├── shop-starter-redis/          #   Redis配置、分布式锁
│   └── shop-starter-wechat/         #   微信登录/支付封装
├── shop-module-member/              # 会员模块
│   ├── shop-module-member-api/      #   对外接口定义
│   └── shop-module-member-biz/      #   业务实现
├── shop-module-product/             # 商品模块
│   ├── shop-module-product-api/
│   └── shop-module-product-biz/
├── shop-module-trade/               # 交易模块(订单/购物车/售后/物流)
│   ├── shop-module-trade-api/
│   └── shop-module-trade-biz/
├── shop-module-promotion/           # 营销模块(优惠券/满减/秒杀)
│   ├── shop-module-promotion-api/
│   └── shop-module-promotion-biz/
├── shop-module-course/              # 课程模块 ★ 自建
│   ├── shop-module-course-api/
│   └── shop-module-course-biz/
├── shop-module-pay/                 # 支付模块
│   ├── shop-module-pay-api/
│   └── shop-module-pay-biz/
├── shop-module-system/              # 系统模块(管理员/角色/权限/配置)
│   ├── shop-module-system-api/
│   └── shop-module-system-biz/
├── shop-server/                     # 启动入口
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
└── Dockerfile                       # 微信云托管部署
```

### 2.3 前端项目结构

```
shop-miniapp/                        # uni-app 小程序
├── src/
│   ├── pages/                       # 页面
│   │   ├── index/                   #   首页/分类/购物车/我的(TabBar)
│   │   ├── product/                 #   商品详情/列表
│   │   ├── course/                  #   课程列表/详情/播放 ★ 自建
│   │   ├── order/                   #   订单确认/列表/详情/售后
│   │   ├── user/                    #   个人信息/地址/收藏/钱包
│   │   ├── member/                  #   会员中心/开通
│   │   ├── coupon/                  #   优惠券
│   │   └── common/                  #   登录/搜索/设置/反馈
│   ├── api/                         # 接口定义
│   ├── components/                  # 公共组件
│   ├── store/                       # Pinia状态管理
│   ├── utils/                       # 工具函数
│   │   ├── request.js              #   请求封装(callContainer)
│   │   └── auth.js                 #   登录态管理
│   ├── static/                      # 静态资源
│   ├── App.vue
│   ├── main.ts
│   ├── manifest.json
│   ├── pages.json                   # 路由配置
│   └── uni.scss                     # 全局样式变量
├── package.json
└── vite.config.ts

shop-admin/                          # 管理后台
├── src/
│   ├── views/                       # 页面
│   │   ├── dashboard/              #   数据概览
│   │   ├── product/                #   商品管理
│   │   ├── course/                 #   课程管理 ★ 自建
│   │   ├── order/                  #   订单管理
│   │   ├── member/                 #   用户/会员管理
│   │   ├── promotion/              #   营销管理
│   │   ├── content/                #   内容管理(Banner/公告)
│   │   ├── finance/                #   财务管理
│   │   └── system/                 #   系统设置/权限
│   ├── api/                        # 接口
│   ├── components/                 # 组件
│   ├── router/                     # 路由
│   ├── store/                      # 状态
│   └── utils/                      # 工具
├── package.json
└── vite.config.ts
```

---

## 3. 复用 vs 自建 清单

### 3.1 从芋道复用的部分

| 复用内容 | 来源模块 | 复用方式 | 改动程度 |
|----------|----------|----------|----------|
| **基础框架设计思路** | yudao-framework | 参考其starter分层思路，精简实现 | 重写，保留思路 |
| **统一响应/异常处理** | yudao-common | 复制CommonResult+GlobalExceptionHandler | 微调 |
| **MyBatis-Plus基础配置** | yudao-starter-mybatis | 复制BaseMapperX、分页配置 | 直接用 |
| **Spring Security + Token** | yudao-starter-security | 参考Token认证+多端支持逻辑 | 精简（去掉多租户） |
| **微信登录流程** | yudao-module-member | 复制小程序登录code→openid→token逻辑 | 直接用 |
| **微信支付封装** | yudao-module-pay | 复制WxJava支付下单/回调/退款逻辑 | 精简（只保留小程序支付） |
| **商品SPU/SKU模型** | yudao-module-product | 参考表设计+CRUD逻辑 | 精简字段 |
| **商品分类树** | yudao-module-product | 复制二级分类实现 | 直接用 |
| **购物车逻辑** | yudao-module-trade | 复制加购/改量/删除/选中逻辑 | 直接用 |
| **订单状态机** | yudao-module-trade | 参考状态流转设计(待付→待发→待收→完成) | 精简 |
| **订单价格计算** | yudao-module-trade | 参考TradePriceCalculator链式计算 | 精简（去掉积分/砍价） |
| **售后退款流程** | yudao-module-trade | 参考状态机+退款逻辑 | 直接用 |
| **优惠券模板+实例** | yudao-module-promotion | 复制模板→领取→使用→核销逻辑 | 直接用 |
| **满减活动** | yudao-module-promotion | 复制满减规则匹配逻辑 | 直接用 |
| **物流查询** | yudao-module-trade | 参考快递对接方式 | 改为顺丰API |
| **uni-app请求封装** | yudao-mall-uniapp/sheep/request | 参考拦截器+Token刷新机制 | 改为callContainer |
| **uni-app路由分包** | yudao-mall-uniapp/pages.json | 参考分包加载策略 | 调整页面结构 |
| **Pinia Store设计** | yudao-mall-uniapp/sheep/store | 参考user/cart/app的store划分 | 精简 |

### 3.2 完全自建的部分

| 自建内容 | 原因 | 复杂度 |
|----------|------|--------|
| **课程模块（后端）** | 芋道完全没有此功能 | 中 |
| **课程播放页（前端）** | 视频播放+章节+进度+付费解锁 | 中 |
| **付费会员体系** | 芋道是经验值升级制，我们是月卡/年卡付费制 | 中 |
| **分享奖励系统** | 芋道是二级分销（过于复杂），我们只要一级分享奖励 | 低 |
| **余额/钱包** | 充值+提现+明细，芋道有但耦合其分销系统 | 低 |
| **管理后台UI** | 芋道后台功能过多，自建更精准 | 中 |
| **运费模板** | 按我们的规则（满X包邮）简化实现 | 低 |
| **消息中心** | 小程序订阅消息+站内信 | 低 |

### 3.3 明确不用的芋道模块

```
❌ yudao-module-bpm        — 工作流（不需要）
❌ yudao-module-crm        — 客户关系（不需要）
❌ yudao-module-erp        — 企业资源（不需要）
❌ yudao-module-wms        — 仓储（不需要）
❌ yudao-module-mes        — 制造执行（不需要）
❌ yudao-module-ai         — AI模块（不需要）
❌ yudao-module-iot        — 物联网（不需要）
❌ yudao-module-im         — 即时通讯（不需要）
❌ yudao-module-mp         — 公众号管理（不需要）
❌ yudao-module-report     — 报表（首期不需要）
❌ 多租户SaaS              — 不需要
❌ 砍价/拼团/积分商城       — 不需要
❌ DIY页面装修              — 首期不需要
❌ 直播组件                 — 不需要
```

---

## 4. 数据库设计

### 4.1 表结构总览（首期 30张）

#### 会员相关（6张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `member_user` | 用户基本信息(openid/手机号/昵称/头像) | 芋道member_user精简 |
| `member_address` | 收货地址 | 芋道member_address |
| `member_plan` | 会员套餐定义(月卡99元/年卡699元/权益) | **自建** |
| `member_subscription` | 用户会员订阅记录(开始/到期/状态) | **自建** |
| `member_wallet` | 用户钱包(余额/冻结金额) | **自建** |
| `member_wallet_transaction` | 钱包流水(充值/消费/提现/奖励) | **自建** |

#### 商品相关（6张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `product_category` | 商品分类(二级树) | 芋道product_category |
| `product_spu` | 商品SPU(type区分实物/虚拟) | 芋道product_spu精简 |
| `product_sku` | 商品SKU(价格/库存/规格值) | 芋道product_sku |
| `product_property` | 商品属性(颜色/重量/材质) | 芋道product_property |
| `product_property_value` | 属性值 | 芋道product_property_value |
| `product_favorite` | 用户收藏 | 芋道product_favorite |

#### 课程相关（4张）— 完全自建

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `course` | 课程主表(标题/封面/价格/简介/状态) | **自建** |
| `course_chapter` | 课程章节(排序/标题/视频URL/时长/是否试看) | **自建** |
| `course_enrollment` | 用户购买记录(用户/课程/订单关联) | **自建** |
| `course_progress` | 学习进度(用户/章节/播放进度/是否完成) | **自建** |

#### 交易相关（7张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `trade_cart` | 购物车 | 芋道trade_cart |
| `trade_order` | 订单主表 | 芋道trade_order精简 |
| `trade_order_item` | 订单商品明细 | 芋道trade_order_item |
| `trade_order_logistics` | 物流信息 | 芋道精简 |
| `trade_after_sale` | 售后退款单 | 芋道trade_after_sale |
| `pay_order` | 支付单(关联微信支付) | 芋道pay_order精简 |
| `pay_refund` | 退款单 | 芋道pay_refund精简 |

#### 营销相关（3张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `promotion_coupon_template` | 优惠券模板 | 芋道promotion_coupon_template |
| `promotion_coupon` | 用户优惠券实例 | 芋道promotion_coupon |
| `promotion_reward_activity` | 满减活动 | 芋道promotion_reward_activity |

#### 分享奖励（2张）— 自建

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `share_record` | 分享奖励记录(推荐人/被推荐人/订单/奖励金额/状态) | **自建** |
| `share_config` | 分享奖励配置(奖励比例/固定金额/冻结天数) | **自建** |

#### 内容+系统（5张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `content_banner` | 轮播图 | 芋道promotion_banner |
| `content_notice` | 公告 | **自建** |
| `sys_admin_user` | 后台管理员 | 芋道system_users精简 |
| `sys_role` | 角色 | 芋道system_role精简 |
| `sys_config` | 系统配置(运费规则/订单超时等) | **自建** |

#### 运费（1张）

| 表名 | 说明 | 参考来源 |
|------|------|----------|
| `trade_freight_template` | 运费模板(满X包邮/按地区) | 芋道精简 |

---

## 5. 核心业务流程

### 5.1 实物商品购买流程

```
浏览商品 → 选规格 → 加入购物车 → 确认订单
    → 选地址/优惠券/备注 → 计算价格（商品价+运费-优惠）
    → 微信支付 → 支付回调 → 订单状态→待发货
    → 后台发货(填顺丰单号) → 状态→待收货
    → 用户确认收货 / 15天自动确认 → 已完成
```

### 5.2 课程购买流程

```
浏览课程 → 查看详情/试看 → 立即购买
    → 微信支付 → 支付回调 → 自动"发货"（生成enrollment记录）
    → 用户进入"我的课程" → 选择章节播放
    → 记录播放进度 → 支持断点续播
```

### 5.3 付费会员流程

```
用户进入会员中心 → 选择套餐（月卡/年卡）
    → 微信支付 → 支付回调
    → 创建subscription记录（开始日期+到期日期）
    → 用户获得权益（会员价/优先发货/新品抢先购）
    → 到期前推送提醒 → 续费或过期
```

### 5.4 分享奖励流程

```
用户A分享商品(带专属参数) → 好友B点击链接 → 绑定推荐关系
    → B下单付款 → 确认收货后(或N天冻结期后)
    → 系统计算A的奖励金额 → 入A的钱包余额
    → A可提现到微信零钱
```

### 5.5 订单价格计算链（参考芋道TradePriceCalculator）

```
原始商品金额
  → 会员折扣（如果是付费会员）
  → 满减优惠
  → 优惠券抵扣
  → 运费计算（满X包邮规则）
  = 最终支付金额
```

---

## 6. 接口设计规范

### 6.1 API路径规范

```
用户端API:   /app-api/member/**    (会员)
             /app-api/product/**   (商品)
             /app-api/trade/**     (交易)
             /app-api/course/**    (课程)
             /app-api/promotion/** (营销)

管理端API:   /admin-api/member/**
             /admin-api/product/**
             /admin-api/trade/**
             /admin-api/course/**
             /admin-api/promotion/**
             /admin-api/system/**
```

### 6.2 统一响应格式

```json
{
  "code": 0,
  "msg": "success",
  "data": { ... }
}
```

### 6.3 认证方式

- 用户端：微信登录获取openid → 服务端生成JWT Token → 请求头 `Authorization: Bearer xxx`
- 管理端：账号密码登录 → JWT Token
- Token存Redis，支持续期和踢出

---

## 7. 部署架构

### 7.1 微信云托管配置

```
服务：shop-api（Java后端，端口80）
  - 最小实例：1（避免冷启动）
  - 最大实例：3（自动扩缩容）
  - CPU：1核
  - 内存：2G
  - JVM：-Xms512m -Xmx1024m

数据库：MySQL 8.0（云托管内置）
缓存：  Redis（云托管内置）
存储：  腾讯云COS（图片+视频+课程）
```

### 7.2 CI/CD

```
Git Push → 云托管自动构建Docker镜像 → 滚动部署
```

---

## 8. 分期实施策略

### 第一期（8-10周）— 能买东西 + 能看课程 + 能当会员

**Week 1-2：基础搭建**
- [ ] 初始化Git仓库 + 项目骨架
- [ ] 从芋道提取framework层代码（精简）
- [ ] MySQL建表（30张）
- [ ] Spring Boot启动 + 统一响应/异常/鉴权
- [ ] uni-app项目初始化 + 请求封装
- [ ] 管理后台脚手架
- [ ] 微信云托管环境配置 + 首次部署

**Week 3-4：商品 + 用户**
- [ ] 商品模块（SPU/SKU/分类/搜索）— 参考芋道
- [ ] 用户模块（微信登录/手机号/个人信息/地址）— 参考芋道
- [ ] 购物车 — 复用芋道逻辑
- [ ] 管理后台：商品管理/分类管理

**Week 5-6：交易 + 支付 + 课程**
- [ ] 订单模块（创建/取消/发货/收货/售后）— 参考芋道
- [ ] 微信支付对接（下单/回调/退款）— 复用芋道支付封装
- [ ] 优惠券（模板/领取/使用）— 复用芋道
- [ ] 课程模块（CRUD/视频上传/章节管理/播放权限）— **自建**
- [ ] 管理后台：订单管理/课程管理

**Week 7-8：小程序页面**
- [ ] 首页（Banner/分类/推荐/搜索/公告）
- [ ] 商品（分类/列表/详情/收藏/历史）
- [ ] 课程（列表/详情/播放/我的课程）
- [ ] 购物车 + 下单 + 支付
- [ ] 订单（列表/详情/物流/售后）
- [ ] 个人中心（全部子页面）

**Week 9-10：会员 + 联调 + 提审**
- [ ] 付费会员体系（套餐/购买/权益/会员价）— **自建**
- [ ] 余额/钱包（充值/明细）
- [ ] 管理后台收尾（数据看板/财务/权限/系统设置）
- [ ] 全流程联调测试
- [ ] Bug修复 + 体验优化
- [ ] 准备资质材料 + 提交微信审核

### 第二期（4-6周）— 营销 + 分享奖励

- [ ] 分享奖励体系（推荐关系/奖励计算/冻结/入账）— **自建**
- [ ] 分享海报生成
- [ ] 满减/满赠活动 — 复用芋道
- [ ] 限时秒杀 — 参考芋道
- [ ] 新人礼包
- [ ] 买赠活动
- [ ] 商品评价体系 — 参考芋道
- [ ] 消息中心（订阅消息+站内通知）

### 第三期（4-6周）— 扩展

- [ ] 多商家入驻
- [ ] 电子发票
- [ ] 微信卡包会员卡
- [ ] 手机端管理（如需要）

---

## 9. 关键里程碑

| 时间点 | 里程碑 | 验收标准 |
|--------|--------|----------|
| Week 2 | 框架跑通 | 小程序→Java→MySQL 一条链路通 |
| Week 4 | 商品流程通 | 后台上架商品，小程序能浏览+加购物车 |
| Week 6 | 交易闭环 | 下单→微信支付→发货 完整走通 |
| Week 6 | 课程可用 | 后台上传课程，用户购买后能播放 |
| Week 8 | 前端完整 | 小程序所有页面开发完成 |
| Week 10 | 提交审核 | 全流程测试通过，提交微信小程序审核 |

---

## 10. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| 微信类目审核不通过 | 无法上线 | 提前准备食品经营许可证+保健品资质 |
| 芋道代码理解成本超预期 | 延期 | 只参考不深入，能自写就自写 |
| 课程视频体积大 | COS费用+加载慢 | 视频压缩+分段加载+CDN |
| 微信支付商户号申请慢 | 阻塞支付开发 | 先用Mock支付开发，商户号并行申请 |
| 一人开发工作量大 | 延期 | 严格控制scope，P1全部推后 |

# 药食同源微信小程序商城

微信小程序电商平台，支持实物商品（农副产品+保健品）和虚拟商品（课程研学）。

## 技术栈

- **后端**: Java 25 + Spring Boot 3.5.16 + MyBatis-Plus 3.5.6 + MySQL 8 + Redis 7
- **小程序**: uni-app (Vue2)（基于开源 wx-mall uni-mall 版本，35 页面）
- **管理后台**: Vue3 + Element Plus（待开发）
- **部署**: 微信云托管（Docker 容器）

## 项目结构

```
wechatMiniprogram_Store/
├── docs/superpowers/              # 项目规格与计划
│   ├── specs/                     # 设计规格
│   ├── plans/                     # 实施计划
│   └── status.md                  # 项目状态仪表盘
├── shop-backend/                  # Java 后端（Maven 多模块）
│   ├── shop-framework/            # 基础框架层
│   │   ├── shop-common/           # 通用工具/常量/异常
│   │   ├── shop-starter-web/      # Web配置/CORS
│   │   ├── shop-starter-mybatis/  # MyBatis-Plus配置/分页
│   │   └── shop-starter-security/ # JWT+Spring Security
│   ├── shop-module-product/       # 商品模块
│   ├── shop-module-member/        # 会员模块
│   ├── shop-module-trade/         # 交易模块（购物车/订单/支付/售后/物流）
│   ├── shop-module-system/        # 系统模块
│   ├── shop-server/               # 启动入口
│   └── Dockerfile                 # 云托管部署
├── shop-miniapp/                  # uni-app 小程序（Vue2，35 页面）
│   ├── pages/                     # 首页/分类/购物车/我的/专题/商品详情等
│   ├── utils/api.js               # API 接口路径配置
│   ├── utils/util.js              # 工具函数/请求封装
│   ├── static/images/             # 图标资源
│   ├── store/                     # Vuex 状态管理
│   └── pages.json                 # 小程序全局配置
├── sql/                           # 数据库初始化脚本
│   └── init.sql
└── README.md
```

## 当前协作入口

- [项目状态仪表盘](docs/superpowers/status.md)：查看当前阶段、进度和下一步行动
- [后端三人并行开发分工](docs/superpowers/plans/2026-07-24-backend-three-person-division.md)：查看后端 A/B/C 三条工作线、接口范围、数据表和验收标准
- [后续开发路径规划](docs/superpowers/plans/2026-07-16-next-development-path.md)：查看 Phase 1-5 的整体推进路线
- [交易模块剩余工作清单](docs/superpowers/plans/2026-07-26-trade-remaining-work.md)：查看交易模块从测试阶段到企业交付的 P0/P1/P2 清单
- [交易闭环验收方案](docs/superpowers/plans/2026-07-26-trade-acceptance.md)：查看交易验收流程和自动验收脚本说明

## 当前已完成内容汇总

### 交易闭环 P0 已完成

交易模块当前已经从“用户端能测试下单”推进到“商家端可以处理交易”的 P0 企业验收状态，主要包含：

| 范围 | 已完成内容 | 关键文件/接口 |
|------|------------|---------------|
| 购物车 | 加购、列表、改数量、删除、勾选、立即购买与购物车结算隔离 | `/app-api/cart/**`、`/app-api/buy/add` |
| 地址 | 地址列表、详情、保存、删除、默认地址、结算页选地址 | `/app-api/address/**` |
| 结算下单 | 商品金额、运费、实付金额计算，提交订单，扣库存，清理已结算购物车 | `/app-api/cart/checkout`、`/app-api/order/submit` |
| 支付 | Mock 预支付、Mock 支付成功、支付状态查询、重复支付拦截、取消后禁止支付 | `/app-api/pay/prepay`、`/app-api/pay/mock-success`、`/app-api/pay/query` |
| 订单状态 | 待付款、待发货、待收货、已完成、已取消、退款中、已退款 | `trade_order` |
| 管理端处理 | 管理端订单列表、订单详情、发货 | `/admin-api/trade/order/list`、`detail`、`ship` |
| 物流 | 发货后保存物流公司/单号，小程序可查看物流信息 | `/app-api/order/logistics`、`trade_order_logistics` |
| 售后退款 | 用户申请售后、撤销售后；管理端同意、拒绝售后；拒绝/撤销后恢复订单状态 | `/app-api/order/refund/**`、`/admin-api/trade/after-sale/**` |
| 超时关闭 | 待付款订单自动超时关闭，库存回补，支付单关闭 | `trade.order` 配置、定时任务 |
| 订单日志 | 提交、支付、取消、超时关闭、发货、确认收货、申请售后、退款完成、拒绝售后、撤销售后均有日志 | `trade_order_log` |
| 用户端收口 | 小程序默认隐藏“模拟发货”“模拟退款通过”等开发按钮 | `shop-miniapp/utils/api.js` 的 `TradeDevActionEnabled` |
| 自动验收 | 一条脚本跑通交易主流程并自动清理测试数据 | `scripts/verify-trade-flow.ps1` |

### 给其他同事的补充方向

后续同事可以在当前交易闭环基础上补真实信息和真实能力，建议按职责边界接入：

| 同事/模块 | 建议补充内容 | 对交易模块的影响 |
|-----------|--------------|------------------|
| 商品模块 | 商品真实列表、商品详情、SKU 规格、上下架状态、真实库存 | 交易侧后续从当前 SPU 库存升级为 SKU 库存扣减与商品快照 |
| 内容/首页模块 | 首页 banner、频道、专题、新品、热卖、品牌等真实数据 | 不影响交易主流程，只影响用户选择商品入口 |
| 支付模块 | 微信支付 V3 下单、支付回调验签、真实退款、退款回调 | 替换当前 Mock 支付和 Mock 退款审核结果 |
| 物流模块 | 快递公司编码、真实物流轨迹查询、物流订阅或定时同步 | 替换当前发货后返回的模拟物流轨迹 |
| 管理后台前端 | 订单列表页、订单详情页、发货表单、售后审核页 | 直接调用已经完成的 `/admin-api/trade/**` 接口 |
| 营销模块 | 优惠券、会员价、满减、包邮规则 | 需要接入结算价格计算和下单锁定/核销逻辑 |
| 客户真实资料 | 小程序 AppID/Secret、微信商户号、API v3 密钥、证书、回调域名 | 真实登录、真实支付和真实退款上线前必需 |

补充真实信息时请注意：交易模块已经保存订单商品快照和收货地址快照，商品名称、价格、图片、地址后续变更不应影响历史订单展示。

## 开发流程

本项目采用 **Spec-Driven Development**：
1. `status.md` 是项目状态的唯一真相来源
2. 所有功能先写 spec，再写 plan，再编码
3. AI 协作时读取/更新 specs 来保持上下文同步

## 本地测试流程

### 环境要求

- JDK 25
- Maven 3.8+
- MySQL 8.0
- Redis 7.x
- [HBuilderX](https://www.dcloud.io/hbuilderx.html)（App开发版，用于编译 uni-app）
- [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)

### 第一步：启动基础服务

```bash
# 启动 MySQL（如果使用 Docker）
docker run -d --name shop-mysql -e MYSQL_ROOT_PASSWORD=root -p 3307:3306 mysql:8.0

# 启动 Redis（如果使用 Docker）
docker run -d --name shop-redis -p 6380:6379 redis:7-alpine
```

### 第二步：初始化或迁移数据库

全新数据库使用初始化脚本：

```bash
# 将 SQL 文件复制到容器中并执行（避免字符集问题）
docker exec shop-mysql mysql -u root -proot --default-character-set=utf8mb4 -e "DROP DATABASE IF EXISTS shop;"
docker cp sql/init.sql shop-mysql:/tmp/init.sql
docker exec shop-mysql mysql -u root -proot --default-character-set=utf8mb4 -e "SOURCE /tmp/init.sql;"
```

已有 `shop` 数据库禁止重新执行 `init.sql`。请先备份，再执行增量迁移：

```powershell
$backupFile = "sql\backups\shop-before-migration-$(Get-Date -Format 'yyyyMMddHHmmss').sql"
New-Item -ItemType Directory -Force 'sql\backups' | Out-Null
docker exec shop-mysql mysqldump -uroot -proot shop | Set-Content -Encoding utf8 $backupFile
.\scripts\migrate-db.ps1 -Database shop
```

增量迁移文件位于 `sql/migrations/`。脚本会在 `schema_migration_history` 中保存版本和校验和；已执行迁移会被跳过，修改已记录迁移文件会终止执行。

### 第三步：启动后端服务

```bash
cd shop-backend
# 设置 JAVA_HOME 指向 Java 25（如果默认 Java 版本不是 25）
export JAVA_HOME=/path/to/jdk-25
# 全量构建（必须，确保所有子模块编译到本地仓库）
mvn install -DskipTests -q
# 启动后端（开发环境，使用 Mock 微信登录）
cd shop-server
mvn spring-boot:run
```

> 如需测试**真实微信登录**，先配置 `application-local.yml`（填入真实 AppID/Secret，已 gitignore），然后用双 profile 启动：
> ```bash
> mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
> ```
> 注意：修改子模块代码后需重新执行 `mvn install -DskipTests -q` 才能生效。

后端启动成功后监听端口 `8085`。

**验证接口：**

```bash
# 获取商品分类列表
curl http://localhost:8085/app-api/product/category/list

# 创建测试商品
curl -X POST http://localhost:8085/admin-api/product/spu/create \
  -H "Content-Type: application/json" \
  -d '{"categoryId":1,"name":"宁夏枸杞 500g","introduction":"头茬大果粒","picUrl":"https://via.placeholder.com/400","price":5900,"marketPrice":9900,"stock":100,"status":1}'

# 获取商品分页列表
curl http://localhost:8085/app-api/product/spu/page?pageNo=1&pageSize=10
```

### 交易闭环自动验收

后端服务启动后，可执行：

```powershell
.\scripts\verify-trade-flow.ps1 -BaseUrl "http://localhost:8085"
```

验收脚本会自动跑通下单、支付、管理端发货、确认收货、售后同意、售后拒绝、用户撤销、超时关闭与库存回补，并在结束后清理测试数据。

### 第四步：启动小程序

1. 在微信开发者工具中导入 `shop-miniapp` 目录，AppID 填写你的真实小程序 AppID
2. 在「本地设置」中勾选「不校验合法域名」
3. 如使用 HBuilderX 编译 uni-app：
   - HBuilderX「设置 → 运行配置 → 微信开发者工具路径」→ 选择安装目录
   - 菜单栏「运行 → 运行到小程序模拟器 → 微信开发者工具」
   - HBuilderX 会将 `.vue` 文件编译为微信小程序原生格式，每次保存自动热更新

> HBuilderX 会将 `.vue` 文件编译为微信小程序原生格式，每次保存代码会自动热更新。

> 注意：后端目前已有首页、分类、商品详情等核心 mock 接口，购物车/订单/收藏等接口待补全。

### 测试验证清单

| 验证项 | 预期结果 |
|--------|----------|
| `mvn install -DskipTests` | BUILD SUCCESS |
| 后端启动（端口 8085） | 日志无报错 |
| HBuilderX 运行到微信开发者工具 | 编译成功，自动打开模拟器 |
| 小程序首页 | Banner + 分类导航 + 品牌商品 |
| 小程序分类页 | 左侧分类 + 右侧子分类 |
| 小程序商品详情 | 轮播图 + 商品参数 + 常见问题 |
| 小程序购物车 | 真实交易接口可用，可加购、删除、勾选、结算 |
| 交易验收脚本 | 自动验收通过并清理测试数据 |

## 许可证

Private

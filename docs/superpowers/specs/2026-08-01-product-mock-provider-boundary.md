# Issue #23 商品内容 Mock Provider 与正式 API 边界规格

> 日期：2026-08-01
> 状态：已确认
> Issue：[GitHub #23](https://github.com/QtImM/wechatMiniprogram_Store/issues/23)

## 目标

商品浏览的正式 API 只依赖统一 Provider 契约，由 `product.provider` 选择 Mock 或数据库实现。Mock 商品种子不再位于 Controller 包，正式 Controller 与业务 Service 不得直接读取 Mock Fixture。

## 功能范围

1. 建立商品目录 Provider 契约，覆盖分类、列表、详情、关联商品与商品数量。
2. 把商品 Mock 数据迁入独立 Fixture，并由 Mock Provider 组装与数据库实现兼容的响应。
3. 正式商品 Controller 经统一路由服务选择 Mock 或数据库 Provider，API 路径和字段保持不变。
4. 把热销、新品、品牌、专题等正式路径从 `AppMockController` 迁入正式内容 Controller 和数据库查询 Service。
5. `AppMockController` 只保留 `/app-api/mock/**` 兼容路径；通过统一配置控制这些入口，生产环境强制拒绝。
6. 为 Provider 契约、配置路由、Mock 入口环境守卫和正式响应增加测试。

## 兼容约束

- 不改变小程序现有正式 API 路径。
- 商品列表与详情保留现有兼容字段、价格格式、稳定排序和 SKU 可售信息。
- `product.provider=mock|database` 只改变数据来源，不改变 Controller。
- 开发环境可显式开启 `product.mock-endpoints-enabled`；生产环境无论配置值如何都拒绝 `/app-api/mock/**`。
- 不修改交易状态机、支付、物流、前端本地 `utils/mock.js` 或数据库结构。

## 验收标准

- 正式 Controller/Service 中不存在对 Mock Fixture 的直接引用。
- `MockProductSkuProvider` 不再依赖 Controller 包。
- Mock 与数据库商品 Provider 使用同一接口，核心响应契约断言通过。
- `AppMockController` 不再承载正式路径。
- 开发开关关闭及生产 profile 下访问 `/app-api/mock/**` 均返回业务码 `403`，正式浏览接口仍按配置工作。
- 商品与交易模块测试、Docker 全量构建，以及 Mock/数据库双模式 HTTP 验收通过。

## 非目标

- 不补充新的演示种子规模；该工作属于 Issue #24。
- 不移除小程序本地 Mock 或改造全页面 API；该工作属于 Issue #25。
- 不引入缓存、消息队列或第三方服务。

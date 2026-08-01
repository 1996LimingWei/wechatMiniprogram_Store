# 交易权限与 Mock 写操作边界设计

> 日期：2026-08-01  
> 优先级：P0  
> 背景：交易审计确认管理端匿名开放，且 Mock 支付、发货和退款写操作可被直接调用。

## 目标

1. 管理端订单、售后、商品接口只能由独立管理员身份访问。
2. Mock 写操作只在 `local/dev/test` 环境可用，且必须经过已登录用户校验；生产环境无论配置为何值均拒绝。
3. 自动验收使用明确的测试管理员和测试环境开关，不再依赖匿名管理端访问。

## 方案

- 新增最小管理员认证模型与 `ROLE_ADMIN` 权限；管理员 Token 与小程序会员 Token 分离。
- `SecurityAutoConfiguration` 将 `/admin-api/**` 改为要求 `ROLE_ADMIN`；小程序公开读取接口按现有路径逐项保留，私有写操作继续由服务层校验用户身份。
- 新增 `trade.mock-actions-enabled` 配置属性，统一守卫以下接口：`/app-api/pay/mock-success`、`/app-api/order/mock-ship`、`/app-api/order/refund/mock-approve`。
- `application-prod.yml` 强制 `trade.mock-actions-enabled=false`；开发、测试 profile 可显式开启，禁止默认由前端展示逻辑决定。
- 验收脚本改为先获取测试管理员 Token，再调用管理端接口；增加匿名管理端和生产 Mock 写接口拒绝断言。

## 非目标

- 不在本阶段实现完整后台 UI、细粒度菜单权限或真实微信支付。
- 不改变现有小程序业务接口的响应结构。

## 验收

- 匿名与普通会员 Token 请求任意 `/admin-api/**` 分别返回 401/403。
- 管理员 Token 可完成既有订单和售后操作，并记录管理员操作人。
- 生产 profile 的三类 Mock 写接口均不可调用；开发测试 profile 按开关生效。
- 自动验收与安全回归在隔离 Docker 环境通过。

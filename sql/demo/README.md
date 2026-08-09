# 管理后台交易演示数据

本目录仅用于 local/dev 环境，禁止在生产数据库执行。

## Docker 环境

先执行最新迁移，再导入演示数据：

```powershell
Get-Content sql/migrations/V20260806_01__trade_provider_metadata.sql -Raw |
  docker exec -i shop-mysql mysql -uroot -proot shop

Get-Content sql/demo/trade_admin_demo.sql -Raw |
  docker exec -i shop-mysql mysql -uroot -proot shop
```

## 数据范围

- 演示用户：`810000-819999`
- 演示订单：`810000-814999`
- 演示订单项：`815000-819999`
- 演示支付单：`820000-824999`
- 演示物流：`825000-829999`
- 演示售后：`830000-839999`

脚本每次只删除并重建上述预留区间，其他数据不会被修改。获得真实客户数据后停止执行本脚本即可，管理后台与正式 API 不需要切换。

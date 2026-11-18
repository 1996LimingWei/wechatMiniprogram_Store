# 成套备份和隔离恢复工具

状态：已通过离线单元测试；真实 Docker/MySQL 备份恢复尚未验证，不作为生产恢复承诺。

要求：Python 3.10+、Docker Compose、已填写的 `.env.prod`。工具根据生产 Compose 读取现有 MySQL 容器环境中的密码，不把密码放入宿主机命令参数或输出。

## 发布预检

```powershell
python scripts/release_preflight.py --env .env.prod
```

该命令只检查环境变量，不证明域名、证书、数据库或支付渠道正常。占位配置、弱密码、通配 CORS 和未知交付模式会拒绝通过。catalog 不要求未启用的微信渠道凭据，commerce 要求相应资料。

## 成套备份

```powershell
python scripts/delivery_backup.py backup --env .env.prod --database shop --archive deploy/backups/shop-20261104.zip
python scripts/delivery_backup.py verify --archive deploy/backups/shop-20261104.zip
```

工具确认后端原先正在运行，暂停后端写入，导出数据库并归档 `deploy/data/material`，写入每个文件的 SHA256 清单，最后恢复后端运行。失败不覆盖已有备份。该操作有短暂维护窗口，应安排在低峰期。系统外若还有其他数据库写入者，必须一起停止后再备份。

备份包包含客户数据，当前格式是未加密 ZIP；SHA256 只检测损坏，不提供来源认证。正式自动备份使用 `run_backup_cycle.py`，并配置异机挂载、访问控制、保留周期与告警，不能把本机 ZIP 等同于灾备。证书、密钥和环境文件不包含在该包内，应由独立的安全备份策略管理。

## 隔离恢复演练

```powershell
python scripts/delivery_backup.py restore --env .env.prod --database shop_restore_20261104 --archive deploy/backups/shop-20261104.zip --material deploy/restore-check/20261104-material
```

恢复仅接受名称含 `_restore_` 的新数据库，且目标数据库与素材目录均不得存在。工具不会覆盖生产数据库或生产素材，也不会自动让线上程序切换到恢复库。导入失败会保留现场供排查，不能将失败现场标为恢复成功。

只恢复由自己生成并可信保管的备份。演练后核对商品/SKU/库存/图片、订单金额、支付与退款单据及迁移历史；恢复内容的业务正确性不由文件校验和代替。生产事故恢复需要另行核对备份时间点之后的真实资金变化。

## 本地测试

```powershell
python scripts/test_release_preflight.py
python scripts/test_delivery_backup.py
node scripts/verify-miniapp-env.js
```

当前包括配置模式隔离、弱密码/CORS 拒绝、数据库与素材往返、篡改拒绝、路径越界和生产库恢复保护等 9 项 Python 测试，以及 5 项小程序环境用例。它们不依赖客户服务器，因此可以先完成。

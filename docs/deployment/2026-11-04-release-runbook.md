# 陕铺发布执行手册

本手册对应 `codex/customer-delivery-closure` 收口版本。工具已完成离线测试；本机 Docker 无法连接，首次真实部署必须先在 staging 执行本手册并归档结果。不能把这些命令已经写好视为已部署成功。

## 1. 客户需提供的条件

服务器、域名/DNS 和 HTTPS 证书，小程序与商户协作权限、实际商品/库存/运费及售后资料。录入后台不等待微信支付资料。域名和云账号归客户所有，开发者使用授权账号。

建议 Linux 云主机，Docker Compose V2、Git、Python 3.12；当前实现使用本机目录保存图片。数据库、Redis、8085 不开放公网，仅开放所需 HTTPS 与受控运维入口。

### 已有宝塔环境的接入前检查

客户本次资料包含宝塔账号和域名证书。取得完整入口及运维授权后，先只读盘点操作系统、资源、现有网站、Nginx、数据库、80/443 和已有备份。账号可用不等于主机为空，不能停止或覆盖原网站来腾端口。

当前 Compose 默认发布 80/443，已有宝塔监听时不能直接执行首次部署。技术方应根据盘点结果实施并验证宿主反向代理或隔离端口适配；适配与回滚通过后才部署正式录入环境。本手册当前不代表已完成该客户主机适配。

现有证书覆盖根域名和 www；使用其他子域名需要对应 DNS 授权及覆盖该名称的证书。微信支付商户证书用于请求签名，不能填到微信平台验签证书路径；使用客户提供的支付公钥时还需匹配公钥 ID。全部材料通过受控配置使用，不打包进镜像或教程。

## 2. 准备录入版

在固定、已经审查的 commit 上执行，工作目录示例 `/opt/shanpu`：

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-delivery.txt
.venv/bin/python scripts/configure_delivery.py --domain <客户实际域名>
.venv/bin/python scripts/release_preflight.py --env .env.prod
.venv/bin/python scripts/generate_migration_manifest.py --check
```

配置生成器默认 `catalog` 模式，生成数据库、Redis、初始管理员和备份的独立随机密码，拒绝覆盖已有环境文件。不要把 `.env.prod` 发群或提交 Git。初始管理员账号是 `admin`，初始密码在该私有文件的 `DELIVERY_ADMIN_PASSWORD` 中，按客户认可的方式移交，客户修改后删除该配置项。

将证书放到 `deploy/secrets/tls/fullchain.pem` 与 `privkey.pem`。证书、环境文件和备份只授予必要用户权限；Windows 的 `chmod` 不等于完整 ACL，正式权限必须在 Linux 主机确认。

## 3. 首次部署

在干净的构建机从本次固定 commit 封存镜像包，只构建一次：

```bash
.venv/bin/python scripts/release_images.py --env .env.prod --output deploy/releases/candidate-<版本>
```

该目录含 `images.tar` 与 `manifest.json`，不包含配置、私钥或数据库。先在隔离测试主机验收该包，再将同一目录通过受控通道传到客户主机，双方使用同一源码 commit。保留已验收包和发布记录，不覆盖旧包。

完整容器 CI 成功后会提供 `catalog-verified-images-<commit>` 下载包，内含本次验收的镜像归档与 manifest，临时保留 7 天；交付方需在到期前转存到受控发布存储并核对 SHA256。客户服务器架构须与该产物一致，架构不同时重新构建并完成对应验收。不能把临时 Actions 附件当作长期发布存储。

```bash
.venv/bin/python scripts/deploy_release.py initialize --env .env.prod --bundle deploy/releases/candidate-<版本>
```

流程核对源码版本与镜像包 SHA256，导入精确镜像；再执行环境预检、配置校验、启动数据库与 Redis、空库初始化与全量迁移、清理演示业务数据、设置管理员密码、准备素材目录属主、启动应用、HTTPS 业务就绪检查、记录 commit、包校验值与镜像。部署禁止现场重新构建。

首次初始化只接受空库，失败后保留现场，不会自动重置或清空非空库。不要通过删除数据库再试来处理已有客户环境。初始化过程中后端必须保持停止，数据库不对外暴露。

完成后登录后台，创建商品运营账号，验证分类、图片上传、单/多规格商品、Excel 预检及导入、默认下架、重启持久化。正式数据在此数据库中持续维护，后续开放交易不另建一套客户商品库。

## 4. 后续升级

```bash
.venv/bin/python scripts/deploy_release.py upgrade --env .env.prod --bundle deploy/releases/candidate-<版本> --backup deploy/backups/before-release-<版本>.zip --offsite-dir /mnt/shanpu-backup
```

工具要求升级前成套备份，从备份到迁移保持后端停止；校验归档后加密写入独立存储，并读回解密比较 SHA256，全部成功才继续迁移。`--offsite-dir` 必须为挂载根目录，部署负责人另行核实其存储确实处于独立故障域；挂载检测本身不能证明异机。配置缺失在停机前拒绝；备份或迁移失败时保持后端停止，运维核对现场后恢复，禁止未经检查直接重跑或开放写入。

迁移前核对全部历史校验和。没有迁移历史的数据库直接拒绝升级，不自动创建基线或执行历史种子；先在隔离副本核对数据库来源。历史校验冲突时停止，不能手工覆盖记录强行通过。数据库曾使用 10 月改名后的历史 SQL 时，需要拿副本核对差异，决定经审查的修复步骤。

部署要求与源码版本一致的固定镜像包，逐个核对导入后的镜像 ID；不接受 `latest` 等可变标签。上线前保留上一版本镜像包和配置。工具不自动回退数据库。

## 5. 加密异机备份

先配置独立存储挂载根目录 `/mnt/shanpu-backup`，环境文件中填好 `BACKUP_ALERT_URL`。当前通知格式为通用 JSON `{"text":"..."}`，所选通知服务需要接受该格式，交付时必须实际测试收到通知；否则应使用匹配该服务的转接入口。

```bash
.venv/bin/python scripts/run_backup_cycle.py --env .env.prod --offsite-dir /mnt/shanpu-backup
```

没有独立挂载时任务拒绝执行，不会把本机目录冒充异机灾备。备份包含数据库和图片，经过清单校验及 AES-256-GCM 加密后复制，核对异机文件哈希；密码独立安全保管。默认不自动删除历史备份，正式运维需按容量与客户保留要求配置存储生命周期，避免磁盘持续增长。

`deploy/systemd/shanpu-backup.service` 和 `.timer` 是每30分钟任务模板，超过60分钟没有有效备份触发状态告警。该频率用于支撑待确认的1小时恢复点目标，仍须实测备份耗时与最后可恢复时间；不能仅凭任务频率承诺 RPO。默认路径 `/opt/shanpu`，安装前按实际路径核对。备份有短暂后端停写窗口；高频交易阶段如需无停写方案，应增加数据库日志及文件一致性策略后验证。

备份状态记录在 `deploy/backup-status.json`，监控必须同时检查最后成功时间；只依靠“任务运行失败时发通知”无法发现整个定时器没有运行。

## 6. 隔离恢复

### 备份保留

```bash
.venv/bin/python scripts/backup_retention.py --env .env.prod --offsite-dir /mnt/shanpu-backup
# 确认预览后，由运维启用执行；客户无需运行命令。
.venv/bin/python scripts/backup_retention.py --env .env.prod --offsite-dir /mnt/shanpu-backup --apply
```

默认只预览；执行保留48小时内全部定期副本、30天内每日最新副本，以及至少两个最新恢复点。清理前两个最新保留副本必须均能解密并通过完整归档校验，失败时不删除；不处理升级前副本、未知文件名或符号链接。`shanpu-retention.service/.timer` 提供每日03:15执行模板，运维需先核实专用挂载目录、权限、容量和保留约定。删除的过期副本不能由此工具撤销，保留的恢复点仍可使用。

部署、回滚、定期备份、恢复和保留入口共用非阻塞互斥锁，防止备份与升级同时改变服务状态。检测到已有任务时拒绝本次操作；不要删除锁文件来绕过检查。Windows可运行离线测试，正式运维入口要求Linux。

### 恢复操作

```bash
.venv/bin/python scripts/backup_crypto.py decrypt /mnt/shanpu-backup/<备份>.enc /受控临时目录/restore.zip
.venv/bin/python scripts/delivery_backup.py restore --env .env.prod --database shop_restore_<日期> --archive /受控临时目录/restore.zip --material deploy/restore-check/<日期>-material
```

恢复数据库和素材目录必须不存在，不覆盖线上数据。解密密码通过交互输入或受控环境注入，不放在命令参数中。核对图片、商品/SKU/库存、订单金额、支付退款和迁移历史；演练完成后按运维流程处理含客户数据的临时文件。

## 7. 开放商城交易

保持原库与素材目录，补齐客户正式 AppID/Secret、商户关联、私钥、API v3 Key、验签材料、退款回调及快递 100 信息；设置 `APP_DELIVERY_MODE=commerce` 和 `WECHAT_PAY_ENABLED=true`，重新预检并发布。

验签支持平台证书或微信支付公钥：公钥模式填写 `WECHAT_PAY_PUBLIC_KEY_ID/PATH`，清空不使用的单证书路径；证书轮换可将多个有效证书路径放入 `WECHAT_PAY_PLATFORM_CERTIFICATE_PATHS`（逗号分隔）。证书必须在有效期内。首次切换与轮换都必须执行真实回调验证，不能仅凭本地 RSA 测试放行。

小程序 API 基地址填写域名根地址，工具会规范化旧的 `/app-api/` 后缀。正式版必须以对应生产配置重新编译，不能直接发布开发模式构建产物。

完成真实登录、支付、退款、发货、物流、确认收货、评价、反馈和日终对账；按客户账号核对微信发货信息管理及结算要求。iOS/Android 真机、微信审核与客户签收完成后才标记 M2 交付完成。

## 8. 回滚与事故

应用镜像回滚入口（由技术运维执行）：

```bash
.venv/bin/python scripts/rollback_release.py --env .env.prod --release deploy/releases/release-<目标版本部署时间>.json --bundle deploy/releases/<目标版本>
```

工具核对已部署记录、配置校验值、镜像包校验值及目标提交的迁移清单，只有数据库结构清单完全一致才自动执行；不会回放旧 SQL，不会恢复数据库。旧发布记录没有配置校验值时拒绝执行，需要先完成兼容审查。通过后保留现有数据库和素材，重新启动指定应用镜像并检查 HTTPS 就绪；失败时入口保持停止。迁移清单相同仍需业务回归，不能代替跨版本实际演练；跨结构版本需单独设计恢复路径。

应用异常优先停止入口，回滚到已保存的镜像和兼容配置；保留所有上线后订单。数据库恢复先在隔离库演练，结合微信账单补核备份时间点之后的资金状态，禁止直接用旧库覆盖新增真实交易。涉及资金和数据损坏由客户负责人、财务及技术负责人共同确认恢复路径。

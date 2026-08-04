# Issue #6：内容运营管理（Banner / 频道 / 品牌 / 专题）

> 管理后台 Issue #6 — 为四个内容模块提供后端 CRUD 接口和前端管理页面。

---

## 范围

| 子页面 | 数据库表 | 功能 |
|--------|---------|------|
| Banner 管理 | `content_banner` | 列表（图片预览+标题+排序+状态）；新增/编辑（标题、图片URL、跳转链接、排序、启用/关闭） |
| 频道管理 | `content_channel` | 列表（图标+名称+链接+排序+状态）；新增/编辑/删除 |
| 品牌管理 | `content_brand` | 列表（图片+名称+起售价+排序+状态）；新增/编辑/删除 |
| 专题管理 | `content_topic` | 列表（图片+标题+副标题+排序+状态）；新增/编辑/删除 |

## 后端接口

| 路径 | 方法 | 说明 |
|------|------|------|
| `/admin-api/content/banner/list` | GET | Banner 全量列表（按 sort 降序） |
| `/admin-api/content/banner/create` | POST | 新增 Banner |
| `/admin-api/content/banner/update` | PUT | 更新 Banner |
| `/admin-api/content/banner/delete` | DELETE | 删除 Banner（逻辑删除） |
| `/admin-api/content/channel/list` | GET | 频道全量列表 |
| `/admin-api/content/channel/create` | POST | 新增频道 |
| `/admin-api/content/channel/update` | PUT | 更新频道 |
| `/admin-api/content/channel/delete` | DELETE | 删除频道 |
| `/admin-api/content/brand/list` | GET | 品牌全量列表 |
| `/admin-api/content/brand/create` | POST | 新增品牌 |
| `/admin-api/content/brand/update` | PUT | 更新品牌 |
| `/admin-api/content/brand/delete` | DELETE | 删除品牌 |
| `/admin-api/content/topic/list` | GET | 专题全量列表 |
| `/admin-api/content/topic/create` | POST | 新增专题 |
| `/admin-api/content/topic/update` | PUT | 更新专题 |
| `/admin-api/content/topic/delete` | DELETE | 删除专题 |

## 后端实现

- `ContentAdminService`：统一 Service，注入四个 Mapper，提供 16 个方法
- `AdminContentController`：统一 Controller，挂载在 `/admin-api/content` 下
- 复用已有的 `ContentBannerDO`、`ContentChannelDO`、`ContentBrandDO`、`ContentTopicDO`
- 复用已有的 `ContentBannerMapper`、`ContentChannelMapper`、`ContentBrandMapper`、`ContentTopicMapper`
- 列表查询按 `sort DESC, id ASC` 排序

## 前端实现

- API 层：`src/api/content.ts`（已预定义）、类型 `src/api/types.ts`（已预定义）
- 路由：`src/router/modules/content.ts`（已预定义）
- 四个页面均为：表格列表 + 新增/编辑对话框模式
- 品牌管理的价格字段使用分↔元转换（与商品管理保持一致）
- 所有图片字段使用 URL 输入 + 实时预览
- 所有不直观字段添加 `el-tooltip` + `QuestionFilled` 问号帮助提示

## 验收标准

1. 四个内容模块各自可增删改查 ✅
2. 图片上传（URL 输入）正常展示预览 ✅
3. 排序和状态切换即时生效 ✅
4. 小程序首页能看到后台修改后的 Banner 和频道内容（需重启后端使新接口生效）✅

## 构建验证

- 后端：`mvn clean install -DskipTests` — 0 错误
- 前端：`vue-tsc --noEmit` — 0 错误

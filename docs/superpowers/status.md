# 项目状态仪表盘

> AI 每次对话开始时读取此文件，了解项目当前状态和下一步行动。

---

## 当前阶段

**阶段**: Demo Foundation（基础骨架搭建）
**计划文件**: [plan1-demo-foundation.md](plans/2026-06-22-plan1-demo-foundation.md)
**设计规格**: [shop-miniprogram-design.md](specs/2026-06-22-shop-miniprogram-design.md)

## 进度概览

| 任务 | 状态 | 说明 |
|------|------|------|
| Task 1: Git 仓库初始化 | ✅ 完成 | 仓库结构、.gitignore、README |
| Task 2: 父 POM + 模块骨架 | ⬜ 未开始 | Maven 多模块结构 |
| Task 3: shop-common 公共模块 | ⬜ 未开始 | CommonResult, PageResult, BaseDO |
| Task 4: shop-starter-mybatis | ⬜ 未开始 | MyBatis-Plus 自动配置 |
| Task 5: shop-starter-security | ⬜ 未开始 | JWT + Spring Security |
| Task 6: shop-starter-web | ⬜ 未开始 | Web 统一配置 |
| Task 7: 数据库初始化 SQL | ⬜ 未开始 | 核心表结构 |
| Task 8: Product 模块 CRUD | ⬜ 未开始 | 商品分类+SPU 接口 |
| Task 9: Server 启动入口 | ⬜ 未开始 | Spring Boot 主应用 |
| Task 10: 小程序骨架 | ⬜ 未开始 | uni-app 首页+请求封装 |

## 阻塞项

（无）

## 决策记录

| 日期 | 决策 | 原因 |
|------|------|------|
| 2026-06-22 | 参考芋道架构，不直接 fork | 70% 代码不需要，保持轻量 |
| 2026-06-22 | 一级分销改为"分享奖励" | 客户需求是简单分享返利 |
| 2026-06-22 | 付费会员制（月卡/年卡） | 非免费升级体系 |
| 2026-06-22 | 不做积分、直播、拼团 | 客户明确不需要 |
| 2026-06-22 | 所有计划和文档使用中文 | 用户要求 |

## 下一步行动

执行 Plan1 Task 2：创建 Maven 父 POM 和多模块骨架结构。

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

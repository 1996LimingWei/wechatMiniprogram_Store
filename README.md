# 药食同源微信小程序商城

微信小程序电商平台，支持实物商品（农副产品+保健品）和虚拟商品（课程研学）。

## 技术栈

- **后端**: Java 17 + Spring Boot 3.2 + MyBatis-Plus + MySQL 8 + Redis
- **小程序**: uni-app (Vue3 + TypeScript + Pinia)
- **管理后台**: Vue3 + Element Plus
- **部署**: 微信云托管

## 项目结构

```
codes/
├── docs/superpowers/        # 项目规格与计划（Spec-Driven）
│   ├── specs/               # 设计规格
│   ├── plans/               # 实施计划
│   └── status.md            # 项目状态仪表盘
├── backend/                 # Java 后端（待创建）
├── miniapp/                 # uni-app 小程序（待创建）
└── admin/                   # Vue3 管理后台（待创建）
```

## 开发流程

本项目采用 **Spec-Driven Development**：
1. `status.md` 是项目状态的唯一真相来源
2. 所有功能先写 spec，再写 plan，再编码
3. AI 协作时读取/更新 specs 来保持上下文同步

## 许可证

Private

# Agent Loop Skill 实施计划

> **适用于 Agent：** 创建或修改此计划中的 skill 时，先使用 `superpowers:writing-skills` 进行场景验证。

**目标：** 创建项目版和全局版两份独立的 Agent Loop skill，并完成格式与场景校验。

**架构：** 项目版保留仓库协作约束；全局版仅提供通用闭环。两者均引用 OpenAI Codex Agent Loop 原文。

---

### 任务 1：创建并校验 skill

**文件：**

- 创建：`skills/agent-loop/SKILL.md`
- 创建：`skills/agent-loop/agents/openai.yaml`
- 创建：`C:\Users\Tim\.codex\skills\agent-loop\SKILL.md`
- 创建：`C:\Users\Tim\.codex\skills\agent-loop\agents/openai.yaml`

- [x] 编写两个独立的 `SKILL.md`，使用 `name`、`description` frontmatter，并包含完成标准、行动、观察、调整、停止条件和 OpenAI 引用。
- [x] 为两个版本创建 UI 元数据。
- [x] 运行 `quick_validate.py` 校验两个 skill 目录。
- [x] 使用真实任务场景验证全局 skill 的闭环行为。

### 任务 2：记录项目状态并提交

**文件：**

- 修改：`docs/superpowers/status.md`

- [x] 记录项目版 skill 的位置、独立维护方式和引用来源。
- [x] 在校验通过后创建包含项目内文档与 skill 的中文提交；全局 skill 不纳入 Git。

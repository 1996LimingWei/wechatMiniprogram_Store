# Agent Loop Skill 设计

## 目标

创建基于 OpenAI Codex Agent Loop 的通用工作 skill，使 Agent 在多步骤任务中用环境反馈持续校验并调整行动。

## 两个独立版本

- `skills/agent-loop/`：团队项目版，要求先读取项目规则、计划和状态，并在完成时更新状态。
- `C:\Users\Tim\.codex\skills\agent-loop\`：个人全局版，不依赖任何仓库路径或技术栈。

两个版本独立维护，均以 OpenAI 的 [Unrolling the Codex agent loop](https://openai.com/index/unrolling-the-codex-agent-loop/) 为引用源。

## 行为

两个版本都要求：定义完成标准和授权边界；选择最小安全行动；根据工具输出等证据判断完成、进展、无效或受阻；无效时改变方法；在验收通过或需要用户输入时退出。

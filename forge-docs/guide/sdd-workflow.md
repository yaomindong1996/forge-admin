# 渐进式 Spec 驱动开发（SDD）

Forge Admin 项目采用 **Spec 驱动开发（Spec-Driven Development）** 工作流，核心理念是 **"No Spec, No Code"**——代码是廉价的消耗品，文档（Spec）才是昂贵的核心资产。

## 核心原则

| 原则 | 说明 |
|------|------|
| **No Spec, No Code** | 没有确认的 Spec，不准写代码 |
| **Spec is Truth** | Spec 和代码冲突时，错的一定是代码 |
| **Reverse Sync** | 执行中发现 Spec 与实际不符，先修 Spec 再修代码 |
| **代码现状必须有出处** | 每个结论必须标注文件路径和类名/方法名 |
| **变更即记录** | 任何代码变更完成后都必须同步更新对应文档 |

## 工作流概览

```
/spec-init → /propose → /apply → /review → /test → /archive
                 ↕              ↕
               (确认)          /fix (迭代)
```

## 命令详解

### `/spec-init` — 初始化项目上下文

分析工程结构、依赖、分层模式，自动生成 `code-copilot/rules/project-context.md`。

**用途**：首次使用或项目结构发生重大变化时运行，让 AI 助手了解当前项目的技术栈、目录结构、模块依赖关系等。

**产出**：`code-copilot/rules/project-context.md`

---

### `/propose <需求描述>` — 创建变更提案

将自然语言需求转化为结构化的 Spec 文档。

**流程**：
1. **Research** — 研究现有代码库，查找相关入口和已有实现
2. **逐个提问** — 一次只问一个待澄清问题，提供选项和推荐方案
3. **YAGNI 裁剪** — 去除不必要的功能，保持最小可行范围
4. **生成 Spec** — 分三段生成，每段确认后再生成下一段
5. **生成 Tasks** — 将 Spec 拆解为原子化的执行任务（每个 task 3-5 个文件）
6. **HARD-GATE 确认** — 用户明确确认后才能进入 `/apply`

**产出**：`code-copilot/changes/<变更名>/spec.md` + `tasks.md`

#### Spec 文档结构

Spec 文档包含 13 个章节：

| 章节 | 内容 |
|------|------|
| 1. 背景与目标 | 为什么做 + 可验证的结果描述 |
| 2. 代码现状 | 相关入口、现有实现、发现与风险（附代码出处） |
| 3. 功能点 | 输入 → 处理 → 输出 |
| 4. 业务规则 | 业务约束和规则 |
| 5. 数据变更 | 表名、字段、索引变更 |
| 6. 接口变更 | 接口路径、方法、变更内容 |
| 7. 影响范围 | 受影响的模块和文件 |
| 8. 风险与关注点 | ⚠️ 资金/状态流转/权限变更必须标注 |
| 8.5 测试策略 | 测试范围和覆盖率目标 |
| 9. 待澄清 | 必须全部解决才能进入 `/apply` |
| 10. 技术决策 | 技术方案选择及理由 |
| 11. 执行日志 | Task 状态、实际改动文件、备注 |
| 12. 审查结论 | 两阶段审查结果 |
| 13. 确认记录 | HARD-GATE 确认信息 |

---

### `/apply <变更名>` — 执行编码

根据已确认的 Spec 逐 task 执行编码。

**前置检查**：
- Spec 文档存在且状态为 propose
- Tasks 文档存在
- 用户已明确确认

**执行规则**：
1. **逐 task 执行** — 每个 task 是原子化的（3-5 个文件）
2. **验证铁律** — 每个 task 完成后必须展示编译通过证据
3. **零偏差原则** — Plan 是合同，严格按 Spec 执行
4. **自动 commit** — 一个 task 一个 commit
5. **文档同步** — 同步更新 spec/tasks/log 文档

**产出**：代码变更 + Git 提交 + 文档更新

---

### `/fix <变更名> [描述]` — Review 后修正

根据 Review 意见进行增量修正。

**规则**：
- 读取当前 spec/tasks/log 文档
- 增量修改，不重写
- 验证编译通过
- 同步更新所有文档
- Git commit

---

### `/review <变更名>` — 两阶段代码审查

采用两阶段审查策略，使用专用 Sub Agent 执行（上下文隔离）。

| 阶段 | 内容 | 执行者 |
|------|------|--------|
| **阶段一** | Spec 合规性审查 — 逐条验证 Spec 功能点是否在代码中实现 | Spec Reviewer Agent |
| **阶段二** | 代码质量审查 — 正确性、安全性、性能、代码质量 | Code Quality Reviewer Agent |

**规则**：阶段一必须 PASS 后才启动阶段二。

#### 审查结果分级

| 级别 | 说明 |
|------|------|
| **PASS** | 无问题或仅有建议性意见 |
| **PASS_WITH_COMMENTS** | 有建议性意见，非阻塞 |
| **FAIL** | 有阻塞性问题，必须修复 |

---

### `/test <变更名>` — 生成测试

采用 Red/Green TDD 策略生成单元测试。

**两种模式**：
1. **Spec 先行（推荐）** — 先生成测试 Spec，再生成测试代码
2. **直接生成** — 直接生成测试代码

**规则**：
- 测试必须先 Red（失败），再 Green（通过）
- 确保测试能真正验证业务逻辑

---

### `/archive <变更名>` — 归档 + 知识沉淀

变更完成后归档并沉淀知识。

**流程**：
1. 展示 `log.md` 中的知识发现
2. 用户确认哪些需要沉淀
3. 写入 `knowledge/tech-*.md`
4. 移动变更到 `archive/YYYY-MM-DD-<名称>/`
5. 设置 Spec 状态为 `done`

**产出**：归档的变更 + 新的知识文章

---

## 变更管理

所有变更产物统一存放在 `code-copilot/changes/[变更名]/` 目录下：

```
code-copilot/changes/
├── templates/              # 模板文件
│   ├── spec.md             # Spec 模板
│   ├── tasks.md            # Task 拆解模板
│   ├── log.md              # 执行日志模板
│   └── test-spec.md        # 测试策略模板
├── <变更名>/               # 具体变更
│   ├── spec.md             # 需求规格
│   ├── tasks.md            # 任务拆解
│   ├── log.md              # 执行日志
│   └── test-spec.md        # 测试策略
└── archive/                # 已归档的变更
    └── YYYY-MM-DD-<名称>/
        ├── spec.md
        ├── tasks.md
        ├── log.md
        └── test-spec.md
```

## 知识沉淀

有价值的技术发现会沉淀到 `code-copilot/knowledge/` 目录：

| 文件 | 内容 |
|------|------|
| `index.md` | 知识索引 |
| `tech-spring-boot-autoconfig.md` | Spring Boot 自动配置注册 |
| `tech-maven-multimodule.md` | Maven 多模块添加 Starter |
| `tech-spel-expression.md` | SpEL 表达式解析 |
| `tech-aop-parameter-names.md` | AOP 参数名发现 |
| `tech-global-exception-handler.md` | 全局异常处理 |

## Git 规范

| 规则 | 说明 |
|------|------|
| 禁止 master 分支变更 | 必须使用 feature 分支 |
| 一个 task 一个 commit | 每个 task 完成后自动 commit |
| Commit 必须可编译 | 每次提交必须保证编译通过 |
| 禁止自动 push | 需要用户手动触发 push |
| Message 格式 | `[<变更名>] <中文简述>` |

## 调试流程

遇到问题时遵循四阶段流程：

1. **根因调查** — 找到问题的根本原因
2. **模式分析** — 分析代码模式和可能的陷阱
3. **假设验证** — 验证修复假设
4. **实施修复** — 实施修复并验证

**禁止在未确认根因前直接改代码。**

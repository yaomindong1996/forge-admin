# AI 代码生成稳定性重构
> status: apply
> created: 2026-04-27
> complexity: 🟡中等

## 1. 背景与目标
在不影响现有 AI CRUD 生成、大屏生成、通用 AI 对话功能的前提下，优先修复第一阶段稳定性问题：会话串线、上下文丢失、同步调用会话缺失、动态 CRUD 读取链路顺序不合理等问题。

可验证结果：
- 不同 session 的 AI 对话不会复用同一记忆上下文
- 通用聊天接口可正确传递 projectName/canvasContext 与 userInput
- 同步/流式调用都能正确创建/更新会话
- 动态 CRUD 查询结果按“解密 -> 翻译 -> 脱敏”顺序处理

## 2. 代码现状（Research Findings）
> 每个结论必须有代码出处（文件路径 + 类名/方法名）

### 2.1 相关入口与链路
- AI 通用调用核心在 `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/client/AiClientImpl.java` 的 `call()` / `stream()`
- 大屏与通用聊天编排在 `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/chat/service/AiChatService.java`
- CRUD 动态运行时读取链路在 `forge/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`

### 2.2 现有实现
- `ChatClientCache#getOrCreate()` 仅以 `providerId:modelName` 作为缓存键，但构建时绑定了 `sessionId` 与 memory advisor，存在跨会话复用风险
- `AiChatService#chatStream()` 先设置了拼装后的 `userPrompt`，随后又被原始 `content` 覆盖
- `AiClientImpl#call()` 持久化消息前不会显式创建会话，`touchSession()` 对不存在会话仅执行 update
- `DynamicCrudService#selectPage()` / `selectById()` 当前顺序为脱敏 -> 翻译 -> 解密

### 2.3 发现与风险
- 若直接修改接口协议或前端调用路径，容易影响现有 AI 管理和 CRUD 生成器页面
- 当前仓库根 POM 默认跳过 surefire/testCompile，新增测试骨架后仍需通过显式开启测试或后续专项测试步骤验证

## 3. 功能点
- [x] 功能 1：修复 ChatClient 缓存与会话隔离问题（输入：同 provider/model 不同 session；处理：避免共享带 memory 的 client；输出：会话独立）
- [x] 功能 2：修复通用聊天 prompt/userInput 透传问题（输入：chatStream 请求；处理：保留拼装 prompt 与 userInput；输出：上下文不丢失）
- [x] 功能 3：统一同步/流式会话创建（输入：任意 AI 调用；处理：持久化前确保 session 存在；输出：会话列表可见）
- [x] 功能 4：调整动态 CRUD 读取顺序（输入：查询结果；处理：先解密，再翻译，再脱敏；输出：展示正确）
- [x] 功能 5：收敛动态 CRUD 写链路准备逻辑（输入：insert/update 请求；处理：提取统一 helper 并保留原语义；输出：后续扩展更安全）
- [x] 功能 6：收敛动态 CRUD 查询/删除公共条件拼装（输入：select/update/delete 请求；处理：统一租户与 where 拼装；输出：重复逻辑下降）
- [x] 功能 7：补最小回归测试骨架（输入：AI/动态 CRUD 关键稳定性逻辑；处理：新增单测骨架与依赖；输出：后续可逐步激活）

## 4. 业务规则
- 不修改现有 controller 路径、请求结构、响应结构
- 不删除旧字段，不改数据库 schema
- ChatClient 可继续缓存，但缓存对象不能绑定具体 session memory
- 同步调用若传入/生成 sessionId，必须确保 `ai_chat_session` 有对应记录

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 无 | - | - | 本阶段不做数据库变更 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 无 | - | - | 保持兼容 |

## 7. 影响范围
- `forge-plugin-ai`：AiClient / AiChatService / ChatClientCache / 会话服务 / AiInvocationResolver
- `forge-plugin-generator`：DynamicCrudService / DynamicCrudRepository
- `forge-plugin-ai` 与 `forge-plugin-generator` 的测试目录与 POM 测试依赖
- `code-copilot/changes/ai-codegen-stability-refactor/` 文档同步

## 8. 风险与关注点
- ⚠️ 变更涉及 AI 会话与缓存逻辑，需重点关注多轮对话回归
- ⚠️ 动态 CRUD 的展示顺序调整可能影响加密字段的页面表现，需验证典型加密/脱敏场景
- ⚠️ 仓库当前默认跳过测试执行，新增测试骨架不会自动参与现有构建链路

## 8.5 测试策略
- **测试范围**：AI 通用聊天、流式会话、CRUD 动态查询处理顺序、动态 CRUD 写链路、AI 调用解析
- **覆盖率目标**：本阶段以关键链路回归为主，先完成模块编译验证与测试骨架落位
- **独立 Test Spec**：否

## 9. 待澄清
- [x] 问题 1：是否允许在不改接口协议的情况下先完成第一阶段稳定性重构 —— 用户已明确要求“逐步执行，别影响现有功能”

## 10. 技术决策
- 缓存 `ChatClient` 时只缓存“无 session memory advisor”的基础 client；带 session 的 client 每次基于基础 client/基础 model 构建
- 同步调用沿用现有 `sessionId` 语义，但在持久化前补齐 `getOrCreate()`
- 动态 CRUD 的读链路统一为“解密 -> 翻译 -> 脱敏”
- 动态 CRUD 的写链路在 repository 内统一收口输入校验、不可更新字段过滤与审计字段填充
- 动态 CRUD 的查询/删除租户条件与 where 拼装统一收口，避免多处拼接分叉
- 测试先采用 Mockito 单测骨架，不引入额外运行时改造

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|-------------|------|
| T1 | ✅ 完成 | spec.md, tasks.md | 建立第一阶段稳定性重构文档 |
| T2 | ✅ 完成 | AiClientImpl.java, ChatClientCache.java, AiChatService.java, AiChatSessionService.java, DynamicCrudService.java | 完成第一阶段低风险稳定性修复 |
| T3 | ✅ 完成 | spec.md | 使用 JDK 17 重新执行模块编译，验证通过 |
| T4 | ✅ 完成 | AiInvocationResolver.java, AiClientImpl.java, spec.md | 抽离 AI 调用解析职责并再次编译通过 |
| T5 | ✅ 完成 | DynamicCrudRepository.java, spec.md | 补齐动态 CRUD 写链路审计字段自动填充并编译通过 |
| T6 | ✅ 完成 | DynamicCrudRepository.java, spec.md, tasks.md | 抽离动态 CRUD 写链路准备 helper，并再次编译通过 |
| T7 | ✅ 完成 | DynamicCrudRepository.java, spec.md, tasks.md | 收敛动态 CRUD 查询/删除公共 SQL 条件拼装，并编译通过 |
| T8 | ✅ 完成 | forge-plugin-generator/pom.xml, forge-plugin-ai/pom.xml, DynamicCrudRepositoryTest.java, AiInvocationResolverTest.java, spec.md, tasks.md | 新增最小回归测试骨架；当前仓库默认仍跳过测试执行 |

## 12. 审查结论
- 第一阶段稳定性改造已完成，未修改接口与数据库结构
- 第二步完成 `AiClientImpl` 解析职责抽离，核心行为保持不变
- 第三步补齐动态 CRUD 写链路的 `tenant_id/create_by/create_dept/update_by/create_time/update_time` 自动填充，且不覆盖显式业务输入
- 第四步将 `DynamicCrudRepository` 的写链路准备逻辑提取为统一 helper，保留原有更新字段过滤与审计填充语义
- 第五步将 `DynamicCrudRepository` 的租户条件与 where 拼装逻辑统一收敛，减少 select/update/delete 重复分支
- 第六步补入 `DynamicCrudRepositoryTest` 与 `AiInvocationResolverTest` 测试骨架，并为对应模块补充 `spring-boot-starter-test` 依赖
- 已使用 JDK 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator,forge-framework/forge-plugin-parent/forge-plugin-ai,forge-admin-server -am -DskipTests compile` 编译通过
- 尝试执行定向测试时，确认仓库根 POM 当前默认跳过 `testCompile/surefire`，因此本次先落地测试骨架，不调整现有构建默认行为

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-04-27
- **确认人**：用户（要求“帮我按照步骤逐步执行”）

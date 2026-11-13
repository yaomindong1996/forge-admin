# 四期：多模态 + AI 创建 Agent · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现多模态能力（图片生成、ASR、TTS）和 AI 创建 Agent（流式逐字段生成 + 智能推荐绑定）。

**Architecture:** 多模态复用一期模型类型（`image_generation`/`asr`/`tts`）与适配器工厂。图片生成作为独立页面 + Agent 工具，ASR/TTS 作为对话语音通道。AI 创建 Agent 用 LLM 流式逐字段生成基础配置 + 智能推荐知识库/技能/工具绑定。参考 snail-ai 的 AgentWizardDrawer + agent 创建服务。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 2.0.0（ImageModel/AudioModel）、Vue 3 + Naive UI

**Spec:** `docs/superpowers/specs/2026-08-04-ai-upgrade-master-design.md`

## 决策记录（2026-08-04 评审确认）

- **前置**：依赖 Phase 0（Spring AI 2.0.0）+ 一期 + 二期 + 三期（Agent CRUD/工具/对话 UI）。
- **语音模型源**：对话语音通道由 **Agent 显式绑定** ASR/TTS 模型（新增 `asr_model_id`/`tts_model_id` 字段，可选），多 Agent 各配各的语音模型。
- **推荐绑定**：`AgentBindRecommender` 的推荐**仅展示供用户勾选**（参照 snail-ai `AgentWizardDrawer`），**不自动绑定**。
- **AI 创建模型**：生成阶段用**系统默认 Chat 模型**（`AiProviderService.requireEnabledDefaultProvider` + 默认模型），非新建 Agent 的模型。
- **逻辑删除**：新表统一 `del_flag bigint NOT NULL DEFAULT 0` + 唯一键直接建在 `del_flag` 上（无生成列）。
- **Spring AI 1.1.8**：图片/语音适配器按 1.1.8 API（ImageModel/AudioTranscriptionModel/TextToSpeechModel 包名与构造以 1.1.8 为准，已探针验证）。

## Global Constraints

- 查询 SQL 必须写 Mapper XML，禁止 Service 层用 `LambdaQueryWrapper`
- 业务数据 `tenant_id` 必须为 `1`
- 分页参数：`pageNum`/`pageSize`
- 逻辑删除默认：`del_flag bigint NOT NULL DEFAULT 0`，数值主键表 `@TableLogic(value = "0", delval = "id")`，唯一键直接建在 `del_flag` 上（无生成列）
- API Key 脱敏；日志禁止打印敏感信息；ASR/TTS 涉及用户语音，日志禁止记录音频内容
- Flyway 迁移单调递增（> 1.0.84），SQL 幂等，`NOT EXISTS` 防重复
- 字典不硬编码；菜单 `sys_resource` 带 `NOT EXISTS`
- 禁止 Service 互相注入，跨 Service 协调上提 Controller
- 基础包：`com.mdframe.forge`

---

### Task 1: 数据库迁移（多模态记录表 + AI 创建 Agent 记录 + 菜单）

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.89__add_ai_multimodal_agent_create.sql`

**Interfaces:**
- Produces: 表 `ai_image_generate_record`、`ai_agent_generate_record`；字典 + 菜单

> 注：**不建 `ai_voice_session` 表**。语音输入链路为"用户发语音 → ASR 转文本 → 文本进现有会话消息表（`ai_chat_record`）"，转完后与普通文本消息一致，无需独立语音会话表；且 Global Constraints 要求不记录音频内容。

- [x] **Step 1: 图片生成记录表**

```sql
CREATE TABLE IF NOT EXISTS `ai_image_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `provider_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `model_id` bigint DEFAULT NULL COMMENT '模型ID',
  `prompt` longtext DEFAULT NULL COMMENT '提示词',
  `negative_prompt` longtext DEFAULT NULL COMMENT '负面提示词',
  `size` varchar(32) DEFAULT '1024x1024' COMMENT '尺寸',
  `result_file_id` bigint DEFAULT NULL COMMENT '生成图片文件ID',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(pending/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI图片生成记录';
```

- [x] **Step 2: AI 创建 Agent 记录表**

```sql
CREATE TABLE IF NOT EXISTS `ai_agent_generate_record` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `description` longtext NOT NULL COMMENT '用户需求描述',
  `generated_config_json` longtext DEFAULT NULL COMMENT '生成结果(名称/描述/问候语/预设问题/指令/推荐绑定)',
  `status` varchar(32) DEFAULT 'pending' COMMENT '状态(generating/success/failed)',
  `error_msg` varchar(1000) DEFAULT NULL COMMENT '错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI创建Agent生成记录';
```

- [x] **Step 3: 字典 + 菜单**

新增字典：`ai_image_generate_status`（pending/success/failed）。
新增菜单：`AI 工具` 分组下 `图片生成` / `语音设置`（ASR/TTS 配置）。菜单 SQL 按 `V1.0.18` 完整模板：`sys_resource` 含 `client_code`/`menu_status`/`visible`/`resource_type` + `sys_role_resource` 授权 admin，全部 `NOT EXISTS` 防重复，`tenant_id = 1`。

- [x] **Step 4: 提交**

```bash
git add forge-server/db/mysql/migration/V1.0.89__add_ai_multimodal_agent_create.sql
git commit -m "feat(db): 多模态与AI创建Agent表结构"
```

---

### Task 2: 图片生成能力

**Files:**
- Create: `.../multimodal/image/AiImageGenerationService.java`
- Create: `.../multimodal/image/adapter/AiImageModelAdapter.java`（接口）
- Create: `.../multimodal/image/adapter/OpenAiCompatibleImageModelAdapter.java`（@Component）
- Create: `.../multimodal/image/controller/AiImageGenerateController.java`
- Modify: `.../model/adapter/AiModelAdapterRegistry.java`（新增 `List<AiImageModelAdapter>` + `getImage(modelKey)`）
- Test: `.../multimodal/image/AiImageGenerationServiceTest.java`

**Interfaces:**
- Consumes: 一期 `AiModelAdapterRegistry`（扩展图片分支）、Task 1（`ai_image_generate_record`）
- Produces:
  - `AiImageModelAdapter` 接口：`boolean supports(String modelKey)` / `String generate(String baseUrl, String apiKey, String model, String prompt, String negativePrompt, String size)`（返回图片 URL 或 base64）
  - `OpenAiCompatibleImageModelAdapter`（@Component，用 Spring AI `OpenAiImageModel`）
  - `AiModelAdapterRegistry.getImage(String modelKey) -> AiImageModelAdapter`
  - `AiImageGenerationService.generate(record) -> recordId`（异步，结果存 `sys_file` 得 fileId）；`GET /ai/image-generate/page`、`POST /ai/image-generate`、`POST /ai/image-generate/:id`（取结果）

- [x] **Step 1: 实现图片模型适配器接口与实现**

`AiImageModelAdapter` 接口（分类型接口，同方案 C 风格）。`OpenAiCompatibleImageModelAdapter`（@Component）用 Spring AI `OpenAiImageModel` 构造（baseUrl + apiKey，`AiSecretCrypto.decrypt` 解密）。`supports` 按 modelKey 前缀匹配（`dall-e`/`gpt-image`/`stable-diffusion`/`qwen-image` 等）。

- [x] **Step 2: 扩展 AiModelAdapterRegistry**

在 `AiModelAdapterRegistry` 注入 `List<AiImageModelAdapter>`，新增 `getImage(String modelKey) -> AiImageModelAdapter`（按 supports 匹配，无匹配抛 BusinessException）。参照一期 Task 5 的 `getEmbedding`/`getRerank`。

- [x] **Step 3: 实现生成服务**

`AiImageGenerationService.generate`：插 `ai_image_generate_record`（pending）→ `@Async` 调 `registry.getImage(modelKey).generate(...)` → 结果图片存 `sys_file`（`forge-starter-file`）→ 更新记录（success + fileId）。失败写 `error_msg`。

- [x] **Step 4: 实现图片生成工具（Agent 工具）**

`ImageGenerateTool` 实现 `AgentTool`（三期 Task 3 接口）：`generate(prompt, size) -> ToolResult(type=image, content=fileId)`。注册到 `BuiltinToolSource`。Agent 对话中 LLM 可调用生成图片。

- [x] **Step 5: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [x] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/multimodal/
git commit -m "feat(ai): 图片生成能力与Agent工具"
```

---

### Task 3: ASR / TTS 能力

**Files:**
- Create: `.../multimodal/voice/AiAsrService.java`
- Create: `.../multimodal/voice/AiTtsService.java`
- Create: `.../multimodal/voice/adapter/AiAsrModelAdapter.java`（接口）
- Create: `.../multimodal/voice/adapter/AiTtsModelAdapter.java`（接口）
- Create: `.../multimodal/voice/adapter/OpenAiCompatibleAsrModelAdapter.java`（@Component）
- Create: `.../multimodal/voice/adapter/OpenAiCompatibleTtsModelAdapter.java`（@Component）
- Create: `.../multimodal/voice/controller/AiVoiceController.java`
- Modify: `.../model/adapter/AiModelAdapterRegistry.java`（新增 `List<AiAsrModelAdapter>`/`List<AiTtsModelAdapter>` + `getAsr(modelKey)`/`getTts(modelKey)`）
- Modify: `.../agent/domain/AiAgent.java`（新增 `asr_model_id`/`tts_model_id` 可选字段）
- Modify: `forge-server/db/mysql/migration/V1.0.89__add_ai_multimodal_agent_create.sql`（ai_agent 补 `asr_model_id`/`tts_model_id` 列，幂等）
- Test: `.../multimodal/voice/AiVoiceServiceTest.java`

**Interfaces:**
- Consumes: 一期 `AiModelAdapterRegistry`（扩展 ASR/TTS 分支）
- Produces:
  - `AiAsrModelAdapter` 接口：`boolean supports(String modelKey)` / `String transcribe(String baseUrl, String apiKey, String model, byte[] audio, String mimeType)`
  - `AiTtsModelAdapter` 接口：`boolean supports(String modelKey)` / `byte[] synthesize(String baseUrl, String apiKey, String model, String text)`
  - `AiAsrService.transcribe(MultipartFile audio, Long agentId) -> String`（语音→文本，agentId 解析 Agent 绑定的 `asr_model_id`）
  - `AiTtsService.synthesize(String text, Long agentId) -> fileId`（文本→语音，agentId 解析 Agent 绑定的 `tts_model_id`）
  - `POST /ai/voice/asr`、`POST /ai/voice/tts`
  - 对话语音通道：**Agent 显式绑定** ASR/TTS 模型；对话页语音输入（前端录音→ASR→发文本）、回复播报（TTS）

- [x] **Step 1: 实现 ASR/TTS 适配器接口与实现**

`AiAsrModelAdapter` / `AiTtsModelAdapter` 分类型接口（同方案 C 风格）。`OpenAiCompatibleAsrModelAdapter`（@Component）用 Spring AI `OpenAiAudioTranscriptionModel`；`OpenAiCompatibleTtsModelAdapter`（@Component）用 Spring AI `OpenAiAudioSpeechModel`。构造时 `AiSecretCrypto.decrypt` 解密 API Key。

- [x] **Step 2: 扩展 AiModelAdapterRegistry**

注入 `List<AiAsrModelAdapter>` / `List<AiTtsModelAdapter>`，新增 `getAsr(String modelKey)` / `getTts(String modelKey)`（按 supports 匹配，无匹配抛 BusinessException）。

- [x] **Step 3: 实现 ASR/TTS 服务**

`AiAsrService.transcribe`：音频流 → `registry.getAsr(modelKey).transcribe(...)` → 返回文本。
`AiTtsService.synthesize`：文本 → `registry.getTts(modelKey).synthesize(...)` → 音频存 `sys_file` → 返回 fileId。

- [x] **Step 4: 对话集成（Agent 显式绑定模型）**

对话语音通道的模型来自 **Agent 配置显式绑定**（`AiAgent` 新增 `asr_model_id`/`tts_model_id` 可选字段）：
- `AiAsrService.transcribe` 从 Agent 绑定的 `asr_model_id` 解析 modelKey；未绑定则返回错误提示
- `AiTtsService.synthesize` 从 Agent 绑定的 `tts_model_id` 解析 modelKey；未绑定则返回错误提示
- **语音输入**：前端录音（参照 internet-hospital `ChatVoiceRecordingOverlay`，30 秒上限）→ 上传 → `AiAsrService.transcribe` → 文本入对话框
- **回复播报**：Agent 回复文本 → `AiTtsService.synthesize` → 播放（流式 TTS 可选，先做整段）

- [x] **Step 5: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [x] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/multimodal/voice/
git commit -m "feat(ai): ASR/TTS能力与对话语音通道"
```

---

### Task 4: AI 创建 Agent（流式逐字段生成）

**Files:**
- Create: `.../agent/engine/create/AgentCreateService.java`
- Create: `.../agent/engine/create/AgentFieldGenerator.java`（逐字段流式）
- Create: `.../agent/engine/create/AgentBindRecommender.java`（智能推荐绑定）
- Create: `.../agent/engine/create/controller/AgentCreateController.java`
- Test: `.../agent/engine/create/AgentCreateServiceTest.java`

**Interfaces:**
- Consumes: 一期 Chat 模型、二期知识库（推荐）、三期技能/工具（推荐）、Task 1（`ai_agent_generate_record`）
- Produces:
  - `POST /ai/agent/ai-create`（SSE 流式）：`description` → 依次返回 `start` / `field_done:{name,value}`（name=agentName/description/greeting/presetQuestions/instruction/keeps） / `recommend`（知识库/技能/工具推荐） / `done`
  - `POST /ai/agent/ai-create/confirm`：接收用户编辑后的完整配置，创建 Agent

- [x] **Step 1: 实现逐字段生成**

`AgentFieldGenerator.streamGenerate(description)`：一个 LLM 调用，返回结构化 JSON（含全部字段），服务端拆成多个 `field_done` SSE 事件逐个推送（前端显示每字段状态标签，参照 snail-ai-admin `AgentWizardDrawer`）。若 LLM 支持流式则逐字段流式，否则一次生成后分批推送。

- [x] **Step 2: 实现智能推荐绑定**

`AgentBindRecommender.recommend(description, agentConfig)`：
1. 描述 → 提取关键词
2. 关键词语义匹配知识库（名称/描述向量相似度）
3. 匹配技能（名称/描述）
4. 匹配工具（能力/关键词）
5. 返回推荐列表（含置信度），SSE `recommend` 事件推送

> **仅展示，不自动绑定**：推荐列表供用户在确认步勾选（参照 snail-ai `AgentWizardDrawer`），**不自动绑定**。确认创建时才落库。

- [x] **Step 3: 实现确认创建**

`/ai/agent/ai-create/confirm`：接收用户编辑后的配置（基础字段 + 用户勾选的绑定），复用三期 Agent CRUD 保存逻辑创建 Agent。

- [x] **Step 4: 记录生成过程**

写 `ai_agent_generate_record`（Task 1），失败记 `error_msg`。生成阶段用**系统默认 Chat 模型**（`AiProviderService.requireEnabledDefaultProvider` + 默认模型），非新建 Agent 的模型。

- [x] **Step 5: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [x] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/engine/create/
git commit -m "feat(ai): AI创建Agent流式逐字段生成与智能推荐"
```

---

### Task 5: 多模态 + AI 创建前端

**Files:**
- Create: `forge-admin-ui/src/views/ai/image-generate/index.vue`（图片生成页面）
- Create: `forge-admin-ui/src/views/ai/voice/index.vue`（语音设置：ASR/TTS 模型配置）
- Create: `forge-admin-ui/src/views/ai/agent-create/index.vue`（AI 创建 Agent 向导）
- Modify: `forge-admin-ui/src/views/ai/agent.vue`（入口接入 AI 创建）
- Modify: `forge-admin-ui/src/api/ai.js`

**Interfaces:**
- Consumes: Task 2-4 接口
- Produces: 图片生成页、语音设置页、AI 创建 Agent 向导

- [x] **Step 1: 图片生成页**

提示词输入 + 负面提示词 + 尺寸选择 + 生成按钮 → 结果图展示（`AuthImage` 按 fileId）+ 历史记录列表。可"重新生成"。

- [x] **Step 2: 语音设置页**

ASR/TTS 模型选择（用 `useDict('ai_model_type')` 过滤 asr/tts 类型模型）+ 测试按钮（录音试 ASR、文本试 TTS）。

- [x] **Step 3: AI 创建 Agent 向导**

参照 snail-ai-admin `AgentWizardDrawer`：4 步（描述 → 生成中 → 确认 → 完成）。生成中显示每字段状态标签（pending/running/done/error）；确认步可编辑字段 + 勾选推荐绑定；完成后跳转 Agent 工作台。

- [x] **Step 4: 对话语音集成**

对话页（三期 Task 9）：输入框语音按钮 → 录音 → ASR → 文本入框；回复播报按钮 → TTS。

- [x] **Step 5: 本地验证**

Run: `cd forge-admin-ui && pnpm dev`
Expected: 图片生成可用、语音设置可配置、AI 创建 Agent 向导可用（流式生成 + 推荐绑定 + 确认创建）

- [x] **Step 6: 提交**

```bash
git add forge-admin-ui/src/views/ai/ forge-admin-ui/src/api/ai.js
git commit -m "feat(ui): 图片生成/语音设置/AI创建Agent向导"
```

---

## Self-Review 记录

- **Spec 覆盖**：四期覆盖设计文档全部（图片生成 ✓、ASR/TTS ✓、AI 创建 Agent ✓、Vision 已提前到三期）
- **占位符扫描**：无 TBD/TODO
- **类型一致性**：`AiImageModelAdapter.generate`、`AiAsrService.transcribe`、`AiTtsService.synthesize`、`AgentCreateController` 的 SSE 事件名（`start`/`field_done`/`recommend`/`done`）全篇一致
- **依赖顺序**：Task 2/3 依赖一期 `AiModelAdapterRegistry`（分类型接口注册表，扩展图片/语音分支）与 `AiSecretCrypto`；Task 4 依赖三期 Agent CRUD；Task 3 的对话集成依赖三期对话 UI
- **逻辑删除**：本期两张记录表统一 `del_flag bigint NOT NULL DEFAULT 0` + 唯一键直接建在 `del_flag` 上（无生成列）
- **语音模型源**：对话语音通道由 Agent 显式绑定 `asr_model_id`/`tts_model_id`（Task 3），多 Agent 各配各的
- **推荐绑定**：AI 创建 Agent 的推荐**仅展示供用户勾选**，不自动绑定（Task 4）
- **AI 创建模型**：生成阶段用系统默认 Chat 模型（Task 4）
- **菜单脚本**：按 `V1.0.18` 完整模板（`client_code`/`menu_status`/`sys_role_resource` 授权 admin）
- **说明**：三期 Task 9（对话 UI）已含 Vision 图片输入；本期的 ASR/TTS 对话语音通道在四期 Task 3/5 补充录音与播报

# 任务拆分 — 用户反馈平台问题修复
> status: complete
> created: 2026-07-15
> 拆分顺序：契约与纯函数 → 公共组件/底层实现 → 页面与配置编排 → SQL 修复 → 增量验证

## 前置条件

- [x] 已读取根 `AGENTS.md`、项目记忆、编码规范和自动化测试标准。
- [x] 已切换到 `fix/user-reported-issues-20260715`，不在主分支直接编码。
- [x] 已确认本变更不需要数据库结构迁移。
- [x] 已确认跳过 `ui-ux-pro-max`，页面改动只遵循现有 Forge 组件规范。

## Task 1: 修复流程已办消息审批动作

- [x] 已完成
- **目标**：只有仍未读/未完成的 `FLOW_TODO` 消息显示并触发“去审批”，已办消息保留查看能力但不进入待办。
- **涉及文件**：
  - `forge-admin-ui/src/layouts/components/message-notification-utils.js` — 新增可测试的消息状态判断。
  - `forge-admin-ui/src/layouts/components/__tests__/message-notification-utils.spec.js` — 覆盖待办、已办和普通消息。
  - `forge-admin-ui/src/layouts/components/MessageNotification.vue` — 按待办状态控制按钮和卡片点击。
- **关键签名**：
  ```js
  export function isFlowApprovalMessage(message) {}
  export function isPendingFlowApprovalMessage(message) {}
  ```
- **验收**：`FLOW_TODO/readFlag=1` 不渲染“去审批”；`FLOW_TODO/readFlag=0` 仍可跳转 `/flow/todo`。

## Task 2: 增强通用导入交互与模板

- [x] 已完成
- **目标**：提供本地有限预览、真实上传进度/服务端处理状态、结构化导入结果和带样例/说明的模板。
- **涉及文件**：
  - `forge-admin-ui/src/components/ai-form/import-utils.js` — 预览行与导入结果归一化纯函数。
  - `forge-admin-ui/src/components/ai-form/__tests__/import-utils.spec.js` — 覆盖预览截断、指标和错误字段兼容。
  - `forge-admin-ui/src/components/ai-form/AiCrudImportModal.vue` — 新增选择、预览、导入、结果四阶段弹窗。
  - `forge-admin-ui/src/components/ai-form/AiCrudPage.vue` — 接入新弹窗并把现有上传/模板下载封装为回调。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-excel/src/main/java/com/mdframe/forge/starter/excel/model/ImportTemplateColumn.java` — 公共模板列描述。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-excel/src/main/java/com/mdframe/forge/starter/excel/core/ExcelImportTemplateWriter.java` — 写入数据样例页和字段说明页。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-excel/src/main/java/com/mdframe/forge/starter/excel/service/impl/ExcelImportServiceImpl.java` — 通用配置映射到模板列。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudExcelService.java` — 动态 CRUD 模板接入公共写入器并推导样例/说明。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-excel/src/test/java/com/mdframe/forge/starter/excel/core/ExcelImportTemplateWriterTest.java` — 验证两个工作表、样例行和说明字段。
- **关键签名**：
  ```js
  export function buildImportPreview(rows, maxRows = 20) {}
  export function normalizeImportResult(payload) {}
  ```
  ```java
  public byte[] write(String dataSheetName, List<ImportTemplateColumn> columns);
  public void write(OutputStream outputStream, String dataSheetName, List<ImportTemplateColumn> columns);
  ```
- **验收**：首个工作表最多预览 20 行；结果弹窗显示四项指标和失败原因；模板包含“导入数据/填写说明”两个工作表及一行样例。

## Task 3: 打通加解密全局开关

- [x] 已完成
- **目标**：统一配置中心保存后后端即时生效，前端启动和当前页面同步使用相同开关，字段级加解密也服从全局关闭。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/keyexchange/CryptoRuntimeConfig.java` — 安全裁剪的运行配置 DTO。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/keyexchange/KeyExchangeController.java` — 新增匿名 `/crypto/config`。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/config/SaTokenConfig.java` — 放行运行配置接口。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/interceptor/ApiPermissionInterceptor.java` — 密码强制修改白名单补齐运行配置接口。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/service/ConfigManagerService.java` — 保存 crypto 分组后同步刷新运行属性。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/service/ConfigSyncService.java` — 把分组属性写入 `sys_config` 后刷新运行属性。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/java/com/mdframe/forge/starter/config/controller/ConfigManageController.java` — 校验保存/同步结果。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/advice/EncryptResponseBodyAdvice.java` — 固定运行配置和开关保存响应为明文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/advice/DecryptRequestBodyAdvice.java` — 固定开关保存请求为明文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/JacksonCryptoConfiguration.java` — 注入动态属性到字段序列化上下文。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/serializer/CryptoFieldSerializer.java` — 序列化时动态检查总开关。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/serializer/CryptoFieldDeserializer.java` — 反序列化时动态检查总开关。
  - `forge-admin-ui/src/utils/crypto/crypto-config.js` — 加载、归一化、应用服务端运行配置。
  - `forge-admin-ui/src/utils/crypto/key-exchange.js` — 全局关闭时跳过 API 密钥协商。
  - `forge-admin-ui/src/utils/crypto/crypto-interceptor.js`、`src/utils/http/interceptors.js` — 请求、响应和防重放处理服从运行开关。
  - `forge-admin-ui/src/utils/crypto/__tests__/crypto-config.spec.js` — 覆盖关闭、开启及安全默认值。
  - `forge-admin-ui/src/main.js` — 路由初始化前加载运行配置。
  - `forge-admin-ui/src/views/system/config-center.vue` — 保存后更新当前浏览器配置。
- **关键签名**：
  ```java
  @GetMapping("/config")
  public ResponseEntity<Map<String, Object>> getRuntimeConfig();
  ```
  ```js
  export function normalizeRuntimeCryptoConfig(config) {}
  export function applyRuntimeCryptoConfig(config) {}
  export async function loadRuntimeCryptoConfig() {}
  ```
- **验收**：关闭后普通浏览器请求/响应均为明文，前端不再执行动态密钥协商；重新开启后保持默认安全链路。

## Task 4: 修复模型列表操作列裁剪

- [x] 已完成
- **目标**：确保模型管理操作列容纳三个操作按钮。
- **涉及文件**：
  - `forge-admin-ui/src/views/ai/provider-model.vue` — 调整右固定操作列宽度、表格横向宽度和操作间距。
- **关键配置**：
  ```js
  { title: '操作', key: 'actions', width: 160, fixed: 'right' }
  ```
- **验收**：宽屏和横向滚动场景均能看到“测试、编辑、删除”。

## Task 5: 修复用户关联管理角色摘要 SQL

- [x] 已完成
- **目标**：兼容 MySQL 8 `ONLY_FULL_GROUP_BY` 并保持稳定去重排序。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml` — 用显式 `GROUP BY` 替代非法 `DISTINCT + ORDER BY`。
- **关键 SQL**：
  ```sql
  SELECT r.role_name
  FROM sys_user_org_role uor
  INNER JOIN sys_role r ON ...
  WHERE ...
  GROUP BY r.id, r.role_name, r.sort
  ORDER BY r.sort ASC, r.role_name ASC
  ```
- **验收**：Mapper XML 可解析，查询不再包含 `SELECT DISTINCT r.role_name`，排序语义不变。

## Task 6: 增量验证与文档回填

- [x] 已完成
- **目标**：按本轮差异执行最小验证矩阵并记录证据。
- **涉及文件**：
  - `code-copilot/changes/user-reported-platform-issues/test-spec.md` — 更新实际结果。
  - `code-copilot/changes/user-reported-platform-issues/execution-log.md` — 追加命令、结果、警告和跳过项。
  - `code-copilot/changes/user-reported-platform-issues/spec.md` — 回填任务状态和审查结论。
  - `code-copilot/changes/user-reported-platform-issues/tasks.md` — 勾选已完成任务。
- **命令**：
  ```bash
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm exec vitest run src/components/ai-form/__tests__/import-utils.spec.js src/layouts/components/__tests__/message-notification-utils.spec.js src/utils/crypto/__tests__/crypto-config.spec.js
  NODE_OPTIONS=--max-old-space-size=8192 pnpm build
  ```
  ```bash
  JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-excel \
    -Dtest=ExcelImportTemplateWriterTest test
  ```
  ```bash
  JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -pl forge-admin-server -am package -DskipTests
  xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysUserOrgRoleMapper.xml
  git diff --check
  ```
- **验收**：实际执行结果与未执行项均写入 `execution-log.md`，不把警告或跳过项表述为通过。

## Task 7: 修复关闭加解密后的启动失败

- [x] 已完成
- **目标**：全局关闭时不因数据库残留的无效 SM4/AES/RSA 密钥阻断 Bean 初始化；运行时重新开启后读取最新密钥。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/impl/SM4Encryptor.java`
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/crypto/impl/AESEncryptor.java`
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/config/CryptoAutoConfiguration.java`
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/test/java/com/mdframe/forge/starter/crypto/crypto/impl/CryptoEncryptorLifecycleTest.java`
- **验收**：关闭状态构造加密器不解析无效历史密钥；关闭状态忽略无效 RSA 配置；重新开启并设置有效密钥后 SM4/AES 往返成功。

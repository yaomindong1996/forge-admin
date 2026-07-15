# 用户反馈平台问题修复
> status: complete
> created: 2026-07-15
> complexity: 🔴复杂

## 1. 背景与目标

本变更处理用户集中反馈的五项平台问题：流程已办消息动作不准确、通用导入缺少预览/进度/结果反馈、加解密总开关未贯通前后端、AI 模型列表操作列裁剪、用户关联管理查询在 MySQL `ONLY_FULL_GROUP_BY` 下报错。

完成后应达到以下可验证结果：

- 流程完成后自动置为已读的待办消息不再显示或触发“去审批”；未完成待办仍保留审批入口。
- 通用导入在真正提交前最多预览 20 行，提交时展示上传/服务端处理状态，完成后展示总数、成功数、失败数和失败原因。
- 通用 Excel 与动态 CRUD 导入模板包含样例行和独立的字段填写说明。
- 配置中心关闭全局加解密后，后端 API 包装加解密、字段级加解密和前端请求加密均立即停止；重新开启后可恢复。
- `/ai/provider-model` 模型表格在宽屏和需要横向滚动的场景下都能看到“测试、编辑、删除”三个操作。
- 用户关联管理的角色名称查询兼容 MySQL 8 `ONLY_FULL_GROUP_BY`。

## 2. 代码现状（Research Findings）

### 2.1 流程消息入口

- `forge-admin-ui/src/layouts/components/MessageNotification.vue#isApprovalMessage` 只按 `bizType === 'FLOW_TODO'` 判断，并对所有此类消息展示“去审批”。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java` 在任务完成后调用 `messageService.markWebReadByBiz("FLOW_TODO", taskId)`，因此 `FLOW_TODO + readFlag=1` 是现有链路中已完成消息的可用标识。

### 2.2 通用导入

- `forge-admin-ui/src/components/ai-form/AiCrudPage.vue#handleImportRequest` 选中文件后立即上传；成功后直接关闭弹窗，只用消息提示摘要。
- `ImportResult` 和 `DynamicCrudImportResult` 已提供 `totalRows/successRows/failedRows/errors/summary`，无需新增数据库表即可展示核心指标。
- `ExcelImportServiceImpl#downloadTemplate` 可按配置写样例行，但模板没有字段说明页。
- `DynamicCrudExcelService#downloadImportTemplate` 当前只写表头和空数据，未提供样例值或字段说明。
- 前端已有 `xlsx` 依赖，可在浏览器本地读取工作表并限制预览行数，不需要先把原始文件上传到服务器。

### 2.3 加解密开关

- `EncryptResponseBodyAdvice`、`DecryptRequestBodyAdvice` 已检查 `CryptoProperties.enabled` 与 `enableApiCrypto`。
- `ConfigManagerService#saveCryptoConfig` 只更新 `sys_config_group`，没有同步 `sys_config` 并刷新 `@RefreshScope` 运行时属性。
- `forge-admin-ui/src/utils/crypto/crypto-config.js` 静态设置 `enabled: true`，应用启动时没有读取后端配置。
- `JacksonCryptoConfiguration` 只在启动阶段按属性注册字段加解密，已注册的 `CryptoFieldSerializer/CryptoFieldDeserializer` 没有在每次序列化时检查动态总开关。

### 2.4 模型列表操作列

- `forge-admin-ui/src/views/ai/provider-model.vue#modelColumns` 在宽度 `100px` 的右固定列内渲染三个文本按钮，实际内容宽度超过列宽而被固定列表层裁剪。

### 2.5 用户关联管理 SQL

- `SysUserOrgRoleMapper.xml#selectRoleNamesByUserOrg` 使用 `SELECT DISTINCT r.role_name`，但按未出现在选择列表里的 `r.sort` 排序，MySQL 8 严格模式会拒绝该查询。

## 3. 功能点

- [x] 已完成流程消息不再显示或触发审批动作。
- [x] 导入文件选择后本地预览首 20 行并显示总行数。
- [x] 导入时展示文件上传百分比和服务端处理阶段。
- [x] 导入完成后保留弹窗并展示总数、成功数、失败数、摘要和失败原因列表。
- [x] 通用及动态 CRUD 导入模板包含样例行与字段填写说明页。
- [x] 新增匿名、明文、安全裁剪的前端加解密运行配置接口。
- [x] 配置中心保存加解密配置后立即同步后端运行时，并同步当前浏览器内存配置。
- [x] 全局关闭时字段级序列化/反序列化同样停止。
- [x] 模型操作列完整展示删除按钮。
- [x] 用户组织角色名称查询兼容 `ONLY_FULL_GROUP_BY`。

## 4. 业务规则

- `FLOW_TODO` 且 `readFlag=0` 才视为当前可办理的审批消息；已读流程消息仍可在消息列表中查看，但不进入待办审批页。
- 本地导入预览只读取第一个工作表，最多渲染 20 行；空行不计入预览数据。
- 上传百分比只表示文件传输进度；上传完成等待接口返回时明确显示“服务端校验并导入中”，不伪造逐行处理百分比。
- 导入结果中的失败明细最多在弹窗中展示前 100 条，避免大结果集阻塞页面；汇总数使用后端权威结果。
- 模板样例行仅用于说明，填写说明页明确提示导入前删除或替换样例行。
- 前端运行配置接口不得返回 `secretKey`、RSA 私钥或其它密钥材料。
- 后端总开关 `enabled=false` 或 API 开关 `enableApiCrypto=false` 任一成立，前端 API 加密均视为关闭。
- 加解密运行配置接口和加解密配置保存响应必须保持明文，保证开关切换过程中不会出现“旧状态无法解析新响应”的死锁。
- 字段级开关受全局 `enabled` 和 `enableFieldCrypto` 双重约束。
- SQL 查询继续显式限定租户、用户、组织和逻辑删除，不改变权限边界。

## 5. 数据变更

本变更不新增表、字段或内置数据，不修改已执行 Flyway 脚本。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/crypto/config` | GET | 匿名返回安全裁剪后的前端加解密运行配置，响应固定明文 |
| 调整 | `/api/config/manage/crypto` | PUT | 保存成功后立即同步运行时配置；响应固定明文 |
| 调整 | 既有导入接口 | POST | 协议不变，前端完整展示既有导入结果指标 |
| 调整 | 既有导入模板接口 | GET | 工作簿增加样例行与“填写说明”工作表 |

## 7. 影响范围

- 前端全局 HTTP 加解密初始化与配置中心。
- `AiCrudPage` 的所有通用导入页面。
- 动态 CRUD 和通用 Excel 导入模板。
- 顶部消息通知抽屉。
- AI 供应商/模型管理页面。
- 系统用户组织/角色关联管理。

## 8. 风险与关注点

- 加解密属于安全边界变更：默认配置获取失败时前端必须保持 `enabled=true` 的安全默认值，不能因网络异常自动降级明文。
- 从关闭切换为开启时，配置保存响应必须保持明文；下一次需加密请求再执行正常密钥协商。
- 不把 `X-Inner-Call` 暴露给浏览器，不改变既有内部调用信任边界。
- 导入仍使用现有同步服务端处理，不宣称提供逐行业务处理进度；大数据异步导入需另立变更。
- 工作区已有大量未提交改动，本变更只修改列出的相关文件，不重写或回滚用户现有差异。

## 8.5 测试策略

- **测试范围**：前端导入预览/结果归一化工具、流程消息动作判断、加解密运行配置归一化；Excel 模板工作簿结构；相关 Maven 模块编译；前端生产构建；Mapper XML 语法和 SQL 静态契约。
- **覆盖率目标**：本次新增的纯函数分支和模板生成核心路径覆盖；安全开关的启用/关闭/配置失败默认值均有静态或单测证据。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

无。用户已明确要求继续实施，并明确禁止使用 `ui-ux-pro-max`。

## 10. 技术决策

- 导入预览采用浏览器本地解析，避免为预览上传整份文件；正式导入继续复用现有接口和权限校验。
- 新增聚焦的 `AiCrudImportModal`，避免继续扩大已有 5000 行以上的 `AiCrudPage.vue`。
- 模板说明采用第二工作表，不改变第一工作表表头，确保现有导入解析兼容。
- 后端运行配置继续以 `CryptoProperties` 为单一运行时事实源；配置中心保存后通过现有 `ConfigSyncService` 同步并刷新。
- 前端启动先读取安全裁剪的 `/crypto/config`，失败时保留静态加密开启默认值。
- 角色名称去重使用 `GROUP BY r.id, r.role_name, r.sort`，保证排序字段确定且兼容同名不同角色。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | complete | `MessageNotification.vue`、消息判断工具及测试 | 已办消息不再提供审批动作 |
| Task 2 | complete | `AiCrudImportModal.vue`、导入工具、Excel 模板写入器及接入服务 | 支持有限预览、真实上传进度、结果指标、样例和填写说明 |
| Task 3 | complete | Config/Crypto/Auth Starter、前端运行配置与配置中心 | 保存后写入 `sys_config` 并刷新；前后端与字段级开关贯通 |
| Task 4 | complete | `provider-model.vue` | 操作列 `160px`、横向宽度 `1030px` |
| Task 5 | complete | `SysUserOrgRoleMapper.xml` | 严格模式兼容 SQL 已落地 |
| Task 6 | complete | `test-spec.md`、`execution-log.md` | 单测、Lint、XML、前后端构建和空白检查已完成 |

## 12. 审查结论

实现与静态审查通过，新增前端测试 11/11、Excel 模板测试 1/1 通过，Admin 42 模块打包和前端生产构建通过。未启动真实服务或数据库；匿名运行配置、开关切换后的明文/密文以及用户关联管理接口需在部署环境做运行态冒烟。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-15
- **确认人**：用户
- **确认内容**：用户提出五项问题并回复“继续”，同时要求不要使用 `ui-ux-pro-max`；视为授权按本 Spec 直接实施。

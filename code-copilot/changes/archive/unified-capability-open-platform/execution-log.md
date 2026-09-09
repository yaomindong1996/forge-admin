# 执行日志 — 统一能力开放平台

## 1. 本轮基线

- 日期：2026-08-01。
- 工作区包含本变更及其它用户未提交差异；本轮未重置、未清理、未提交无关文件。
- 已复用 `spec.md`、`tasks.md`、`test-spec.md` 和自动化测试规范，只对能力注册、客户端、授权及其安全链路做增量验证。
- 未主动启动 Admin、MySQL、Redis 或 Flow 服务；前端开发服务已在 `http://127.0.0.1:3000/` 运行。

## 2. 问题与处理

| 问题 | 根因 | 处理 |
|------|------|------|
| 前端不知道如何注册能力 | 目录页原先只提供查询和停用，受控发布 API 没有可视化入口 | 新增业务动作/流程动作注册弹窗，复用受控发布器生成 Schema 与策略 |
| 授权管理下拉为空 | 使用分页接口拉 500 条，但后端上限 100，且客户端/能力查询需要额外权限 | 新增同权限 `/ai/capability/grant/options`，返回安全候选项与白名单摘要 |
| 授权策略无法正确填写 | 页面暴露任意 JSON，用户无法知道 `allowedFields` / `allowedOperations` 结构 | 改为字段与流程操作多选，按能力版本策略快照初始化 |
| 动作/流程能力授权后仍被拒绝 | `CapabilityGrantPolicy` 把所有非 READ_ONLY 能力统一拒绝 | 仅保留 HIGH 风险拒绝，ACTION/FLOW 继续执行既有租户、组织、状态、版本和策略校验 |
| 客户端需手填用户/组织 ID | 表单没有接入系统用户与组织绑定数据 | 改为 `UserSelectPicker` + 用户组织绑定下拉 |
| 下拉已选中仍无法通过校验 | 雪花 `Long` 超出 JS 安全整数时会序列化为字符串，表单却限定 `type: number` | 能力对象、客户端、服务账号和组织 ID 均改为无精度损失的正整数 ID 校验 |
| 新建 OAUTH 客户端无法兑换 Token | 创建服务固定写 `oauth_enabled=0`，身份模块会按该字段拒绝客户端 | 按 `authModes` 同步写入；OAUTH=1，纯 SIGNATURE=0，并补分支测试 |
| 签名 AppId 不可见且会随 OAuth 密钥轮换变化 | 签名认证错误地用内部 `keyId` 查客户端 | 改用稳定的数值 `clientId`；前端列表和一次性凭据弹窗明确显示“客户端 ID / AppId” |
| 流程注册可选到无主流程对象 | 目录只校验对象已发布，没有校验启用主流程绑定 | 新增同发布权限的注册来源校验接口；START 额外要求平台托管运行对象 |
| 流程能力注册来源接口返回 404 | 管理 Controller 与运行时执行 Bean 共用 `flow-actions.enabled`，开关关闭时 Spring 没有注册路由 | 拆分管理控制面与运行时装配；发布/来源校验始终可用，真实执行仍受开关失败关闭 |
| 字典下拉无选项 | V1.0.74/V1.0.75 尚未执行或 Flyway locations 被覆盖 | 新增缺失错误态并阻止无效提交；迁移脚本补齐认证模式、主体类型和流程动作字典 |
| USER_DELEGATION 首次手机号映射可能命中重复用户 | 原登录查询 `LIMIT 1`，数据库未保证手机号唯一 | 新增外部身份专用唯一匹配查询，最多读取两条有效候选；无匹配或重复匹配均返回 `invalid_grant` |

## 3. 验证记录

| 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|-----------|
| 前端定向 Lint | Node 20.19.0 下 `pnpm exec eslint src/api/ai/capability.js src/views/ai/capability/*.vue src/views/ai/capability/components/*.vue` | passed，零错误 | 无 |
| 流程来源与发布 | Flow Actions 模块执行 `FlowActionSourceServiceTest,FlowActionCapabilityPublisherTest` | passed，5/5 | Reactor 首次尝试被既有 `forge-starter-datascope` Surefire/JUnit 配置阻断，改为目标模块直跑后通过 |
| 控制面 | Control Plane 模块执行 `CapabilityGrantPolicyTest,CapabilityCatalogServiceTest,CapabilityClientServiceTest,CapabilityGrantServiceTest` | passed，23/23 | 无 |
| 防重放 | OpenAPI Security 模块执行 `OpenApiReplayGuardTest` | passed，10/10 | 测试覆盖 Redis 异常失败关闭，日志中的 ERROR 为预期分支 |
| 网关认证与编排 | Open Gateway 模块执行 `OpenGatewayAuthenticatorTest,CapabilityInvokeOrchestratorTest` | passed，12/12 | 测试覆盖稳定数值 AppId、非法 AppId、503 失败映射；WARN 为预期分支 |
| Admin 聚合打包 | Java 17 下 `mvn -Penable-tests -pl forge-admin-server -am package -Dforge.tests.skip=true` | passed，47/47 Reactor 模块，`BUILD SUCCESS` | 测试通过上述目标命令独立执行；存在既有 deprecated/unchecked 编译警告 |
| 前端生产构建 | Node 20.19.0 下 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed，8818 modules，2m11s | 既有组件命名冲突、CSS `//` 注释、动态/静态导入 chunk 警告 |
| 流程动作装配分支 | Flow Actions 模块 `-Penable-tests -Dtest=FlowActionAutoConfigurationTest,FlowActionSourceServiceTest,FlowActionCapabilityPublisherTest test` | passed，8/8 | 默认 JDK 首次编译报目标版本 17 无效；切换 Java 17 后重跑 |
| 受控业务动作装配分支 | Secure Actions 模块 `-Penable-tests -Dtest=SecureActionAutoConfigurationTest,BusinessActionCapabilityPublisherTest test` | passed，9/9 | 不带 `enable-tests` 的中间尝试只编译主码且跳过测试，未计为通过证据 |
| Admin 聚合编译 | Java 17 下 `mvn -pl forge-admin-server -am -DskipTests compile` | passed，47/47 Reactor 模块，`BUILD SUCCESS` | 存在既有 deprecated/unchecked 编译警告 |
| 404 前端提示 | Node 20.19.0 下定向 ESLint | passed，零错误 | 未重启用户已启动 Admin，故当前进程尚不包含新映射 |
| System 用户目录 | Java 17 下目标模块 `mvn -Penable-tests test` | passed，46/46 | 短信通道异常日志为既有预期测试分支 |
| Identity 外部委托 | Java 17 下目标模块 `mvn -Penable-tests test` | passed，48/48 | 仓储/JWK 不可用日志为预期失败关闭分支 |
| Control Plane | Java 17 下目标模块 `mvn -Penable-tests test` | passed，36/36 | 含 USER_DELEGATION 客户端与 OpenAPI 文档契约 |
| Flow Actions | Java 17 下目标模块 `mvn -Penable-tests test` | passed，26/26 | 含 USER 身份 serviceUserId 可空与 Mapper 装配回归 |
| Open Gateway | Java 17 下目标模块 `mvn -Penable-tests test` | passed，13/13 | INTERNAL_ERROR WARN 为预期异常映射分支 |
| 最终 Admin 聚合编译 | Java 17 下 `mvn -pl forge-admin-server -am -DskipTests compile` | passed，47/47 Reactor 模块 | 仅既有 deprecated/unchecked 编译警告 |
| 最终前端生产构建 | Node 20.19.0 下 `pnpm build` | passed，8818 modules，1m33s | 既有组件命名冲突、CSS 注释与 chunk 提示 |
| 最终前端定向 Lint | Node 20.19.0 下 capability API/页面/组件 ESLint | passed，零错误 | 无 |
| V1.0.76 与 Mapper 静态检查 | 占位符/tenant_id/逻辑删除规则扫描 + `xmllint` | passed | 未连接真实数据库执行迁移 |

## 4. 未执行项

- 未连接真实 MySQL/Redis，未实际执行 V1.0.74/V1.0.75/V1.0.76 或查询 `forge_schema_history`。
- 未执行真实 OIDC Token Exchange、签名验签、幂等重试、nonce 重放和 USER 委托流程审批 curl E2E；按约定由用户执行。
- 当前 Admin 仍是本轮修复前启动的进程，未经用户重启，因此不把当前 404 记为修复后接口验证；浏览器插件连接还受到本地运行时 sandbox metadata 缺失阻断。

## 5. Spec-Code 偏差

| 偏差 | 影响 | 结论 |
|------|------|------|
| Spec 业务规则 1 写明网关能力类型不设限；当前 `OpenGatewayCapabilityResolver` 与执行适配器只支持 `BUSINESS_ACTION/ACTION`、`FLOW_ACTION/FLOW` | 本次可视化注册的两类能力均可执行；通过通用 `/publish` 注册的其它来源/行为即使授权，网关仍会返回 `CONFLICT` | 本轮不伪造通用执行语义；列为后续架构任务，需要为其它来源提供受控执行适配器或接入可验证的 `CapabilityRegistry` |

## 6. 服务状态

- Admin：用户已启动，PID 11041，监听 `8580`；本轮未重启，需重启后加载新 Controller 映射。
- Flow：用户已启动，PID 13100，监听 `8081`。MySQL/Redis 未由本轮启动或停止。
- 前端 Vite：运行中，`http://127.0.0.1:3000/`。

## 7. Capability Pepper 自动引导增量验证

- 日期：2026-08-01。
- 变更范围：Starter Crypto 启动前环境处理器、Capability 配置说明及旧 `crypto.properties` 升级。
- 根因：Capability 三个 Pepper 只有空占位符与启动校验，开发环境未导出变量时身份模块直接失败；临时随机值又会破坏跨重启凭据稳定性。

| 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|-----------|
| 失败测试基线 | Starter Crypto `-Penable-tests -Dtest=CryptoSecretEnvironmentPostProcessorTest test` | 按预期在新增 Pepper 常量/行为未实现时编译失败 | 实现后复跑通过 |
| Starter Crypto 全量 | Java 17 下目标模块 `mvn -Penable-tests test` | passed，43/43 | AES 异常日志为既有预期测试分支 |
| Capability Identity 全量 | Java 17 下目标模块 `mvn -Penable-tests test` | passed，48/48 | 数据库/JWK 不可用日志为既有失败关闭测试分支 |
| Admin 聚合编译 | Java 17 下 `mvn -pl forge-admin-server -am -DskipTests compile` | passed，47/47 Reactor 模块 | 仅既有 deprecated/unchecked 编译警告 |
| 静态差异 | `git diff --check` | passed，无空白错误 | 工作区其它未提交差异保持不动 |

验证覆盖首次生成、三个 32 字节随机值互异、重启复用、仅显式配置 Crypto 传输密钥时补齐、逐项显式覆盖、空占位符忽略、Bootstrap 关闭、旧文件原子升级、并发首次启动和 POSIX 权限。

## 8. 本轮服务状态

- Admin：用户进程 PID 56575，监听 `8580`；本轮未重启或停止。需要用户重启后才会执行旧密钥文件升级并加载新 Pepper。
- Flow：用户进程 PID 63165，监听 `8081`；本轮未重启或停止。
- 前端 Vite：已有进程监听 `3000`；本轮未启动或停止。
- 未连接或修改 MySQL、Redis、Flowable 运行态；未执行真实 OIDC/REST 网关 E2E。

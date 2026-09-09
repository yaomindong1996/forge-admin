# Capability 插件模块收敛
> status: implemented_pending_user_verification
> created: 2026-08-04
> complexity: 🔴复杂

## 1. 背景与目标

能力开放平台当前拆分为 7 个同级 Maven 模块，模块间形成较长依赖链，控制面、身份、开放网关和动作适配器之间的边界难以从聚合工程中识别，维护和装配成本偏高。

本变更将 Capability 建立为独立父级聚合模块，并把现有 7 个模块收敛为 4 个子模块：

```text
forge-plugin-capability-parent
├── forge-plugin-capability-core
├── forge-plugin-capability-platform
├── forge-plugin-capability-actions
└── forge-plugin-capability-high-risk-approval
```

## 2. 模块职责

| 模块 | 职责 | 合并来源 |
|---|---|---|
| `core` | 协议无关模型、Schema、Registry、公共 SPI 与执行契约 | `capability` |
| `platform` | 能力目录、客户端、授权、审计、身份令牌、外围用户映射、REST 开放网关 | `control-plane + identity + open-gateway` |
| `actions` | 低代码业务动作、系统服务动作、流程动作的发布与执行适配 | `secure-actions + flow-actions` |
| `high-risk-approval` | 高风险能力人工审批扩展 | 原模块保留 |

父模块仅负责 Maven 聚合，不承载 Java 业务代码。

## 3. 依赖方向

依赖方向固定为：

```text
core ← platform ← actions ← high-risk-approval
```

其中 `platform` 只依赖 `core` 中的执行扩展契约，通过 Spring 注入 `List<GovernedOpenGatewayAdapter>` 发现 `actions` 提供的适配器，不得反向编译依赖 `actions`。

为解除原 `open-gateway → secure-actions → identity/control-plane` 环，以下公共契约迁入 `core` 的 `com.mdframe.forge.plugin.capability.execution` 包：

- `SecureActionDescriptor`
- `GovernedCapabilitySnapshot`
- `GovernedCapabilityExecutionAdapter`
- `GovernedOpenGatewayAdapter`
- `SecureActionUnavailableException`

业务动作开放网关适配器迁入 `actions`，并由 `SecureActionAutoConfiguration` 注册。

## 4. 兼容性与安全边界

- 保持现有 Java 业务包、REST 路径、配置前缀、数据表和 Flyway 脚本不变。
- 仅公共执行契约调整 Java 包名；全仓调用方同步修改。
- 不改变 OAuth/HMAC、租户、用户委托、权限、限流、幂等和审计语义。
- 高风险审批继续单独成模块并由原开关控制，避免默认扩大高风险能力暴露面。
- Admin 显式装配 `platform / actions / high-risk-approval`，`core` 通过依赖传递引入。

## 5. 非目标

- 不重写 Capability 业务逻辑、接口协议或数据库结构。
- 不合并 Java package 为单一大包。
- 不启动 Admin/Flow 服务，不执行真实数据库、Redis、OAuth 或流程联调。
- 不处理本变更之外的前端、登录加密、数据集和顶部菜单修改。

## 6. 验收标准

- Reactor 只聚合一个 Capability 父模块，父模块下恰好有 4 个子模块。
- 全仓不再引用 6 个被移除的旧 artifactId，也不残留旧模块目录。
- `platform` 的 POM 不依赖 `actions`、generator 或 flow-client。
- `actions` 统一包含业务动作与流程动作源码、资源和测试。
- Spring 自动配置导入文件分别按新模块合并，既有配置类仍可被发现。
- Maven POM 可解析、无循环依赖，代码无旧公共契约包引用。

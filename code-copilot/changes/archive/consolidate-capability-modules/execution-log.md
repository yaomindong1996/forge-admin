# 执行记录 — Capability 插件模块收敛

## 2026-08-04

- 已确认工作区存在大量 Capability 产品化未提交修改，本变更采用目录迁移和定向补丁保留这些修改，不执行 reset/checkout/clean。
- 已确认目标结构为独立 Capability 父模块 + 四个子模块。
- 已确认原依赖链存在 `open-gateway → secure-actions → identity → control-plane → capability`，合并前需先下沉公共执行契约并把业务适配器移入 actions，避免新模块循环依赖。
- 已完成目录收敛：`core=32`、`platform=122`、`actions=44`、`high-risk-approval=22` 个主源码类，合计仍为 220，原源码规模保持一致。
- 已将公共执行描述符、快照、SPI 和不可用异常迁入 `com.mdframe.forge.plugin.capability.execution`；开放网关不再直接引用 generator 或 actions 实现。
- 已将业务动作开放网关适配器移入 actions，并改由 `SecureActionAutoConfiguration` 注册；Open Gateway 只注入通用适配器集合。
- 已更新 plugin parent、Capability parent、四个子模块、BOM、Admin 和 MCP POM；新父 POM 的子模块计数为 4。
- 已合并自动配置导入：core 1 项、platform 3 项、actions 2 项、high-risk 1 项，均无重复。

### 静态验证证据

- `xmllint --noout` 解析 plugin parent、Capability parent、四个子模块、BOM、Admin 和 MCP 共 9 个 POM：通过。
- 扫描旧 `capability/control-plane/identity/secure-actions/flow-actions/open-gateway` artifactId 与 module：无有效 POM 引用。
- 扫描旧 `secureaction.catalog/spi/exception` 公共契约 import：无残留。
- 扫描 Java package 与目录映射、Capability 内部 import 解析和重复 FQCN：仅识别到一个合法内部嵌套类型引用，无缺失或重复类。
- 检查 platform POM 对 `actions/generator/flow-client` 的反向依赖：无输出。
- `git diff --check`（本轮已跟踪文件）和新 Capability/SDD 目录尾随空白扫描：无输出。

### 跳过项

- 按用户此前明确“由用户自行测试”的偏好，未执行 Maven compile/test/package，未启动 Admin/Flow，未连接 MySQL/Redis，也未执行 OAuth、开放网关或流程 E2E。
- 目录迁移后已移出四个新模块下继承的旧 `target` 和 `.flattened-pom.xml` 生成物，避免旧自动配置清单干扰；用户编译时会自动重新生成。
- 本轮未启动任何服务，无需清理进程。

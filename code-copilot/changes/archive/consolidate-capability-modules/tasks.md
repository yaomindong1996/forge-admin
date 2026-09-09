# 任务清单 — Capability 插件模块收敛

## Task 1: 变更基线

- [x] 读取仓库规则、长期记忆和现有 Capability 改动。
- [x] 固化四模块职责、依赖方向和兼容边界。

## Task 2: 公共执行契约下沉

- [x] 将通用执行描述符、快照、SPI 和不可用异常迁入 `core`。
- [x] 更新全仓 Java import 和测试引用。
- [x] 将业务动作开放网关适配器迁入 `actions` 并调整 Bean 装配。
- [x] 移除 `platform` 对动作模块和 generator 的编译依赖。

## Task 3: Maven 模块合并

- [x] 新增 `forge-plugin-capability-parent` 聚合父模块。
- [x] 将原内核迁为 `forge-plugin-capability-core`。
- [x] 合并控制面、身份、开放网关为 `forge-plugin-capability-platform`。
- [x] 合并安全动作、流程动作为 `forge-plugin-capability-actions`。
- [x] 将高风险审批迁入父模块并保持独立。

## Task 4: 聚合装配与依赖管理

- [x] 更新 plugin parent、BOM、Admin 和 MCP POM。
- [x] 合并各模块 `AutoConfiguration.imports`。
- [x] 清理旧 artifactId、旧目录和旧公共契约引用。

## Task 5: 静态验证与交付

- [x] 按用户偏好只执行非运行态静态检查。
- [x] 记录验证结果、未执行项和用户复验入口。
- [x] 将长期架构决策写入项目记忆。

# 定时任务可靠性加固 Test Spec

## 1. 测试目标

验证 V1 只加固现有 Cron 调度可靠性，覆盖迁移契约、配置校验、Mapper XML、Quartz 同步、启动恢复、日志筛选与脱敏、管理接口和前端同步状态。

## 2. 基线

- 当前 job 模块无 src/test 测试文件。
- 当前最大 Flyway 版本为 V1.0.39，本变更使用 V1.0.40。
- 当前工作区存在与本变更无关的全量初始化 SQL 和 .DS_Store 改动，验证与提交均排除。

## 3. 增量验证矩阵

| 优先级 | 范围 | 命令/方式 |
|---|---|---|
| P0 | job 模块当前编译基线 | mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test |
| P0 | Flyway 迁移契约 | JobReliabilityMigrationContractTest |
| P0 | 配置校验和 Mapper XML | JobConfigValidatorTest、MapperContractTest |
| P0 | Quartz 同步和启动恢复 | JobScheduleCoordinatorTest、JobStartupReconcilerTest |
| P0 | 日志脱敏和查询 | JobLogSanitizerTest、SysJobLogMapperContractTest |
| P0 | job 模块回归 | mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test |
| P1 | Admin 聚合构建 | mvn -pl forge-admin-server -am package -DskipTests |
| P1 | 前端 | pnpm 定向测试、lint、build |
| P1 | 文档和空白 | git diff --check |

## 4. 环境边界

- 不启动真实 Admin、Quartz 集群、MySQL 或 Redis 服务。
- 数据库迁移使用静态契约测试；真实 Flyway 和运行态重启恢复由用户联调。
- 前端命令使用 Node v20.19.0。

## 5. Task 4 增量验证

- 定向覆盖注解登记、唯一启动入口、缺失任务恢复、禁用任务保持暂停和非 Forge Job 保留。
- job 模块全量回归用于确认删除旧 `JobConfigLoader` 后 Spring 装配和既有测试无回归。
- Mapper XML 继续执行 `xmllint --noout`，相关目录执行 `git diff --check`。

## 6. Task 5 增量验证

- 红灯覆盖 Token、Authorization、手机号、密码字段脱敏，以及参数、结果和异常长度上限。
- 执行日志契约覆盖 `job_config_id`、`SCHEDULED/MANUAL` 触发来源和应用日志不输出原始执行结果。
- `JobMonitor` 单测验证实际存储对象已脱敏并带稳定任务关联；读取服务再次清洗历史日志响应。
- 前端定向 ESLint 验证时间范围按 `yyyy-MM-dd HH:mm:ss` 发送，避免 ISO UTC 转换导致筛选偏移。

## 7. Task 6 增量验证

- Controller 契约验证 `POST /job/config/{id}/sync` 调用 `retrySynchronization`，并保持 DTO/VO 分页和详情协议。
- 前端契约验证同步状态、错误摘要和“重新同步”入口存在，立即运行包含确认提示，编辑时 JobKey 不可修改。
- 页面默认信息架构只突出任务、执行内容、执行计划、运行状态、调度同步和操作；技术字段不再占据默认主列表。
- Node `v20.19.0` 下定向 ESLint 和生产构建必须通过；仓库既有构建警告记录但不作为本次阻断项。

## 8. Task 7 最终验证

- 复用当前成功基线，重新执行 job 模块全量测试和 Admin 聚合构建。
- 静态检查 Flyway placeholder、Mapper XML、查询规范和全部本变更差异空白。
- 尝试无真实后端的浏览器页面验证；若鉴权或接口依赖阻断，明确记录真实数据 UI 未联调，不以静态构建替代 E2E 结论。

## 9. 最终结果

| 范围 | 结果 |
|---|---|
| job 模块全量测试 | 37/37 通过 |
| Admin 聚合构建 | 42/42 模块通过 |
| 前端定向 ESLint | 通过 |
| 前端生产构建 | 8699 modules transformed，构建通过 |
| Flyway placeholder | 无业务占位符 |
| Mapper XML | `xmllint` 通过 |
| 本变更差异 | `git diff --check` 通过 |
| 浏览器 UI | 未完成；开发服务触发 `EMFILE`，生产预览端口绑定被沙箱 `EPERM` 拒绝 |

真实 MySQL/Flyway、Admin/Quartz 运行态和带真实权限数据的浏览器交互继续由开发环境联调，不能由本轮静态和构建验证替代。

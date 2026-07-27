# 执行日志 — Controller 边界与查询规范整改

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-27 | Research | 重新扫描当前 Controller 越层、异常和分页基线 | 14 文件/35 数据操作，4 文件/9 裸异常，3 个未兼容分页端点 |
| 2026-07-27 | Research | 复核 Flow 删除语义 | 2 类已是逻辑删除；5 类是管理员显式不可逆清理 |
| 2026-07-27 | Proposal/HARD-GATE | 创建四份 SDD 文档 | 用户已授权按分阶段方案继续 `/apply` |
| 2026-07-27 | Apply/Task 1 | 完成分页双读和 Controller 异常边界整改 | 保留旧 `page` 别名；Excel 响应不再透传底层异常 |
| 2026-07-27 | Apply/Task 2 | 完成 System 查询下沉 | `SysLoginLogController`、`LoginTenantAssetController` 不再构造查询或直连 Mapper |
| 2026-07-27 | Apply/Task 3 | 完成 AI 查询和默认项更新下沉 | 新增模型/供应商协调 Manager，保证供应商状态同步 |
| 2026-07-27 | Apply/Task 4 | 完成 Generator 查询和字段配置操作下沉 | 新增字段配置 Service，迁移 Mapper 注解 SQL 和查询 Wrapper 到 XML |
| 2026-07-27 | Apply/Task 5 | 完成 Data/Message 数据操作下沉 | 数据集发布状态更新保留租户/逻辑删除门禁；消息业务类型查询进入 XML |
| 2026-07-27 | Apply/Task 6 | 完成 Flow 查询、统计、状态同步和清理下沉 | 复用 `FlowMonitorService`；2 类逻辑删除、5 类管理员物理清理及确认门禁保持 |
| 2026-07-27 | Spec Review | 独立 Reviewer 逐条核验 Spec 与实现 | PASS；真实数据库 SQL 和不可逆清理 E2E 保留为部署门禁 |
| 2026-07-27 | Code Quality Review 1 | 独立 Reviewer 检查事务、安全边界、并发和模块依赖 | FAIL：0 Critical、4 Important；发现流程逐实例事务、异常泄露、AI 摘要并发覆盖和 Flow/System Service 耦合问题 |
| 2026-07-27 | Review Fix | 修复 4 个 Important | 流程增加逐实例事务和稳定公开文案，引入用户查询 SPI；AI 增加供应商/模型 `FOR UPDATE` 行锁和确定性锁顺序 |
| 2026-07-27 | Code Quality Review 2 | 独立 Reviewer 基于最新工作树复审 | FAIL：0 Critical、4 Important；发现事务传播、跨表锁顺序、供应商删除竞态和降级日志缺口 |
| 2026-07-27 | Review Fix 2 | 修复第二轮 4 个 Important | Flow 固定 `REQUIRES_NEW` 并记录降级堆栈；AI 统一供应商先锁、模型后锁，删除供应商进入 Manager 原子编排 |
| 2026-07-27 | Code Quality Review 3 | 独立 Reviewer 基于最新工作树复审 | FAIL：0 Critical、2 Important、1 Minor；发现批量清理前置查询异常边界、默认供应商写入旁路和外层事务测试缺口 |
| 2026-07-27 | Review Fix 3 | 修复第三轮全部发现 | Flow 增加前置查询稳定错误边界和外层事务挂起/恢复测试；AI 默认切换进入全量升序锁 Manager，通用保存忽略默认标志 |
| 2026-07-27 | Code Quality Review 4 | 独立 Reviewer 基于最新工作树复审 | FAIL：2 Critical、1 Important；发现清理专用权限/租户边界、批量筛选范围和 AI 超管跨租户缺口 |
| 2026-07-27 | Review Fix 4 | 修复第四轮全部发现 | Flow 清理端点增加专用权限和当前租户候选/归属校验；AI 默认切换显式贯穿必填租户 |
| 2026-07-27 | Code Quality Review 5 | 独立 Reviewer 基于最新工作树复审 | FAIL：3 Critical、2 Important、1 Minor；发现权限资源唯一键、表单清理租户、监控分级权限/归属、Mapper 参数和合同假阳性问题 |
| 2026-07-27 | Review Fix 5 | 修复第五轮全部发现 | Flow 查询/管理/清理权限分级，实例/任务归属前置校验，表单状态和清理 SQL 显式传租户，合同按目标 statement 精确断言 |
| 2026-07-27 | Code Quality Review 6 | 独立 Reviewer 基于最新工作树复审 | FAIL：2 Critical、2 Important、1 Minor；错误日志和监控聚合租户边界、迁移兼容碰撞、管理事务与 UI 权限可见性未闭合 |
| 2026-07-27 | Review Fix 6 | 修复第六轮全部发现 | 错误日志与监控查询显式租户化，管理/清理使用事务行锁，迁移补 PUT 权限并移除碰撞 UPDATE，UI 按 manage/cleanup 隐藏操作 |
| 2026-07-27 | Code Quality Final | 独立 Reviewer 定向复审最新工作树 | PASS；统一 business-first 锁顺序并让 UI 权限缺失时失败关闭后，无剩余或新增 Critical/Important/Minor |

## 验证记录

| 时间 | 范围 | 命令 | 结果 | 警告/跳过 |
|------|------|------|------|-----------|
| 2026-07-27 | Generator 静态合同 | `rg` 扫描 4 个目标 Controller；`xmllint --noout` 校验 4 份 Mapper XML；目标文件 `git diff --check` | 通过：Controller Wrapper/Mapper 直连、目标 Service 查询 Wrapper 均无输出，XML 和差异格式通过 | 未连接真实数据库，未执行 Mapper SQL |
| 2026-07-27 | Generator Reactor | `JAVA_HOME=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am package -DskipTests` | 29 个 Reactor 模块全部成功，`BUILD SUCCESS` | 测试按命令跳过；依赖模块存在既有 deprecation/unchecked/Builder 警告 |
| 2026-07-27 | Data/Message 静态合同 | 目标 Controller/Service `rg`；两份 Mapper XML `xmllint --noout`；目标差异 `git diff --check` | 通过：Wrapper/`lambda*` 扫描为空，XML 和差异格式通过 | 未连接真实数据库，未执行 Mapper SQL |
| 2026-07-27 | Data/Message Reactor | `JAVA_HOME=... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-framework/forge-plugin-parent/forge-plugin-message -am package -DskipTests` | 27 个 Reactor 模块全部成功，`BUILD SUCCESS` | 测试按命令跳过；依赖模块存在既有编译警告 |
| 2026-07-27 | Flow 合同测试 Red | `mvn -pl forge-flow/forge-flow-server test ... -Dtest=FlowControllerBoundaryContractTest` | 首次使用默认 Java 8 被目标版本 17 拒绝；切换 JDK 17 后 3 条合同测试按预期失败，证明 Controller/Mapper 基线可被测试捕获 | Java 版本问题已通过显式 `JAVA_HOME` 修正 |
| 2026-07-27 | Flow Mapper XML | `xmllint --noout` 校验 `FlowBusiness/Task/Comment/Cc/ErrorLog/FormInstance/FillBatchItem` 七份 XML | 全部解析通过 | 未连接真实 MySQL，未执行 SQL |
| 2026-07-27 | Flow Reactor | `JAVA_HOME=... mvn -pl forge-flow/forge-flow-server -am package -DskipTests` | 32 个 Reactor 模块全部成功，`BUILD SUCCESS`；最终复跑同样通过 | 测试按命令跳过；Generator/Flow 依赖模块有既有 deprecation/unchecked 警告 |
| 2026-07-27 | Flow 合同测试 Green | `mvn -pl forge-flow/forge-flow-server compiler:testCompile surefire:test ... -Dtest=FlowControllerBoundaryContractTest` | 5 条通过，0 failure/error/skip | 先由 Reactor 生成当前依赖类，规避只构建子模块时本地仓库旧快照 |
| 2026-07-27 | Flow 既有委托身份测试 | `mvn -pl forge-flow/forge-flow-server test ... -Dtest=FlowDelegatedIdentityControllerTest` | 未进入断言：3 条均因 Mockito inline Byte Buddy 无法 self-attach 报错 | 本机 JDK attach 环境限制；测试源码已编译，未将该结果计为通过 |
| 2026-07-27 | Maven 本地安装尝试 | `mvn -pl .../forge-plugin-flow install -DskipTests` | 失败：沙箱禁止写入用户目录 `.m2` | 非代码失败；改用 Reactor 联合构建和直接测试目标完成验证 |
| 2026-07-27 | Controller 聚合静态合同 | 全仓 Controller Wrapper/`lambda*`、裸 `RuntimeException` 扫描；Flow 分页双读检查；`git diff --check` | 全部通过：Wrapper 和裸异常无输出，`page/pageNum` 保持旧参数优先，差异格式通过 | Controller 直连 Mapper 的全仓历史存量另行盘点，不混入本变更基线 |
| 2026-07-27 | Spec Review 修复复跑 | 子模块直接执行 `compiler:testCompile surefire:test -Dtest=FlowControllerBoundaryContractTest` | 未进入断言：测试编译看不到当前工作树新增的 `PageParamResolver` | 子模块引用本机 `.m2` 旧 `forge-starter-core` 快照；当前 Reactor 的目标类已存在，不计为代码失败 |
| 2026-07-27 | Spec Review 修复有效验证 | `dependency:build-classpath` 后以 `javac` 加入当前 `forge-starter-core/target/classes` 编译目标测试，再用 `mvn surefire:test -Dmaven.test.additionalClasspath=...` 执行 | 7 条通过，0 failure/error/skip；覆盖分页四组合、分布首个非空标题、清理和统计合同 | Maven 报三个本地快照 POM 的既有 transitive dependency 警告；未影响本次测试 |
| 2026-07-27 | Excel 安全合同初次复跑 | `mvn -pl .../forge-starter-excel test -Dtest=AsyncExportSecurityContractTest,ExcelImportSecurityContractTest`，并尝试覆盖通用 skip 参数 | `BUILD SUCCESS`，但 `testCompile` 和 Surefire 明确显示测试被跳过，不计为通过 | 根 POM 使用 `forge.tests.skip=true`，需启用 `enable-tests` profile |
| 2026-07-27 | Excel 安全合同有效复跑 | `mvn -Penable-tests -pl .../forge-starter-excel test -Dtest=AsyncExportSecurityContractTest,ExcelImportSecurityContractTest` | 5 条通过，0 failure/error/skip；异步导出 2 条、同步导入 3 条 | 测试故意触发的内部异常完整写入测试日志，公开对象断言确认不含内部消息和路径 |
| 2026-07-27 | System 安全合同 Reactor 尝试 | `mvn -Penable-tests -pl .../forge-plugin-system -am test -Dtest=SysUserImportSecurityContractTest -Dsurefire.failIfNoSpecifiedTests=false` | 未执行到 System：前置 `forge-starter-datascope` 因测试类路径没有 JUnit/TestNG engine 且配置了 groups 而失败 | 环境/聚合测试配置问题，不计为目标测试失败 |
| 2026-07-27 | System 安全合同有效复跑 | 根 Reactor 生成测试依赖 classpath，`javac` 单独编译 `SysUserImportSecurityContractTest`，随后 `mvn -Penable-tests -pl .../forge-plugin-system surefire:test -Dtest=SysUserImportSecurityContractTest` | 1 条通过，0 failure/error/skip；确认用户导入只公开稳定文案并记录完整异常 | 测试为源码合同，不连接数据库；规避 System 单模块读取 `.m2` 旧 `starter-core` 快照 |
| 2026-07-27 | Flow Code Review 修复合同首次复跑 | `mvn -Penable-tests -pl forge-flow/forge-flow-server surefire:test -Dtest=FlowControllerBoundaryContractTest -Dmaven.test.additionalClasspath=.../forge-starter-core/target/classes` | 执行 9 条，8 条通过、1 条失败；发现流程变量和活动节点接口仍拼接系统异常消息 | 有效测试失败，继续修复，不计为通过 |
| 2026-07-27 | Flow Code Review 修复合同 Green | 同上，修复两个遗留异常分支后复跑 | 9 条通过，0 failure/error/skip | 未连接数据库、未执行流程清理 |
| 2026-07-27 | AI 并发合同 | `mvn -Penable-tests -pl .../forge-plugin-ai test -Dtest=AiModelProviderConcurrencyContractTest` | 1 条通过，0 failure/error/skip | Mockito-free 源码合同；未连接数据库执行真实并发事务 |
| 2026-07-27 | Flow+AI 修复后 Reactor | `mvn -pl forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-ai -am package -DskipTests` | 32 个 Reactor 模块全部成功，`BUILD SUCCESS` | 测试按命令跳过；既有 deprecation/unchecked/Builder 警告不阻断 |
| 2026-07-27 | AI 第二轮并发合同首次复跑 | `mvn -Penable-tests -pl .../forge-plugin-ai test -Dtest=AiModelProviderConcurrencyContractTest` | 生产代码和测试编译通过，1 条断言失败 | 测试源码切片错误地要求下一个方法为 `public`；修复测试解析后复跑，不计为实现失败 |
| 2026-07-27 | AI 第二轮并发合同 Green | 同上 | 1 条通过，0 failure/error/skip | 覆盖供应商先锁、模型后锁和供应商删除原子编排的源码合同 |
| 2026-07-27 | Flow 独立事务测试 | `mvn -Penable-tests -pl .../forge-plugin-flow test -Dtest=FlowCleanupTransactionExecutorTest` | 1 条通过，0 failure/error/skip | 无 Mockito；验证 3 次均为 `REQUIRES_NEW`、2 次提交和 1 次回滚 |
| 2026-07-27 | Flow 第二轮边界合同 | 手工 `javac` 使用当前 Reactor classpath 编译测试后执行 `surefire:test -Dtest=FlowControllerBoundaryContractTest` | 9 条通过，0 failure/error/skip | 覆盖事务执行器源码、稳定文案、降级日志和 SPI 解耦 |
| 2026-07-27 | Flow+AI 第二轮修复后 Reactor | `mvn -pl forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-ai -am package -DskipTests` | 32 个 Reactor 模块全部成功，`BUILD SUCCESS` | 测试按命令跳过；既有编译警告不阻断 |
| 2026-07-27 | 第三轮回归合同 Red | AI 并发合同直接执行；Flow 合同按常规 Maven 命令尝试 | AI 1 条按预期失败，证明默认供应商旁路可被捕获；Flow 因本机 `.m2` 旧 `forge-starter-core` 快照无法编译新增 `PageParamResolver` | Flow 环境限制与既有记录一致，不计为实现失败 |
| 2026-07-27 | AI 第三轮并发合同 Green | `mvn -Penable-tests -pl .../forge-plugin-ai test -Dtest=AiModelProviderConcurrencyContractTest` | 1 条通过，0 failure/error/skip | 覆盖默认切换 Manager、全量升序供应商锁和通用保存默认标志隔离 |
| 2026-07-27 | Flow 第三轮边界合同 Green | 使用 `/private/tmp/forge-flow-test-classpath.txt` 和当前 `forge-starter-core/target/classes` 手工编译目标测试，再执行 `surefire:test` | 10 条通过，0 failure/error/skip | 新增批量清理候选查询稳定错误边界合同；未连接数据库或执行清理 |
| 2026-07-27 | Flow 独立事务增强测试 | `mvn -Penable-tests -pl .../forge-plugin-flow test -Dtest=FlowCleanupTransactionExecutorTest` | 2 条通过，0 failure/error/skip | 覆盖 3 次独立执行、2 提交/1 回滚及外层事务挂起/恢复 |
| 2026-07-27 | AI Mapper 与目标格式 | `xmllint --noout AiProviderMapper.xml`；目标文件 `git diff --check`；默认写入旁路 `rg` | 全部通过 | `isDefault` 仅保留响应读取和兼容 DTO 字段，生产保存路径不读取该字段 |
| 2026-07-27 | Flow+AI 第三轮修复后 Reactor | `mvn -q -pl forge-flow/forge-flow-server,forge-framework/forge-plugin-parent/forge-plugin-ai -am package -DskipTests` | 32 模块聚合构建退出码 0 | 测试按命令跳过；目标测试已单独有效执行，既有编译警告不阻断 |
| 2026-07-27 | Code Quality Review 4 | 独立 Reviewer 基于最新工作树复审 | FAIL：2 Critical、1 Important；清理权限/租户、筛选范围和 AI 超管跨租户边界不完整 | 未连接数据库，未执行清理 |
| 2026-07-27 | 第四轮安全合同 Red/Green | Flow 合同先 2 failure/1 error、AI 合同 1 failure；修复后执行 `FlowControllerBoundaryContractTest`、`AiModelProviderConcurrencyContractTest` | Green：Flow 12/12、AI 1/1 | Flow 单模块首次受 `.m2` 旧 core 快照影响，按既有方案使用当前 Reactor 产物编译/执行 |
| 2026-07-27 | 第四轮事务/XML/Flyway | `FlowCleanupTransactionExecutorTest`；9 份 Mapper XML `xmllint`；`V1.0.55` placeholder/权限/防重复静态检查 | 事务 2/2，XML 全部合法，Flyway 静态合同通过 | 未执行真实 MySQL/Flyway 或 Flowable 清理 |
| 2026-07-27 | 第五轮 Flow 合同与事务 | `FlowControllerBoundaryContractTest`；`FlowCleanupTransactionExecutorTest` | Flow Controller 合同 14/14、独立事务测试 2/2 | 未连接真实数据库，未执行 Flowable 清理 |
| 2026-07-27 | 第五轮 Flow XML/构建/静态合同 | 七份 Flow Mapper XML `xmllint --noout`；Flow Server 依赖 Reactor `package -DskipTests`；Controller/Flyway/差异扫描 | XML 全部合法，32 个 Reactor 模块构建成功；Wrapper、裸异常、placeholder、Flyway 版本重复均无输出，`git diff --check` 通过 | 测试按构建命令跳过；目标合同已单独有效执行，真实 MySQL/Flyway 和角色矩阵保留为部署门禁 |
| 2026-07-27 | 第六轮 Flow 定向验证 | `forge-plugin-flow compile -DskipTests`；`FlowControllerBoundaryContractTest`；两份 Mapper XML；监控页 ESLint；目标差异检查 | 插件编译成功，合同最终 18/18，XML、ESLint 和差异格式通过 | 按用户要求不重复全 Reactor/UI build；真实数据库、角色矩阵、并发与不可逆清理未执行 |

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 原清单 Wrapper 数量 | 14 文件/40 处 | 当前代码为 14 文件/35 个操作 | 以可复现的当前扫描基线为准 |
| 原清单裸异常 | 4 文件/10 处 | 当前代码为 4 文件/9 处 | 以当前扫描基线为准 |
| 原清单分页 | 4 处 | `FlowFormController` 已双读，剩余 3 处 | 保留其为兼容参考，修复剩余 3 处 |
| 原清单流程删除 | 7 处均视为物理删除 | 2 个实体已有 `@TableLogic` | 明确 2 逻辑 + 5 管理清理物理语义 |

## 服务状态

- 本变更尚未启动任何服务。
- 未连接真实数据库，未执行任何 Flowable 或 Forge 流程数据清理。

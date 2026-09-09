# 低代码统一参数化只读查询源执行日志

## 1. 基线

- 日期：2026-08-10
- 状态：实施中
- 前置变更：`external-connector-hardening` 已完成并通过 external 12、data 3、outbound 48 个测试及受影响模块聚合编译。
- 用户授权：按既定七阶段路线连续实施，无需阶段间确认。
- 工作区已有改动继续保留，不覆盖预售原型/PRD、`.DS_Store` 和 `output/视频草稿-企业协同与开放平台集成.md`。
- 本阶段不启动真实服务，不修改真实数据库、Redis 或外部系统运行态。

## 2. 设计结论

- 统一来源固定为 `EXTERNAL_API`、`DATASET`，不接受任意 URL 或 SQL。
- 外部来源使用 `systemCode/apiCode`，数据集使用 `datasetCode`，保证低代码配置可跨环境迁移。
- 外部 API 默认不开放，只有显式启用、声明输入/输出契约并配置权限后可用。
- 数据集继续复用现有发布态、ACL、行权限、字段白名单和脱敏链路。
- 统一网关只做路由和协议归一，不复制来源模块安全逻辑。

## 3. 执行记录

| 时间 | 动作 | 结果 |
|---|---|---|
| 2026-08-10 | 盘点现有 linkageSchema、外部 API、数据集 Runtime 和低代码 Controller | 完成 |
| 2026-08-10 | 建立第二阶段四份 SDD 文档 | 完成 |
| 2026-08-10 | 完成 external 查询资格、输入/输出契约、稳定键 Facade 与幂等 Flyway | 完成 |
| 2026-08-10 | 完成 data Runtime Service 抽取及按 `datasetCode` 的元数据/执行入口 | 完成 |
| 2026-08-10 | 完成 generator 统一目录、元数据、执行路由和前端 API/管理配置 | 完成 |
| 2026-08-10 | 自审发现并修复非法 `maxLength` 冒出底层 `NumberFormatException` | 已先以失败测试复现，再统一为 `BusinessException` |

## 4. 验证记录

### 4.1 Java 测试

- external 全量：`mvn -Penable-tests test`，19 tests passed。
- data 定向：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data -am -Penable-tests -Dtest=DataDatasetRuntimeServiceTest,DataQueryExecutorTest,DataQueryRuntimeCacheTest -Dsurefire.failIfNoSpecifiedTests=false test`，5 tests passed。
- generator 首次 Reactor 定向测试被既有 `forge-plugin-message/MessageServiceImplTest` 构造器少传 `ApplicationEventPublisher` 阻断，目标模块尚未执行；未修改该无关模块。
- 隔离验证：先以 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am install -Dmaven.test.skip=true` 安装上游，再在 generator 模块执行 `mvn -Penable-tests -Dtest=LowcodeQuerySourceServiceTest,LowcodeQuerySourceControllerContractTest test`，5 tests passed。
- external/data/generator 聚合编译：`mvn -pl :forge-plugin-external,:forge-plugin-data,:forge-plugin-generator -am compile -DskipTests`，32 个 Reactor 模块 BUILD SUCCESS。
- 所有 Maven 命令均显式使用 JDK 17。

### 4.2 前端与静态检查

- 目标文件 ESLint：0 errors；`external/api.ts`、`lowcode-query-source.ts` 因仓库 ESLint 未提供匹配 TypeScript 的配置而提示 ignored warning。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`：成功，8883 modules transformed。
- 构建保留仓库既有 warning：组件同名、动态/静态混合导入、CSS `//` 注释；均非本阶段引入且不阻断。
- `git diff --check`：通过。
- V1.0.101/V1.0.102 Flyway `${...}`、敏感信息、租户 0 扫描：无命中。
- 统一网关 DTO/VO/Controller 的 URL/Header/SQL/脚本/凭据字段扫描：无命中。
- 查询网关敏感日志扫描：无命中。

## 5. 警告与跳过项

- 未执行真实 MySQL Flyway、真实外部 API、真实数据集、多租户和不同角色联调，按既定分工留待部署环境补验。
- 未启动 Admin、Flow、Vite、数据库、Redis 或外部服务，无新增服务进程需要清理。
- Reactor 全测试仍存在与本阶段无关的 `forge-plugin-message/MessageServiceImplTest` 测试编译问题；本阶段目标模块已隔离验证通过。

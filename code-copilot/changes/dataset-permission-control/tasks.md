# 任务拆分 — 数据集权限控制
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）
> 每个任务必须精确到文件路径和函数签名

## 前置条件
- [x] 已确认采用双层权限模型：访问权限 + 行权限。
- [x] 第一阶段默认 `PUBLIC`，不影响历史数据集。

## Task 1: 数据集访问权限后端基础
- **目标**: 增加数据集访问模式和 ACL 表，并在后端列表、详情、运行时入口校验。
- **涉及文件**:
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/sql/data_tables.sql` — 增加建表字段和 ACL 表。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/sql/data_dataset_permission_upgrade_20260515.sql` — 新增幂等升级脚本。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDataset.java` — 新增 `accessMode`。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDatasetAcl.java` — 新增 ACL 实体。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/dto/DataDatasetAclDTO.java` — 新增授权项 DTO。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/dto/DataDatasetSaveDTO.java` — 增加访问权限字段。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/vo/DataDatasetDetailVO.java` — 返回访问权限字段。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/mapper/DataDatasetMapper.java` — 数据集列表增加访问过滤参数。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetMapper.xml` — 数据集查询增加 ACL 过滤 SQL。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/mapper/DataDatasetAclMapper.java` — 新增 ACL Mapper。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetAclMapper.xml` — 新增 ACL 查询 XML。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataDatasetAccessService.java` — 新增访问权限服务接口。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetAccessServiceImpl.java` — 新增访问权限服务实现。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetServiceImpl.java` — 列表查询注入访问过滤。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetController.java` — 保存/返回/校验访问权限。
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetRuntimeController.java` — 运行时校验访问权限。
- **关键签名**:
  ```java
  void requireAccess(DataDataset dataset, DataDatasetAccessLevelEnum requiredLevel);
  DataDatasetAccessQuery buildCurrentUserAccessQuery(DataDatasetAccessLevelEnum requiredLevel);
  void saveDatasetAcl(Long datasetId, List<DataDatasetAclDTO> aclItems);
  ```
- **验证**:
  ```bash
  cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data -am compile -DskipTests
  ```
- **状态**: completed

## Task 2: 数据集行权限后端引擎
- **目标**: 增加数据集字段语义映射和运行时行过滤条件构造，不改现有 `DataScopeInterceptor`。
- **涉及文件**:
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/entity/DataDatasetRowScope.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/dto/DataDatasetRowScopeDTO.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/mapper/DataDatasetRowScopeMapper.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/mapper/DataDatasetRowScopeMapper.xml`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataDatasetRowScopeService.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/impl/DataDatasetRowScopeServiceImpl.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/support/DataDatasetRowScopeCondition.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataQueryExecutor.java`
  - `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/resources/sql/data_dataset_row_scope_upgrade_20260515.sql`
- **关键签名**:
  ```java
  DataDatasetRowScopeCondition buildCondition(DataDataset dataset, DbDialect dialect);
  QueryBuildResult applyRowScope(DataDataset dataset, DbDialect dialect, QueryBuildResult buildResult);
  ```
- **验证**:
  ```bash
  cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data -am compile -DskipTests
  cd forge && mvn -pl forge-admin-server -am compile -DskipTests
  ```
- **状态**: completed

## Task 3: admin-ui 权限配置与联调
- **目标**: 在数据集编辑流中增加访问权限和行权限配置页。
- **涉及文件**:
  - `forge-admin-ui/src/api/data/dataset.ts`
  - `forge-admin-ui/src/views/data/dataset.vue`
  - 复用 `/system/role/page`、`/system/user/page`、`/system/org/tree` 作为授权主体选择数据源。
- **验证**:
  ```bash
  cd forge-admin-ui && pnpm exec eslint src/views/data/dataset.vue
  cd forge-admin-ui && pnpm build
  ```
- **状态**: completed

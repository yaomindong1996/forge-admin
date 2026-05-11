# 任务清单：report-datasource-dataset
> status: completed
> created: 2026-05-11

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 新建平台数据插件与数据库表 | pending | P0 |
| Task 2 | 数据连接管理 | pending | P0 |
| Task 3 | 数据集管理 | pending | P0 |
| Task 4 | 查询执行器 | pending | P0 |
| Task 5 | admin-ui 管理页面 | pending | P0 |
| Task 6 | report-ui 数据集模式 | pending | P0 |
| Task 7 | 联调与回归 | pending | P0 |

---

## Task 1: 新建平台数据插件与数据库表

**目标**: 新建 `forge-plugin-data` 模块，建立三张数据表实体、Mapper、XML、Service 基础 CRUD。

**状态**: completed

**范围**:
- 新建 `forge/forge-framework/forge-plugin-parent/forge-plugin-data` 模块
- 创建 `pom.xml`，依赖 forge-starter-core、forge-starter-web、forge-starter-orm、forge-starter-cache、forge-starter-crypto
- 修改 `forge-plugin-parent/pom.xml` 增加模块
- 修改 `forge-admin-server/pom.xml` 增加依赖
- 修改 `forge-report-server/pom.xml` 增加依赖
- 创建数据库表 SQL 文件（`ai_report_data_connection`、`ai_report_data_dataset`、`ai_report_data_dataset_field`）
- 创建 Entity 类（`DataConnection`、`DataDataset`、`DataDatasetField`）
- 创建 Mapper 接口和 XML（基础 CRUD）
- 创建 Service 接口和实现（基础 CRUD）

**验证**:
```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-admin-server,forge-report-server -am compile -DskipTests
```

**产出**:
- `forge-plugin-data` 模块结构
- 三张表 SQL
- Entity/Mapper/Service 基础框架

**状态**: pending

---

## Task 2: 数据连接管理

**目标**: 实现数据连接 CRUD、测试连接、元数据查询接口。

**状态**: completed

**范围**:
- Controller：`DataConnectionController`
  - 分页查询 `GET /data/connection/page`
  - 列表查询 `GET /data/connection/list`
  - 详情查询 `GET /data/connection/:id`
  - 新增 `POST /data/connection`
  - 修改 `PUT /data/connection`
  - 删除 `DELETE /data/connection/:id`
  - 测试已保存连接 `POST /data/connection/:id/test`
  - 测试未保存连接 `POST /data/connection/test`
  - 表列表 `GET /data/connection/:id/tables`
  - 字段列表 `GET /data/connection/:id/tables/:tableName/fields`
- Service 实现：
  - 密码加密/解密（使用统一密钥）
  - 连接详情脱敏返回（`hasPassword` 标记）
  - 测试连接（创建临时 DataSource 并执行 testSql）
  - 元数据查询（DatabaseMetaData 读取表和字段）
- 支持类：
  - `JdbcDataSourceProvider`（连接池管理）
  - `DbDialect`（数据库方言抽象，首期实现 MySQL）
- DTO/VO：
  - `DataConnectionPageDTO`
  - `DataConnectionDetailVO`
  - `DataConnectionTestDTO`
  - `DataConnectionTableVO`
  - `DataConnectionFieldVO`
- 权限资源：`data:connection:list/add/edit/remove/test`

**验证**:
```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-admin-server -am compile -DskipTests
```

**产出**:
- 数据连接管理完整接口
- 连接测试和元数据查询能力

**状态**: pending

---

## Task 3: 数据集管理

**目标**: 实现数据集 CRUD、字段同步、预览接口。

**状态**: completed

**范围**:
- Controller：
  - `DataDatasetController`
    - 分页查询 `GET /data/dataset/page`
    - 列表查询 `GET /data/dataset/list`
    - 详情查询 `GET /data/dataset/:id`
    - 新增 `POST /data/dataset`
    - 修改 `PUT /data/dataset`
    - 删除 `DELETE /data/dataset/:id`
    - 同步字段 `POST /data/dataset/:id/sync-fields`
    - 字段保存 `PUT /data/dataset/:id/fields`
    - 预览 `POST /data/dataset/:id/preview`
- Service 实现：
  - 数据集保存校验（TABLE 必须有表名，SQL 必须通过安全校验）
  - 字段同步（TABLE 从元数据读取，SQL 从预览结果推断）
  - 字段批量保存
  - 预览查询
- DTO/VO：
  - `DataDatasetPageDTO`
  - `DataDatasetDetailVO`
  - `DataDatasetSaveDTO`
  - `DataDatasetFieldVO`
  - `DataDatasetPreviewDTO`
- 权限资源：`data:dataset:list/add/edit/remove/preview`

**验证**:
```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-admin-server -am compile -DskipTests
```

**产出**:
- 数据集管理完整接口
- 字段同步和预览能力

**状态**: pending

---

## Task 4: 查询执行器

**目标**: 实现 TABLE 查询构造、SQL 安全校验、参数绑定、结果转换。

**状态**: completed

**范围**:
- Controller：
  - `DataDatasetRuntimeController`
    - 执行查询 `POST /data/dataset/runtime/query`
    - 获取元数据 `GET /data/dataset/runtime/:id/metadata`
- Service 实现：
  - `DataQueryExecutor`
    - TABLE 查询构造（字段白名单、筛选条件、排序、分页）
    - SQL 查询执行（预编译 + 参数绑定）
    - 结果转换（`{ dimensions, source, total, fields }`）
    - 字段脱敏（`HIDDEN` 字段移除，`MASK` 字段脱敏）
- 支持类：
  - `SqlSafetyValidator`（SQL 安全校验：禁止 DDL/DML、多语句、危险函数）
  - `SqlParameterBinder`（命名参数解析和 PreparedStatement 绑定）
  - `TableQueryBuilder`（TABLE 数据集 SQL 构造）
  - `DatasetResultMapper`（JDBC ResultSet 转换为 JSON）
- DTO/VO：
  - `DataDatasetQueryDTO`
  - `DataDatasetQueryResultVO`
  - `DataDatasetMetadataVO`
- 权限：运行时查询首期要求登录态

**验证**:
```bash
cd forge && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-data,forge-report-server -am compile -DskipTests
```

**产出**:
- 查询执行器完整实现
- SQL 安全校验和参数绑定

**状态**: pending

---

## Task 5: admin-ui 管理页面

**目标**: 新增数据连接和数据集管理页面。

**范围**:
- 菜单资源：
  - 数据资产（父菜单）
    - 数据连接
    - 数据集管理
- 页面：
  - `forge-admin-ui/src/views/data/connection/index.vue`（数据连接列表页）
  - `forge-admin-ui/src/views/data/dataset/index.vue`（数据集列表页）
  - `forge-admin-ui/src/views/data/dataset/FieldConfigPanel.vue`（字段配置面板）
  - `forge-admin-ui/src/views/data/dataset/ParamConfigPanel.vue`（参数配置面板）
  - `forge-admin-ui/src/views/data/dataset/PreviewPanel.vue`（预览面板）
- API 封装：
  - `forge-admin-ui/src/api/data/connection.ts`
  - `forge-admin-ui/src/api/data/dataset.ts`
- 组件：
  - 数据连接表单（测试连接按钮）
  - 数据集表单（TABLE/SQL 切换、字段同步）
  - 字段配置表格（批量编辑）
  - 预览面板（参数输入 + 结果表格）

**验证**:
```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && cd forge-admin-ui && pnpm build
```

**产出**:
- 数据连接管理页面
- 数据集管理页面（含字段配置、参数配置、预览）

**状态**: pending

---

## Task 6: report-ui 数据集模式

**目标**: 新增报表组件「数据库数据集」数据加载模式。

**状态**: completed

---

## Task 7: 联调与回归

**目标**: 验证管理端和报表端完整流程，确保无破坏性变更。

**状态**: completed
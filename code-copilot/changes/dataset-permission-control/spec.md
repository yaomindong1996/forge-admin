# 数据集权限控制
> status: apply
> created: 2026-05-15
> complexity: 🔴复杂

## 1. 背景与目标
现有数据集发布后只依赖菜单/API权限和登录态控制，无法限制“哪些角色/用户可以使用指定数据集”，也没有把数据集查询结果纳入系统数据权限体系。

目标：
- 为数据集增加第一层访问权限，控制数据集是否可见、可查询。
- 后续为数据集运行时查询增加第二层行权限，复用登录人的系统数据权限上下文。
- 不改动现有 `sys_role.data_scope`、`DataScopeInterceptor` 的行为，新增能力默认保持兼容。

## 2. 代码现状（Research Findings）
### 2.1 相关入口与链路
- `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetController.java`：数据集管理入口，包含分页、列表、详情、发布、预览。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/controller/DataDatasetRuntimeController.java`：运行时元数据和查询入口。
- `forge/forge-framework/forge-plugin-parent/forge-plugin-data/src/main/java/com/mdframe/forge/plugin/data/service/DataQueryExecutor.java`：数据集运行时直接通过 JDBC 执行外部数据源 SQL，不走 MyBatis 查询。

### 2.2 现有实现
- `DataDatasetMapper.xml` 查询 `ai_report_data_dataset` 时仅按租户、状态、发布状态过滤，没有数据集访问 ACL。
- `DataScopeInterceptor` 按 MyBatis `mapperMethod` 匹配 `sys_data_scope_config` 后改写 SQL，只覆盖 MyBatis 查询链路。
- `DataScopeType.REGION` 当前是本级 + 直接下级行政区划，不满足“盟市看区县和网格”这种所有后代要求。

### 2.3 发现与风险
- 数据集运行时 SQL 可能查询外部业务库，不能在外部 SQL 中直接依赖平台库 `sys_region_code`。
- 行权限必须在数据集执行器中根据登录人上下文预先计算可访问范围，再参数化拼接到运行时 SQL。
- 第一阶段只做访问权限，所有历史数据集默认 `PUBLIC`，避免影响现有报表和数据资产管理。

## 3. 功能点
- [x] 阶段一：数据集访问权限数据模型和后端校验，默认公开不影响现有行为。
- [x] 阶段二：数据集行权限配置和运行时 SQL 过滤引擎。
- [x] 阶段三：admin-ui 权限配置页、联调和回归。

## 4. 业务规则
- 数据集访问权限分两层：
  - 访问权限：控制能不能看到/使用数据集。
  - 行权限：控制能看到哪些数据行。
- 阶段一 `access_mode=PUBLIC` 时保持现有行为。
- 阶段一 `access_mode=PRIVATE` 时，超级管理员、租户管理员、创建人、ACL 授权主体可访问。
- ACL 主体支持 `USER`、`ROLE`、`ORG`，权限级别支持 `VIEW`、`QUERY`、`MANAGE`。
- 查询数据集运行时接口要求 `QUERY` 权限；元数据、列表、详情要求 `VIEW` 权限。

## 5. 数据变更
| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 修改 | `ai_report_data_dataset` | `access_mode` | 数据集访问模式，默认 `PUBLIC` |
| 新增 | `ai_report_data_dataset_acl` | `dataset_id/subject_type/subject_id/access_level` | 数据集授权主体表 |
| 新增 | `ai_report_data_dataset_row_scope` | `dataset_id/enabled/*_column/region_strategy` | 数据集行权限字段映射表 |

## 6. 接口变更
| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 查询 | `/data/dataset/page` | GET | 私有数据集按当前用户 ACL 过滤 |
| 查询 | `/data/dataset/list` | GET | 私有数据集按当前用户 ACL 过滤 |
| 查询 | `/data/dataset/{id}` | GET | 私有数据集校验 VIEW 权限并返回 ACL |
| 新增 | `/data/dataset` | POST | 可保存 `accessMode` 和 `aclItems` |
| 修改 | `/data/dataset` | PUT | 可保存 `accessMode` 和 `aclItems` |
| 查询 | `/data/dataset/runtime/{id}/metadata` | GET | 校验 VIEW 权限 |
| 查询 | `/data/dataset/runtime/query` | POST | 校验 QUERY 权限 |
| 新增/修改 | `/data/dataset` | POST/PUT | 可保存 `rowScope` 行权限配置 |

## 7. 影响范围
- 只影响 `forge-plugin-data` 数据集模块。
- 不修改系统角色、菜单授权、现有数据权限拦截器。
- 历史数据集默认 `PUBLIC`，行为保持不变。

## 8. 风险与关注点
> ⚠️ 涉及权限变更，必须保持默认兼容。

- 私有数据集运行时必须二次校验，不能只过滤列表。
- SQL 行权限阶段不能直接拼接用户输入，必须参数绑定。
- 数据集行权限后续不能依赖外部数据源存在平台组织/区划表。

## 8.5 测试策略
- **测试范围**：`forge-plugin-data` 编译、数据集列表过滤、运行时权限校验、前端数据集编辑页 lint/build。
- **覆盖率目标**：以编译、前端 lint/build 和接口回归为主。
- **独立 Test Spec**：否。

## 9. 待澄清
- [x] 阶段三 UI 允许授权到角色、用户和组织。

## 10. 技术决策
- 第一阶段只新增数据集 ACL，不改现有 `sys_role.data_scope` 和 `DataScopeInterceptor`。
- `PUBLIC` 作为默认值，确保升级后所有历史数据集仍可被原使用方访问。
- ACL 查询写在 `DataDatasetMapper.xml` / `DataDatasetAclMapper.xml`，符合项目 SQL 约定。
- 第二阶段不直接依赖 `forge-starter-datascope`，避免数据插件引入全局 MyBatis 拦截器影响现有服务。
- 行权限默认关闭；SQL 数据集启用行权限时必须显式放置 `/*DATA_SCOPE*/` 占位符。
- 第三阶段 UI 将权限配置放到数据集编辑向导第 4 步，字段映射优先来自数据集字段，单表新增时可刷新来源表字段作为待选项。

## 11. 执行日志
| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | completed | `forge-plugin-data` ACL 模型、Mapper、Service、Controller、SQL；`forge-admin-ui/src/api/data/dataset.ts` | 阶段一：数据集访问权限，默认 PUBLIC 兼容 |
| Task 2 | completed | `forge-plugin-data` RowScope 模型、Mapper、Service、`DataQueryExecutor`、SQL | 阶段二：行权限配置与运行时过滤，默认关闭兼容 |
| Task 3 | completed | `forge-admin-ui/src/views/data/dataset.vue`、`forge-admin-ui/src/api/data/dataset.ts` | 阶段三：编辑向导第 4 步，访问 ACL 与行权限配置 |

## 12. 审查结论
三阶段已完成；未修改现有系统权限模块和 `DataScopeInterceptor`。数据集权限能力默认兼容：访问模式默认 `PUBLIC`，行权限默认关闭。

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-05-15
- **确认人**：用户口头要求分阶段实现

# 低代码统一参数化只读查询源测试规格

## 1. 外部查询契约

| ID | 场景 | 预期 |
|---|---|---|
| EXTQ-01 | 未开启低代码查询 | 不出现在目录，统一网关拒绝执行 |
| EXTQ-02 | PUT/PATCH/DELETE 开启低代码查询 | 保存配置失败 |
| EXTQ-03 | 未启用权限或权限码为空 | 保存配置失败 |
| EXTQ-04 | 输入/输出 Schema 非数组、字段重复或类型非法 | 保存配置失败 |
| EXTQ-05 | 运行参数包含未声明字段 | 失败且不发送外部请求 |
| EXTQ-06 | 必填参数缺失、类型错误或字符串超长 | 明确业务错误且不回显参数值 |
| EXTQ-07 | 合法参数 | 仅把 Schema 声明字段交给现有代理链 |
| EXTQ-08 | 稳定 key 解析 | 按当前租户、启用系统/API 和查询开关解析 |
| EXTQ-09 | 目录返回 | 不包含 URL、Header、固定参数、脚本和认证信息 |

## 2. 数据集 Runtime Service

| ID | 场景 | 预期 |
|---|---|---|
| DATAQ-01 | 数据集不存在/未发布/禁用 | 明确拒绝 |
| DATAQ-02 | 当前用户无 QUERY 权限 | ACL 拒绝 |
| DATAQ-03 | 连接不存在/禁用 | 明确拒绝 |
| DATAQ-04 | 合法查询 | 加载字段并委托 `DataQueryExecutor` |
| DATAQ-05 | 原 Controller 查询 | 委托 Runtime Service，协议不变 |
| DATAQ-06 | 按 datasetCode 查询 | 仍执行发布态、ACL 和字段安全校验 |

## 3. 统一网关

| ID | 场景 | 预期 |
|---|---|---|
| GATE-01 | `EXTERNAL_API` | 路由到外部查询 Facade，统一封装来源和数据 |
| GATE-02 | `DATASET` | 路由到数据集 Runtime Service，统一封装分页和字段 |
| GATE-03 | 非法来源类型/空 sourceKey | 失败关闭 |
| GATE-04 | 目录关键字 | 只过滤名称/编码，不泄露内部配置 |
| GATE-05 | 元数据 | 返回输入 Schema 和最小字段描述 |

## 4. Flyway 与静态检查

| ID | 场景 | 预期 |
|---|---|---|
| SQL-01 | 列迁移 | 三列通过 `information_schema` 幂等新增 |
| SQL-02 | 索引迁移 | 通过 `information_schema.STATISTICS` 幂等新增 |
| SQL-03 | 默认行为 | `lowcode_query_enabled` 默认 0，不自动开放存量接口 |
| SQL-04 | 安全扫描 | 无 `${...}`、租户 0、客户 URL、凭据或业务专用字段 |

## 5. 验证层级

1. Java 单元测试与 Mapper/Flyway 静态契约测试。
2. external、data、generator 定向 Maven 测试，必须使用 JDK 17 和 `-Penable-tests`。
3. 受影响模块聚合编译。
4. 前端 ESLint、类型/生产构建。
5. `git diff --check`、敏感日志和任意 URL/SQL 接口静态扫描。

## 6. 部署环境补验

- Flyway 在真实 MySQL 8 测试库执行；
- 外部 API 白名单、权限、限流和真实查询联调；
- 数据集真实只读账号、ACL、行权限和脱敏联调；
- 多租户、不同角色目录可见性验证。

## 7. 2026-08-10 增量验证结果

- external 模块全量回归：19 个测试通过，覆盖阶段 1 安全基线及本阶段契约、稳定键、Mapper 和 Flyway。
- data 定向回归：5 个测试通过，覆盖 Runtime Service、只读执行失败语义和运行缓存。
- generator 定向回归：5 个测试通过，覆盖 Controller 契约和两类来源路由。
- external/data/generator Reactor 聚合编译：32 个模块全部成功。
- 前端目标页面 ESLint 无错误；两份 `.ts` API 文件因当前 ESLint 未匹配 TypeScript 配置而产生 ignored warning，生产构建成功。
- `git diff --check`、Flyway 占位符、迁移敏感信息、网关禁用字段和敏感日志扫描均通过。

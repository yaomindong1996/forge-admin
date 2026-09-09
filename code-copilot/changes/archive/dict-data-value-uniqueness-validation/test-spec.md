# 单测 Spec — 字典数据键值唯一性校验
> status: done
> created: 2026-08-04

## 0. 测试原则

- 使用 Red/Green TDD：先让新增 Service 测试因缺少重复校验失败，再实现最小业务逻辑。
- 复用 `forge-plugin-system` 当前 JUnit 5 与 Mapper 动态代理测试风格，不启动 Spring 容器或真实数据库。
- 数据库并发唯一性已由现有索引保证；本轮测试聚焦业务提示、分支和 Mapper 参数。
- 所有实际命令与结果写入 `execution-log.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit Jupiter（由 `spring-boot-starter-test` 提供） |
| Mock 框架 | JDK 动态代理 Mapper + 简单测试替身 |
| 已有测试数量 | `forge-plugin-system` 当前 13 个测试源码文件 |
| 已有测试风格 | JUnit 5，直接构造 Service，使用动态代理隔离 Mapper |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名：`SysDictDataServiceImpl`

| 方法 | 场景 | 输入 | Mapper 行为 | 预期结果 |
|------|------|------|-------------|----------|
| `insertDictData` | 同字典重复键值 | `dictType=sys_status, dictValue=1` | 重复计数返回 1 | 抛 `BusinessException`，不执行 insert |
| `insertDictData` | 非重复键值 | 同上 | 重复计数返回 0，insert 返回 1 | 返回 true，执行 insert |
| `updateDictData` | 修改为其它项已有键值 | `dictCode=2` | `selectById` 返回当前项，排除 2 后计数返回 1 | 抛 `BusinessException`，不执行 update |
| `updateDictData` | 仅修改自身其它字段 | `dictCode=2` | 排除 2 后计数返回 0，update 返回 1 | 返回 true，执行 update |

### P1 — 数据访问层

- 静态检查 `SysDictDataMapper.xml`：
  - SQL 位于 Mapper XML。
  - 条件包含 `dict_type = #{dictType}`、`dict_value = #{dictValue}`、`del_flag = 0`。
  - `excludeDictCode` 非空时包含 `dict_code <> #{excludeDictCode}`。
- 插件资源编译能够解析并复制 Mapper XML。

### 不测试

- 不连接真实 MySQL：本轮无 Flyway 变更，且已有唯一索引已经在历史迁移中定义。
- 不启动 Admin 或执行浏览器 E2E：接口路径与前端没有变更，Service 单测与模块编译足以覆盖本轮风险。
- 不测试多实例并发写入：由数据库唯一索引兜底，真实并发异常映射不在本次需求范围内。

## 3. 执行计划

- [x] Step 1：运行已有 `SysConfigServiceCryptoGuardTest`，确认 JUnit 基线可用。
- [x] Step 2：生成 P0 测试并确认 Red。
- [x] Step 3：实现 Mapper/Service 并确认 Green。
- [x] Step 4：运行目标插件编译和静态检查。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-08-04 | `forge-plugin-system` JUnit 基线 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest=SysConfigServiceCryptoGuardTest -Dsurefire.failIfNoSpecifiedTests=false` | 4/4 passed，BUILD SUCCESS | JDK 17，总耗时 37.055s |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-08-04 | Service 测试 Red | 定向测试 | 与 Green 相同的 Reactor 定向测试命令 | 4 tests，4 failures，符合预期 | 失败原因为未抛重复异常、未调用唯一性查询 |
| 2026-08-04 | Mapper/Service/测试 | 定向测试与 Reactor 编译 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest=SysDictDataServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 4/4 passed，BUILD SUCCESS | JDK 17，总耗时 17.926s |
| 2026-08-04 | `forge-plugin-system` | 模块全量单测 | 在模块目录执行 `mvn test -Penable-tests` | 56/56 passed，BUILD SUCCESS | 测试夹具产生预期 WARN/异常栈，不阻断 |
| 2026-08-04 | Mapper XML / 差异 | XML 解析、关键条件、格式 | `xmllint --noout SysDictDataMapper.xml`、`rg`、`git diff --check` | passed | 用户已有 `.DS_Store` 变更未纳入本任务 |

## 6. 执行证据

- `execution-log.md`：记录实际 Maven 和静态检查输出。
- 关键接口：不启动服务，未执行 HTTP 调用。
- 关键数据库检查：复用 `V1.0.51` 中现有唯一索引定义，未执行真实数据库查询。
- 服务启动与停止：本轮不启动服务。

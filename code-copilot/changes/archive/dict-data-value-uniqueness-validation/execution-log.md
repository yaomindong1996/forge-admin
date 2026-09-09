# 执行日志 — 字典数据键值唯一性校验

## 1. 环境

- 日期：2026-08-04
- 分支：`main`
- 仓库：`/Users/yaomindong/Desktop/project/mdframe/forge-project`
- 后端：Java 17 / Maven 3.9.3 / JUnit 5
- 本轮不启动服务、不连接真实数据库。

## 2. 记录

| 时间 | 阶段 | 操作 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-08-04 | Research | 核对字典新增/修改 Service、Mapper、前端必填规则和逻辑删除唯一索引 | passed | `V1.0.51` 已存在 `(tenant_id, dict_type, dict_value, del_flag)` 唯一索引 |
| 2026-08-04 | Baseline | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest=SysConfigServiceCryptoGuardTest -Dsurefire.failIfNoSpecifiedTests=false` | passed，4/4，BUILD SUCCESS | JDK 17，总耗时 37.055s；现有编译弃用/unchecked 警告不阻断 |
| 2026-08-04 | Proposal | 创建本变更四份 SDD 文档 | passed | 状态进入 apply，生产代码尚未修改 |
| 2026-08-04 | TDD Red | 新增 `SysDictDataServiceImplTest` 后运行定向 Reactor 测试 | expected failure，4/4 failed | 重复场景未抛异常，唯一性查询未发生；总耗时 21.164s |
| 2026-08-04 | Apply | 新增 Mapper XML 有效记录计数查询，并在新增/修改前统一校验 | completed | 修改时通过 `excludeDictCode` 排除自身，重复时抛 `BusinessException` |
| 2026-08-04 | TDD Green | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest=SysDictDataServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false` | passed，4/4，BUILD SUCCESS | JDK 17，总耗时 17.926s |
| 2026-08-04 | Regression | 在 `forge-plugin-system` 模块执行 `mvn test -Penable-tests` | passed，56/56，BUILD SUCCESS | 总耗时 8.610s；短信异常、RSA 解码等 WARN/栈为测试预期输出 |
| 2026-08-04 | Static | `xmllint --noout SysDictDataMapper.xml`，`rg` 检查类型/键值/逻辑删除/排除自身条件 | passed | Mapper XML 资源随模块构建复制，共 25 个资源 |
| 2026-08-04 | Final Check | `git diff --check`、目标新增文件尾随空白扫描、`git status --short` | passed | Service 历史文件仍有既有空行空白；本轮新增行和文件无新增格式错误 |

## 3. 验证结论

- 新增和修改均在写入前按 `dictType + dictValue` 查询有效记录。
- 修改传入当前 `dictCode`，不会把自身判为重复。
- 重复时返回稳定业务文案，非重复路径保持写入与缓存事件行为。
- 现有数据库唯一索引未修改，继续承担并发最终防线。

## 4. 跳过项与服务清理

- 真实 MySQL：本轮无数据库结构变更，未连接；现有唯一索引通过迁移源码核对。
- Admin/API E2E：接口协议未变，未启动；业务异常响应由现有全局异常处理链路负责。
- 本轮启动服务：无，因此无 PID 需要清理。

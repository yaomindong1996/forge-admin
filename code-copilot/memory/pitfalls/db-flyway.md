# 踩坑：数据库 / Flyway / 索引

> 从 `code-copilot/memory/pitfalls.md` 按主题拆出。新条目追加到本文件。共 13 条。

## 15. Flyway 已执行版本脚本不能二次修改


**发现日期**: 2026-05-19

**问题描述**:
应用启动时报错：

```text
FlywayValidateException: Migration checksum mismatch for migration version 1.0.1
```

典型表现是 `forge_schema_history` 中已经记录了 `V1.0.1` 的 checksum，但本地 `forge/db/migration/V1.0.1__*.sql` 又被修改，Flyway 在执行新版本前先做校验，因此后续 `V1.0.2` 不会继续执行。

**根本原因**:
Flyway 的版本化 migration 是不可变变更记录。脚本一旦在任意数据库执行成功并写入 `forge_schema_history`，后续改文件内容就会造成数据库记录的 checksum 与本地解析出的 checksum 不一致。

**解决方案**:
- 已执行过的 `Vx.y.z__*.sql` 禁止继续编辑。
- 需要修正表结构、菜单、初始化数据时，新增更高版本脚本，例如 `V1.0.3__fix_dashboard_version_menu.sql`。
- 如果确实要把当前本地脚本作为数据库认可版本，必须明确确认数据库现状无误后执行 `flyway repair`，或等价更新 `forge_schema_history`，这是数据库操作，不能作为常规开发手段。
- 排查“新脚本不执行”时，先看启动日志是否有 `Validate failed`，它通常说明卡在旧版本校验，不是 `locations` 没扫到新脚本。

**影响范围**:
- `forge/db/migration` 下所有 Flyway 版本化 SQL
- 所有启动时依赖 `forge-admin-server` 自动迁移的本地、测试、生产数据库

## 23. 菜单活跃项函数签名不一致导致选中状态停留


**发现日期**: 2026-05-27

**问题描述**:
在 `top-side-menu` 布局下，先点击“应用总览”，再点击同级的“引擎中心 / 移动端中心 / 集成中心”，页面可以跳转，但菜单选中状态仍停留在“应用总览”。

**根本原因**:
`useMenu()` 返回的 `findMenuIdByPath` 只支持 `(targetPath)`，内部固定从全量菜单查找；`top-side-menu/components/SideMenu.vue` 按 `(sideMenuOptions, route.path)` 调用。第二个参数被忽略后，活跃 key 算不出真实菜单 ID，Naive Menu 受控 `value` 失效，视觉上保留上一次选中项。

同时，数据库里的顶层“应用中心”目录如果残留 `/app-center` 路径、`app-center/index` 组件或 `ai:businessApp:list` 权限，会和子级“应用总览”语义重叠，增加 `/app-center` 抢占选中的概率。

**解决方案**:
- `useMenu()` 暴露的 `findMenuIdByPath` 兼容 `(targetPath)` 和 `(items, targetPath)` 两种调用方式。
- 路径匹配先用 `normalizeLocalPath()` 统一前导斜杠，空路径不参与匹配。
- 新增 Flyway 迁移把顶层“应用中心”规范为纯目录：`path/component/perms = NULL`；真正页面入口保留在子菜单“应用总览”。

**影响范围**:
- `normal`、`top-side-menu`、`immersive` 等依赖 `useMenu()` 的菜单布局
- 应用中心这类“目录 + 默认页 + 同级子页”的菜单结构

## 26. Flyway 已执行版本禁止复用或改写


**发现日期**: 2026-05-28

**问题描述**:
启动 `forge-admin-server` 时，Flyway 校验失败：
```text
Migration checksum mismatch for migration version 1.0.32
Migration checksum mismatch for migration version 1.0.33
```

**根本原因**:
数据库 `forge_schema_history` 已经记录过对应版本的 checksum，但本地 `forge/db/migration/V1.0.32__*.sql` 或 `V1.0.33__*.sql` 内容后来被修改、重新生成或复用了同一个版本号。Flyway 会把它视为历史迁移被篡改，启动阶段直接失败。

**解决方案**:
- 已落库的 Flyway 脚本禁止修改，新增修正必须使用下一个版本号。
- 本地开发库如果确认为临时脚本迭代，可删除从首次变更版本开始的连续尾部 `forge_schema_history` 记录后重跑；不要只删除中间版本。
- `flyway repair` 只更新 schema history checksum，不会重新执行脚本；如果脚本内容包含新的 seed/update 数据，优先重跑迁移或新增后续版本脚本。

**影响范围**:
- 所有 `forge/db/migration/V*.sql` 版本化脚本
- 远程共享开发库、测试库和生产库的 Flyway 启动校验

## 43. Flyway 迁移 tenant_id 0 归一化前必须先处理唯一键重复


**发现日期**: 2026-06-04

**问题描述**:
多租户迁移中把历史 `tenant_id = 0` 统一改为默认租户 `1` 时，开发库启动失败：

```text
Validate failed: Detected failed migration to version 1.0.56
Duplicate entry '1-sys_notice_status' for key 'sys_dict_type.uk_tenant_dict_type'
```

**根本原因**:
`sys_dict_type` 同时存在 tenant 0 和 tenant 1 的同名 `dict_type`，直接执行 `UPDATE ... SET tenant_id = 1` 会撞上租户内唯一键。失败后 Flyway 会保留 `success = 0` 的历史记录，后续启动会先卡在 validation。

**解决方案**:
1. 确认失败迁移尚未成功落库，先删除或 repair 对应的失败 history 行。
2. 在迁移脚本的归一化语句前增加去重逻辑，删除 tenant 0/null 中已被 tenant 1 覆盖的重复字典类型和字典数据。
3. 重新通过应用启动或 Flyway migrate 执行迁移，让 `forge_schema_history` 正常记录 `success = 1`。

**影响范围**:
- 所有把 `tenant_id IS NULL OR tenant_id = 0` 回填到默认租户 `1` 的 Flyway 脚本。
- 带租户内唯一键的表，例如字典、角色编码、流程 key、业务自然键等。

## 55. Flyway 低版本补脚本会被默认校验拦截


**发现日期**: 2026-06-06

**问题描述**:
启动时报 `jobAutoRegistrar -> jobScheduler -> flywayInitializer` 依赖创建失败，真正根因是 Flyway 校验失败：

```text
Detected resolved migration not applied to database: 1.0.55
Detected resolved migration not applied to database: 1.0.56
```

**根本原因**:
开发库 `forge_schema_history` 已经存在更高版本，例如 `1.0.57/1.0.58`，但本地后来新增或恢复了更低版本的迁移脚本。Flyway 默认 `outOfOrder=false`，会拒绝这种低版本补迁移。

**解决方案**:
先查 `forge_schema_history` 确认缺失版本和已执行高版本；再确认缺失脚本具备重复执行保护。对已经出现历史缺口的开发库，用 Flyway `outOfOrder=true` 正式补跑一次迁移，让历史表记录缺失版本。补跑后再用默认配置执行 Flyway validate，确认 `validationSuccessful=true`。

**注意**:
不要手工插入 `forge_schema_history`，不要修改已经执行过的迁移脚本。后续新增迁移必须继续按当前最高版本顺延，不能再补低版本脚本。

## 91. MySQL 唯一索引遇到 NULL 不能作为幂等防线


**发现日期**: 2026-07-03

**问题描述**:
动作执行日志用 `tenant_id + object_code + record_id + action_code + idempotency_key` 做唯一键防重复提交时，如果 `record_id` 允许为 `NULL`，MySQL 唯一索引会允许多条 `NULL` 组合记录，导致同一幂等键仍可能重复写入和重复执行。

**解决方案**:
- 幂等唯一键中的业务维度字段尽量设为 `NOT NULL DEFAULT ''`，服务端也要把空值归一化为空字符串。
- 执行业务副作用前先以独立事务写入 `RUNNING` 预占日志，再进入真实步骤事务。
- 重复请求命中 `RUNNING`、`FAILED` 等既有幂等记录时只返回/抛出对应结果，不要再尝试写一条新的失败日志。

**影响范围**:
- 动作执行日志、数量流水、锁定记录等所有依赖数据库唯一键实现幂等的表。
- 任何包含可空业务 ID、详情 ID、来源 ID 的唯一索引设计。

## 126. Flyway 替换旧索引时不能假设历史索引仍存在


**发现日期**: 2026-07-23

**问题描述**:
逻辑删除迁移根据早期 Flyway 清单无条件执行 `DROP INDEX uk_xxx_active`，但后续迁移可能已经删除、改名或替换该索引。真实库仍有待迁移生成列，却没有预期索引名时，MySQL 会报 `Can't DROP ...; check that column/key exists`；如果只跳过缺失索引，还可能遗漏后续迁移留下的永久唯一索引，继续阻止逻辑删除后重建。

**解决方案**:
- 扫描目标索引完整迁移历史，不能只读取首次创建脚本。
- 执行前查询 `information_schema.STATISTICS`，动态拼接实际存在的 `DROP INDEX` 子句。
- 将实际旧索引删除、生成列删除和目标唯一索引创建放在同一个 `ALTER TABLE` 中，避免表级迁移留下无唯一约束窗口。
- 对已知语义替代索引显式加入候选清单，例如 `ai_code_rule.uk_ai_code_rule_code`。
- MySQL 非事务 DDL 迁移失败后，修复脚本前先确认 `flyway_schema_history` 失败记录；执行 Flyway repair 后再重跑幂等迁移。

**影响范围**:
- 所有修改、重建或重命名存量索引/约束的 Flyway 迁移。
- 所有可能跨多个历史版本、手工修复或部分执行状态升级的数据库环境。

## 146. 字典请求失败结果不能写入全局缓存


**发现日期**: 2026-08-02

**问题描述**:
`useDict` 捕获请求异常后返回空数组，而缓存层无法区分“合法空字典”和“加载失败”，会把空数组长期缓存。之后同一 SPA 内所有页面都直接读取空缓存，`DictTag` 和下拉框退化为英文/原始值，只有整页刷新才能恢复。

**解决方案**:
- 只有服务端成功响应才缓存字典结果；请求失败必须保持为失败状态，不得缓存空数组。
- 多字典并发加载使用 `Promise.allSettled` 逐项更新，一个字典失败不能清空或阻断其它成功字典。
- 首次加载可对失败项做一次受控重试，手动刷新时保留已成功数据；业务提交依赖关键字典时，在字典未就绪前禁用提交。
- 缺失的业务枚举仍必须通过 Flyway 写入 `sys_dict_type/sys_dict_data`，不能用前端硬编码掩盖。

**影响范围**:
- 公共 `useDict`、`DictTag`、`DictSelect` 及所有依赖系统字典的页面。
- 登录态初始化、后端重启、网络瞬时失败和多字典并发加载场景。

## 163. 删除业务应用后保留的停用入口不能永久阻断业务域删除


**发现日期**：2026-08-05

**问题描述**：
业务应用删除时，为保留可迁移的设计资产，已停用访问入口只解除 `application_id` 而不删除。业务域删除若继续按 `suite_code` 统计所有入口并无条件阻断，会导致“已无业务应用，但永远无法删除业务域”的死路。

**解决方案**：
- 有效业务应用仍优先阻断业务域删除。
- 仅剩入口和对象元数据时，前端展示具体数量并传递显式清理确认；后端不信任前端统计，在事务内重新计数。
- 清理前 JOIN 未删除业务应用校验 `application_id`，防御跨域或历史脏绑定；任何有效引用都必须失败关闭。
- 所有入口/对象引用校验完成后才产生副作用；先停用入口菜单，再用 `status=0, del_flag=id` 逻辑删除入口，不物理删除菜单资源或历史数据。
- 为保持旧调用方兼容，可保留原查询参数名，但应在 Spec 中明确语义已扩展。

**影响范围**：
- 所有采用“删除聚合时保留子资产供迁移”语义的业务域、应用、菜单入口和设计态元数据。

## 179. 逻辑删除业务键回填去重必须与最终唯一索引使用相同维度


**发现日期**：2026-08-18

数值主键墓碑逻辑删除使用 `UNIQUE (tenant_id, 业务键, del_flag)` 时，迁移回填新业务键后若只按 `(tenant_id, 业务键)` 去重，会把“已删除历史 + 当前有效记录”误判为冲突。若历史行 ID 更小，去重脚本甚至会保留历史值、改写当前有效行，导致正式访问地址等业务标识意外变化。

处理原则：存量冲突检测、`GROUP BY`、回写 JOIN 和最终唯一索引必须使用完全相同的键维度；主键墓碑模式下包含 `del_flag`。有效行之间的真实冲突仍需确定性改名，删除历史与有效行可以保留相同业务键。迁移静态审查应同时核对去重维度、JOIN 条件和索引列顺序。

## 企业协同连接根的 client_id/client_secret 不能继续 NOT NULL

**发现日期**: 2026-08-31

**问题描述**:
企业协同「新建连接」报错，根因是 `sys_social_config.client_id`、`client_secret` 仍为 `NOT NULL`。连接升级为多应用模型后，新增连接不再填写这两列；MyBatis-Plus 省略 null 字段，MySQL 严格模式就会拒绝插入。

**解决方案**:
- 用新的 Flyway 脚本把这两列改为 `DEFAULT NULL`，已执行脚本不要回改。
- 连接保存 DTO 继续不透传凭据；应用 ID/Secret 只写 `sys_social_app_config`。
- 登录解析可以继续「应用优先、连接回退」，但新连接必须依赖应用凭据。

## 共享库已执行的高版本 Flyway 脚本必须原样出现在当前分支

**发现日期**: 2026-09-20

**问题描述**:
同事分支已把共享开发库迁到 `V1.0.174`，本分支没有 `V1.0.171`–`V1.0.174` 文件。启动报 `Validate failed: Detected applied migration not resolved locally: 1.0.171`（以及 172–174）。`jobAutoRegistrar` 只是后续连锁失败。

**解决方案**:
- Flyway 校验要求：库里已经成功执行的每个版本，当前工作区都必须有同版本、同内容的脚本。
- 共享库被别的分支跑到更高版本时，把对方的 `V1.0.171`–`V1.0.174` 原样放到本分支；本需求新脚本用下一个空号（如 `V1.0.175`）。
- 不要把本需求占用已存在的 `1.0.171`，也不要用 `flyway repair` 把别人已执行的版本标成删除。
- 排查时先对照 `forge_schema_history` 和 `forge-server/db/migration/V*`，缺文件就补文件，不要改已落库脚本。

## 模板库清理必须在 Flyway 增量之后执行

**发现日期**: 2026-09-30

**问题描述**:
全量 SQL 里的 `forge_schema_history` 是空的，admin 启动时会重跑所有迁移。V1.0.105（预售应用和 `ps_presale_*` 表）、V1.0.114（预售流程）、V1.0.158（Gitee 租户）、V1.0.107（外部接口示例）会写入演示数据。先清理再迁移，这些数据会被重新写回来。

**解决方案**:
- 固定顺序为：全量 SQL → required seed → Flyway → `clean-db.sh`。`clean-db.sh` 在增量没有全部成功执行时拒绝运行。
- 有 Java 实体或 Mapper 引用的演示表（`sample_purchase_order`、`biz_leave_request` 等）只能 TRUNCATE，DROP 后服务启动或访问会报表不存在。

## macOS bash 3.2 会把变量后紧跟的中文字符当成变量名

**发现日期**: 2026-09-30

**问题描述**:
`init-db.sh` 里的 `"重建数据库 $DATABASE，原数据..."` 在 macOS `/bin/bash` 3.2（UTF-8 locale）下报 `DATABASE�: unbound variable`，脚本在确认提示处直接退出。

**解决方案**:
- 变量后面直接跟非 ASCII 字符时一律写 `${VAR}`。可以用 `LC_ALL=C grep -nE '\$[A-Za-z_][A-Za-z0-9_]*[^ -~]' *.sh` 检查。
- 同时避免使用 bash 4 语法：关联数组、`mapfile`、`${var,,}`。`set -u` 下空数组展开前要先判断长度。

## docker 初始化 SQL 与全量 SQL 漂移

**发现日期**: 2026-09-30

**问题描述**:
`docker-forge-admin/init-sql/01-init.sql` 与 `forge-server/db/全量初始化SQL.sql` 相差 677 行：
- 混入了其它生成项目的包名 `com.craken`（Quartz 任务类、数据权限 Mapper 方法、流程服务类）；
- Quartz 用小写 `qrtz_*` 表，而配置的 `tablePrefix: QRTZ_` 是大写。Linux 上 MySQL 表名区分大小写，docker 部署后 Quartz 会找不到表，已保留的任务类也不存在。

**解决方案**:
- docker 的初始化 SQL 必须与全量 SQL 字节一致；更新全量 SQL 后执行 `cp forge-server/db/全量初始化SQL.sql docker-forge-admin/init-sql/01-init.sql`。
- `forge:create` 生成项目时直接用生成后的全量 SQL 覆盖 docker 的 `01-init.sql`。
- 不要从改过名的生成项目反向拷贝 SQL 回模板仓库。

## 隐藏业务路由不能直接升级为独立功能菜单

**发现日期**: 2026-09-30

**问题描述**:
打印页面最初作为“应用总览”的隐藏子路由创建。后续建设独立打印中心时，只把该资源改名并设为可见，导致打印中心仍挂在应用总览下面；动态菜单同时把应用总览当成父级，最终出现原入口消失、菜单层级错乱和多个功能挤在一个页面的问题。

**解决方案**:
- 独立业务模块必须新建顶层目录资源，再建立真实的功能子菜单，不能只把其它模块的隐藏路由设为可见。
- 复用旧路由资源时要显式迁移 `parent_id`、`path`、`component`、`perms` 和角色资源关系，同时恢复原业务入口的名称、路径和父级。
- 设计器、预览等跳转页继续保留为隐藏路由，不要伪装成侧边栏菜单。
- 新目录和菜单只继承角色已有的业务权限，不能借菜单修复给无权限角色扩权。

## 完整租户主题 JSON 不能继续存入 varchar(1000)

**发现日期**: 2026-10-05

**问题描述**:
主题配置增加多布局、明暗分组和自动配色后，完整默认 JSON 已达 2108 字符，但 sys_tenant.theme_config
仍为 varchar(1000)。租户编辑触发 `Data too long for column 'theme_config'`；不能通过截断 JSON 或删除未知字段规避。

**解决方案**:
- 用新 Flyway 版本扩为 TEXT，同步全量和 Docker 初始化 SQL，保留现有 String 接口协议和完整配置。
- 迁移限定当前数据库与目标列，保留字符集/排序规则；重复执行和已有更大文本/JSON 列时不缩列。
- 仅提交代码不会修复已部署数据库；确认迁移成功后再验证真实保存。应用回滚不要求缩列。

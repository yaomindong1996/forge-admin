# 单测 Spec — 编码规则配置优化
> status: reviewed_passed
> created: 2026-07-16

## 0. 测试原则

- 按 Red/Green TDD 实现序列底座、进制转换、规则引擎和前端纯函数。
- 先执行低成本静态检查和定向单测，再执行 Generator 聚合编译与前端生产构建。
- 不把“编号不重复”等同于“编号连续”；分别验证唯一性、单实例连续边界和允许空洞的契约。
- 默认不启动真实 Admin/MySQL/Redis；Flyway 实跑和 HTTP 加密链路由可用环境补充。
- 所有命令、关键输出、警告和跳过项追加到 `execution-log.md`。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 后端 | JUnit 5 + Mockito；Maven `enable-tests` profile |
| 前端 | Vitest；Node v20.19.0 |
| SQL/XML | `xmllint`、`rg` 静态契约检查 |
| 构建 | Java 17 Admin reactor；Vite production build |

## 2. 覆盖范围

### P0 — 核心业务逻辑

| 类/模块 | 场景 | 输入 | 预期结果 |
|---------|------|------|----------|
| `SegmentSequenceGenerator` | 新 key 起始值 | start=25 | 首值 25 |
| `SegmentSequenceGenerator` | 跨号段 | 连续取 1200 个 | 无重复且单实例严格连续 |
| `SegmentSequenceGenerator` | 并发取号 | 8 线程各 500 个 | 4000 个唯一值 |
| `CodeRuleRadixCodec` | 五种进制 | 边界值和长度 | 固定宽度结果正确 |
| `CodeRuleRadixCodec` | 易混淆字符 | I/O/Z 前后边界 | 开启后字符集不含 I/O/Z |
| `CodeRuleRadixCodec` | 溢出 | value=capacity | 抛业务异常，不截断 |
| `CodeRuleEngine` | 五种段组合 | 日期、固定、序列、变量、系统变量 | 拼接顺序和逐段结果正确 |
| `CodeRuleEngine` | 多分组段 | 两个不同组合 | groupHash 不同且 key 长度受控 |
| `CodeRuleEngine` | SYS_VAR 防伪造 | fields 含 tenantId/userId | 使用可信 Session 值或失败关闭 |
| `CodeRuleEngine` | 预览 | 任意规则 | 不调用序列服务 |
| `LegacyCodeRuleParser` | 旧内置模板 | 11 条旧规则 | 生成等价分段 |
| `LegacyCodeRuleParser` | 不可解析模板 | 未知 token | 保留 legacy 警告，不丢数据 |

### P1 — 数据访问与事务

- `CodeRuleMapper.xml` 分页、详情、唯一校验显式过滤 `tenant_id` 和 `del_flag`。
- `CodeRuleSegmentMapper.xml` 按 `segment_order,id` 稳定排序，逻辑删除和批量插入在同一事务中。
- Flyway 新表包含标准审计字段、逻辑删除字段和有效记录唯一索引。
- 字典、菜单、角色权限和内置分段插入均具备 `NOT EXISTS` 防重复。
- 更新版本冲突、内置规则非法字段修改、分段保存失败均回滚主表修改。

### P2 — 入口与集成层

- `/system/code-rule` DTO 使用 JSR303 校验，方法具备独立权限和加解密注解。
- `/ai/code-rule/list|preview|generate` 对旧请求字段保持兼容。
- `DynamicCrudService` 将 recordData 作为 VARIABLE fields 传入，不允许其覆盖 SYS_VAR。
- 前端工具函数覆盖新增段、类型切换清理、重排、SEQ 唯一和总长度校验。
- 前端生产构建覆盖编码规则列表、全屏编辑工作台和 ForgePropertyPanel。

### 不测试

- 不验证无空洞连续编号；数据库号段预分配天然允许空洞。
- 不执行真实跨节点并发和数据库故障恢复；需要共享测试环境。
- 不自动修改或启动用户本地数据库，Flyway 实跑由用户环境补充。

## 3. 执行计划

- [x] Step 1：记录当前定向测试基线。
- [x] Step 2：序列底座测试先 Red，再修复为 Green。
- [x] Step 3：进制、分组和 legacy parser 测试先 Red，再实现为 Green。
- [x] Step 4：服务/Controller/低代码兼容测试。
- [x] Step 5：前端纯函数 Vitest 和 production build。
- [x] Step 6：Mapper XML、Flyway placeholder、尾随空白和 Admin 聚合构建。
- [x] Step 7：回填执行证据和两阶段审查结论。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-16 | 需求分析 | 只读检查现有 CodeRule/Sequence/UI/Flyway | passed | 未执行构建或运行服务 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-16 | SDD 文档及全部差异 | tracked/untracked diff check | `git diff --check`；untracked 文件逐项 `git diff --no-index --check` | passed | 28 个 untracked 文件已覆盖 |
| 2026-07-16 | 序列底座 | JUnit | `mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-id -Dtest=SegmentSequenceGeneratorTest test` | passed，3/3 | 首次 Red 为缺少起始值接口；首次 Green 被 Mockito attach 环境限制阻断，改用 JDK Proxy 后通过 |
| 2026-07-16 | 结构化引擎、legacy、Controller、低代码 | JUnit | Generator 模块 `mvn -Penable-tests -Dtest=CodeRuleEngineTest,LegacyCodeRuleParserTest,DynamicCrudServiceAutoGenerationTest,SystemCodeRuleControllerContractTest test` | passed，10/10 | 聚合定向测试先被上游 datascope 测试引擎配置拦截，改为目标模块直跑后通过 |
| 2026-07-16 | 前端工具函数 | Vitest | `pnpm exec vitest run src/views/app-center/__tests__/code-rule-utils.spec.js` | passed，6/6 | Node v20.19.0 |
| 2026-07-16 | 前端页面与组件 | production build | `NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | passed，8691 modules | 仅仓库既有动态/静态导入、CSS `//` 注释和组件同名警告 |
| 2026-07-16 | 主应用装配 | Admin reactor compile | `mvn -pl forge-admin-server -am compile -DskipTests` | passed，42/42 | Java 17；使用临时只读镜像 Maven 仓库配置 |
| 2026-07-16 | Mapper/Flyway | XML 与静态检查 | `xmllint --noout ...`、`rg -n '\$\{[^}]+\}' forge-server/db/migration` | passed | placeholder 检查无输出；补验主表逻辑删除字段与唯一索引 |
| 2026-07-16 | 真实环境 | Flyway/HTTP | 未执行 | skipped | 未启动 Admin/MySQL/Redis，按既定分工由可用环境补充 |

## 6. 执行证据

- `execution-log.md`：同目录增量追加。
- 关键接口：静态协议 + 条件允许时本地 HTTP 冒烟。
- 关键数据库检查：Flyway 静态检查；真实迁移结果由用户环境回填。
- 服务启动与停止：默认不启动；如启动则记录 PID 并只清理本轮服务。

## 7. Review 增量测试缺口（2026-07-16）

- 现有并发测试只共享一个 `SegmentSequenceGenerator`，不能覆盖两个实例在 UPDATE 与 SELECT 之间交错导致的重复号段。
- 缺少外层业务事务回滚后，数据库 `max_id` 与 JVM `SegmentHolder` 一致性的集成测试。
- 缺少带旧 `code-rule:*` 计数器的升级场景，未验证新引擎首次生成值大于历史已分配水位。
- 缺少 Flyway 在新增 `category` 字段后中断、修复后重跑的幂等测试。

## 8. Fix 增量验证计划（2026-07-16）

- [x] Red：Starter ID 测试因缺少独立事务构造器、旧水位参数和查询契约而编译失败。
- [x] Red：Generator 测试因缺少旧 key 前缀/周期传递契约而编译失败。
- [x] Red：前端 Vitest 因缺少过期响应 Guard 和独立权限判断函数而失败 2 项。
- [x] Green：Starter ID 覆盖旧水位首号、多实例交错区间和 `REQUIRES_NEW` 传播。
- [x] Green：Generator 覆盖新 key 向序列服务传递旧规则前缀/周期及 Flyway 分类幂等契约。
- [x] Green：前端纯函数覆盖过期响应失效和新增/编辑/删除独立权限。
- [x] 回归：Starter ID 6/6、Generator 编码规则相关测试 11/11 全部通过。
- [x] 静态：三个 Mapper XML、Flyway placeholder、迁移幂等断言和全部 tracked/untracked 差异格式通过。
- [x] 聚合：Admin reactor 42/42 编译通过；前端生产构建 8691 modules、exit 0。

## 9. Fix 验证结果（2026-07-16）

| 范围 | 结果 | 关键证据 |
|------|------|----------|
| Starter ID | passed，6/6 | 覆盖旧水位首号、两个生成器交错分配、本实例快照区间和 `REQUIRES_NEW` 提交契约 |
| Generator | passed，11/11 | Engine 4、migration contract 1、legacy parser 2、Dynamic CRUD 3、Controller contract 1 |
| 前端工具函数 | passed，8/8 | 新增 latest request guard 与独立权限判断用例 |
| 前端生产构建 | passed | Node v20.19.0；8691 modules；`exit 0`，仅仓库既有非阻断警告 |
| Admin 聚合编译 | passed，42/42 | Java 17；Starter ID、Generator、Admin 均成功 |
| XML/Flyway/差异 | passed | 三个 Mapper XML；placeholder 和 `@category_added` 扫描无输出；tracked/untracked 格式检查无输出 |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未实跑 Flyway 和登录态 HTTP |

## 10. 用户验收主题复修（2026-07-16）

| 检查 | 结果 | 证据 |
|------|------|------|
| 主题变量静态检查 | passed | 编码规则三个 Vue 文件不再使用 `--body-color`、`--n-color`、`--n-action-color` 等页面背景变量 |
| 定向 ESLint | passed | `code-rules.vue`、`CodeRuleEditorWorkspace.vue`、`CodeRuleSegmentEditor.vue` 0 errors |
| 前端回归 | passed，8/8 | `code-rule-utils.spec.js` |
| 生产构建 | passed | 8691 modules，exit 0；仅仓库既有非阻断警告 |

## 11. 用户验收编辑工作台复修（2026-07-16）

| 检查 | 结果 | 证据 |
|------|------|------|
| 抽屉移除契约 | passed | 编码规则入口和工作台无 `n-drawer`、`show/update:show` 或旧 Drawer 引用 |
| 同路由权限边界 | passed | 使用 `/app-center/code-rules?editor=create|edit` 切换视图，仍由 add/edit 独立权限控制，不新增白名单路由 |
| 操作列可见性 | passed | 分段表头“操作”和行内按钮均使用 `position: sticky; right: 0` |
| 定向 ESLint | passed | 三个相关 Vue 文件 0 errors/warnings |
| 前端回归 | passed，8/8 | `code-rule-utils.spec.js` |
| 生产构建 | passed | 8691 modules，exit 0；仅仓库既有非阻断警告 |

## 12. 低代码字段映射增量验证计划（2026-07-16）

- [x] Generator：场景/对象筛选 Mapper 契约、运行时对象匹配、Controller capability 签名和 Dynamic CRUD `objectCode` 上下文。
- [x] 前端：VARIABLE 必须绑定来源对象，场景字典、业务对象/字段下拉和接口参数通过 ESLint/Vitest。
- [x] SQL/XML：来源对象字段先补列后建索引，场景字典 tenant_id=1 且防重复，Mapper XML 语法正确。
- [x] 聚合：Generator 定向测试、Admin 42 模块编译和前端生产构建。
- [x] 真实环境：不启动 Admin/MySQL/Redis，不实跑 Flyway/HTTP，继续由可用环境补验。

| 范围 | 结果 | 证据 |
|------|------|------|
| Generator | passed，15/15 | Controller 2、migration 2、engine 4、legacy 2、Mapper 1、Dynamic CRUD 3、对象绑定 1 |
| 前端 ESLint/Vitest | passed，9/9 | 6 个相关前端文件 0 errors；VARIABLE 来源对象校验新增用例 |
| 前端生产构建 | passed | 8691 modules，`exit 0`；仅仓库既有非阻断警告 |
| Admin 聚合编译 | passed，42/42 | Java 17；Generator 与 Admin 装配成功 |
| XML/Flyway | passed | 三个 Mapper XML；placeholder 无残留；来源对象补列先于索引；场景字典 tenant_id=1 |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未实跑 Flyway 和登录态 HTTP |

## 13. VARIABLE 双来源增量验证计划（2026-07-17）

- [x] 前端纯函数：CUSTOM VARIABLE 不选择业务对象时合法；LOWCODE VARIABLE 缺少来源对象时失败；切换来源时清理 `segmentValue`。
- [x] Controller 契约：只收集 LOWCODE VARIABLE 做字段目录校验，CUSTOM 变量只验证安全标识符。
- [x] Service/引擎：纯 CUSTOM 规则无 `objectCode` 可生成；LOWCODE/混合规则继续拒绝缺失或不匹配的 `objectCode`。
- [x] Legacy/迁移：`${field:xxx}` 和未知历史变量默认 CUSTOM；Flyway 包含 `variable_source` 防重补列及已绑定存量分段 LOWCODE 回填。
- [x] 前端页面：来源方式在 VARIABLE 分段内选择，只有 LOWCODE 分段显示来源对象与字段下拉；保持 Forge 亮/暗主题 Token。
- [x] 增量构建：Generator 定向测试、ESLint/Vitest、前端 build、Admin reactor compile 和 XML/Flyway 静态检查。
- [x] 真实环境：按既定分工不启动 Admin/MySQL/Redis，不实跑 Flyway/HTTP，并在执行日志记录跳过原因。

| 范围 | 结果 | 证据 |
|------|------|------|
| Red | passed | 前端旧实现 2 项失败；后端测试编译 7 处缺少 `variableSource`/分类契约 |
| Generator | passed，20/20 | Controller 2、migration 3、engine 4、legacy 2、Mapper 2、Dynamic CRUD 3、对象绑定 4 |
| 前端 ESLint/Vitest | passed，11/11 | 4 个相关 Vue/JS 文件 0 errors；纯函数用例全部通过 |
| 前端生产构建 | passed | 8691 modules，`exit 0`；仅仓库既有非阻断警告 |
| Admin 聚合编译 | passed，42/42 | Java 17，Generator 与 Admin 装配成功 |
| XML/Flyway/差异 | passed | Mapper XML 语法、placeholder 和 `git diff --check` 通过 |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未实跑 Flyway、HTTP 和浏览器登录态验收 |

## 14. LOWCODE 弹窗映射增量验证计划（2026-07-17）

- [x] Red：新增 `applyLowCodeVariableMapping` 用例，旧实现因函数不存在而失败。
- [x] 同对象重新映射：只更新目标 LOWCODE 分段，其它分段保持。
- [x] 切换来源对象：目标分段使用新字段，其它 LOWCODE 分段清空，CUSTOM 分段不受影响。
- [x] 弹窗交互静态契约：分段组件不再渲染顶部 `segment-editor__mapping`；选择 LOWCODE 和行内重新映射均触发工作台 Modal。
- [x] 取消弹窗不改动规则草稿；确认前必须同时选中业务对象和字段。
- [x] 定向 ESLint/Vitest、前端生产构建和差异格式检查通过；不启动 Admin/MySQL/Redis。

| 范围 | 结果 | 证据 |
|------|------|------|
| Red | passed | 新增 2 个映射原子应用用例，旧实现因 `applyLowCodeVariableMapping is not a function` 失败 |
| Vitest | passed，13/13 | 同对象保留、换对象清理、CUSTOM 保留与既有双来源用例全部通过 |
| ESLint | passed | `code-rule-utils.js`、定向测试和两个编辑组件 0 errors |
| 交互静态契约 | passed | 无顶部 `segment-editor__mapping`；存在行内 `request-low-code-mapping`、工作台 `n-modal` 和原子应用函数 |
| 前端生产构建 | passed | 8691 modules，`exit 0`；仅仓库既有非阻断警告 |
| 真实页面 | skipped | 未启动 Admin/MySQL/Redis，未执行登录态浏览器点击验收 |

## 15. 归档前旧水位容量组合验证（2026-07-17）

- [x] Red：新增“旧安全起点 1001 + DECIMAL 三位”组合用例，旧实现因缺少只读旧起点契约而编译失败。
- [x] 兼容路径：实际值 1001、旧安全起点 1001 时输出 `1001`，避免水位回退和立即溢出。
- [x] 兼容边界：旧安全起点 1001 只扩宽到四位，实际值 10000 时仍抛出容量异常。
- [x] 严格路径：没有旧水位的新规则在三位实际值 1000 时继续抛出容量异常。
- [x] 回归：Starter ID、Generator 编码规则测试和 Admin 聚合编译。

| 范围 | 结果 | 证据 |
|------|------|------|
| Red | passed | `CodeRuleEngineTest` 新契约在旧接口下 testCompile 失败 |
| Starter ID | passed，7/7 | 起始值、跨段、并发、多实例、旧水位只读解析和 `REQUIRES_NEW` |
| Generator | passed，21/21 | Engine 5，其余编码规则/低代码回归 16 |
| Admin 聚合编译 | passed，42/42 | Starter ID、Generator 与 Admin 装配成功 |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未执行真实 Flyway、HTTP 或浏览器验收 |

## 16. 最终 Code Quality Fix 回归（2026-07-17）

- [x] 兼容宽度缓存：历史扩宽和无旧水位负结果均按规则/分段/周期/进制配置缓存。
- [x] 号段缓存：高基数 key 后缓存规模不超过 10000；淘汰旧 holder 后从数据库新号段继续取号且不重复。
- [x] 事务隔离：`REQUIRES_NEW` 同时显式使用 `READ_COMMITTED`。
- [x] legacy 协议：单独 `HHmmss` + AUTO 保持 NONE/all；旧 all 水位续接。
- [x] 字段协议：VARIABLE 双向 snake/camel alias；SYS_VAR 拒绝别名业务值。
- [x] 计数器身份：ruleCode 查询包含逻辑删除历史且数据库永久唯一；已有 SEQ 删除/替换拒绝，排序/属性编辑和首次添加允许。

| 范围 | 结果 | 证据 |
|------|------|------|
| Starter ID | passed，9/9 | 高基数缓存、淘汰唯一性、READ_COMMITTED、旧 all 水位及既有回归 |
| Generator | passed，25/25 | Engine 6、legacy 2、migration 4、Mapper 3、Controller 2、Dynamic CRUD 3、对象/SEQ identity 5 |
| Admin 聚合编译 | passed，42/42 | 新 Caffeine 依赖、Starter ID、Generator 与 Admin 装配成功 |
| 前端基线 | reused | Vitest 13/13、定向 ESLint 0 errors、生产构建 8691 modules |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未实跑 Flyway、HTTP 或浏览器登录态验收 |

## 17. 归档验收（2026-07-17）

- [x] 四份 SDD 文档状态、最终 Review 结论和跳过项一致。
- [x] `git diff --check` 通过。
- [x] `CodeRuleMapper.xml`、`CodeRuleSegmentMapper.xml`、`SysIdSequenceMapper.xml` 通过 `xmllint --noout`。
- [x] `V1.0.36__add_structured_code_rule_segments.sql` 无 Flyway `${...}` placeholder。
- [x] 本轮仅调整归档文档与目录，不重复执行已经通过的后端、前端测试和构建。
- [x] 本轮未启动服务，无需清理 PID。

## 18. 归档后性能修复增量验证计划（2026-07-17）

### P0

- [x] 有限容量号段在 JVM 重启等价缓存淘汰后不跳过整个 1000 区间。
- [x] 分配区间不得超过当前进制/宽度最大值，失败时数据库水位不推进。
- [x] `/sequence` API 默认关闭、仅 POST、具备专用权限和 `bizKey` 边界。

### P1

- [x] 新建结构化规则不查询 legacy 水位；迁移规则继续安全续接。
- [x] legacy SQL 使用可索引的转义前缀 LIKE；下划线规则编码不扩大匹配范围。
- [x] 同版本规则分段只加载一次，版本变化后重新加载且并发调用不共享可变 DTO。
- [x] 真实生成不构造预览专属字段；规则分段和上下文字段数量有硬上限。
- [x] 热门 key 的当前号段通过 CAS 并发消费，只有段切换进入同步区。

### P2

- [x] 前端新预览发起时取消旧请求；取消不弹错误、不覆盖新响应。
- [x] 自动预览接口不写操作日志，真实生成继续保留审计。

### 增量命令

- Starter ID：`SegmentSequenceGeneratorTest,SequenceControllerContractTest`。
- Generator：编码规则 Engine、Service cache、Mapper、Migration、Controller、Dynamic CRUD 全集。
- 前端：`code-rule-utils.spec.js`、相关 API/工作台 ESLint 和生产构建。
- 静态：三个 Mapper `xmllint`、V1.0.36/V1.0.37 placeholder、`git diff --check`。
- 真实 Flyway、MySQL/Redis、登录态 HTTP 和浏览器点击继续按既定分工跳过。

## 19. 归档后性能 Fix 验证结果（2026-07-17）

| 范围 | 结果 | 关键证据 |
|------|------|----------|
| Red / Task 15-16 | passed | 六参数 `nextId` 缺失导致 Starter ID testCompile 失败；新增容量、缓存淘汰、API 默认关闭/POST/权限/边界契约可识别旧实现 |
| Red / Task 17 | passed | `SysIdSequenceMapperContractTest` 在旧 `LEFT(biz_key,...)` 查询下 1 项失败；V1.0.37 和 legacy 标记契约随后补齐 |
| Red / Task 18 | passed | `CodeRuleServiceDefinitionCacheTest` 因旧 `loadDefinition` 私有且无缓存契约导致 testCompile 3 处失败 |
| Starter ID | passed，15/15 | `SegmentSequenceGeneratorTest` 12、`SequenceControllerContractTest` 2、`SysIdSequenceMapperContractTest` 1 |
| Generator | passed，31/31 | Controller 3、migration 5、engine 8、legacy 2、Mapper 4、Dynamic CRUD 3、对象/SEQ identity 5、定义缓存 1 |
| 前端 | passed，14/14 | latest guard、取消识别和既有分段/映射/权限用例；相关 API/JS/Vue ESLint 0 errors |
| 前端生产构建 | passed | Node v20.19.0；8691 modules；仅仓库既有组件同名、动态/静态导入及 CSS 注释警告 |
| Admin 聚合编译 | passed，42/42 | Java 17；Starter ID、Generator、Caffeine 和 Admin 装配成功 |
| XML/Flyway/差异 | passed | 三个 Mapper `xmllint`；V1.0.36/V1.0.37 placeholder、`LEFT(biz_key` 和空白检查无输出 |
| 两阶段 Review | PASS / PASS_WITH_COMMENTS | P0/P1 为零；合法高基数计数器行的监控/留存策略作为 P2 运维治理项 |
| 真实环境 | skipped | 未启动 Admin/MySQL/Redis，未实跑 Flyway、登录态 HTTP 或浏览器点击验收 |

- 聚合 `-am` 定向测试尝试仍会在既有 `forge-starter-datascope` 的 surefire groups/engine 配置处提前失败；按历史基线改为目标模块直跑，目标测试均成功，Admin reactor compile 另行覆盖完整装配。
- 本轮未启动任何服务，无新增或遗留 PID。

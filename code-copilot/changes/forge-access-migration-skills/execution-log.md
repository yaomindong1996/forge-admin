# 执行日志

- 2026-11-28。`git fetch --multiple --no-tags origin cursor` 成功，本地 `main` 与 `origin/main` 均为 `34d200697`。
- `pnpm skills:smoke` 校验 14 个 Skill、两套业务编排路径，`TOTAL_ISSUES 0`。两个新 Skill 均通过 `skill-creator` 元数据校验。`python3 -B -m unittest discover -s scripts -p test_skills_smoke_check.py` 共 9 项通过。完整与最小后台临时工程生成后均通过 Skill 冒烟，完整工程的采购 DataScope Mapper 全名已换成生成的包名。`node --test scripts/forge-create/project-tools.test.mjs` 共 27 项通过。
- `python3 scripts/docs-check.py`、`pnpm check:edition` 和本轮文件 `git diff --check` 通过。`next-migration-version.mjs --desc add_purchase_export_permissions` 核对本地和远端水位均为 `1.0.245`，只读演算 `1.0.246`。PG/SQL Server 配对、三库版本查重、`python3 scripts/check-mapper-dialect.py` 与本轮样例 SQL 的 Flyway 占位符扫描通过。
- 独立前向试读补足了角色资源替换语义、按钮与 API 的权限码隔离、采购组合角色及分页对账、PG/SQL Server 全量快照携带历史时的校验办法。
- `mvn -o -pl forge-business/forge-business-core -am -Penable-tests -Dforge.compiler.skip=true -Dtest=PurchaseExportPermissionTest,DataScopeInterceptorTest,DataScopeServiceImplTest,PluginFlywayMigrationIntegrationTest,SamplePurchaseOrderPrintDataProviderTest -Dsurefire.failIfNoSpecifiedTests=false test` 在已编译的隔离副本上通过 28 项。临时副本通过项目当前 Mockito agent 参数避免 sandbox 自附加限制。真实回环 HTTP 同时检查菜单树与按钮权限，采购组织 A 导出 `PO-A-02` 和 `PO-A-01`，组织 B 只导出 `PO-B-01`。按供应商筛选后 Excel 与分页列表一致。逻辑删除、其他组织及同组织编号的其他租户订单均未出现在不应可见的工作簿。
- 只读、仅按钮、仅 API 和另一租户同名角色直调均返回业务码 `403`，匿名为 `401`，错误 HTTP 方法为 `405`，拒绝响应均无附件。测试库 `V1→V2` 与空库执行结果一致，权限关联从 15 增至 17，仅新增目标角色的两条关联。Flyway `validate` 通过，重跑没有待执行迁移，样例增量手动重放无重复。
- 前端既有 `useCrudImportExport.spec.js` 和 `download-axios-blob.spec.js` 通过 20 项，使用已安装的 Vitest 与 Node 20 启动。采购测试接口、测试账号及 SQL 仅存在于测试源码，真实数据库引擎与生产页面接入不在本次验证范围。后端测试使用 H2 MySQL 兼容模式，三库验证为静态检查。
- 最终去掉 `-Dforge.compiler.skip=true` 重跑同一 Maven 回归，包含依赖模块编译，结果为 `BUILD SUCCESS`，28 项通过，其中采购用例 9 项。Tomcat 使用 `127.0.0.1:53984`（测试进程 PID 3801），由测试上下文关闭，三个测试内存库均已关闭。
- `quality:check` 未通过。补齐临时分析器后，在 `HEAD` 副本上复现相对基线新增 45 项、超出基线 22 项和过期基线 14 项。门禁扫描的后端和前端源码目录与 `HEAD` 一致。自检在该副本上 3 项通过、2 项失败，两个失败项均要求现有工作树通过全仓门禁。采购用例的定向复杂度检查通过，仓库基线保持原样。
- 提交前从暂存区导出独立副本，`python3 scripts/skills-smoke-check.py` 和文档检查通过。移除新 Skill 的独立路由会触发冒烟失败。副本中的采购模块测试源码与 POM 和前述 28 项通过时完全一致。

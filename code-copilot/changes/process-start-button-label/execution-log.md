# 执行记录：业务流程启动按钮文案回退到流程名称

## 2026-11-13

- 版本：未发版，变更计入 `CHANGELOG.md` Unreleased。
- 状态：代码已完成并自测，待提交 / 待部署。部署后运行时渲染配置即时生效（投影链路按渲染实时编译），
  存量流程无需重新发布；已发布应用版本快照内的 `runtimeActions` 仅在下次发布时按新规则生成。

### 验证

| 命令（`forge-server`，JDK 环境 `env-java.sh`） | 结果 |
|------|------|
| `mvn -o -Penable-tests -pl …/forge-plugin-generator -am test -Dtest='FormulaFunctionMarketServiceTest,BusinessProcessRuntimeActionCompilerTest,BusinessApplicationPhaseFiveSecurityTest,BusinessProcessServiceTest'` | 30 条全部通过 |
| `mvn -o -Penable-tests -pl …/forge-plugin-generator test`（全模块，未加 `-am`） | 1313 条，1 失败 14 错误 |
| 基线（暂存本次改动，`-am`，仅跑上述失败类） | 27 条，1 失败 13 错误（同一批用例） |

全模块失败均为存量问题，与本次无关：`PrintApplicationPersistenceTest`（8，迁移脚本相对路径不存在）、
`DynamicCrudServiceChildListTest`（4）、`FlowModelPublicationDictionaryMigrationTest`（1，找不到字典增量脚本）、
`PrintCodegenContributorTest`（1）。`FormulaFunctionMarketServiceTest` 仅在不加 `-am` 时因本地仓库
`forge-starter-orm` 过期失败，加 `-am` 后通过。

### 跳过

- 未部署演示环境，未在页面实测按钮文案；部署后报销应用按钮应显示「报销审批」。

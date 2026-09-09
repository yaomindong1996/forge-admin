# 任务拆分 — 编码规则易混淆字符独立选择
> 拆分顺序：协议与测试 → 持久化 → 引擎 → 页面 → 验证

## 前置条件

- [x] 已读取根 `AGENTS.md`、项目记忆、编码规范与自动化测试标准。
- [x] 已确认目标分支为 `main`，不是规范禁止直接修改的 `master`。
- [x] 已确认目标前端文件存在未提交改动，本轮采用局部编辑并保留原差异。

## Task 1: 增量测试先行

- **目标**：用失败测试固定具体字符集合和旧开关兼容语义。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/__tests__/code-rule-utils.spec.js`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/manager/coderule/CodeRuleEngineTest.java`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/manager/coderule/CodeRuleMigrationContractTest.java`
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/manager/coderule/CodeRuleMapperContractTest.java`
- **状态**：完成；前端 2 项 Red，后端缺少重载/字段/迁移的编译 Red 均已取证。

## Task 2: 扩展分段协议与持久化

- **目标**：保存 `excludedCharacters`，并让旧 `excludeAmbiguous=1` 自动等价全选。
- **涉及文件**：
  - `AiCodeRuleSegment.java`
  - `CodeRuleSegmentDTO.java`
  - `CodeRuleService.java`
  - `CodeRuleSegmentMapper.xml`
  - `forge-server/db/migration/V1.0.39__add_code_rule_excluded_characters.sql`
- **状态**：完成。

## Task 3: 按具体字符生成编码

- **目标**：进制字母表、容量、步长与 legacy 宽度缓存统一使用规范化字符集合。
- **涉及文件**：
  - `CodeRuleRadixCodec.java`
  - `CodeRuleEngine.java`
- **关键签名**：
  ```java
  public String normalizeExcludedCharacters(String excludedCharacters, boolean legacyExcludeAll)
  public String alphabet(String radixType, String excludedCharacters)
  ```
- **状态**：完成。

## Task 4: 页面独立勾选

- **目标**：把总开关改为 I、O、Z 三项独立选择，并保持进制切换不清空配置。
- **涉及文件**：
  - `forge-admin-ui/src/views/app-center/code-rule-utils.js`
  - `forge-admin-ui/src/views/app-center/components/CodeRuleSegmentEditor.vue`
- **状态**：完成；浏览器验证 I/Z 可独立选中，O 保持未选。

## Task 5: 增量验证与回填

- **目标**：完成前后端定向测试、Lint、构建、XML/Flyway/差异检查并记录证据。
- **涉及文件**：
  - `code-copilot/changes/编码规则易混淆字符独立选择/test-spec.md`
  - `code-copilot/changes/编码规则易混淆字符独立选择/execution-log.md`
  - `code-copilot/changes/编码规则易混淆字符独立选择/spec.md`
  - `code-copilot/changes/编码规则易混淆字符独立选择/tasks.md`
- **状态**：完成；36/36 后端、18/18 前端、ESLint、build、Admin 42/42 与静态检查通过。

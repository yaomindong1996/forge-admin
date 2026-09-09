# 任务拆分 — 字典数据键值唯一性校验

## 前置条件

- [x] 已读取根规范、项目记忆、编码规范和自动化测试标准。
- [x] 已核对字典新增/修改链路和数据库现有唯一索引。
- [x] 已确认当前工作树仅有用户已有的 `.DS_Store` 变更，本任务不覆盖或暂存这些文件。

## Task 1：字典键值唯一性校验

- **目标**：同一租户、同一 `dictType` 下禁止保存重复的有效 `dictValue`，修改时排除自身。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/SysDictDataServiceImplTest.java` — 新增，覆盖重复拒绝和正常写入。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysDictDataMapper.java` — 新增重复计数签名。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/resources/mapper/SysDictDataMapper.xml` — 新增，查询有效字典项并支持排除当前编码。
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysDictDataServiceImpl.java` — 新增统一业务校验并接入新增/修改。
- **关键签名**：

  ```java
  int countByDictTypeAndValue(String dictType, String dictValue, Long excludeDictCode);
  ```

- [x] Step 1：先新增 Service 单元测试并运行，确认因缺少校验而 Red。
- [x] Step 2：新增 Mapper 接口与 XML 查询，显式包含 `del_flag = 0` 和可选 `dict_code <> #{excludeDictCode}`。
- [x] Step 3：在新增、修改写入前调用统一校验，重复时抛出 `BusinessException`。
- [x] Step 4：运行定向测试，确认新增重复、修改重复、排除自身和正常写入全部 Green。

## Task 2：增量验证与文档回填

- **目标**：按测试标准形成可复跑证据并关闭本轮变更。
- **涉及文件**：
  - `code-copilot/changes/dict-data-value-uniqueness-validation/spec.md`
  - `code-copilot/changes/dict-data-value-uniqueness-validation/tasks.md`
  - `code-copilot/changes/dict-data-value-uniqueness-validation/test-spec.md`
  - `code-copilot/changes/dict-data-value-uniqueness-validation/execution-log.md`
- [x] Step 1：执行目标单测、插件编译和 Mapper XML 静态检查。
- [x] Step 2：执行 `git diff --check` 并检查本轮差异范围。
- [x] Step 3：记录命令、结果、警告、跳过项和服务清理情况。
- [x] Step 4：回填 Spec/Task 状态，仅提交本任务文件，不暂存或推送用户已有工作树变更。

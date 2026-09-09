# 字典数据键值唯一性校验
> status: done
> created: 2026-08-04
> complexity: 🟢简单

## 1. 背景与目标

字典数据表已经通过数据库唯一索引限制同一租户、同一字典类型下的有效 `dict_value` 不重复，但新增或修改重复值时只能依赖数据库异常，前端无法获得稳定、清晰的业务提示。

本变更在字典数据新增和修改入口增加业务唯一性校验：同一租户、同一 `dict_type` 下，未删除字典项的 `dict_value` 不能重复；修改时排除当前字典项自身。数据库唯一索引继续作为并发写入的最终防线。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysDictDataController.java`：`add`、`edit` 分别调用 `insertDictData`、`updateDictData`。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysDictDataServiceImpl.java`：当前新增、修改直接调用 MyBatis-Plus `insert` / `updateById`，没有业务唯一性预检。
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysDictDataMapper.java`：当前没有字典键值重复查询方法。

### 2.2 现有数据库约束

- `forge-server/db/migration/V1.0.51__replace_logic_delete_generated_columns.sql` 已建立 `uk_tenant_dict_data_active (tenant_id, dict_type, dict_value, del_flag)`。
- `SysDictData.delFlag` 使用 `@TableLogic(value = "0", delval = "dict_code")`，有效数据为 `del_flag = 0`，删除后允许同值重建。

### 2.3 发现与风险

- 只做 Service 预查询不能消除并发竞态，因此不能替代现有数据库唯一索引。
- 查询 SQL 必须写入 Mapper XML，并显式过滤 `del_flag = 0`；租户条件由租户拦截器追加。
- 修改校验必须通过 `dict_code <> 当前值` 排除自身，否则仅修改标签等其它属性也会被误判重复。

## 3. 功能点

- [x] 新增字典项时，若同一字典类型已存在相同键值，返回业务异常“同一字典下的字典键值不能重复”。
- [x] 修改字典项时，排除当前 `dictCode` 后执行相同校验。
- [x] 非重复新增、修改保持原保存和字典缓存刷新行为。
- [x] 保留数据库唯一索引作为并发最终防线。

## 4. 业务规则

1. “同一个字典”以当前租户内的 `dictType` 标识。
2. 唯一键为当前租户下 `dictType + dictValue`，只统计 `del_flag = 0` 的有效记录。
3. 修改时排除当前 `dictCode`。
4. 值比较遵循数据库列现有字符集与排序规则，不在本变更中改变大小写、空格或存储值协议。
5. 已逻辑删除记录不阻止相同键值重新创建。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | `sys_dict_data` | 复用 `uk_tenant_dict_data_active` | 不新增或修改 Flyway 脚本 |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 行为优化 | `/system/dict/data/add` | POST | 重复键值返回明确业务异常 |
| 行为优化 | `/system/dict/data/edit` | POST | 排除自身后校验重复键值 |

请求和成功响应协议不变。

## 7. 影响范围

- `forge-plugin-system` 字典数据 Mapper、Mapper XML、Service。
- 字典数据 Service 单元测试。
- 不修改前端表单、接口路径和数据库结构。

## 8. 风险与关注点

- Service 预检与写入之间仍存在并发窗口，数据库唯一索引负责最终拒绝重复数据。
- 本变更不涉及资金、状态流转或权限放开。

## 8.5 测试策略

- **测试范围**：新增重复、修改重复、修改排除自身、非重复写入。
- **覆盖率目标**：覆盖 `insertDictData`、`updateDictData` 新增分支及 Mapper 参数传递。
- **独立 Test Spec**：是，见 `test-spec.md`。

## 9. 待澄清

无。按用户原始要求将“同一个字典”解释为同一租户下相同 `dictType`。

## 10. 技术决策

1. 使用 Mapper XML `countByDictTypeAndValue(dictType, dictValue, excludeDictCode)` 完成业务预检，遵循数据权限可审查约定。
2. Service 统一通过私有方法抛出 `BusinessException`，新增和修改复用相同提示。
3. 不新增数据库脚本，复用已经存在的有效记录唯一索引处理并发竞态。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | 完成 | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | 已完成现状和唯一索引核对 |
| Task 1 | 完成 | `SysDictDataServiceImplTest.java`、`SysDictDataMapper.java/xml`、`SysDictDataServiceImpl.java` | Red 4/4 失败；Green 4/4 通过 |
| Task 2 | 完成 | 本变更四份 SDD 文档 | 系统插件全量单测 56/56 通过，静态检查通过 |

## 12. 审查结论

- Spec 覆盖自检：新增、修改、排除自身、非重复保存和数据库并发兜底均已覆盖。
- 代码质量自检：查询位于 Mapper XML，显式过滤 `del_flag = 0`，无前端/接口/数据库结构变更。
- 验证结论：定向测试和 `forge-plugin-system` 全量测试通过；真实 API/数据库 E2E 未执行并已记录为跳过项。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-04
- **确认人**：用户
- **确认内容**：用户明确提出“同一个字典下面字典的值不能有重复”，授权实现该校验。

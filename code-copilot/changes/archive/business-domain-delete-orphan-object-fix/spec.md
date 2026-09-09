# 业务域删除孤立对象优化
> status: done
> created: 2026-08-04
> complexity: 🟡中等

## 1. 背景与目标

应用中心删除业务应用时会保留可复用的业务对象。当业务域下最后一个应用删除后，业务对象仍存在，但应用中心没有独立的全局对象清理入口；此时删除业务域会提示“该业务套件已存在业务对象，不能删除”，用户无法完成业务域清理。

本变更将删除行为调整为显式清理：仍有子业务域、业务应用或访问入口时继续阻止删除；业务域仅剩未被有效应用使用的孤立对象时，在用户确认后清理对象关系、逻辑删除业务对象并逻辑删除业务域。业务数据表、历史设计版本和发布记录不做物理删除。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/app-center/index.vue` 的 `removeSuite` 从应用中心业务域树发起删除。
- `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` 的 `deleteSuite` 从旧业务域详情页发起删除。
- `forge-admin-ui/src/api/business-app.js` 的 `deleteBusinessSuite` 调用 `DELETE /ai/business/suite/{id}`。
- `BusinessSuiteController.delete` 委托 `BusinessSuiteService.delete` 执行删除。

### 2.2 现有实现

- `BusinessApplicationService.delete` 会逻辑删除应用对象关联，但明确保留业务对象，支持后续复用。
- `BusinessSuiteService.delete` 按“子域 → 业务对象 → 访问入口 → 业务应用”顺序校验；只要 `ai_business_object` 中仍有未删除对象，就直接报“该业务套件已存在业务对象，不能删除”。
- `BusinessSuiteMapper.countObjectsBySuite` 已显式过滤 `del_flag = 0`，问题不是已删除数据误计数，而是应用删除后的业务对象按现有产品语义仍被保留。
- `BusinessApplicationObjectService.validateAndConvert` 要求业务应用和业务对象属于同一业务域；应用带对象/入口时也禁止直接迁移业务域。
- `ai_business_object_relation` 是关系重建表，现有实现使用物理删除；`ai_business_object`、`ai_business_suite` 使用主键墓碑逻辑删除。

### 2.3 发现与风险

- 直接移除业务对象校验会留下仍可查询、且可能在同编码业务域重建后重新出现的孤立对象，不能采用。
- 直接无提示级联删除业务对象会扩大现有删除接口语义，其他调用方可能在不知情时丢失设计入口，必须由调用方显式传入清理意图。
- 业务对象理论上只能被同域应用引用，但删除前仍应按有效应用关联做防御性检查，避免历史脏数据导致误删共享对象。
- 删除业务域不删除动态业务数据表，不物理清理设计版本、发布版本或运行日志；本轮只清理当前可见的域、对象和对象关系元数据。

## 3. 功能点

- [x] 删除业务域时优先校验子域、业务应用和访问入口，提示使用“业务域”术语。
- [x] 业务域存在业务对象且调用方未明确确认清理时，返回可操作的确认提示。
- [x] 调用方明确确认且对象未被有效业务应用引用时，事务内物理删除对象关系、逻辑删除业务对象和业务域。
- [x] 应用中心与旧业务域详情页根据对象数量显示清理影响，并传递显式清理参数。
- [x] 删除期间若仍有有效应用引用对象，则失败关闭，不清理对象或业务域。

## 4. 业务规则

1. 存在未删除子业务域时不能删除。
2. 存在未删除业务应用或访问入口时不能删除，无论是否传入清理参数。
3. 存在业务对象时，只有 `cleanupOrphanObjects=true` 才允许进入孤立对象清理。
4. 清理前若发现业务对象仍被未删除业务应用通过 `ai_business_application_object` 引用，则禁止删除。
5. `ai_business_object_relation` 属于关系重建表，随业务域删除做物理清理；`ai_business_object` 与 `ai_business_suite` 使用 `del_flag = id` 逻辑删除。
6. 不删除业务对象对应的业务数据表，不物理删除历史版本、日志或运行数据。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 物理删除 | `ai_business_object_relation` | 按 `tenant_id + suite_code` | 关系重建表，避免同编码业务域重建后读取旧关系 |
| 逻辑删除 | `ai_business_object` | `del_flag = id` | 仅清理目标业务域当前未删除对象 |
| 逻辑删除 | `ai_business_suite` | `del_flag = id` | 保持现有 MyBatis-Plus 删除语义 |

不新增 Flyway 脚本，不修改表结构或索引。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 修改 | `/ai/business/suite/{id}` | DELETE | 新增可选查询参数 `cleanupOrphanObjects`，默认 `false`；显式为 `true` 时可清理孤立对象 |

## 7. 影响范围

- 后端 generator 插件：业务域删除服务、Mapper 接口和 XML、Controller 参数。
- 前端管理端：业务域删除 API、应用中心和旧业务域详情页确认文案。
- 不影响业务应用删除语义，不影响单个业务对象删除接口，不改数据库结构。

## 8. 风险与关注点

- ⚠️ 逻辑删除属于设计态元数据状态流转，必须在同一事务内完成，任一步失败时整体回滚。
- ⚠️ 清理参数默认关闭，避免旧调用方无提示级联清理对象。
- ⚠️ 物理删除仅限 `ai_business_object_relation` 关系重建表；业务对象、业务域继续逻辑删除。
- ⚠️ 不承诺删除动态业务表或历史记录，前端确认文案必须明确边界。

## 8.5 测试策略

- **测试范围**：`BusinessSuiteService.delete` 的阻断顺序、显式确认、有效应用引用保护、关系清理和逻辑删除调用；Mapper XML 关键 SQL；前端 API 参数与删除确认文案；后端目标模块测试和前端构建。
- **覆盖率目标**：新增删除分支核心场景全部覆盖，不单独设置全模块覆盖率阈值。
- **独立 Test Spec**：是。

## 9. 待澄清

- 无。依据当前产品已有“删除应用保留业务对象”语义，业务域删除采用显式确认清理孤立对象。

## 10. 技术决策

- 不在 `BusinessSuiteService` 反向注入 `BusinessObjectService`，避免与 `BusinessObjectService → BusinessSuiteService` 形成循环依赖；批量清理由 `BusinessSuiteMapper` XML 完成。
- 删除顺序固定为：依赖校验 → 对象有效应用引用校验 → 删除对象关系 → 逻辑删除对象 → 逻辑删除业务域。
- 前端只在已加载汇总数据表明 `objectCount > 0` 时传 `cleanupOrphanObjects=true`；若并发新增对象导致数量变化，后端默认关闭策略会要求重新确认。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Task 1 | 已完成 | `BusinessSuiteServiceTest.java` | Red 因新删除协议缺失而失败，Green 6/6 通过 |
| Task 2 | 已完成 | Controller、Service、Mapper、Mapper XML | 显式清理参数与事务内孤立对象清理 |
| Task 3 | 已完成 | 前端 API、应用中心、旧业务域详情页 | 两处入口统一提示清理影响并传参 |
| Task 4 | 已完成 | Spec、Test Spec、Execution Log、memory | 完成定向测试、全量回归、构建与静态检查 |

## 12. 审查结论

实现符合本 Spec：默认删除协议不会无提示清理对象；显式确认后只物理清理关系重建表，并逻辑删除对象与业务域。定向测试 6/6、前端构建和 XML/差异检查通过。generator 全量 569 个用例中有 2 个失败和 4 个错误，均位于本轮未修改的公式、Binding、扩展版本和运行配置测试，已记录为非本轮阻断项。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-08-04
- **确认人**：用户直接提出修复业务域删除阻断问题；按最小安全范围执行。

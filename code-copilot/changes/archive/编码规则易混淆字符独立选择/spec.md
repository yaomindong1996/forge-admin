# 编码规则易混淆字符独立选择
> status: done
> created: 2026-07-18
> complexity: 🟡中等

## 1. 背景与目标

编码规则流水号段当前只有“排除 I / O / Z”总开关，管理员无法只排除其中一个或两个字符。本变更将配置改为 I、O、Z 三个独立选项，并让预览、真实生成、容量计算和持久化使用同一份字符集合。

完成后应满足：

- 用户可分别勾选 I、O、Z，支持任意组合和全部不选；
- 字母及字母数字进制只移除选中的字符，小写进制同步移除对应小写字符；
- 历史 `excludeAmbiguous=1` 且没有新字段的数据继续等价于同时排除 I/O/Z；
- 十进制、十六进制不受选择影响，切换进制时不丢失用户已经选择的字符。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- `forge-admin-ui/src/views/app-center/components/CodeRuleSegmentEditor.vue`：流水号段高级配置当前使用单个 `n-switch` 修改 `excludeAmbiguous`。
- `forge-admin-ui/src/views/app-center/code-rule-utils.js`：新分段、回显归一化和预览载荷只维护 0/1 总开关。
- `CodeRuleService` / `CodeRuleSegmentMapper.xml`：分段保存到 `ai_code_rule_segment.exclude_ambiguous`。
- `CodeRuleEngine` / `CodeRuleRadixCodec`：用 boolean 决定是否一次性移除 I/O/Z。

### 2.2 现有实现

- `CodeRuleRadixCodec#alphabet(String, boolean)` 在开关为 true 时固定移除 I/O/Z 及小写。
- `AiCodeRuleSegment` 与 `CodeRuleSegmentDTO` 没有具体字符集合字段。
- `V1.0.36__add_structured_code_rule_segments.sql` 中 `exclude_ambiguous` 是历史兼容字段。

### 2.3 发现与风险

- 直接把旧字段改成位掩码会让历史值 `1` 的语义从“全部排除”变成“只排除 I”，不可接受。
- 字符集合会改变进制容量、号段步长和 legacy 兼容宽度缓存键，不能只改前端展示。
- 目标前端文件已有用户未提交的预览/交互改动，本轮必须做局部补丁，禁止覆盖。

## 3. 功能点

- [x] 流水号段显示 I、O、Z 三个独立勾选项。
- [x] 新协议保存规范化的具体排除字符集合。
- [x] 编码引擎按具体集合执行编码、容量和宽度计算。
- [x] 历史总开关数据自动回显为全选并保持原生成结果。
- [x] Flyway 为存量表补字段并回填历史全选数据。

## 4. 业务规则

1. 允许字符仅为 I、O、Z；忽略重复值并按 I、O、Z 固定顺序保存。
2. 新字段为空且 `excludeAmbiguous=1` 时按 I/O/Z 全选处理；新字段非空时以新字段为准。
3. 小写字母进制选择 I/O/Z 时实际移除 i/o/z。
4. DECIMAL、HEX 的字母表不因该配置改变；配置仍保留，切回字母进制后继续生效。
5. 旧 `excludeAmbiguous` 字段保留：全选时同步为 1，其余组合为 0，供旧协议兼容读取。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增字段 | `ai_code_rule_segment` | `excluded_characters varchar(16)` | 保存规范化字符集合，如 `I,O,Z` |
| 数据回填 | `ai_code_rule_segment` | `excluded_characters` | 历史 `exclude_ambiguous=1` 回填为 `I,O,Z` |

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 兼容扩展 | `/system/code-rule` 详情/新增/更新 | GET/POST/PUT | 分段新增 `excludedCharacters` 字符串；保留 `excludeAmbiguous` |
| 兼容扩展 | `/system/code-rule/preview`、`/ai/code-rule/preview` | POST | 预览按具体字符集合生成 |

## 7. 影响范围

- 编码规则配置页流水号段高级配置；
- 分段 DTO、实体、Mapper XML、定义缓存快照；
- 编码进制转换、容量计算和 legacy 宽度兼容；
- `ai_code_rule_segment` Flyway 增量迁移。

## 8. 风险与关注点

- 不改变规则编码、分段键、序列 key，不重置任何计数器。
- 字符集合变化会改变未来编码字符表与容量，属于管理员显式配置结果。
- 不修改现有 `V1.0.36`/`V1.0.37`，新增更高版本迁移。

## 8.5 测试策略

- **测试范围**：前端归一化/交互纯函数、后端 codec/engine、Mapper/Flyway 静态契约、定向 Lint 和前端构建。
- **覆盖率目标**：覆盖单选、组合、全选、全不选、历史总开关、小写映射、十六进制无影响。
- **独立 Test Spec**：是。

## 9. 待澄清

- 无。用户已明确要求 I、O、Z 可单独选择。

## 10. 技术决策

- 新增 `excludedCharacters` 字符串协议并保留旧 0/1 字段，避免位掩码破坏历史语义。
- 字符集合规范化收敛到前端工具函数和后端 `CodeRuleRadixCodec`，引擎、容量与缓存统一消费规范值。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 建立基线 | 完成 | 本目录四份 SDD 文档 | 复用 2026-07-17 编码规则验证基线 |
| 前后端实现 | 完成 | UI、DTO/Entity、Codec/Engine、Service/Mapper、V1.0.39 | 旧总开关兼容为全选 |
| 增量验证 | 完成 | Test、Lint、Build、Playwright、静态检查 | 详细证据见 execution-log.md |

## 12. 审查结论

- Spec Compliance：PASS。
- Code Quality：PASS；没有重置规则编码、分段键或序列 key。
- 真实 Flyway 和新版后端保存接口未实跑，按项目分工由用户环境补验。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-18
- **确认人**：用户直接提出实现请求

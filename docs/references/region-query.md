# 行政区划查询规则

> 适用于所有含 `region_code` 字段的业务表查询。

## 附录 A：行政区划查询规则（重要）

> 此规则适用于所有含 `region_code` 字段的业务表查询。

**核心概念**：虚拟组织节点 code 以 `ALL` 结尾（如 `150000ALL`），代表"本级 + 下级"聚合。

**MyBatis XML 写法**（推荐，写在 Mapper XML 中）：
```xml
<if test="regionCode != null and regionCode != '' and regionCode.contains('ALL')">
    AND (region_code = REPLACE(#{regionCode},'ALL','')
         OR region_code IN (SELECT code FROM sys_region_code WHERE parent_code = REPLACE(#{regionCode},'ALL','')))
</if>
<if test="regionCode != null and regionCode != '' and !regionCode.contains('ALL')">
    AND region_code = #{regionCode}
</if>
```

**规则说明**：
- 选择虚拟组织（如 `150100ALL`）：查询本级 + **直属**下级区划（`parent_code = 去 ALL 后的编码`）
- 选择普通区划（如 `150102`）：精确匹配
- 前端 `RegionTreeSelect` 中虚拟组织节点 `disabled: true`，不可选中
- 手写 `parent_code = ?` **仅覆盖直属子级**。多级树（县→乡镇/街道→村/社区）的「本级+全部子孙」请走 DataScope 的 `getRegionAndChildCodes`（元数据快照已展开全子孙），不要只依赖上述 XML 子查询
- 级别约定：`1-省,2-市,3-区/县,4-乡镇/街道,5-村/社区`；村/社区由平台超管在「行政区划」中手工维护，无官方种子

---


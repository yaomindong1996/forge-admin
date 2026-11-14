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
- 选择虚拟组织（如 `150100ALL`）：查询本级 + 所有下级区划
- 选择普通区划（如 `150102`）：精确匹配
- 前端 `RegionTreeSelect` 中虚拟组织节点 `disabled: true`，不可选中

---


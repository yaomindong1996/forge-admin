-- 统一 sys_flow_model.category 和 sys_flow_template.category 存储为分类 ID
-- 使 JOIN 条件可以简化为 c.id = m.category，避免 OR 导致索引失效
-- 关联变更：FlowTaskMapper.xml / FlowModelMapper.xml 中 7 处 JOIN OR 同步修改

-- 1. 迁移 sys_flow_model.category：将 category_code 值更新为对应 ID
UPDATE sys_flow_model m
INNER JOIN sys_flow_category c ON m.category = c.category_code
SET m.category = c.id
WHERE m.del_flag = 0;

-- 2. 迁移 sys_flow_template.category：将 category_code 值更新为对应 ID
UPDATE sys_flow_template t
INNER JOIN sys_flow_category c ON t.category = c.category_code
SET t.category = c.id
WHERE t.del_flag = 0;

-- 回填企业协同连接缺失的租户ID
-- 早期创建的连接（超管租户忽略态下 MP 租户插件不注入 tenant_id，且当时无代码兜底）tenant_id 为 NULL，
-- 导致目录同步等以 connection.tenant_id 构造执行上下文的链路报"企业协同执行上下文不完整"。
-- 业务数据租户规则：归属默认租户 1。

UPDATE sys_social_config
SET tenant_id = 1
WHERE tenant_id IS NULL;

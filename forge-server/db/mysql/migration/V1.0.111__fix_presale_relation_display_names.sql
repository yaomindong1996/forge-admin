-- V1.0.111 修复预售登记关系名称显示为 key 而非人类可读名称的问题
-- 原因：V1.0.105 种子数据中 ai_business_object_relation.relation_name 写入了 'presale_items'（relationKey），
-- 前端"目标明细关系"下拉直接显示该 key，用户看到的是 presale_items 而非"预售商品明细"。

UPDATE ai_business_object_relation
SET relation_name = '预售商品明细', update_by = 1, update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND source_object_code = 'PS_PRESALE_ORDER'
  AND target_object_code = 'PS_PRESALE_ORDER_ITEM'
  AND relation_name = 'presale_items';

UPDATE ai_business_object_relation
SET relation_name = '操作日志', update_by = 1, update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND source_object_code = 'PS_PRESALE_ORDER'
  AND target_object_code = 'PS_PRESALE_OPERATION_LOG'
  AND relation_name = 'operation_logs';

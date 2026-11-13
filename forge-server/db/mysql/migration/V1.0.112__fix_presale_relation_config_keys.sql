-- V1.0.112 修复预售登记关系 relation_config 中 relationKey 被 relation designer 覆盖的问题
-- 原因：BusinessRelationDesigner 保存时用 lowerSnake(targetObjectCode) 重新生成 relationKey，
-- 将种子数据的 'presale_items' 覆盖为 'ps_presale_order_item'，导致动作配置中的 relationKey 不匹配。
-- 本脚本将 relation_config.relationKey 恢复为与动作配置一致的关系键。

UPDATE ai_business_object_relation
SET relation_config = JSON_SET(
      COALESCE(relation_config, JSON_OBJECT()),
      '$.relationKey', 'presale_items'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND source_object_code = 'PS_PRESALE_ORDER'
  AND target_object_code = 'PS_PRESALE_ORDER_ITEM'
  AND JSON_UNQUOTE(JSON_EXTRACT(relation_config, '$.relationKey')) != 'presale_items';

UPDATE ai_business_object_relation
SET relation_config = JSON_SET(
      COALESCE(relation_config, JSON_OBJECT()),
      '$.relationKey', 'operation_logs'
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND source_object_code = 'PS_PRESALE_ORDER'
  AND target_object_code = 'PS_PRESALE_OPERATION_LOG'
  AND JSON_UNQUOTE(JSON_EXTRACT(relation_config, '$.relationKey')) != 'operation_logs';

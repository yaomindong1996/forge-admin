-- 低代码单据流程实例关联补充提交轮次：同一业务单据多次发起（终态驳回后重新发起）时按轮次区分历史。

SET @round_no_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'round_no'
);

SET @sql := IF(
  @round_no_exists = 0,
  'ALTER TABLE ai_business_flow_instance_link ADD COLUMN round_no INT NOT NULL DEFAULT 1 COMMENT ''提交轮次，同一 business_key 第几次发起'' AFTER result',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量数据按发起时间回填轮次，避免全部停在 1。
UPDATE ai_business_flow_instance_link link
JOIN (
  SELECT
    id,
    ROW_NUMBER() OVER (
      PARTITION BY tenant_id, business_key
      ORDER BY start_time ASC, create_time ASC, id ASC
    ) AS computed_round
  FROM ai_business_flow_instance_link
) ranked ON ranked.id = link.id
SET link.round_no = ranked.computed_round
WHERE link.round_no IS NULL OR link.round_no < 1 OR link.round_no <> ranked.computed_round;

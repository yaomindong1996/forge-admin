-- 流程模型目录排序。排序值越小越靠前；未设置时保持原有创建时间排序。
SET @flow_model_sort_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_model'
      AND COLUMN_NAME = 'sort_order'
);
SET @flow_model_sort_sql := IF(
    @flow_model_sort_column = 0,
    'ALTER TABLE sys_flow_model ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT ''模型目录排序值，数值越小越靠前'' AFTER importance_level',
    'SELECT 1'
);
PREPARE flow_model_sort_stmt FROM @flow_model_sort_sql;
EXECUTE flow_model_sort_stmt;
DEALLOCATE PREPARE flow_model_sort_stmt;

SET @flow_model_sort_index := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_model'
      AND INDEX_NAME = 'idx_flow_model_tenant_sort'
);
SET @flow_model_sort_index_sql := IF(
    @flow_model_sort_index = 0,
    'ALTER TABLE sys_flow_model ADD KEY idx_flow_model_tenant_sort (tenant_id, del_flag, sort_order, create_time, id)',
    'SELECT 1'
);
PREPARE flow_model_sort_index_stmt FROM @flow_model_sort_index_sql;
EXECUTE flow_model_sort_index_stmt;
DEALLOCATE PREPARE flow_model_sort_index_stmt;

-- 保留迁移前按创建时间倒序的目录顺序，避免新增列全部为0时打乱现有模型列表。
UPDATE sys_flow_model m
INNER JOIN (
    SELECT ranked_source.id, ranked_source.calculated_sort
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY create_time DESC, id ASC) - 1 AS calculated_sort
        FROM sys_flow_model
        WHERE del_flag = 0
    ) ranked_source
) ranked ON ranked.id = m.id
SET m.sort_order = ranked.calculated_sort
WHERE m.del_flag = 0;

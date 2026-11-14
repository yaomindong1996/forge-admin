-- Cover the normalized todo candidate visibility lookup, including the task ID returned to the outer query.

SET @candidate_lookup_covering_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task_candidate'
      AND INDEX_NAME = 'idx_flow_task_candidate_visibility'
);

SET @candidate_lookup_covering_sql = IF(
    @candidate_lookup_covering_exists = 0,
    'CREATE INDEX idx_flow_task_candidate_visibility ON sys_flow_task_candidate (tenant_id, status, candidate_type, candidate_value, task_id)',
    'SELECT 1'
);

PREPARE candidate_lookup_covering_stmt FROM @candidate_lookup_covering_sql;
EXECUTE candidate_lookup_covering_stmt;
DEALLOCATE PREPARE candidate_lookup_covering_stmt;

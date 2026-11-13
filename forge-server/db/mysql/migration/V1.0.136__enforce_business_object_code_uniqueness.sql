-- 业务对象编码是流程 businessKey 的稳定身份。
-- 历史版本只按 tenant_id + suite_code + object_code 建唯一索引，允许不同业务套件重复编码，
-- 导致待办表单按 objectCode 回查时可能命中错误对象。本迁移先给重复对象生成稳定的新编码，
-- 同步可按 object_id/config_key 确定身份的引用，最后把唯一约束提升到租户范围。

DROP TEMPORARY TABLE IF EXISTS forge_business_object_code_repair;

CREATE TEMPORARY TABLE forge_business_object_code_repair (
    tenant_id BIGINT NOT NULL,
    object_id BIGINT NOT NULL,
    suite_code VARCHAR(48) NOT NULL,
    old_code VARCHAR(48) NOT NULL,
    new_code VARCHAR(48) NOT NULL,
    config_key VARCHAR(128) DEFAULT NULL,
    PRIMARY KEY (tenant_id, object_id),
    UNIQUE KEY uk_repair_new_code (tenant_id, new_code)
);

-- 每个重复编码保留创建时间最早（ID 最小）的对象，其余对象使用对象主键生成新编码。
-- 主键在租户内唯一，因此该编码不依赖概率摘要，不会与现有对象产生理论碰撞。
INSERT INTO forge_business_object_code_repair
    (tenant_id, object_id, suite_code, old_code, new_code, config_key)
SELECT object_row.tenant_id,
       object_row.id,
       object_row.suite_code,
       object_row.object_code,
       CONCAT('bo_', object_row.id),
       object_row.config_key
FROM ai_business_object object_row
WHERE object_row.del_flag = 0
  AND EXISTS (
      SELECT 1
      FROM ai_business_object duplicate_row
      WHERE duplicate_row.tenant_id = object_row.tenant_id
        AND duplicate_row.object_code = object_row.object_code
        AND duplicate_row.del_flag = 0
      GROUP BY duplicate_row.tenant_id, duplicate_row.object_code
      HAVING COUNT(1) > 1
  )
  AND object_row.id <> (
      SELECT MIN(keeper.id)
      FROM ai_business_object keeper
      WHERE keeper.tenant_id = object_row.tenant_id
        AND keeper.object_code = object_row.object_code
        AND keeper.del_flag = 0
  );

-- 带 suite/object_id 的设计态和运行态引用。
UPDATE ai_business_app app
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = app.tenant_id
       AND repair.suite_code = app.suite_code
       AND repair.old_code = app.object_code
    SET app.object_code = repair.new_code,
    app.options = REPLACE(REPLACE(
        app.options,
        CONCAT('"objectCode":"', repair.old_code, '"'),
        CONCAT('"objectCode":"', repair.new_code, '"')),
        CONCAT('"objectCode": "', repair.old_code, '"'),
        CONCAT('"objectCode": "', repair.new_code, '"')),
    app.update_time = NOW()
WHERE app.del_flag = 0;

UPDATE ai_business_application application_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = application_row.tenant_id
       AND repair.suite_code = application_row.suite_code
SET application_row.options = REPLACE(
        REPLACE(application_row.options,
            CONCAT('"objectCode":"', repair.old_code, '"'),
            CONCAT('"objectCode":"', repair.new_code, '"')),
        CONCAT('"objectCode": "', repair.old_code, '"'),
        CONCAT('"objectCode": "', repair.new_code, '"')),
    application_row.update_time = NOW()
WHERE application_row.del_flag = 0
  AND application_row.options IS NOT NULL;

UPDATE ai_business_binding binding_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = binding_row.tenant_id
       AND binding_row.target_type = 'OBJECT'
       AND binding_row.target_id = repair.object_id
SET binding_row.target_code = repair.new_code,
    binding_row.binding_config = REPLACE(REPLACE(
        binding_row.binding_config,
        CONCAT('"objectCode":"', repair.old_code, '"'),
        CONCAT('"objectCode":"', repair.new_code, '"')),
        CONCAT('"objectCode": "', repair.old_code, '"'),
        CONCAT('"objectCode": "', repair.new_code, '"'));

UPDATE ai_business_object_relation relation_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = relation_row.tenant_id
       AND repair.suite_code = relation_row.suite_code
       AND repair.old_code = relation_row.source_object_code
SET relation_row.source_object_code = repair.new_code;

UPDATE ai_business_object_relation relation_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = relation_row.tenant_id
       AND repair.suite_code = relation_row.suite_code
       AND repair.old_code = relation_row.target_object_code
SET relation_row.target_object_code = repair.new_code;

UPDATE ai_business_object_design_version version_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = version_row.tenant_id
       AND repair.object_id = version_row.object_id
SET version_row.object_code = repair.new_code,
    version_row.suite_code = repair.suite_code;

UPDATE ai_business_document_config document_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = document_row.tenant_id
       AND (
           repair.object_id = document_row.object_id
           OR (repair.config_key IS NOT NULL AND repair.config_key = document_row.config_key)
       )
SET document_row.object_code = repair.new_code,
    document_row.suite_code = repair.suite_code;

UPDATE ai_business_trigger trigger_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = trigger_row.tenant_id
       AND repair.suite_code = trigger_row.suite_code
       AND repair.old_code = trigger_row.object_code
SET trigger_row.object_code = repair.new_code;

UPDATE ai_business_trigger_log trigger_log
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = trigger_log.tenant_id
       AND repair.suite_code = trigger_log.suite_code
       AND repair.old_code = trigger_log.object_code
SET trigger_log.object_code = repair.new_code;

UPDATE ai_business_action_execution_log action_log
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = action_log.tenant_id
       AND repair.suite_code = action_log.suite_code
       AND repair.old_code = action_log.object_code
SET action_log.object_code = repair.new_code;

UPDATE ai_business_process process_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = process_row.tenant_id
       AND repair.object_id = process_row.subject_object_id
SET process_row.subject_object_code = repair.new_code;

UPDATE ai_business_process process_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = process_row.tenant_id
        AND repair.object_id = process_row.subject_object_id
SET process_row.draft_schema_json = CASE
    WHEN JSON_VALID(process_row.draft_schema_json)
        THEN JSON_SET(process_row.draft_schema_json, '$.subject.objectCode', repair.new_code)
    ELSE process_row.draft_schema_json
END;

UPDATE ai_business_process_version version_row
INNER JOIN ai_business_process process_row
        ON process_row.tenant_id = version_row.tenant_id
       AND process_row.id = version_row.process_id
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = process_row.tenant_id
       AND repair.object_id = process_row.subject_object_id
SET version_row.schema_json = REPLACE(
        version_row.schema_json,
        CONCAT('"objectCode":"', repair.old_code, '"'),
        CONCAT('"objectCode":"', repair.new_code, '"')
    ),
    version_row.dependency_snapshot_json = REPLACE(
        version_row.dependency_snapshot_json,
        repair.old_code,
        repair.new_code
    );

UPDATE ai_business_process_run process_run
INNER JOIN ai_business_process process_row
        ON process_row.tenant_id = process_run.tenant_id
       AND process_row.id = process_run.process_id
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = process_row.tenant_id
       AND repair.object_id = process_row.subject_object_id
SET process_run.subject_object_code = repair.new_code,
    process_run.context_snapshot = REPLACE(
        process_run.context_snapshot,
        repair.old_code,
        repair.new_code
    ),
    process_run.business_key = CASE
        WHEN process_run.business_key LIKE CONCAT(repair.old_code, ':%')
            THEN CONCAT(repair.new_code, SUBSTRING(process_run.business_key, CHAR_LENGTH(repair.old_code) + 1))
        ELSE process_run.business_key
    END;

-- CRUD 配置没有 object_id 时按 config_key（稳定身份）更新；老配置再按 domain_code/suite_code 兜底。
UPDATE ai_crud_config crud
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = crud.tenant_id
       AND (
           (repair.config_key IS NOT NULL AND repair.config_key = crud.config_key)
           OR (repair.config_key IS NULL AND repair.suite_code = crud.domain_code
               AND repair.old_code = crud.object_code)
       )
SET crud.object_code = repair.new_code;

UPDATE ai_crud_config_version version_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = version_row.tenant_id
       AND (
           (repair.config_key IS NOT NULL AND repair.config_key = version_row.config_key)
           OR (repair.config_key IS NULL AND repair.suite_code = version_row.domain_code
               AND repair.old_code = version_row.object_code)
       )
SET version_row.object_code = repair.new_code;

UPDATE ai_code_rule rule_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = rule_row.tenant_id
       AND repair.object_id = rule_row.source_object_id
SET rule_row.source_object_code = repair.new_code;

-- 流程实例关联优先使用 variables_snapshot 中的 configKey 区分历史同编码对象。
-- 不能改写 business_key：Flowable 运行表仍以旧 businessKey 关联任务，保留它才能让待办列表回查到实例。
UPDATE ai_business_flow_instance_link flow_link
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = flow_link.tenant_id
       AND repair.config_key IS NOT NULL
       AND JSON_VALID(flow_link.variables_snapshot)
       AND JSON_UNQUOTE(JSON_EXTRACT(flow_link.variables_snapshot, '$.configKey')) = repair.config_key
SET flow_link.object_code = repair.new_code,
    flow_link.variables_snapshot = JSON_SET(
        flow_link.variables_snapshot,
        '$.objectCode', repair.new_code
    );

-- 业务对象自身最后更新，避免引用更新过程中丢失 old_code -> object_id 的映射。
UPDATE ai_business_object object_row
INNER JOIN forge_business_object_code_repair repair
        ON repair.tenant_id = object_row.tenant_id
       AND repair.object_id = object_row.id
SET object_row.object_code = repair.new_code,
    object_row.update_time = NOW();

SET @old_object_active_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
      AND INDEX_NAME = 'uk_ai_business_object_code_active'
);
SET @sql = IF(
    @old_object_active_index_exists > 0,
    'ALTER TABLE ai_business_object DROP INDEX uk_ai_business_object_code_active',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_object_legacy_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
      AND INDEX_NAME = 'uk_ai_business_object_code'
);
SET @sql = IF(
    @old_object_legacy_index_exists > 0,
    'ALTER TABLE ai_business_object DROP INDEX uk_ai_business_object_code',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @new_object_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
      AND INDEX_NAME = 'uk_ai_business_object_code_tenant_active'
);
SET @sql = IF(
    @new_object_index_exists = 0,
    'ALTER TABLE ai_business_object ADD UNIQUE INDEX uk_ai_business_object_code_tenant_active (tenant_id, object_code, del_flag)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS forge_business_object_code_repair;

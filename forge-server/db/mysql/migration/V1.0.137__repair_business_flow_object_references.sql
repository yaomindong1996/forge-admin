-- 修复业务对象编码唯一化后遗漏的流程表单引用。
-- V1.0.136 已更新业务对象、应用页面和主流程 subject.objectCode，
-- 但历史流程草稿、Flowable 模型表单引用及应用版本快照仍可能保留旧编码。
-- 这里以应用发布快照中的 objectId -> 旧 objectCode 作为稳定映射，避免按编码误关联对象。

DROP TEMPORARY TABLE IF EXISTS forge_business_flow_object_repair;

CREATE TEMPORARY TABLE forge_business_flow_object_repair (
    tenant_id BIGINT NOT NULL,
    object_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    old_code VARCHAR(128) NOT NULL,
    new_code VARCHAR(128) NOT NULL,
    config_key VARCHAR(128) DEFAULT NULL,
    PRIMARY KEY (tenant_id, object_id, application_id),
    KEY idx_repair_application_code (tenant_id, application_id, old_code)
);

-- 从每个应用最新版本快照读取迁移前编码。应用快照保留了 objectId，
-- 即使 objectCode 已改名也能准确定位对应业务对象。
INSERT INTO forge_business_flow_object_repair
    (tenant_id, object_id, application_id, old_code, new_code, config_key)
SELECT DISTINCT
       object_row.tenant_id,
       object_row.id,
       application_version.application_id,
       snapshot_object.old_code,
       object_row.object_code,
       object_row.config_key
FROM ai_business_application_version application_version
INNER JOIN (
    SELECT tenant_id, application_id, MAX(version_no) AS version_no
    FROM ai_business_application_version
    WHERE del_flag = 0
    GROUP BY tenant_id, application_id
) latest_version
        ON latest_version.tenant_id = application_version.tenant_id
       AND latest_version.application_id = application_version.application_id
       AND latest_version.version_no = application_version.version_no
INNER JOIN JSON_TABLE(
    application_version.snapshot_json,
    '$.objects[*]' COLUMNS (
        object_id BIGINT PATH '$.objectId',
        old_code VARCHAR(128) PATH '$.objectCode'
    )
) snapshot_object
        ON 1 = 1
INNER JOIN ai_business_object object_row
        ON object_row.tenant_id = application_version.tenant_id
       AND object_row.id = snapshot_object.object_id
       AND object_row.del_flag = 0
WHERE application_version.del_flag = 0
  AND snapshot_object.old_code IS NOT NULL
  AND snapshot_object.old_code <> object_row.object_code;

-- 草稿中的 dependencies.objects、动作节点 config.objectCode 以及其它 JSON 引用
-- 都使用字符串编码；按对象 ID 限定替换范围，避免影响其它应用的同名编码。
UPDATE ai_business_process process_row
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = process_row.tenant_id
       AND repair.object_id = process_row.subject_object_id
SET process_row.draft_schema_json = REPLACE(
        process_row.draft_schema_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    process_row.subject_object_code = repair.new_code,
    process_row.update_time = NOW()
WHERE process_row.del_flag = 0
  AND process_row.draft_schema_json LIKE CONCAT('%"', repair.old_code, '"%');

UPDATE ai_business_process_version version_row
INNER JOIN ai_business_process process_row
        ON process_row.tenant_id = version_row.tenant_id
       AND process_row.id = version_row.process_id
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = process_row.tenant_id
       AND repair.object_id = process_row.subject_object_id
SET version_row.schema_json = REPLACE(
        version_row.schema_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    version_row.dependency_snapshot_json = REPLACE(
        version_row.dependency_snapshot_json,
        repair.old_code,
        repair.new_code
    ),
    version_row.update_time = NOW()
WHERE version_row.del_flag = 0
  AND (
      version_row.schema_json LIKE CONCAT('%"', repair.old_code, '"%')
      OR version_row.dependency_snapshot_json LIKE CONCAT('%', repair.old_code, '%')
  );

-- 应用版本和发布运行快照会被后续发布重新读取，必须同步修复，
-- 否则重新发布应用时会把旧 objectCode 再次带回运行配置。
UPDATE ai_business_application_version version_row
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = version_row.tenant_id
       AND repair.application_id = version_row.application_id
       AND repair.old_code = (
           SELECT snapshot_object.old_code
           FROM JSON_TABLE(
               version_row.snapshot_json,
               '$.objects[*]' COLUMNS (
                   object_id BIGINT PATH '$.objectId',
                   old_code VARCHAR(128) PATH '$.objectCode'
               )
           ) snapshot_object
           WHERE snapshot_object.object_id = repair.object_id
           LIMIT 1
       )
SET version_row.snapshot_json = REPLACE(
        version_row.snapshot_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    version_row.update_time = NOW()
WHERE version_row.del_flag = 0
  AND version_row.snapshot_json LIKE CONCAT('%"', repair.old_code, '"%');

UPDATE ai_business_application_publish_run run_row
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = run_row.tenant_id
       AND repair.application_id = run_row.application_id
       AND run_row.snapshot_json LIKE CONCAT('%"objectId":"', repair.object_id, '"%')
SET run_row.snapshot_json = REPLACE(
        run_row.snapshot_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    run_row.update_time = NOW()
WHERE run_row.snapshot_json LIKE CONCAT('%"', repair.old_code, '"%');

-- 流程模型全局表单引用包含 applicationId。同步 form_json 和 bpmn_xml，
-- 让流程中心测试入口及新部署的 Flowable 节点使用规范编码。
UPDATE sys_flow_model model_row
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = model_row.tenant_id
       AND JSON_VALID(model_row.form_json)
       AND JSON_UNQUOTE(JSON_EXTRACT(model_row.form_json, '$.applicationId'))
           = CAST(repair.application_id AS CHAR)
SET model_row.form_json = REPLACE(
        model_row.form_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    model_row.bpmn_xml = REPLACE(model_row.bpmn_xml, repair.old_code, repair.new_code),
    model_row.update_time = NOW()
WHERE model_row.form_json LIKE CONCAT('%"', repair.old_code, '"%');

UPDATE sys_flow_model_version version_row
INNER JOIN sys_flow_model model_row
        ON model_row.tenant_id = version_row.tenant_id
       AND model_row.id = version_row.model_id
INNER JOIN forge_business_flow_object_repair repair
        ON repair.tenant_id = model_row.tenant_id
       AND JSON_VALID(model_row.form_json)
       AND JSON_UNQUOTE(JSON_EXTRACT(model_row.form_json, '$.applicationId'))
           = CAST(repair.application_id AS CHAR)
SET version_row.form_json = REPLACE(
        version_row.form_json,
        CONCAT('"', repair.old_code, '"'),
        CONCAT('"', repair.new_code, '"')
    ),
    version_row.bpmn_xml = REPLACE(version_row.bpmn_xml, repair.old_code, repair.new_code)
WHERE version_row.form_json LIKE CONCAT('%"', repair.old_code, '"%')
   OR version_row.bpmn_xml LIKE CONCAT('%', repair.old_code, '%');

DROP TEMPORARY TABLE IF EXISTS forge_business_flow_object_repair;

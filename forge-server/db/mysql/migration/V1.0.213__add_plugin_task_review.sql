-- 部署前人工审查/失联关闭，不执行部署、删除包或伪造机器报告。
-- 回滚：隐藏新按钮/停用入口；保留任务与审计，不能改回已释放的占用绕过人工核查。
CREATE TABLE IF NOT EXISTS sys_plugin_task_review (
    id varchar(36) NOT NULL,
    tenant_id bigint NOT NULL,
    task_id varchar(36) NOT NULL,
    request_id varchar(36) NOT NULL,
    command_sha256 char(64) NOT NULL,
    expected_revision int NOT NULL,
    package_sha256 char(64) NOT NULL,
    result_sha256 char(64) DEFAULT NULL,
    decision varchar(32) NOT NULL,
    previous_status varchar(32) NOT NULL,
    target_status varchar(32) NOT NULL,
    executor_stopped boolean NOT NULL COMMENT '管理员声明，不是平台远程验证',
    not_deployed boolean NOT NULL,
    artifacts_reviewed boolean DEFAULT NULL,
    migrations_reviewed boolean DEFAULT NULL,
    note varchar(1000) NOT NULL COMMENT '人工核查说明，禁止记录凭证/原始日志',
    del_flag int NOT NULL DEFAULT 0,
    create_by bigint NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint NOT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_plugin_review_request (tenant_id, task_id, create_by, request_id),
    UNIQUE KEY uk_sys_plugin_review_decision (tenant_id, task_id, decision),
    KEY idx_sys_plugin_review_history (tenant_id, task_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件任务人工审查审计（只追加）';

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_public, menu_status, visible,
    perms, api_method, api_url, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, entry.name, menu.id, entry.type, entry.sort, 0, 1, 1, 'system:plugin:review',
       entry.method, entry.url, 1, NOW(), 1, NOW(), 1, 'pc', 0, 0
FROM sys_resource menu CROSS JOIN (
    SELECT '人工核查插件任务' AS name, 4 AS type, 20 AS sort, 'POST' AS method,
           '/system/plugin-task/*/review' AS url
    UNION ALL SELECT '构建审查与关闭', 3, 21, NULL, NULL
) entry
WHERE menu.tenant_id = 1 AND menu.client_code = 'pc' AND menu.resource_type = 2 AND menu.del_flag = 0
  AND menu.min_user_type = 0 AND menu.perms = 'system:plugin:view' AND menu.path = '/system/plugin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing WHERE existing.tenant_id = 1 AND existing.del_flag = 0
      AND existing.client_code = 'pc' AND existing.resource_type = entry.type
      AND (existing.perms = 'system:plugin:review'
           OR (entry.type = 4 AND existing.api_method = entry.method AND existing.api_url = entry.url))
  );

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, '插件任务核查结论', 'sys_plugin_review_decision', 1, 1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_plugin_review_decision' AND del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.sort, entry.label, entry.value, entry.type, entry.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0 FROM (
    SELECT 8 AS sort, '已审查（待部署）' AS label, 'release_ready' AS value,
           'sys_plugin_task_status' AS type, 'info' AS style
    UNION ALL SELECT 9, '已关闭（保留审计）', 'closed', 'sys_plugin_task_status', 'default'
    UNION ALL SELECT 1, '构建审查通过', 'approve_build', 'sys_plugin_review_decision', 'info'
    UNION ALL SELECT 2, '人工关闭任务', 'close_task', 'sys_plugin_review_decision', 'default'
) entry WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id = 1
    AND existing.dict_type = entry.type AND existing.dict_value = entry.value AND existing.del_flag = 0
);

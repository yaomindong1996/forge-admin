-- 只登记人工核查的候选元数据，不上传制品、不授权机器写入或执行部署。
-- 回滚先停用新增入口/权限；审计保留，禁止删除历史或修改已执行迁移。
CREATE TABLE IF NOT EXISTS sys_plugin_artifact_registration (
    id varchar(36) NOT NULL,
    tenant_id bigint NOT NULL,
    task_id varchar(36) NOT NULL,
    review_id varchar(36) NOT NULL,
    request_id varchar(36) NOT NULL,
    command_sha256 char(64) NOT NULL,
    expected_revision int NOT NULL,
    release_id varchar(68) NOT NULL,
    manifest_sha256 char(64) NOT NULL,
    repository_id varchar(64) NOT NULL COMMENT '人工本地仓库标签，不是远程地址',
    server_result_sha256 char(64) NOT NULL,
    metadata_json text NOT NULL COMMENT '有界规范元数据，无路径、源码或凭证',
    local_verified boolean NOT NULL COMMENT '管理员声明，不是平台实际制品复验',
    not_deployed boolean NOT NULL,
    note varchar(1000) NOT NULL COMMENT '人工核查说明，禁止记录凭证',
    del_flag int NOT NULL DEFAULT 0,
    create_by bigint NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint NOT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_plugin_artifact_request (tenant_id, task_id, create_by, request_id),
    UNIQUE KEY uk_sys_plugin_artifact_approval (tenant_id, task_id, review_id),
    KEY idx_sys_plugin_artifact_task (tenant_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件候选制品人工登记（只追加）';

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_public, menu_status, visible,
    perms, api_method, api_url, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, entry.name, menu.id, entry.type, entry.sort, 0, 1, 1, 'system:plugin:artifact:register',
       entry.method, entry.url, 1, NOW(), 1, NOW(), 1, 'pc', 0, 0
FROM sys_resource menu CROSS JOIN (
    SELECT '登记插件候选制品' AS name, 4 AS type, 22 AS sort, 'POST' AS method,
           '/system/plugin-task/*/artifact' AS url
    UNION ALL SELECT '登记候选制品', 3, 23, NULL, NULL
) entry
WHERE menu.tenant_id = 1 AND menu.client_code = 'pc' AND menu.resource_type = 2 AND menu.del_flag = 0
  AND menu.min_user_type = 0 AND menu.perms = 'system:plugin:view' AND menu.path = '/system/plugin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing WHERE existing.tenant_id = 1 AND existing.del_flag = 0
      AND existing.client_code = 'pc' AND existing.resource_type = entry.type
      AND (existing.perms = 'system:plugin:artifact:register'
           OR (entry.type = 4 AND existing.api_method = entry.method AND existing.api_url = entry.url))
  );

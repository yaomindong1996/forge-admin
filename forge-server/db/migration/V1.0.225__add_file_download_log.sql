-- 系统下载记录：本人可查最近下载，超级管理员与租户管理员可查本租户全部。
-- 入口在界面左下角，不新增菜单。
-- 回滚：删除本脚本新增的接口权限、字典和 sys_file_download_log 表；不要改历史迁移。

CREATE TABLE IF NOT EXISTS sys_file_download_log (
    id            BIGINT       NOT NULL COMMENT '主键',
    tenant_id     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    user_id       BIGINT       NOT NULL COMMENT '下载人',
    username      VARCHAR(64)  NULL COMMENT '下载人账号',
    file_id       VARCHAR(64)  NULL COMMENT '文件ID，导出类下载可为空',
    file_name     VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size     BIGINT       NULL COMMENT '字节数',
    source        VARCHAR(32)  NOT NULL COMMENT '来源：file 文件，export 导出',
    request_path  VARCHAR(255) NULL COMMENT '请求路径',
    client_ip     VARCHAR(64)  NULL COMMENT '客户端IP',
    create_by     BIGINT       NULL,
    create_time   DATETIME     NULL,
    create_dept   BIGINT       NULL,
    update_by     BIGINT       NULL,
    update_time   DATETIME     NULL,
    del_flag      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未删除',
    PRIMARY KEY (id),
    KEY idx_file_download_user_time (tenant_id, user_id, create_time),
    KEY idx_file_download_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件下载记录';

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, '文件下载来源', 'sys_file_download_source', 1, 1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type
    WHERE tenant_id = 1 AND dict_type = 'sys_file_download_source' AND del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, seed.sort, seed.label, seed.value, 'sys_file_download_source', seed.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 1 AS sort, '文件' AS label, 'file' AS `value`, 'info' AS style
    UNION ALL SELECT 2, '导出', 'export', 'success'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing
    WHERE existing.tenant_id = 1 AND existing.dict_type = 'sys_file_download_source'
      AND existing.dict_value = seed.`value` AND existing.del_flag = 0
);

SET @file_config_parent_id = (
    SELECT parent_id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/system/file-list' AND resource_type = 2 AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible,
    perms, api_method, api_url, keep_alive, always_show,
    remark, create_by, create_time, update_by, update_time, create_dept,
    client_code, min_user_type, del_flag
)
SELECT 1, '下载记录查询', @file_config_parent_id, 4, 22,
       0, '_self', 0, 1, 0,
       'system:file:downloadLog:list', 'GET', '/system/file/download-log/page', 0, 0,
       '左下角最近下载，不进菜单', 1, NOW(), 1, NOW(), 1,
       'pc', 2, 0
WHERE @file_config_parent_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND perms = 'system:file:downloadLog:list' AND del_flag = 0
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT role.tenant_id, role.id, resource.id, NOW()
FROM sys_role role
JOIN sys_resource resource
  ON resource.tenant_id = 1
 AND resource.del_flag = 0
 AND resource.perms = 'system:file:downloadLog:list'
WHERE role.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.role_id = role.id AND existing.resource_id = resource.id
  );

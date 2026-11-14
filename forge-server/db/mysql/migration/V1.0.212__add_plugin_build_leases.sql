-- 机器凭证只来自部署配置，默认禁用。审计表不含明文凭证、源码路径或部署命令。
-- 回滚：停用 worker 配置/隐藏构建入口，保留任务、占用和审计；不删除产物/业务库。
CREATE TABLE IF NOT EXISTS sys_plugin_build (
    id varchar(36) NOT NULL COMMENT '对应插件任务，一任务最多一次构建，不自动抢占重试',
    tenant_id bigint NOT NULL,
    worker_id varchar(64) NOT NULL,
    lease_hash char(64) NOT NULL COMMENT '私有随机租约摘要，不出普通API',
    phase varchar(32) NOT NULL,
    source_commit char(40) NOT NULL,
    image varchar(201) NOT NULL,
    started_time datetime NOT NULL,
    lease_expires_time datetime NOT NULL,
    deadline_time datetime NOT NULL,
    finished_time datetime DEFAULT NULL,
    result_json text DEFAULT NULL COMMENT '有界执行器报告，不是部署/独立复验结果',
    result_sha256 char(64) DEFAULT NULL,
    del_flag int NOT NULL DEFAULT 0,
    create_by bigint NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint NOT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_plugin_build_tenant (tenant_id, del_flag, started_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件构建租约及执行审计';

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, '插件构建阶段', 'sys_plugin_build_phase', 1, 1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE tenant_id = 1 AND dict_type = 'sys_plugin_build_phase' AND del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.sort, entry.label, entry.value, entry.type, entry.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0 FROM (
    SELECT 5 AS sort, '构建中' AS label, 'building' AS value, 'sys_plugin_task_status' AS type, 'info' AS style
    UNION ALL SELECT 6, '构建通过（待部署）', 'built', 'sys_plugin_task_status', 'success'
    UNION ALL SELECT 7, '构建失败', 'build_failed', 'sys_plugin_task_status', 'error'
    UNION ALL SELECT 1, '源码快照', 'source_snapshot', 'sys_plugin_build_phase', 'default'
    UNION ALL SELECT 2, '插件包核验', 'package_preflight', 'sys_plugin_build_phase', 'default'
    UNION ALL SELECT 3, '宿主源码预检', 'source_preflight', 'sys_plugin_build_phase', 'default'
    UNION ALL SELECT 4, '隔离容器构建', 'container_build', 'sys_plugin_build_phase', 'info'
    UNION ALL SELECT 5, '产物核验', 'artifact_verification', 'sys_plugin_build_phase', 'info'
) entry WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id = 1
    AND existing.dict_type = entry.type AND existing.dict_value = entry.value AND existing.del_flag = 0
);

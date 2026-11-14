-- Application integrations contain references only; no credentials, grants or demo data.
CREATE TABLE IF NOT EXISTS ai_application_integration (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL DEFAULT 1, application_id BIGINT NOT NULL,
  connection_id BIGINT NULL, revision BIGINT NOT NULL DEFAULT 0,
  create_by BIGINT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept BIGINT NULL, update_by BIGINT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uk_application_integration (tenant_id, application_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码应用集成引用';

CREATE TABLE IF NOT EXISTS ai_application_capability (
  id BIGINT NOT NULL, tenant_id BIGINT NOT NULL DEFAULT 1,
  application_id BIGINT NOT NULL, capability_id BIGINT NOT NULL,
  create_by BIGINT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept BIGINT NULL, update_by BIGINT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uk_application_capability (tenant_id, application_id, capability_id, del_flag),
  KEY idx_application_capability_reverse (tenant_id, capability_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码应用对外能力关联';

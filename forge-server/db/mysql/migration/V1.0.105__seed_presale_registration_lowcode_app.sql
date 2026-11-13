-- 门店预售登记低代码应用种子。
-- 只发布通用低代码元数据和运行表；企业微信/会员/商品/收款 sourceKey 的真实连接由部署环境配置。

CREATE TABLE IF NOT EXISTS `ps_presale_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `presale_no` varchar(64) NOT NULL COMMENT '预售单号',
  `sales_user_id` varchar(64) NOT NULL COMMENT '导购企业微信userid',
  `sales_user_name` varchar(128) DEFAULT NULL COMMENT '导购姓名',
  `staff_no` varchar(64) DEFAULT NULL COMMENT '工号',
  `store_id` varchar(64) DEFAULT NULL COMMENT '门店编码',
  `store_name` varchar(128) DEFAULT NULL COMMENT '门店名称',
  `member_phone` varchar(32) NOT NULL COMMENT '会员手机号',
  `member_id` varchar(64) DEFAULT NULL COMMENT '会员ID',
  `member_name` varchar(128) DEFAULT NULL COMMENT '会员姓名',
  `pay_method` varchar(32) NOT NULL DEFAULT 'STATIC_CODE' COMMENT '收款方式',
  `static_payment_no` varchar(128) DEFAULT NULL COMMENT '静态码单号',
  `static_payment_info` varchar(2048) DEFAULT NULL COMMENT '静态码收款信息快照',
  `cash_amount` bigint DEFAULT NULL COMMENT '现金金额，单位分',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '单据状态',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新人时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，1删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ps_presale_order_no` (`tenant_id`, `presale_no`),
  KEY `idx_ps_presale_order_user` (`tenant_id`, `sales_user_id`, `status`, `del_flag`),
  KEY `idx_ps_presale_order_member` (`tenant_id`, `member_phone`, `del_flag`),
  KEY `idx_ps_presale_order_status` (`tenant_id`, `status`, `create_time`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店预售登记主单';

CREATE TABLE IF NOT EXISTS `ps_presale_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `presale_order_id` bigint NOT NULL COMMENT '预售单ID',
  `barcode` varchar(128) NOT NULL COMMENT '商品条码',
  `product_name` varchar(256) DEFAULT NULL COMMENT '商品名称',
  `presale_quantity` int NOT NULL DEFAULT 1 COMMENT '预售数量',
  `picked_quantity` int NOT NULL DEFAULT 0 COMMENT '已提数量',
  `pending_quantity` int NOT NULL DEFAULT 1 COMMENT '待提数量',
  `returned_quantity` int NOT NULL DEFAULT 0 COMMENT '退货数量',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新人时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，1删除',
  PRIMARY KEY (`id`),
  KEY `idx_ps_presale_item_order` (`tenant_id`, `presale_order_id`, `del_flag`),
  KEY `idx_ps_presale_item_barcode` (`tenant_id`, `barcode`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店预售登记商品明细';

CREATE TABLE IF NOT EXISTS `ps_presale_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `presale_order_id` bigint NOT NULL COMMENT '预售单ID',
  `product_item_id` bigint NOT NULL COMMENT '商品明细ID',
  `product_name` varchar(256) DEFAULT NULL COMMENT '商品名称快照',
  `quantity` int NOT NULL COMMENT '操作数量',
  `action_type` varchar(32) NOT NULL COMMENT '操作类型：PICKUP/RETURN',
  `operator_name` varchar(128) DEFAULT NULL COMMENT '操作人姓名',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新人时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，1删除',
  PRIMARY KEY (`id`),
  KEY `idx_ps_presale_log_order` (`tenant_id`, `presale_order_id`, `operation_time`, `del_flag`),
  KEY `idx_ps_presale_log_item` (`tenant_id`, `product_item_id`, `operation_time`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店预售登记提货退货日志';

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '预售登记收款方式' dict_name, 'ps_presale_pay_method' dict_type,
         '预售登记收款方式：静态码或现金' remark
  UNION ALL SELECT 1, '预售登记单据状态', 'ps_presale_status',
         '预售登记单据状态：草稿、已提交、已完成、已取消'
  UNION ALL SELECT 1, '预售登记操作类型', 'ps_presale_operation_type',
         '预售登记提货和退货操作类型'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type existing
  WHERE existing.tenant_id = seed.tenant_id AND existing.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '静态码' dict_label, 'STATIC_CODE' dict_value,
         'ps_presale_pay_method' dict_type, 'primary' list_class, 'Y' is_default, '静态码收款' remark
  UNION ALL SELECT 1, 2, '现金', 'CASH', 'ps_presale_pay_method', 'warning', 'N', '现金收款'
  UNION ALL SELECT 1, 1, '草稿', 'DRAFT', 'ps_presale_status', 'default', 'Y', '未提交的预售单'
  UNION ALL SELECT 1, 2, '已提交', 'SUBMITTED', 'ps_presale_status', 'info', 'N', '可进行提货操作'
  UNION ALL SELECT 1, 3, '已完成', 'COMPLETED', 'ps_presale_status', 'success', 'N', '预售单已完成'
  UNION ALL SELECT 1, 4, '已取消', 'CANCELLED', 'ps_presale_status', 'error', 'N', '预售单已取消'
  UNION ALL SELECT 1, 1, '提货', 'PICKUP', 'ps_presale_operation_type', 'success', 'Y', '商品提货'
  UNION ALL SELECT 1, 2, '退货', 'RETURN', 'ps_presale_operation_type', 'warning', 'N', '商品退货'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data existing
  WHERE existing.tenant_id = seed.tenant_id
    AND existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);

INSERT INTO ai_lowcode_domain (id, tenant_id, parent_id, domain_code, domain_name, domain_desc, icon, sort,
                               status, menu_parent_id, table_prefix, config_key_prefix, default_app_type,
                               default_layout_type, default_table_mode, domain_schema, create_by, create_time,
                               update_by, update_time, create_dept)
SELECT 1900000000000001101, 1, 0, 'PRESALE_REGISTRATION', '预售登记',
       '门店预售登记、提货和退货闭环低代码业务域', 'ionicons5:CartOutline', 40, 'ENABLED', NULL,
       'ps_', 'ps_', 'MOBILE', 'master-detail-crud', 'EXISTING',
       JSON_OBJECT(
         'aiContext', JSON_OBJECT('description', '门店预售登记低代码业务域',
           'terms', JSON_ARRAY('预售单', '会员', '商品条码', '静态码', '提货', '退货'),
           'commonObjects', JSON_ARRAY('PS_PRESALE_ORDER', 'PS_PRESALE_ORDER_ITEM', 'PS_PRESALE_OPERATION_LOG'),
           'fieldNamingPreference', 'lowerCamel'),
         'naming', JSON_OBJECT('tablePrefix', 'ps_', 'configKeyPrefix', 'ps_', 'objectCodeStyle', 'upper_snake'),
         'defaults', JSON_OBJECT('appType', 'MOBILE', 'layoutType', 'master-detail-crud', 'tableMode', 'EXISTING')
       ),
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM ai_lowcode_domain existing
  WHERE existing.tenant_id = 1 AND existing.domain_code = 'PRESALE_REGISTRATION'
);

SET @ps_domain_id := (
  SELECT id FROM ai_lowcode_domain
  WHERE tenant_id = 1 AND domain_code = 'PRESALE_REGISTRATION' LIMIT 1
);

INSERT INTO ai_business_suite (id, tenant_id, suite_code, suite_name, icon, description, status, sort_order,
                               options, create_by, create_time, create_dept, update_by, update_time)
SELECT 1910000000000001101, 1, 'PRESALE_REGISTRATION', '门店预售登记', 'ionicons5:CartOutline',
       '面向门店导购的预售登记和提货退货应用', 1, 40,
       JSON_OBJECT('lowcodeApp', true, 'domainCode', 'PRESALE_REGISTRATION', 'mobileFirst', true),
       1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_suite existing
  WHERE existing.tenant_id = 1 AND existing.suite_code = 'PRESALE_REGISTRATION'
);

INSERT INTO ai_lowcode_model (id, tenant_id, domain_id, domain_code, model_code, model_name, model_desc,
                              status, tenant_enabled, master_data, model_schema, create_by, create_time,
                              create_dept, update_by, update_time)
SELECT seed.model_id, 1, @ps_domain_id, 'PRESALE_REGISTRATION', seed.model_code, seed.model_name,
       seed.model_desc, 'ENABLED', 1, seed.master_data, seed.model_schema, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1920000000000001101 model_id, 'ps_presale_order' model_code, '预售单' model_name,
         '预售登记主单模型' model_desc, 1 master_data,
         JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('code', 'PRESALE_REGISTRATION', 'name', '预售登记'),
           'object', JSON_OBJECT('code', 'PS_PRESALE_ORDER', 'name', '预售单'), 'appType', 'MOBILE',
           'tableMode', 'EXISTING', 'tableName', 'ps_presale_order', 'businessName', '预售单',
           'fields', JSON_ARRAY(
             JSON_OBJECT('field', 'id', 'columnName', 'id', 'label', 'ID', 'dataType', 'bigint', 'systemField', true, 'primaryKey', true, 'readonly', true, 'autoIncrement', true),
             JSON_OBJECT('field', 'tenantId', 'columnName', 'tenant_id', 'label', '租户ID', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'presaleNo', 'columnName', 'presale_no', 'label', '预售单号', 'dataType', 'varchar', 'length', 64, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 1),
             JSON_OBJECT('field', 'salesUserId', 'columnName', 'sales_user_id', 'label', '导购userid', 'dataType', 'varchar', 'length', 64, 'required', true, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 2),
             JSON_OBJECT('field', 'salesUserName', 'columnName', 'sales_user_name', 'label', '导购姓名', 'dataType', 'varchar', 'length', 128, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 3),
             JSON_OBJECT('field', 'staffNo', 'columnName', 'staff_no', 'label', '工号', 'dataType', 'varchar', 'length', 64, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 4),
             JSON_OBJECT('field', 'storeId', 'columnName', 'store_id', 'label', '门店编码', 'dataType', 'varchar', 'length', 64, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 5),
             JSON_OBJECT('field', 'storeName', 'columnName', 'store_name', 'label', '门店名称', 'dataType', 'varchar', 'length', 128, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 6),
             JSON_OBJECT('field', 'memberPhone', 'columnName', 'member_phone', 'label', '会员手机号', 'dataType', 'varchar', 'length', 32, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'sortOrder', 7),
             JSON_OBJECT('field', 'memberId', 'columnName', 'member_id', 'label', '会员ID', 'dataType', 'varchar', 'length', 64, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 8),
             JSON_OBJECT('field', 'memberName', 'columnName', 'member_name', 'label', '会员姓名', 'dataType', 'varchar', 'length', 128, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 9),
             JSON_OBJECT('field', 'payMethod', 'columnName', 'pay_method', 'label', '收款方式', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'STATIC_CODE', 'formVisible', true, 'componentType', 'dictSelect', 'dictType', 'ps_presale_pay_method', 'sortOrder', 10),
             JSON_OBJECT('field', 'staticPaymentNo', 'columnName', 'static_payment_no', 'label', '静态码单号', 'dataType', 'varchar', 'length', 128, 'formVisible', true, 'componentType', 'input', 'sortOrder', 11),
             JSON_OBJECT('field', 'staticPaymentInfo', 'columnName', 'static_payment_info', 'label', '收款信息', 'dataType', 'varchar', 'length', 2048, 'formVisible', true, 'componentType', 'textarea', 'readonly', true, 'sortOrder', 12),
             JSON_OBJECT('field', 'cashAmount', 'columnName', 'cash_amount', 'label', '现金金额', 'dataType', 'bigint', 'businessFieldType', 'MONEY', 'precision', 2, 'basicProps', JSON_OBJECT('min', 0.01), 'formVisible', true, 'componentType', 'money', 'sortOrder', 13),
             JSON_OBJECT('field', 'remark', 'columnName', 'remark', 'label', '备注', 'dataType', 'varchar', 'length', 500, 'formVisible', true, 'componentType', 'textarea', 'sortOrder', 14),
             JSON_OBJECT('field', 'status', 'columnName', 'status', 'label', '状态', 'dataType', 'varchar', 'length', 32, 'required', true, 'defaultValue', 'DRAFT', 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'dictSelect', 'dictType', 'ps_presale_status', 'readonly', true, 'sortOrder', 15),
             JSON_OBJECT('field', 'createBy', 'columnName', 'create_by', 'label', '创建人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createTime', 'columnName', 'create_time', 'label', '创建时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createDept', 'columnName', 'create_dept', 'label', '创建部门', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateBy', 'columnName', 'update_by', 'label', '更新人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateTime', 'columnName', 'update_time', 'label', '更新时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'delFlag', 'columnName', 'del_flag', 'label', '删除标志', 'dataType', 'bigint', 'systemField', true, 'readonly', true)
           ),
           'relations', JSON_ARRAY(
             JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_ORDER_ITEM', 'sourceField', 'id', 'targetField', 'presaleOrderId'),
             JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_OPERATION_LOG', 'sourceField', 'id', 'targetField', 'presaleOrderId')
           ),
           'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag'),
           'children', JSON_ARRAY('ps_presale_order_item', 'ps_presale_operation_log')) model_schema
  UNION ALL
  SELECT 1920000000000001102, 'ps_presale_order_item', '预售商品明细', '预售单商品条目', 0,
         JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('code', 'PRESALE_REGISTRATION', 'name', '预售登记'),
           'object', JSON_OBJECT('code', 'PS_PRESALE_ORDER_ITEM', 'name', '预售商品明细'), 'appType', 'MOBILE', 'tableMode', 'EXISTING', 'tableName', 'ps_presale_order_item', 'businessName', '预售商品明细',
           'fields', JSON_ARRAY(
             JSON_OBJECT('field', 'id', 'columnName', 'id', 'label', 'ID', 'dataType', 'bigint', 'systemField', true, 'primaryKey', true, 'readonly', true, 'autoIncrement', true),
             JSON_OBJECT('field', 'tenantId', 'columnName', 'tenant_id', 'label', '租户ID', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'presaleOrderId', 'columnName', 'presale_order_id', 'label', '预售单ID', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'barcode', 'columnName', 'barcode', 'label', '商品条码', 'dataType', 'varchar', 'length', 128, 'required', true, 'searchable', true, 'listVisible', true, 'formVisible', true, 'componentType', 'barcodeScanner', 'businessFieldType', 'TEXT', 'sortOrder', 1),
             JSON_OBJECT('field', 'productName', 'columnName', 'product_name', 'label', '商品名称', 'dataType', 'varchar', 'length', 256, 'required', true, 'listVisible', true, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 2),
             JSON_OBJECT('field', 'presaleQuantity', 'columnName', 'presale_quantity', 'label', '预售数量', 'dataType', 'int', 'required', true, 'defaultValue', 1, 'listVisible', true, 'formVisible', true, 'componentType', 'integer', 'sortOrder', 3),
             JSON_OBJECT('field', 'pickedQuantity', 'columnName', 'picked_quantity', 'label', '已提数量', 'dataType', 'int', 'required', true, 'defaultValue', 0, 'listVisible', true, 'formVisible', true, 'componentType', 'integer', 'readonly', true, 'sortOrder', 4),
             JSON_OBJECT('field', 'pendingQuantity', 'columnName', 'pending_quantity', 'label', '待提数量', 'dataType', 'int', 'required', true, 'defaultValue', 1, 'listVisible', true, 'formVisible', true, 'componentType', 'integer', 'readonly', true, 'sortOrder', 5,
               'formulaConfig', JSON_OBJECT('type', 'CALC', 'mode', 'STORED', 'expression', 'presaleQuantity-pickedQuantity+returnedQuantity', 'dependsOn', JSON_ARRAY('presaleQuantity', 'pickedQuantity', 'returnedQuantity'))),
             JSON_OBJECT('field', 'returnedQuantity', 'columnName', 'returned_quantity', 'label', '退货数量', 'dataType', 'int', 'required', true, 'defaultValue', 0, 'listVisible', true, 'formVisible', true, 'componentType', 'integer', 'readonly', true, 'sortOrder', 6),
             JSON_OBJECT('field', 'createBy', 'columnName', 'create_by', 'label', '创建人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createTime', 'columnName', 'create_time', 'label', '创建时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createDept', 'columnName', 'create_dept', 'label', '创建部门', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateBy', 'columnName', 'update_by', 'label', '更新人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateTime', 'columnName', 'update_time', 'label', '更新时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'delFlag', 'columnName', 'del_flag', 'label', '删除标志', 'dataType', 'bigint', 'systemField', true, 'readonly', true)
           ),
           'relations', JSON_ARRAY(), 'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag'))
  UNION ALL
  SELECT 1920000000000001103, 'ps_presale_operation_log', '预售操作日志', '预售单提货退货操作日志', 0,
         JSON_OBJECT('schemaVersion', 2, 'domain', JSON_OBJECT('code', 'PRESALE_REGISTRATION', 'name', '预售登记'),
           'object', JSON_OBJECT('code', 'PS_PRESALE_OPERATION_LOG', 'name', '预售操作日志'), 'appType', 'MOBILE', 'tableMode', 'EXISTING', 'tableName', 'ps_presale_operation_log', 'businessName', '预售操作日志',
           'fields', JSON_ARRAY(
             JSON_OBJECT('field', 'id', 'columnName', 'id', 'label', 'ID', 'dataType', 'bigint', 'systemField', true, 'primaryKey', true, 'readonly', true, 'autoIncrement', true),
             JSON_OBJECT('field', 'tenantId', 'columnName', 'tenant_id', 'label', '租户ID', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'presaleOrderId', 'columnName', 'presale_order_id', 'label', '预售单ID', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'productItemId', 'columnName', 'product_item_id', 'label', '商品明细ID', 'dataType', 'bigint', 'required', true, 'formVisible', true, 'componentType', 'input-number', 'readonly', true, 'sortOrder', 1),
             JSON_OBJECT('field', 'productName', 'columnName', 'product_name', 'label', '商品名称', 'dataType', 'varchar', 'length', 256, 'required', true, 'formVisible', true, 'componentType', 'select', 'readonly', true, 'sortOrder', 2),
             JSON_OBJECT('field', 'quantity', 'columnName', 'quantity', 'label', '操作数量', 'dataType', 'int', 'required', true, 'formVisible', true, 'componentType', 'integer', 'sortOrder', 3),
             JSON_OBJECT('field', 'actionType', 'columnName', 'action_type', 'label', '操作类型', 'dataType', 'varchar', 'length', 32, 'required', true, 'formVisible', true, 'componentType', 'dictSelect', 'dictType', 'ps_presale_operation_type', 'sortOrder', 4),
             JSON_OBJECT('field', 'operatorName', 'columnName', 'operator_name', 'label', '操作人', 'dataType', 'varchar', 'length', 128, 'formVisible', true, 'componentType', 'input', 'readonly', true, 'sortOrder', 5),
             JSON_OBJECT('field', 'operationTime', 'columnName', 'operation_time', 'label', '操作时间', 'dataType', 'datetime', 'formVisible', true, 'componentType', 'datetime', 'readonly', true, 'sortOrder', 6),
             JSON_OBJECT('field', 'createBy', 'columnName', 'create_by', 'label', '创建人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createTime', 'columnName', 'create_time', 'label', '创建时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'createDept', 'columnName', 'create_dept', 'label', '创建部门', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateBy', 'columnName', 'update_by', 'label', '更新人', 'dataType', 'bigint', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'updateTime', 'columnName', 'update_time', 'label', '更新时间', 'dataType', 'datetime', 'systemField', true, 'readonly', true),
             JSON_OBJECT('field', 'delFlag', 'columnName', 'del_flag', 'label', '删除标志', 'dataType', 'bigint', 'systemField', true, 'readonly', true)
           ),
           'relations', JSON_ARRAY(), 'policies', JSON_OBJECT('dataScope', 'TENANT', 'auditEnabled', true, 'primaryKeyStrategy', 'AUTO_INCREMENT', 'primaryKeyField', 'id', 'tenantField', 'tenantId', 'tenantColumn', 'tenant_id', 'logicDeleteField', 'delFlag', 'logicDeleteColumn', 'del_flag'))
) seed
WHERE @ps_domain_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ai_lowcode_model existing
    WHERE existing.tenant_id = 1 AND existing.domain_id = @ps_domain_id AND existing.model_code = seed.model_code
  );

INSERT INTO ai_business_object (id, tenant_id, suite_code, object_code, object_name, object_type, model_id,
                                model_code, display_field, icon, description, status, sort_order, options,
                                design_status, config_key, last_publish_time, last_publish_version,
                                designer_options, create_by, create_time, create_dept, update_by, update_time)
SELECT seed.object_id, 1, 'PRESALE_REGISTRATION', seed.object_code, seed.object_name, seed.object_type,
       seed.model_id, seed.model_code, seed.display_field, seed.icon, seed.description, 1, seed.sort_order,
       JSON_OBJECT('lowcodeApp', true, 'domainCode', 'PRESALE_REGISTRATION'), 'READY', seed.config_key,
       NULL, NULL, JSON_OBJECT('defaultPanel', 'form', 'documentManaged', true, 'seedKey', 'presale-registration-v1'), 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000001111 object_id, 1920000000000001101 model_id, 'PS_PRESALE_ORDER' object_code,
         '预售单' object_name, 'TRANSACTION' object_type, 'ps_presale_order' model_code, 'presaleNo' display_field,
         'ionicons5:CartOutline' icon, '预售登记主单，包含会员、收款和商品明细' description,
         10 sort_order, 'ps_presale_order' config_key
  UNION ALL SELECT 1910000000000001112, 1920000000000001102, 'PS_PRESALE_ORDER_ITEM', '预售商品明细', 'DETAIL', 'ps_presale_order_item', 'productName', 'ionicons5:BarcodeOutline', '预售单商品和提货数量', 20, NULL
  UNION ALL SELECT 1910000000000001113, 1920000000000001103, 'PS_PRESALE_OPERATION_LOG', '预售操作日志', 'DETAIL', 'ps_presale_operation_log', 'operationTime', 'ionicons5:TimeOutline', '预售提货退货操作流水', 30, NULL
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_object existing
  WHERE existing.tenant_id = 1 AND existing.suite_code = 'PRESALE_REGISTRATION'
    AND existing.object_code = seed.object_code AND existing.del_flag = 0
);

INSERT INTO ai_business_object_relation (id, tenant_id, suite_code, source_object_code, target_object_code,
                                         relation_type, relation_name, source_field_code, target_field_code,
                                         relation_config, description, status, sort_order, create_by, create_time,
                                         create_dept, update_by, update_time)
SELECT seed.id, 1, 'PRESALE_REGISTRATION', seed.source_object_code, seed.target_object_code, 'CHILD_LIST',
       seed.relation_name, 'id', seed.target_field_code,
       JSON_OBJECT('relationKey', seed.relation_key, 'saveMode', 'merge', 'inlineCreateEnabled', true, 'inlineEditEnabled', true),
       seed.description, 1, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1940000000000001101 id, 'PS_PRESALE_ORDER' source_object_code, 'PS_PRESALE_ORDER_ITEM' target_object_code,
         'presale_items' relation_name, 'presaleOrderId' target_field_code, 'presale_items' relation_key,
         '预售单商品明细关系' description, 10 sort_order
  UNION ALL SELECT 1940000000000001102, 'PS_PRESALE_ORDER', 'PS_PRESALE_OPERATION_LOG', 'operation_logs', 'presaleOrderId', 'operation_logs', '预售单操作日志关系', 20
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM ai_business_object_relation existing
  WHERE existing.tenant_id = 1 AND existing.suite_code = 'PRESALE_REGISTRATION'
    AND existing.source_object_code = seed.source_object_code
    AND existing.target_object_code = seed.target_object_code
    AND existing.relation_name = seed.relation_name
);

SET @ps_form_schema := JSON_OBJECT(
  'schemaVersion', 'form-first-v1',
  'formKey', 'ps_presale_order_form',
  'formName', '预售信息登记',
  'layout', JSON_OBJECT('gridColumns', 2, 'labelPlacement', 'left', 'labelAlign', 'right',
    'labelWidth', 104, 'size', 'medium', 'columnGap', 16, 'rowGap', 12, 'showFeedback', true),
  'settings', JSON_OBJECT(
    'governance', JSON_OBJECT(
      'fieldEvents', JSON_ARRAY(
        JSON_OBJECT(
          'id', 'wecom_user_store', 'name', '当前导购和门店', 'enabled', true,
          'trigger', 'FORM_LOAD', 'sourceField', '', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'wecom/user-store', 'debounceMs', 0, 'skipWhenEmpty', false,
          'clearTargetsOnTrigger', false,
          'paramMappings', JSON_ARRAY(
            JSON_OBJECT('param', 'userid', 'source', 'CONTEXT_PATH', 'path', 'currentUser.userId')
          ),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(
            JSON_OBJECT('from', 'userid', 'to', 'salesUserId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'userName', 'to', 'salesUserName', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'staffNo', 'to', 'staffNo', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'storeId', 'to', 'storeId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'storeName', 'to', 'storeName', 'whenMissing', 'CLEAR')
          ),
          'errorMode', 'MESSAGE', 'errorMessage', '未能读取当前导购和门店信息'
        ),
        JSON_OBJECT(
          'id', 'member_by_mobile', 'name', '手机号查询会员', 'enabled', true,
          'trigger', 'BLUR', 'sourceField', 'memberPhone', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'member/member-by-mobile', 'debounceMs', 300, 'skipWhenEmpty', true,
          'clearTargetsOnTrigger', true,
          'paramMappings', JSON_ARRAY(
            JSON_OBJECT('param', 'mobile', 'source', 'FORM_FIELD', 'field', 'memberPhone')
          ),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(
            JSON_OBJECT('from', 'memberId', 'to', 'memberId', 'whenMissing', 'CLEAR'),
            JSON_OBJECT('from', 'memberName', 'to', 'memberName', 'whenMissing', 'CLEAR')
          ),
          'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到会员信息', 'errorMessage', '会员查询失败'
        ),
        JSON_OBJECT(
          'id', 'payment_static_code', 'name', '静态码单号查询收款', 'enabled', true,
          'trigger', 'BLUR', 'sourceField', 'staticPaymentNo', 'sourceType', 'EXTERNAL_API',
          'sourceKey', 'payment/static-code', 'debounceMs', 300, 'skipWhenEmpty', true,
          'clearTargetsOnTrigger', true,
          'paramMappings', JSON_ARRAY(
            JSON_OBJECT('param', 'paymentNo', 'source', 'FORM_FIELD', 'field', 'staticPaymentNo')
          ),
          'resultMode', 'ROOT',
          'resultMappings', JSON_ARRAY(
            JSON_OBJECT('from', 'summary', 'to', 'staticPaymentInfo', 'whenMissing', 'CLEAR')
          ),
          'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到静态码收款记录', 'errorMessage', '收款记录查询失败'
        )
      )
    )
  ),
  'components', JSON_ARRAY(
    JSON_OBJECT('id', 'cmp_presale_no', 'label', '预售单号', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '保存后自动生成'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'presaleNo', 'columnName', 'presale_no'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_sales_user_id', 'label', '导购userid', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '由当前企业微信用户自动识别'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'requiredMessage', '未识别到导购账号', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '未识别到导购账号', 'trigger', JSON_ARRAY('change')))),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'salesUserId', 'columnName', 'sales_user_id'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_sales_user_name', 'label', '导购姓名', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'salesUserName', 'columnName', 'sales_user_name'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_staff_no', 'label', '工号', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staffNo', 'columnName', 'staff_no'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_store_id', 'label', '门店编码', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeId', 'columnName', 'store_id'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_store_name', 'label', '门店名称', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'storeName', 'columnName', 'store_name'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_member_phone', 'label', '会员手机号', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '请输入11位手机号', 'maxlength', 11, 'clearable', true), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入会员手机号',
        'rules', JSON_ARRAY(
          JSON_OBJECT('required', true, 'message', '请输入会员手机号', 'trigger', JSON_ARRAY('blur', 'change')),
          JSON_OBJECT('pattern', '^1[3-9][0-9]{9}$', 'message', '请输入正确的11位手机号', 'trigger', JSON_ARRAY('blur'))
        )),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberPhone', 'columnName', 'member_phone'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_member_id', 'label', '会员ID', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '查询后自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberId', 'columnName', 'member_id'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_member_name', 'label', '会员姓名', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '查询后自动回填'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'memberName', 'columnName', 'member_name'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_pay_method', 'label', '收款方式', 'componentKey', 'dictSelect',
      'props', JSON_OBJECT('dictType', 'ps_presale_pay_method', 'placeholder', '请选择收款方式', 'clearable', false),
      'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'requiredMessage', '请选择收款方式', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请选择收款方式', 'trigger', JSON_ARRAY('change')))),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'payMethod', 'columnName', 'pay_method'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_static_payment_no', 'label', '静态码单号', 'componentKey', 'input',
      'props', JSON_OBJECT('placeholder', '请输入静态码单号', 'clearable', true,
        'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_no', 'enabled', true, 'mode', 'all',
          'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')),
          'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
      'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入静态码单号', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请输入静态码单号', 'trigger', JSON_ARRAY('blur', 'change')))),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staticPaymentNo', 'columnName', 'static_payment_no'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_static_payment_info', 'label', '收款信息', 'componentKey', 'textarea',
      'props', JSON_OBJECT('placeholder', '查询后自动回填', 'rows', 2,
        'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_static_payment_info', 'enabled', true, 'mode', 'all',
          'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'STATIC_CODE')),
          'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
      'layout', JSON_OBJECT('span', 2), 'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'staticPaymentInfo', 'columnName', 'static_payment_info'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_cash_amount', 'label', '现金金额', 'componentKey', 'money',
      'props', JSON_OBJECT('placeholder', '请输入现金金额', 'min', 0.01, 'precision', 2, 'step', 0.01,
        'runtimeRules', JSON_ARRAY(JSON_OBJECT('id', 'show_cash_amount', 'enabled', true, 'mode', 'all',
          'conditions', JSON_ARRAY(JSON_OBJECT('source', 'formData', 'field', 'payMethod', 'operator', 'eq', 'value', 'CASH')),
          'effect', JSON_OBJECT('visible', true, 'whenUnmatched', 'hidden')))),
      'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'requiredMessage', '请输入现金金额', 'rules', JSON_ARRAY(JSON_OBJECT('required', true, 'message', '请输入现金金额', 'trigger', JSON_ARRAY('blur', 'change')))),
      'visibility', JSON_OBJECT('hidden', true, 'readonly', false),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'cashAmount', 'columnName', 'cash_amount'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_remark', 'label', '备注', 'componentKey', 'textarea',
      'props', JSON_OBJECT('placeholder', '请输入备注', 'rows', 3, 'maxlength', 500, 'showWordLimit', true),
      'layout', JSON_OBJECT('span', 2), 'validation', JSON_OBJECT('required', false, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', false),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'remark', 'columnName', 'remark'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT()),
    JSON_OBJECT('id', 'cmp_status', 'label', '状态', 'componentKey', 'dictSelect',
      'props', JSON_OBJECT('dictType', 'ps_presale_status', 'placeholder', '状态由系统维护'), 'layout', JSON_OBJECT('span', 1),
      'validation', JSON_OBJECT('required', true, 'rules', JSON_ARRAY()),
      'visibility', JSON_OBJECT('hidden', false, 'readonly', true),
      'fieldBinding', JSON_OBJECT('mode', 'field', 'source', 'field_asset', 'fieldCode', 'status', 'columnName', 'status'),
      'children', JSON_ARRAY(), 'advancedProps', JSON_OBJECT())
  )
);

SET @ps_product_field_events := JSON_ARRAY(
  JSON_OBJECT(
    'id', 'product_scan_lookup', 'name', '扫码查询商品', 'enabled', true,
    'trigger', 'SCAN_COMPLETE', 'sourceField', 'barcode', 'sourceType', 'EXTERNAL_API',
    'sourceKey', 'product/product-by-barcode', 'debounceMs', 0, 'skipWhenEmpty', true,
    'clearTargetsOnTrigger', true,
    'paramMappings', JSON_ARRAY(JSON_OBJECT('param', 'barcode', 'source', 'FORM_FIELD', 'field', 'barcode')),
    'resultMode', 'ROOT',
    'resultMappings', JSON_ARRAY(JSON_OBJECT('from', 'productName', 'to', 'productName', 'whenMissing', 'CLEAR')),
    'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到商品', 'errorMessage', '商品查询失败'
  ),
  JSON_OBJECT(
    'id', 'product_manual_lookup', 'name', '条码输入查询商品', 'enabled', true,
    'trigger', 'BLUR', 'sourceField', 'barcode', 'sourceType', 'EXTERNAL_API',
    'sourceKey', 'product/product-by-barcode', 'debounceMs', 300, 'skipWhenEmpty', true,
    'clearTargetsOnTrigger', true,
    'paramMappings', JSON_ARRAY(JSON_OBJECT('param', 'barcode', 'source', 'FORM_FIELD', 'field', 'barcode')),
    'resultMode', 'ROOT',
    'resultMappings', JSON_ARRAY(JSON_OBJECT('from', 'productName', 'to', 'productName', 'whenMissing', 'CLEAR')),
    'errorMode', 'MESSAGE', 'notFoundMessage', '未查询到商品', 'errorMessage', '商品查询失败'
  )
);

SET @ps_submit_action := JSON_OBJECT(
  'actionCode', 'submit_presale', 'actionName', '提交预售单', 'actionPosition', 'ROW',
  'actionType', 'COMMAND', 'permission', '', 'confirmRequired', true,
  'successMessage', '预售单已提交', 'failureMessage', '预售单提交失败', 'status', 1, 'sortOrder', 5,
  'actionConfig', JSON_OBJECT(
    'triggerScene', 'MANUAL', 'executionMode', 'LOCAL_TRANSACTION', 'inputSchema', JSON_ARRAY(),
    'steps', JSON_ARRAY(
      JSON_OBJECT('stepCode', 'submit_presale_status', 'stepName', '提交预售单',
        'stepType', 'TRANSITION_STATUS', 'sortOrder', 10, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order',
          'targetRecordIdField', 'record.id', 'statusField', 'status',
          'fromValue', 'DRAFT', 'toValue', 'SUBMITTED'))
    )
  )
);

SET @ps_pickup_action := JSON_OBJECT(
  'actionCode', 'record_pickup', 'actionName', '登记提货', 'actionPosition', 'CHILD_ROW',
  'actionType', 'COMMAND', 'permission', '', 'confirmRequired', true,
  'successMessage', '提货登记成功', 'failureMessage', '提货登记失败', 'status', 1, 'sortOrder', 10,
  'actionConfig', JSON_OBJECT(
    'triggerScene', 'MANUAL', 'relationKey', 'presale_items', 'executionMode', 'LOCAL_TRANSACTION',
    'inputSchema', JSON_ARRAY(JSON_OBJECT('name', 'quantity', 'label', '提货数量', 'type', 'INTEGER', 'required', true, 'min', 1, 'max', 999999)),
    'steps', JSON_ARRAY(
      JSON_OBJECT('stepCode', 'assert_pickup_parent', 'stepName', '校验预售单状态', 'stepType', 'ASSERT_RECORD', 'sortOrder', 10, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order', 'targetRecordIdField', 'parentRecord.id', 'expectedValues', JSON_OBJECT('status', 'SUBMITTED'))),
      JSON_OBJECT('stepCode', 'assert_pickup_item', 'stepName', '校验商品归属和待提数量', 'stepType', 'ASSERT_RECORD', 'sortOrder', 20, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order_item', 'targetRecordIdField', 'record.id',
          'expectedFieldMappings', JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
          'numericConstraints', JSON_ARRAY(JSON_OBJECT('field', 'pendingQuantity', 'operator', 'gte', 'sourceType', 'form', 'sourceField', 'quantity')))),
      JSON_OBJECT('stepCode', 'create_pickup_log', 'stepName', '创建提货日志', 'stepType', 'CREATE_RECORD', 'sortOrder', 30, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_operation_log',
          'fieldMappings', JSON_ARRAY(
            JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'),
            JSON_OBJECT('targetField', 'productItemId', 'sourceType', 'record', 'sourceField', 'id'),
            JSON_OBJECT('targetField', 'productName', 'sourceType', 'record', 'sourceField', 'productName'),
            JSON_OBJECT('targetField', 'quantity', 'sourceType', 'form', 'sourceField', 'quantity'),
            JSON_OBJECT('targetField', 'operatorName', 'sourceType', 'system', 'sourceField', 'realName')
          ), 'staticValues', JSON_OBJECT('actionType', 'PICKUP'))),
      JSON_OBJECT('stepCode', 'adjust_pickup_quantity', 'stepName', '调整提货数量', 'stepType', 'ADJUST_NUMBER', 'sortOrder', 40, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order_item', 'targetRecordIdField', 'record.id',
          'expectedFieldMappings', JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
          'adjustments', JSON_ARRAY(
            JSON_OBJECT('targetField', 'pickedQuantity', 'sourceType', 'form', 'sourceField', 'quantity', 'operator', 'ADD', 'min', 0),
            JSON_OBJECT('targetField', 'pendingQuantity', 'sourceType', 'form', 'sourceField', 'quantity', 'operator', 'SUBTRACT', 'min', 0)
          )))
    )
  )
);

SET @ps_return_action := JSON_OBJECT(
  'actionCode', 'record_return', 'actionName', '登记退货', 'actionPosition', 'CHILD_ROW',
  'actionType', 'COMMAND', 'permission', '', 'confirmRequired', true,
  'successMessage', '退货登记成功', 'failureMessage', '退货登记失败', 'status', 1, 'sortOrder', 20,
  'actionConfig', JSON_OBJECT(
    'triggerScene', 'MANUAL', 'relationKey', 'presale_items', 'executionMode', 'LOCAL_TRANSACTION',
    'inputSchema', JSON_ARRAY(JSON_OBJECT('name', 'quantity', 'label', '退货数量', 'type', 'INTEGER', 'required', true, 'min', 1, 'max', 999999)),
    'steps', JSON_ARRAY(
      JSON_OBJECT('stepCode', 'assert_return_parent', 'stepName', '校验预售单状态', 'stepType', 'ASSERT_RECORD', 'sortOrder', 10, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order', 'targetRecordIdField', 'parentRecord.id', 'expectedValues', JSON_OBJECT('status', 'SUBMITTED'))),
      JSON_OBJECT('stepCode', 'assert_return_item', 'stepName', '校验商品归属和已提数量', 'stepType', 'ASSERT_RECORD', 'sortOrder', 20, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order_item', 'targetRecordIdField', 'record.id',
          'expectedFieldMappings', JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
          'numericConstraints', JSON_ARRAY(JSON_OBJECT('field', 'pickedQuantity', 'operator', 'gte', 'sourceType', 'form', 'sourceField', 'quantity')))),
      JSON_OBJECT('stepCode', 'create_return_log', 'stepName', '创建退货日志', 'stepType', 'CREATE_RECORD', 'sortOrder', 30, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_operation_log',
          'fieldMappings', JSON_ARRAY(
            JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id'),
            JSON_OBJECT('targetField', 'productItemId', 'sourceType', 'record', 'sourceField', 'id'),
            JSON_OBJECT('targetField', 'productName', 'sourceType', 'record', 'sourceField', 'productName'),
            JSON_OBJECT('targetField', 'quantity', 'sourceType', 'form', 'sourceField', 'quantity'),
            JSON_OBJECT('targetField', 'operatorName', 'sourceType', 'system', 'sourceField', 'realName')
          ), 'staticValues', JSON_OBJECT('actionType', 'RETURN'))),
      JSON_OBJECT('stepCode', 'adjust_return_quantity', 'stepName', '调整退货数量', 'stepType', 'ADJUST_NUMBER', 'sortOrder', 40, 'rollbackOnFailure', true,
        'stepConfig', JSON_OBJECT('targetConfigKey', 'ps_presale_order_item', 'targetRecordIdField', 'record.id',
          'expectedFieldMappings', JSON_ARRAY(JSON_OBJECT('targetField', 'presaleOrderId', 'sourceType', 'parent', 'sourceField', 'id')),
          'adjustments', JSON_ARRAY(
            JSON_OBJECT('targetField', 'returnedQuantity', 'sourceType', 'form', 'sourceField', 'quantity', 'operator', 'ADD', 'min', 0),
            JSON_OBJECT('targetField', 'pendingQuantity', 'sourceType', 'form', 'sourceField', 'quantity', 'operator', 'ADD', 'min', 0)
          )))
    )
  )
);

SET @ps_item_fields := JSON_ARRAY(
  JSON_OBJECT('field', 'barcode', 'sourceField', 'barcode', 'fieldRef', 'ps_presale_order_item__barcode',
    'columnName', 'barcode', 'label', '商品条码', 'type', 'barcodeScanner', 'componentType', 'barcodeScanner',
    'dataType', 'varchar', 'required', true, 'formVisible', true, 'listVisible', true, 'width', 180,
    'props', JSON_OBJECT('allowManualInput', true, 'buttonText', '扫码', 'timeoutMs', 30000, 'formats', JSON_ARRAY('CODE_128', 'EAN_13', 'EAN_8', 'UPC_A'))),
  JSON_OBJECT('field', 'productName', 'sourceField', 'productName', 'fieldRef', 'ps_presale_order_item__productName',
    'columnName', 'product_name', 'label', '商品名称', 'type', 'input', 'componentType', 'input',
    'dataType', 'varchar', 'required', true, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 220),
  JSON_OBJECT('field', 'presaleQuantity', 'sourceField', 'presaleQuantity', 'fieldRef', 'ps_presale_order_item__presaleQuantity',
    'columnName', 'presale_quantity', 'label', '预售数量', 'type', 'integer', 'componentType', 'integer',
    'dataType', 'int', 'required', true, 'defaultValue', 1, 'min', 1, 'formVisible', true, 'listVisible', true, 'width', 100),
  JSON_OBJECT('field', 'pickedQuantity', 'sourceField', 'pickedQuantity', 'fieldRef', 'ps_presale_order_item__pickedQuantity',
    'columnName', 'picked_quantity', 'label', '已提数量', 'type', 'integer', 'componentType', 'integer',
    'dataType', 'int', 'required', true, 'defaultValue', 0, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 90),
  JSON_OBJECT('field', 'pendingQuantity', 'sourceField', 'pendingQuantity', 'fieldRef', 'ps_presale_order_item__pendingQuantity',
    'columnName', 'pending_quantity', 'label', '待提数量', 'type', 'integer', 'componentType', 'integer',
    'dataType', 'int', 'required', true, 'defaultValue', 1, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 90),
  JSON_OBJECT('field', 'returnedQuantity', 'sourceField', 'returnedQuantity', 'fieldRef', 'ps_presale_order_item__returnedQuantity',
    'columnName', 'returned_quantity', 'label', '退货数量', 'type', 'integer', 'componentType', 'integer',
    'dataType', 'int', 'required', true, 'defaultValue', 0, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 90)
);

SET @ps_log_fields := JSON_ARRAY(
  JSON_OBJECT('field', 'productItemId', 'sourceField', 'productItemId', 'fieldRef', 'ps_presale_operation_log__productItemId',
    'columnName', 'product_item_id', 'label', '商品明细ID', 'type', 'number', 'componentType', 'number',
    'dataType', 'bigint', 'required', true, 'readonly', true, 'formVisible', false, 'listVisible', false),
  JSON_OBJECT('field', 'productName', 'sourceField', 'productName', 'fieldRef', 'ps_presale_operation_log__productName',
    'columnName', 'product_name', 'label', '商品名称', 'type', 'select', 'componentType', 'select',
    'dataType', 'varchar', 'required', true, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 220,
    'props', JSON_OBJECT('optionSource', JSON_OBJECT('type', 'CURRENT_CHILDREN', 'relationKey', 'presale_items',
      'valueField', 'id', 'labelField', 'productName', 'persistedOnly', true))),
  JSON_OBJECT('field', 'quantity', 'sourceField', 'quantity', 'fieldRef', 'ps_presale_operation_log__quantity',
    'columnName', 'quantity', 'label', '数量', 'type', 'integer', 'componentType', 'integer',
    'dataType', 'int', 'required', true, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 90),
  JSON_OBJECT('field', 'actionType', 'sourceField', 'actionType', 'fieldRef', 'ps_presale_operation_log__actionType',
    'columnName', 'action_type', 'label', '操作类型', 'type', 'dictSelect', 'componentType', 'dictSelect',
    'dataType', 'varchar', 'dictType', 'ps_presale_operation_type', 'required', true, 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 100),
  JSON_OBJECT('field', 'operatorName', 'sourceField', 'operatorName', 'fieldRef', 'ps_presale_operation_log__operatorName',
    'columnName', 'operator_name', 'label', '操作人', 'type', 'input', 'componentType', 'input',
    'dataType', 'varchar', 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 120),
  JSON_OBJECT('field', 'operationTime', 'sourceField', 'operationTime', 'fieldRef', 'ps_presale_operation_log__operationTime',
    'columnName', 'operation_time', 'label', '操作时间', 'type', 'datetime', 'componentType', 'datetime',
    'dataType', 'datetime', 'readonly', true, 'formVisible', true, 'listVisible', true, 'width', 170)
);

UPDATE ai_business_object
SET designer_options = JSON_SET(
      COALESCE(designer_options, JSON_OBJECT()),
      '$.defaultPanel', 'form',
      '$.documentManaged', true,
      '$.formDesignerSchema', JSON_EXTRACT(@ps_form_schema, '$'),
      '$.actions', JSON_ARRAY(JSON_EXTRACT(@ps_submit_action, '$'), JSON_EXTRACT(@ps_pickup_action, '$'), JSON_EXTRACT(@ps_return_action, '$'))
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND suite_code = 'PRESALE_REGISTRATION'
  AND object_code = 'PS_PRESALE_ORDER'
  AND del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(designer_options, '$.seedKey')) = 'presale-registration-v1';

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, search_schema,
                            columns_schema, edit_schema, api_config, options, mode, build_mode, status,
                            publish_status, menu_name, menu_parent_id, menu_sort, menu_resource_id,
                            layout_type, model_schema, page_schema, draft_version, published_version,
                            publish_time, publish_by, domain_id, domain_code, object_code, object_name,
                            runtime_table_name, primary_key_field, primary_key_column, primary_key_type,
                            tenant_strategy, audit_strategy, logic_delete_strategy,
                            create_by, create_time, create_dept, update_by, update_time)
SELECT 1930000000000001101, 1, 'ps_presale_order', 'ps_presale_order', '门店预售登记主单', '预售信息登记',
       JSON_ARRAY(
         JSON_OBJECT('field', 'presaleNo', 'label', '预售单号', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'memberPhone', 'label', '会员手机号', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'storeName', 'label', '门店名称', 'type', 'input', 'queryType', 'like'),
         JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'dictSelect', 'dictType', 'ps_presale_status', 'queryType', 'eq')
       ),
       JSON_ARRAY(
         JSON_OBJECT('prop', 'presaleNo', 'label', '预售单号', 'width', 170),
         JSON_OBJECT('prop', 'storeName', 'label', '门店', 'width', 150),
         JSON_OBJECT('prop', 'salesUserName', 'label', '导购', 'width', 110),
         JSON_OBJECT('prop', 'memberPhone', 'label', '会员手机号', 'width', 130),
         JSON_OBJECT('prop', 'memberName', 'label', '会员姓名', 'width', 120),
         JSON_OBJECT('prop', 'payMethod', 'label', '收款方式', 'width', 100, 'dictType', 'ps_presale_pay_method'),
         JSON_OBJECT('prop', 'status', 'label', '状态', 'width', 100, 'dictType', 'ps_presale_status'),
         JSON_OBJECT('prop', 'createTime', 'label', '登记时间', 'width', 170)
       ),
       JSON_ARRAY(
         JSON_OBJECT('field', 'presaleNo', 'label', '预售单号', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'salesUserId', 'label', '导购userid', 'type', 'input', 'required', true, 'readonly', true),
         JSON_OBJECT('field', 'salesUserName', 'label', '导购姓名', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'staffNo', 'label', '工号', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'storeId', 'label', '门店编码', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'storeName', 'label', '门店名称', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'memberPhone', 'label', '会员手机号', 'type', 'input', 'required', true),
         JSON_OBJECT('field', 'memberId', 'label', '会员ID', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'memberName', 'label', '会员姓名', 'type', 'input', 'readonly', true),
         JSON_OBJECT('field', 'payMethod', 'label', '收款方式', 'type', 'dictSelect', 'dictType', 'ps_presale_pay_method', 'required', true, 'defaultValue', 'STATIC_CODE'),
         JSON_OBJECT('field', 'staticPaymentNo', 'label', '静态码单号', 'type', 'input'),
         JSON_OBJECT('field', 'staticPaymentInfo', 'label', '收款信息', 'type', 'textarea', 'readonly', true),
         JSON_OBJECT('field', 'cashAmount', 'label', '现金金额', 'type', 'money', 'min', 0.01, 'precision', 2, 'step', 0.01),
         JSON_OBJECT('field', 'remark', 'label', '备注', 'type', 'textarea'),
         JSON_OBJECT('field', 'status', 'label', '状态', 'type', 'dictSelect', 'dictType', 'ps_presale_status', 'readonly', true, 'defaultValue', 'DRAFT')
       ),
       JSON_OBJECT(
         'list', 'get@/ai/crud/ps_presale_order/page',
         'detail', 'get@/ai/crud/ps_presale_order/:id',
         'create', 'post@/ai/crud/ps_presale_order',
         'update', 'put@/ai/crud/ps_presale_order',
         'delete', 'delete@/ai/crud/ps_presale_order/:id',
         'importTemplate', 'get@/ai/crud/ps_presale_order/import-template',
         'import', 'post@/ai/crud/ps_presale_order/import',
         'export', 'post@/ai/crud/ps_presale_order/export'
       ),
       JSON_OBJECT(
         'layoutType', 'master-detail-crud', 'rowKey', 'id', 'modalWidth', '1280px',
         'editGridCols', 2, 'showImport', false, 'showExport', true, 'enableDetail', true,
         'formDesignerSchema', JSON_EXTRACT(@ps_form_schema, '$'),
         'actions', JSON_ARRAY(JSON_EXTRACT(@ps_submit_action, '$'), JSON_EXTRACT(@ps_pickup_action, '$'), JSON_EXTRACT(@ps_return_action, '$')),
         'rowActions', JSON_ARRAY(
           JSON_OBJECT('key', 'submit_presale', 'actionCode', 'submit_presale', 'label', '提交',
             'position', 'row', 'actionType', 'command', 'objectCode', 'PS_PRESALE_ORDER',
             'type', 'success', 'visible', true, 'displayCondition', 'status == DRAFT',
             'confirmText', '确认提交当前预售单？', 'successMessage', '预售单已提交')
         ),
         'masterDetailConfig', JSON_OBJECT(
           'primary', JSON_OBJECT('modelCode', 'ps_presale_order', 'modelName', '预售单', 'tableName', 'ps_presale_order', 'keyField', 'id'),
           'children', JSON_ARRAY(
             JSON_OBJECT(
               'key', 'presale_items', 'relationKey', 'presale_items', 'modelCode', 'ps_presale_order_item',
               'modelName', '预售商品明细', 'tableName', 'ps_presale_order_item', 'relationType', 'ONE_TO_MANY',
               'sourceField', 'presaleOrderId', 'targetField', 'id', 'showInCreate', true, 'showInEdit', true,
               'showInDetail', true, 'saveMode', 'merge', 'inlineCreateEnabled', true, 'inlineEditEnabled', true,
               'tabTitle', '预售商品', 'relationName', '预售商品',
               'fieldEvents', JSON_EXTRACT(@ps_product_field_events, '$'), 'fields', JSON_EXTRACT(@ps_item_fields, '$'),
               'rowActions', JSON_ARRAY(
                 JSON_OBJECT('key', 'record_pickup', 'actionCode', 'record_pickup', 'label', '提货', 'position', 'childRow', 'actionType', 'command', 'relationKey', 'presale_items', 'type', 'success', 'visible', true),
                 JSON_OBJECT('key', 'record_return', 'actionCode', 'record_return', 'label', '退货', 'position', 'childRow', 'actionType', 'command', 'relationKey', 'presale_items', 'type', 'warning', 'visible', true)
               )
             ),
             JSON_OBJECT(
               'key', 'operation_logs', 'relationKey', 'operation_logs', 'modelCode', 'ps_presale_operation_log',
               'modelName', '提货退货日志', 'tableName', 'ps_presale_operation_log', 'relationType', 'ONE_TO_MANY',
               'sourceField', 'presaleOrderId', 'targetField', 'id', 'showInCreate', false, 'showInEdit', false,
               'showInDetail', true, 'saveMode', 'merge', 'inlineCreateEnabled', false, 'inlineEditEnabled', false,
               'tabTitle', '操作日志', 'relationName', '操作日志', 'readonly', true,
               'fields', JSON_EXTRACT(@ps_log_fields, '$'), 'rowActions', JSON_ARRAY()
             )
           )
         )
       ),
       'CONFIG', 'LOWCODE', '0', 'PUBLISHED', '预售信息登记', NULL, 10, NULL,
       'master-detail-crud', model.model_schema,
       JSON_OBJECT(
         'layoutType', 'master-detail-crud', 'primaryModelCode', 'ps_presale_order',
         'modelRefs', JSON_ARRAY(
           JSON_OBJECT('modelCode', 'ps_presale_order', 'modelName', '预售单', 'tableName', 'ps_presale_order', 'primary', true,
             'relations', JSON_ARRAY(
               JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_ORDER_ITEM', 'sourceField', 'id', 'targetField', 'presaleOrderId', 'displayField', 'productName'),
               JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_OPERATION_LOG', 'sourceField', 'id', 'targetField', 'presaleOrderId', 'displayField', 'operationTime')
             ), 'props', JSON_OBJECT(), 'fields', JSON_ARRAY()),
           JSON_OBJECT('modelCode', 'ps_presale_order_item', 'modelName', '预售商品明细', 'tableName', 'ps_presale_order_item', 'primary', false,
             'relations', JSON_ARRAY(JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_ORDER', 'sourceField', 'presaleOrderId', 'targetField', 'id', 'displayField', 'productName')),
             'props', JSON_OBJECT('relationKey', 'presale_items', 'saveMode', 'merge', 'inlineCreateEnabled', true, 'inlineEditEnabled', true, 'showInDetail', true,
               'tabTitle', '预售商品', 'relationName', '预售商品', 'fieldEvents', JSON_EXTRACT(@ps_product_field_events, '$'),
               'rowActions', JSON_ARRAY(
                 JSON_OBJECT('key', 'record_pickup', 'actionCode', 'record_pickup', 'label', '提货', 'position', 'childRow', 'actionType', 'command', 'relationKey', 'presale_items', 'type', 'success', 'visible', true),
                 JSON_OBJECT('key', 'record_return', 'actionCode', 'record_return', 'label', '退货', 'position', 'childRow', 'actionType', 'command', 'relationKey', 'presale_items', 'type', 'warning', 'visible', true)
               )),
             'fields', JSON_EXTRACT(@ps_item_fields, '$')),
           JSON_OBJECT('modelCode', 'ps_presale_operation_log', 'modelName', '提货退货日志', 'tableName', 'ps_presale_operation_log', 'primary', false,
             'relations', JSON_ARRAY(JSON_OBJECT('relationType', 'ONE_TO_MANY', 'targetObjectCode', 'PS_PRESALE_ORDER', 'sourceField', 'presaleOrderId', 'targetField', 'id', 'displayField', 'operationTime')),
             'props', JSON_OBJECT('relationKey', 'operation_logs', 'saveMode', 'merge', 'inlineCreateEnabled', false, 'inlineEditEnabled', false, 'showInDetail', true, 'readonly', true, 'tabTitle', '操作日志', 'relationName', '操作日志'),
             'fields', JSON_EXTRACT(@ps_log_fields, '$'))
         ),
         'zones', JSON_ARRAY(
           JSON_OBJECT('key', 'search', 'type', 'search', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'table', 'type', 'table', 'props', JSON_OBJECT(
             'showImport', false, 'showExport', true,
             'customActions', JSON_ARRAY(
               JSON_OBJECT('key', 'submit_presale', 'actionCode', 'submit_presale', 'label', '提交',
                 'position', 'row', 'actionType', 'command', 'objectCode', 'PS_PRESALE_ORDER',
                 'type', 'success', 'displayCondition', 'status == DRAFT',
                 'confirmText', '确认提交当前预售单？', 'successMessage', '预售单已提交')
             ))),
           JSON_OBJECT('key', 'edit', 'type', 'form', 'props', JSON_OBJECT('editGridCols', 2, 'formDesignerSchema', JSON_EXTRACT(@ps_form_schema, '$'))),
           JSON_OBJECT('key', 'detail', 'type', 'detail', 'props', JSON_OBJECT())
         )
       ),
       1, 1, NOW(), 1, @ps_domain_id, 'PRESALE_REGISTRATION', 'PS_PRESALE_ORDER', '预售单',
       'ps_presale_order', 'id', 'id', 'bigint',
       JSON_OBJECT('mode', 'FORGE_TENANT_ID', 'columnName', 'tenant_id'),
       JSON_OBJECT('mode', 'FORGE_COLUMNS', 'createByColumn', 'create_by', 'createTimeColumn', 'create_time', 'createDeptColumn', 'create_dept', 'updateByColumn', 'update_by', 'updateTimeColumn', 'update_time'),
       JSON_OBJECT('mode', 'DEL_FLAG', 'columnName', 'del_flag', 'activeValue', '0', 'deletedValue', '1'),
       1, NOW(), 1, 1, NOW()
FROM ai_lowcode_model model
WHERE model.tenant_id = 1 AND model.domain_id = @ps_domain_id AND model.model_code = 'ps_presale_order'
  AND NOT EXISTS (
    SELECT 1 FROM ai_crud_config existing
    WHERE existing.tenant_id = 1 AND existing.config_key = 'ps_presale_order' AND existing.del_flag = 0
  )
LIMIT 1;

INSERT INTO ai_crud_config (id, tenant_id, config_key, table_name, table_comment, app_name, search_schema,
                            columns_schema, edit_schema, api_config, options, mode, build_mode, status,
                            publish_status, menu_name, menu_sort, layout_type, model_schema, page_schema,
                            draft_version, published_version, publish_time, publish_by, domain_id, domain_code,
                            object_code, object_name, runtime_table_name, primary_key_field, primary_key_column,
                            primary_key_type, tenant_strategy, audit_strategy, logic_delete_strategy,
                            create_by, create_time, create_dept, update_by, update_time)
SELECT seed.config_id, 1, seed.config_key, seed.table_name, seed.table_comment, seed.app_name,
       seed.search_schema, seed.columns_schema, seed.edit_schema,
       JSON_OBJECT(
         'list', CONCAT('get@/ai/crud/', seed.config_key, '/page'),
         'detail', CONCAT('get@/ai/crud/', seed.config_key, '/:id'),
         'create', CONCAT('post@/ai/crud/', seed.config_key),
         'update', CONCAT('put@/ai/crud/', seed.config_key),
         'delete', CONCAT('delete@/ai/crud/', seed.config_key, '/:id')
       ),
       JSON_OBJECT('layoutType', 'simple-crud', 'rowKey', 'id', 'modalWidth', '1040px', 'editGridCols', 2,
         'showImport', false, 'showExport', true, 'enableDetail', true),
       'CONFIG', 'LOWCODE', '0', 'PUBLISHED', seed.app_name, seed.sort_order, 'simple-crud', model.model_schema,
       JSON_OBJECT('layoutType', 'simple-crud', 'primaryModelCode', seed.config_key,
         'zones', JSON_ARRAY(
           JSON_OBJECT('key', 'search', 'type', 'search', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'table', 'type', 'table', 'props', JSON_OBJECT()),
           JSON_OBJECT('key', 'edit', 'type', 'form', 'props', JSON_OBJECT('editGridCols', 2)),
           JSON_OBJECT('key', 'detail', 'type', 'detail', 'props', JSON_OBJECT())
         )),
       1, 1, NOW(), 1, @ps_domain_id, 'PRESALE_REGISTRATION', seed.object_code, seed.object_name,
       seed.table_name, 'id', 'id', 'bigint',
       JSON_OBJECT('mode', 'FORGE_TENANT_ID', 'columnName', 'tenant_id'),
       JSON_OBJECT('mode', 'FORGE_COLUMNS', 'createByColumn', 'create_by', 'createTimeColumn', 'create_time', 'createDeptColumn', 'create_dept', 'updateByColumn', 'update_by', 'updateTimeColumn', 'update_time'),
       JSON_OBJECT('mode', 'DEL_FLAG', 'columnName', 'del_flag', 'activeValue', '0', 'deletedValue', '1'),
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1930000000000001102 config_id, 'ps_presale_order_item' config_key, 'ps_presale_order_item' table_name,
         '门店预售登记商品明细' table_comment, '预售商品明细' app_name, 'PS_PRESALE_ORDER_ITEM' object_code,
         '预售商品明细' object_name, 20 sort_order,
         JSON_ARRAY(JSON_OBJECT('field', 'barcode', 'label', '商品条码', 'type', 'input', 'queryType', 'like')) search_schema,
         JSON_ARRAY(JSON_OBJECT('prop', 'barcode', 'label', '商品条码', 'width', 180), JSON_OBJECT('prop', 'productName', 'label', '商品名称', 'width', 220), JSON_OBJECT('prop', 'presaleQuantity', 'label', '预售数量', 'width', 100), JSON_OBJECT('prop', 'pickedQuantity', 'label', '已提数量', 'width', 90), JSON_OBJECT('prop', 'pendingQuantity', 'label', '待提数量', 'width', 90), JSON_OBJECT('prop', 'returnedQuantity', 'label', '退货数量', 'width', 90)) columns_schema,
         JSON_ARRAY(JSON_OBJECT('field', 'barcode', 'label', '商品条码', 'type', 'barcodeScanner', 'required', true), JSON_OBJECT('field', 'productName', 'label', '商品名称', 'type', 'input', 'required', true, 'readonly', true), JSON_OBJECT('field', 'presaleQuantity', 'label', '预售数量', 'type', 'integer', 'required', true, 'defaultValue', 1), JSON_OBJECT('field', 'pickedQuantity', 'label', '已提数量', 'type', 'integer', 'readonly', true), JSON_OBJECT('field', 'pendingQuantity', 'label', '待提数量', 'type', 'integer', 'readonly', true), JSON_OBJECT('field', 'returnedQuantity', 'label', '退货数量', 'type', 'integer', 'readonly', true)) edit_schema
  UNION ALL
  SELECT 1930000000000001103, 'ps_presale_operation_log', 'ps_presale_operation_log',
         '门店预售登记提货退货日志', '预售操作日志', 'PS_PRESALE_OPERATION_LOG', '预售操作日志', 30,
         JSON_ARRAY(JSON_OBJECT('field', 'presaleOrderId', 'label', '预售单ID', 'type', 'input-number', 'queryType', 'eq'), JSON_OBJECT('field', 'actionType', 'label', '操作类型', 'type', 'dictSelect', 'dictType', 'ps_presale_operation_type', 'queryType', 'eq')),
         JSON_ARRAY(JSON_OBJECT('prop', 'productName', 'label', '商品名称', 'width', 220), JSON_OBJECT('prop', 'quantity', 'label', '数量', 'width', 90), JSON_OBJECT('prop', 'actionType', 'label', '操作类型', 'width', 100, 'dictType', 'ps_presale_operation_type'), JSON_OBJECT('prop', 'operatorName', 'label', '操作人', 'width', 120), JSON_OBJECT('prop', 'operationTime', 'label', '操作时间', 'width', 170)),
         JSON_ARRAY(JSON_OBJECT('field', 'productItemId', 'label', '商品明细ID', 'type', 'input-number', 'required', true, 'readonly', true), JSON_OBJECT('field', 'productName', 'label', '商品名称', 'type', 'input', 'readonly', true), JSON_OBJECT('field', 'quantity', 'label', '数量', 'type', 'integer', 'required', true, 'readonly', true), JSON_OBJECT('field', 'actionType', 'label', '操作类型', 'type', 'dictSelect', 'dictType', 'ps_presale_operation_type', 'readonly', true), JSON_OBJECT('field', 'operatorName', 'label', '操作人', 'type', 'input', 'readonly', true), JSON_OBJECT('field', 'operationTime', 'label', '操作时间', 'type', 'datetime', 'readonly', true))
) seed
INNER JOIN ai_lowcode_model model
  ON model.tenant_id = 1 AND model.domain_id = @ps_domain_id AND model.model_code = seed.config_key
WHERE NOT EXISTS (
  SELECT 1 FROM ai_crud_config existing
  WHERE existing.tenant_id = 1 AND existing.config_key = seed.config_key AND existing.del_flag = 0
);

SET @ps_order_object_id := (
  SELECT id
  FROM ai_business_object
  WHERE tenant_id = 1
    AND suite_code = 'PRESALE_REGISTRATION'
    AND object_code = 'PS_PRESALE_ORDER'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO ai_business_document_config (
  id, tenant_id, object_id, suite_code, object_code, config_key,
  document_name, document_no_rule, document_enabled, status_field,
  starter_field, owner_field, default_flow_key, status_mapping, options,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT 1970000000000001101, 1, @ps_order_object_id, 'PRESALE_REGISTRATION',
       'PS_PRESALE_ORDER', 'ps_presale_order', '预售单', 'PS-{yyyyMMdd}-{seq4}', 1,
       'status', 'createBy', 'createBy', NULL,
       JSON_OBJECT(
         'DRAFT', 'DRAFT',
         'SUBMITTED', 'SUBMITTED',
         'COMPLETED', 'CLOSED',
         'CANCELLED', 'CANCELED'
       ),
       JSON_OBJECT(
         'lowcodeApp', true,
         'scenario', 'presale_registration',
         'noRuleTemplate', 'PS-{yyyyMMdd}-{seq4}',
         'documentNoField', 'presaleNo',
         'showStartFlowAction', false,
         'detailFlowDiagramVisible', false,
         'detailFlowTimelineVisible', false
       ),
       1, NOW(), 1, 1, NOW()
WHERE @ps_order_object_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_document_config existing
    WHERE existing.tenant_id = 1
      AND existing.object_code = 'PS_PRESALE_ORDER'
  );

-- BusinessAction 只能从不可变对象发布快照执行；为种子对象建立首个发布版本。
INSERT INTO ai_business_object_design_version (
  id, tenant_id, object_id, suite_code, object_code, config_id, config_key,
  crud_config_version_id, version_no, version_type, model_snapshot, page_snapshot,
  relation_snapshot, designer_options_snapshot, publish_status, publish_version,
  remark, create_by, create_time, create_dept, update_by, update_time
)
SELECT 1970000000000001102, 1, object_row.id, object_row.suite_code, object_row.object_code,
       config_row.id, config_row.config_key, NULL, 1, 'publish',
       config_row.model_schema, config_row.page_schema,
       JSON_ARRAY(
         JSON_OBJECT(
           'id', 1940000000000001101,
           'sourceObjectCode', 'PS_PRESALE_ORDER',
           'targetObjectCode', 'PS_PRESALE_ORDER_ITEM',
           'relationType', 'CHILD_LIST',
           'relationName', 'presale_items',
           'sourceFieldCode', 'id',
           'targetFieldCode', 'presaleOrderId',
           'relationConfig', JSON_OBJECT('relationKey', 'presale_items', 'saveMode', 'merge'),
           'status', 1,
           'sortOrder', 10
         ),
         JSON_OBJECT(
           'id', 1940000000000001102,
           'sourceObjectCode', 'PS_PRESALE_ORDER',
           'targetObjectCode', 'PS_PRESALE_OPERATION_LOG',
           'relationType', 'CHILD_LIST',
           'relationName', 'operation_logs',
           'sourceFieldCode', 'id',
           'targetFieldCode', 'presaleOrderId',
           'relationConfig', JSON_OBJECT('relationKey', 'operation_logs', 'saveMode', 'merge'),
           'status', 1,
           'sortOrder', 20
         )
       ),
       object_row.designer_options, 'PUBLISHED', 1,
       '预售登记低代码应用初始发布快照', 1, NOW(), 1, 1, NOW()
FROM ai_business_object object_row
INNER JOIN ai_crud_config config_row
  ON config_row.tenant_id = object_row.tenant_id
 AND config_row.config_key = 'ps_presale_order'
 AND config_row.del_flag = 0
WHERE object_row.tenant_id = 1
  AND object_row.suite_code = 'PRESALE_REGISTRATION'
  AND object_row.object_code = 'PS_PRESALE_ORDER'
  AND object_row.del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(object_row.designer_options, '$.seedKey')) = 'presale-registration-v1'
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_object_design_version existing
    WHERE existing.tenant_id = 1
      AND existing.object_id = object_row.id
  )
LIMIT 1;

UPDATE ai_business_object object_row
INNER JOIN ai_business_object_design_version version_row
  ON version_row.tenant_id = object_row.tenant_id
 AND version_row.object_id = object_row.id
 AND version_row.publish_status = 'PUBLISHED'
SET object_row.design_status = 'PUBLISHED',
    object_row.last_publish_version = version_row.publish_version,
    object_row.last_publish_time = version_row.create_time,
    object_row.update_by = 1,
    object_row.update_time = NOW()
WHERE object_row.tenant_id = 1
  AND object_row.suite_code = 'PRESALE_REGISTRATION'
  AND object_row.object_code = 'PS_PRESALE_ORDER'
  AND object_row.del_flag = 0
  AND JSON_UNQUOTE(JSON_EXTRACT(object_row.designer_options, '$.seedKey')) = 'presale-registration-v1';

INSERT INTO ai_business_application (
  id, tenant_id, application_code, application_name, suite_code,
  icon, description, status, design_status, last_publish_version,
  last_publish_time, options, del_flag,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT 1950000000000001101, 1, 'PRESALE_REGISTRATION_APP', '门店预售登记',
       'PRESALE_REGISTRATION', 'ionicons5:CartOutline',
       '企微/H5 门店预售登记、分批提货和退货低代码应用', 1, 'READY', NULL, NULL,
       JSON_OBJECT(
         'lowcodeApp', true,
         'mobileFirst', true,
         'primaryObjectCode', 'PS_PRESALE_ORDER',
         'integrationStatus', 'WAITING_BINDING',
         'requiredQuerySources', JSON_ARRAY(
           'wecom/user-store',
           'member/member-by-mobile',
           'product/product-by-barcode',
           'payment/static-code'
         )
       ),
       0, 1, NOW(), 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_application existing
  WHERE existing.tenant_id = 1
    AND existing.application_code = 'PRESALE_REGISTRATION_APP'
    AND existing.del_flag = 0
);

SET @ps_application_id := (
  SELECT id
  FROM ai_business_application
  WHERE tenant_id = 1
    AND application_code = 'PRESALE_REGISTRATION_APP'
    AND del_flag = 0
  LIMIT 1
);

INSERT INTO ai_business_application_object (
  id, tenant_id, application_id, object_id, object_role, sort_order,
  options, del_flag, create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, @ps_application_id, object_row.id, seed.object_role, seed.sort_order,
       seed.options, 0, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1950000000000001111 id, 'PS_PRESALE_ORDER' object_code, 'PRIMARY' object_role,
         10 sort_order, JSON_OBJECT('primary', true, 'pageKey', 'list') options
  UNION ALL
  SELECT 1950000000000001112, 'PS_PRESALE_ORDER_ITEM', 'DETAIL', 20,
         JSON_OBJECT('relationKey', 'presale_items', 'inlineEditEnabled', true)
  UNION ALL
  SELECT 1950000000000001113, 'PS_PRESALE_OPERATION_LOG', 'DETAIL', 30,
         JSON_OBJECT('relationKey', 'operation_logs', 'readonly', true)
) seed
INNER JOIN ai_business_object object_row
  ON object_row.tenant_id = 1
 AND object_row.suite_code = 'PRESALE_REGISTRATION'
 AND object_row.object_code = seed.object_code
 AND object_row.del_flag = 0
WHERE @ps_application_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_application_object existing
    WHERE existing.tenant_id = 1
      AND existing.application_id = @ps_application_id
      AND existing.object_id = object_row.id
      AND existing.del_flag = 0
  );

INSERT INTO ai_business_app (
  id, tenant_id, app_code, app_name, app_type, application_id, suite_code,
  object_code, entry_mode, entry_url, config_key, icon, description,
  status, sort_order, options, del_flag,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT 1960000000000001101, 1, 'PRESALE_REGISTRATION_RUNTIME', '预售信息登记',
       'MOBILE', @ps_application_id, 'PRESALE_REGISTRATION', 'PS_PRESALE_ORDER',
       'RUNTIME', NULL, 'ps_presale_order', 'ionicons5:CartOutline',
       '在企业微信或普通 H5 中登记预售单并办理提货、退货', 1, 10,
       JSON_OBJECT(
         'lowcodeApp', true,
         'appMode', 'DYNAMIC_RENDER',
         'entryType', 'OBJECT_LIST',
         'mountTarget', 'MOBILE',
         'runtimeOpenMode', 'LIST',
         'targetPageKey', 'list',
         'mobileFirst', true,
         'collaborationContainer', 'WECOM',
         'defaultParams', JSON_OBJECT()
       ),
       0, 1, NOW(), 1, 1, NOW()
WHERE @ps_application_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_app existing
    WHERE existing.tenant_id = 1
      AND existing.app_code = 'PRESALE_REGISTRATION_RUNTIME'
      AND existing.del_flag = 0
  );

-- 仅登记稳定 sourceKey；真实 URL、认证、Header、脚本或 SQL 均由部署环境的受管查询源保存。
INSERT INTO ai_business_binding (
  id, tenant_id, target_type, target_id, target_code, binding_type,
  binding_key, binding_name, binding_config, description, status, sort_order,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT seed.id, 1, 'APPLICATION', @ps_application_id, 'PRESALE_REGISTRATION_APP',
       'INTEGRATION', seed.source_key, seed.binding_name,
       JSON_OBJECT('sourceKey', seed.source_key),
       CONCAT(seed.binding_name, '；部署时在集成中心绑定真实查询源后启用'),
       0, seed.sort_order, 1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1980000000000001101 id, 'wecom/user-store' source_key,
         '企微用户与工号门店映射' binding_name, 10 sort_order
  UNION ALL
  SELECT 1980000000000001102, 'member/member-by-mobile', '手机号查询会员', 20
  UNION ALL
  SELECT 1980000000000001103, 'product/product-by-barcode', '条码查询商品', 30
  UNION ALL
  SELECT 1980000000000001104, 'payment/static-code', '静态码单号查询收款', 40
) seed
WHERE @ps_application_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_binding existing
    WHERE existing.tenant_id = 1
      AND existing.target_type = 'APPLICATION'
      AND existing.target_code = 'PRESALE_REGISTRATION_APP'
      AND existing.binding_type = 'INTEGRATION'
      AND existing.binding_key = seed.source_key
  );

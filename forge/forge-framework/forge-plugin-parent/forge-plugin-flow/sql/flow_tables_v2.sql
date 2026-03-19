-- =============================================
-- Forge Flow 流程管理模块数据库表 V2
-- 基于 Flowable 6.8.0
-- 新增：流程模板表、审批节点配置表、条件规则表
-- =============================================

-- 流程模型表
CREATE TABLE IF NOT EXISTS sys_flow_model (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    model_key VARCHAR(100) NOT NULL UNIQUE COMMENT '模型标识',
    model_name VARCHAR(200) NOT NULL COMMENT '模型名称',
    description VARCHAR(500) COMMENT '描述',
    category VARCHAR(100) COMMENT '分类',
    flow_type VARCHAR(50) COMMENT '流程类型（leave-请假/expense-报销/approval-审批）',
    form_type VARCHAR(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/custom-业务表单）',
    form_id VARCHAR(64) COMMENT '表单ID（业务表单时使用）',
    form_json TEXT COMMENT '动态表单JSON配置',
    bpmn_xml TEXT COMMENT 'BPMN流程定义XML',
    version INT DEFAULT 1 COMMENT '版本号',
    process_definition_id VARCHAR(64) COMMENT 'Flowable流程定义ID',
    deployment_id VARCHAR(64) COMMENT 'Flowable部署ID',
    deployment_key VARCHAR(100) COMMENT '部署KEY（发布后生成）',
    status TINYINT DEFAULT 0 COMMENT '状态（0-设计/1-已发布/2-已挂起/3-已禁用）',
    deploy_time DATETIME COMMENT '发布时间',
    last_update_by VARCHAR(64) COMMENT '最后修改人',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志（0-正常/1-删除）',
    INDEX idx_model_key(model_key),
    INDEX idx_category(category),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模型表';

-- 如果表已存在，添加缺失的列
ALTER TABLE sys_flow_model ADD COLUMN IF NOT EXISTS process_definition_id VARCHAR(64) COMMENT 'Flowable流程定义ID' AFTER version;
ALTER TABLE sys_flow_model ADD COLUMN IF NOT EXISTS last_update_by VARCHAR(64) COMMENT '最后修改人' AFTER deploy_time;

-- 流程业务关联表（业务系统使用）
CREATE TABLE IF NOT EXISTS sys_flow_business (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    business_key VARCHAR(100) NOT NULL COMMENT '业务Key',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型（order/contract等）',
    process_instance_id VARCHAR(64) COMMENT '流程实例ID',
    process_def_id VARCHAR(64) COMMENT '流程定义ID',
    process_def_key VARCHAR(100) COMMENT '流程定义KEY',
    title VARCHAR(200) COMMENT '流程标题',
    status VARCHAR(50) DEFAULT 'draft' COMMENT '业务状态（draft-草稿/running-审批中/approved-已通过/rejected-已驳回/canceled-已取消）',
    apply_user_id VARCHAR(64) COMMENT '申请人',
    apply_user_name VARCHAR(100) COMMENT '申请人姓名',
    apply_dept_id VARCHAR(64) COMMENT '申请部门ID',
    apply_dept_name VARCHAR(100) COMMENT '申请部门名称',
    apply_time DATETIME COMMENT '申请时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '流程耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_business_key(business_key),
    INDEX idx_process_instance_id(process_instance_id),
    INDEX idx_apply_user_id(apply_user_id),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程业务关联表';

-- 流程任务表（我的待办/已办）
CREATE TABLE IF NOT EXISTS sys_flow_task (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    task_id VARCHAR(64) COMMENT 'Flowable任务ID',
    task_name VARCHAR(200) COMMENT '任务名称',
    task_def_key VARCHAR(100) COMMENT '任务定义Key',
    task_def_id VARCHAR(100) COMMENT '任务定义ID',
    process_instance_id VARCHAR(64) COMMENT '流程实例ID',
    process_def_id VARCHAR(64) COMMENT '流程定义ID',
    process_def_key VARCHAR(100) COMMENT '流程定义KEY',
    business_key VARCHAR(100) COMMENT '业务Key',
    business_type VARCHAR(50) COMMENT '业务类型',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    assignee VARCHAR(64) COMMENT '处理人（签收后）',
    assignee_name VARCHAR(100) COMMENT '处理人姓名',
    candidate_users VARCHAR(1000) COMMENT '候选人（逗号分隔）',
    candidate_groups VARCHAR(1000) COMMENT '候选组（逗号分隔）',
    owner VARCHAR(64) COMMENT '任务拥有人',
    due_date DATETIME COMMENT '截止日期',
    priority INT DEFAULT 50 COMMENT '优先级（0-100）',
    status TINYINT DEFAULT 0 COMMENT '状态（0-待办/1-已签收/2-已通过/3-已驳回/4-已转办/5-已委派/6-已撤回）',
    comment TEXT COMMENT '审批意见',
    attachment_urls TEXT COMMENT '附件URL（逗号分隔）',
    start_user_id VARCHAR(64) COMMENT '流程发起人',
    start_user_name VARCHAR(100) COMMENT '发起人姓名',
    start_dept_id VARCHAR(64) COMMENT '发起部门ID',
    start_dept_name VARCHAR(100) COMMENT '发起部门名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    claim_time DATETIME COMMENT '签收时间',
    complete_time DATETIME COMMENT '完成时间',
    INDEX idx_task_id(task_id),
    INDEX idx_process_instance_id(process_instance_id),
    INDEX idx_assignee(assignee),
    INDEX idx_status(status),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程任务表';

-- 流程抄送表
CREATE TABLE IF NOT EXISTS sys_flow_cc (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    process_instance_id VARCHAR(64) COMMENT '流程实例ID',
    process_def_key VARCHAR(100) COMMENT '流程定义KEY',
    task_id VARCHAR(64) COMMENT '来源任务ID',
    title VARCHAR(200) COMMENT '标题',
    content TEXT COMMENT '内容摘要',
    business_key VARCHAR(100) COMMENT '业务Key',
    cc_user_id VARCHAR(64) NOT NULL COMMENT '抄送人ID',
    cc_user_name VARCHAR(100) COMMENT '抄送人姓名',
    send_user_id VARCHAR(64) COMMENT '发送人ID',
    send_user_name VARCHAR(100) COMMENT '发送人姓名',
    cc_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '抄送时间',
    read_time DATETIME COMMENT '阅读时间',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读（0-未读/1-已读）',
    INDEX idx_process_instance_id(process_instance_id),
    INDEX idx_cc_user_id(cc_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程抄送表';

-- 流程意见表
CREATE TABLE IF NOT EXISTS sys_flow_comment (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    process_instance_id VARCHAR(64) COMMENT '流程实例ID',
    process_def_key VARCHAR(100) COMMENT '流程定义KEY',
    task_id VARCHAR(64) COMMENT '任务ID',
    task_name VARCHAR(200) COMMENT '任务名称',
    type VARCHAR(20) DEFAULT 'comment' COMMENT '类型（comment-审批意见/event-流程事件）',
    message TEXT COMMENT '意见内容',
    user_id VARCHAR(64) COMMENT '用户ID',
    user_name VARCHAR(100) COMMENT '用户姓名',
    full_message TEXT COMMENT '完整消息（JSON）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_process_instance_id(process_instance_id),
    INDEX idx_task_id(task_id),
    INDEX idx_user_id(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程意见表';

-- 流程分类表
CREATE TABLE IF NOT EXISTS sys_flow_category (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（0-禁用/1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_code(category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程分类表';

-- 流程模板表（V2新增）
CREATE TABLE IF NOT EXISTS sys_flow_template (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    template_key VARCHAR(100) NOT NULL UNIQUE COMMENT '模板标识',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    category VARCHAR(50) COMMENT '分类',
    description VARCHAR(500) COMMENT '描述',
    icon VARCHAR(100) COMMENT '图标',
    form_type VARCHAR(50) COMMENT '表单类型',
    form_json TEXT COMMENT '表单JSON',
    bpmn_xml TEXT COMMENT 'BPMN流程XML',
    thumbnail VARCHAR(500) COMMENT '缩略图',
    variables TEXT COMMENT '流程变量定义（JSON）',
    version INT DEFAULT 1 COMMENT '版本',
    status TINYINT DEFAULT 1 COMMENT '状态（0-禁用/1-启用）',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统内置（0-否/1-是）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志（0-正常/1-删除）',
    INDEX idx_template_key(template_key),
    INDEX idx_category(category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板表';

-- 审批节点配置表（V2新增）
CREATE TABLE IF NOT EXISTS sys_flow_node_config (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    model_id VARCHAR(64) NOT NULL COMMENT '流程模型ID',
    node_id VARCHAR(100) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) DEFAULT 'approval' COMMENT '节点类型',
    
    -- 审批人配置
    assignee_type VARCHAR(50) COMMENT '审批人类型(user/role/dept/post/expr)',
    assignee_value TEXT COMMENT '审批人值',
    assignee_expr VARCHAR(500) COMMENT '审批人表达式',
    
    -- 多人审批策略
    multi_instance_type VARCHAR(50) DEFAULT 'none' COMMENT '会签类型(none/sequential/parallel)',
    completion_condition VARCHAR(500) COMMENT '完成条件',
    pass_rate DECIMAL(5,2) COMMENT '通过比例',
    
    -- 超时设置
    due_date_days INT COMMENT '超时天数',
    due_date_hours INT COMMENT '超时小时数',
    timeout_action VARCHAR(50) COMMENT '超时动作(auto_pass/auto_reject/notify)',
    
    -- 其他配置
    allow_delegate TINYINT DEFAULT 1 COMMENT '允许转办',
    allow_transfer TINYINT DEFAULT 1 COMMENT '允许转交',
    allow_add_sign TINYINT DEFAULT 0 COMMENT '允许加签',
    allow_counter_sign TINYINT DEFAULT 0 COMMENT '允许减签',
    
    -- 表单权限
    form_permission TEXT COMMENT '表单字段权限配置JSON',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_model_node(model_id, node_id),
    INDEX idx_model_id(model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点配置表';

-- 审批层级配置表（V2新增，支持动态层级）
CREATE TABLE IF NOT EXISTS sys_flow_approval_level (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    node_config_id VARCHAR(64) NOT NULL COMMENT '节点配置ID',
    level_index INT NOT NULL COMMENT '层级序号',
    level_name VARCHAR(100) COMMENT '层级名称',
    assignee_type VARCHAR(50) COMMENT '审批人类型',
    assignee_value TEXT COMMENT '审批人值',
    condition_expr VARCHAR(500) COMMENT '层级条件表达式',
    skip_condition VARCHAR(500) COMMENT '跳过条件',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_node_config_id(node_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批层级配置表';

-- 流程条件规则表（V2新增）
CREATE TABLE IF NOT EXISTS sys_flow_condition_rule (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    model_id VARCHAR(64) NOT NULL COMMENT '流程模型ID',
    sequence_flow_id VARCHAR(100) NOT NULL COMMENT '序列流ID',
    rule_name VARCHAR(200) COMMENT '规则名称',
    rule_type VARCHAR(50) DEFAULT 'expression' COMMENT '规则类型',
    
    -- 条件配置
    condition_groups TEXT COMMENT '条件组配置JSON',
    condition_expr VARCHAR(1000) COMMENT '条件表达式',
    priority INT DEFAULT 0 COMMENT '优先级',
    
    -- 默认分支
    is_default TINYINT DEFAULT 0 COMMENT '是否默认分支',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_model_flow(model_id, sequence_flow_id),
    INDEX idx_model_id(model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程条件规则表';

-- 条件规则明细表（V2新增）
CREATE TABLE IF NOT EXISTS sys_flow_condition_item (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    rule_id VARCHAR(64) NOT NULL COMMENT '规则ID',
    group_index INT NOT NULL COMMENT '条件组索引',
    item_index INT NOT NULL COMMENT '条件项索引',
    
    -- 条件配置
    field_name VARCHAR(100) COMMENT '字段名',
    operator VARCHAR(50) COMMENT '操作符(eq/ne/gt/lt/ge/le/contains/empty)',
    field_value VARCHAR(500) COMMENT '字段值',
    value_type VARCHAR(50) DEFAULT 'string' COMMENT '值类型',
    
    -- 逻辑关系
    logic_operator VARCHAR(10) DEFAULT 'AND' COMMENT '逻辑操作符(AND/OR)',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_rule_id(rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='条件规则明细表';

-- 流程监控统计表（V2新增）
CREATE TABLE IF NOT EXISTS sys_flow_statistics (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    process_def_key VARCHAR(100) NOT NULL COMMENT '流程定义KEY',
    stat_date DATE NOT NULL COMMENT '统计日期',
    
    -- 实例统计
    total_instances INT DEFAULT 0 COMMENT '总实例数',
    running_instances INT DEFAULT 0 COMMENT '运行中实例数',
    completed_instances INT DEFAULT 0 COMMENT '已完成实例数',
    terminated_instances INT DEFAULT 0 COMMENT '已终止实例数',
    
    -- 耗时统计
    avg_duration BIGINT COMMENT '平均耗时(毫秒)',
    max_duration BIGINT COMMENT '最大耗时',
    min_duration BIGINT COMMENT '最小耗时',
    
    -- 任务统计
    total_tasks INT DEFAULT 0 COMMENT '总任务数',
    avg_task_duration BIGINT COMMENT '平均任务耗时',
    timeout_tasks INT DEFAULT 0 COMMENT '超时任务数',
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_process_date(process_def_key, stat_date),
    INDEX idx_stat_date(stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程监控统计表';

-- =============================================
-- 初始化数据
-- =============================================

-- 默认分类数据
INSERT INTO sys_flow_category (id, category_code, category_name, description, sort_order, status) VALUES
('1', 'leave', '请假流程', '员工请假申请审批', 1, 1),
('2', 'expense', '报销流程', '费用报销申请审批', 2, 1),
('3', 'approval', '审批流程', '通用审批流程', 3, 1),
('4', 'contract', '合同流程', '合同审批流程', 4, 1),
('5', 'purchase', '采购流程', '采购申请审批', 5, 1)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

-- 系统内置流程模板
INSERT INTO sys_flow_template (id, template_key, template_name, category, description, icon, form_type, form_json, bpmn_xml, version, status, usage_count, is_system, sort_order) VALUES
('1', 'leave_simple', '简单请假流程', 'leave', '适用于短期请假（3天以内），只需直属领导审批', 'i-material-symbols:event', 'dynamic', 
 '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
 '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_simple" name="简单请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="直属领导审批" flowable:assignee="${initiatorLeader}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
 1, 1, 0, 1, 1),

('2', 'leave_multi', '多级请假流程', 'leave', '适用于长期请假（3天以上），需要多级领导审批', 'i-material-symbols:event-note', 'dynamic',
 '[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"}]}},{"type":"datePicker","field":"startDate","label":"开始日期"},{"type":"datePicker","field":"endDate","label":"结束日期"},{"type":"inputNumber","field":"days","label":"请假天数"},{"type":"textarea","field":"reason","label":"请假原因"}]',
 '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="leave_multi" name="多级请假流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="deptApprove" name="部门经理审批" flowable:assignee="${deptManager}"/><bpmn:userTask id="hrApprove" name="HR审批" flowable:candidateGroups="${hr}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="deptApprove"/><bpmn:sequenceFlow id="flow2" sourceRef="deptApprove" targetRef="hrApprove"/><bpmn:sequenceFlow id="flow3" sourceRef="hrApprove" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_multi"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="deptApprove_di" bpmnElement="deptApprove"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="hrApprove_di" bpmnElement="hrApprove"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
 1, 1, 0, 1, 2),

('3', 'expense_common', '通用报销流程', 'expense', '适用于日常费用报销，如差旅费、办公费等', 'i-material-symbols:payments', 'dynamic',
 '[{"type":"input","field":"title","label":"报销标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"其他","value":"4"}]}},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"precision":2}},{"type":"datePicker","field":"expenseDate","label":"报销日期"},{"type":"textarea","field":"description","label":"报销说明"},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
 '<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="expense_common" name="通用报销流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${initiatorLeader}"/><bpmn:userTask id="financeTask" name="财务审核" flowable:candidateGroups="finance"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="financeTask"/><bpmn:sequenceFlow id="flow3" sourceRef="financeTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="expense_common"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="financeTask_di" bpmnElement="financeTask"><dc:Bounds x="440" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="620" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
 1, 1, 0, 1, 3),

('4', 'approval_simple', '通用审批流程', 'approval', '适用于简单的审批场景，单级审批', 'i-material-symbols:approval', 'dynamic',
'[{"type":"input","field":"title","label":"申请标题","rules":[{"required":true,"message":"请输入标题"}]},{"type":"textarea","field":"content","label":"申请内容"},{"type":"upload","field":"attachments","label":"附件"}]',
'<?xml version="1.0" encoding="UTF-8"?><bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://flowable.org/processdef"><bpmn:process id="approval_simple" name="通用审批流程" isExecutable="true"><bpmn:startEvent id="startEvent" name="开始" flowable:initiator="initiator"/><bpmn:userTask id="approveTask" name="审批" flowable:assignee="${approver}"/><bpmn:endEvent id="endEvent" name="结束"/><bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="approveTask"/><bpmn:sequenceFlow id="flow2" sourceRef="approveTask" targetRef="endEvent"/></bpmn:process><bpmndi:BPMNDiagram id="BPMNDiagram_1"><bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="approval_simple"><bpmndi:BPMNShape id="startEvent_di" bpmnElement="startEvent"><dc:Bounds x="180" y="160" width="36" height="36"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="approveTask_di" bpmnElement="approveTask"><dc:Bounds x="280" y="138" width="100" height="80"/></bpmndi:BPMNShape><bpmndi:BPMNShape id="endEvent_di" bpmnElement="endEvent"><dc:Bounds x="460" y="160" width="36" height="36"/></bpmndi:BPMNShape></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn:definitions>',
1, 1, 0, 1, 4)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

-- =============================================
-- 表单定义表
-- =============================================

-- 流程表单定义表
CREATE TABLE IF NOT EXISTS sys_flow_form (
   id BIGINT PRIMARY KEY COMMENT '主键ID',
   form_key VARCHAR(100) NOT NULL UNIQUE COMMENT '表单Key（唯一标识）',
   form_name VARCHAR(200) NOT NULL COMMENT '表单名称',
   form_type VARCHAR(50) DEFAULT 'dynamic' COMMENT '表单类型（dynamic-动态表单/external-外部表单/builtin-内置表单）',
   form_schema TEXT COMMENT '表单Schema（JSON格式）',
   form_url VARCHAR(500) COMMENT '外部表单URL（formType为external时使用）',
   component_path VARCHAR(200) COMMENT '内置表单组件路径（formType为builtin时使用）',
   form_config TEXT COMMENT '表单配置（JSON格式，包含校验规则、事件等）',
   version INT DEFAULT 1 COMMENT '版本号',
   status TINYINT DEFAULT 1 COMMENT '状态（0-禁用/1-启用）',
   description VARCHAR(500) COMMENT '描述',
   tenant_id BIGINT COMMENT '租户ID',
   create_by VARCHAR(64) COMMENT '创建者',
   create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   update_by VARCHAR(64) COMMENT '更新者',
   update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
   deleted TINYINT DEFAULT 0 COMMENT '删除标志（0-正常/1-删除）',
   
   INDEX idx_form_key(form_key),
   INDEX idx_form_name(form_name),
   INDEX idx_status(status),
   INDEX idx_tenant_id(tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程表单定义表';

-- 初始化表单数据
INSERT INTO sys_flow_form (id, form_key, form_name, form_type, form_schema, version, status, description) VALUES
(1, 'leave_form', '请假申请表单', 'dynamic',
'[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入标题"},"rules":[{"required":true,"message":"请输入标题"}]},{"type":"select","field":"leaveType","label":"请假类型","props":{"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"},{"label":"婚假","value":"4"},{"label":"产假","value":"5"}]},"rules":[{"required":true,"message":"请选择请假类型"}]},{"type":"datePicker","field":"startDate","label":"开始日期","rules":[{"required":true,"message":"请选择开始日期"}]},{"type":"datePicker","field":"endDate","label":"结束日期","rules":[{"required":true,"message":"请选择结束日期"}]},{"type":"inputNumber","field":"days","label":"请假天数","props":{"min":0.5,"precision":1},"rules":[{"required":true,"message":"请输入请假天数"}]},{"type":"textarea","field":"reason","label":"请假原因","props":{"placeholder":"请输入请假原因","rows":3},"rules":[{"required":true,"message":"请输入请假原因"}]}]',
1, 1, '员工请假申请表单'),

(2, 'expense_form', '报销申请表单', 'dynamic',
'[{"type":"input","field":"title","label":"报销标题","props":{"placeholder":"请输入报销标题"},"rules":[{"required":true,"message":"请输入报销标题"}]},{"type":"select","field":"expenseType","label":"报销类型","props":{"options":[{"label":"差旅费","value":"1"},{"label":"办公费","value":"2"},{"label":"招待费","value":"3"},{"label":"交通费","value":"4"},{"label":"其他","value":"5"}]},"rules":[{"required":true,"message":"请选择报销类型"}]},{"type":"inputNumber","field":"amount","label":"报销金额","props":{"min":0,"precision":2,"prefix":"¥"},"rules":[{"required":true,"message":"请输入报销金额"}]},{"type":"datePicker","field":"expenseDate","label":"报销日期","rules":[{"required":true,"message":"请选择报销日期"}]},{"type":"textarea","field":"description","label":"报销说明","props":{"placeholder":"请输入报销说明","rows":3}},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true,"accept":".jpg,.jpeg,.png,.pdf"}}]',
1, 1, '费用报销申请表单'),

(3, 'approval_form', '通用审批表单', 'dynamic',
'[{"type":"input","field":"title","label":"申请标题","props":{"placeholder":"请输入申请标题"},"rules":[{"required":true,"message":"请输入申请标题"}]},{"type":"select","field":"approvalType","label":"审批类型","props":{"options":[{"label":"通用审批","value":"1"},{"label":"合同审批","value":"2"},{"label":"项目审批","value":"3"},{"label":"其他","value":"4"}]},"rules":[{"required":true,"message":"请选择审批类型"}]},{"type":"textarea","field":"content","label":"申请内容","props":{"placeholder":"请输入申请内容","rows":4},"rules":[{"required":true,"message":"请输入申请内容"}]},{"type":"upload","field":"attachments","label":"附件","props":{"multiple":true}}]',
1, 1, '通用审批申请表单')
ON DUPLICATE KEY UPDATE form_name = VALUES(form_name);
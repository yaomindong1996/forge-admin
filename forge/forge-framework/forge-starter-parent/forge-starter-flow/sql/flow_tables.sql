-- =============================================
-- Forge Flow 流程管理模块数据库表
-- 基于 Flowable 6.8.0
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
    version INT DEFAULT 1 COMMENT '版本号',
    deployment_id VARCHAR(64) COMMENT 'Flowable部署ID',
    deployment_key VARCHAR(100) COMMENT '部署KEY（发布后生成）',
    status TINYINT DEFAULT 0 COMMENT '状态（0-设计/1-已发布/2-禁用）',
    deploy_time DATETIME COMMENT '发布时间',
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT DEFAULT 0 COMMENT '删除标志（0-正常/1-删除）',
    INDEX idx_model_key(model_key),
    INDEX idx_category(category),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模型表';

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

-- 默认分类数据
INSERT INTO sys_flow_category (id, category_code, category_name, description, sort_order, status) VALUES
('1', 'leave', '请假流程', '员工请假申请审批', 1, 1),
('2', 'expense', '报销流程', '费用报销申请审批', 2, 1),
('3', 'approval', '审批流程', '通用审批流程', 3, 1),
('4', 'contract', '合同流程', '合同审批流程', 4, 1),
('5', 'purchase', '采购流程', '采购申请审批', 5, 1);

-- 流程模板表（常用流程模板）
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
    create_by VARCHAR(64) COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_key(template_key),
    INDEX idx_category(category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程模板表';
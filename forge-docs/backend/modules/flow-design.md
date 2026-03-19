# Forge Flow 流程管理模块 - 自研设计方案

## 一、现有模块分析

### 1.1 技术架构

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| 流程引擎 | Flowable 6.8.0 | 开源工作流引擎，符合BPMN 2.0规范 |
| 后端框架 | Spring Boot 3.x | 微服务架构 |
| ORM框架 | MyBatis-Plus | 数据持久化 |
| 前端框架 | Vue 3 + Naive UI | 现代化UI组件库 |
| 流程设计器 | bpmn-js | BPMN 2.0 建模工具 |

### 1.2 现有功能清单

#### 后端模块 (`forge-plugin-flow`)

**实体层**：
- [`FlowModel`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowModel.java) - 流程模型
- [`FlowBusiness`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowBusiness.java) - 流程业务关联
- [`FlowTask`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowTask.java) - 流程任务
- [`FlowCc`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowCc.java) - 流程抄送
- [`FlowComment`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowComment.java) - 流程意见
- [`FlowCategory`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/entity/FlowCategory.java) - 流程分类

**服务层**：
- [`FlowModelService`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowModelService.java) - 模型管理
- [`FlowInstanceService`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowInstanceService.java) - 实例管理
- [`FlowTaskService`](forge/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/service/FlowTaskService.java) - 任务管理

**控制器层**：
- [`FlowModelController`](forge/forge-flow/src/main/java/com/mdframe/forge/flow/controller/FlowModelController.java) - 模型接口
- [`FlowTaskController`](forge/forge-flow/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java) - 任务接口
- [`FlowInstanceController`](forge/forge-flow/src/main/java/com/mdframe/forge/flow/controller/FlowInstanceController.java) - 实例接口

#### 前端模块 (`forge-admin-ui`)

**页面**：
- [`model.vue`](forge-admin-ui/src/views/flow/model.vue) - 流程模型列表
- [`design.vue`](forge-admin-ui/src/views/flow/design.vue) - 流程设计器
- [`todo.vue`](forge-admin-ui/src/views/flow/todo.vue) - 待办任务
- [`done.vue`](forge-admin-ui/src/views/flow/done.vue) - 已办任务
- [`started.vue`](forge-admin-ui/src/views/flow/started.vue) - 我发起的
- [`cc.vue`](forge-admin-ui/src/views/flow/cc.vue) - 抄送我的

**组件**：
- [`BpmnModeler.vue`](forge-admin-ui/src/components/bpmn/BpmnModeler.vue) - BPMN流程设计器

### 1.3 数据库表结构

```sql
-- 核心表
sys_flow_model       -- 流程模型表
sys_flow_business    -- 流程业务关联表
sys_flow_task        -- 流程任务表
sys_flow_cc          -- 流程抄送表
sys_flow_comment     -- 流程意见表
sys_flow_category    -- 流程分类表
sys_flow_template    -- 流程模板表
```

### 1.4 现有功能评估

| 功能模块 | 完成度 | 说明 |
|---------|-------|------|
| 流程模型管理 | ✅ 80% | 基本CRUD、部署、启用/禁用 |
| BPMN流程设计 | ⚠️ 50% | 基础设计器，缺少复杂节点配置 |
| 流程任务管理 | ✅ 70% | 待办、已办、签收、审批、驳回、转办 |
| 流程实例管理 | ✅ 60% | 发起、终止、状态查询 |
| 动态表单设计 | ❌ 20% | 仅有字段配置，无可视化设计器 |
| 流程条件设置 | ⚠️ 30% | 基础条件表达式，无可视化配置 |
| 审批节点配置 | ⚠️ 40% | 支持基本审批人设置，缺少动态层级 |
| 组织架构集成 | ⚠️ 50% | 基本用户/部门关联，缺少深度集成 |

---

## 二、自研功能规划

### 2.1 整体架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 流程设计器   │  │ 表单设计器   │  │ 流程监控中心 │              │
│  │ (BPMN)      │  │ (Form Builder)│  │ (Dashboard) │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 节点配置面板 │  │ 条件规则配置 │  │ 组织选择器   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                        API网关层                                 │
├─────────────────────────────────────────────────────────────────┤
│                        后端服务层 (Spring Boot)                   │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 流程定义服务 │  │ 表单引擎服务 │  │ 任务管理服务 │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 条件规则引擎 │  │ 组织集成服务 │  │ 流程监控服务 │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                      Flowable引擎层                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ RuntimeService | RepositoryService | TaskService | History...││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│                        数据层                                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ MySQL       │  │ Redis       │  │ Flowable DB │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心功能模块设计

#### 2.2.1 可视化表单设计器

**功能需求**：
- 拖拽式表单组件布局
- 丰富的表单组件库（输入框、选择器、日期、上传、签名等）
- 表单字段属性配置（校验规则、默认值、权限控制）
- 表单数据源绑定
- 表单预览与导出

**技术方案**：
- 参考：form-generator、variant-form3-vite、form-create
- 组件库：基于Naive UI封装
- 数据结构：JSON Schema标准

**新增表结构**：
```sql
-- 表单定义表
CREATE TABLE sys_form_definition (
    id VARCHAR(64) PRIMARY KEY,
    form_key VARCHAR(100) NOT NULL UNIQUE COMMENT '表单标识',
    form_name VARCHAR(200) NOT NULL COMMENT '表单名称',
    form_schema TEXT COMMENT '表单JSON Schema',
    form_config TEXT COMMENT '表单配置',
    version INT DEFAULT 1,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 表单组件表
CREATE TABLE sys_form_component (
    id VARCHAR(64) PRIMARY KEY,
    component_type VARCHAR(50) NOT NULL COMMENT '组件类型',
    component_name VARCHAR(100) NOT NULL COMMENT '组件名称',
    component_icon VARCHAR(100) COMMENT '组件图标',
    component_config TEXT COMMENT '组件默认配置JSON',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1
);
```

#### 2.2.2 动态审批节点配置

**功能需求**：
- 支持任意层级审批节点
- 审批人动态配置（按角色、部门、岗位、指定人员）
- 会签/或签配置
- 审批策略配置（依次审批、并行审批、百分比通过）
- 审批超时设置
- 审批代理/转办规则

**技术方案**：
- 扩展BPMN UserTask节点属性
- 自定义审批策略处理器
- 监听器实现动态审批人分配

**新增表结构**：
```sql
-- 审批节点配置表
CREATE TABLE sys_flow_node_config (
    id VARCHAR(64) PRIMARY KEY,
    model_id VARCHAR(64) NOT NULL COMMENT '流程模型ID',
    node_id VARCHAR(100) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) DEFAULT 'approval' COMMENT '节点类型',
    
    -- 审批人配置
    assignee_type VARCHAR(50) COMMENT '审批人类型(user/role/dept/post/expr)',
    assignee_value TEXT COMMENT '审批人值',
    assignee_expr VARCHAR(500) COMMENT '审批人表达式',
    
    -- 多人审批策略
    multi_instance_type VARCHAR(50) DEFAULT 'sequential' COMMENT '会签类型(sequential/parallel)',
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
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_model_node(model_id, node_id)
);

-- 审批层级配置表（支持动态层级）
CREATE TABLE sys_flow_approval_level (
    id VARCHAR(64) PRIMARY KEY,
    node_config_id VARCHAR(64) NOT NULL COMMENT '节点配置ID',
    level_index INT NOT NULL COMMENT '层级序号',
    level_name VARCHAR(100) COMMENT '层级名称',
    assignee_type VARCHAR(50) COMMENT '审批人类型',
    assignee_value TEXT COMMENT '审批人值',
    condition_expr VARCHAR(500) COMMENT '层级条件表达式',
    skip_condition VARCHAR(500) COMMENT '跳过条件',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 2.2.3 复杂流程条件设置

**功能需求**：
- 可视化条件规则编辑器
- 支持表单字段条件判断
- 支持流程变量条件
- 支持组织架构条件（部门、角色、职级）
- 支持复杂逻辑组合（AND/OR/NOT）
- 条件表达式预览与测试

**技术方案**：
- 规则引擎：参考JSON Logic规范
- 表达式语言：Spring EL + Flowable UEL
- 可视化编辑：条件组配置面板

**新增表结构**：
```sql
-- 流程条件规则表
CREATE TABLE sys_flow_condition_rule (
    id VARCHAR(64) PRIMARY KEY,
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
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_model_flow(model_id, sequence_flow_id)
);

-- 条件规则明细表
CREATE TABLE sys_flow_condition_item (
    id VARCHAR(64) PRIMARY KEY,
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
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### 2.2.4 组织架构集成

**功能需求**：
- 与现有用户、部门、角色、岗位体系深度集成
- 支持按组织层级查找审批人
- 支持上级领导、部门负责人动态获取
- 支持按业务规则动态计算审批人
- 组织变更自动感知

**技术方案**：
- 扩展组织服务接口
- 实现Flowable用户组解析器
- 缓存优化组织数据查询

**新增服务接口**：
```java
/**
 * 组织架构集成服务
 */
public interface FlowOrganizationService {
    
    /**
     * 获取用户上级领导
     */
    User getLeader(String userId, int level);
    
    /**
     * 获取部门负责人
     */
    User getDeptManager(String deptId);
    
    /**
     * 获取用户所在部门
     */
    Dept getUserDept(String userId);
    
    /**
     * 获取部门所有上级部门
     */
    List<Dept> getParentDepts(String deptId);
    
    /**
     * 获取角色下的用户
     */
    List<User> getUsersByRole(String roleCode);
    
    /**
     * 获取岗位下的用户
     */
    List<User> getUsersByPost(String postCode);
    
    /**
     * 按条件动态计算审批人
     */
    List<User> calculateApprovers(String expression, Map<String, Object> variables);
}
```

#### 2.2.5 流程监控与管理

**功能需求**：
- 流程实例监控大屏
- 任务超时预警
- 流程效率分析
- 瓶颈节点识别
- 流程优化建议

**新增表结构**：
```sql
-- 流程监控统计表
CREATE TABLE sys_flow_statistics (
    id VARCHAR(64) PRIMARY KEY,
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
    
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_process_date(process_def_key, stat_date)
);
```

---

## 三、参考开源项目

### 3.1 流程设计器

| 项目名称 | GitHub | 特点 |
|---------|--------|------|
| **bpmn-js** | https://github.com/bpmn-io/bpmn-js | Flowable官方推荐，功能强大 |
| **bpmn-process-designer** | https://github.com/moon-studio/vite-vue-bpmn-process | Vue3 + BPMN，中文友好 |
| **RuoYi-Flowable** | https://github.com/tony2y/RuoYi-Flowable | 若依集成Flowable，完整方案 |
| **Flowable UI** | https://github.com/flowable/flowable-engine | Flowable官方设计器 |

### 3.2 表单设计器

| 项目名称 | GitHub | 特点 |
|---------|--------|------|
| **form-generator** | https://github.com/JakHuang/form-generator | Vue2/3，功能丰富 |
| **variant-form3-vite** | https://github.com/vform666/variant-form3-vite | Vue3表单设计器 |
| **form-create** | https://github.com/xaboy/form-create | 低代码表单生成 |
| **Avue** | https://github.com/nmxiaowai/avue | 数据驱动视图 |

### 3.3 完整工作流方案

| 项目名称 | GitHub | 特点 |
|---------|--------|------|
| **RuoYi-Flowable** | https://github.com/tony2y/RuoYi-Flowable | 若依+Flowable完整方案 |
| **Snowy** | https://github.com/xiaonuobase/snowy | Layui+Flowable |
| **芋道源码** | https://github.com/YunaiV/ruoyi-vue-pro | 微服务+Flowable |
| **JeecgBoot** | https://github.com/jeecgboot/jeecg-boot | 低代码+Flowable |

---

## 四、实施计划

### 4.1 阶段一：基础增强（2周）

**目标**：完善现有功能，提升易用性

**任务清单**：
- [ ] 完善BPMN设计器节点属性配置面板
- [ ] 实现流程模板功能
- [ ] 优化流程部署与版本管理
- [ ] 完善流程分类管理
- [ ] 增加流程导入/导出功能

### 4.2 阶段二：表单设计器（3周）

**目标**：实现可视化表单设计

**任务清单**：
- [ ] 设计表单JSON Schema规范
- [ ] 开发表单组件库（15+基础组件）
- [ ] 实现拖拽式表单设计器
- [ ] 表单属性配置面板
- [ ] 表单预览与数据验证
- [ ] 表单与流程关联

### 4.3 阶段三：审批节点增强（2周）

**目标**：支持复杂审批场景

**任务清单**：
- [ ] 审批节点配置面板开发
- [ ] 动态审批人计算引擎
- [ ] 会签/或签策略实现
- [ ] 审批超时处理
- [ ] 审批代理/转办功能增强

### 4.4 阶段四：条件规则引擎（2周）

**目标**：可视化条件配置

**任务清单**：
- [ ] 条件规则编辑器开发
- [ ] 条件表达式解析引擎
- [ ] 条件规则测试功能
- [ ] 与流程设计器集成

### 4.5 阶段五：组织架构集成（1周）

**目标**：深度集成组织体系

**任务清单**：
- [ ] 组织服务接口开发
- [ ] Flowable用户组解析器
- [ ] 审批人计算策略
- [ ] 组织选择器组件

### 4.6 阶段六：监控与优化（1周）

**目标**：流程监控与分析

**任务清单**：
- [ ] 流程监控大屏
- [ ] 统计分析功能
- [ ] 超时预警机制
- [ ] 性能优化

---

## 五、技术要点

### 5.1 BPMN扩展属性

```xml
<!-- 用户任务扩展配置示例 -->
<userTask id="approveTask" name="审批">
  <extensionElements>
    <flowable:assignee>${initiator}</flowable:assignee>
    <flowable:candidateUsers>${candidateUsers}</flowable:candidateUsers>
    <flowable:taskListener event="create" class="com.mdframe.forge.flow.listener.TaskCreateListener"/>
  </extensionElements>
  <multiInstanceLoopCharacteristics isSequential="false">
    <completionCondition>${nrOfCompletedInstances/nrOfInstances >= 0.5}</completionCondition>
  </multiInstanceLoopCharacteristics>
</userTask>
```

### 5.2 表单JSON Schema示例

```json
{
  "formKey": "leave_form",
  "formName": "请假申请表",
  "fields": [
    {
      "type": "input",
      "field": "title",
      "label": "申请标题",
      "props": {
        "placeholder": "请输入标题"
      },
      "rules": [
        { "required": true, "message": "请输入标题" }
      ]
    },
    {
      "type": "select",
      "field": "leaveType",
      "label": "请假类型",
      "props": {
        "options": [
          { "label": "事假", "value": "1" },
          { "label": "病假", "value": "2" },
          { "label": "年假", "value": "3" }
        ]
      }
    },
    {
      "type": "datePicker",
      "field": "startDate",
      "label": "开始日期"
    },
    {
      "type": "datePicker",
      "field": "endDate",
      "label": "结束日期"
    },
    {
      "type": "inputNumber",
      "field": "days",
      "label": "请假天数"
    },
    {
      "type": "textarea",
      "field": "reason",
      "label": "请假原因"
    }
  ]
}
```

### 5.3 条件规则JSON示例

```json
{
  "conditionGroups": [
    {
      "logic": "AND",
      "conditions": [
        {
          "field": "days",
          "operator": "gt",
          "value": 3
        },
        {
          "field": "leaveType",
          "operator": "eq",
          "value": "1"
        }
      ]
    },
    {
      "logic": "OR",
      "conditions": [
        {
          "field": "deptId",
          "operator": "in",
          "value": ["dept001", "dept002"]
        }
      ]
    }
  ]
}
```

---

## 六、总结

本方案基于现有Flowable流程引擎，通过自研可视化表单设计器、动态审批节点配置、复杂条件规则引擎等核心模块，构建一套完整的企业级流程管理解决方案。方案充分参考了RuoYi-Flowable、芋道源码、JeecgBoot等优秀开源项目的实现经验，确保技术方案的可行性和先进性。

**核心优势**：
1. 符合Flowable设计规范，兼容性好
2. 可视化配置，降低使用门槛
3. 灵活的审批策略，适应复杂业务场景
4. 深度组织集成，自动感知组织变更
5. 完善的监控分析，支持流程优化
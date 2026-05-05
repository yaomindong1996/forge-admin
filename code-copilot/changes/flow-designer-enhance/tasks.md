# 任务拆分 — 流程设计器优化
> 拆分顺序：数据模型 → 接口协议 → 后端实现 → 前端页面 → 前端组件改造
> 每个任务 = 可独立提交的原子变更（3-5 个文件）

## 前置条件
- [ ] 数据库可访问
- [ ] forge-plugin-flow 模块可编译
- [ ] forge-admin-ui 前端项目可启动

---

## Task 1: 创建SPEL模板数据库表

- **目标**: 创建 `sys_flow_spel_template` 表并初始化默认数据
- **涉及文件**:
    - `forge-plugin-flow/sql/spel_template.sql` — 新增，创建表 + 初始化数据

- **SQL内容**:

```sql
-- SPEL表达式模板表
CREATE TABLE IF NOT EXISTS `sys_flow_spel_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `expression` varchar(500) NOT NULL COMMENT 'SPEL表达式',
    `description` varchar(200) DEFAULT NULL COMMENT '描述说明',
    `category` varchar(50) DEFAULT 'general' COMMENT '分类：general/dept/role/region/custom',
    `example_params` varchar(500) DEFAULT NULL COMMENT '示例参数（JSON格式）',
    `status` int NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    `sort` int DEFAULT '100' COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`tenant_id`, `template_code`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程SPEL表达式模板';

-- 初始化默认模板数据（迁移自NodePropertiesPanel.vue硬编码模板）
INSERT INTO `sys_flow_spel_template` (`tenant_id`, `template_name`, `template_code`, `expression`, `description`, `category`, `status`, `sort`) VALUES
(1, '发起人', 'initiator', '${initiator}', '流程发起人作为审批人', 'general', 1, 1),
(1, '发起人上级', 'initiatorLeader', '${initiatorLeader}', '流程发起人的直属上级', 'general', 1, 2),
(1, '部门经理', 'deptManager', '${deptManager}', '当前部门的经理', 'general', 1, 3),
(1, 'HR', 'hr', '${hr}', '人力资源部门负责人', 'general', 1, 4),
(1, '根据部门查找负责人', 'findDeptManager', '${flowSpelService.findDeptManager(execution.getVariable("deptId"))}', '查找指定部门的负责人', 'dept', 1, 10),
(1, '根据角色查找用户', 'findUsersByRole', '${flowSpelService.findUsersByRole(execution.getVariable("roleKey"))}', '查找具有指定角色的所有用户', 'role', 1, 20),
(1, '根据行政区划查找负责人', 'findRegionManager', '${flowSpelService.findRegionManager(execution.getVariable("regionCode"))}', '查找指定行政区划的区域负责人', 'region', 1, 30),
(1, '发起人的直属上级', 'getInitiatorLeader', '${flowSpelService.getInitiatorLeader(execution)}', '流程发起人的直属上级作为审批人', 'general', 1, 5),
(1, '根据订单金额动态审批', 'amountDynamic', '${execution.getVariable("amount") > 10000 ? flowSpelService.findUsersByRole("finance_manager") : flowSpelService.findUsersByRole("finance_staff")}', '订单金额大于1万由财务经理审批', 'custom', 1, 50),
(1, '部门+角色组合查询', 'deptAndRole', '${flowSpelService.findUsersByDeptAndRole(execution.getVariable("deptId"), "dept_manager")}', '查找指定部门的部门经理', 'dept', 1, 15)
ON DUPLICATE KEY UPDATE `template_name` = VALUES(`template_name`);
```

- **验证**: 执行SQL后检查表结构和初始数据

---

## Task 2: 后端Entity + Mapper

- **目标**: 创建 `FlowSpelTemplate` 实体类和 Mapper
- **涉及文件**:
    - `forge-plugin-flow/src/main/java/.../entity/FlowSpelTemplate.java` — 新增
    - `forge-plugin-flow/src/main/java/.../mapper/FlowSpelTemplateMapper.java` — 新增
    - `forge-plugin-flow/src/main/resources/mapper/FlowSpelTemplateMapper.xml` — 新增

- **Entity代码** (`FlowSpelTemplate.java`):

```java
package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_flow_spel_template")
public class FlowSpelTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String templateName;
    private String templateCode;
    private String expression;
    private String description;
    private String category;
    private String exampleParams;
    private Integer status;
    private Integer sort;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

- **Mapper接口** (`FlowSpelTemplateMapper.java`):

```java
package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FlowSpelTemplateMapper extends BaseMapper<FlowSpelTemplate> {
}
```

- **Mapper XML** (`FlowSpelTemplateMapper.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mdframe.forge.starter.flow.mapper.FlowSpelTemplateMapper">
    <resultMap id="BaseResultMap" type="com.mdframe.forge.starter.flow.entity.FlowSpelTemplate">
        <id column="id" property="id"/>
        <result column="tenant_id" property="tenantId"/>
        <result column="template_name" property="templateName"/>
        <result column="template_code" property="templateCode"/>
        <result column="expression" property="expression"/>
        <result column="description" property="description"/>
        <result column="category" property="category"/>
        <result column="example_params" property="exampleParams"/>
        <result column="status" property="status"/>
        <result column="sort" property="sort"/>
        <result column="remark" property="remark"/>
        <result column="create_by" property="createBy"/>
        <result column="create_time" property="createTime"/>
        <result column="update_by" property="updateBy"/>
        <result column="update_time" property="updateTime"/>
        <result column="deleted" property="deleted"/>
    </resultMap>

    <!-- 查询启用状态的模板列表 -->
    <select id="selectEnabledList" resultMap="BaseResultMap">
        SELECT * FROM sys_flow_spel_template
        WHERE tenant_id = #{tenantId}
          AND status = 1
          AND deleted = 0
        ORDER BY sort ASC, id ASC
    </select>
</mapper>
```

---

## Task 3: 后端Service + Controller

- **目标**: 创建 Service 和 Controller，提供 CRUD API
- **涉及文件**:
    - `forge-plugin-flow/src/main/java/.../service/FlowSpelTemplateService.java` — 新增
    - `forge-plugin-flow/src/main/java/.../service/impl/FlowSpelTemplateServiceImpl.java` — 新增
    - `forge-flow-server/src/main/java/.../controller/FlowSpelTemplateController.java` — 新增

- **Service接口**:

```java
package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import java.util.List;

public interface FlowSpelTemplateService extends IService<FlowSpelTemplate> {
    List<FlowSpelTemplate> getEnabledList(Long tenantId);
}
```

- **ServiceImpl**:

```java
package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.mapper.FlowSpelTemplateMapper;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FlowSpelTemplateServiceImpl extends ServiceImpl<FlowSpelTemplateMapper, FlowSpelTemplate> implements FlowSpelTemplateService {
    @Override
    public List<FlowSpelTemplate> getEnabledList(Long tenantId) {
        return baseMapper.selectEnabledList(tenantId);
    }
}
```

- **Controller** (`FlowSpelTemplateController.java`):

```java
package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;
import com.mdframe.forge.starter.flow.service.FlowSpelTemplateService;
import com.mdframe.forge.starter.common.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/flow/spelTemplate")
@RequiredArgsConstructor
public class FlowSpelTemplateController {

    private final FlowSpelTemplateService spelTemplateService;

    // 分页查询
    @GetMapping("/page")
    public RespInfo<?> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        Page<FlowSpelTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlowSpelTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(templateName != null, FlowSpelTemplate::getTemplateName, templateName);
        wrapper.eq(category != null, FlowSpelTemplate::getCategory, category);
        wrapper.eq(status != null, FlowSpelTemplate::getStatus, status);
        wrapper.orderByAsc(FlowSpelTemplate::getSort);
        return RespInfo.success(spelTemplateService.page(page, wrapper));
    }

    // 启用状态列表（节点配置下拉用）
    @GetMapping("/list")
    public RespInfo<?> list() {
        Long tenantId = 1L; // 从上下文获取当前租户ID
        List<FlowSpelTemplate> list = spelTemplateService.getEnabledList(tenantId);
        return RespInfo.success(list);
    }

    // 详情
    @GetMapping("/{id}")
    public RespInfo<?> getById(@PathVariable Long id) {
        return RespInfo.success(spelTemplateService.getById(id));
    }

    // 新增
    @PostMapping
    public RespInfo<?> save(@RequestBody FlowSpelTemplate entity) {
        spelTemplateService.save(entity);
        return RespInfo.success();
    }

    // 修改
    @PutMapping
    public RespInfo<?> update(@RequestBody FlowSpelTemplate entity) {
        spelTemplateService.updateById(entity);
        return RespInfo.success();
    }

    // 删除
    @DeleteMapping("/{id}")
    public RespInfo<?> delete(@PathVariable Long id) {
        spelTemplateService.removeById(id);
        return RespInfo.success();
    }
}
```

---

## Task 4: 前端SPEL模板管理页面

- **目标**: 创建 `spelTemplate.vue` 管理页面
- **涉及文件**:
    - `forge-admin-ui/src/views/flow/spelTemplate.vue` — 新增

- **页面结构**: 使用 `AiCrudPage` 组件实现 CRUD

```vue
<template>
  <AiCrudPage
    ref="crudRef"
    :api-config="{
      list: 'get@/api/flow/spelTemplate/page',
      detail: 'get@/api/flow/spelTemplate/{id}',
      add: 'post@/api/flow/spelTemplate',
      update: 'put@/api/flow/spelTemplate',
      delete: 'delete@/api/flow/spelTemplate/{id}'
    }"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    row-key="id"
    :load-detail-on-edit="true"
  />
</template>

<script setup>
import { AiCrudPage } from '@/components/ai-form'
import { h, ref } from 'vue'
import { NTag } from 'naive-ui'

defineOptions({ name: 'FlowSpelTemplate' })

const categoryOptions = [
  { label: '通用', value: 'general' },
  { label: '部门', value: 'dept' },
  { label: '角色', value: 'role' },
  { label: '行政区划', value: 'region' },
  { label: '自定义', value: 'custom' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const searchSchema = [
  { field: 'templateName', label: '模板名称', type: 'input', props: { placeholder: '请输入模板名称' } },
  { field: 'category', label: '分类', type: 'select', props: { options: categoryOptions, clearable: true } },
  { field: 'status', label: '状态', type: 'select', props: { options: statusOptions, clearable: true } },
]

const tableColumns = [
  { prop: 'templateName', label: '模板名称', width: 150 },
  { prop: 'templateCode', label: '模板编码', width: 120 },
  { prop: 'expression', label: '表达式', ellipsis: { tooltip: true } },
  { prop: 'category', label: '分类', width: 100, render: (row) => {
    const item = categoryOptions.find(o => o.value === row.category)
    return item ? item.label : row.category
  }},
  { prop: 'status', label: '状态', width: 80, render: (row) => h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, () => row.status === 1 ? '启用' : '禁用') },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 160 },
]

const editSchema = [
  { field: 'templateName', label: '模板名称', type: 'input', rules: [{ required: true, message: '请输入模板名称' }] },
  { field: 'templateCode', label: '模板编码', type: 'input', rules: [{ required: true, message: '请输入模板编码' }] },
  { field: 'expression', label: 'SPEL表达式', type: 'textarea', rules: [{ required: true, message: '请输入表达式' }], props: { rows: 3, placeholder: '${flowSpelService.findDeptManager(...)}' } },
  { field: 'description', label: '描述说明', type: 'input', props: { placeholder: '简要描述模板用途' } },
  { field: 'category', label: '分类', type: 'select', props: { options: categoryOptions } },
  { field: 'exampleParams', label: '示例参数', type: 'textarea', props: { rows: 2, placeholder: 'JSON格式，如 {"deptId": "123"}' } },
  { field: 'status', label: '状态', type: 'switch', defaultValue: 1, props: { checkedValue: 1, uncheckedValue: 0 } },
  { field: 'sort', label: '排序', type: 'input-number', defaultValue: 100, props: { min: 0, max: 999 } },
  { field: 'remark', label: '备注', type: 'textarea', props: { rows: 2 } },
]
</script>
```

---

## Task 5: NodePropertiesPanel改用Tab + 手动保存

- **目标**: 重构节点配置面板，使用 `n-tabs` 分隔 + 底部添加"保存配置"按钮
- **涉及文件**:
    - `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue` — 重构

- **关键改动点**:
  1. 将 `n-collapse` 替换为 `n-tabs`
  2. Tab分组：基础属性 / 审批设置 / 会签配置 / 监听器
  3. 底部添加固定"保存配置"按钮
  4. 添加"未保存"状态提示
  5. 从 API `/api/flow/spelTemplate/list` 加载 SPEL 模板

- **Tab结构设计**:

```vue
<template>
  <div class="node-properties-panel">
    <!-- 用户/角色选择弹窗保持不变 -->
    
    <!-- Tab分隔 -->
    <n-tabs v-model:value="activeTab" type="line" size="small">
      <!-- 基础属性Tab -->
      <n-tab-pane name="basic" tab="基础属性">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="节点ID">
            <n-input v-model:value="properties.id" @input="markDirty" />
          </n-form-item>
          <n-form-item label="节点名称">
            <n-input v-model:value="properties.name" @input="markDirty" />
          </n-form-item>
          <n-form-item label="节点描述">
            <n-input v-model:value="properties.documentation" type="textarea" @input="markDirty" />
          </n-form-item>
        </n-form>
      </n-tab-pane>

      <!-- 审批设置Tab（仅用户任务） -->
      <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="approval" tab="审批设置">
        <!-- 原审批设置内容迁移到此 -->
      </n-tab-pane>

      <!-- 会签配置Tab -->
      <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="multiInstance" tab="会签配置">
        <!-- 原会签配置内容迁移到此 -->
      </n-tab-pane>

      <!-- 监听器Tab -->
      <n-tab-pane v-if="showExecutionListener" name="listener" tab="监听器">
        <!-- 原监听器内容迁移到此 -->
      </n-tab-pane>
    </n-tabs>

    <!-- 底部固定按钮区 -->
    <div class="panel-footer">
      <n-alert v-if="isDirty" type="warning" size="small">
        配置已修改，请点击保存按钮生效
      </n-alert>
      <n-button type="primary" block :loading="saving" @click="handleSaveConfig">
        <template #icon>
          <i class="i-material-symbols:save" />
        </template>
        保存配置
      </n-button>
    </div>
  </div>
</template>

<script setup>
// 新增状态
const activeTab = ref('basic')
const isDirty = ref(false)
const saving = ref(false)
const spelTemplatesFromApi = ref([])

// 标记为未保存
function markDirty() {
  isDirty.value = true
}

// 从API加载SPEL模板
async function loadSpelTemplates() {
  try {
    const res = await request.get('/api/flow/spelTemplate/list')
    if (res.code === 200) {
      spelTemplatesFromApi.value = (res.data || []).map(t => ({
        label: t.templateName,
        value: t.expression,
        description: t.description
      }))
    }
  } catch (e) {
    console.error('加载SPEL模板失败', e)
  }
}

// 手动保存配置
async function handleSaveConfig() {
  saving.value = true
  try {
    // 执行所有必要的update方法
    updateProperty('id')
    updateProperty('name')
    updateProperty('documentation')
    
    if (elementType.value === 'bpmn:UserTask') {
      updateUserTaskAssignee()
      updateMultiInstance()
      updateTaskListeners()
    }
    
    // 触发父组件更新
    emit('update')
    
    isDirty.value = false
    window.$message?.success('配置已保存')
  } finally {
    saving.value = false
  }
}

// 组件挂载时加载SPEL模板
onMounted(() => {
  loadSpelTemplates()
})
</script>

<style scoped>
.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  background: var(--surface);
  position: sticky;
  bottom: 0;
}
</style>
```

---

## Task 6: NodePropertiesPanel从API加载SPEL模板

- **目标**: 替换硬编码模板，改为从 API 加载
- **涉及文件**:
    - `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue` — 修改

- **关键改动**:
  1. 删除 `spelTemplates` 硬编码数组（第705-742行）
  2. 新增 `spelTemplatesFromApi` ref
  3. 在 `onMounted` 中调用 `/api/flow/spelTemplate/list`
  4. SPEL模板下拉框使用动态数据

---

## Task 7: XML预览弹窗样式优化

- **目标**: 优化XML预览弹窗，添加下载按钮和语法高亮
- **涉及文件**:
    - `forge-admin-ui/src/components/bpmn/FlowModeler.vue` — 修改

- **关键改动**:
  1. 弹窗添加 `max-height: 80vh`
  2. 内部容器添加滚动和背景色
  3. 添加"复制XML"和"下载XML"按钮
  4. 代码块添加 `word-wrap`

```vue
<!-- XML预览弹窗优化 -->
<n-modal 
  v-model:show="showPreviewModal" 
  preset="card" 
  title="BPMN XML 预览" 
  style="width: 900px; max-height: 80vh"
>
  <div class="xml-preview-container">
    <n-code 
      :code="previewXml" 
      language="xml" 
      :show-line-numbers="true"
      :word-wrap="true"
    />
  </div>
  
  <template #footer>
    <n-space justify="end">
      <n-button @click="handleCopyXml">
        <template #icon>
          <i class="i-material-symbols:content-copy" />
        </template>
        复制XML
      </n-button>
      <n-button type="primary" @click="handleDownloadXmlFromPreview">
        <template #icon>
          <i class="i-material-symbols:download" />
        </template>
        下载XML
      </n-button>
      <n-button @click="showPreviewModal = false">
        关闭
      </n-button>
    </n-space>
  </template>
</n-modal>

<script setup>
// 新增复制功能
function handleCopyXml() {
  navigator.clipboard.writeText(previewXml.value)
  window.$message?.success('XML已复制到剪贴板')
}

// 从预览弹窗下载
function handleDownloadXmlFromPreview() {
  downloadFile(previewXml.value, 'diagram.bpmn', 'application/xml')
  window.$message?.success('下载成功')
}
</script>

<style scoped>
.xml-preview-container {
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: auto;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
}

:deep(.n-code pre) {
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.n-code) {
  background: transparent;
}
</style>
```

---

## Task 8: 新增菜单资源

- **目标**: 在流程配置目录下添加"SPEL模板管理"菜单
- **涉及文件**:
    - `forge-plugin-flow/sql/spel_template_menu.sql` — 新增

```sql
-- SPEL模板管理菜单（置于流程配置目录下）
-- 假设流程配置目录ID为 @flow_admin_id

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, 'SPEL模板管理', @flow_admin_id, 2, 5, '/flow/admin/spelTemplate', '/flow/spelTemplate', 0, 0, 1, 1, 'mdi:code-braces', 0, 0, 'SPEL表达式模板管理', NOW(), NOW());

SET @spel_template_menu_id = LAST_INSERT_ID();

-- 按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES 
(0, '新增模板', @spel_template_menu_id, 3, 1, 'flow:spelTemplate:create', 0, 0, 1, 1, '新增SPEL模板', NOW(), NOW()),
(0, '编辑模板', @spel_template_menu_id, 3, 2, 'flow:spelTemplate:update', 0, 0, 1, 1, '编辑SPEL模板', NOW(), NOW()),
(0, '删除模板', @spel_template_menu_id, 3, 3, 'flow:spelTemplate:delete', 0, 0, 1, 1, '删除SPEL模板', NOW(), NOW());
```

---

## Task 9: 验证与测试

- **目标**: 编译测试 + 功能验证
- **验证点**:
    1. 后端编译通过 `mvn clean install`
    2. 前端编译通过 `pnpm build`
    3. SPEL模板管理页面 CRUD 功能正常
    4. 节点配置 Tab 切换 + 保存按钮正常
    5. XML 预览弹窗样式正常，下载功能正常
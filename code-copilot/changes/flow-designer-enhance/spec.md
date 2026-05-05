# 流程设计器优化（节点配置改进 + SPEL模板管理 + XML预览优化）
> status: propose
> created: 2026-05-05
> complexity: 🟡中等

## 1. 背景与目标

**为什么做**：
1. 当前节点配置面板使用 `n-collapse` 折叠面板，依赖 `@blur` 失焦自动保存，用户体验不佳，配置可能丢失
2. SPEL 表达式模板硬编码在 `NodePropertiesPanel.vue` 中（第705-742行），无法动态管理
3. XML 预览弹窗（`FlowModeler.vue` 第153行）无高度限制，内容溢出，缺少下载和语法高亮功能

**做完后的效果**：
- 节点配置改用 Tab 分隔，底部添加"保存配置"按钮，用户明确知道配置是否生效
- SPEL 表达式模板独立管理页面，支持增删改查，节点配置时从 API 动态加载
- XML 预览弹窗优化样式（最大高度限制 + 滚动条），支持下载和语法高亮

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

**前端核心文件**：
| 文件 | 功能 | 代码位置 |
|------|------|----------|
| `forge-admin-ui/src/components/bpmn/NodePropertiesPanel.vue` | 节点配置面板 | 第40-60行（n-collapse折叠面板），第705-742行（SPEL模板硬编码） |
| `forge-admin-ui/src/components/bpmn/FlowModeler.vue` | BPMN画布组件 | 第153行（XML预览弹窗 n-modal），第518-527行（handlePreview函数） |
| `forge-admin-ui/src/views/flow/design.vue` | 流程设计页面 | 第141-157行（右侧停靠属性面板），第436-480行（handleSaveDraft保存逻辑） |

**后端核心文件**：
| 文件 | 功能 |
|------|------|
| `forge-plugin-flow/src/main/java/.../controller/FlowNodeConfigController.java` | 节点配置API |
| `forge-plugin-flow/src/main/java/.../entity/FlowNodeConfig.java` | 节点配置实体 |
| `forge-plugin-flow/src/main/java/.../entity/FlowConditionRule.java` | 条件规则实体（含conditionExpression字段） |

**数据库表**：
| 表名 | 说明 |
|------|------|
| `sys_flow_node_config` | 审批节点配置表（flow_tables_v2.sql 第189-225行） |
| `sys_flow_condition_rule` | 条件规则表（flow_tables_v2.sql 第244-264行） |

### 2.2 现有实现

**节点配置保存逻辑**：
- `NodePropertiesPanel.vue` 使用 `@blur="updateProperty('xxx')"` 触发保存
- `updateProperty` 函数调用 `modeling.updateProperties` 更新 BPMN 元素
- 通过 `emit('update')` 通知父组件 `design.vue`，触发 `handleBpmnChange`

**SPEL模板现状**：
```javascript
// NodePropertiesPanel.vue 第705-742行
const spelTemplates = [
  { label: '根据部门查找负责人', value: '${flowSpelService.findDeptManager(...)}', ... },
  { label: '根据角色查找用户', value: '${flowSpelService.findUsersByRole(...)}', ... },
  // ... 共7个硬编码模板
]
```

**XML预览现状**：
```vue
<!-- FlowModeler.vue 第153行 -->
<n-modal v-model:show="showPreviewModal" preset="card" title="BPMN XML 预览" style="width: 800px">
  <n-code :code="previewXml" language="xml" :show-line-numbers="true" />
</n-modal>
```
- 无 `max-height` 限制，XML 内容过长时溢出
- 无下载按钮、无复制功能

### 2.3 发现与风险

| 问题 | 风险等级 | 说明 |
|------|----------|------|
| 失焦保存不稳定 | 🔴高 | 用户点击其他区域或切换Tab时可能未触发 `@blur`，配置丢失 |
| SPEL模板硬编码 | 🟡中 | 无法动态添加模板，不同项目无法定制 |
| XML预览溢出 | 🟡中 | 长流程XML导致弹窗撑满屏幕，影响用户体验 |
| 无保存状态提示 | 🟡中 | 用户不知道配置是否已生效 |

## 3. 功能点

- [ ] **功能1：节点配置Tab分隔 + 手动保存按钮**
  - 输入：用户在节点配置面板编辑属性
  - 处理：使用 `n-tabs` 分隔配置项，底部添加"保存配置"按钮
  - 输出：点击保存按钮后执行所有 pending 更新，显示"已保存"提示

- [ ] **功能2：SPEL表达式模板独立管理**
  - 输入：管理员在 SPEL模板管理页面创建/编辑模板
  - 处理：模板存储到 `sys_flow_spel_template` 表，节点配置时从 API 加载
  - 输出：节点配置下拉框显示动态模板列表

- [ ] **功能3：XML预览样式优化**
  - 输入：用户点击"预览XML"按钮
  - 处理：弹窗最大高度80vh，内部容器60vh+滚动条，添加下载和复制按钮
  - 输出：XML内容在限定区域显示，支持下载为 `.bpmn` 文件

## 4. 业务规则

### 节点配置Tab分组规则
| Tab名称 | 显示条件 | 包含配置项 |
|---------|----------|-----------|
| 基础属性 | 所有节点 | 节点ID、名称、描述 |
| 审批设置 | `bpmn:UserTask` | 审批人类型、表单配置、优先级、截止日期 |
| 会签配置 | `bpmn:UserTask` | 多实例类型、完成条件、通过比例 |
| 监听器 | `bpmn:UserTask`/`bpmn:ServiceTask`/Start/End | 任务监听器、执行监听器 |

### SPEL模板分类规则
| 分类 | 说明 |
|------|------|
| `general` | 通用模板（发起人、上级等） |
| `dept` | 部门相关（部门负责人、部门成员等） |
| `role` | 角色相关（按角色查找用户） |
| `region` | 行政区划相关（区域负责人） |
| `custom` | 用户自定义模板 |

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 新增 | `sys_flow_spel_template` | 全表 | SPEL表达式模板管理表 |

**新增表SQL**：
```sql
CREATE TABLE `sys_flow_spel_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tenant_id` bigint NOT NULL DEFAULT '1' COMMENT '租户编号',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `expression` varchar(500) NOT NULL COMMENT 'SPEL表达式',
    `description` varchar(200) DEFAULT NULL COMMENT '描述说明',
    `category` varchar(50) DEFAULT 'general' COMMENT '分类',
    `example_params` varchar(500) DEFAULT NULL COMMENT '示例参数JSON',
    `status` int NOT NULL DEFAULT '1' COMMENT '状态：0禁用，1启用',
    `sort` int DEFAULT '100' COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` bigint DEFAULT NULL,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` int DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`tenant_id`, `template_code`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程SPEL表达式模板';
```

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 新增 | `/api/flow/spelTemplate/page` | GET | 分页查询SPEL模板 |
| 新增 | `/api/flow/spelTemplate/list` | GET | 启用状态模板列表（节点配置下拉） |
| 新增 | `/api/flow/spelTemplate/{id}` | GET | 模板详情 |
| 新增 | `/api/flow/spelTemplate` | POST | 新增模板 |
| 新增 | `/api/flow/spelTemplate` | PUT | 修改模板 |
| 新增 | `/api/flow/spelTemplate/{id}` | DELETE | 删除模板 |

## 7. 影响范围

| 影域 | 文件/模块 | 影响说明 |
|------|-----------|----------|
| 前端组件 | `NodePropertiesPanel.vue` | 重构为Tab布局 + 手动保存 |
| 前端组件 | `FlowModeler.vue` | XML预览弹窗样式优化 |
| 前端页面 | 新增 `spelTemplate.vue` | SPEL模板管理页面 |
| 后端模块 | `forge-plugin-flow` | 新增Entity/Mapper/Service/Controller |
| 数据库 | 新增表 | `sys_flow_spel_template` |
| 菜单 | 新增菜单 | SPEL模板管理（置于流程配置下） |

## 8. 风险与关注点

| 风险 | 级别 | 应对措施 |
|------|------|----------|
| Tab切换时配置丢失 | 🟡中 | 添加"未保存"提示，切换Tab时提醒保存 |
| SPEL模板迁移 | 🟡中 | 保留原有硬编码模板作为默认数据，新模板可通过API添加 |
| XML预览性能 | 🟢低 | 大文件时可能渲染慢，建议限制显示长度或使用虚拟滚动 |

## 8.5 测试策略

- **测试范围**：
  - 节点配置Tab切换 + 手动保存按钮功能验证
  - SPEL模板CRUD操作验证
  - XML预览弹窗样式验证（滚动、下载、复制）
- **覆盖率目标**：核心功能100%验证
- **独立 Test Spec**：否（随功能验证）

## 9. 待澄清

- [x] Tab分组方案确认 → 已确认（基础/审批/会签/监听器）
- [x] SPEL模板是否绑定流程模型 → 不绑定，全局通用
- [x] XML预览需要下载功能 → 已确认

## 10. 技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| Tab组件 | `n-tabs` | Naive UI原生组件，样式统一 |
| 手动保存按钮位置 | 面板底部固定 | 醒目，不遮挡配置项 |
| SPEL模板表设计 | 不绑定model_id | 用户确认全局通用，便于复用 |
| XML代码高亮 | `n-code` + word-wrap | 已有组件，添加换行样式即可 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| 创建Spec | 完成 | spec.md | 本文件 |

## 12. 审查结论

（待审查后填写）

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-05-05
- **确认人**：用户确认方案
- **确认内容**：
  1. Tab分组方案：基础属性 + 审批设置 + 会签配置 + 监听器
  2. SPEL模板不绑定流程模型，全局通用
  3. XML预览添加下载按钮 + 语法高亮
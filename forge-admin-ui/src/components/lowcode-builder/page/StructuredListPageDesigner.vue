<template>
  <div class="structured-designer">
    <div class="structured-workspace" :class="{ 'config-expanded': configExpanded, 'config-collapsed': configCollapsed }">
      <div v-show="!configExpanded" class="structured-preview-column">
        <section class="surface-section crud-preview-section">
          <div class="section-head">
            <div>
              <div class="section-title">
                默认 CRUD 组件预览
              </div>
              <div class="section-desc">
                当前页面默认使用 AiCrudPage。右侧配置字段、排序、列样式后，会同步到自由画布里的 CRUD 组件。
              </div>
            </div>
            <n-space size="small" align="center">
              <NSwitch
                :value="searchZone?.enabled !== false"
                size="small"
                @update:value="patchZone('search', { enabled: $event })"
              />
              <NButton size="small" @click="resetSearchFields">
                恢复默认
              </NButton>
            </n-space>
          </div>

          <div
            class="crud-component-preview-frame"
            :class="{ 'tree-crud-preview': layoutType === 'tree-crud' }"
          >
            <GridBlockRenderer
              v-if="layoutType === 'tree-crud'"
              :block="treePreviewBlock"
              :fields="fields"
              :selected="false"
            />
            <GridBlockRenderer
              :block="crudPreviewBlock"
              :fields="fields"
              :selected="false"
            />
          </div>

          <div class="crud-preview-status">
            <span>排序：{{ defaultSortText }}</span>
            <span>查询字段：{{ searchFields.length }}</span>
            <span>列表列：{{ tableFields.length }}/{{ fields.length }}</span>
            <span v-if="layoutType === 'tree-crud'">模板：左树筛选右表</span>
            <NButton size="tiny" type="primary" secondary @click="openFieldPanel('table')">
              配置列表字段
            </NButton>
          </div>
        </section>
      </div>

      <button
        v-if="configCollapsed"
        type="button"
        class="config-collapsed-rail"
        title="展开 CRUD 配置"
        @click="configCollapsed = false"
      >
        <span>配置</span>
      </button>

      <aside v-else class="structured-config-panel">
        <div class="config-panel-head">
          <div>
            <div class="config-title">
              CRUD 快速配置
            </div>
            <div class="config-desc">
              按步骤配置默认 CRUD 组件，不需要理解内部 JSON。复杂页面布局请切回自由画布。
            </div>
          </div>
          <n-space size="small" align="center">
            <NButton size="small" secondary @click="configExpanded = !configExpanded">
              {{ configExpanded ? '收起配置' : '展开配置' }}
            </NButton>
            <NButton size="small" secondary @click="collapseConfigPanel">
              收起
            </NButton>
            <NButton size="small" type="primary" secondary @click="resetTableFields">
              恢复列
            </NButton>
          </n-space>
        </div>
        <div class="config-panel-search designer-panel-search">
          <NInput
            v-model:value="configKeyword"
            clearable
            size="small"
            placeholder="搜索配置项，例如：接口、弹窗、提交前、树、字段"
          >
            <template #prefix>
              <NIcon><SearchOutline /></NIcon>
            </template>
          </NInput>
        </div>
        <div class="config-scroll-body">
          <div class="config-step-list">
            <div class="config-step active">
              1. 选模板
            </div>
            <div class="config-step active">
              2. 配字段
            </div>
            <div class="config-step">
              3. 保存发布
            </div>
          </div>
          <section v-if="configSectionVisible(['字段配置入口', '字段', '查询字段', '列表字段', '列样式'])" class="config-section field-config-entry">
            <div class="config-section-title">
              字段配置入口
            </div>
            <div class="field-config-entry-actions">
              <NButton
                size="small"
                :type="activeFieldZone === 'search' ? 'primary' : 'default'"
                secondary
                @click="openFieldPanel('search')"
              >
                配置查询字段
              </NButton>
              <NButton
                size="small"
                :type="activeFieldZone === 'table' ? 'primary' : 'default'"
                secondary
                @click="openFieldPanel('table')"
              >
                配置列表字段 / 列样式
              </NButton>
            </div>
            <div class="config-section-tip">
              列表字段里可以配置列宽、固定列、文字颜色、点击跳转、跳转参数、名称字段和渲染方式。字典字段默认按“字典标签”展示，也可以手动切换。
            </div>
          </section>
          <section v-if="configSectionVisible(['基础显示', '标题', '主键', '排序', '行间距', '分页', '导入', '导出', '批量删除', '边框', '斑马纹'])" class="config-section">
            <div class="config-section-title">
              基础显示
            </div>
            <div class="config-form-grid">
              <label class="config-control">
                <span>组件标题</span>
                <NInput
                  :value="tableZone?.props?.title || '业务列表'"
                  size="small"
                  placeholder="列表标题"
                  @update:value="updateTableProp('title', $event)"
                />
              </label>
              <label class="config-control">
                <span>主键字段</span>
                <NSelect
                  :value="tableZone?.props?.rowKey || 'id'"
                  :options="sortFieldOptions"
                  size="small"
                  filterable
                  tag
                  @update:value="updateTableProp('rowKey', $event || 'id')"
                />
              </label>
              <label class="config-control">
                <span>默认排序字段</span>
                <NSelect
                  :value="tableZone?.props?.defaultSortField || 'id'"
                  :options="sortFieldOptions"
                  size="small"
                  filterable
                  @update:value="updateTableProp('defaultSortField', $event)"
                />
              </label>
              <label class="config-control">
                <span>排序方向</span>
                <NSelect
                  :value="tableZone?.props?.defaultSortOrder || 'desc'"
                  :options="sortOrderOptions"
                  size="small"
                  @update:value="updateTableProp('defaultSortOrder', $event)"
                />
              </label>
              <label class="config-control">
                <span>批量列对齐</span>
                <NSelect
                  :value="tableZone?.props?.globalAlign || 'left'"
                  :options="alignOptions"
                  size="small"
                  @update:value="applyTableGlobalAlign"
                />
              </label>
              <label class="config-control">
                <span>行间距</span>
                <NInputNumber
                  :value="tableZone?.props?.rowGap ?? 8"
                  size="small"
                  :min="0"
                  :max="32"
                  :step="2"
                  :show-button="false"
                  @update:value="updateTableRowGap"
                />
              </label>
              <label class="config-control">
                <span>表格尺寸</span>
                <NSelect
                  :value="tableZone?.props?.tableSize || 'small'"
                  :options="sizeOptions"
                  size="small"
                  @update:value="updateTableProp('tableSize', $event || 'small')"
                />
              </label>
              <label class="config-control">
                <span>展示模式</span>
                <NSelect
                  :value="tableZone?.props?.renderMode || 'table'"
                  :options="renderModeOptions"
                  size="small"
                  @update:value="updateTableProp('renderMode', $event || 'table')"
                />
              </label>
            </div>
            <div class="config-switch-grid">
              <label>
                <NSwitch
                  :value="searchZone?.enabled !== false"
                  size="small"
                  @update:value="patchZone('search', { enabled: $event })"
                />
                <span>搜索区</span>
              </label>
              <label>
                <NSwitch
                  :value="tableZone?.props?.showPagination !== false"
                  size="small"
                  @update:value="updateTableProp('showPagination', $event)"
                />
                <span>分页</span>
              </label>
              <label>
                <NSwitch
                  :value="tableZone?.props?.hideSelection !== true"
                  size="small"
                  @update:value="updateTableProp('hideSelection', !$event)"
                />
                <span>勾选列</span>
              </label>
              <label>
                <NSwitch
                  :value="!!tableZone?.props?.showImport"
                  size="small"
                  @update:value="updateTableProp('showImport', $event)"
                />
                <span>显示导入</span>
              </label>
              <label>
                <NSwitch
                  :value="!!tableZone?.props?.showExport"
                  size="small"
                  @update:value="updateTableProp('showExport', $event)"
                />
                <span>显示导出</span>
              </label>
              <label>
                <NSwitch
                  :value="tableZone?.props?.enableCustomQuery !== false"
                  size="small"
                  @update:value="updateTableProp('enableCustomQuery', $event)"
                />
                <span>自定义查询</span>
              </label>
              <label>
                <NSwitch
                  :value="tableZone?.props?.hideBatchDelete !== true"
                  size="small"
                  @update:value="updateTableProp('hideBatchDelete', !$event)"
                />
                <span>批量删除</span>
              </label>
              <label>
                <NSwitch
                  :value="!!tableZone?.props?.bordered"
                  size="small"
                  @update:value="updateTableProp('bordered', $event)"
                />
                <span>边框</span>
              </label>
              <label>
                <NSwitch
                  :value="!!tableZone?.props?.striped"
                  size="small"
                  @update:value="updateTableProp('striped', $event)"
                />
                <span>斑马纹</span>
              </label>
            </div>
          </section>
          <section v-if="configSectionVisible(['接口配置', '接口', 'api', '分页接口', '新增接口', '编辑接口', '删除接口', '列表数据字段', '总数字段', '真实接口预览'])" class="config-section">
            <div class="config-section-title">
              接口配置
            </div>
            <div class="config-section-tip">
              不填时使用业务对象默认接口；需要接已有后端接口时再配置这些地址。
            </div>
            <div class="config-form-grid one-col">
              <label class="config-control">
                <span>接口前缀</span>
                <NInput
                  :value="tableZone?.props?.api || defaultApiValues.api"
                  size="small"
                  :placeholder="defaultApiValues.api"
                  @update:value="updateTableProp('api', $event)"
                />
              </label>
              <div class="config-switch-grid two">
                <label>
                  <NSwitch
                    :value="tableZone?.props?.previewLiveData === true"
                    size="small"
                    @update:value="updateTableProp('previewLiveData', $event)"
                  />
                  <span>真实接口预览</span>
                </label>
              </div>
              <div class="config-section-tip">
                默认不请求真实接口；打开后中间预览会根据接口配置加载数据，适合验证接口字段和字典回显。
              </div>
              <label class="config-control">
                <span>分页接口</span>
                <NInput
                  :value="tableZone?.props?.listApi || defaultApiValues.listApi"
                  size="small"
                  :placeholder="defaultApiValues.listApi"
                  @update:value="updateTableProp('listApi', $event)"
                />
              </label>
              <div class="config-form-grid nested">
                <label class="config-control">
                  <span>详情接口</span>
                  <NInput
                    :value="tableZone?.props?.detailApi || defaultApiValues.detailApi"
                    size="small"
                    :placeholder="defaultApiValues.detailApi"
                    @update:value="updateTableProp('detailApi', $event)"
                  />
                </label>
                <label class="config-control">
                  <span>新增接口</span>
                  <NInput
                    :value="tableZone?.props?.createApi || defaultApiValues.createApi"
                    size="small"
                    :placeholder="defaultApiValues.createApi"
                    @update:value="updateTableProp('createApi', $event)"
                  />
                </label>
                <label class="config-control">
                  <span>编辑接口</span>
                  <NInput
                    :value="tableZone?.props?.updateApi || defaultApiValues.updateApi"
                    size="small"
                    :placeholder="defaultApiValues.updateApi"
                    @update:value="updateTableProp('updateApi', $event)"
                  />
                </label>
                <label class="config-control">
                  <span>删除接口</span>
                  <NInput
                    :value="tableZone?.props?.deleteApi || defaultApiValues.deleteApi"
                    size="small"
                    :placeholder="defaultApiValues.deleteApi"
                    @update:value="updateTableProp('deleteApi', $event)"
                  />
                </label>
                <label class="config-control">
                  <span>列表数据字段</span>
                  <NInput
                    :value="tableZone?.props?.listDataField || 'records'"
                    size="small"
                    placeholder="records"
                    @update:value="updateTableProp('listDataField', $event || 'records')"
                  />
                </label>
                <label class="config-control">
                  <span>总数字段</span>
                  <NInput
                    :value="tableZone?.props?.listTotalField || 'total'"
                    size="small"
                    placeholder="total"
                    @update:value="updateTableProp('listTotalField', $event || 'total')"
                  />
                </label>
              </div>
            </div>
          </section>
          <section v-if="configSectionVisible(['搜索和表格', '搜索', '查询', '表格', '最大高度', '横向滚动', '搜索折叠', '模式切换'])" class="config-section">
            <div class="config-section-title">
              搜索和表格
            </div>
            <div class="config-form-grid">
              <label class="config-control">
                <span>搜索列数</span>
                <NInputNumber
                  :value="tableZone?.props?.searchGridCols || 4"
                  size="small"
                  :min="1"
                  :max="6"
                  :show-button="false"
                  @update:value="updateTableProp('searchGridCols', $event || 4)"
                />
              </label>
              <label class="config-control">
                <span>默认显示查询项</span>
                <NInputNumber
                  :value="tableZone?.props?.searchMaxVisibleFields || 3"
                  size="small"
                  :min="1"
                  :max="12"
                  :show-button="false"
                  @update:value="updateTableProp('searchMaxVisibleFields', $event || 3)"
                />
              </label>
              <label class="config-control">
                <span>搜索标签宽度</span>
                <NInput
                  :value="tableZone?.props?.searchLabelWidth || 'auto'"
                  size="small"
                  placeholder="auto / 100px"
                  @update:value="updateTableProp('searchLabelWidth', $event || 'auto')"
                />
              </label>
              <label class="config-control">
                <span>表格最大高度</span>
                <NInput
                  :value="tableZone?.props?.maxHeight || ''"
                  size="small"
                  placeholder="例如 520px，留空自适应"
                  @update:value="updateTableProp('maxHeight', $event || undefined)"
                />
              </label>
              <label class="config-control">
                <span>横向滚动宽度</span>
                <NInput
                  :value="tableZone?.props?.scrollX || ''"
                  size="small"
                  placeholder="例如 1200"
                  @update:value="updateTableProp('scrollX', $event || undefined)"
                />
              </label>
            </div>
            <div class="config-switch-grid">
              <label>
                <NSwitch
                  :value="tableZone?.props?.searchEnableCollapse !== false"
                  size="small"
                  @update:value="updateTableProp('searchEnableCollapse', $event)"
                />
                <span>查询折叠</span>
              </label>
              <label>
                <NSwitch
                  :value="tableZone?.props?.showRenderModeSwitch !== false"
                  size="small"
                  @update:value="updateTableProp('showRenderModeSwitch', $event)"
                />
                <span>模式切换</span>
              </label>
            </div>
          </section>
          <section v-if="configSectionVisible(['新增编辑弹窗', '新增', '编辑', '弹窗', '抽屉', '表单', '标签', '详情宽度'])" class="config-section">
            <div class="config-section-title">
              新增编辑弹窗
            </div>
            <div class="config-section-tip">
              新增、编辑弹窗使用当前表单设计字段渲染；这里控制弹窗形态和表单布局。
            </div>
            <div class="config-form-grid">
              <label class="config-control">
                <span>弹出方式</span>
                <NSelect
                  :value="tableZone?.props?.modalType || 'modal'"
                  :options="modalTypeOptions"
                  size="small"
                  @update:value="updateTableProp('modalType', $event || 'modal')"
                />
              </label>
              <label class="config-control">
                <span>抽屉方向</span>
                <NSelect
                  :value="tableZone?.props?.drawerPlacement || 'right'"
                  :options="drawerPlacementOptions"
                  size="small"
                  @update:value="updateTableProp('drawerPlacement', $event || 'right')"
                />
              </label>
              <label class="config-control">
                <span>弹窗宽度</span>
                <NInput
                  :value="tableZone?.props?.modalWidth || '800px'"
                  size="small"
                  placeholder="800px / 80vw"
                  @update:value="updateTableProp('modalWidth', $event || '800px')"
                />
              </label>
              <label class="config-control">
                <span>详情宽度</span>
                <NInput
                  :value="tableZone?.props?.detailModalWidth || 'min(1080px, 92vw)'"
                  size="small"
                  placeholder="min(1080px, 92vw)"
                  @update:value="updateTableProp('detailModalWidth', $event || 'min(1080px, 92vw)')"
                />
              </label>
              <label class="config-control">
                <span>表单列数</span>
                <NInputNumber
                  :value="tableZone?.props?.editGridCols || 1"
                  size="small"
                  :min="1"
                  :max="4"
                  :show-button="false"
                  @update:value="updateTableProp('editGridCols', $event || 1)"
                />
              </label>
              <label class="config-control">
                <span>表单尺寸</span>
                <NSelect
                  :value="tableZone?.props?.editSize || 'medium'"
                  :options="sizeOptions"
                  size="small"
                  @update:value="updateTableProp('editSize', $event || 'medium')"
                />
              </label>
              <label class="config-control">
                <span>标签位置</span>
                <NSelect
                  :value="tableZone?.props?.editLabelPlacement || 'left'"
                  :options="labelPlacementOptions"
                  size="small"
                  @update:value="updateTableProp('editLabelPlacement', $event || 'left')"
                />
              </label>
              <label class="config-control">
                <span>标签宽度</span>
                <NInput
                  :value="tableZone?.props?.editLabelWidth || 'auto'"
                  size="small"
                  placeholder="auto / 100px"
                  @update:value="updateTableProp('editLabelWidth', $event || 'auto')"
                />
              </label>
            </div>
          </section>
          <section v-if="configSectionVisible(['工具栏和事件', '工具栏', '事件', '按钮文案', '导出文件名', '自定义操作', '行操作', '默认参数', '公共参数', '查询默认参数', '提交固定参数', '表单默认值', 'publicParams', 'publicQuery', '回调', '参数处理', '提交前', '搜索前', '加载列表前', 'beforeSubmit'])" class="config-section">
            <div class="config-section-title">
              工具栏和事件
            </div>
            <div class="config-form-grid">
              <label class="config-control">
                <span>新增按钮文案</span>
                <NInput
                  :value="tableZone?.props?.addButtonText || '新增'"
                  size="small"
                  @update:value="updateTableProp('addButtonText', $event || '新增')"
                />
              </label>
              <label class="config-control">
                <span>导出按钮文案</span>
                <NInput
                  :value="tableZone?.props?.exportButtonText || '导出'"
                  size="small"
                  @update:value="updateTableProp('exportButtonText', $event || '导出')"
                />
              </label>
              <label class="config-control">
                <span>导出文件名</span>
                <NInput
                  :value="tableZone?.props?.exportFileName || ''"
                  size="small"
                  placeholder="不填使用页面标题"
                  @update:value="updateTableProp('exportFileName', $event)"
                />
              </label>
            </div>
            <div class="event-editor compact">
              <div v-for="(eventItem, eventIdx) in tableEvents" :key="eventItem.id || eventIdx" class="event-row">
                <div class="event-row-head">
                  <span>{{ eventTriggerText(eventItem.trigger) }} · {{ eventActionText(eventItem.action) }}</span>
                  <NButton size="tiny" quaternary type="error" @click="removeTableEvent(eventIdx)">
                    删除
                  </NButton>
                </div>
                <div class="event-grid">
                  <NSelect
                    :value="eventItem.trigger"
                    :options="eventTriggerOptions"
                    size="tiny"
                    @update:value="updateTableEvent(eventIdx, { trigger: $event })"
                  />
                  <NSelect
                    :value="eventItem.action"
                    :options="eventActionOptions"
                    size="tiny"
                    @update:value="updateTableEvent(eventIdx, { action: $event })"
                  />
                  <NSelect
                    :value="eventItem.targetPageKey"
                    :options="pageTargetOptions"
                    size="tiny"
                    filterable
                    clearable
                    placeholder="目标页面"
                    @update:value="updateTableEvent(eventIdx, { targetPageKey: $event || '' })"
                  />
                  <NInput
                    :value="eventItem.description"
                    size="tiny"
                    placeholder="说明，方便以后维护"
                    @update:value="updateTableEvent(eventIdx, { description: $event })"
                  />
                </div>
              </div>
              <NButton size="tiny" dashed block @click="addTableEvent">
                新增事件
              </NButton>
            </div>
            <CrudDefaultParamsEditor
              class="default-params-block"
              :model-value="resolveTableDefaultParams()"
              :field-options="sortFieldOptions"
              @update:model-value="updateTableDefaultParams"
            />
            <CrudHookRulesEditor
              class="hook-rules-block"
              :model-value="tableZone?.props?.crudHookRules || {}"
              :legacy-before-submit-rules="tableZone?.props?.beforeSubmitRules || []"
              :field-options="sortFieldOptions"
              @update:model-value="updateTableHookRules"
            />
            <div class="custom-action-editor">
              <div class="custom-action-head">
                <strong>列表操作按钮</strong>
                <NButton size="tiny" type="primary" secondary @click="addTableCustomAction">
                  新增操作
                </NButton>
              </div>
              <div v-if="tableCustomActions.length" class="custom-action-list">
                <div v-for="(action, actionIdx) in tableCustomActions" :key="action.key || actionIdx" class="custom-action-row">
                  <NInput
                    :value="action.label"
                    size="tiny"
                    placeholder="按钮名称"
                    @update:value="updateTableCustomAction(actionIdx, { label: $event })"
                  />
                  <NSelect
                    :value="action.position || 'row'"
                    :options="customActionPositionOptions"
                    size="tiny"
                    @update:value="updateTableCustomAction(actionIdx, { position: $event || 'row' })"
                  />
                  <NSelect
                    :value="action.type || 'primary'"
                    :options="buttonTypeOptions"
                    size="tiny"
                    @update:value="updateTableCustomAction(actionIdx, { type: $event || 'primary' })"
                  />
                  <NSelect
                    :value="action.actionType || 'route'"
                    :options="customActionTypeOptions"
                    size="tiny"
                    @update:value="updateTableCustomAction(actionIdx, { actionType: $event || 'route' })"
                  />
                  <NSelect
                    v-if="action.actionType === 'page'"
                    :value="action.targetPageKey || ''"
                    :options="pageTargetOptions"
                    size="tiny"
                    filterable
                    clearable
                    placeholder="目标页面"
                    @update:value="updateTableCustomAction(actionIdx, { targetPageKey: $event || '' })"
                  />
                  <NInput
                    v-else
                    :value="action.routePath || ''"
                    size="tiny"
                    placeholder="路由 / 接口 / 脚本标识"
                    @update:value="updateTableCustomAction(actionIdx, { routePath: $event })"
                  />
                  <NButton size="tiny" quaternary type="error" @click="removeTableCustomAction(actionIdx)">
                    删除
                  </NButton>
                </div>
              </div>
              <div v-else class="config-empty-text">
                暂未配置自定义操作。行操作会显示在列表操作列，工具栏操作会显示在表格上方。
              </div>
            </div>
          </section>
          <section v-if="layoutType === 'tree-crud' && configSectionVisible(['树表模板', '树', '树标题', '父级字段', '显示字段', '加载方式'])" class="config-section">
            <div class="config-section-title">
              树表模板
            </div>
            <div class="config-form-grid">
              <label class="config-control">
                <span>树标题</span>
                <NInput
                  :value="treeConfig.treeTitle"
                  size="small"
                  placeholder="例如：组织架构"
                  @update:value="updateTreeConfig('treeTitle', $event)"
                />
              </label>
              <label class="config-control">
                <span>父级字段</span>
                <NSelect
                  :value="treeConfig.parentField"
                  :options="fieldOptions"
                  size="small"
                  filterable
                  placeholder="请选择父级字段"
                  @update:value="updateTreeConfig('parentField', $event)"
                />
              </label>
              <label class="config-control">
                <span>显示字段</span>
                <NSelect
                  :value="treeConfig.labelField"
                  :options="fieldOptions"
                  size="small"
                  filterable
                  placeholder="请选择树节点显示字段"
                  @update:value="updateTreeConfig('labelField', $event)"
                />
              </label>
              <label class="config-control">
                <span>加载方式</span>
                <NSelect
                  :value="treeConfig.loadMode || 'full'"
                  :options="treeLoadModeOptions"
                  size="small"
                  @update:value="updateTreeConfig('loadMode', $event)"
                />
              </label>
            </div>
            <div class="field-help-text">
              树节点点击后筛选右侧列表。这里配置的是模板数据关系，不是页面左侧菜单。
            </div>
          </section>
          <div ref="fieldEditorAnchor" class="field-config-anchor">
            <n-radio-group
              class="field-zone-tabs"
              :value="activeFieldZone"
              size="small"
              @update:value="openFieldPanel"
            >
              <n-radio-button value="search">
                查询 {{ searchFields.length }}
              </n-radio-button>
              <n-radio-button value="table">
                列表 {{ tableFields.length }}
              </n-radio-button>
            </n-radio-group>
            <div class="quick-config-help">
              快速配置只服务默认 CRUD 组件：新增/删除字段、排序、列宽会同步到自由画布里的 AiCrudPage。
            </div>
            <FieldOrderEditor
              v-if="activeFieldEditor"
              :title="activeFieldEditor.title"
              :empty-text="activeFieldEditor.emptyText"
              :fields="fields"
              :selected-refs="activeFieldEditor.selectedRefs"
              :filter="activeFieldEditor.filter"
              :mode="activeFieldEditor.mode"
              :settings="activeFieldEditor.settings"
              :page-options="pageTargetOptions"
              @update="updateZoneRefs(activeFieldEditor.zoneKey, $event)"
              @update-settings="updateZoneFieldSetting(activeFieldEditor.zoneKey, $event)"
            />
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { SearchOutline } from '@vicons/ionicons5'
import { NButton, NColorPicker, NIcon, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { computed, defineComponent, h, nextTick, ref } from 'vue'
import draggable from 'vuedraggable'
import CrudDefaultParamsEditor from './CrudDefaultParamsEditor.vue'
import CrudHookRulesEditor from './CrudHookRulesEditor.vue'
import GridBlockRenderer from './GridBlockRenderer.vue'
import { isPageFieldVisible } from './page-schema'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  layoutType: {
    type: String,
    default: 'simple-crud',
  },
  pages: {
    type: Array,
    default: () => [],
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:modelValue'])
const queryTypeOptions = [
  { label: '等于', value: 'eq' },
  { label: '包含', value: 'like' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]
const searchComponentOptions = [
  { label: '自动', value: '' },
  { label: '输入框', value: 'input' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '字典选择', value: 'dictSelect' },
  { label: '组织树', value: 'orgTreeSelect' },
  { label: '用户选择', value: 'userSelect' },
  { label: '区划树', value: 'regionTreeSelect' },
  { label: '树形选择', value: 'treeSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间', value: 'time' },
]
const tableRenderOptions = [
  { label: '自动（字典字段默认标签）', value: '' },
  { label: '字典标签', value: 'dictTag' },
  { label: '组织名称', value: 'orgName' },
  { label: '用户名称', value: 'userName' },
  { label: '区划名称', value: 'regionName' },
  { label: '文件名称', value: 'fileUpload' },
  { label: '图片预览', value: 'imageUpload' },
]
const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
const sortOrderOptions = [
  { label: '降序', value: 'desc' },
  { label: '升序', value: 'asc' },
]
const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const sizeOptions = [
  { label: '紧凑', value: 'small' },
  { label: '默认', value: 'medium' },
  { label: '宽松', value: 'large' },
]
const renderModeOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'card' },
]
const modalTypeOptions = [
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
]
const drawerPlacementOptions = [
  { label: '右侧', value: 'right' },
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
  { label: '底部', value: 'bottom' },
]
const labelPlacementOptions = [
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
]
const fixedOptions = [
  { label: '不固定', value: '' },
  { label: '固定左侧', value: 'left' },
  { label: '固定右侧', value: 'right' },
]
const columnClickActionOptions = [
  { label: '无动作', value: 'none' },
  { label: '跳转页面', value: 'navigate' },
]
const eventTriggerOptions = [
  { label: '行点击', value: 'rowClick' },
  { label: '加载完成', value: 'load' },
  { label: '新增成功', value: 'createSuccess' },
  { label: '编辑成功', value: 'updateSuccess' },
  { label: '删除成功', value: 'deleteSuccess' },
  { label: '导出完成', value: 'exportSuccess' },
]
const eventActionOptions = [
  { label: '无动作', value: 'none' },
  { label: '跳转页面', value: 'navigate' },
  { label: '刷新当前组件', value: 'refreshBlock' },
  { label: '打开弹窗', value: 'openModal' },
  { label: '打开抽屉', value: 'openDrawer' },
  { label: '接口请求', value: 'request' },
  { label: '自定义脚本', value: 'customScript' },
]
const customActionPositionOptions = [
  { label: '行操作列', value: 'row' },
  { label: '工具栏', value: 'toolbar' },
]
const customActionTypeOptions = [
  { label: '打开路由', value: 'route' },
  { label: '跳转页面', value: 'page' },
  { label: '打开弹窗', value: 'modal' },
  { label: '接口请求', value: 'request' },
  { label: '自定义脚本', value: 'script' },
]
const buttonTypeOptions = [
  { label: '主要', value: 'primary' },
  { label: '默认', value: 'default' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]

const FieldOrderEditor = defineComponent({
  name: 'FieldOrderEditor',
  props: {
    title: {
      type: String,
      required: true,
    },
    emptyText: {
      type: String,
      required: true,
    },
    fields: {
      type: Array,
      default: () => [],
    },
    selectedRefs: {
      type: Array,
      default: () => [],
    },
    filter: {
      type: Function,
      required: true,
    },
    mode: {
      type: String,
      default: '',
    },
    settings: {
      type: Object,
      default: () => ({}),
    },
    pageOptions: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update', 'updateSettings'],
  setup(editorProps, { emit }) {
    const fieldMap = computed(() => new Map(editorProps.fields.map(field => [field.field, field])))
    const selectedRows = computed(() => editorProps.selectedRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean))
    const availableRows = computed(() => editorProps.fields.filter((field) => {
      return editorProps.filter(field) && !editorProps.selectedRefs.includes(field.field)
    }))
    const queryFieldOptions = computed(() => {
      const options = editorProps.fields
        .filter(field => !field.systemField || field.field === 'id')
        .map(field => ({
          label: field.label ? `${field.label}（${field.sourceField || field.field}）` : (field.sourceField || field.field),
          value: field.field,
        }))
      if (!options.some(item => item.value === 'id'))
        options.unshift({ label: 'ID（id）', value: 'id' })
      return options
    })
    const renderTargetFieldOptions = (field = {}) => {
      const options = queryFieldOptions.value.map(item => ({ ...item }))
      const defaultTarget = `${field.field}Name`
      if (!options.some(item => item.value === defaultTarget)) {
        options.unshift({
          label: `${defaultTarget}（默认翻译字段）`,
          value: defaultTarget,
        })
      }
      return options
    }
    const updateRows = rows => emit('update', rows.map(row => row.field))
    const remove = field => emit('update', editorProps.selectedRefs.filter(ref => ref !== field))
    const add = field => emit('update', [...editorProps.selectedRefs, field])
    const updateSetting = (field, patch) => {
      emit('updateSettings', {
        field,
        settings: {
          ...(editorProps.settings?.[field] || {}),
          ...patch,
        },
      })
    }
    const fieldControl = (label, control, desc = '') => h('label', { class: 'field-setting-control' }, [
      h('span', { class: 'field-setting-label' }, label),
      control,
      desc ? h('small', null, desc) : null,
    ])
    return () => h('div', { class: 'field-editor' }, [
      h('div', { class: 'field-editor-title' }, editorProps.title),
      selectedRows.value.length
        ? h(draggable, {
            'modelValue': selectedRows.value,
            'itemKey': 'field',
            'handle': '.field-handle',
            'animation': 160,
            'class': 'selected-field-list',
            'onUpdate:modelValue': updateRows,
          }, {
            item: ({ element }) => {
              const setting = editorProps.settings?.[element.field] || {}
              const tableRenderType = setting.renderType || resolveDefaultTableRenderType(element)
              return h('div', { class: ['selected-field-row', editorProps.mode ? `mode-${editorProps.mode}` : ''] }, [
                h('span', { class: 'field-handle' }, '☰'),
                h('span', { class: 'field-name' }, [
                  h('span', null, element.label || element.field),
                  element.sourceLabel || element.modelName
                    ? h('small', null, element.sourceLabel || element.modelName)
                    : null,
                ]),
                h('span', { class: 'field-code' }, element.field),
                h('button', { type: 'button', onClick: () => remove(element.field) }, '移除'),
                editorProps.mode === 'search'
                  ? h('div', { class: 'field-setting-row search-setting-row' }, [
                      fieldControl('查询方式', h(NSelect, {
                        value: setting.queryType || element.queryType || 'like',
                        options: queryTypeOptions,
                        size: 'tiny',
                        placeholder: '查询方式',
                        onUpdateValue: value => updateSetting(element.field, { queryType: value }),
                      })),
                      fieldControl('查询组件', h(NSelect, {
                        value: setting.componentType || resolveDefaultSearchComponentType(element),
                        options: searchComponentOptions,
                        size: 'tiny',
                        placeholder: '查询组件',
                        onUpdateValue: value => updateSetting(element.field, { componentType: value }),
                      })),
                      fieldControl('映射字段', h(NSelect, {
                        value: setting.queryField || element.field,
                        options: queryFieldOptions.value,
                        size: 'tiny',
                        filterable: true,
                        placeholder: '映射字段',
                        onUpdateValue: value => updateSetting(element.field, { queryField: value }),
                      }), '接口查询参数使用哪个字段'),
                      fieldControl('对齐', h(NSelect, {
                        value: setting.align || 'left',
                        options: alignOptions,
                        size: 'tiny',
                        placeholder: '对齐',
                        onUpdateValue: value => updateSetting(element.field, { align: value || 'left' }),
                      })),
                    ])
                  : null,
                editorProps.mode === 'table'
                  ? h('div', { class: 'field-setting-row table-setting-row' }, [
                      h('span', { class: 'field-inline-switch' }, [
                        h('span', null, '排序'),
                        h(NSwitch, {
                          value: Boolean(setting.sortable ?? element.sortable),
                          size: 'small',
                          onUpdateValue: value => updateSetting(element.field, { sortable: value }),
                        }),
                      ]),
                      fieldControl('渲染方式', h(NSelect, {
                        value: tableRenderType,
                        options: tableRenderOptions,
                        size: 'tiny',
                        placeholder: '渲染方式',
                        onUpdateValue: value => updateSetting(element.field, { renderType: value }),
                      })),
                      fieldControl('对齐', h(NSelect, {
                        value: setting.align || 'left',
                        options: alignOptions,
                        size: 'tiny',
                        placeholder: '对齐',
                        onUpdateValue: value => updateSetting(element.field, { align: value || 'left' }),
                      })),
                      fieldControl('列宽', h(NInputNumber, {
                        value: setting.width || element.width || 160,
                        min: 80,
                        max: 800,
                        size: 'tiny',
                        showButton: false,
                        placeholder: '列宽',
                        onUpdateValue: value => updateSetting(element.field, { width: value || null }),
                      }), '单位 px'),
                      fieldControl('固定列', h(NSelect, {
                        value: setting.fixed || '',
                        options: fixedOptions,
                        size: 'tiny',
                        placeholder: '固定列',
                        onUpdateValue: value => updateSetting(element.field, { fixed: value || null }),
                      })),
                      fieldControl('文字颜色', h(NColorPicker, {
                        value: setting.textColor || '',
                        size: 'small',
                        showAlpha: true,
                        placeholder: '文字颜色',
                        onUpdateValue: value => updateSetting(element.field, { textColor: value || '' }),
                      })),
                      fieldControl('点击动作', h(NSelect, {
                        value: setting.clickAction || 'none',
                        options: columnClickActionOptions,
                        size: 'tiny',
                        placeholder: '点击动作',
                        onUpdateValue: value => updateSetting(element.field, { clickAction: value || 'none' }),
                      })),
                      setting.clickAction === 'navigate'
                        ? fieldControl('目标页面', h(NSelect, {
                            value: setting.targetPageKey || '',
                            options: editorProps.pageOptions,
                            size: 'tiny',
                            filterable: true,
                            placeholder: '选择详情页或自定义页',
                            onUpdateValue: value => updateSetting(element.field, { targetPageKey: value || '' }),
                          }))
                        : null,
                      setting.clickAction === 'navigate'
                        ? fieldControl('参数名', h(NInput, {
                            value: setting.targetParamName || 'id',
                            size: 'tiny',
                            placeholder: '目标页面参数名',
                            onUpdateValue: value => updateSetting(element.field, { targetParamName: normalizeParamName(value) || 'id' }),
                          }), '目标页面接收的参数名，默认 id')
                        : null,
                      setting.clickAction === 'navigate'
                        ? fieldControl('取值字段', h(NSelect, {
                            value: setting.targetParamField || 'id',
                            options: queryFieldOptions.value,
                            size: 'tiny',
                            filterable: true,
                            placeholder: '从当前行取哪个字段',
                            onUpdateValue: value => updateSetting(element.field, { targetParamField: value || 'id' }),
                          }), '默认从当前行 id 取值')
                        : null,
                      isNameRenderType(tableRenderType)
                        ? fieldControl('名称字段', h(NSelect, {
                            value: setting.targetField || `${element.field}Name`,
                            options: renderTargetFieldOptions(element),
                            size: 'tiny',
                            filterable: true,
                            tag: true,
                            placeholder: '名称字段',
                            onUpdateValue: value => updateSetting(element.field, { targetField: value }),
                          }), '用哪个字段显示名称')
                        : null,
                    ])
                  : null,
              ])
            },
          })
        : h('div', { class: 'field-empty' }, editorProps.emptyText),
      availableRows.value.length
        ? h('div', { class: 'available-field-list' }, availableRows.value.map(field => h('button', {
            key: field.field,
            type: 'button',
            onClick: () => add(field.field),
          }, [
            h('span', null, field.label || field.field),
            field.sourceLabel || field.modelName ? h('small', null, field.sourceLabel || field.modelName) : null,
          ])))
        : null,
    ])
  },
})

const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
const searchZone = computed(() => findZone('search'))
const tableZone = computed(() => findZone('table'))
const activeFieldZone = ref('search')
const configExpanded = ref(false)
const configCollapsed = ref(false)
const fieldEditorAnchor = ref(null)
const fieldOptions = computed(() => props.fields.map(field => ({
  label: field.label ? `${field.label}（${field.field}）` : field.field,
  value: field.field,
})))
const pageTargetOptions = computed(() => props.pages.map(page => ({
  label: `${page.pageName || page.pageKey}（${page.pageType || 'custom'}）`,
  value: page.pageKey,
})))
const sortFieldOptions = computed(() => {
  const options = fieldOptions.value.map(item => ({ ...item }))
  if (!options.some(item => item.value === 'id')) {
    options.unshift({ label: 'ID（id）', value: 'id' })
  }
  return options
})
const searchFields = computed(() => resolveFields(searchZone.value, field => field.searchable))
const tableFields = computed(() => resolveFields(tableZone.value, field => field.listVisible !== false))
const treeConfig = computed(() => tableZone.value?.props?.treeConfig || {})
const tableEvents = computed(() => tableZone.value?.props?.events || [])
const tableCustomActions = computed(() => tableZone.value?.props?.customActions || [])
const configKeyword = ref('')
const defaultSortText = computed(() => {
  const field = sortFieldOptions.value.find(item => item.value === (tableZone.value?.props?.defaultSortField || 'id'))
  const order = sortOrderOptions.find(item => item.value === (tableZone.value?.props?.defaultSortOrder || 'desc'))
  return `${field?.label || 'ID（id）'} ${order?.label || '降序'}`
})
const defaultApiValues = computed(() => {
  const key = props.modelValue.configKey
    || props.modelSchema?.configKey
    || props.modelSchema?.object?.configKey
    || props.modelSchema?.object?.code
    || props.modelSchema?.objectCode
    || props.modelSchema?.modelCode
    || ''
  const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
  return {
    api: prefix,
    listApi: `get@${prefix}/page`,
    detailApi: `get@${prefix}/:id`,
    createApi: `post@${prefix}`,
    updateApi: `put@${prefix}`,
    deleteApi: `delete@${prefix}/:id`,
  }
})
const crudPreviewBlock = computed(() => {
  const fieldRefs = uniqueRefs([
    ...(searchZone.value?.fieldRefs || []),
    ...(tableZone.value?.fieldRefs || []),
  ])
  const tableProps = tableZone.value?.props || {}
  const searchProps = searchZone.value?.props || {}
  return {
    id: 'structured_crud_preview',
    blockType: 'AiCrudPage',
    label: tableProps.title || '业务列表',
    fieldRefs: fieldRefs.length ? fieldRefs : tableFields.value.map(field => field.field),
    props: {
      title: tableProps.title || '业务列表',
      rowKey: tableProps.rowKey || 'id',
      api: tableProps.api || defaultApiValues.value.api,
      listApi: tableProps.listApi || defaultApiValues.value.listApi,
      detailApi: tableProps.detailApi || defaultApiValues.value.detailApi,
      createApi: tableProps.createApi || defaultApiValues.value.createApi,
      updateApi: tableProps.updateApi || defaultApiValues.value.updateApi,
      deleteApi: tableProps.deleteApi || defaultApiValues.value.deleteApi,
      importApi: tableProps.importApi || '',
      exportApi: tableProps.exportApi || '',
      listDataField: tableProps.listDataField || 'records',
      listTotalField: tableProps.listTotalField || 'total',
      showSearch: searchZone.value?.enabled !== false,
      showPagination: tableProps.showPagination !== false,
      showImport: tableProps.showImport === true,
      showExport: tableProps.showExport === true,
      enableCustomQuery: tableProps.enableCustomQuery === true,
      hideBatchDelete: tableProps.hideBatchDelete === true,
      hideSelection: tableProps.hideSelection === true,
      showRenderModeSwitch: tableProps.showRenderModeSwitch !== false,
      tableSize: tableProps.tableSize || 'small',
      renderMode: tableProps.renderMode || 'table',
      bordered: tableProps.bordered === true,
      striped: tableProps.striped === true,
      maxHeight: tableProps.maxHeight,
      scrollX: tableProps.scrollX,
      searchGridCols: tableProps.searchGridCols || 4,
      searchLabelWidth: tableProps.searchLabelWidth || 'auto',
      searchMaxVisibleFields: tableProps.searchMaxVisibleFields || 3,
      searchEnableCollapse: tableProps.searchEnableCollapse !== false,
      modalType: tableProps.modalType || 'modal',
      drawerPlacement: tableProps.drawerPlacement || 'right',
      modalWidth: tableProps.modalWidth || '800px',
      detailModalWidth: tableProps.detailModalWidth || 'min(1080px, 92vw)',
      editGridCols: tableProps.editGridCols || 1,
      editLabelPlacement: tableProps.editLabelPlacement || 'left',
      editLabelWidth: tableProps.editLabelWidth || 'auto',
      editSize: tableProps.editSize || 'medium',
      addButtonText: tableProps.addButtonText || '新增',
      exportButtonText: tableProps.exportButtonText || '导出',
      exportFileName: tableProps.exportFileName || '',
      events: tableProps.events || [],
      customActions: tableProps.customActions || [],
      crudHookRules: tableProps.crudHookRules || {},
      beforeSubmitRules: tableProps.beforeSubmitRules || [],
      publicParams: tableProps.publicParams || {},
      publicQuery: tableProps.publicQuery || {},
      formDefaultValues: tableProps.formDefaultValues || {},
      submitDefaultParams: tableProps.submitDefaultParams || {},
      previewLiveData: tableProps.previewLiveData === true,
      defaultSortField: tableProps.defaultSortField || 'id',
      defaultSortOrder: tableProps.defaultSortOrder || 'desc',
      fieldSettings: {
        ...(searchProps.fieldSettings || {}),
        ...(tableProps.fieldSettings || {}),
      },
      style: {
        widthMode: 'full',
        width: '100%',
        height: '100%',
      },
    },
  }
})
const treePreviewBlock = computed(() => ({
  id: 'structured_tree_preview',
  blockType: 'tree-panel',
  label: treeConfig.value.treeTitle || '筛选树',
  fieldRefs: [],
  props: {
    ...treeConfig.value,
    enabled: treeConfig.value.enabled !== false,
    treeTitle: treeConfig.value.treeTitle || '筛选树',
    sourceModelName: treeConfig.value.sourceModelName || '树形筛选',
    keyField: treeConfig.value.keyField || 'id',
    parentField: treeConfig.value.parentField || 'parentId',
    labelField: treeConfig.value.labelField || firstUsableField(),
    filterField: treeConfig.value.filterField || treeConfig.value.parentField || 'parentId',
    targetField: treeConfig.value.targetField || 'id',
    childrenField: treeConfig.value.childrenField || 'children',
    loadMode: treeConfig.value.loadMode || 'full',
    style: {
      widthMode: 'full',
      width: '100%',
      height: '100%',
    },
  },
}))
const activeFieldEditor = computed(() => {
  if (activeFieldZone.value === 'table') {
    return {
      zoneKey: 'table',
      title: '列表字段',
      emptyText: '当前没有列表字段',
      mode: 'table',
      selectedRefs: tableZone.value?.fieldRefs || [],
      settings: tableZone.value?.props?.fieldSettings || {},
      filter: field => isPageFieldVisible(field, 'table'),
    }
  }
  return {
    zoneKey: 'search',
    title: '查询字段',
    emptyText: '当前没有查询字段',
    mode: 'search',
    selectedRefs: searchZone.value?.fieldRefs || [],
    settings: searchZone.value?.props?.fieldSettings || {},
    filter: field => isPageFieldVisible(field, 'search'),
  }
})
function findZone(zoneKey) {
  return props.modelValue.zones?.find(zone => zone.zoneKey === zoneKey) || null
}

function resolveFields(zone, fallback) {
  if (!zone || zone.enabled === false)
    return []
  const refs = zone.fieldRefs?.length
    ? zone.fieldRefs
    : props.fields.filter(fallback).map(field => field.field)
  return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
}

function firstUsableField() {
  return props.fields.find(field => !field.systemField)?.field || props.fields[0]?.field || 'name'
}

function uniqueRefs(refs = []) {
  return Array.from(new Set(refs.filter(Boolean)))
}

function resolveDefaultSearchComponentType(field = {}) {
  const componentType = field.componentType || field.dataType || 'input'
  if (field.dictType)
    return 'dictSelect'
  if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
    return 'number'
  return componentType === 'inputNumber' ? 'number' : componentType
}

function resolveDefaultTableRenderType(field = {}) {
  const componentType = field.componentType || ''
  if (field.dictType)
    return 'dictTag'
  if (componentType === 'orgTreeSelect')
    return 'orgName'
  if (componentType === 'userSelect')
    return 'userName'
  if (componentType === 'regionTreeSelect')
    return 'regionName'
  if (componentType === 'fileUpload' || componentType === 'imageUpload')
    return componentType
  return ''
}

function isNameRenderType(renderType) {
  return ['orgName', 'userName', 'regionName', 'fileUpload', 'imageUpload'].includes(renderType)
}

function updateZoneRefs(zoneKey, refs) {
  patchZone(zoneKey, { fieldRefs: refs }, createGridPatchForZone(zoneKey, { fieldRefs: refs }))
}

function updateTableProp(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      [key]: value,
    },
  }, createGridPatchForZone('table', {
    props: {
      [key]: value,
    },
  }))
}

function applyTableGlobalAlign(value) {
  const align = ['left', 'center', 'right'].includes(value) ? value : 'left'
  const zone = tableZone.value
  if (!zone)
    return
  const nextSettings = { ...(zone.props?.fieldSettings || {}) }
  ;(zone.fieldRefs || []).forEach((fieldName) => {
    nextSettings[fieldName] = {
      ...(nextSettings[fieldName] || {}),
      align,
    }
  })
  patchZone('table', {
    props: {
      ...(zone.props || {}),
      globalAlign: align,
      fieldSettings: nextSettings,
    },
  }, createGridPatchForZone('table', {
    props: {
      globalAlign: align,
      fieldSettings: nextSettings,
    },
  }))
}

function updateTableRowGap(value) {
  updateTableProp('rowGap', Math.max(0, Math.min(32, Number(value ?? 8))))
}

function updateTreeConfig(key, value) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      treeConfig: {
        ...(treeConfig.value || {}),
        enabled: true,
        [key]: value,
      },
    },
  })
}

function addTableEvent() {
  updateTableProp('events', [
    ...tableEvents.value,
    {
      id: `evt_${Date.now()}`,
      trigger: 'rowClick',
      action: 'none',
      targetPageKey: '',
      targetBlockId: '',
      description: '',
      params: [],
    },
  ])
}

function updateTableEvent(eventIdx, patch) {
  const list = [...tableEvents.value]
  list[eventIdx] = { ...(list[eventIdx] || {}), ...patch }
  updateTableProp('events', list)
}

function removeTableEvent(eventIdx) {
  const list = [...tableEvents.value]
  list.splice(eventIdx, 1)
  updateTableProp('events', list)
}

function addTableCustomAction() {
  updateTableProp('customActions', [
    ...tableCustomActions.value,
    {
      key: `custom_${Date.now()}`,
      label: '操作按钮',
      position: 'row',
      type: 'primary',
      actionType: 'route',
      routePath: '',
      targetPageKey: '',
      params: [],
    },
  ])
}

function updateTableCustomAction(actionIdx, patch) {
  const list = [...tableCustomActions.value]
  list[actionIdx] = { ...(list[actionIdx] || {}), ...patch }
  updateTableProp('customActions', list)
}

function removeTableCustomAction(actionIdx) {
  const list = [...tableCustomActions.value]
  list.splice(actionIdx, 1)
  updateTableProp('customActions', list)
}

function configSectionVisible(keywords = []) {
  const keyword = String(configKeyword.value || '').trim().toLowerCase()
  if (!keyword)
    return true
  return keywords.some(item => String(item || '').toLowerCase().includes(keyword))
}

function updateTableHookRules(rules) {
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      crudHookRules: rules || {},
      beforeSubmitRules: [],
    },
  }, createGridPatchForZone('table', {
    props: {
      crudHookRules: rules || {},
      beforeSubmitRules: [],
    },
  }))
}

function resolveTableDefaultParams() {
  const props = tableZone.value?.props || {}
  return {
    publicParams: props.publicParams || {},
    publicQuery: props.publicQuery || {},
    formDefaultValues: props.formDefaultValues || {},
    submitDefaultParams: props.submitDefaultParams || {},
  }
}

function updateTableDefaultParams(params = {}) {
  if (isSameDefaultParams(resolveTableDefaultParams(), params))
    return
  patchZone('table', {
    props: {
      ...(tableZone.value?.props || {}),
      publicParams: params.publicParams || {},
      publicQuery: params.publicQuery || {},
      formDefaultValues: params.formDefaultValues || {},
      submitDefaultParams: params.submitDefaultParams || {},
    },
  }, createGridPatchForZone('table', {
    props: {
      publicParams: params.publicParams || {},
      publicQuery: params.publicQuery || {},
      formDefaultValues: params.formDefaultValues || {},
      submitDefaultParams: params.submitDefaultParams || {},
    },
  }))
}

function isSameDefaultParams(left = {}, right = {}) {
  return JSON.stringify(normalizeDefaultParams(left)) === JSON.stringify(normalizeDefaultParams(right))
}

function normalizeDefaultParams(source = {}) {
  return ['publicParams', 'publicQuery', 'formDefaultValues', 'submitDefaultParams'].reduce((result, key) => {
    const params = source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])
      ? source[key]
      : {}
    result[key] = Object.keys(params).sort().reduce((next, paramKey) => {
      next[paramKey] = params[paramKey]
      return next
    }, {})
    return result
  }, {})
}

function eventTriggerText(trigger) {
  return eventTriggerOptions.find(item => item.value === trigger)?.label || trigger || '触发'
}

function eventActionText(action) {
  return eventActionOptions.find(item => item.value === action)?.label || action || '动作'
}

function updateZoneFieldSetting(zoneKey, payload) {
  const zone = findZone(zoneKey)
  if (!zone || !payload?.field)
    return
  patchZone(zoneKey, {
    props: {
      ...(zone.props || {}),
      fieldSettings: {
        ...(zone.props?.fieldSettings || {}),
        [payload.field]: payload.settings,
      },
    },
  }, createGridPatchForZone(zoneKey, {
    props: {
      fieldSettings: {
        ...(zone.props?.fieldSettings || {}),
        [payload.field]: payload.settings,
      },
    },
  }))
}

function openFieldPanel(zoneKey) {
  activeFieldZone.value = zoneKey
  configCollapsed.value = false
  nextTick(() => {
    fieldEditorAnchor.value?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
  })
}

function collapseConfigPanel() {
  configExpanded.value = false
  configCollapsed.value = true
}

function normalizeParamName(value) {
  return String(value || '').trim().replace(/[^\w.$-]/g, '')
}

function patchZone(zoneKey, patch, gridPatch = null) {
  const zones = (props.modelValue.zones || []).map((zone) => {
    if (zone.zoneKey !== zoneKey)
      return zone
    return {
      ...zone,
      ...patch,
      props: patch.props ? { ...(zone.props || {}), ...patch.props } : zone.props || {},
    }
  })
  const nextValue = { ...props.modelValue, zones }
  if (gridPatch) {
    nextValue.listGridLayout = patchCrudGridLayout(props.modelValue.listGridLayout, gridPatch)
    nextValue.pages = patchPagesGridLayout(props.modelValue.pages || [], 'list', nextValue.listGridLayout)
  }
  emit('update:modelValue', nextValue)
}

function patchPagesGridLayout(pages = [], pageKey = 'list', gridLayout) {
  if (!Array.isArray(pages) || !pages.length)
    return pages
  return pages.map(page => page.pageKey === pageKey
    ? { ...page, gridLayout }
    : page)
}

function createGridPatchForZone(zoneKey, patch) {
  if (zoneKey === 'search') {
    return {
      blockTypes: ['AiCrudPage', 'search-form'],
      fieldRefs: patch.fieldRefs,
      props: patch.props,
    }
  }
  if (zoneKey === 'table') {
    return {
      blockTypes: ['AiCrudPage', 'data-table', 'AiTable'],
      fieldRefs: patch.fieldRefs,
      props: patch.props,
    }
  }
  return null
}

function patchCrudGridLayout(layout = {}, gridPatch = {}) {
  if (!layout?.items?.length)
    return layout
  const blockTypes = new Set(gridPatch.blockTypes || [])
  return {
    ...layout,
    items: (layout.items || []).map((item) => {
      if (!blockTypes.has(item.blockType))
        return item
      return {
        ...item,
        fieldRefs: Array.isArray(gridPatch.fieldRefs) ? gridPatch.fieldRefs : item.fieldRefs,
        props: gridPatch.props
          ? {
              ...(item.props || {}),
              ...gridPatch.props,
              fieldSettings: gridPatch.props.fieldSettings
                ? {
                    ...(item.props?.fieldSettings || {}),
                    ...gridPatch.props.fieldSettings,
                  }
                : item.props?.fieldSettings,
            }
          : item.props,
      }
    }),
  }
}

function resetSearchFields() {
  updateZoneRefs('search', props.fields.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
}

function resetTableFields() {
  updateZoneRefs('table', props.fields.filter(field => isPageFieldVisible(field, 'table')).map(field => field.field))
}
</script>

<style scoped>
.structured-designer {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  container-type: inline-size;
}

.structured-workspace {
  display: flex;
  align-items: stretch;
  gap: 12px;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.structured-preview-column {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  gap: 12px;
  padding-right: 2px;
  scrollbar-gutter: stable;
}

.structured-workspace.config-expanded .structured-config-panel {
  flex-basis: 100%;
  width: 100%;
}

.structured-workspace.config-collapsed .structured-preview-column {
  padding-right: 0;
}

.config-collapsed-rail {
  display: grid;
  place-items: center;
  flex: 0 0 40px;
  width: 40px;
  min-width: 40px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease;
}

.config-collapsed-rail:hover {
  border-color: #60a5fa;
  background: #dbeafe;
  color: #1e40af;
}

.config-collapsed-rail span {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
}

.surface-section {
  padding: 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
}

.crud-preview-section {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.structured-config-panel {
  position: relative;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  flex: 0 0 420px;
  width: 420px;
  align-self: stretch;
  min-height: 0;
  min-width: 0;
  border: 1px solid #dbe3ee;
  border-left: 3px solid #2563eb;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.config-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
}

.config-panel-search,
.designer-panel-search {
  position: sticky;
  top: 0;
  z-index: 3;
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  background: #fff;
  box-shadow: 0 1px 0 rgba(226, 232, 240, 0.75);
}

.config-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.config-desc {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.config-step-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.config-scroll-body {
  min-height: 0;
  overflow: auto;
  padding: 10px 12px 12px;
  scrollbar-gutter: stable;
}

.config-step {
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

.config-step.active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.config-section {
  display: grid;
  gap: 10px;
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff 0%, #f8fbff 100%);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.config-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
}

.config-section-title::before {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: #2563eb;
}

.config-section-tip {
  padding: 7px 9px;
  border: 1px dashed #bfdbfe;
  border-radius: 7px;
  background: #eff6ff;
  color: #475569;
  font-size: 11px;
  line-height: 17px;
}

.config-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.config-form-grid.one-col {
  grid-template-columns: minmax(0, 1fr);
}

.config-form-grid.nested {
  grid-template-columns: minmax(0, 1fr);
}

.config-control {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.config-control > span {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  line-height: 15px;
}

.config-control :deep(.n-select),
.config-control :deep(.n-input),
.config-control :deep(.n-input-number) {
  width: 100%;
  min-width: 0;
}

.config-switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.config-switch-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.config-switch-grid label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 12px;
}

.event-editor.compact {
  display: grid;
  gap: 8px;
}

.event-row {
  display: grid;
  gap: 8px;
  padding: 9px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.event-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.event-row-head span {
  min-width: 0;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.event-grid :deep(.n-select),
.event-grid :deep(.n-input) {
  width: 100%;
  min-width: 0;
}

.field-config-entry {
  background: #f8fbff;
}

.field-config-entry-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.custom-action-editor {
  display: grid;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px dashed #dbe3ee;
}

.custom-action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.custom-action-head strong {
  color: #0f172a;
  font-size: 12px;
}

.custom-action-list {
  display: grid;
  gap: 8px;
}

.custom-action-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.custom-action-row :deep(.n-select),
.custom-action-row :deep(.n-input) {
  width: 100%;
  min-width: 0;
}

.config-empty-text {
  padding: 8px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.field-zone-tabs {
  margin-top: 10px;
}

.quick-config-help {
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px dashed #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
}

.section-head,
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-head {
  margin-bottom: 10px;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.section-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.table-toolbar {
  margin-bottom: 10px;
}

.crud-component-preview-frame {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 420px;
  max-height: 100%;
  margin-bottom: 12px;
  overflow: auto;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.crud-component-preview-frame.tree-crud-preview {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  overflow: auto;
}

.crud-component-preview-frame :deep(.grid-block) {
  min-width: 0;
  min-height: 400px;
}

.crud-component-preview-frame.tree-crud-preview :deep(.block-AiCrudPage) {
  min-width: 0;
  overflow: hidden;
}

.crud-component-preview-frame.tree-crud-preview :deep(.ai-crud-preview),
.crud-component-preview-frame.tree-crud-preview :deep(.system-component-preview),
.crud-component-preview-frame.tree-crud-preview :deep(.ai-crud-page),
.crud-component-preview-frame.tree-crud-preview :deep(.ai-crud-main),
.crud-component-preview-frame.tree-crud-preview :deep(.ai-table-wrapper) {
  min-width: 0;
  max-width: 100%;
}

.crud-component-preview-frame.tree-crud-preview :deep(.ai-crud-preview) {
  overflow: auto;
}

.crud-preview-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.crud-preview-status span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.template-hint,
.field-help-text {
  margin-top: 10px;
  padding: 8px 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

:deep(.field-editor) {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

.field-config-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.field-config-summary.active {
  border-color: #93c5fd;
  background: #eff6ff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
}

.summary-main {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.summary-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-title strong {
  color: #0f172a;
  font-size: 13px;
}

.summary-title span,
.summary-empty {
  color: #64748b;
  font-size: 12px;
}

.summary-chip-list {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-chip {
  max-width: 128px;
  min-height: 24px;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-chip.muted {
  color: #64748b;
}

:deep(.field-editor-title) {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

:deep(.selected-field-list),
:deep(.available-field-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.selected-field-row) {
  display: grid;
  grid-template-columns: 16px minmax(120px, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  font-size: 12px;
}

:deep(.selected-field-row.mode-search) {
  grid-template-columns: 16px minmax(120px, 1fr) auto;
}

:deep(.selected-field-row.mode-table) {
  grid-template-columns: 16px minmax(120px, 1fr) auto;
}

:deep(.field-setting-row) {
  display: grid;
  grid-column: 2 / -1;
  gap: 6px;
}

:deep(.search-setting-row) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

:deep(.table-setting-row) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

:deep(.field-inline-switch) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
}

:deep(.field-handle) {
  color: #94a3b8;
  cursor: grab;
}

:deep(.field-name) {
  display: grid;
  gap: 2px;
  color: #0f172a;
  font-weight: 600;
}

:deep(.field-name small),
:deep(.available-field-list small) {
  color: #64748b;
  font-size: 11px;
  font-weight: 400;
}

:deep(.field-code) {
  grid-column: 2 / -1;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

:deep(.field-setting-row .n-select),
:deep(.field-setting-row .n-input),
:deep(.field-setting-row .n-input-number),
:deep(.field-setting-row .n-color-picker) {
  width: 100%;
  min-width: 0;
}

:deep(.field-setting-control) {
  display: grid;
  gap: 4px;
  min-width: 0;
}

:deep(.field-setting-label) {
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
}

:deep(.field-setting-control small) {
  color: #94a3b8;
  font-size: 11px;
  line-height: 15px;
}

:deep(.selected-field-row button),
:deep(.available-field-list button) {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

:deep(.field-empty) {
  color: #94a3b8;
  font-size: 12px;
}

:deep(.available-field-list) {
  margin-top: 10px;
}

:deep(.available-field-list button) {
  display: inline-grid;
  gap: 2px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
}

.structured-config-panel :deep(.field-editor) {
  min-height: 0;
  margin-top: 0;
  overflow: visible;
  padding: 12px 0 0;
  border-top: 0;
}

.structured-config-panel :deep(.field-editor-title) {
  display: none;
}

.structured-config-panel :deep(.selected-field-list),
.structured-config-panel :deep(.available-field-list) {
  display: grid;
  flex-wrap: nowrap;
}

.structured-config-panel :deep(.selected-field-list) {
  max-height: none;
  overflow: auto;
  padding-right: 4px;
}

.structured-config-panel :deep(.available-field-list) {
  max-height: 210px;
  overflow: auto;
  padding-right: 4px;
}

.structured-config-panel :deep(.selected-field-row),
.structured-config-panel :deep(.selected-field-row.mode-search),
.structured-config-panel :deep(.selected-field-row.mode-table) {
  grid-template-columns: 18px minmax(0, 1fr) 44px;
  align-items: start;
  min-height: 40px;
  padding: 8px;
  border-color: #dbe3ee;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.structured-config-panel :deep(.field-code) {
  display: none;
}

.structured-config-panel :deep(.field-setting-row) {
  grid-column: 1 / -1;
  margin-left: 0;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.structured-config-panel :deep(.search-setting-row) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.structured-config-panel :deep(.table-setting-row) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.structured-config-panel :deep(.field-inline-switch) {
  min-height: 28px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
}

@container (max-width: 980px) {
  .structured-workspace {
    align-items: stretch;
    flex-direction: column;
  }

  .structured-config-panel {
    flex-basis: auto;
    width: 100%;
    height: auto;
    max-height: none;
    min-height: 520px;
  }

  .section-head,
  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@container (max-width: 620px) {
  .field-config-summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

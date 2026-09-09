<template>
  <section class="panel-item sub-table-panel">
    <!-- 使用说明 -->
    <div class="subtable-guide">
      <div class="subtable-guide-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" fill="currentColor"/></svg>
        主子表使用说明
      </div>
      <p class="subtable-guide-desc">
        主子表让一张表单同时录入主表与关联子表数据，典型场景：采购订单 + 明细行、报销单 + 费用行。
      </p>
      <div class="subtable-guide-steps">
        <div class="subtable-guide-step">
          <span class="step-num">1</span>
          <span>先在「关系与级联」中用 ER 图建立对象间的关联关系</span>
        </div>
        <div class="subtable-guide-step">
          <span class="step-num">2</span>
          <span>回到表单设计，点击下方添加子表，从关系下拉选择或手填标识</span>
        </div>
        <div class="subtable-guide-step">
          <span class="step-num">3</span>
          <span>子表会跟随主表一起展示和保存，运行时自动加载关联数据</span>
        </div>
      </div>
    </div>

    <!-- 无可用关系时的提示 -->
    <div v-if="!relationOptions.length" class="subtable-no-relations-hint">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z" fill="currentColor"/></svg>
      当前对象尚未配置关联关系。如需从下拉选择子表，请先在
      <strong>业务对象设计 → 关系与级联</strong> 中通过 ER 图创建关系。
      也可以直接手填子表标识。
    </div>

    <div v-if="subTables.length" class="sub-table-list">
      <div v-for="component in subTables" :key="component.id" class="sub-table-card">
        <div class="sub-table-card-main">
          <n-input
            :value="component.props?.header || ''"
            size="small"
            placeholder="子表标题"
            @update:value="updateSubTableProps(component.id, { header: $event || '关联子表' })"
          />
          <n-input
            :value="component.props?.relationKey || ''"
            size="small"
            clearable
            placeholder="子表标识（可手填，如 order_item）"
            @update:value="updateSubTableRelationKey(component.id, $event)"
          />
          <n-select
            v-if="relationOptions.length"
            :value="component.props?.relationKey || ''"
            :options="relationOptions"
            size="small"
            filterable
            clearable
            placeholder="从对象关系选择（可选）"
            @update:value="applySubTableRelation(component.id, $event)"
          />
          <n-select
            :value="component.props?.displayMode || 'inline_grid'"
            :options="displayModeOptions"
            size="small"
            placeholder="展示方式"
            @update:value="updateSubTableProps(component.id, { displayMode: $event || 'inline_grid' })"
          />
        </div>
        <div class="sub-table-card-actions">
          <n-button size="tiny" tertiary @click="designerStore.selectComponent(component.id)">
            画布定位
          </n-button>
          <n-button size="tiny" quaternary type="error" @click="removeSubTable(component.id)">
            删除
          </n-button>
        </div>
      </div>
    </div>
    <div v-else class="sub-table-empty">
      还没有子表。点击下方按钮直接添加一个子表分区，再按需填写子表标识。
    </div>

    <div class="sub-table-add">
      <n-button size="small" type="primary" secondary block @click="addSubTable">
        + 添加子表
      </n-button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useFormDesignerStore } from '@/store'
import { normalizeRelationOption } from '../pageSectionEditorUtils'

/**
 * 主子表配置面板（从 ForgePropertyPanel 拆出，Pinia 化改造）
 *
 * 按产品决策：不再依赖「数据模型-对象关系」作为前置校验——
 * 用户可直接添加子表分区并手填子表标识；对象关系仅作为可选快捷来源。
 */
const designerStore = useFormDesignerStore()

const subTables = computed(() => designerStore.subTableComponents)

const relationOptions = computed(() => {
  return (designerStore.relations || [])
    .map(normalizeRelationOption)
    .filter(Boolean)
})

const displayModeOptions = [
  { label: '行内表格', value: 'inline_grid' },
  { label: '卡片列表', value: 'card_list' },
  { label: '底部抽屉', value: 'bottom_sheet' },
]

function updateSubTableProps(componentId, propsPatch = {}) {
  designerStore.updateComponent(componentId, { props: propsPatch })
}

/** 手填子表标识：直接写入 relationKey，不做存在性校验 */
function updateSubTableRelationKey(componentId, relationKey = '') {
  designerStore.updateComponent(componentId, {
    props: { relationKey: String(relationKey || '').trim() },
  })
}

/** 从对象关系下拉选择：同时回填标识与标题（保留手动改过的标题时不覆盖？——选择关系视为一次明确的绑定，覆盖标题） */
function applySubTableRelation(componentId, relationKey = '') {
  const matched = relationOptions.value.find(option => option.value === relationKey)
  if (!matched) {
    updateSubTableRelationKey(componentId, relationKey)
    return
  }
  const header = matched.label.replace(/（[^（）]+）$/, '')
  designerStore.updateComponent(componentId, {
    label: header,
    props: { relationKey, header },
  })
}

function removeSubTable(componentId) {
  designerStore.removeComponent(componentId)
}

/** 直接添加子表分区：默认标题「子表 N」，标识可后续手填，不依赖对象关系 */
function addSubTable() {
  const header = `子表 ${subTables.value.length + 1}`
  const component = {
    id: `cmp_subTable_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    componentKey: 'subTable',
    label: header,
    props: { header, relationKey: '', displayMode: 'inline_grid' },
    layout: { span: designerStore.schema.layout?.gridColumns || 2, align: 'left' },
  }
  designerStore.insertComponent(
    { parentId: '', index: designerStore.schema.components?.length || 0 },
    component,
  )
  // 不再自动选中子表，避免属性面板切到空的子表属性；用户可在下方卡片中编辑或点击“画布定位”
}
</script>

<style scoped>
.sub-table-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-table-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-table-card {
  padding: 8px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.sub-table-card-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sub-table-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 6px;
}

.sub-table-add {
  display: flex;
  gap: 8px;
}

.sub-table-empty {
  padding: 14px 10px;
  border: 1px dashed var(--border-light, #e5e7eb);
  border-radius: 6px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

.subtable-guide {
  padding: 10px 12px;
  border-radius: 8px;
  background: #f0f5ff;
  border: 1px solid #d6e4ff;
}

.subtable-guide-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #1d39c4;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  margin-bottom: 4px;
}

.subtable-guide-desc {
  margin: 0 0 8px;
  color: #434343;
  font-size: 12px;
  line-height: 1.6;
}

.subtable-guide-steps {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.subtable-guide-step {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #434343;
  font-size: 12px;
  line-height: 1.6;
}

.step-num {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #1d39c4;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  margin-top: 1px;
}

.subtable-no-relations-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  color: #614700;
  font-size: 12px;
  line-height: 1.6;
}

.subtable-no-relations-hint svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #d48806;
}

.subtable-no-relations-hint strong {
  color: #1d39c4;
  font-weight: 600;
}
</style>

<template>
  <div class="field-list">
    <div class="field-list-head">
      <div>
        <h3>业务字段</h3>
        <p>{{ fields.length }} 个字段，系统字段默认只读。</p>
      </div>
      <n-button type="primary" size="small" @click="$emit('create')">
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        新增字段
      </n-button>
    </div>

    <div class="field-scroll">
      <button
        v-for="(field, index) in fields"
        :key="field.fieldCode || field.fieldName || index"
        type="button"
        class="field-row"
        :class="{ active: field.fieldCode === selectedFieldCode }"
        @click="$emit('select', field)"
      >
        <div class="field-main">
          <strong>{{ field.fieldName || field.fieldCode || '未命名字段' }}</strong>
          <span>{{ fieldTypeLabel(field.fieldType) }} · {{ field.componentType || '自动控件' }}</span>
        </div>
        <div class="field-meta">
          <n-tag v-if="field.systemField" size="small" :bordered="false">
            系统
          </n-tag>
          <n-tag v-else size="small" :type="field.fieldStatus === 'DISABLED' ? 'warning' : 'success'" :bordered="false">
            {{ field.fieldStatus === 'DISABLED' ? '停用' : '启用' }}
          </n-tag>
          <span>{{ field.fieldCode || '-' }}</span>
        </div>
        <div class="field-actions" @click.stop>
          <n-button quaternary circle size="small" :disabled="index === 0" @click="$emit('move', index, index - 1)">
            <template #icon>
              <n-icon><ChevronUpOutline /></n-icon>
            </template>
          </n-button>
          <n-button quaternary circle size="small" :disabled="index === fields.length - 1" @click="$emit('move', index, index + 1)">
            <template #icon>
              <n-icon><ChevronDownOutline /></n-icon>
            </template>
          </n-button>
          <n-button quaternary circle size="small" :disabled="field.systemField" @click="$emit('duplicate', field)">
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
          </n-button>
          <n-button quaternary circle size="small" type="error" :disabled="field.systemField || field.canDelete === false" @click="$emit('delete', field)">
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
          </n-button>
        </div>
      </button>

      <n-empty v-if="!fields.length" description="还没有业务字段" />
    </div>
  </div>
</template>

<script setup>
import {
  AddOutline,
  ChevronDownOutline,
  ChevronUpOutline,
  CopyOutline,
  TrashOutline,
} from '@vicons/ionicons5'

defineProps({
  fields: {
    type: Array,
    default: () => [],
  },
  selectedFieldCode: {
    type: String,
    default: '',
  },
})

defineEmits(['select', 'create', 'patch', 'duplicate', 'delete', 'move'])

function fieldTypeLabel(value) {
  const labels = {
    TEXT: '文本',
    TEXTAREA: '多行文本',
    NUMBER: '数字',
    MONEY: '金额',
    DATE: '日期',
    DATETIME: '日期时间',
    DICT: '下拉',
    RADIO: '单选',
    CHECKBOX: '多选',
    SWITCH: '开关',
    FILE: '附件',
    IMAGE: '图片',
    USER: '人员',
    DEPT: '部门',
    REGION: '地区',
    REFERENCE: '引用对象',
  }
  return labels[value] || value || '文本'
}
</script>

<style scoped>
.field-list {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  border-right: 1px solid #e5e7eb;
}

.field-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px;
}

.field-list-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.field-list-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.field-scroll {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}

.field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(110px, 0.45fr) 136px;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  padding: 12px;
}

.field-row:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.field-row.active {
  border-color: #2563eb;
  box-shadow: inset 3px 0 0 #2563eb;
}

.field-main strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-main span,
.field-meta span {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

@media (max-width: 1180px) {
  .field-row {
    grid-template-columns: 1fr;
  }

  .field-actions {
    justify-content: flex-start;
  }
}
</style>

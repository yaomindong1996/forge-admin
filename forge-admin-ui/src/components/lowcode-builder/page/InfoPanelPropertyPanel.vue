<script setup>
import { computed } from 'vue'
import {
  NColorPicker,
  NInput,
  NSelect,
  NSwitch,
} from 'naive-ui'
import IconSelector from '@/components/IconSelector.vue'
import {
  FREE_LAYOUT_INFO_TYPE_OPTIONS,
  FREE_LAYOUT_LINK_TYPE_OPTIONS,
} from './free-layout-widget-kit.js'

const props = defineProps({
  block: { type: Object, required: true },
  pageOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(['patch-props'])

const pageSelectOptions = computed(() =>
  (Array.isArray(props.pageOptions) ? props.pageOptions : [])
    .map(item => ({
      label: item.label || item.value,
      value: String(item.value || ''),
    }))
    .filter(item => item.value),
)

function patch(partial) {
  emit('patch-props', partial)
}
</script>

<template>
  <div class="ip-prop">
    <div class="ip-field">
      <label>标题</label>
      <NInput
        :value="block.props?.title || ''"
        size="small"
        placeholder="提示标题"
        @update:value="(v) => patch({ title: v })"
      />
    </div>
    <div class="ip-field">
      <label>内容</label>
      <NInput
        :value="block.props?.content || ''"
        type="textarea"
        :autosize="{ minRows: 3, maxRows: 8 }"
        size="small"
        placeholder="提示正文"
        @update:value="(v) => patch({ content: v })"
      />
    </div>
    <div class="ip-field">
      <label>类型</label>
      <NSelect
        :value="block.props?.type || 'info'"
        :options="FREE_LAYOUT_INFO_TYPE_OPTIONS"
        size="small"
        @update:value="(v) => patch({ type: v || 'info' })"
      />
    </div>
    <div class="ip-field ip-field-row">
      <label>显示图标</label>
      <NSwitch
        :value="block.props?.showIcon !== false"
        size="small"
        @update:value="(v) => patch({ showIcon: v !== false })"
      />
    </div>
    <div class="ip-field">
      <label>自定义图标</label>
      <IconSelector
        :model-value="block.props?.icon || ''"
        @update:model-value="(v) => patch({ icon: v || '' })"
      />
    </div>
    <div class="ip-field">
      <label>强调色（可选）</label>
      <NColorPicker
        :value="block.props?.color || null"
        :modes="['hex']"
        size="small"
        clearable
        @update:value="(v) => patch({ color: v || '' })"
      />
    </div>
    <div class="ip-field">
      <label>跳转类型</label>
      <NSelect
        :value="block.props?.linkType || 'none'"
        :options="FREE_LAYOUT_LINK_TYPE_OPTIONS"
        size="small"
        @update:value="(v) => patch({ linkType: v || 'none' })"
      />
    </div>
    <div v-if="(block.props?.linkType || 'none') === 'page'" class="ip-field">
      <label>目标页面</label>
      <NSelect
        :value="block.props?.targetPageId || null"
        :options="pageSelectOptions"
        clearable
        filterable
        size="small"
        @update:value="(v) => patch({ targetPageId: v || '', linkType: 'page' })"
      />
    </div>
    <div v-if="block.props?.linkType === 'url'" class="ip-field">
      <label>外部链接</label>
      <NInput
        :value="block.props?.href || ''"
        size="small"
        placeholder="https://"
        @update:value="(v) => patch({ href: v })"
      />
    </div>
  </div>
</template>

<style scoped>
.ip-prop {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ip-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ip-field > label {
  font-size: 12px;
  color: #64748b;
}

.ip-field-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
</style>

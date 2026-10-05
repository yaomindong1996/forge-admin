<template>
  <div class="tenant-appearance">
    <!-- 租户默认布局 -->
    <div class="appearance-label">
      默认布局
    </div>
    <LayoutPicker v-model="layout" tenant />
    <!-- 租户默认配色，不即时修改当前会话主题 -->
    <div class="appearance-label">
      导航配色
    </div>
    <AppearanceThemeEditor v-model="theme" :layout="layout" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppearanceThemeEditor from '@/components/common/appearance/AppearanceThemeEditor.vue'
import LayoutPicker from '@/components/common/appearance/LayoutPicker.vue'
import { defaultLayout } from '@/settings'
import { readTenantAppearance } from './tenant-appearance'

const props = defineProps({ formData: { type: Object, required: true } })
const emit = defineEmits(['layoutChange', 'themeChange'])
const layout = computed({
  get: () => props.formData.systemLayout || defaultLayout,
  set: value => emit('layoutChange', value),
})
const theme = computed({
  get: () => readTenantAppearance(props.formData),
  set: value => emit('themeChange', value),
})
</script>

<style scoped>
.tenant-appearance {
  display: grid;
  gap: 12px;
  width: 100%;
}
.appearance-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}
</style>

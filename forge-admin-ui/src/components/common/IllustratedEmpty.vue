<script setup>
import { NEmpty } from 'naive-ui'
import WorkspaceIllustration from './WorkspaceIllustration.vue'

defineProps({
  artwork: { type: String, default: 'application' },
  description: { type: String, default: '暂无数据' },
  compact: Boolean,
})
</script>

<template>
  <NEmpty class="illustrated-empty" :class="{ 'is-compact': compact }" :description="description">
    <template #icon>
      <WorkspaceIllustration :artwork="artwork" :size="compact ? 'small' : 'empty'" />
    </template>
    <template v-if="$slots.extra" #extra>
      <slot name="extra" />
    </template>
  </NEmpty>
</template>

<style scoped>
.illustrated-empty {
  padding: 20px 16px;
}

.illustrated-empty :deep(.n-empty__icon) {
  width: auto;
  height: auto;
  margin: 0 0 12px;
}

.illustrated-empty :deep(.n-empty__description) {
  color: var(--text-tertiary, #86909c);
  font-size: 13px;
  line-height: 1.5;
}

.illustrated-empty.is-compact {
  padding: 16px 12px;
}
</style>

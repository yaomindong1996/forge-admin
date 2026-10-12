<template>
  <div v-if="visible" class="flow-task-business-summary">
    <span v-if="headline" class="task-business-headline">{{ headline }}</span>
    <span
      v-for="field in fields"
      :key="field.key"
      class="task-business-chip"
    >
      <em v-if="field.label">{{ field.label }}</em>{{ field.value }}
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { buildTaskBusinessHeadline, buildTaskDisplayFields } from '@/views/flow/utils/processDisplay'

const props = defineProps({
  row: { type: Object, default: () => ({}) },
})

const headline = computed(() => buildTaskBusinessHeadline(props.row))
const fields = computed(() => buildTaskDisplayFields(props.row))
const visible = computed(() => Boolean(headline.value || fields.value.length))
</script>

<style scoped>
.flow-task-business-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.task-business-headline {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 18px;
}

.task-business-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 180px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--bg-tertiary);
  color: var(--text-primary);
  font-size: 11px;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-business-chip em {
  color: var(--text-secondary);
  font-style: normal;
}

.task-business-chip em::after {
  content: '：';
}
</style>

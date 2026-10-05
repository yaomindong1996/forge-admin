<template>
  <button
    v-if="interactive"
    type="button"
    class="system-table-cell system-table-cell--interactive system-table-cell--entity"
    :title="tooltip || title || '-'"
    @click="emit('activate')"
  >
    <span v-if="avatar" class="system-table-cell__avatar" aria-hidden="true">{{ avatarInitial }}</span>
    <span class="system-table-cell__identity">
      <span class="system-table-cell__primary">{{ title || '-' }}</span>
      <span v-if="subtitle" class="system-table-cell__secondary">{{ subtitle }}</span>
    </span>
  </button>
  <div v-else class="system-table-cell" :title="tooltip || resolvedValues.join('、') || '-'">
    <span class="system-table-cell__primary">{{ resolvedValues[0] || title || '-' }}</span>
    <NPopover v-if="resolvedValues.length > 1" trigger="click" placement="bottom-start" :show-arrow="false">
      <template #trigger>
        <button type="button" class="system-table-cell__count" :aria-label="`查看全部 ${resolvedValues.length} 项`" @click.stop>
          +{{ resolvedValues.length - 1 }}
        </button>
      </template>
      <div class="system-table-cell__popover">
        <span v-for="value in resolvedValues" :key="value">{{ value }}</span>
      </div>
    </NPopover>
    <span v-if="subtitle" class="system-table-cell__secondary">{{ subtitle }}</span>
  </div>
</template>

<script setup>
import { NPopover } from 'naive-ui'
import { computed } from 'vue'

defineOptions({ name: 'SystemTableCell' })

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
  subtitle: {
    type: String,
    default: '',
  },
  values: {
    type: Array,
    default: () => [],
  },
  tooltip: {
    type: String,
    default: '',
  },
  interactive: {
    type: Boolean,
    default: false,
  },
  /** 是否展示实体标识，用于用户等可识别主体。 */
  avatar: {
    type: Boolean,
    default: false,
  },
  /** 实体标识文字，默认取主标题首字符。 */
  avatarText: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['activate'])

const resolvedValues = computed(() => Array.from(new Set(
  (props.values || [])
    .map(value => String(value || '').trim())
    .filter(Boolean),
)))

const avatarInitial = computed(() => {
  const text = String(props.avatarText || props.title || '?').trim()
  return text.slice(0, 1).toLocaleUpperCase() || '?'
})
</script>

<style scoped>
.system-table-cell {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
  align-items: center;
  gap: 6px;
  color: var(--text-primary, #0f172a);
  font-size: 13px;
  line-height: 1.4;
  text-align: left;
}

.system-table-cell--interactive {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.system-table-cell--entity {
  gap: 8px;
}

.system-table-cell__identity {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
}

.system-table-cell__avatar {
  width: 28px;
  height: 28px;
  display: inline-flex;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--primary-color, #2563eb);
  background: color-mix(in srgb, var(--primary-color, #2563eb) 12%, transparent);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.system-table-cell__primary {
  overflow: hidden;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-table-cell--interactive .system-table-cell__primary {
  color: var(--primary-color, #2563eb);
}

.system-table-cell--interactive:hover .system-table-cell__primary {
  color: var(--primary-hover-color, #1d4ed8);
}

.system-table-cell--interactive:hover .system-table-cell__avatar {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 18%, transparent);
}

.system-table-cell__secondary {
  overflow: hidden;
  max-width: 100%;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-table-cell__count {
  flex: 0 0 auto;
  padding: 0;
  border: 0;
  color: var(--text-tertiary, #64748b);
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.system-table-cell__count:hover {
  color: var(--primary-color, #2563eb);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.system-table-cell__popover {
  display: flex;
  min-width: 132px;
  max-width: min(320px, 70vw);
  flex-direction: column;
  gap: 6px;
  padding: 2px;
}

.system-table-cell__popover span {
  overflow: hidden;
  color: var(--text-primary, #0f172a);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-table-cell--interactive:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--primary-color, #2563eb) 42%, transparent);
  outline-offset: 2px;
}

:global(.dark) .system-table-cell__popover span {
  color: #e2e8f0;
}
</style>

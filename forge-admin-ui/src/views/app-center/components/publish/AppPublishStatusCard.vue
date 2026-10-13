<template>
  <section class="publish-status-card">
    <div class="publish-status-icon" :class="application.status === 1 ? 'is-active' : 'is-disabled'">
      <n-icon><RocketOutline /></n-icon>
    </div>
    <div class="publish-status-copy">
      <span>当前发布状态</span>
      <h2>{{ statusTitle }}</h2>
      <p>{{ statusDescription }}</p>
    </div>
    <div class="publish-status-facts">
      <div><span>当前版本</span><strong>{{ application.lastPublishVersion ? `v${application.lastPublishVersion}` : '未发布' }}</strong></div>
      <div><span>最近发布</span><strong>{{ application.lastPublishTime || '-' }}</strong></div>
      <div><span>页面</span><strong>{{ pageCount }}</strong></div>
    </div>
    <n-space class="publish-status-actions">
      <n-button secondary :type="application.status === 1 ? 'warning' : 'success'" :loading="toggling" @click="emit('toggle')">
        <template #icon>
          <n-icon>
            <PauseCircleOutline v-if="application.status === 1" />
            <PlayCircleOutline v-else />
          </n-icon>
        </template>
        {{ application.status === 1 ? '停用应用' : '启用应用' }}
      </n-button>
    </n-space>
  </section>
</template>

<script setup>
import { PauseCircleOutline, PlayCircleOutline, RocketOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import { parseJsonObject } from '../portal/portal-config'

const props = defineProps({
  application: { type: Object, required: true },
  toggling: Boolean,
})
const emit = defineEmits(['toggle'])
const pageCount = computed(() => {
  const nodes = parseJsonObject(props.application.options)?.inAppBuilder?.nodes
  return Array.isArray(nodes) ? nodes.filter(node => node?.type === 'page').length : 0
})

const statusTitle = computed(() => {
  if (props.application.status !== 1)
    return '应用已停用'
  return props.application.lastPublishVersion ? '应用运行中' : '等待首次发布'
})
const statusDescription = computed(() => {
  if (props.application.status !== 1)
    return '正式门户当前不可访问，历史版本快照仍然保留。'
  if (!props.application.lastPublishVersion)
    return '完成发布检查并生成第一个不可变版本后，门户才会开放访问。'
  return '正式门户使用当前不可变版本；设计态修改需重新发布后生效。'
})
</script>

<style scoped>
.publish-status-card {
  display: grid;
  grid-template-columns: 54px minmax(220px, 1fr) minmax(300px, 1.2fr) auto;
  align-items: center;
  gap: 18px;
  border: 1px solid var(--border-color, #e5e6eb);
  border-radius: 10px;
  background: var(--card-color, #fff);
  padding: 20px;
}

.publish-status-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 12px;
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-size: 26px;
}

.publish-status-icon.is-active {
  background: color-mix(in srgb, #16a34a 12%, transparent);
  color: #15803d;
}

.publish-status-copy span,
.publish-status-facts span {
  color: var(--text-color-3, #86909c);
  font-size: 12px;
}

.publish-status-copy h2 {
  margin: 3px 0;
  font-size: 20px;
}

.publish-status-copy p {
  margin: 0;
  color: var(--text-color-2, #646a73);
  font-size: 13px;
  line-height: 1.5;
}

.publish-status-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.publish-status-facts div {
  display: grid;
  gap: 4px;
  border-left: 1px solid var(--border-color, #e5e6eb);
  padding-left: 14px;
}

.publish-status-facts strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .publish-status-card {
    grid-template-columns: 52px minmax(0, 1fr) auto;
  }

  .publish-status-facts {
    grid-column: 1 / -1;
  }
}
</style>

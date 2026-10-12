<template>
  <div class="version-timeline">
    <div class="panel-title">
      发布版本
    </div>
    <n-spin :show="loading">
      <n-empty v-if="!visibleVersions.length" description="暂无发布版本" />
      <n-timeline v-else>
        <n-timeline-item
          v-for="item in visibleVersions"
          :key="item.id"
          :type="item.versionType === 'rollback' ? 'warning' : 'success'"
          :title="`版本 ${item.versionNo}`"
          :content="resolveContent(item)"
          :time="formatTime(item.createTime)"
        >
          <template #footer>
            <n-popconfirm @positive-click="$emit('rollback', item.id)">
              <template #trigger>
                <n-button text size="tiny" class="text-warning">
                  回滚到此版本
                </n-button>
              </template>
              确认回滚到版本 {{ item.versionNo }}？
            </n-popconfirm>
          </template>
        </n-timeline-item>
      </n-timeline>
      <div v-if="versions.length > maxVisible" class="version-more">
        仅显示最近 {{ maxVisible }} 个版本，共 {{ versions.length }} 个
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  versions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['rollback'])

const maxVisible = 5
const visibleVersions = computed(() => (props.versions || []).slice(0, maxVisible))

function resolveContent(item) {
  const action = item.remark || (item.versionType === 'rollback' ? '回滚发布' : '发布上线')
  const time = formatTime(item.createTime)
  return time ? `${action} · 发布时间 ${time}` : action
}

function formatTime(value) {
  if (!value)
    return ''
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.version-timeline {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.version-more {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  text-align: center;
}
</style>

<template>
  <n-drawer
    :show="show"
    :width="820"
    placement="right"
    @update:show="value => emit('update:show', value)"
  >
    <n-drawer-content :title="`版本与差异 · ${extension?.extensionName || ''}`" closable>
      <n-spin :show="loading">
        <div v-if="versions.length" class="version-workbench">
          <aside class="version-list">
            <button
              v-for="item in versions"
              :key="item.versionNo"
              type="button"
              :class="{ active: item.versionNo === targetVersion }"
              @click="selectVersion(item.versionNo)"
            >
              <span><strong>v{{ item.versionNo }}</strong><small>{{ item.createTime || '-' }}</small></span>
              <span class="version-flags">
                <i :class="{ passed: item.validationPassed === 1 }">校验</i>
                <i :class="{ passed: item.testPassed === 1 }">测试</i>
              </span>
            </button>
          </aside>

          <main class="diff-panel">
            <div class="diff-toolbar">
              <label>对比</label>
              <n-select v-model:value="baseVersion" :options="versionOptions" @update:value="loadDiff" />
              <span>→</span>
              <n-select v-model:value="targetVersion" :options="versionOptions" @update:value="loadDiff" />
              <n-button
                size="small"
                secondary
                :disabled="targetVersion === extension?.draftVersion"
                @click="rollbackTarget"
              >
                回滚为新草稿
              </n-button>
            </div>

            <n-alert v-if="diff && !diff.changed" type="info" :show-icon="false">
              两个版本内容摘要一致。
            </n-alert>

            <div v-if="diff" class="diff-columns">
              <section>
                <header>v{{ diff.baseVersion }}</header>
                <pre>{{ diff.baseContent || '(空内容)' }}</pre>
              </section>
              <section>
                <header>v{{ diff.targetVersion }}</header>
                <pre>{{ diff.targetContent || '(空内容)' }}</pre>
              </section>
            </div>
          </main>
        </div>
        <n-empty v-else-if="!loading" description="暂无版本记录" />
      </n-spin>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { useDialog, useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import {
  acquireBusinessExtensionLock,
  businessExtensionDiff,
  businessExtensionVersions,
  releaseBusinessExtensionLock,
  rollbackBusinessExtension,
} from '@/api/business-extension'

const props = defineProps({
  show: Boolean,
  extension: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'changed'])
const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const versions = ref([])
const baseVersion = ref(null)
const targetVersion = ref(null)
const diff = ref(null)

const versionOptions = computed(() => versions.value.map(item => ({
  label: `v${item.versionNo} · ${item.changeSummary || '无变更说明'}`,
  value: item.versionNo,
})))

watch(() => props.show, (visible) => {
  if (visible)
    loadVersions()
})

async function loadVersions() {
  if (!props.extension?.id)
    return
  loading.value = true
  try {
    const response = await businessExtensionVersions(props.extension.id)
    versions.value = response.data || []
    targetVersion.value = props.extension.draftVersion || versions.value[0]?.versionNo || null
    baseVersion.value = versions.value.find(item => item.versionNo < targetVersion.value)?.versionNo
      || targetVersion.value
    await loadDiff()
  }
  finally {
    loading.value = false
  }
}

async function loadDiff() {
  if (!props.extension?.id || !baseVersion.value || !targetVersion.value)
    return
  const response = await businessExtensionDiff(props.extension.id, {
    baseVersion: baseVersion.value,
    targetVersion: targetVersion.value,
  })
  diff.value = response.data || null
}

function selectVersion(versionNo) {
  targetVersion.value = versionNo
  const previous = versions.value.find(item => item.versionNo < versionNo)
  baseVersion.value = previous?.versionNo || versionNo
  loadDiff()
}

function rollbackTarget() {
  dialog.warning({
    title: '回滚扩展草稿',
    content: `将 v${targetVersion.value} 的内容复制为新的草稿版本，历史版本和当前运行版本都不会被覆盖。`,
    positiveText: '生成回滚草稿',
    negativeText: '取消',
    onPositiveClick: async () => {
      const lockResponse = await acquireBusinessExtensionLock(props.extension.id)
      const lockToken = lockResponse.data?.lockToken
      try {
        await rollbackBusinessExtension(props.extension.id, targetVersion.value, lockToken)
        message.success('已从历史版本生成新的草稿')
        emit('changed')
        await loadVersions()
      }
      finally {
        if (lockToken) {
          try {
            await releaseBusinessExtensionLock(props.extension.id, lockToken)
          }
          catch {
            // 锁释放失败不覆盖回滚结果，超时机制会自动清理。
          }
        }
      }
    },
  })
}
</script>

<style scoped>
.version-workbench {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: calc(100vh - 120px);
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.version-list {
  overflow-y: auto;
  border-right: 1px solid var(--border-default, #c9cdd4);
  background: var(--bg-secondary, #f7f8fa);
}

.version-list button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 58px;
  padding: 8px 10px;
  cursor: pointer;
  border: 0;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  background: transparent;
  text-align: left;
}

.version-list button.active {
  color: var(--primary-color, #165dff);
  background: var(--bg-primary, #fff);
  box-shadow: inset 3px 0 var(--primary-color, #165dff);
}

.version-list button > span:first-child {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.version-list small {
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
}

.version-flags {
  display: flex;
  gap: 3px;
  flex-direction: column;
}

.version-flags i {
  color: var(--text-tertiary, #86909c);
  font-size: 10px;
  font-style: normal;
}

.version-flags i.passed {
  color: var(--success-600, #16a34a);
}

.diff-panel {
  min-width: 0;
  padding: 12px;
}

.diff-toolbar {
  display: grid;
  grid-template-columns: auto minmax(130px, 1fr) auto minmax(130px, 1fr) auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.diff-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.diff-columns section {
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 6px;
}

.diff-columns header {
  padding: 7px 9px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
  font-weight: 600;
}

.diff-columns pre {
  overflow: auto;
  min-height: 420px;
  max-height: calc(100vh - 250px);
  margin: 0;
  padding: 10px;
  color: var(--text-secondary, #4e5969);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

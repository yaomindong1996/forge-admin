<template>
  <section class="plugin-artifact">
    <!-- 当前任务候选审计，不表示制品上传/部署 -->
    <header>
      <h3>候选制品</h3>
      <NButton v-if="canRegister && available && !editing" :disabled="busy" @click="editing = true">
        登记候选制品
      </NButton>
    </header>
    <p class="hint">
      人工登记仅保存摘要；平台未读取实际制品，不是远程发布或部署许可。
    </p>
    <div v-for="row in task.artifacts || []" :key="row.id" class="registration">
      <NAlert :type="row.currentApprovalMatches ? 'info' : 'warning'" :bordered="false">
        {{ row.currentApprovalMatches ? '当前审批匹配（仅查询时点）' : '当前审批不匹配，保留登记历史' }} · 未部署
      </NAlert>
      <NDescriptions bordered :column="1" size="small" label-placement="left">
        <NDescriptionsItem label="候选编号">
          <code>{{ row.metadata.releaseId }}</code>
        </NDescriptionsItem>
        <NDescriptionsItem label="本地仓库标签">
          {{ row.metadata.repositoryId }}（人工登记）
        </NDescriptionsItem>
        <NDescriptionsItem label="产物摘要">
          <code>{{ row.metadata.result.artifactManifestSha256 }}</code>
        </NDescriptionsItem>
        <NDescriptionsItem label="产物规模">
          {{ row.metadata.result.artifactCount }} 个文件 · {{ size(row.metadata.result.artifactBytes) }}
        </NDescriptionsItem>
        <NDescriptionsItem label="登记人 / 时间">
          用户 {{ row.registeredBy }} · {{ pluginTime(row.registeredTime) }}
        </NDescriptionsItem>
        <NDescriptionsItem label="核查说明">
          {{ row.note }}
        </NDescriptionsItem>
      </NDescriptions>
    </div>
    <p v-if="!task.artifacts?.length && !editing" class="hint">
      暂无候选制品登记。
    </p>
    <!-- 导入和冻结重试表单 -->
    <div v-if="canRegister && (editing || store.pending)" class="artifact-form">
      <NAlert v-if="store.pending" type="warning" :bordered="false">
        登记结果未确认，请先刷新核查记录；重试使用原请求。切换任务会清除本地草稿，服务器审计不受影响。
      </NAlert>
      <NAlert v-if="store.error" type="error" :bordered="false">
        {{ store.error }}
      </NAlert>
      <NUpload accept=".json" :show-file-list="false" :disabled="locked" @before-upload="importFile">
        <NButton :disabled="locked">
          导入登记 JSON（≤64 KiB）
        </NButton>
      </NUpload>
      <label class="form-label">登记元数据</label>
      <NInput
        v-model:value="store.draft.text" type="textarea" :disabled="locked" :maxlength="65536"
        :input-props="{ 'aria-label': '登记元数据' }"
        :autosize="{ minRows: 3, maxRows: 6 }"
        placeholder="导入或粘贴 prepare-registration 输出；不要粘贴原始日志、路径或凭证"
        @update:value="store.metadata = null"
      />
      <NButton v-if="!store.pending" :disabled="locked || !store.draft.text" @click="store.preview(task)">
        核对导入内容
      </NButton>
      <NDescriptions v-if="store.metadata" bordered :column="1" size="small" label-placement="left">
        <NDescriptionsItem label="候选编号">
          <code>{{ store.metadata.releaseId }}</code>
        </NDescriptionsItem>
        <NDescriptionsItem label="审批 / 修订">
          {{ store.metadata.reviewId }} · {{ store.metadata.revision }}
        </NDescriptionsItem>
        <NDescriptionsItem label="原报告文件摘要">
          <code>{{ store.metadata.resultSha256 }}</code>
        </NDescriptionsItem>
        <NDescriptionsItem label="服务端报告摘要">
          <code>{{ store.metadata.serverResultSha256 }}</code>
        </NDescriptionsItem>
      </NDescriptions>
      <NCheckbox v-model:checked="store.draft.localVerified" :disabled="locked">
        已在私有目录逐文件复验，理解这属于人工声明，不是平台独立复验。
      </NCheckbox>
      <NCheckbox v-model:checked="store.draft.notDeployed" :disabled="locked">
        本次候选制品尚未部署。
      </NCheckbox>
      <label class="form-label">核查说明</label>
      <NInput
        v-model:value="store.draft.note" type="textarea" :disabled="locked" :maxlength="1000" show-count
        :input-props="{ 'aria-label': '登记核查说明' }"
        :autosize="{ minRows: 2, maxRows: 4 }" placeholder="10至1000字，说明核查依据；不要填写凭证"
      />
      <NSpace>
        <NButton v-if="!store.pending" :disabled="busy" @click="cancel">
          取消登记
        </NButton>
        <NButton
          type="primary" :loading="busy" :disabled="busy || (!store.pending && !complete)"
          @click="submit"
        >
          {{ store.pending ? '重试同一登记请求' : '确认登记候选制品' }}
        </NButton>
      </NSpace>
    </div>
  </section>
</template>

<script setup>
import {
  NAlert,
  NButton,
  NCheckbox,
  NDescriptions,
  NDescriptionsItem,
  NInput,
  NSpace,
  NUpload,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { usePluginArtifactStore } from '@/stores/plugin/artifactStore'
import { pluginTime } from '../pluginTaskUtils'
import { usePluginPermission } from '../usePluginPermission'

const props = defineProps({ task: { type: Object, required: true }, busy: Boolean })
const emit = defineEmits(['register'])
const store = usePluginArtifactStore()
const permission = usePluginPermission()
const canRegister = computed(() => permission('system:plugin:artifact:register'))
const editing = ref(false)
const available = computed(() => props.task.status === 'release_ready' && !props.task.artifacts?.length)
const locked = computed(() => props.busy || Boolean(store.pending))
const complete = computed(() => available.value && Boolean(store.metadata)
  && store.draft.localVerified && store.draft.notDeployed && store.draft.note.trim().length >= 10)
const size = bytes => `${(bytes / 1024 / 1024).toFixed(2)} MiB`
let importSequence = 0
watch(() => props.task, (task) => {
  store.select(task)
  if (task.artifacts?.length)
    editing.value = false
}, { immediate: true })
watch(() => props.task.id, () => {
  ++importSequence
  editing.value = false
})

async function importFile({ file }) {
  const sequence = ++importSequence
  const id = props.task.id
  if (locked.value)
    return false
  if (!file.file || file.file.size > 65536 || !/\.json$/i.test(file.name)) {
    store.error = '请选择不超过64 KiB的JSON文件'
    return false
  }
  try {
    const text = await file.file.text()
    if (sequence !== importSequence || props.task.id !== id || store.taskId !== id || locked.value)
      return false
    store.draft.text = text
    store.preview(props.task)
  }
  catch {
    if (sequence === importSequence && store.taskId === id)
      store.error = '读取登记文件失败，请重新选择'
  }
  return false // 只读入浏览器内存；NUpload不得自动发送文件。
}
function cancel() {
  store.clear()
  editing.value = false
}
function submit() {
  if (!canRegister.value || props.busy)
    return
  store.prepare(props.task)
  if (store.pending)
    emit('register')
}
</script>

<style scoped>
.plugin-artifact,
.artifact-form,
.registration {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.plugin-artifact {
  border-top: 1px solid var(--border-light);
  padding-top: 12px;
}
header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}
.hint {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
}
.form-label {
  font-size: 12px;
  color: var(--text-secondary);
}
code,
:deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}
.artifact-form {
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
}
</style>

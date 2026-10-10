<template>
  <section class="plugin-review">
    <!-- 当前任务的人工审查入口，运行时授权仍由服务端决定 -->
    <div v-if="canReview && (canApprove || canClose)" class="plugin-review__actions">
      <NButton v-if="canApprove" size="small" :disabled="busy" @click="start('approve_build')">
        审查构建
      </NButton>
      <NButton v-if="canClose" size="small" :disabled="busy" @click="start('close_task')">
        核查并关闭任务
      </NButton>
    </div>
    <NAlert v-if="localError && !decision" type="warning">
      {{ localError }}
    </NAlert>
    <NAlert v-if="decision && canReview" type="warning" :title="title" :bordered="false">
      <!-- 明确声明与核查范围，不把人工勾选写成平台验证 -->
      <p>这是管理员的人工确认，不是系统远程验证。不会部署、重试构建或删除私有包、产物及审计。</p>
      <p v-if="decision === 'close_task'">
        关闭后释放插件占用；失联构建会封存，旧续期/回写不再生效。
      </p>
      <div class="plugin-review__form">
        <NCheckbox v-model:checked="form.executorStopped" :disabled="locked">
          已核查并停止本次执行器及对应容器
        </NCheckbox>
        <NCheckbox v-model:checked="form.notDeployed" :disabled="locked">
          已确认本次产物尚未部署
        </NCheckbox>
        <template v-if="decision === 'approve_build'">
          <NCheckbox v-model:checked="form.artifactsReviewed" :disabled="locked">
            已独立核对产物摘要及完整性
          </NCheckbox>
          <NCheckbox v-model:checked="form.migrationsReviewed" :disabled="locked">
            已审查迁移、权限变化及数据库恢复方案
          </NCheckbox>
        </template>
        <NInput
          v-model:value="form.note" type="textarea" :rows="3" :maxlength="1000" show-count :disabled="locked"
          placeholder="填写至少10字核查说明；不要包含凭证、私有路径或原始日志" aria-label="核查说明"
        />
        <p v-if="store.pending">
          请求已冻结；结果不确定时先刷新核查，只能重试同一内容。
        </p>
        <p v-if="localError" role="alert">
          {{ localError }}
        </p>
        <NSpace>
          <NButton :disabled="busy || (!store.pending && !valid)" :loading="busy" @click="submit">
            {{ store.pending ? '重试同一核查请求' : title }}
          </NButton>
          <NButton v-if="!store.pending" :disabled="busy" @click="decision = ''">
            取消
          </NButton>
        </NSpace>
      </div>
    </NAlert>
    <!-- 审查历史始终可见，说明只按纯文本展示 -->
    <div v-if="task.reviews?.length" class="plugin-review__history">
      <div>人工核查记录</div>
      <article v-for="review in task.reviews" :key="review.id">
        <DictTag :options="dict.sys_plugin_review_decision" :value="review.decision" />
        <span> 管理员 #{{ review.reviewedBy }} · {{ pluginTime(review.reviewedTime) }}</span>
        <p>{{ review.note }}</p>
        <small>执行器停止、尚未部署：管理员确认；非平台独立验证。</small>
      </article>
    </div>
  </section>
</template>

<script setup>
import { NAlert, NButton, NCheckbox, NInput, NSpace } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { usePluginReviewStore } from '@/stores/plugin/reviewStore'
import { pluginTime } from '../pluginTaskUtils'
import { usePluginPermission } from '../usePluginPermission'

const props = defineProps({ task: { type: Object, required: true }, busy: Boolean })
const emit = defineEmits(['review'])
const store = usePluginReviewStore()
const permission = usePluginPermission()
const { dict } = useDict('sys_plugin_review_decision')
const canReview = computed(() => permission('system:plugin:review'))
const canApprove = computed(() => props.task.status === 'built' && props.task.execution?.result?.success)
const canClose = computed(() => ['built', 'build_failed', 'release_ready'].includes(props.task.status)
  || (props.task.status === 'building' && props.task.execution?.leaseExpired))
const decision = ref('')
const localError = ref('')
function initial() {
  return {
    executorStopped: false,
    notDeployed: false,
    artifactsReviewed: false,
    migrationsReviewed: false,
    note: '',
  }
}
const form = reactive(initial())
const locked = computed(() => props.busy || !!store.pending)
const title = computed(() => decision.value === 'approve_build' ? '确认审查通过（待部署）' : '确认关闭并释放占用')
const valid = computed(() => form.executorStopped && form.notDeployed && form.note.trim().length >= 10
  && (decision.value !== 'approve_build' || (form.artifactsReviewed && form.migrationsReviewed)))
watch(() => [props.task.id, props.task.revision], () => {
  decision.value = store.pending?.taskId === props.task.id ? store.pending.decision : ''
  Object.assign(form, initial(), store.pending?.taskId === props.task.id ? store.pending : {})
  localError.value = ''
}, { immediate: true })
function start(value) {
  if (store.pending) {
    decision.value = store.pending.taskId === props.task.id ? store.pending.decision : ''
    localError.value = store.pending.taskId !== props.task.id ? '请先返回上一任务核查未确认的请求' : ''
    return
  }
  decision.value = value
  Object.assign(form, initial())
}
function submit() {
  if (props.busy || !canReview.value || (!store.pending && !valid.value))
    return
  try {
    store.prepare(props.task, decision.value, form)
    emit('review')
  }
  catch (error) { localError.value = error.message }
}
</script>

<style scoped>
.plugin-review,
.plugin-review__form,
.plugin-review__history {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.plugin-review__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.plugin-review p {
  margin: 0;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
.plugin-review__form {
  margin-top: 12px;
}
.plugin-review__history article {
  border-top: 1px solid var(--border-light);
  padding-top: 8px;
}
.plugin-review__history span,
.plugin-review__history small {
  color: var(--text-tertiary);
  font-size: 12px;
}
</style>

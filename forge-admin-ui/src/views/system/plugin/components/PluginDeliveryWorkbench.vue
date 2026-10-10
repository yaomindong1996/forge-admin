<template>
  <section class="delivery-workbench">
    <!-- 运维交付：远程制品发布 / 目标部署，不是本机插件浏览 -->
    <header>
      <div>
        <strong>运维交付工作台</strong>
        <p>锁定已审核制品 → 发布到目标 → 部署核验。不替代「已加载插件」清单。</p>
      </div>
      <NButton :loading="store.loading" @click="store.load">
        <template #icon>
          <i class="i-lucide:refresh-cw" />
        </template>
        刷新状态
      </NButton>
    </header>
    <NAlert v-if="store.error" type="error" :bordered="false">
      {{ store.error }}
    </NAlert>
    <!-- 目标固定配置与确认操作 -->
    <PluginDeliveryForm v-if="permission('system:plugin:delivery:execute')" />
    <!-- 最近100条真实执行记录 -->
    <NDataTable
      :columns="columns" :data="store.tasks" :loading="store.loading"
      :row-key="row => row.id" :scroll-x="980" :bordered="false"
    />
    <p class="hint">
      最近100条记录；“结果待核查”保持目标占用，不能自动重试或视作部署成功。
    </p>
    <!-- 人工关闭只释放占用，绝不填写虚假运行版本 -->
    <NModal
      v-model:show="visible" preset="card" title="人工核查并关闭占用" class="reconcile-modal"
      :style="{ width: 'min(520px, 94vw)' }" :mask-closable="!store.busy"
    >
      <NAlert type="warning" :bordered="false">
        仅解除占用，不标记成功、不更新当前版本、不回滚数据库。先核查容器和执行器；实际状态不一致须人工恢复。
      </NAlert>
      <NCheckbox v-model:checked="check.executorStopped">
        已确认执行器和任务进程停止
      </NCheckbox>
      <NInput v-model:value="check.note" type="textarea" :maxlength="1000" placeholder="至少10字人工核查结果" />
      <template #footer>
        <NSpace justify="end">
          <NButton :disabled="store.busy" @click="visible = false">
            取消
          </NButton>
          <NButton
            type="warning" :loading="store.busy" :disabled="!check.executorStopped || check.note.trim().length < 10"
            @click="closeTask"
          >
            确认关闭
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </section>
</template>

<script setup>
import { NAlert, NButton, NCheckbox, NDataTable, NInput, NModal, NSpace } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { usePluginDeliveryStore } from '@/stores/plugin/deliveryStore'
import { pluginTime } from '../pluginTaskUtils'
import { usePluginPermission } from '../usePluginPermission'
import PluginDeliveryForm from './PluginDeliveryForm.vue'

const store = usePluginDeliveryStore()
const permission = usePluginPermission()
const { dict } = useDict('sys_plugin_delivery_action', 'sys_plugin_delivery_status')
const visible = ref(false)
const check = reactive({ id: '', executorStopped: false, note: '' })
const columns = computed(() => [
  { type: 'expand', renderExpand: row => h('div', [
    h('p', `确认依据：${row.note || '—'}`),
    h('p', `备份凭据：${row.backupReference || '无'}`),
    h('p', `人工核查：${row.reconcileNote || '无'} · ${pluginTime(row.reconciledTime)}`),
  ]) },
  { title: '目标 / 任务', key: 'targetId', width: 160, render: row => h('div', [
    h('div', row.targetId),
    h('small', { class: 'hint' }, row.id),
  ]) },
  { title: '锁定制品', key: 'releaseId', minWidth: 220, ellipsis: { tooltip: true } },
  { title: '动作', key: 'action', width: 120, render: row =>
    h(DictTag, { value: row.action, options: dict.value.sys_plugin_delivery_action }) },
  { title: '执行状态', key: 'status', width: 130, render: row =>
    h(DictTag, { value: row.status, options: dict.value.sys_plugin_delivery_status }) },
  { title: '核验证据', key: 'evidence', width: 140, render: row =>
    `COS：${row.cosVerified ? '已读回' : '未核验'} / 运行：${row.runtimeVerified ? '已核验' : '未核验'}` },
  { title: '时间', key: 'createTime', width: 170, render: row => pluginTime(row.createTime) },
  { title: '错误码 / 操作', key: 'failureCode', minWidth: 170, render: row => h('div', [
    h('span', row.failureCode || '—'),
    ...(permission('system:plugin:delivery:reconcile') && ['queued', 'uncertain'].includes(row.status)
      ? [h(NButton, { text: true, type: 'warning', onClick: () => open(row) }, () => '人工核查')]
      : []),
  ]) },
])
function open(row) {
  Object.assign(check, { id: row.id, executorStopped: false, note: '' })
  visible.value = true
}
async function closeTask() {
  if (await store.reconcile(check.id, check))
    visible.value = false
}
onMounted(() => {
  if (permission('system:plugin:delivery:list'))
    store.load()
})
</script>

<style scoped>
.delivery-workbench {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
header strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
}
header p {
  margin: 4px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}
.hint {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.reconcile-modal :deep(.n-checkbox) {
  margin: 12px 0;
}
</style>

<template>
  <n-modal
    v-model:show="modalShow"
    preset="dialog"
    title="迁移业务领域"
    positive-text="确认迁移"
    negative-text="取消"
    :loading="saving"
    @positive-click="move"
  >
    <div class="move-body">
      <div class="app-summary">
        <strong>{{ app?.appName || app?.tableComment || app?.configKey || '-' }}</strong>
        <span>{{ app?.configKey || '-' }} · {{ app?.tableName || '-' }}</span>
      </div>
      <n-form label-placement="left" label-width="96" size="small">
        <n-form-item label="目标领域">
          <n-select
            v-model:value="form.domainId"
            filterable
            :options="domainOptions"
            placeholder="请选择启用状态的业务领域"
          />
        </n-form-item>
        <n-form-item label="对象编码">
          <n-input v-model:value="form.objectCode" placeholder="例如 contract_archive" />
        </n-form-item>
        <n-form-item label="对象名称">
          <n-input v-model:value="form.objectName" placeholder="例如 合同档案" />
        </n-form-item>
      </n-form>
    </div>
  </n-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { lowcodeMoveDomain } from '@/api/lowcode-crud'

defineOptions({ name: 'MoveDomainModal' })

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
    type: Object,
    default: null,
  },
  domains: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:show', 'moved'])

const saving = ref(false)
const form = reactive({
  domainId: null,
  objectCode: '',
  objectName: '',
})

const modalShow = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})

const domainOptions = computed(() => flattenDomains(props.domains)
  .filter(item => item.status === 'ENABLED')
  .map(item => ({
    label: `${'  '.repeat(item.level)}${item.domainName} (${item.domainCode})`,
    value: item.id,
  })))

watch(
  () => props.show,
  (visible) => {
    if (!visible)
      return
    form.domainId = props.app?.domainId || null
    form.objectCode = props.app?.objectCode || props.app?.configKey || ''
    form.objectName = props.app?.objectName || props.app?.appName || props.app?.tableComment || ''
  },
)

async function move() {
  if (!props.app?.id || !form.domainId) {
    window.$message?.warning('请选择目标领域')
    return false
  }
  saving.value = true
  try {
    await lowcodeMoveDomain(props.app.id, {
      domainId: form.domainId,
      objectCode: form.objectCode,
      objectName: form.objectName,
    })
    window.$message?.success('应用领域已迁移')
    emit('moved')
    return true
  }
  catch (e) {
    window.$message?.error(e?.message || '迁移失败')
    return false
  }
  finally {
    saving.value = false
  }
}

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}
</script>

<style scoped>
.move-body {
  display: grid;
  gap: 14px;
}

.app-summary {
  display: grid;
  gap: 4px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 12px;
}

.app-summary strong {
  color: var(--text-primary);
  font-size: 14px;
}

.app-summary span {
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
</style>

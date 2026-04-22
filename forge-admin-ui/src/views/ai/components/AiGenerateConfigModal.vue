<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="AI 生成 CRUD 配置"
    style="width: 700px"
    :mask-closable="false"
  >
    <n-spin :show="generating">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="100">
        <n-form-item label="生成方式" path="mode">
          <n-radio-group v-model:value="formData.mode">
            <n-radio value="description">自然语言描述</n-radio>
            <n-radio value="table">已有数据表</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item label="configKey" path="configKey">
          <n-input v-model:value="formData.configKey" placeholder="小写字母开头，仅含小写字母+数字+下划线" />
        </n-form-item>

        <n-form-item v-if="formData.mode === 'description'" label="功能描述" path="description">
          <n-input
            v-model:value="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请描述需要生成的CRUD页面功能，如：员工管理，包含姓名、部门、职位、入职日期等字段"
          />
        </n-form-item>

        <n-form-item v-if="formData.mode === 'table'" label="数据表名" path="tableName">
          <n-input v-model:value="formData.tableName" placeholder="请输入数据库表名，如 sys_user" />
        </n-form-item>
      </n-form>
    </n-spin>

    <template #action>
      <n-space>
        <n-button @click="handleClose">取消</n-button>
        <n-button type="primary" :loading="generating" @click="handleGenerate">
          AI 生成
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { ref, watch } from 'vue'
import { crudConfigAiGenerate, crudConfigAiGenerateFromTable } from '@/api/ai'
import { useDiscreteMessage } from '@/composables/useDiscreteMessage'

const props = defineProps({
  show: { type: Boolean, default: false },
})
const emit = defineEmits(['update:show', 'success'])

const showModal = ref(false)
const generating = ref(false)
const { success, error, warning } = useDiscreteMessage()

const formData = ref({
  mode: 'description',
  configKey: '',
  description: '',
  tableName: '',
})

watch(() => props.show, (val) => {
  showModal.value = val
})

watch(showModal, (val) => {
  emit('update:show', val)
})

async function handleGenerate() {
  if (!formData.value.configKey) {
    warning('请输入 configKey')
    return
  }
  if (!formData.value.configKey.match(/^[a-z][a-z0-9_]{1,63}$/)) {
    warning('configKey格式不正确')
    return
  }

  generating.value = true
  try {
    let res
    if (formData.value.mode === 'description') {
      if (!formData.value.description) {
        warning('请输入功能描述')
        return
      }
      res = await crudConfigAiGenerate({
        description: formData.value.description,
        configKey: formData.value.configKey,
      })
    } else {
      if (!formData.value.tableName) {
        warning('请输入数据表名')
        return
      }
      res = await crudConfigAiGenerateFromTable({
        tableName: formData.value.tableName,
        configKey: formData.value.configKey,
      })
    }

    if (res.data?.success) {
      emit('success', res.data)
      handleClose()
      success('AI 生成配置成功')
    } else {
      error(res.data?.errorMessage || 'AI 生成失败')
    }
  } catch (e) {
    error(e.message || 'AI 生成异常')
  } finally {
    generating.value = false
  }
}

function handleClose() {
  showModal.value = false
  formData.value = { mode: 'description', configKey: '', description: '', tableName: '' }
}
</script>

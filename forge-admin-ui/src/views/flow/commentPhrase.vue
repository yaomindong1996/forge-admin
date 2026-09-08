<template>
  <div class="flow-comment-phrase-page">
    <AiCrudPage
      ref="crudRef"
      api="/api/flow/comment-phrases"
      :api-config="{
        list: 'get@/api/flow/comment-phrases/page',
        detail: 'get@/api/flow/comment-phrases/:id',
        add: 'post@/api/flow/comment-phrases',
        update: 'put@/api/flow/comment-phrases',
        delete: 'delete@/api/flow/comment-phrases/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      edit-label-placement="left"
      edit-label-align="left"
      edit-label-width="92px"
      modal-width="640px"
      add-button-text="新增常用意见"
      :load-detail-on-edit="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :search-grid-cols="4"
      :search-max-visible-fields="4"
      :search-y-gap="8"
      search-label-width="72px"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'FlowCommentPhrase' })

const crudRef = ref(null)
const { dict } = useDict('flow_comment_phrase_scene', 'flow_comment_phrase_owner_type', 'sys_enable_disable')
const sceneOptions = computed(() => dict.value.flow_comment_phrase_scene || [])
const ownerTypeOptions = computed(() => toNumberDictOptions(dict.value.flow_comment_phrase_owner_type))
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

const searchSchema = computed(() => [
  {
    field: 'keyword',
    label: '意见',
    type: 'input',
    props: {
      placeholder: '意见内容',
      clearable: true,
    },
  },
  {
    field: 'ownerType',
    label: '范围',
    type: 'select',
    defaultValue: 0,
    props: {
      placeholder: '全部范围',
      options: ownerTypeOptions.value,
      clearable: true,
    },
  },
  {
    field: 'scene',
    label: '场景',
    type: 'select',
    props: {
      placeholder: '全部场景',
      options: sceneOptions.value,
      clearable: true,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '全部状态',
      options: statusOptions.value,
      clearable: true,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'content',
    label: '审批意见',
    minWidth: 280,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'scene',
    label: '场景',
    width: 100,
    render: row => h(DictTag, {
      dictType: 'flow_comment_phrase_scene',
      value: row.scene,
      forceTag: true,
    }),
  },
  {
    prop: 'ownerType',
    label: '范围',
    width: 110,
    render: row => h(DictTag, {
      options: ownerTypeOptions.value,
      value: row.ownerType,
      forceTag: true,
    }),
  },
  {
    prop: 'sortOrder',
    label: '排序',
    width: 72,
  },
  {
    prop: 'status',
    label: '状态',
    width: 88,
    render: row => h(DictTag, {
      options: statusOptions.value,
      value: row.status,
      forceTag: true,
    }),
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 168,
  },
])

const editSchema = computed(() => [
  {
    field: 'content',
    label: '审批意见',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入审批意见', trigger: 'blur' }],
    props: {
      rows: 3,
      maxlength: 200,
      showCount: true,
      placeholder: '审批时可一键点选该意见',
    },
  },
  {
    field: 'scene',
    label: '适用场景',
    type: 'select',
    defaultValue: 'ALL',
    rules: [{ required: true, message: '请选择适用场景', trigger: 'change' }],
    props: {
      options: sceneOptions.value,
    },
  },
  {
    field: 'ownerType',
    label: '范围',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: ownerTypeOptions.value,
    },
  },
  {
    field: 'sortOrder',
    label: '排序',
    type: 'inputNumber',
    defaultValue: 0,
    props: {
      min: 0,
      max: 999,
      placeholder: '越小越靠前',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: statusOptions.value,
    },
  },
])

function beforeSubmit(formData) {
  const payload = {
    id: formData.id,
    content: typeof formData.content === 'string' ? formData.content.trim() : formData.content,
    scene: formData.scene,
    ownerType: formData.ownerType === null || formData.ownerType === undefined || formData.ownerType === ''
      ? 0
      : Number(formData.ownerType),
    sortOrder: formData.sortOrder === null || formData.sortOrder === undefined || formData.sortOrder === ''
      ? 0
      : Number(formData.sortOrder),
    status: formData.status === null || formData.status === undefined || formData.status === ''
      ? 1
      : Number(formData.status),
  }
  if (payload.id) {
    delete payload.ownerType
  }
  else {
    delete payload.id
  }
  return payload
}
</script>

<style scoped>
.flow-comment-phrase-page {
  height: 100%;
  min-height: 0;
}
</style>

<template>
  <div class="flow-condition-rule-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/api/flow/conditionRule/page',
      }"
      list-data-field="list"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :before-load-list="beforeLoadList"
      row-key="id"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
      :search-grid-cols="3"
      :search-max-visible-fields="3"
      :search-y-gap="8"
      search-label-width="72px"
      :scroll-x="1100"
    >
      <template #toolbar-end>
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-mdi:plus" />
          </template>
          新增规则
        </NButton>
      </template>
    </AiCrudPage>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" :style="{ width: '800px' }">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="规则名称" path="ruleName">
          <n-input v-model:value="formData.ruleName" placeholder="请输入规则名称" />
        </n-form-item>
        <n-form-item label="规则编码" path="ruleCode">
          <n-input v-model:value="formData.ruleCode" placeholder="请输入规则编码" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="规则类型" path="ruleType">
          <n-select v-model:value="formData.ruleType" :options="ruleTypeOptions" placeholder="请选择规则类型" />
        </n-form-item>
        <n-form-item label="关联流程" path="modelId">
          <n-select
            v-model:value="formData.modelId"
            :options="modelOptions"
            placeholder="请选择关联流程"
            filterable
            clearable
          />
        </n-form-item>
        <n-form-item label="关联节点" path="nodeId">
          <n-input v-model:value="formData.nodeId" placeholder="请输入关联节点ID（可选）" />
        </n-form-item>
        <n-form-item label="条件表达式" path="conditionExpression">
          <n-input
            v-model:value="formData.conditionExpression"
            type="textarea"
            :rows="3"
            placeholder="请输入SpEL条件表达式，如：${amount > 10000 && deptLevel == 'A'}"
          />
        </n-form-item>
        <n-form-item label="条件项">
          <div class="condition-items">
            <div v-for="(item, index) in formData.conditionItems" :key="index" class="condition-item">
              <n-select
                v-model:value="item.fieldName"
                :options="fieldOptions"
                placeholder="字段"
                style="width: 150px"
              />
              <n-select
                v-model:value="item.operator"
                :options="operatorOptions"
                placeholder="操作符"
                style="width: 100px"
              />
              <n-input v-model:value="item.fieldValue" placeholder="值" style="width: 150px" />
              <NButton text type="error" @click="removeConditionItem(index)">
                <i class="i-mdi:delete" />
              </NButton>
            </div>
            <NButton dashed block @click="addConditionItem">
              <template #icon>
                <i class="i-mdi:plus" />
              </template>
              添加条件项
            </NButton>
          </div>
        </n-form-item>
        <n-form-item label="优先级" path="priority">
          <n-input-number v-model:value="formData.priority" :min="0" :max="999" placeholder="数值越小优先级越高" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>
              启用
            </template>
            <template #unchecked>
              禁用
            </template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </n-form-item>
      </n-form>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="modalVisible = false">
            取消
          </NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </NButton>
        </NSpace>
      </template>
    </n-modal>

    <!-- 测试规则弹窗 -->
    <n-modal v-model:show="testModalVisible" preset="card" title="测试规则" :style="{ width: '600px' }">
      <n-form :model="testForm" label-placement="left" label-width="80">
        <n-form-item label="测试数据">
          <n-input
            v-model:value="testForm.testData"
            type="textarea"
            :rows="6"
            placeholder="请输入JSON格式的测试数据"
          />
        </n-form-item>
      </n-form>

      <n-divider>测试结果</n-divider>

      <n-alert v-if="testResult !== null" :type="testResult ? 'success' : 'warning'">
        {{ testResult ? '条件满足，规则匹配成功' : '条件不满足，规则匹配失败' }}
      </n-alert>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="testModalVisible = false">
            关闭
          </NButton>
          <NButton type="primary" :loading="testLoading" @click="handleTest">
            执行测试
          </NButton>
        </NSpace>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NSpace } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'FlowConditionRule' })

const message = window.$message
const { dict } = useDict('flow_rule_type', 'flow_rule_operator', 'sys_enable_disable')
const crudRef = ref(null)

const ruleTypeOptions = computed(() => dict.value.flow_rule_type || [])
const statusOptions = computed(() => toNumberOptions(dict.value.sys_enable_disable))
const operatorOptions = computed(() => dict.value.flow_rule_operator || [])

const fieldOptions = ref([
  { label: '金额', value: 'amount' },
  { label: '部门级别', value: 'deptLevel' },
  { label: '请假天数', value: 'leaveDays' },
  { label: '处理状态', value: 'approvalStatus' },
])

const modelOptions = ref([])

const searchSchema = computed(() => [
  {
    field: 'ruleName',
    label: '规则名称',
    type: 'input',
    props: { placeholder: '请输入规则名称', clearable: true },
  },
  {
    field: 'ruleType',
    label: '规则类型',
    type: 'select',
    props: { placeholder: '请选择规则类型', options: ruleTypeOptions.value, clearable: true },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择状态', options: statusOptions.value, clearable: true },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'ruleName',
    label: '规则名称',
    minWidth: 180,
    render: row => h(SystemTableCell, {
      title: row.ruleName || '-',
      subtitle: row.ruleCode || '',
      interactive: true,
      tooltip: `编辑规则：${row.ruleName || row.ruleCode || '-'}`,
      onActivate: () => handleEdit(row),
    }),
  },
  {
    prop: 'ruleType',
    label: '规则类型',
    width: 110,
    render: row => h(DictTag, { dictType: 'flow_rule_type', value: row.ruleType }),
  },
  {
    prop: 'priority',
    label: '优先级',
    width: 90,
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render: row => h(DictTag, { dictType: 'sys_enable_disable', value: row.status }),
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 170,
  },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '测试', key: 'test', type: 'info', onClick: handleTestRule },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

function beforeLoadList(params = {}) {
  const next = { ...params }
  if (next.pageNum != null) {
    next.page = next.pageNum
    delete next.pageNum
  }
  return next
}

function refreshList() {
  crudRef.value?.refresh?.()
}

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('新增规则')
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

const formData = reactive({
  id: null,
  ruleName: '',
  ruleCode: '',
  ruleType: null,
  modelId: null,
  nodeId: '',
  conditionExpression: '',
  conditionItems: [],
  priority: 100,
  status: 1,
  remark: '',
})

const formRules = {
  ruleName: { required: true, message: '请输入规则名称', trigger: 'blur' },
  ruleCode: { required: true, message: '请输入规则编码', trigger: 'blur' },
  ruleType: { required: true, message: '请选择规则类型', trigger: 'change' },
}

// 测试弹窗
const testModalVisible = ref(false)
const testLoading = ref(false)
const testForm = reactive({
  testData: '',
})
const testResult = ref(null)
const currentTestRule = ref(null)

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

// 加载流程模型列表
async function loadModels() {
  try {
    const res = await request.get('/api/flow/model/list')
    if (res.code === 200) {
      modelOptions.value = (res.data || []).map(item => ({
        label: item.modelName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('加载流程模型失败', error)
  }
}

// 新增
function handleAdd() {
  modalTitle.value = '新增规则'
  isEdit.value = false
  Object.assign(formData, {
    id: null,
    ruleName: '',
    ruleCode: '',
    ruleType: null,
    modelId: null,
    nodeId: '',
    conditionExpression: '',
    conditionItems: [],
    priority: 100,
    status: 1,
    remark: '',
  })
  modalVisible.value = true
}

// 编辑
function handleEdit(row) {
  modalTitle.value = '编辑规则'
  isEdit.value = true
  Object.assign(formData, {
    ...row,
    conditionItems: row.conditionItems || [],
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(row) {
  try {
    const res = await request.delete(`/api/flow/conditionRule/${row.id}`)
    if (res.code === 200) {
      message.success('删除成功')
      refreshList()
    }
  }
  catch {
    message.error('删除失败')
  }
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }

  submitLoading.value = true
  try {
    const url = isEdit.value ? '/api/flow/conditionRule' : '/api/flow/conditionRule'
    const method = isEdit.value ? 'put' : 'post'
    const res = await request[method](url, formData)
    if (res.code === 200) {
      message.success(isEdit.value ? '修改成功' : '新增成功')
      modalVisible.value = false
      refreshList()
    }
  }
  catch {
    message.error('操作失败')
  }
  finally {
    submitLoading.value = false
  }
}

// 添加条件项
function addConditionItem() {
  formData.conditionItems.push({
    fieldName: '',
    operator: '',
    fieldValue: '',
  })
}

// 移除条件项
function removeConditionItem(index) {
  formData.conditionItems.splice(index, 1)
}

// 测试规则
function handleTestRule(row) {
  currentTestRule.value = row
  testForm.testData = ''
  testResult.value = null
  testModalVisible.value = true
}

// 执行测试
async function handleTest() {
  if (!testForm.testData) {
    message.warning('请输入测试数据')
    return
  }

  testLoading.value = true
  try {
    const variables = JSON.parse(testForm.testData)
    const res = await request.post('/api/flow/conditionRule/test', {
      ruleId: currentTestRule.value.id,
      variables,
    })
    if (res.code === 200) {
      testResult.value = res.data
    }
  }
  catch {
    message.error('测试失败，请检查数据格式')
  }
  finally {
    testLoading.value = false
  }
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.flow-condition-rule-page {
  height: 100%;
}

.flow-condition-rule-page :deep(.ai-crud-page) {
  height: 100%;
}

.condition-items {
  width: 100%;
}

.condition-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
</style>

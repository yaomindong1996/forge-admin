<template>
  <div v-if="isTaskFormMode" class="purchase-task-form">
    <n-spin :show="detailLoading">
      <section class="task-form-section">
        <div class="section-title">
          <span>采购单信息</span>
          <DictTag v-if="detail.status" dict-type="sample_purchase_order_status" :value="detail.status" size="small" />
        </div>
        <n-descriptions :column="2" bordered label-placement="left" size="small">
          <n-descriptions-item v-if="showDetailField('orderNo')" :label="detailFieldLabel('orderNo', '采购单号')">
            {{ detail.orderNo || '-' }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('title')" :label="detailFieldLabel('title', '采购主题')">
            {{ detail.title || '-' }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('supplierName')" :label="detailFieldLabel('supplierName', '供应商')">
            {{ detail.supplierName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('amountCent')" :label="detailFieldLabel('amountCent', '采购金额')">
            {{ formatMoney(detail.amountCent) }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('applicantName')" :label="detailFieldLabel('applicantName', '申请人')">
            {{ detail.applicantName || '-' }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('needDate')" :label="detailFieldLabel('needDate', '需求日期')">
            {{ detail.needDate || '-' }}
          </n-descriptions-item>
          <n-descriptions-item v-if="showDetailField('purchaseItems')" :label="detailFieldLabel('purchaseItems', '采购明细')" :span="2">
            <span class="preserve-text">{{ detail.purchaseItems || '-' }}</span>
          </n-descriptions-item>
          <n-descriptions-item v-if="detail.rejectReason && showDetailField('rejectReason')" :label="detailFieldLabel('rejectReason', '最近驳回原因')" :span="2">
            <span class="text-error">{{ detail.rejectReason }}</span>
          </n-descriptions-item>
        </n-descriptions>
      </section>

      <section class="task-form-section">
        <div class="section-title">
          <span>{{ currentNodeTitle }}</span>
          <n-tag size="small" :type="isModifyTask ? 'warning' : 'info'">
            {{ currentNodeDisplayName }}
          </n-tag>
        </div>

        <n-form
          ref="taskFormRef"
          :model="taskForm"
          :rules="taskFormRules"
          label-placement="top"
          require-mark-placement="right-hanging"
        >
          <n-grid :cols="2" :x-gap="16" :y-gap="4" responsive="screen">
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('title')" label="采购主题" path="title">
              <n-input v-model:value="taskForm.title" :disabled="isTaskFieldDisabled('title')" maxlength="128" show-count />
            </n-form-item-gi>
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('supplierName')" label="供应商" path="supplierName">
              <n-input v-model:value="taskForm.supplierName" :disabled="isTaskFieldDisabled('supplierName')" maxlength="128" show-count />
            </n-form-item-gi>
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('amountCent')" label="采购金额" path="amountYuan">
              <n-input-number
                v-model:value="taskForm.amountYuan"
                :disabled="isTaskFieldDisabled('amountCent')"
                :min="0.01"
                :precision="2"
                :step="100"
                style="width: 100%"
              >
                <template #suffix>
                  元
                </template>
              </n-input-number>
            </n-form-item-gi>
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('needDate')" label="期望到货日期" path="needDate">
              <n-date-picker
                v-model:formatted-value="taskForm.needDate"
                :disabled="isTaskFieldDisabled('needDate')"
                type="date"
                value-format="yyyy-MM-dd"
                clearable
                style="width: 100%"
              />
            </n-form-item-gi>
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('purchaseItems')" label="采购明细" path="purchaseItems" :span="2">
              <n-input
                v-model:value="taskForm.purchaseItems"
                :disabled="isTaskFieldDisabled('purchaseItems')"
                type="textarea"
                :rows="4"
                maxlength="1000"
                show-count
              />
            </n-form-item-gi>
            <n-form-item-gi v-if="isModifyTask && canShowTaskField('applicantModifyRemark')" label="修改说明" path="applicantModifyRemark" :span="2">
              <n-input
                v-model:value="taskForm.applicantModifyRemark"
                :disabled="isTaskFieldDisabled('applicantModifyRemark')"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-count
              />
            </n-form-item-gi>

            <n-form-item-gi v-if="isDeptLeaderTask && canShowTaskField('arrivalListFileIds')" label="负责人上传清单" path="arrivalListFileIds" :span="2">
              <FileUpload
                v-model="taskForm.arrivalListFileIds"
                business-type="sample_purchase_order"
                :business-id="String(detail.id || '')"
                :limit="5"
                :file-size="20"
                :disabled="isTaskFieldDisabled('arrivalListFileIds')"
                upload-button-text="上传清单"
              />
            </n-form-item-gi>
            <n-form-item-gi v-if="isDeptLeaderTask && canShowTaskField('deptLeaderRemark')" label="部门负责人补充意见" path="deptLeaderRemark" :span="2">
              <n-input
                v-model:value="taskForm.deptLeaderRemark"
                :disabled="isTaskFieldDisabled('deptLeaderRemark')"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-count
              />
            </n-form-item-gi>

            <n-form-item-gi v-if="isEngineeringTask && canShowTaskField('engineeringManagerRemark')" label="工程部经理意见" path="engineeringManagerRemark" :span="2">
              <n-input
                v-model:value="taskForm.engineeringManagerRemark"
                :disabled="isTaskFieldDisabled('engineeringManagerRemark')"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-count
              />
            </n-form-item-gi>

            <n-form-item-gi v-if="isCountersignTask && canShowTaskField('countersignRemark')" label="会签意见" path="countersignRemark" :span="2">
              <n-input
                v-model:value="taskForm.countersignRemark"
                :disabled="isTaskFieldDisabled('countersignRemark')"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-count
              />
            </n-form-item-gi>
          </n-grid>

          <n-alert v-if="!writableNodeFields.length" type="info" :show-icon="false">
            当前节点只查看采购单，不开放业务字段编辑。
          </n-alert>

          <n-form-item label="审批意见" path="comment" class="approval-comment">
            <FlowCommentPhraseInput
              v-model="taskForm.comment"
              :disabled="taskFormDisabled"
              :rows="3"
              :maxlength="500"
              :placeholder="requireComment ? '请输入审批意见' : '请输入审批意见（可选）'"
            />
          </n-form-item>

          <div v-if="!taskRuntime.readOnly" class="task-actions">
            <slot name="actions" />
            <n-button
              v-if="canApprove"
              type="primary"
              size="large"
              :loading="isActionSubmitting('approve')"
              :disabled="taskFormDisabled"
              @click="submitTask('approve')"
            >
              {{ isModifyTask ? '保存并重提' : '同意' }}
            </n-button>
            <n-button
              v-if="canReject"
              type="error"
              secondary
              size="large"
              :loading="isActionSubmitting('reject')"
              :disabled="taskFormDisabled"
              @click="submitTask('reject')"
            >
              {{ isModifyTask ? '终止申请' : '驳回修改' }}
            </n-button>
          </div>
        </n-form>
      </section>
    </n-spin>
  </div>

  <div v-else class="purchase-order-page">
    <div class="page-toolbar">
      <div>
        <h2>采购单审批测试</h2>
        <p>用于验证代码业务接入流程：串行审批、会签、驳回修改、完成抄送。</p>
      </div>
      <n-space>
        <n-button secondary :loading="initLoading" @click="handleInitFlow">
          <template #icon>
            <i class="i-material-symbols:account-tree-rounded" />
          </template>
          初始化测试流程
        </n-button>
        <n-button secondary @click="openFlowConfig">
          <template #icon>
            <i class="i-material-symbols:tune-rounded" />
          </template>
          业务流程配置
        </n-button>
        <n-button type="primary" @click="openCreate">
          <template #icon>
            <i class="i-material-symbols:add-rounded" />
          </template>
          新建采购单
        </n-button>
      </n-space>
    </div>

    <n-card class="query-card" :bordered="false">
      <n-form :model="query" label-placement="left" label-width="78">
        <n-grid :cols="4" :x-gap="16" :y-gap="12" responsive="screen">
          <n-form-item-gi label="采购单号">
            <n-input v-model:value="query.orderNo" clearable placeholder="请输入采购单号" />
          </n-form-item-gi>
          <n-form-item-gi label="采购主题">
            <n-input v-model:value="query.title" clearable placeholder="请输入采购主题" />
          </n-form-item-gi>
          <n-form-item-gi label="供应商">
            <n-input v-model:value="query.supplierName" clearable placeholder="请输入供应商" />
          </n-form-item-gi>
          <n-form-item-gi label="状态">
            <n-select v-model:value="query.status" :options="statusOptions" clearable placeholder="全部状态" />
          </n-form-item-gi>
        </n-grid>
        <div class="query-actions">
          <n-space>
            <n-button type="primary" @click="handleSearch">
              查询
            </n-button>
            <n-button secondary @click="handleReset">
              重置
            </n-button>
          </n-space>
        </div>
      </n-form>
    </n-card>

    <n-card :bordered="false" class="table-card">
      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="row => row.id"
        :pagination="false"
        remote
      />
      <div class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          @update:page="loadData"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <n-modal
      v-model:show="formModalVisible"
      preset="card"
      :title="formModalTitle"
      style="width: min(760px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <n-form ref="formRef" :model="formState" :rules="formRules" label-placement="top">
        <n-grid :cols="2" :x-gap="16" responsive="screen">
          <n-form-item-gi v-if="showFormModalField('title')" :label="formFieldLabel('title', '采购主题')" path="title">
            <n-input v-model:value="formState.title" :disabled="formReadonly" maxlength="128" show-count placeholder="请输入采购主题" />
          </n-form-item-gi>
          <n-form-item-gi v-if="showFormModalField('supplierName')" :label="formFieldLabel('supplierName', '供应商')" path="supplierName">
            <n-input v-model:value="formState.supplierName" :disabled="formReadonly" maxlength="128" show-count placeholder="请输入供应商" />
          </n-form-item-gi>
          <n-form-item-gi v-if="showFormModalField('amountCent')" :label="formFieldLabel('amountCent', '采购金额')" path="amountYuan">
            <n-input-number
              v-model:value="formState.amountYuan"
              :disabled="formReadonly"
              :min="0.01"
              :precision="2"
              :step="100"
              style="width: 100%"
            >
              <template #suffix>
                元
              </template>
            </n-input-number>
          </n-form-item-gi>
          <n-form-item-gi v-if="showFormModalField('needDate')" :label="formFieldLabel('needDate', '需求日期')" path="needDate">
            <n-date-picker
              v-model:formatted-value="formState.needDate"
              :disabled="formReadonly"
              type="date"
              value-format="yyyy-MM-dd"
              clearable
              style="width: 100%"
            />
          </n-form-item-gi>
          <n-form-item-gi v-if="showFormModalField('purchaseItems')" :label="formFieldLabel('purchaseItems', '采购明细')" path="purchaseItems" :span="2">
            <n-input
              v-model:value="formState.purchaseItems"
              :disabled="formReadonly"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-count
              placeholder="请输入采购内容、数量、用途等"
            />
          </n-form-item-gi>
          <n-form-item-gi v-if="showFormModalField('remark')" :label="formFieldLabel('remark', '备注')" path="remark" :span="2">
            <n-input v-model:value="formState.remark" :disabled="formReadonly" type="textarea" :rows="3" maxlength="500" show-count />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="formModalVisible = false">
            取消
          </n-button>
          <n-button v-if="!formReadonly" type="primary" :loading="saveLoading" @click="submitForm">
            保存
          </n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="submitModalVisible"
      preset="card"
      title="提交采购单审批"
      style="width: min(720px, calc(100vw - 32px))"
      :mask-closable="false"
    >
      <n-form ref="submitFormRef" :model="submitState" :rules="submitRules" label-placement="top">
        <n-grid :cols="2" :x-gap="16" responsive="screen">
          <n-form-item-gi label="部门负责人" path="deptLeaderId">
            <UserSelectPicker
              v-model="submitState.deptLeaderId"
              v-model:label-value="submitState.deptLeaderName"
              placeholder="选择部门负责人"
            />
          </n-form-item-gi>
          <n-form-item-gi label="工程部经理" path="engineeringManagerId">
            <UserSelectPicker
              v-model="submitState.engineeringManagerId"
              v-model:label-value="submitState.engineeringManagerName"
              placeholder="选择工程部经理"
            />
          </n-form-item-gi>
          <n-form-item-gi label="会签人员" path="countersignUserIds" :span="2">
            <UserSelectPicker
              v-model="submitState.countersignUserIds"
              v-model:label-value="submitState.countersignUserNames"
              multiple
              placeholder="至少选择两名会签人员"
            />
          </n-form-item-gi>
          <n-form-item-gi label="完成后抄送角色" path="ccRoleKeys" :span="2">
            <n-select
              v-model:value="submitState.ccRoleKeys"
              multiple
              tag
              :options="ccRoleOptions"
              placeholder="默认抄送 admin，可输入 general_manager"
            />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="submitModalVisible = false">
            取消
          </n-button>
          <n-button type="primary" :loading="submitLoading" @click="submitFlow">
            发起流程
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createPurchaseOrder,
  initPurchaseOrderFlow,
  purchaseOrderDetail,
  purchaseOrderPage,
  removePurchaseOrder,
  savePurchaseOrderTaskFields,
  submitPurchaseOrder,
  updatePurchaseOrder,
} from '@/api/business/purchase-order-test'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DictTag from '@/components/DictTag.vue'
import FileUpload from '@/components/file-upload/index.vue'
import FlowCommentPhraseInput from '@/components/flow/FlowCommentPhraseInput.vue'
import { useBusinessTaskFormContext } from '@/composables/useBusinessTaskFormContext'
import { useCodeAppMetadata } from '@/composables/useCodeAppMetadata'
import { useDict } from '@/composables/useDict'
import { getTaskDisplayName } from '../flow/utils/processDisplay'

defineOptions({ name: 'BusinessPurchaseOrderTest' })

const props = defineProps({
  taskId: { type: String, default: null },
  businessKey: { type: String, default: null },
  processInstanceId: { type: String, default: null },
  taskDefKey: { type: String, default: null },
  processDefKey: { type: String, default: null },
  variables: { type: Object, default: () => ({}) },
  approvalPolicy: { type: Object, default: () => ({}) },
  initialTaskContext: { type: Object, default: null },
  readOnly: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false },
  submittingAction: { type: String, default: '' },
})

const emit = defineEmits(['submit', 'cancel'])

const route = useRoute()
const router = useRouter()
const { dict } = useDict('sample_purchase_order_status')
const codeAppMetadata = useCodeAppMetadata('sample_purchase_order')
const statusOptions = computed(() => dict.value.sample_purchase_order_status || [])
const taskRuntime = computed(() => ({
  taskId: props.taskId || route.query.taskId || '',
  businessKey: props.businessKey || route.query.businessKey || '',
  processInstanceId: props.processInstanceId || route.query.processInstanceId || '',
  taskDefKey: props.taskDefKey || route.query.taskDefKey || '',
  processDefKey: props.processDefKey || route.query.processDefKey || '',
  readOnly: props.readOnly || route.query.readOnly === 'true',
}))
const isTaskFormMode = computed(() => Boolean(taskRuntime.value.taskId || taskRuntime.value.businessKey || taskRuntime.value.processInstanceId))
const taskRuntimeReadonly = computed(() => taskRuntime.value.readOnly)
const businessTaskForm = useBusinessTaskFormContext({}, { readonly: taskRuntimeReadonly })
const currentTaskDefKey = computed(() => taskRuntime.value.taskDefKey || businessTaskForm.context.value?.taskDefKey || '')

const query = reactive({
  orderNo: '',
  title: '',
  supplierName: '',
  status: null,
})
const rows = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })
const initLoading = ref(false)
const saveLoading = ref(false)
const submitLoading = ref(false)
const formModalVisible = ref(false)
const submitModalVisible = ref(false)
const formRef = ref(null)
const submitFormRef = ref(null)
const currentSubmitRow = ref(null)
const formReadonly = ref(false)

const formState = reactive(defaultForm())
const submitState = reactive(defaultSubmitForm())
const formModalTitle = computed(() => {
  if (formReadonly.value)
    return '采购单详情'
  return formState.id ? '编辑采购单' : '新建采购单'
})

const formRules = computed(() => {
  if (formReadonly.value)
    return {}
  const rules = {}
  if (showBusinessFormField('title'))
    rules.title = { required: true, message: '请输入采购主题', trigger: ['blur', 'input'] }
  if (showBusinessFormField('supplierName'))
    rules.supplierName = { required: true, message: '请输入供应商', trigger: ['blur', 'input'] }
  if (showBusinessFormField('amountCent')) {
    rules.amountYuan = {
      required: true,
      type: 'number',
      validator: (_, value) => Number(value) > 0,
      message: '采购金额必须大于0',
      trigger: ['blur', 'change'],
    }
  }
  return rules
})

const submitRules = {
  deptLeaderId: {
    required: true,
    validator: (_, value) => isFilledValue(value),
    message: '请选择部门负责人',
    trigger: ['change'],
  },
  engineeringManagerId: {
    required: true,
    validator: (_, value) => isFilledValue(value),
    message: '请选择工程部经理',
    trigger: ['change'],
  },
  countersignUserIds: {
    required: true,
    type: 'array',
    validator: (_, value) => Array.isArray(value) && value.filter(Boolean).length >= 2,
    message: '会签人员至少选择2人',
    trigger: ['change'],
  },
}

const ccRoleOptions = [
  { label: '超级管理员(admin)', value: 'admin' },
  { label: '总经理(general_manager)', value: 'general_manager' },
]

const defaultFormFields = ['title', 'supplierName', 'amountCent', 'needDate', 'purchaseItems', 'remark']
const defaultListFields = ['orderNo', 'title', 'supplierName', 'amountCent', 'status', 'applicantName', 'updateTime']
const defaultDetailFields = ['orderNo', 'title', 'supplierName', 'amountCent', 'applicantName', 'needDate', 'purchaseItems', 'rejectReason']
const columnBuilders = {
  orderNo: () => ({ title: listFieldLabel('orderNo', '采购单号'), key: 'orderNo', width: 190, ellipsis: { tooltip: true } }),
  title: () => ({ title: listFieldLabel('title', '采购主题'), key: 'title', minWidth: 180, ellipsis: { tooltip: true } }),
  supplierName: () => ({ title: listFieldLabel('supplierName', '供应商'), key: 'supplierName', minWidth: 150, ellipsis: { tooltip: true } }),
  amountCent: () => ({ title: listFieldLabel('amountCent', '金额'), key: 'amountCent', width: 120, render: row => formatMoney(row.amountCent) }),
  status: () => ({
    title: listFieldLabel('status', '状态'),
    key: 'status',
    width: 110,
    render: row => h(DictTag, { dictType: 'sample_purchase_order_status', value: row.status, size: 'small' }),
  }),
  applicantName: () => ({ title: listFieldLabel('applicantName', '申请人'), key: 'applicantName', width: 120, render: row => row.applicantName || '-' }),
  updateTime: () => ({ title: listFieldLabel('updateTime', '更新时间'), key: 'updateTime', width: 170, render: row => formatDateTime(row.updateTime) }),
}
const columns = computed(() => [
  ...codeAppMetadata.viewFields('LIST', defaultListFields)
    .map(field => columnBuilders[field]?.())
    .filter(Boolean),
  {
    title: '操作',
    key: 'actions',
    width: 280,
    fixed: 'right',
    render: row => h('div', { class: 'row-actions' }, [
      actionLink('详情', 'text-info', () => openDetail(row)),
      actionLink('编辑', canEdit(row) ? 'text-primary' : 'text-disabled', () => canEdit(row) && openEdit(row)),
      actionLink('提交审批', canSubmit(row) ? 'text-success' : 'text-disabled', () => canSubmit(row) && openSubmit(row)),
      actionLink('删除', canDelete(row) ? 'text-error' : 'text-disabled', () => canDelete(row) && handleRemove(row)),
    ]),
  },
])

const detail = reactive({})
const detailLoading = ref(false)
const taskFormRef = ref(null)
const localSubmitting = ref(false)
const localSubmittingAction = ref('')
const taskForm = reactive(defaultTaskForm())

const isDeptLeaderTask = computed(() => currentTaskDefKey.value === 'dept_leader_approve')
const isEngineeringTask = computed(() => currentTaskDefKey.value === 'engineering_manager_approve')
const isCountersignTask = computed(() => currentTaskDefKey.value === 'purchase_countersign')
const isModifyTask = computed(() => currentTaskDefKey.value === 'applicant_modify')
const editableNodeFields = computed(() => {
  if (isDeptLeaderTask.value)
    return ['arrivalListFileIds', 'deptLeaderRemark']
  if (isEngineeringTask.value)
    return ['engineeringManagerRemark']
  if (isCountersignTask.value)
    return ['countersignRemark']
  if (isModifyTask.value)
    return ['title', 'supplierName', 'amountCent', 'purchaseItems', 'needDate', 'applicantModifyRemark']
  return []
})
const writableNodeFields = computed(() => editableNodeFields.value.filter(field => canShowTaskField(field) && canEditTaskField(field)))
const currentNodeTitle = computed(() => {
  if (isDeptLeaderTask.value)
    return '部门负责人审批'
  if (isEngineeringTask.value)
    return '工程部经理审批'
  if (isCountersignTask.value)
    return '采购会签'
  if (isModifyTask.value)
    return '申请人修改'
  return '采购单审批'
})
const currentNodeDisplayName = computed(() => getTaskDisplayName({
  taskName: businessTaskForm.context.value?.taskName || currentTaskDefKey.value,
  taskDefKey: currentTaskDefKey.value,
}, currentNodeTitle.value || '未知节点'))
const canApprove = computed(() => props.approvalPolicy?.allowApprove !== false)
const canReject = computed(() => props.approvalPolicy?.allowReject !== false)
const requireComment = computed(() => props.approvalPolicy?.requireComment !== false)
const taskFormDisabled = computed(() => taskRuntime.value.readOnly || props.submitting || localSubmitting.value)

function canShowTaskField(field) {
  if (isTaskFormMode.value)
    return businessTaskForm.canShowField(field)
  return codeAppMetadata.isFormFieldVisible(field, true) && businessTaskForm.canShowField(field)
}

function canEditTaskField(field) {
  return businessTaskForm.canEditField(field)
}

function isTaskFieldDisabled(field) {
  return taskFormDisabled.value || !canEditTaskField(field)
}

function isTaskFieldRequired(field, fallback = false) {
  const permission = businessTaskForm.fieldPermission(field)
  return permission ? permission.required === true : fallback
}

function listFieldLabel(field, fallback) {
  return codeAppMetadata.viewFieldLabel('LIST', field, fallback)
}

function detailFieldLabel(field, fallback) {
  if (isTaskFormMode.value)
    return businessTaskForm.fieldConfig(field)?.label || fallback
  return codeAppMetadata.viewFieldLabel('DETAIL', field, fallback)
}

function formFieldLabel(field, fallback) {
  if (isTaskFormMode.value)
    return businessTaskForm.fieldConfig(field)?.label || fallback
  return codeAppMetadata.viewFieldLabel('FORM', field, fallback)
}

function showBusinessFormField(field) {
  return codeAppMetadata.formFields(defaultFormFields).includes(field)
}

function showFormModalField(field) {
  if (formReadonly.value)
    return codeAppMetadata.viewFields('DETAIL', defaultDetailFields).includes(field)
  return showBusinessFormField(field)
}

function showDetailField(field) {
  if (isTaskFormMode.value)
    return businessTaskForm.canShowField(field)
  const visibleInDetailView = codeAppMetadata.viewFields('DETAIL', defaultDetailFields).includes(field)
  if (!visibleInDetailView)
    return false
  return true
}

function shouldValidateTaskField(field, fallbackRequired = false) {
  return canShowTaskField(field) && canEditTaskField(field) && isTaskFieldRequired(field, fallbackRequired)
}

const taskFormRules = computed(() => {
  const rules = {}
  if (isModifyTask.value) {
    if (shouldValidateTaskField('title', true))
      rules.title = { required: true, message: '请输入采购主题', trigger: ['blur', 'input'] }
    if (shouldValidateTaskField('supplierName', true))
      rules.supplierName = { required: true, message: '请输入供应商', trigger: ['blur', 'input'] }
    if (shouldValidateTaskField('amountCent', true)) {
      rules.amountYuan = {
        required: true,
        type: 'number',
        validator: (_, value) => Number(value) > 0,
        message: '采购金额必须大于0',
        trigger: ['blur', 'change'],
      }
    }
    if (shouldValidateTaskField('needDate'))
      rules.needDate = { required: true, message: '请选择期望到货日期', trigger: ['blur', 'change'] }
    if (shouldValidateTaskField('purchaseItems'))
      rules.purchaseItems = { required: true, message: '请输入采购明细', trigger: ['blur', 'input'] }
    if (shouldValidateTaskField('applicantModifyRemark'))
      rules.applicantModifyRemark = { required: true, message: '请输入修改说明', trigger: ['blur', 'input'] }
  }
  if (isDeptLeaderTask.value) {
    if (shouldValidateTaskField('arrivalListFileIds'))
      rules.arrivalListFileIds = { required: true, message: '请上传清单', trigger: ['change'] }
    if (shouldValidateTaskField('deptLeaderRemark'))
      rules.deptLeaderRemark = { required: true, message: '请输入部门负责人意见', trigger: ['blur', 'input'] }
  }
  if (isEngineeringTask.value && shouldValidateTaskField('engineeringManagerRemark'))
    rules.engineeringManagerRemark = { required: true, message: '请输入工程部经理意见', trigger: ['blur', 'input'] }
  if (isCountersignTask.value && shouldValidateTaskField('countersignRemark'))
    rules.countersignRemark = { required: true, message: '请输入会签意见', trigger: ['blur', 'input'] }
  if (requireComment.value) {
    rules.comment = { required: true, message: '请输入审批意见', trigger: ['blur', 'input'] }
  }
  return rules
})

watch(() => [
  taskRuntime.value.taskId,
  taskRuntime.value.businessKey,
  taskRuntime.value.processInstanceId,
  taskRuntime.value.taskDefKey,
], () => {
  if (isTaskFormMode.value)
    loadTaskDetail()
}, { immediate: true })

onMounted(() => {
  if (!isTaskFormMode.value) {
    codeAppMetadata.load()
    loadData()
  }
})

async function loadData() {
  loading.value = true
  try {
    const res = await purchaseOrderPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...query,
    })
    if (res.code === 200) {
      rows.value = res.data?.records || []
      pagination.itemCount = res.data?.total || 0
      return
    }
    window.$message.error(res.message || '采购单列表加载失败')
  }
  finally {
    loading.value = false
  }
}

function handlePageSizeChange(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  Object.assign(query, { orderNo: '', title: '', supplierName: '', status: null })
  handleSearch()
}

function openFlowConfig() {
  router.push({
    path: '/app-center/object/sample_purchase_order/designer',
    query: {
      panel: 'flow-app',
      codeApp: '1',
      name: '采购申请',
      returnTo: route.fullPath,
    },
  })
}

async function handleInitFlow() {
  initLoading.value = true
  try {
    const res = await initPurchaseOrderFlow()
    if (res.code === 200) {
      window.$message.success(res.data?.message || '测试流程已初始化')
      return
    }
    window.$message.error(res.message || '初始化测试流程失败')
  }
  finally {
    initLoading.value = false
  }
}

function openCreate() {
  Object.assign(formState, defaultForm())
  formReadonly.value = false
  formModalVisible.value = true
}

async function openEdit(row) {
  await loadFormDetail(row)
  formReadonly.value = false
  formModalVisible.value = true
}

async function openDetail(row) {
  await loadFormDetail(row)
  formReadonly.value = true
  formModalVisible.value = true
}

async function loadFormDetail(record) {
  const query = resolveRowDetailQuery(record)
  if (!query)
    throw new Error('采购单ID不能为空')
  const res = await purchaseOrderDetail(query)
  if (res.code !== 200)
    throw new Error(res.message || '采购单详情加载失败')
  Object.assign(formState, toFormState(res.data || {}))
}

async function submitForm() {
  await formRef.value?.validate()
  saveLoading.value = true
  try {
    const payload = formToPayload(formState)
    const res = formState.id ? await updatePurchaseOrder(payload) : await createPurchaseOrder(payload)
    if (res.code === 200) {
      window.$message.success('保存成功')
      formModalVisible.value = false
      loadData()
      return
    }
    window.$message.error(res.message || '保存失败')
  }
  finally {
    saveLoading.value = false
  }
}

function openSubmit(row) {
  currentSubmitRow.value = row
  Object.assign(submitState, defaultSubmitForm())
  submitModalVisible.value = true
}

async function submitFlow() {
  await submitFormRef.value?.validate()
  const purchaseOrderId = normalizeSnowflakeId(currentSubmitRow.value?.id)
  if (!purchaseOrderId) {
    window.$message.error('采购单ID不能为空')
    return
  }
  submitLoading.value = true
  try {
    const res = await submitPurchaseOrder(purchaseOrderId, {
      deptLeaderId: toLong(submitState.deptLeaderId),
      engineeringManagerId: toLong(submitState.engineeringManagerId),
      countersignUserIds: toLongList(submitState.countersignUserIds),
      ccRoleKeys: submitState.ccRoleKeys?.length ? submitState.ccRoleKeys : ['admin'],
    })
    if (res.code === 200) {
      window.$message.success('流程已发起')
      submitModalVisible.value = false
      loadData()
      return
    }
    window.$message.error(res.message || '流程发起失败')
  }
  finally {
    submitLoading.value = false
  }
}

async function handleRemove(row) {
  window.$dialog.warning({
    title: '删除采购单',
    content: `确认删除采购单 ${row.orderNo || row.title}？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      const purchaseOrderId = normalizeSnowflakeId(row.id)
      if (!purchaseOrderId) {
        window.$message.error('采购单ID不能为空')
        return
      }
      const res = await removePurchaseOrder(purchaseOrderId)
      if (res.code === 200) {
        window.$message.success('删除成功')
        loadData()
      }
      else {
        window.$message.error(res.message || '删除失败')
      }
    },
  })
}

async function loadTaskDetail() {
  detailLoading.value = true
  try {
    const context = await loadBusinessTaskContext()
    if (applyTaskContextRecord(context))
      return
    const query = resolveRecordQuery(context)
    if (!query)
      return
    const res = await purchaseOrderDetail(query)
    if (res.code !== 200) {
      window.$message.error(res.message || '采购单详情加载失败')
      return
    }
    Object.keys(detail).forEach(key => delete detail[key])
    Object.assign(detail, res.data || {})
    Object.assign(detail, businessTaskForm.recordData.value || {})
    Object.assign(taskForm, detailToTaskForm(detail))
  }
  finally {
    detailLoading.value = false
  }
}

async function loadBusinessTaskContext() {
  const initialContext = resolveInitialTaskContext()
  if (initialContext) {
    businessTaskForm.setContext(initialContext)
    return initialContext
  }
  try {
    return await businessTaskForm.load({
      taskId: taskRuntime.value.taskId,
      businessKey: taskRuntime.value.businessKey,
      processInstanceId: taskRuntime.value.processInstanceId,
      processDefKey: taskRuntime.value.processDefKey,
      taskDefKey: taskRuntime.value.taskDefKey,
    })
  }
  catch (error) {
    window.$message.warning(error?.message || '字段权限加载失败，将使用默认节点规则')
    return null
  }
}

function resolveInitialTaskContext() {
  const context = props.initialTaskContext
  if (!context || typeof context !== 'object')
    return null
  if (!matchesTaskRuntime(context))
    return null
  return context
}

function matchesTaskRuntime(context) {
  const runtime = taskRuntime.value
  const pairs = [
    ['taskId', runtime.taskId],
    ['businessKey', runtime.businessKey],
    ['processInstanceId', runtime.processInstanceId],
    ['taskDefKey', runtime.taskDefKey],
  ]
  for (const [key, expected] of pairs) {
    if (expected && context[key] && String(context[key]) !== String(expected))
      return false
  }
  return Boolean(context.configured || context.recordData || context.recordId || context.businessKey)
}

function applyTaskContextRecord(context) {
  const recordData = context?.recordData && typeof context.recordData === 'object' ? context.recordData : null
  if (!recordData || Object.keys(recordData).length === 0)
    return false
  const nextDetail = { ...recordData }
  if (context.recordId && !nextDetail.id)
    nextDetail.id = context.recordId
  if (context.businessKey && !nextDetail.businessKey)
    nextDetail.businessKey = context.businessKey
  Object.keys(detail).forEach(key => delete detail[key])
  Object.assign(detail, nextDetail)
  Object.assign(taskForm, detailToTaskForm(detail))
  return true
}

async function saveTaskFields() {
  const recordQuery = resolveRecordQuery() || {}
  const data = {
    title: taskForm.title,
    supplierName: taskForm.supplierName,
    amountCent: yuanToCent(taskForm.amountYuan),
    purchaseItems: taskForm.purchaseItems,
    needDate: taskForm.needDate,
    arrivalListFileIds: taskForm.arrivalListFileIds,
    applicantModifyRemark: taskForm.applicantModifyRemark,
    deptLeaderRemark: taskForm.deptLeaderRemark,
    engineeringManagerRemark: taskForm.engineeringManagerRemark,
    countersignRemark: taskForm.countersignRemark,
  }
  if (businessTaskForm.context.value?.configured) {
    const savedContext = await businessTaskForm.save(data, {
      taskId: taskRuntime.value.taskId,
      businessKey: taskRuntime.value.businessKey || recordQuery.businessKey,
      processInstanceId: taskRuntime.value.processInstanceId,
      processDefKey: taskRuntime.value.processDefKey,
      taskDefKey: taskRuntime.value.taskDefKey,
      recordId: detail.id || recordQuery.id,
    })
    Object.assign(detail, savedContext?.recordData || {})
    return savedContext?.recordData
  }
  const payload = {
    id: detail.id || recordQuery.id,
    businessKey: taskRuntime.value.businessKey || recordQuery.businessKey,
    taskId: taskRuntime.value.taskId,
    taskDefKey: taskRuntime.value.taskDefKey,
    ...data,
  }
  const res = await savePurchaseOrderTaskFields(payload)
  if (res.code !== 200)
    throw new Error(res.message || '业务字段保存失败')
  Object.keys(detail).forEach(key => delete detail[key])
  Object.assign(detail, res.data || {})
  return res.data
}

async function submitTask(action) {
  if (taskFormDisabled.value)
    return
  if (action === 'approve' && !canApprove.value)
    return
  if (action === 'reject' && !canReject.value)
    return

  localSubmitting.value = true
  localSubmittingAction.value = action
  try {
    await taskFormRef.value?.validate()
    if (writableNodeFields.value.length)
      await saveTaskFields()
    emit('submit', {
      action,
      comment: taskForm.comment,
      variables: {
        approvalResult: action === 'approve' ? 'approve' : 'reject',
        approved: action === 'approve',
        purchaseOrderId: detail.id,
        orderNo: detail.orderNo,
        arrivalListFileIds: taskForm.arrivalListFileIds,
        deptLeaderRemark: taskForm.deptLeaderRemark,
        engineeringManagerRemark: taskForm.engineeringManagerRemark,
        countersignRemark: taskForm.countersignRemark,
        applicantModifyRemark: taskForm.applicantModifyRemark,
      },
    })
  }
  catch (error) {
    window.$message.error(error?.message || '提交失败')
  }
  finally {
    localSubmitting.value = false
    localSubmittingAction.value = ''
  }
}

function resolveRecordQuery(context = businessTaskForm.context.value) {
  const businessKey = cleanBusinessKey(
    taskRuntime.value.businessKey || props.variables?.businessKey || context?.businessKey,
  )
  if (businessKey)
    return { businessKey }

  const id = normalizeSnowflakeId(props.variables?.purchaseOrderId)
    || normalizeSnowflakeId(extractIdFromBusinessKey(taskRuntime.value.businessKey || props.variables?.businessKey))
    || normalizeSnowflakeId(props.variables?.recordId)
    || normalizeSnowflakeId(context?.recordId)
    || normalizeSnowflakeId(route.query.recordId)
  return id ? { id } : null
}

function resolveRowDetailQuery(record) {
  if (!record)
    return null
  const businessKey = cleanBusinessKey(record.businessKey)
  if (businessKey)
    return { businessKey }
  const id = normalizeSnowflakeId(record.id)
  return id ? { id } : null
}

function cleanBusinessKey(value) {
  if (value === null || value === undefined)
    return null
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined')
    return null
  return text.includes(':') ? text : null
}

function extractIdFromBusinessKey(value) {
  const businessKey = cleanBusinessKey(value)
  if (!businessKey)
    return null
  const parts = businessKey.split(':')
  return parts[parts.length - 1]
}

function normalizeSnowflakeId(value) {
  if (value === null || value === undefined)
    return null
  const text = String(value).trim()
  if (!/^[1-9]\d*$/.test(text))
    return null
  return text
}

function isActionSubmitting(action) {
  return (localSubmitting.value && localSubmittingAction.value === action)
    || (props.submitting && props.submittingAction === action)
}

function canEdit(row) {
  return ['DRAFT', 'NEED_MODIFY'].includes(row.status)
}

function canSubmit(row) {
  return row.status === 'DRAFT'
}

function canDelete(row) {
  return ['DRAFT', 'REJECTED', 'CANCELED'].includes(row.status)
}

function actionLink(text, className, onClick) {
  return h('a', { class: `${className} cursor-pointer hover:opacity-80`, onClick }, text)
}

function defaultForm() {
  return {
    id: null,
    title: '',
    supplierName: '',
    amountYuan: null,
    purchaseItems: '',
    needDate: null,
    remark: '',
  }
}

function defaultSubmitForm() {
  return {
    deptLeaderId: null,
    deptLeaderName: '',
    engineeringManagerId: null,
    engineeringManagerName: '',
    countersignUserIds: [],
    countersignUserNames: [],
    ccRoleKeys: ['admin'],
  }
}

function defaultTaskForm() {
  return {
    title: '',
    supplierName: '',
    amountYuan: null,
    purchaseItems: '',
    needDate: null,
    arrivalListFileIds: '',
    applicantModifyRemark: '',
    deptLeaderRemark: '',
    engineeringManagerRemark: '',
    countersignRemark: '',
    comment: '',
  }
}

function toFormState(row) {
  return {
    id: row.id || null,
    title: row.title || '',
    supplierName: row.supplierName || '',
    amountYuan: centToYuan(row.amountCent),
    purchaseItems: row.purchaseItems || '',
    needDate: row.needDate || null,
    remark: row.remark || '',
  }
}

function formToPayload(form) {
  return {
    id: form.id,
    title: form.title,
    supplierName: form.supplierName,
    amountCent: yuanToCent(form.amountYuan),
    purchaseItems: form.purchaseItems,
    needDate: form.needDate,
    remark: form.remark,
  }
}

function detailToTaskForm(row) {
  return {
    title: row.title || '',
    supplierName: row.supplierName || '',
    amountYuan: centToYuan(row.amountCent),
    purchaseItems: row.purchaseItems || '',
    needDate: row.needDate || null,
    arrivalListFileIds: row.arrivalListFileIds || '',
    applicantModifyRemark: row.applicantModifyRemark || '',
    deptLeaderRemark: row.deptLeaderRemark || '',
    engineeringManagerRemark: row.engineeringManagerRemark || '',
    countersignRemark: row.countersignRemark || '',
    comment: '',
  }
}

function centToYuan(value) {
  if (value === null || value === undefined || value === '')
    return null
  return Number((Number(value) / 100).toFixed(2))
}

function yuanToCent(value) {
  if (value === null || value === undefined || value === '')
    return null
  return Math.round(Number(value) * 100)
}

function isFilledValue(value) {
  return value !== null && value !== undefined && value !== ''
}

function toLong(value) {
  if (!isFilledValue(value))
    return null
  return String(value)
}

function toLongList(values) {
  return Array.isArray(values) ? values.map(toLong).filter(isFilledValue) : []
}

function formatMoney(value) {
  if (value === null || value === undefined)
    return '-'
  return `${centToYuan(value)?.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元`
}

function formatDateTime(value) {
  if (!value)
    return '-'
  return String(value).replace('T', ' ')
}
</script>

<style scoped>
.purchase-order-page,
.purchase-task-form {
  padding: 16px;
}

.page-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-toolbar h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.page-toolbar p {
  margin: 6px 0 0;
  color: var(--text-color-3);
}

.query-card {
  margin-bottom: 16px;
}

.query-actions,
.modal-footer,
.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.task-actions :deep(.n-button) {
  min-width: 96px;
}

.table-card :deep(.n-card__content) {
  padding-top: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.text-disabled {
  color: var(--text-color-3);
  pointer-events: none;
}

.task-form-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
}

.approval-comment {
  margin-top: 12px;
}

.preserve-text {
  white-space: pre-wrap;
}

.text-error {
  color: var(--error-color);
}

@media (max-width: 768px) {
  .page-toolbar {
    flex-direction: column;
  }

  .query-actions,
  .modal-footer,
  .task-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>

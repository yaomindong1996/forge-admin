<template>
  <div class="node-properties-panel">
    <!-- 用户选择弹窗 -->
    <UserSelectModal
      v-model:show="showUserSelect"
      :title="userSelectTitle"
      :multiple="userSelectMultiple"
      :selected-users="currentSelectedUsers"
      @confirm="handleUserSelectConfirm"
    />
    
    <!-- 角色选择弹窗 -->
    <n-modal
      v-model:show="showRoleSelect"
      preset="card"
      title="选择角色"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-data-table
        :columns="roleColumns"
        :data="roleList"
        :loading="roleLoading"
        :row-key="row => row.id"
        :checked-row-keys="checkedRoleKeys"
        @update:checked-row-keys="handleRoleCheck"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showRoleSelect = false">取消</n-button>
          <n-button type="primary" @click="handleRoleConfirm">确定</n-button>
        </n-space>
      </template>
    </n-modal>
    <!-- 基础属性 -->
    <n-collapse :default-expanded-names="['basic']">
      <n-collapse-item title="基础属性" name="basic">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="节点ID">
            <n-input v-model:value="properties.id" @blur="updateProperty('id')" placeholder="请输入节点ID" />
          </n-form-item>
          <n-form-item label="节点名称">
            <n-input v-model:value="properties.name" @blur="updateProperty('name')" placeholder="请输入节点名称" />
          </n-form-item>
          <n-form-item label="节点描述">
            <n-input 
              v-model:value="properties.documentation" 
              type="textarea" 
              :rows="2"
              @blur="updateProperty('documentation')" 
              placeholder="请输入节点描述" 
            />
          </n-form-item>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 开始节点配置 -->
    <n-collapse v-if="elementType === 'bpmn:StartEvent'" :default-expanded-names="['startConfig']">
      <n-collapse-item title="开始节点配置" name="startConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="发起人变量">
            <n-input 
              v-model:value="properties.initiator" 
              @blur="updateExtensionProperty('initiator')"
              placeholder="默认: initiator" 
            />
          </n-form-item>
          <n-form-item label="表单Key">
            <n-input 
              v-model:value="properties.formKey" 
              @blur="updateExtensionProperty('formKey')"
              placeholder="表单标识" 
            />
          </n-form-item>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 用户任务配置 -->
    <n-collapse v-if="elementType === 'bpmn:UserTask'" :default-expanded-names="['userTaskConfig']">
      <n-collapse-item title="审批设置" name="userTaskConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="任务类型">
            <n-select
              v-model:value="properties.taskType"
              :options="taskTypeOptions"
              @update:value="updateTaskType"
            />
          </n-form-item>

          <!-- 指定审批人 -->
          <template v-if="properties.taskType === 'assignee'">
            <n-form-item label="审批人">
              <n-space vertical size="small" style="width: 100%">
                <n-select
                  v-model:value="properties.assignee"
                  :options="assigneeOptions"
                  @update:value="updateUserTaskAssignee"
                  placeholder="选择审批人类型"
                  filterable
                  tag
                />
                <n-button
                  v-if="properties.assignee === 'custom'"
                  type="primary"
                  dashed
                  block
                  @click="openUserSelect('assignee')"
                >
                  <template #icon>
                    <i class="i-material-symbols:person-add" />
                  </template>
                  从用户列表选择
                </n-button>
              </n-space>
            </n-form-item>
            <n-form-item label="自定义表达式" v-if="properties.assignee === 'custom'">
              <n-input
                v-model:value="properties.assigneeExpr"
                @blur="updateCustomAssignee"
                placeholder="${user.id}"
              />
            </n-form-item>
            <n-form-item label="已选用户" v-if="properties.assigneeUserName">
              <n-tag type="info" closable @close="clearAssigneeUser">
                {{ properties.assigneeUserName }}
              </n-tag>
            </n-form-item>
          </template>

          <!-- 候选用户 -->
          <template v-if="properties.taskType === 'candidateUsers'">
            <n-form-item label="候选用户">
              <n-space vertical size="small" style="width: 100%">
                <n-button
                  type="primary"
                  dashed
                  block
                  @click="openUserSelect('candidateUsers')"
                >
                  <template #icon>
                    <i class="i-material-symbols:group-add" />
                  </template>
                  从用户列表选择
                </n-button>
                <div v-if="properties.candidateUserNames.length > 0" style="margin-top: 8px">
                  <n-tag
                    v-for="(name, index) in properties.candidateUserNames"
                    :key="index"
                    type="info"
                    closable
                    @close="removeCandidateUser(index)"
                    style="margin: 2px"
                  >
                    {{ name }}
                  </n-tag>
                </div>
              </n-space>
            </n-form-item>
          </template>

          <!-- 候选组 -->
          <template v-if="properties.taskType === 'candidateGroups'">
            <n-form-item label="候选组(角色)">
              <n-space vertical size="small" style="width: 100%">
                <n-button
                  type="primary"
                  dashed
                  block
                  @click="openRoleSelect"
                >
                  <template #icon>
                    <i class="i-material-symbols:shield" />
                  </template>
                  从角色列表选择
                </n-button>
                <div v-if="properties.candidateGroupNames.length > 0" style="margin-top: 8px">
                  <n-tag
                    v-for="(name, index) in properties.candidateGroupNames"
                    :key="index"
                    type="success"
                    closable
                    @close="removeCandidateGroup(index)"
                    style="margin: 2px"
                  >
                    {{ name }}
                  </n-tag>
                </div>
              </n-space>
            </n-form-item>
          </template>

          <!-- 表单配置 -->
          <n-form-item label="表单类型">
            <n-select
              v-model:value="properties.formType"
              :options="formTypeOptions"
              @update:value="updateFormType"
            />
          </n-form-item>
          
          <template v-if="properties.formType === 'dynamic'">
            <n-form-item label="表单Key">
              <n-input
                v-model:value="properties.formKey"
                @blur="updateExtensionProperty('formKey')"
                placeholder="表单标识"
              />
            </n-form-item>
            <n-form-item label="表单JSON">
              <n-input
                v-model:value="properties.formJson"
                type="textarea"
                :rows="3"
                @blur="updateExtensionProperty('formJson')"
                placeholder="表单JSON配置"
              />
            </n-form-item>
          </template>
          
          <template v-if="properties.formType === 'external'">
            <n-form-item label="表单URL">
              <n-input
                v-model:value="properties.formUrl"
                @blur="updateExtensionProperty('formUrl')"
                placeholder="外部表单URL"
              />
            </n-form-item>
          </template>

          <!-- 优先级 -->
          <n-form-item label="优先级">
            <n-slider
              v-model:value="properties.priority"
              :min="0"
              :max="100"
              :step="10"
              @update:value="updateExtensionProperty('priority')"
            />
          </n-form-item>

          <!-- 截止日期 -->
          <n-form-item label="截止日期(天)">
            <n-input-number
              v-model:value="properties.dueDate"
              :min="0"
              @update:value="updateDueDate"
              placeholder="0表示不限制"
            />
          </n-form-item>
        </n-form>
      </n-collapse-item>

      <!-- 多实例配置 -->
      <n-collapse-item title="会签/或签配置" name="multiInstance">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="多人审批方式">
            <n-radio-group v-model:value="properties.multiInstanceType" @update:value="updateMultiInstance">
              <n-radio-button value="none">单人审批</n-radio-button>
              <n-radio-button value="parallel">并行会签</n-radio-button>
              <n-radio-button value="sequential">依次审批</n-radio-button>
            </n-radio-group>
          </n-form-item>

          <template v-if="properties.multiInstanceType !== 'none'">
            <n-form-item label="完成条件">
              <n-select
                v-model:value="properties.completionCondition"
                :options="completionConditionOptions"
                @update:value="updateMultiInstance"
              />
            </n-form-item>
            
            <n-form-item label="通过比例(%)" v-if="properties.completionCondition === 'rate'">
              <n-slider
                v-model:value="properties.passRate"
                :min="0"
                :max="100"
                :step="10"
                @update:value="updateMultiInstance"
              />
            </n-form-item>
          </template>
        </n-form>
      </n-collapse-item>

      <!-- 任务监听器 -->
      <n-collapse-item title="任务监听器" name="taskListener">
        <div class="listener-list">
          <div v-for="(listener, index) in properties.taskListeners" :key="index" class="listener-item">
            <n-card size="small" :title="listener.event">
              <template #header-extra>
                <n-button text type="error" @click="removeTaskListener(index)">
                  <i class="i-material-symbols:delete" />
                </n-button>
              </template>
              <n-form :model="listener" label-placement="left" size="small">
                <n-form-item label="事件">
                  <n-select
                    v-model:value="listener.event"
                    :options="taskEventOptions"
                    size="small"
                  />
                </n-form-item>
                <n-form-item label="类名">
                  <n-input v-model:value="listener.class" placeholder="全限定类名" />
                </n-form-item>
              </n-form>
            </n-card>
          </div>
          <n-button dashed block @click="addTaskListener">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            添加监听器
          </n-button>
        </div>
      </n-collapse-item>
    </n-collapse>

    <!-- 服务任务配置 -->
    <n-collapse v-if="elementType === 'bpmn:ServiceTask'" :default-expanded-names="['serviceConfig']">
      <n-collapse-item title="服务任务配置" name="serviceConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="实现方式">
            <n-select
              v-model:value="properties.implementationType"
              :options="implementationTypeOptions"
              @update:value="updateServiceImplementation"
            />
          </n-form-item>
          <n-form-item label="实现值">
            <n-input
              v-model:value="properties.implementation"
              @blur="updateServiceImplementation"
              :placeholder="getImplementationPlaceholder()"
            />
          </n-form-item>
          <n-form-item label="异步执行">
            <n-switch v-model:value="properties.async" @update:value="updateAsync" />
          </n-form-item>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 排他网关配置 -->
    <n-collapse v-if="elementType === 'bpmn:ExclusiveGateway'" :default-expanded-names="['gatewayConfig']">
      <n-collapse-item title="网关配置" name="gatewayConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="网关类型">
            <n-radio-group v-model:value="properties.gatewayType" disabled>
              <n-radio value="exclusive">排他网关</n-radio>
              <n-radio value="parallel">并行网关</n-radio>
              <n-radio value="inclusive">包容网关</n-radio>
            </n-radio-group>
          </n-form-item>
          <n-alert type="info" size="small">
            排他网关：只选择一条路径执行<br/>
            并行网关：所有路径同时执行<br/>
            包容网关：满足条件的路径同时执行
          </n-alert>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 序列流配置 -->
    <n-collapse v-if="elementType === 'bpmn:SequenceFlow'" :default-expanded-names="['sequenceConfig']">
      <n-collapse-item title="流转条件" name="sequenceConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item>
            <n-checkbox v-model:checked="properties.hasCondition" @update:checked="toggleCondition">
              启用条件表达式
            </n-checkbox>
          </n-form-item>
          
          <template v-if="properties.hasCondition">
            <n-form-item label="条件类型">
              <n-radio-group v-model:value="properties.conditionType" @update:value="updateConditionType">
                <n-radio-button value="expression">表达式</n-radio-button>
                <n-radio-button value="script">脚本</n-radio-button>
              </n-radio-group>
            </n-form-item>
            
            <n-form-item label="条件表达式" v-if="properties.conditionType === 'expression'">
              <n-input
                v-model:value="properties.condition"
                type="textarea"
                :rows="3"
                @blur="updateCondition"
                placeholder="${approved == true}"
              />
            </n-form-item>
            
            <n-form-item label="脚本内容" v-if="properties.conditionType === 'script'">
              <n-input
                v-model:value="properties.script"
                type="textarea"
                :rows="5"
                @blur="updateCondition"
                placeholder="return approved == true;"
              />
            </n-form-item>
            
            <n-form-item label="脚本语言" v-if="properties.conditionType === 'script'">
              <n-select
                v-model:value="properties.scriptFormat"
                :options="scriptFormatOptions"
                @update:value="updateCondition"
              />
            </n-form-item>
          </template>

          <n-form-item>
            <n-checkbox v-model:checked="properties.isDefault" @update:checked="toggleDefault">
              设为默认路径
            </n-checkbox>
          </n-form-item>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 结束节点配置 -->
    <n-collapse v-if="elementType === 'bpmn:EndEvent'" :default-expanded-names="['endConfig']">
      <n-collapse-item title="结束节点配置" name="endConfig">
        <n-form :model="properties" label-placement="top" size="small">
          <n-form-item label="结束类型">
            <n-radio-group v-model:value="properties.endType">
              <n-radio value="terminate">终止流程</n-radio>
              <n-radio value="normal">正常结束</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-form>
      </n-collapse-item>
    </n-collapse>

    <!-- 执行监听器（通用） -->
    <n-collapse v-if="showExecutionListener" :default-expanded-names="['executionListener']">
      <n-collapse-item title="执行监听器" name="executionListener">
        <div class="listener-list">
          <div v-for="(listener, index) in properties.executionListeners" :key="index" class="listener-item">
            <n-card size="small" :title="listener.event">
              <template #header-extra>
                <n-button text type="error" @click="removeExecutionListener(index)">
                  <i class="i-material-symbols:delete" />
                </n-button>
              </template>
              <n-form :model="listener" label-placement="left" size="small">
                <n-form-item label="事件">
                  <n-select
                    v-model:value="listener.event"
                    :options="executionEventOptions"
                    size="small"
                  />
                </n-form-item>
                <n-form-item label="类名">
                  <n-input v-model:value="listener.class" placeholder="全限定类名" />
                </n-form-item>
              </n-form>
            </n-card>
          </div>
          <n-button dashed block @click="addExecutionListener">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            添加监听器
          </n-button>
        </div>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, inject, toRaw } from 'vue'
import UserSelectModal from './UserSelectModal.vue'
import { request } from '@/utils/http'

const props = defineProps({
  element: {
    type: Object,
    default: null
  },
  modeler: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update'])
// 使用全局 message 实例
const message = window.$message

// 获取原始元素（避免 Vue 代理与 bpmn-js 冲突）
const rawElement = computed(() => toRaw(props.element))

// 元素类型
const elementType = computed(() => rawElement.value?.type || '')

// 是否显示执行监听器
const showExecutionListener = computed(() => {
  return ['bpmn:UserTask', 'bpmn:ServiceTask', 'bpmn:ScriptTask', 'bpmn:StartEvent', 'bpmn:EndEvent'].includes(elementType.value)
})

// 属性对象
const properties = reactive({
  id: '',
  name: '',
  documentation: '',
  // 用户任务
  taskType: 'assignee',
  assignee: '',
  assigneeExpr: '',
  assigneeUserName: '',
  candidateUsers: [],
  candidateUserNames: [],
  candidateGroups: [],
  candidateGroupNames: [],
  formType: 'dynamic',
  formKey: '',
  formJson: '',
  formUrl: '',
  priority: 50,
  dueDate: 0,
  // 多实例
  multiInstanceType: 'none',
  completionCondition: 'all',
  passRate: 100,
  // 任务监听器
  taskListeners: [],
  // 执行监听器
  executionListeners: [],
  // 服务任务
  implementationType: 'class',
  implementation: '',
  async: false,
  // 序列流
  hasCondition: false,
  conditionType: 'expression',
  condition: '',
  script: '',
  scriptFormat: 'javascript',
  isDefault: false,
  // 开始节点
  initiator: 'initiator',
  // 结束节点
  endType: 'normal'
})

// 选项配置
const taskTypeOptions = [
  { label: '指定审批人', value: 'assignee' },
  { label: '候选用户', value: 'candidateUsers' },
  { label: '候选组(角色)', value: 'candidateGroups' }
]

const assigneeOptions = [
  { label: '发起人', value: '${initiator}' },
  { label: '发起人上级', value: '${initiatorLeader}' },
  { label: '部门经理', value: '${deptManager}' },
  { label: 'HR', value: '${hr}' },
  { label: '自定义表达式', value: 'custom' }
]

const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外部表单', value: 'external' },
  { label: '无表单', value: 'none' }
]

// 用户选择相关
const showUserSelect = ref(false)
const userSelectTitle = ref('选择用户')
const userSelectMultiple = ref(false)
const userSelectType = ref('')
const currentSelectedUsers = ref([])

// 角色选择相关
const showRoleSelect = ref(false)
const roleList = ref([])
const roleLoading = ref(false)
const checkedRoleKeys = ref([])

// 角色表格列
const roleColumns = [
  { type: 'selection' },
  { title: '角色名称', key: 'roleName' },
  { title: '角色编码', key: 'roleKey' }
]

const userOptions = ref([])
const roleOptions = ref([])

const completionConditionOptions = [
  { label: '全部通过', value: 'all' },
  { label: '任一通过', value: 'any' },
  { label: '按比例通过', value: 'rate' }
]

const taskEventOptions = [
  { label: '创建(create)', value: 'create' },
  { label: '分配(assignment)', value: 'assignment' },
  { label: '完成(complete)', value: 'complete' },
  { label: '删除(delete)', value: 'delete' }
]

const executionEventOptions = [
  { label: '开始(start)', value: 'start' },
  { label: '结束(end)', value: 'end' },
  { label: '执行(take)', value: 'take' }
]

const implementationTypeOptions = [
  { label: 'Java类', value: 'class' },
  { label: '表达式', value: 'expression' },
  { label: '委托表达式', value: 'delegateExpression' }
]

const scriptFormatOptions = [
  { label: 'JavaScript', value: 'javascript' },
  { label: 'Groovy', value: 'groovy' },
  { label: 'JUEL', value: 'juel' }
]

// 监听元素变化，加载属性
watch(() => props.element, (newElement) => {
  if (newElement) {
    loadElementProperties(toRaw(newElement))
  }
}, { immediate: true })

// 打开用户选择弹窗
function openUserSelect(type) {
  userSelectType.value = type
  if (type === 'assignee') {
    userSelectTitle.value = '选择审批人'
    userSelectMultiple.value = false
    // 如果已有选中的用户，回显
    if (properties.assigneeExpr && properties.assigneeExpr.startsWith('${user_')) {
      // 从表达式中提取用户ID，格式: ${user_1}
      const match = properties.assigneeExpr.match(/\$\{user_(\d+)\}/)
      if (match) {
        currentSelectedUsers.value = [{
          id: parseInt(match[1]),
          nickName: properties.assigneeUserName
        }]
      } else {
        currentSelectedUsers.value = []
      }
    } else {
      currentSelectedUsers.value = []
    }
  } else if (type === 'candidateUsers') {
    userSelectTitle.value = '选择候选用户'
    userSelectMultiple.value = true
    // 回显已选候选用户
    if (properties.candidateUsers.length > 0) {
      currentSelectedUsers.value = properties.candidateUsers.map((id, index) => ({
        id: parseInt(id),
        nickName: properties.candidateUserNames[index] || ''
      }))
    } else {
      currentSelectedUsers.value = []
    }
  }
  showUserSelect.value = true
}

// 用户选择确认
function handleUserSelectConfirm(users) {
  if (userSelectType.value === 'assignee') {
    const user = Array.isArray(users) ? users[0] : users
    if (user) {
      properties.assignee = 'custom'
      properties.assigneeExpr = `\${user_${user.id}}`
      properties.assigneeUserName = user.nickName || user.userName
      updateUserTaskAssignee()
    }
  } else if (userSelectType.value === 'candidateUsers') {
    const userList = Array.isArray(users) ? users : [users]
    userList.forEach(user => {
      if (!properties.candidateUsers.includes(user.id.toString())) {
        properties.candidateUsers.push(user.id.toString())
        properties.candidateUserNames.push(user.nickName || user.userName)
      }
    })
    updateCandidateUsers()
  }
  showUserSelect.value = false
}

// 清除审批人
function clearAssigneeUser() {
  properties.assignee = ''
  properties.assigneeExpr = ''
  properties.assigneeUserName = ''
  updateUserTaskAssignee()
}

// 移除候选用户
function removeCandidateUser(index) {
  properties.candidateUsers.splice(index, 1)
  properties.candidateUserNames.splice(index, 1)
  updateCandidateUsers()
}

// 打开角色选择弹窗
async function openRoleSelect() {
  // 回显已选角色
  if (properties.candidateGroups.length > 0) {
    checkedRoleKeys.value = properties.candidateGroups.map(id => parseInt(id))
  } else {
    checkedRoleKeys.value = []
  }
  showRoleSelect.value = true
  await loadRoleList()
}

// 加载角色列表
async function loadRoleList() {
  roleLoading.value = true
  try {
    const res = await request.get('/system/role/page', {
      params: { pageNum: 1, pageSize: 1000 }
    })
    if (res.code === 200 && res.data?.records) {
      roleList.value = res.data.records
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  } finally {
    roleLoading.value = false
  }
}

// 角色选择
function handleRoleCheck(keys) {
  checkedRoleKeys.value = keys
}

// 角色选择确认
function handleRoleConfirm() {
  const selectedRoles = roleList.value.filter(r => checkedRoleKeys.value.includes(r.id))
  selectedRoles.forEach(role => {
    if (!properties.candidateGroups.includes(role.id.toString())) {
      properties.candidateGroups.push(role.id.toString())
      properties.candidateGroupNames.push(role.roleName)
    }
  })
  updateCandidateGroups()
  showRoleSelect.value = false
}

// 移除候选组
function removeCandidateGroup(index) {
  properties.candidateGroups.splice(index, 1)
  properties.candidateGroupNames.splice(index, 1)
  updateCandidateGroups()
}

// 加载元素属性
function loadElementProperties(element) {
  const bo = element.businessObject
  if (!bo) return
  
  // 基础属性
  properties.id = bo.id || ''
  properties.name = bo.name || ''
  
  // 文档
  const docs = bo.documentation || []
  properties.documentation = docs.length > 0 ? docs[0].text : ''
  
  // 根据元素类型加载不同属性
  if (element.type === 'bpmn:UserTask') {
    loadUserTaskProperties(bo)
  } else if (element.type === 'bpmn:ServiceTask') {
    loadServiceTaskProperties(bo)
  } else if (element.type === 'bpmn:SequenceFlow') {
    loadSequenceFlowProperties(bo)
  } else if (element.type === 'bpmn:StartEvent') {
    loadStartEventProperties(bo)
  }
}

// 加载用户任务属性
function loadUserTaskProperties(bo) {
  // moddleExtensions 注册后，flowable 属性直接挂载在 bo 上（无命名空间前缀）
  // 同时兼容 $attrs 里的原始属性（XML 里带前缀的情况）
  const attrs = bo.$attrs || {}
  
  // 审批人类型判断 - 先读直接属性，再读 $attrs 兼容
  const assignee = bo.assignee ?? attrs['flowable:assignee'] ?? ''
  const candidateUsers = bo.candidateUsers ?? attrs['flowable:candidateUsers'] ?? ''
  const candidateGroups = bo.candidateGroups ?? attrs['flowable:candidateGroups'] ?? ''
  
  // 读取保存的用户名/角色名（自定义扩展属性）
  const assigneeName = bo.assigneeName ?? attrs['flowable:assigneeName'] ?? ''
  const candidateUserNames = bo.candidateUserNames ?? attrs['flowable:candidateUserNames'] ?? ''
  const candidateGroupNames = bo.candidateGroupNames ?? attrs['flowable:candidateGroupNames'] ?? ''
  
  // 重置审批相关属性，防止上一个节点的数据残留
  properties.taskType = 'assignee'
  properties.assignee = ''
  properties.assigneeExpr = ''
  properties.assigneeUserName = ''
  properties.candidateUsers = []
  properties.candidateUserNames = []
  properties.candidateGroups = []
  properties.candidateGroupNames = []

  if (assignee) {
    properties.taskType = 'assignee'
    properties.assigneeUserName = assigneeName
    // 判断是否是自定义用户 ID 表达式：${user_123}
    if (assignee.startsWith('${user_')) {
      properties.assignee = 'custom'
      properties.assigneeExpr = assignee
    } else {
      properties.assignee = assignee
    }
  } else if (candidateUsers) {
    properties.taskType = 'candidateUsers'
    properties.candidateUsers = candidateUsers.split(',').filter(Boolean)
    properties.candidateUserNames = candidateUserNames ? candidateUserNames.split(',').filter(Boolean) : []
  } else if (candidateGroups) {
    properties.taskType = 'candidateGroups'
    properties.candidateGroups = candidateGroups.split(',').filter(Boolean)
    properties.candidateGroupNames = candidateGroupNames ? candidateGroupNames.split(',').filter(Boolean) : []
  }
  
  // 表单配置 - 直接读 bo 上的属性
  properties.formKey = bo.formKey ?? attrs['flowable:formKey'] ?? ''
  properties.formJson = bo.formJson ?? attrs['flowable:formJson'] ?? ''
  properties.formUrl = bo.formUrl ?? attrs['flowable:formUrl'] ?? ''
  
  // 表单类型判断
  if (properties.formUrl) {
    properties.formType = 'external'
  } else if (properties.formKey || properties.formJson) {
    properties.formType = 'dynamic'
  } else {
    properties.formType = 'none'
  }
  
  // 优先级和截止日期
  const priorityVal = bo.priority ?? attrs['flowable:priority']
  properties.priority = priorityVal != null ? parseInt(priorityVal) : 50
  const dueDateVal = bo.dueDate ?? attrs['flowable:dueDate']
  if (dueDateVal) {
    // 格式可能是 P3D (ISO 8601 duration) 或纯数字
    const match = String(dueDateVal).match(/(\d+)/)
    properties.dueDate = match ? parseInt(match[1]) : 0
  } else {
    properties.dueDate = 0
  }
  
  // 多实例配置
  const loopCharacteristics = bo.loopCharacteristics
  if (loopCharacteristics) {
    properties.multiInstanceType = loopCharacteristics.isSequential ? 'sequential' : 'parallel'
    // 解析完成条件
    if (loopCharacteristics.completionCondition) {
      const condition = loopCharacteristics.completionCondition.body || ''
      if (condition.includes('nrOfCompletedInstances == nrOfInstances')) {
        properties.completionCondition = 'all'
      } else if (condition.includes('nrOfCompletedInstances >= 1')) {
        properties.completionCondition = 'any'
      } else if (condition.includes('/ nrOfInstances')) {
        properties.completionCondition = 'rate'
        const match = condition.match(/>= *([\d.]+)/)
        if (match) {
          properties.passRate = Math.round(parseFloat(match[1]) * 100)
        }
      }
    } else {
      properties.completionCondition = 'all'
    }
  } else {
    properties.multiInstanceType = 'none'
    properties.completionCondition = 'all'
    properties.passRate = 100
  }
  
  // 任务监听器
  properties.taskListeners = []
  const extensionElements = bo.extensionElements?.values || []
  extensionElements.forEach(ext => {
    if (ext.$type === 'flowable:TaskListener') {
      properties.taskListeners.push({
        event: ext.event || 'create',
        class: ext.class || ''
      })
    }
  })
  
  // 执行监听器
  properties.executionListeners = []
  extensionElements.forEach(ext => {
    if (ext.$type === 'flowable:ExecutionListener') {
      properties.executionListeners.push({
        event: ext.event || 'start',
        class: ext.class || ''
      })
    }
  })
}

// 加载服务任务属性
function loadServiceTaskProperties(bo) {
  const attrs = bo.$attrs || {}
  
  // moddleExtensions 注册后直接读 bo.class / bo.expression / bo.delegateExpression
  const classVal = bo['class'] ?? bo['flowable:class'] ?? attrs['flowable:class']
  const exprVal = bo['expression'] ?? bo['flowable:expression'] ?? attrs['flowable:expression']
  const delegateVal = bo['delegateExpression'] ?? bo['flowable:delegateExpression'] ?? attrs['flowable:delegateExpression']

  if (classVal) {
    properties.implementationType = 'class'
    properties.implementation = classVal
  } else if (exprVal) {
    properties.implementationType = 'expression'
    properties.implementation = exprVal
  } else if (delegateVal) {
    properties.implementationType = 'delegateExpression'
    properties.implementation = delegateVal
  } else {
    properties.implementationType = 'class'
    properties.implementation = ''
  }
  properties.async = bo['async'] ?? attrs['flowable:async'] ?? false
}

// 加载序列流属性
function loadSequenceFlowProperties(bo) {
  const conditionExpression = bo.conditionExpression
  if (conditionExpression) {
    properties.hasCondition = true
    properties.condition = conditionExpression.body || ''
    properties.conditionType = 'expression'
  } else {
    properties.hasCondition = false
  }
  
  // 检查是否默认流
  const source = bo.sourceRef
  if (source && source.default) {
    properties.isDefault = source.default.id === bo.id
  } else {
    properties.isDefault = false
  }
}

// 加载开始事件属性
function loadStartEventProperties(bo) {
  const attrs = bo.$attrs || {}
  // moddleExtensions 注册后直接读 bo.initiator
  properties.initiator = bo['initiator'] ?? bo['flowable:initiator'] ?? attrs['flowable:initiator'] ?? 'initiator'
  properties.formKey = bo.formKey ?? bo['flowable:formKey'] ?? attrs['flowable:formKey'] ?? ''
}

// 更新扩展属性（flowable 命名空间属性，通过 moddleExtensions 注册后直接用属性名）
function updateExtensionProperty(prop) {
  if (!rawElement.value || !props.modeler) return

  const modeling = props.modeler.get('modeling')
  const value = properties[prop]
  // 使用 flowable: 前缀写入，bpmn-js 会根据 moddleExtensions 映射
  modeling.updateProperties(rawElement.value, {
    [`flowable:${prop}`]: value !== '' ? value : null
  })
}

// 更新任务类型
function updateTaskType() {
  // 清空其他审批人配置
  properties.assignee = ''
  properties.assigneeExpr = ''
  properties.candidateUsers = []
  properties.candidateGroups = []
}

// 更新表单类型
function updateFormType() {
  if (!rawElement.value || !props.modeler) return

  const modeling = props.modeler.get('modeling')

  if (properties.formType === 'none') {
    properties.formKey = ''
    properties.formJson = ''
    properties.formUrl = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formKey': null,
      'flowable:formJson': null,
      'flowable:formUrl': null
    })
  } else if (properties.formType === 'external') {
    properties.formKey = ''
    properties.formJson = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formKey': null,
      'flowable:formJson': null
    })
  } else if (properties.formType === 'dynamic') {
    properties.formUrl = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formUrl': null
    })
  }
}

// 更新用户任务审批人
function updateUserTaskAssignee() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const value = properties.assignee === 'custom' ? properties.assigneeExpr : properties.assignee
  const element = rawElement.value
  
  modeling.updateProperties(element, {
    'flowable:assignee': value || null,
    'flowable:assigneeName': properties.assigneeUserName || null,
    'flowable:candidateUsers': null,
    'flowable:candidateUserNames': null,
    'flowable:candidateGroups': null,
    'flowable:candidateGroupNames': null
  })
  emit('update')
}

// 更新候选用户
function updateCandidateUsers() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const element = rawElement.value
  const usersStr = properties.candidateUsers.join(',')
  const userNamesStr = properties.candidateUserNames.join(',')
  
  modeling.updateProperties(element, {
    'flowable:assignee': null,
    'flowable:assigneeName': null,
    'flowable:candidateUsers': usersStr || null,
    'flowable:candidateUserNames': userNamesStr || null,
    'flowable:candidateGroups': null,
    'flowable:candidateGroupNames': null
  })
  emit('update')
}

// 更新候选组
function updateCandidateGroups() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const element = rawElement.value
  const groupsStr = properties.candidateGroups.join(',')
  const groupNamesStr = properties.candidateGroupNames.join(',')
  
  modeling.updateProperties(element, {
    'flowable:assignee': null,
    'flowable:assigneeName': null,
    'flowable:candidateUsers': null,
    'flowable:candidateUserNames': null,
    'flowable:candidateGroups': groupsStr || null,
    'flowable:candidateGroupNames': groupNamesStr || null
  })
  emit('update')
}

// 更新截止日期
function updateDueDate() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(rawElement.value, {
    'flowable:dueDate': properties.dueDate > 0 ? `P${properties.dueDate}D` : null
  })
}

// 更新多实例配置
function updateMultiInstance() {
  if (!rawElement.value || !props.modeler) return

  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')

  if (properties.multiInstanceType === 'none') {
    modeling.updateProperties(rawElement.value, {
      loopCharacteristics: null
    })
    return
  }

  // 构建完成条件表达式字符串
  let completionConditionStr = ''
  if (properties.completionCondition === 'all') {
    completionConditionStr = '${nrOfCompletedInstances == nrOfInstances}'
  } else if (properties.completionCondition === 'any') {
    completionConditionStr = '${nrOfCompletedInstances >= 1}'
  } else if (properties.completionCondition === 'rate') {
    const rate = (properties.passRate / 100).toFixed(2)
    completionConditionStr = `\${nrOfCompletedInstances / nrOfInstances >= ${rate}}`
  }

  // 必须使用 moddle.create 创建 bpmn-js 可识别的对象
  const completionConditionObj = completionConditionStr
    ? moddle.create('bpmn:FormalExpression', { body: completionConditionStr })
    : null

  // 集合表达式：多实例的执行人从变量集合中获取
  const loopCardinality = moddle.create('bpmn:FormalExpression', { body: '${nrOfInstances}' })

  const loopCharacteristics = moddle.create('bpmn:MultiInstanceLoopCharacteristics', {
    isSequential: properties.multiInstanceType === 'sequential',
    completionCondition: completionConditionObj
  })

  modeling.updateProperties(rawElement.value, {
    loopCharacteristics
  })
  emit('update')
}

// 添加任务监听器
function addTaskListener() {
  properties.taskListeners.push({
    event: 'create',
    class: ''
  })
}

// 移除任务监听器
function removeTaskListener(index) {
  properties.taskListeners.splice(index, 1)
  updateTaskListeners()
}

// 更新任务监听器
function updateTaskListeners() {
  if (!rawElement.value || !props.modeler) return

  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')
  const bo = rawElement.value.businessObject

  // 保留执行监听器，合并任务监听器
  const existingExtValues = bo.extensionElements?.values || []
  const executionListeners = existingExtValues.filter(v => v.$type === 'flowable:ExecutionListener')

  const taskListeners = properties.taskListeners
    .filter(l => l.class)
    .map(l => moddle.create('flowable:TaskListener', {
      event: l.event,
      class: l.class
    }))

  const allValues = [...executionListeners, ...taskListeners]

  const extensionElements = moddle.create('bpmn:ExtensionElements', { values: allValues })
  modeling.updateProperties(rawElement.value, { extensionElements })
  emit('update')
}

// 添加执行监听器
function addExecutionListener() {
  properties.executionListeners.push({
    event: 'start',
    class: ''
  })
}

// 移除执行监听器
function removeExecutionListener(index) {
  properties.executionListeners.splice(index, 1)
}

// 更新服务任务实现
function updateServiceImplementation() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const updateProps = {
    'flowable:class': null,
    'flowable:expression': null,
    'flowable:delegateExpression': null
  }
  
  const key = `flowable:${properties.implementationType}`
  updateProps[key] = properties.implementation || null
  
  modeling.updateProperties(rawElement.value, updateProps)
  emit('update')
}

// 更新异步
function updateAsync() {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(rawElement.value, {
    'flowable:async': properties.async
  })
  emit('update')
}

// 获取实现方式占位符
function getImplementationPlaceholder() {
  const placeholders = {
    class: 'com.example.MyServiceTask',
    expression: '${myService.execute()}',
    delegateExpression: '${myServiceDelegate}'
  }
  return placeholders[properties.implementationType] || ''
}

// 切换条件
function toggleCondition(checked) {
  if (!checked) {
    properties.condition = ''
    updateCondition()
  }
}

// 更新条件类型
function updateConditionType() {
  properties.condition = ''
  properties.script = ''
}

// 更新流转条件
function updateCondition() {
  if (!rawElement.value || !props.modeler) return
  
  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')
  
  if (properties.conditionType === 'expression' && properties.condition) {
    const expr = moddle.create('bpmn:FormalExpression', { body: properties.condition })
    modeling.updateProperties(rawElement.value, { conditionExpression: expr })
  } else if (properties.conditionType === 'script' && properties.script) {
    const expr = moddle.create('bpmn:FormalExpression', {
      body: properties.script,
      language: properties.scriptFormat
    })
    modeling.updateProperties(rawElement.value, { conditionExpression: expr })
  } else {
    modeling.updateProperties(rawElement.value, { conditionExpression: null })
  }
  emit('update')
}

// 切换默认路径
function toggleDefault(checked) {
  if (!rawElement.value || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const bo = rawElement.value.businessObject
  
  if (checked) {
    // 设置为默认流
    modeling.updateProperties(bo.sourceRef, {
      default: rawElement.value
    })
  } else {
    // 取消默认流
    modeling.updateProperties(bo.sourceRef, {
      default: null
    })
  }
}

// 自定义审批人表达式
function updateCustomAssignee() {
  if (properties.assignee === 'custom') {
    updateUserTaskAssignee()
  }
}

// 更新基础属性（id/name/documentation）时也触发 emit
function updateProperty(prop) {
  if (!rawElement.value || !props.modeler) return

  const modeling = props.modeler.get('modeling')
  const moddle = props.modeler.get('moddle')

  if (prop === 'id') {
    modeling.updateProperties(rawElement.value, { id: properties.id })
  } else if (prop === 'name') {
    modeling.updateProperties(rawElement.value, { name: properties.name })
  } else if (prop === 'documentation') {
    const docs = properties.documentation
      ? [moddle.create('bpmn:Documentation', { text: properties.documentation })]
      : []
    modeling.updateProperties(rawElement.value, { documentation: docs })
  }
  emit('update')
}
</script>

<style scoped>
.node-properties-panel {
  height: 100%;
  overflow-y: auto;
  padding: 8px;
}

.listener-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.listener-item {
  margin-bottom: 8px;
}

:deep(.n-collapse-item__header-main) {
  font-weight: 600;
}

:deep(.n-form-item) {
  margin-bottom: 12px;
}

:deep(.n-form-item:last-child) {
  margin-bottom: 0;
}
</style>
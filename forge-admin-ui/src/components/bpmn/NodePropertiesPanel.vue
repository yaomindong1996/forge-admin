<template>
  <div class="node-properties-panel">
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
              <n-select
                v-model:value="properties.assignee"
                :options="assigneeOptions"
                @update:value="updateUserTaskAssignee"
                placeholder="选择审批人"
                filterable
              />
            </n-form-item>
            <n-form-item label="自定义表达式" v-if="properties.assignee === 'custom'">
              <n-input 
                v-model:value="properties.assigneeExpr" 
                @blur="updateCustomAssignee"
                placeholder="${user.id}" 
              />
            </n-form-item>
          </template>

          <!-- 候选用户 -->
          <template v-if="properties.taskType === 'candidateUsers'">
            <n-form-item label="候选用户">
              <n-select
                v-model:value="properties.candidateUsers"
                :options="userOptions"
                multiple
                filterable
                @update:value="updateCandidateUsers"
                placeholder="选择候选用户"
              />
            </n-form-item>
          </template>

          <!-- 候选组 -->
          <template v-if="properties.taskType === 'candidateGroups'">
            <n-form-item label="候选组(角色)">
              <n-select
                v-model:value="properties.candidateGroups"
                :options="roleOptions"
                multiple
                filterable
                @update:value="updateCandidateGroups"
                placeholder="选择候选组"
              />
            </n-form-item>
          </template>

          <!-- 表单Key -->
          <n-form-item label="表单Key">
            <n-input 
              v-model:value="properties.formKey" 
              @blur="updateExtensionProperty('formKey')"
              placeholder="表单标识" 
            />
          </n-form-item>

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
import { ref, reactive, computed, watch, inject } from 'vue'

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

// 元素类型
const elementType = computed(() => props.element?.type || '')

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
  candidateUsers: [],
  candidateGroups: [],
  formKey: '',
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
    loadElementProperties(newElement)
  }
}, { immediate: true })

// 加载元素属性
function loadElementProperties(element) {
  const bo = element.businessObject
  
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
  // 审批人类型判断
  if (bo.assignee) {
    properties.taskType = 'assignee'
    properties.assignee = bo.assignee
  } else if (bo.candidateUsers) {
    properties.taskType = 'candidateUsers'
    properties.candidateUsers = bo.candidateUsers.split(',').filter(Boolean)
  } else if (bo.candidateGroups) {
    properties.taskType = 'candidateGroups'
    properties.candidateGroups = bo.candidateGroups.split(',').filter(Boolean)
  }
  
  properties.formKey = bo.formKey || ''
  properties.priority = parseInt(bo.priority) || 50
  properties.dueDate = bo.dueDate ? parseInt(bo.dueDate) : 0
  
  // 多实例配置
  const loopCharacteristics = bo.loopCharacteristics
  if (loopCharacteristics) {
    properties.multiInstanceType = loopCharacteristics.isSequential ? 'sequential' : 'parallel'
    // 解析完成条件
    if (loopCharacteristics.completionCondition) {
      const condition = loopCharacteristics.completionCondition.body
      if (condition.includes('nrOfCompletedInstances == nrOfInstances')) {
        properties.completionCondition = 'all'
      } else if (condition.includes('nrOfCompletedInstances >= 1')) {
        properties.completionCondition = 'any'
      } else if (condition.includes('/ nrOfInstances')) {
        properties.completionCondition = 'rate'
        // 提取比例
        const match = condition.match(/>=\s*([\d.]+)/)
        if (match) {
          properties.passRate = Math.round(parseFloat(match[1]) * 100)
        }
      }
    }
  } else {
    properties.multiInstanceType = 'none'
  }
  
  // 任务监听器
  properties.taskListeners = []
  const extensionElements = bo.extensionElements?.values || []
  extensionElements.forEach(ext => {
    if (ext.$type === 'flowable:TaskListener') {
      properties.taskListeners.push({
        event: ext.event,
        class: ext.class
      })
    }
  })
}

// 加载服务任务属性
function loadServiceTaskProperties(bo) {
  if (bo['flowable:class']) {
    properties.implementationType = 'class'
    properties.implementation = bo['flowable:class']
  } else if (bo['flowable:expression']) {
    properties.implementationType = 'expression'
    properties.implementation = bo['flowable:expression']
  } else if (bo['flowable:delegateExpression']) {
    properties.implementationType = 'delegateExpression'
    properties.implementation = bo['flowable:delegateExpression']
  }
  properties.async = bo.async || false
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
  properties.initiator = bo['flowable:initiator'] || 'initiator'
  properties.formKey = bo.formKey || ''
}

// 更新基础属性
function updateProperty(prop) {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  
  if (prop === 'id') {
    modeling.updateProperties(props.element, { id: properties.id })
  } else if (prop === 'name') {
    modeling.updateProperties(props.element, { name: properties.name })
  } else if (prop === 'documentation') {
    modeling.updateProperties(props.element, {
      documentation: properties.documentation ? [{ text: properties.documentation }] : []
    })
  }
}

// 更新扩展属性
function updateExtensionProperty(prop) {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(props.element, {
    [`flowable:${prop}`]: properties[prop]
  })
}

// 更新任务类型
function updateTaskType() {
  // 清空其他审批人配置
  properties.assignee = ''
  properties.candidateUsers = []
  properties.candidateGroups = []
}

// 更新用户任务审批人
function updateUserTaskAssignee() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const value = properties.assignee === 'custom' ? properties.assigneeExpr : properties.assignee
  
  modeling.updateProperties(props.element, {
    'flowable:assignee': value,
    'flowable:candidateUsers': null,
    'flowable:candidateGroups': null
  })
}

// 更新候选用户
function updateCandidateUsers() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(props.element, {
    'flowable:assignee': null,
    'flowable:candidateUsers': properties.candidateUsers.join(','),
    'flowable:candidateGroups': null
  })
}

// 更新候选组
function updateCandidateGroups() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(props.element, {
    'flowable:assignee': null,
    'flowable:candidateUsers': null,
    'flowable:candidateGroups': properties.candidateGroups.join(',')
  })
}

// 更新截止日期
function updateDueDate() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(props.element, {
    'flowable:dueDate': properties.dueDate > 0 ? `P${properties.dueDate}D` : null
  })
}

// 更新多实例配置
function updateMultiInstance() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  
  if (properties.multiInstanceType === 'none') {
    modeling.updateProperties(props.element, {
      loopCharacteristics: null
    })
    return
  }
  
  // 构建完成条件
  let completionCondition = null
  if (properties.completionCondition === 'all') {
    completionCondition = '${nrOfCompletedInstances == nrOfInstances}'
  } else if (properties.completionCondition === 'any') {
    completionCondition = '${nrOfCompletedInstances >= 1}'
  } else if (properties.completionCondition === 'rate') {
    const rate = properties.passRate / 100
    completionCondition = `\${nrOfCompletedInstances / nrOfInstances >= ${rate}}`
  }
  
  modeling.updateProperties(props.element, {
    loopCharacteristics: {
      isSequential: properties.multiInstanceType === 'sequential',
      completionCondition: completionCondition ? {
        $type: 'bpmn:FormalExpression',
        body: completionCondition
      } : null
    }
  })
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
  if (!props.element || !props.modeler) return
  
  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')
  
  const listeners = properties.taskListeners
    .filter(l => l.class)
    .map(l => moddle.create('flowable:TaskListener', {
      event: l.event,
      class: l.class
    }))
  
  modeling.updateProperties(props.element, {
    extensionElements: moddle.create('bpmn:ExtensionElements', {
      values: listeners
    })
  })
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
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const props = {
    'flowable:class': null,
    'flowable:expression': null,
    'flowable:delegateExpression': null
  }
  
  const key = `flowable:${properties.implementationType}`
  props[key] = properties.implementation
  
  modeling.updateProperties(props.element, props)
}

// 更新异步
function updateAsync() {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(props.element, {
    'flowable:async': properties.async
  })
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
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  
  if (properties.conditionType === 'expression' && properties.condition) {
    modeling.updateProperties(props.element, {
      conditionExpression: {
        $type: 'bpmn:FormalExpression',
        body: properties.condition
      }
    })
  } else if (properties.conditionType === 'script' && properties.script) {
    modeling.updateProperties(props.element, {
      conditionExpression: {
        $type: 'bpmn:FormalExpression',
        body: properties.script,
        language: properties.scriptFormat
      }
    })
  } else {
    modeling.updateProperties(props.element, {
      conditionExpression: null
    })
  }
}

// 切换默认路径
function toggleDefault(checked) {
  if (!props.element || !props.modeler) return
  
  const modeling = props.modeler.get('modeling')
  const bo = props.element.businessObject
  
  if (checked) {
    // 设置为默认流
    modeling.updateProperties(bo.sourceRef, {
      default: props.element
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
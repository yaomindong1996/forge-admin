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
          <n-button @click="showRoleSelect = false">
            取消
          </n-button>
          <n-button type="primary" @click="handleRoleConfirm">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
    <!-- Tab分隔配置 - 横向滚动 -->
    <div class="tabs-wrapper">
      <n-tabs
        v-model:value="activeTab"
        type="line"
        size="small"
        class="config-tabs"
      >
        <!-- 基础属性Tab -->
        <n-tab-pane name="basic" tab="基础属性">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item label="节点ID">
              <n-input v-model:value="properties.id" placeholder="请输入节点ID" @input="markDirty" />
            </n-form-item>
            <n-form-item label="节点名称">
              <n-input v-model:value="properties.name" placeholder="请输入节点名称" @input="markDirty" />
            </n-form-item>
            <n-form-item label="节点描述">
              <n-input
                v-model:value="properties.documentation"
                type="textarea"
                :rows="2"
                placeholder="请输入节点描述"
                @input="markDirty"
              />
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 开始节点配置Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:StartEvent'" name="startConfig" tab="开始配置">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item label="发起人变量">
              <n-input
                v-model:value="properties.initiator"
                placeholder="默认: initiator"
                @input="markDirty"
              />
            </n-form-item>
            <n-form-item label="表单Key">
              <n-input
                v-model:value="properties.formKey"
                placeholder="表单标识"
                @input="markDirty"
              />
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 审批设置Tab（仅用户任务） -->
        <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="approval" tab="审批设置">
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
                    placeholder="选择审批人类型"
                    filterable
                    tag
                    @update:value="updateUserTaskAssignee"
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

              <!-- SPEL 表达式配置 -->
              <template v-if="properties.assignee === 'spel'">
                <n-form-item label="表达式模板">
                  <n-select
                    v-model:value="selectedSpelTemplate"
                    :options="spelTemplatesFromApi"
                    placeholder="选择常用表达式模板（可选）"
                    clearable
                    @update:value="applySpelTemplate"
                  >
                    <template #option="{ option }">
                      <div>
                        <div style="font-weight: 500">
                          {{ option.label }}
                        </div>
                        <div v-if="option.description" style="font-size: 12px; color: #999; margin-top: 4px">
                          {{ option.description }}
                        </div>
                      </div>
                    </template>
                  </n-select>
                </n-form-item>

                <n-form-item label="SPEL 表达式">
                  <n-space vertical size="small" style="width: 100%">
                    <n-input
                      v-model:value="properties.assigneeExpr"
                      type="textarea"
                      placeholder="输入 SPEL 表达式，例如：${userService.findContactByRegion(execution.getVariable('regionCode'))}"
                      :autosize="{ minRows: 3, maxRows: 6 }"
                      @blur="validateSpelExpression"
                    />
                    <n-alert
                      v-if="spelValidationError"
                      type="error"
                      :title="spelValidationError"
                      closable
                      @close="spelValidationError = ''"
                    />
                    <n-collapse>
                      <n-collapse-item title="表达式语法提示" name="help">
                        <n-space vertical size="small">
                          <div style="font-size: 13px; line-height: 1.6">
                            <p><strong>可用对象：</strong></p>
                            <ul style="margin: 8px 0; padding-left: 20px">
                              <li><code>execution</code> - 流程执行上下文</li>
                              <li><code>userService</code> - 用户查询服务</li>
                              <li><code>deptService</code> - 部门查询服务</li>
                              <li><code>roleService</code> - 角色查询服务</li>
                            </ul>
                            <p><strong>常用方法：</strong></p>
                            <ul style="margin: 8px 0; padding-left: 20px">
                              <li><code>execution.getVariable("key")</code> - 获取流程变量</li>
                              <li><code>userService.findById(userId)</code> - 根据ID查找用户</li>
                              <li><code>userService.findByRole(roleKey)</code> - 根据角色查找用户</li>
                              <li><code>deptService.findManager(deptId)</code> - 查找部门负责人</li>
                            </ul>
                            <p><strong>示例：</strong></p>
                            <ul style="margin: 8px 0; padding-left: 20px">
                              <li>条件判断：<code>${amount > 10000 ? 'manager' : 'staff'}</code></li>
                              <li>方法调用：<code>${userService.findContactByRegion(regionCode)}</code></li>
                              <li>链式调用：<code>${deptService.findById(deptId).getManager()}</code></li>
                            </ul>
                          </div>
                        </n-space>
                      </n-collapse-item>
                    </n-collapse>
                  </n-space>
                </n-form-item>
              </template>

              <n-form-item v-if="properties.assigneeUserName" label="已选用户">
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
                      style="margin: 2px"
                      @close="removeCandidateUser(index)"
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
                      style="margin: 2px"
                      @close="removeCandidateGroup(index)"
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
                  placeholder="表单标识"
                  @input="markDirty"
                />
              </n-form-item>
              <n-form-item label="表单JSON">
                <n-input
                  v-model:value="properties.formJson"
                  type="textarea"
                  :rows="3"
                  placeholder="表单JSON配置"
                  @input="markDirty"
                />
              </n-form-item>
            </template>

            <template v-if="properties.formType === 'external'">
              <n-form-item label="表单URL">
                <n-input
                  v-model:value="properties.formUrl"
                  placeholder="外部表单URL"
                  @input="markDirty"
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
                placeholder="0表示不限制"
                @update:value="markDirty"
              />
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 会签配置Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="multiInstance" tab="会签配置">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item label="多人审批方式">
              <n-radio-group v-model:value="properties.multiInstanceType" @update:value="updateMultiInstance">
                <n-radio-button value="none">
                  单人审批
                </n-radio-button>
                <n-radio-button value="parallel">
                  并行会签
                </n-radio-button>
                <n-radio-button value="sequential">
                  依次审批
                </n-radio-button>
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

              <n-form-item v-if="properties.completionCondition === 'rate'" label="通过比例">
                <div class="pass-rate-config">
                  <!-- 预设快选 -->
                  <div class="pass-rate-presets">
                    <n-button-group size="tiny">
                      <n-button
                        v-for="preset in passRatePresets"
                        :key="preset.value"
                        :type="properties.passRate === preset.value ? 'primary' : 'default'"
                        @click="setPassRate(preset.value)"
                      >
                        {{ preset.label }}
                      </n-button>
                    </n-button-group>
                  </div>
                  <!-- 滑块 + 精确输入 -->
                  <div class="pass-rate-slider-row">
                    <n-slider
                      v-model:value="properties.passRate"
                      :min="10"
                      :max="100"
                      :step="1"
                      :marks="passRateMarks"
                      :format-tooltip="v => `${v}%`"
                      style="flex: 1"
                      @update:value="updateMultiInstance"
                    />
                    <n-input-number
                      v-model:value="properties.passRate"
                      :min="10"
                      :max="100"
                      size="small"
                      style="width: 82px; flex-shrink: 0"
                      @update:value="updateMultiInstance"
                    >
                      <template #suffix>
                        %
                      </template>
                    </n-input-number>
                  </div>
                  <!-- 描述文字 -->
                  <div class="pass-rate-desc">
                    <i class="i-material-symbols:info-outline" style="color:#2080f0;margin-right:4px" />
                    {{ passRateDesc }}
                  </div>
                </div>
              </n-form-item>
            </template>
          </n-form>
        </n-tab-pane>

        <!-- 任务监听器Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="listener" tab="监听器">
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
        </n-tab-pane>

        <!-- 服务任务配置Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:ServiceTask'" name="service" tab="服务配置">
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
                :placeholder="getImplementationPlaceholder()"
                @blur="updateServiceImplementation"
              />
            </n-form-item>
            <n-form-item label="异步执行">
              <n-switch v-model:value="properties.async" @update:value="markDirty" />
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 网关配置Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:ExclusiveGateway'" name="gateway" tab="网关配置">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item label="网关类型">
              <n-radio-group v-model:value="properties.gatewayType" disabled>
                <n-radio value="exclusive">
                  排他网关
                </n-radio>
                <n-radio value="parallel">
                  并行网关
                </n-radio>
                <n-radio value="inclusive">
                  包容网关
                </n-radio>
              </n-radio-group>
            </n-form-item>
            <n-alert type="info" size="small">
              排他网关：只选择一条路径执行<br>
              并行网关：所有路径同时执行<br>
              包容网关：满足条件的路径同时执行
            </n-alert>
          </n-form>
        </n-tab-pane>

        <!-- 流转条件Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:SequenceFlow'" name="sequence" tab="流转条件">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item>
              <n-checkbox v-model:checked="properties.hasCondition" @update:checked="toggleCondition">
                启用条件表达式
              </n-checkbox>
            </n-form-item>

            <template v-if="properties.hasCondition">
              <n-form-item label="条件类型">
                <n-radio-group v-model:value="properties.conditionType" @update:value="updateConditionType">
                  <n-radio-button value="expression">
                    表达式
                  </n-radio-button>
                  <n-radio-button value="script">
                    脚本
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>

              <n-form-item v-if="properties.conditionType === 'expression'" label="条件表达式">
                <n-input
                  v-model:value="properties.condition"
                  type="textarea"
                  :rows="3"
                  placeholder="${approved == true}"
                  @blur="updateCondition"
                />
              </n-form-item>

              <n-form-item v-if="properties.conditionType === 'script'" label="脚本内容">
                <n-input
                  v-model:value="properties.script"
                  type="textarea"
                  :rows="5"
                  placeholder="return approved == true;"
                  @blur="updateCondition"
                />
              </n-form-item>

              <n-form-item v-if="properties.conditionType === 'script'" label="脚本语言">
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
        </n-tab-pane>

        <!-- 结束节点配置Tab -->
        <n-tab-pane v-if="elementType === 'bpmn:EndEvent'" name="end" tab="结束配置">
          <n-form :model="properties" label-placement="top" size="small">
            <n-form-item label="结束类型">
              <n-radio-group v-model:value="properties.endType">
                <n-radio value="terminate">
                  终止流程
                </n-radio>
                <n-radio value="normal">
                  正常结束
                </n-radio>
              </n-radio-group>
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 执行监听器Tab（通用） -->
        <n-tab-pane v-if="showExecutionListener" name="executionListener" tab="执行监听器">
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
        </n-tab-pane>
      </n-tabs>
    </div>

    <!-- 底部固定按钮区 -->
    <div class="panel-footer">
      <n-alert v-if="isDirty" type="warning" size="small" style="margin-bottom: 8px">
        配置已修改，请点击保存按钮生效
      </n-alert>
      <n-button type="primary" block :loading="saving" @click="handleSaveConfig">
        <template #icon>
          <i class="i-material-symbols:save" />
        </template>
        保存配置
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, toRaw, watch } from 'vue'
import { request } from '@/utils/http'
import UserSelectModal from './UserSelectModal.vue'

const props = defineProps({
  element: {
    type: Object,
    default: null,
  },
  modeler: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update'])

// Tab控制状态
const activeTab = ref('basic')

// 保存状态
const isDirty = ref(false)
const saving = ref(false)

// SPEL模板（从API加载）
const spelTemplatesFromApi = ref([])

// 会签比例预设值
const passRatePresets = [
  { label: '过半', value: 50 },
  { label: '2/3', value: 67 },
  { label: '3/4', value: 75 },
  { label: '全部', value: 100 },
]

// 会签比例滑块刻度
const passRateMarks = { 50: '50%', 67: '2/3', 75: '75%', 100: '100%' }

// 使用全局 message 实例（保留供未来使用）
// const _message = window.$message

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
  endType: 'normal',
})

// 选项配置
const taskTypeOptions = [
  { label: '指定审批人', value: 'assignee' },
  { label: '候选用户', value: 'candidateUsers' },
  { label: '候选组(角色)', value: 'candidateGroups' },
]

const assigneeOptions = [
  { label: '发起人', value: '$' + '{initiator}' },
  { label: '发起人上级', value: '$' + '{initiatorLeader}' },
  { label: '部门经理', value: '$' + '{deptManager}' },
  { label: 'HR', value: '$' + '{hr}' },
  { label: '指定用户', value: 'custom' },
  { label: 'SPEL 表达式', value: 'spel' },
]

const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外部表单', value: 'external' },
  { label: '无表单', value: 'none' },
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
  { title: '角色编码', key: 'roleKey' },
]

// SPEL表达式相关
const selectedSpelTemplate = ref('')
const spelValidationError = ref('')

// 标记为未保存状态
function markDirty() {
  isDirty.value = true
}

// 从API加载SPEL模板
async function loadSpelTemplates() {
  try {
    const res = await request.get('/api/flow/spelTemplate/list')
    if (res.code === 200) {
      spelTemplatesFromApi.value = (res.data || []).map(t => ({
        label: t.templateName,
        value: t.expression,
        description: t.description || '',
      }))
    }
  }
  catch (e) {
    console.error('加载SPEL模板失败', e)
  }
}

// 手动保存配置
async function handleSaveConfig() {
  saving.value = true
  try {
    // 执行所有必要的update方法
    updateProperty('id')
    updateProperty('name')
    updateProperty('documentation')

    // 根据元素类型执行特定更新
    if (elementType.value === 'bpmn:StartEvent') {
      updateExtensionProperty('initiator')
      updateExtensionProperty('formKey')
    }

    if (elementType.value === 'bpmn:UserTask') {
      updateUserTaskAssignee()
      updateFormType()
      updateExtensionProperty('priority')
      updateDueDate()
      updateMultiInstance()
      updateTaskListeners()
    }

    if (elementType.value === 'bpmn:ServiceTask') {
      updateServiceImplementation()
      updateAsync()
    }

    if (elementType.value === 'bpmn:SequenceFlow') {
      updateCondition()
    }

    // 更新执行监听器
    if (showExecutionListener.value) {
      updateExecutionListeners()
    }

    // 触发父组件更新
    emit('update')

    isDirty.value = false
    window.$message?.success('配置已保存')
  }
  catch (error) {
    console.error('保存配置失败:', error)
    window.$message?.error('保存失败')
  }
  finally {
    saving.value = false
  }
}

// 组件挂载时加载SPEL模板
onMounted(() => {
  loadSpelTemplates()
})

const completionConditionOptions = [
  { label: '全部通过', value: 'all' },
  { label: '任一通过', value: 'any' },
  { label: '按比例通过', value: 'rate' },
]

// 通过比例描述文字
const passRateDesc = computed(() => {
  const rate = properties.passRate
  if (rate >= 100)
    return '需要所有审批人全部同意才能通过'
  if (rate === 50)
    return `需要超过一半的审批人同意才能通过`
  if (rate === 67)
    return '需要 2/3 以上的审批人同意才能通过'
  if (rate === 75)
    return '需要 3/4 以上的审批人同意才能通过'
  return `需要至少 ${rate}% 的审批人同意才能通过`
})

// 设置预设比例并触发更新
function setPassRate(value) {
  properties.passRate = value
  updateMultiInstance()
}

const taskEventOptions = [
  { label: '创建(create)', value: 'create' },
  { label: '分配(assignment)', value: 'assignment' },
  { label: '完成(complete)', value: 'complete' },
  { label: '删除(delete)', value: 'delete' },
]

const executionEventOptions = [
  { label: '开始(start)', value: 'start' },
  { label: '结束(end)', value: 'end' },
  { label: '执行(take)', value: 'take' },
]

const implementationTypeOptions = [
  { label: 'Java类', value: 'class' },
  { label: '表达式', value: 'expression' },
  { label: '委托表达式', value: 'delegateExpression' },
]

const scriptFormatOptions = [
  { label: 'JavaScript', value: 'javascript' },
  { label: 'Groovy', value: 'groovy' },
  { label: 'JUEL', value: 'juel' },
]

// 监听元素变化，加载属性
watch(() => props.element, (newElement) => {
  if (newElement) {
    loadElementProperties(toRaw(newElement))
    // 自动修复：老模型可能缺少 loopCardinality，打开时自动补全
    nextTick(() => {
      if (properties.multiInstanceType !== 'none' && rawElement.value?.type === 'bpmn:UserTask') {
        const bo = rawElement.value.businessObject
        if (bo?.loopCharacteristics && !bo.loopCharacteristics.loopCardinality) {
          updateMultiInstance()
        }
      }
    })
    // 切换节点时回到第一个Tab
    activeTab.value = 'basic'
  }
}, { immediate: true })

let spelSaveTimer = null
watch(() => properties.assigneeExpr, (newVal) => {
  if (properties.assignee !== 'spel' || !newVal)
    return
  clearTimeout(spelSaveTimer)
  spelSaveTimer = setTimeout(() => {
    updateUserTaskAssignee()
  }, 500)
})

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
          id: Number.parseInt(match[1]),
          nickName: properties.assigneeUserName,
        }]
      }
      else {
        currentSelectedUsers.value = []
      }
    }
    else {
      currentSelectedUsers.value = []
    }
  }
  else if (type === 'candidateUsers') {
    userSelectTitle.value = '选择候选用户'
    userSelectMultiple.value = true
    // 回显已选候选用户
    if (properties.candidateUsers.length > 0) {
      currentSelectedUsers.value = properties.candidateUsers.map((id, index) => ({
        id: Number.parseInt(id),
        nickName: properties.candidateUserNames[index] || '',
      }))
    }
    else {
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
      properties.assigneeExpr = '$' + `{user_${user.id}}`
      properties.assigneeUserName = user.nickName || user.userName
      updateUserTaskAssignee()
    }
  }
  else if (userSelectType.value === 'candidateUsers') {
    const userList = Array.isArray(users) ? users : [users]
    userList.forEach((user) => {
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
    checkedRoleKeys.value = properties.candidateGroups.map(id => Number.parseInt(id))
  }
  else {
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
      params: { pageNum: 1, pageSize: 1000 },
    })
    if (res.code === 200 && res.data?.records) {
      roleList.value = res.data.records
    }
  }
  catch (error) {
    console.error('加载角色列表失败:', error)
  }
  finally {
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
  selectedRoles.forEach((role) => {
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
  if (!bo)
    return

  // 基础属性
  properties.id = bo.id || ''
  properties.name = bo.name || ''

  // 文档
  const docs = bo.documentation || []
  properties.documentation = docs.length > 0 ? docs[0].text : ''

  // 根据元素类型加载不同属性
  if (element.type === 'bpmn:UserTask') {
    loadUserTaskProperties(bo)
  }
  else if (element.type === 'bpmn:ServiceTask') {
    loadServiceTaskProperties(bo)
  }
  else if (element.type === 'bpmn:SequenceFlow') {
    loadSequenceFlowProperties(bo)
  }
  else if (element.type === 'bpmn:StartEvent') {
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

  // 读取审批人类型标识（用于区分 SPEL 表达式）
  const assigneeType = bo.assigneeType ?? attrs['flowable:assigneeType'] ?? ''
  // 读取 SPEL 表达式模板（用于回显模板选择）
  const spelTemplate = bo.spelTemplate ?? attrs['flowable:spelTemplate'] ?? ''

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

    // 优先使用 assigneeType 标识来判断类型
    if (assigneeType === 'spel') {
      properties.assignee = 'spel'
      properties.assigneeExpr = assignee
    }
    // 判断是否是自定义用户 ID 表达式：${user_123}
    else if (assignee.startsWith('$' + '{user_')) {
      properties.assignee = 'custom'
      properties.assigneeExpr = assignee
    }
    // 预定义表达式：${initiator}, ${initiatorLeader}, ${deptManager}, ${hr}
    else if (['$' + '{initiator}', '$' + '{initiatorLeader}', '$' + '{deptManager}', '$' + '{hr}'].includes(assignee)) {
      properties.assignee = assignee
    }
    // 其他 ${} 表达式默认当作 SPEL（向后兼容旧数据）
    else if (assignee.startsWith('$' + '{') && assignee.endsWith('}')) {
      properties.assignee = 'spel'
      properties.assigneeExpr = assignee
    }
    else {
      properties.assignee = assignee
    }
  }
  else if (assigneeType === 'spel') {
    properties.taskType = 'assignee'
    properties.assignee = 'spel'
  }
  else if (candidateUsers) {
    properties.taskType = 'candidateUsers'
    properties.candidateUsers = candidateUsers.split(',').filter(Boolean)
    properties.candidateUserNames = candidateUserNames ? candidateUserNames.split(',').filter(Boolean) : []
  }
  else if (candidateGroups) {
    properties.taskType = 'candidateGroups'
    properties.candidateGroups = candidateGroups.split(',').filter(Boolean)
    properties.candidateGroupNames = candidateGroupNames ? candidateGroupNames.split(',').filter(Boolean) : []
  }

  if (assigneeType === 'spel') {
    selectedSpelTemplate.value = spelTemplate
  }

  // 表单配置 - 直接读 bo 上的属性
  properties.formKey = bo.formKey ?? attrs['flowable:formKey'] ?? ''
  properties.formJson = bo.formJson ?? attrs['flowable:formJson'] ?? ''
  properties.formUrl = bo.formUrl ?? attrs['flowable:formUrl'] ?? ''

  // 表单类型判断
  if (properties.formUrl) {
    properties.formType = 'external'
  }
  else if (properties.formKey || properties.formJson) {
    properties.formType = 'dynamic'
  }
  else {
    properties.formType = 'none'
  }

  // 优先级和截止日期
  const priorityVal = bo.priority ?? attrs['flowable:priority']
  properties.priority = priorityVal != null ? Number.parseInt(priorityVal) : 50
  const dueDateVal = bo.dueDate ?? attrs['flowable:dueDate']
  if (dueDateVal) {
    // 格式可能是 P3D (ISO 8601 duration) 或纯数字
    const match = String(dueDateVal).match(/(\d+)/)
    properties.dueDate = match ? Number.parseInt(match[1]) : 0
  }
  else {
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
      }
      else if (condition.includes('nrOfCompletedInstances >= 1')) {
        properties.completionCondition = 'any'
      }
      else if (condition.includes('/ nrOfInstances')) {
        properties.completionCondition = 'rate'
        const match = condition.match(/>= *([\d.]+)/)
        if (match) {
          properties.passRate = Math.round(Number.parseFloat(match[1]) * 100)
        }
      }
    }
    else {
      properties.completionCondition = 'all'
    }
  }
  else {
    properties.multiInstanceType = 'none'
    properties.completionCondition = 'all'
    properties.passRate = 100
  }

  // 任务监听器
  properties.taskListeners = []
  const extensionElements = bo.extensionElements?.values || []
  extensionElements.forEach((ext) => {
    if (ext.$type === 'flowable:TaskListener') {
      properties.taskListeners.push({
        event: ext.event || 'create',
        class: ext.class || '',
      })
    }
  })

  // 执行监听器
  properties.executionListeners = []
  extensionElements.forEach((ext) => {
    if (ext.$type === 'flowable:ExecutionListener') {
      properties.executionListeners.push({
        event: ext.event || 'start',
        class: ext.class || '',
      })
    }
  })
}

// 加载服务任务属性
function loadServiceTaskProperties(bo) {
  const attrs = bo.$attrs || {}

  // moddleExtensions 注册后直接读 bo.class / bo.expression / bo.delegateExpression
  const classVal = bo.class ?? bo['flowable:class'] ?? attrs['flowable:class']
  const exprVal = bo.expression ?? bo['flowable:expression'] ?? attrs['flowable:expression']
  const delegateVal = bo.delegateExpression ?? bo['flowable:delegateExpression'] ?? attrs['flowable:delegateExpression']

  if (classVal) {
    properties.implementationType = 'class'
    properties.implementation = classVal
  }
  else if (exprVal) {
    properties.implementationType = 'expression'
    properties.implementation = exprVal
  }
  else if (delegateVal) {
    properties.implementationType = 'delegateExpression'
    properties.implementation = delegateVal
  }
  else {
    properties.implementationType = 'class'
    properties.implementation = ''
  }
  properties.async = bo.async ?? attrs['flowable:async'] ?? false
}

// 加载序列流属性
function loadSequenceFlowProperties(bo) {
  const conditionExpression = bo.conditionExpression
  if (conditionExpression) {
    properties.hasCondition = true
    properties.condition = conditionExpression.body || ''
    properties.conditionType = 'expression'
  }
  else {
    properties.hasCondition = false
  }

  // 检查是否默认流
  const source = bo.sourceRef
  if (source && source.default) {
    properties.isDefault = source.default.id === bo.id
  }
  else {
    properties.isDefault = false
  }
}

// 加载开始事件属性
function loadStartEventProperties(bo) {
  const attrs = bo.$attrs || {}
  // moddleExtensions 注册后直接读 bo.initiator
  properties.initiator = bo.initiator ?? bo['flowable:initiator'] ?? attrs['flowable:initiator'] ?? 'initiator'
  properties.formKey = bo.formKey ?? bo['flowable:formKey'] ?? attrs['flowable:formKey'] ?? ''
}

// 更新扩展属性（flowable 命名空间属性，通过 moddleExtensions 注册后直接用属性名）
function updateExtensionProperty(prop) {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const value = properties[prop]
  // 使用 flowable: 前缀写入，bpmn-js 会根据 moddleExtensions 映射
  modeling.updateProperties(rawElement.value, {
    [`flowable:${prop}`]: value !== '' ? value : null,
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
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')

  if (properties.formType === 'none') {
    properties.formKey = ''
    properties.formJson = ''
    properties.formUrl = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formKey': null,
      'flowable:formJson': null,
      'flowable:formUrl': null,
    })
  }
  else if (properties.formType === 'external') {
    properties.formKey = ''
    properties.formJson = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formKey': null,
      'flowable:formJson': null,
      'flowable:formUrl': properties.formUrl, // ✅ 保存外部表单URL
    })
  }
  else if (properties.formType === 'dynamic') {
    properties.formUrl = ''
    modeling.updateProperties(rawElement.value, {
      'flowable:formUrl': null,
    })
  }
}

// 更新用户任务审批人
function updateUserTaskAssignee() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  let value = properties.assignee
  let assigneeType = null

  // 处理不同的审批人类型
  if (properties.assignee === 'custom') {
    value = properties.assigneeExpr
    assigneeType = 'custom'
  }
  else if (properties.assignee === 'spel') {
    value = properties.assigneeExpr
    assigneeType = 'spel'
  }

  const element = rawElement.value

  modeling.updateProperties(element, {
    'flowable:assignee': value || null,
    'flowable:assigneeType': assigneeType,
    'flowable:assigneeName': properties.assigneeUserName || null,
    'flowable:spelTemplate': selectedSpelTemplate.value || null,
    'flowable:candidateUsers': null,
    'flowable:candidateUserNames': null,
    'flowable:candidateGroups': null,
    'flowable:candidateGroupNames': null,
  })
  emit('update')
}

// 更新候选用户
function updateCandidateUsers() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const element = rawElement.value
  const usersStr = properties.candidateUsers.join(',')
  const userNamesStr = properties.candidateUserNames.join(',')

  modeling.updateProperties(element, {
    'flowable:assignee': null,
    'flowable:assigneeType': null,
    'flowable:assigneeName': null,
    'flowable:spelTemplate': null,
    'flowable:candidateUsers': usersStr || null,
    'flowable:candidateUserNames': userNamesStr || null,
    'flowable:candidateGroups': null,
    'flowable:candidateGroupNames': null,
  })
  emit('update')
}

// 更新候选组
function updateCandidateGroups() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const element = rawElement.value
  const groupsStr = properties.candidateGroups.join(',')
  const groupNamesStr = properties.candidateGroupNames.join(',')

  modeling.updateProperties(element, {
    'flowable:assignee': null,
    'flowable:assigneeType': null,
    'flowable:assigneeName': null,
    'flowable:spelTemplate': null,
    'flowable:candidateUsers': null,
    'flowable:candidateUserNames': null,
    'flowable:candidateGroups': groupsStr || null,
    'flowable:candidateGroupNames': groupNamesStr || null,
  })
  emit('update')
}

// 更新截止日期
function updateDueDate() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(rawElement.value, {
    'flowable:dueDate': properties.dueDate > 0 ? `P${properties.dueDate}D` : null,
  })
}

// 更新多实例配置
function updateMultiInstance() {
  if (!rawElement.value || !props.modeler)
    return

  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')
  const bo = rawElement.value.businessObject

  if (properties.multiInstanceType === 'none') {
    modeling.updateProperties(rawElement.value, { loopCharacteristics: null })
    emit('update')
    return
  }

  // 构建完成条件
  let completionConditionStr = ''
  if (properties.completionCondition === 'all')
    completionConditionStr = '$' + '{nrOfCompletedInstances == nrOfInstances}'
  else if (properties.completionCondition === 'any')
    completionConditionStr = '$' + '{nrOfCompletedInstances >= 1}'
  else if (properties.completionCondition === 'rate')
    completionConditionStr = '$' + `{nrOfCompletedInstances / nrOfInstances >= ${(properties.passRate / 100).toFixed(2)}}`

  const loopCardinalityObj = moddle.create('bpmn:FormalExpression', { body: '$' + '{nrOfInstances}' })
  const completionCondObj = completionConditionStr
    ? moddle.create('bpmn:FormalExpression', { body: completionConditionStr })
    : null

  // 确保 loopCardinality 被正确序列化到 XML：
  // updateModdleProperties 适用于修改已有的 moddle 子元素
  if (bo.loopCharacteristics) {
    modeling.updateModdleProperties(rawElement.value, bo.loopCharacteristics, {
      isSequential: properties.multiInstanceType === 'sequential',
      loopCardinality: loopCardinalityObj,
      completionCondition: completionCondObj,
    })
  }
  else {
    // 首次创建：用 updateProperties 设置整个 loopCharacteristics
    const lc = moddle.create('bpmn:MultiInstanceLoopCharacteristics', {
      isSequential: properties.multiInstanceType === 'sequential',
      loopCardinality: loopCardinalityObj,
      completionCondition: completionCondObj,
    })
    modeling.updateProperties(rawElement.value, { loopCharacteristics: lc })
  }

  emit('update')
}

// 添加任务监听器
function addTaskListener() {
  properties.taskListeners.push({
    event: 'create',
    class: '',
  })
}

// 移除任务监听器
function removeTaskListener(index) {
  properties.taskListeners.splice(index, 1)
  updateTaskListeners()
}

// 更新任务监听器
function updateTaskListeners() {
  if (!rawElement.value || !props.modeler)
    return

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
      class: l.class,
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
    class: '',
  })
}

// 移除执行监听器
function removeExecutionListener(index) {
  properties.executionListeners.splice(index, 1)
}

// 更新执行监听器
function updateExecutionListeners() {
  if (!rawElement.value || !props.modeler)
    return

  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')
  const bo = rawElement.value.businessObject

  // 保留任务监听器，合并执行监听器
  const existingExtValues = bo.extensionElements?.values || []
  const taskListeners = existingExtValues.filter(v => v.$type === 'flowable:TaskListener')

  const executionListeners = properties.executionListeners
    .filter(l => l.class)
    .map(l => moddle.create('flowable:ExecutionListener', {
      event: l.event,
      class: l.class,
    }))

  const allValues = [...taskListeners, ...executionListeners]

  const extensionElements = moddle.create('bpmn:ExtensionElements', { values: allValues })
  modeling.updateProperties(rawElement.value, { extensionElements })
  emit('update')
}

// 更新服务任务实现
function updateServiceImplementation() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const updateProps = {
    'flowable:class': null,
    'flowable:expression': null,
    'flowable:delegateExpression': null,
  }

  const key = `flowable:${properties.implementationType}`
  updateProps[key] = properties.implementation || null

  modeling.updateProperties(rawElement.value, updateProps)
  emit('update')
}

// 更新异步
function updateAsync() {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  modeling.updateProperties(rawElement.value, {
    'flowable:async': properties.async,
  })
  emit('update')
}

// 获取实现方式占位符
function getImplementationPlaceholder() {
  const placeholders = {
    class: 'com.example.MyServiceTask',
    expression: '${' + 'myService.execute()}',
    delegateExpression: '${' + 'myServiceDelegate}',
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
  if (!rawElement.value || !props.modeler)
    return

  const moddle = props.modeler.get('moddle')
  const modeling = props.modeler.get('modeling')

  if (properties.conditionType === 'expression' && properties.condition) {
    const expr = moddle.create('bpmn:FormalExpression', { body: properties.condition })
    modeling.updateProperties(rawElement.value, { conditionExpression: expr })
  }
  else if (properties.conditionType === 'script' && properties.script) {
    const expr = moddle.create('bpmn:FormalExpression', {
      body: properties.script,
      language: properties.scriptFormat,
    })
    modeling.updateProperties(rawElement.value, { conditionExpression: expr })
  }
  else {
    modeling.updateProperties(rawElement.value, { conditionExpression: null })
  }
  emit('update')
}

// 切换默认路径
function toggleDefault(checked) {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const bo = rawElement.value.businessObject

  if (checked) {
    // 设置为默认流
    modeling.updateProperties(bo.sourceRef, {
      default: rawElement.value,
    })
  }
  else {
    // 取消默认流
    modeling.updateProperties(bo.sourceRef, {
      default: null,
    })
  }
}

// 自定义审批人表达式
// 应用 SPEL 表达式模板
function applySpelTemplate(value) {
  if (value) {
    properties.assigneeExpr = value
    selectedSpelTemplate.value = value
    updateUserTaskAssignee()
  }
}

// 验证 SPEL 表达式
function validateSpelExpression() {
  spelValidationError.value = ''

  if (!properties.assigneeExpr || !properties.assigneeExpr.trim()) {
    return
  }

  const expr = properties.assigneeExpr.trim()

  // 检查是否包含 ${} 包裹
  if (!expr.startsWith('$' + '{') || !expr.endsWith('}')) {
    spelValidationError.value = 'SPEL 表达式必须使用 $' + '{} 包裹'
    return
  }

  // 检查括号匹配
  const openCount = (expr.match(/\(/g) || []).length
  const closeCount = (expr.match(/\)/g) || []).length
  if (openCount !== closeCount) {
    spelValidationError.value = '括号不匹配，请检查表达式语法'
    return
  }

  // 验证通过，更新到节点
  updateUserTaskAssignee()
}

// 更新基础属性（id/name/documentation）时也触发 emit
function updateProperty(prop) {
  if (!rawElement.value || !props.modeler)
    return

  const modeling = props.modeler.get('modeling')
  const moddle = props.modeler.get('moddle')

  if (prop === 'id') {
    modeling.updateProperties(rawElement.value, { id: properties.id })
  }
  else if (prop === 'name') {
    modeling.updateProperties(rawElement.value, { name: properties.name })
  }
  else if (prop === 'documentation') {
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
  display: flex;
  flex-direction: column;
}

/* Tab横向滚动容器 */
.tabs-wrapper {
  border-bottom: 1px solid var(--n-border-color);
  margin-bottom: 8px;
}

.config-tabs {
  width: 100%;
}

/* Tab导航栏可滚动 + 显示滚动条 */
.config-tabs :deep(.n-tabs-nav) {
  overflow-x: auto !important;
  overflow-y: hidden !important;
  white-space: nowrap !important;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

/* 自定义滚动条样式（Webkit） */
.config-tabs :deep(.n-tabs-nav::-webkit-scrollbar) {
  height: 6px;
}

.config-tabs :deep(.n-tabs-nav::-webkit-scrollbar-track) {
  background: transparent;
}

.config-tabs :deep(.n-tabs-nav::-webkit-scrollbar-thumb) {
  background: #d1d5db;
  border-radius: 3px;
}

.config-tabs :deep(.n-tabs-nav::-webkit-scrollbar-thumb:hover) {
  background: #9ca3af;
}

/* Tab项不换行 */
.config-tabs :deep(.n-tabs-tab) {
  white-space: nowrap;
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border, #e2e8f0);
  background: var(--surface, #ffffff);
  position: sticky;
  bottom: 0;
  margin-top: 8px;
  flex-shrink: 0;
}

.listener-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.listener-item {
  margin-bottom: 8px;
}

:deep(.n-tabs-pane-wrapper) {
  overflow-y: auto;
}

:deep(.n-tab-pane) {
  padding: 8px 0;
}

:deep(.n-form-item) {
  margin-bottom: 12px;
}

:deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.pass-rate-config {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.pass-rate-presets {
  display: flex;
}

.pass-rate-slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pass-rate-desc {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  background: #f0f7ff;
  border-radius: 4px;
  padding: 5px 8px;
}
</style>

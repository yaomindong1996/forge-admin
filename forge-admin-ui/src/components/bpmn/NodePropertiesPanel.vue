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

    <Teleport to="body">
      <n-modal
        v-model:show="showNodeFormDesigner"
        preset="card"
        :title="nodeFormDesignerTitle"
        style="width: min(1180px, 96vw); height: 90vh"
        content-style="height: calc(90vh - 66px); padding: 16px;"
        :mask-closable="false"
      >
        <FlowFormCreateDesigner
          ref="nodeFormDesignerRef"
          v-model="nodeFormDesignerSchema"
          height="calc(90vh - 156px)"
          @save="handleSaveNodeFormSchema"
        />
      </n-modal>
    </Teleport>

    <Teleport to="body">
      <n-modal
        v-model:show="showNodeFormPreview"
        preset="card"
        :title="nodeFormPreviewTitle"
        style="width: min(780px, 92vw)"
      >
        <FlowFormCreateRenderer
          :schema="nodeFormPreviewSchema"
          read-only
        />
      </n-modal>
    </Teleport>

    <!-- Tab分隔配置 - 横向滚动 -->
    <div ref="tabsWrapperRef" class="tabs-wrapper">
      <div class="tabs-toolbar">
        <n-button
          quaternary
          circle
          size="small"
          :disabled="!canGoPrevTab"
          aria-label="上一个配置页签"
          @click="switchRelativeTab(-1)"
        >
          <template #icon>
            <i class="i-material-symbols:chevron-left" />
          </template>
        </n-button>
        <span class="tabs-position">{{ activeTabPositionText }}</span>
        <n-button
          quaternary
          circle
          size="small"
          :disabled="!canGoNextTab"
          aria-label="下一个配置页签"
          @click="switchRelativeTab(1)"
        >
          <template #icon>
            <i class="i-material-symbols:chevron-right" />
          </template>
        </n-button>
      </div>
      <div class="tabs-shell" :class="{ 'has-next': canGoNextTab }">
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
                    <div v-if="formVariableOptions.length > 0" class="field-catalog-tip">
                      可直接选择绑定表单中的用户、部门或负责人字段作为审批人变量。
                    </div>
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
                          <div v-if="option.description" style="font-size: 12px; color: var(--text-tertiary); margin-top: 4px">
                            {{ option.description }}
                          </div>
                        </div>
                      </template>
                    </n-select>
                  </n-form-item>

                  <n-form-item label="SPEL 表达式">
                    <n-space vertical size="small" style="width: 100%">
                      <n-select
                        v-model:value="selectedSpelVariable"
                        :options="variableCatalogOptions"
                        placeholder="插入表单字段或系统变量"
                        filterable
                        clearable
                        @update:value="insertSpelVariable"
                      />
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
                    <n-select
                      v-if="formVariableOptions.length > 0"
                      :value="null"
                      :options="variableCatalogOptions"
                      placeholder="选择表单字段作为候选用户变量"
                      filterable
                      clearable
                      @update:value="field => applyCandidateVariable('users', field)"
                    />
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
                    <n-select
                      v-if="formVariableOptions.length > 0"
                      :value="null"
                      :options="variableCatalogOptions"
                      placeholder="选择表单字段作为候选组变量"
                      filterable
                      clearable
                      @update:value="field => applyCandidateVariable('groups', field)"
                    />
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
                <n-form-item label="引用表单">
                  <n-select
                    v-model:value="properties.formKey"
                    :options="formDefinitionOptions"
                    :loading="formDefinitionLoading"
                    placeholder="选择已有表单或输入表单Key"
                    clearable
                    filterable
                    tag
                    @update:value="handleFormKeyChange"
                  />
                </n-form-item>

                <n-form-item label="节点表单">
                  <div class="node-form-builder-card">
                    <div class="node-form-builder-main">
                      <div class="node-form-builder-icon">
                        <i class="i-material-symbols:edit-document" />
                      </div>
                      <div class="node-form-builder-copy">
                        <div class="node-form-builder-title">
                          {{ nodeFormBuilderTitle }}
                        </div>
                        <div class="node-form-builder-desc">
                          {{ nodeFormBuilderDesc }}
                        </div>
                      </div>
                    </div>
                    <n-space size="small" class="node-form-builder-actions">
                      <n-button size="small" type="primary" @click="openNodeFormDesigner">
                        <template #icon>
                          <i class="i-material-symbols:edit-document" />
                        </template>
                        在线设计
                      </n-button>
                      <n-button
                        size="small"
                        :disabled="!canPreviewNodeForm"
                        @click="openNodeFormPreview"
                      >
                        <template #icon>
                          <i class="i-material-symbols:visibility" />
                        </template>
                        预览
                      </n-button>
                      <n-button
                        size="small"
                        :disabled="!properties.formJson"
                        @click="clearNodeInlineForm"
                      >
                        <template #icon>
                          <i class="i-material-symbols:delete" />
                        </template>
                        清空设计
                      </n-button>
                    </n-space>
                  </div>
                </n-form-item>

                <n-collapse class="node-form-json-collapse">
                  <n-collapse-item title="高级 JSON 配置" name="formJson">
                    <n-form-item label="表单JSON">
                      <n-input
                        v-model:value="properties.formJson"
                        type="textarea"
                        :autosize="{ minRows: 4, maxRows: 10 }"
                        placeholder="由在线设计器自动生成；仅开发调试时手动修改"
                        @input="markDirty"
                      />
                    </n-form-item>
                  </n-collapse-item>
                </n-collapse>
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

          <!-- 办理控制Tab -->
          <n-tab-pane v-if="elementType === 'bpmn:UserTask'" name="actionControl" tab="办理控制">
            <n-form :model="properties" label-placement="top" size="small">
              <div class="action-control-group">
                <div class="action-control-title">
                  可执行动作
                </div>
                <div class="action-switch-list">
                  <div
                    v-for="item in approvalActionOptions"
                    :key="item.key"
                    class="action-switch-row"
                  >
                    <div class="action-switch-main">
                      <i :class="item.icon" />
                      <span>{{ item.label }}</span>
                    </div>
                    <n-switch
                      v-model:value="properties[item.key]"
                      size="small"
                      @update:value="markDirty"
                    />
                  </div>
                </div>
              </div>

              <div class="action-control-group">
                <div class="action-control-title">
                  办理要求
                </div>
                <div class="action-switch-list">
                  <div class="action-switch-row">
                    <div class="action-switch-main">
                      <i class="i-material-symbols:rate-review" />
                      <span>审批意见必填</span>
                    </div>
                    <n-switch
                      v-model:value="properties.requireComment"
                      size="small"
                      @update:value="markDirty"
                    />
                  </div>
                  <div class="action-switch-row">
                    <div class="action-switch-main">
                      <i class="i-material-symbols:draw" />
                      <span>签名必填</span>
                    </div>
                    <n-switch
                      v-model:value="properties.requireSignature"
                      size="small"
                      @update:value="markDirty"
                    />
                  </div>
                </div>
              </div>
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
              <n-form-item>
                <n-checkbox
                  :checked="properties.flowableType === 'cc'"
                  @update:checked="toggleCarbonCopyService"
                >
                  作为抄送节点
                </n-checkbox>
              </n-form-item>

              <template v-if="properties.flowableType === 'cc'">
                <n-form-item label="抄送来源">
                  <div class="carbon-copy-source-switch">
                    <button
                      v-for="item in carbonCopyReceiverTypeOptions"
                      :key="item.value"
                      type="button"
                      class="carbon-copy-source-button"
                      :class="{ active: properties.ccReceiverType === item.value }"
                      @click="handleCarbonCopyReceiverTypeChange(item.value)"
                    >
                      {{ item.label }}
                    </button>
                  </div>
                </n-form-item>

                <n-form-item v-if="properties.ccReceiverType === 'users'" label="抄送人">
                  <n-space vertical size="small" style="width: 100%">
                    <n-button
                      type="primary"
                      dashed
                      block
                      @click="openUserSelect('carbonCopyUsers')"
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
                        @close="removeCarbonCopyUser(index)"
                      >
                        {{ name }}
                      </n-tag>
                    </div>
                  </n-space>
                </n-form-item>

                <n-form-item v-else-if="properties.ccReceiverType === 'roles'" label="抄送角色">
                  <n-select
                    :value="properties.candidateGroups"
                    :options="carbonCopyRoleOptions"
                    :loading="roleLoading"
                    placeholder="请选择角色"
                    multiple
                    clearable
                    filterable
                    remote
                    @focus="loadRoleList"
                    @search="loadRoleList"
                    @update:value="handleCarbonCopyRolesChange"
                  />
                </n-form-item>

                <template v-else>
                  <n-form-item label="表达式返回内容">
                    <n-select
                      v-model:value="properties.ccExpressionTarget"
                      :options="carbonCopyExpressionTargetOptions"
                      @update:value="updateCarbonCopyExpression"
                    />
                  </n-form-item>
                  <n-form-item label="抄送表达式">
                    <n-input
                      v-model:value="properties.ccExpression"
                      type="textarea"
                      :autosize="{ minRows: 3, maxRows: 5 }"
                      placeholder="${ccUserIds} 或 ${flowSpelService.findUsersByRole('general_manager')}"
                      @blur="updateCarbonCopyExpression"
                    />
                  </n-form-item>
                  <n-alert type="info" size="small" style="margin-bottom: 12px">
                    表达式可返回单个 ID、逗号分隔字符串或数组；选择“返回角色”时系统会按角色编码解析抄送人。
                  </n-alert>
                </template>

                <n-alert type="info" size="small" style="margin-bottom: 12px">
                  流程到达该节点时会发送抄送消息，不需要审批，发送后自动流转到下一节点。
                </n-alert>
              </template>

              <template v-if="properties.flowableType === 'cc'">
                <n-collapse class="advanced-condition-collapse">
                  <n-collapse-item title="开发者高级配置（可选）" name="carbonCopyAdvanced">
                    <n-form-item label="实现方式">
                      <n-select
                        v-model:value="properties.implementationType"
                        :options="carbonCopyImplementationTypeOptions"
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
                  </n-collapse-item>
                </n-collapse>
              </template>
              <template v-else>
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
              </template>
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
                  设置这条线的流转条件
                </n-checkbox>
              </n-form-item>

              <template v-if="properties.hasCondition">
                <n-form-item label="这条线什么时候走">
                  <n-radio-group
                    v-model:value="properties.conditionPreset"
                    class="approval-result-options"
                    @update:value="updateConditionPreset"
                  >
                    <n-radio
                      v-for="item in approvalResultConditionOptions"
                      :key="item.value"
                      :value="item.value"
                      class="approval-result-option"
                    >
                      <div class="approval-result-card" :class="{ active: properties.conditionPreset === item.value }">
                        <div class="approval-result-title">
                          <i :class="item.icon" />
                          <span>{{ item.label }}</span>
                        </div>
                        <div class="approval-result-desc">
                          {{ item.desc }}
                        </div>
                      </div>
                    </n-radio>
                  </n-radio-group>
                </n-form-item>

                <n-form-item v-if="properties.conditionPreset === 'custom'" label="按业务字段判断">
                  <n-space vertical size="small" style="width: 100%">
                    <div class="condition-builder">
                      <n-select
                        v-model:value="conditionBuilder.field"
                        :options="variableCatalogOptions"
                        placeholder="选择字段"
                        filterable
                        clearable
                      />
                      <n-select
                        v-model:value="conditionBuilder.operator"
                        :options="conditionOperatorOptions"
                        class="condition-operator"
                      />
                      <n-input
                        v-model:value="conditionBuilder.value"
                        placeholder="比较值"
                        clearable
                        @keydown.enter="applyConditionBuilder"
                      />
                      <n-button type="primary" secondary @click="applyConditionBuilder">
                        生成
                      </n-button>
                    </div>
                    <div class="field-catalog-tip">
                      例如：采购金额大于 10000 时走经理审批；合同类型等于框架合同时走法务审核。
                    </div>
                  </n-space>
                </n-form-item>

                <n-collapse class="advanced-condition-collapse">
                  <n-collapse-item title="开发者高级配置（可选）" name="advancedCondition">
                    <n-form-item label="高级类型">
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
                        placeholder="${approvalResult == 'approve'}"
                        @blur="syncConditionPresetFromCondition(); updateCondition()"
                      />
                    </n-form-item>

                    <n-form-item v-if="properties.conditionType === 'script'" label="脚本内容">
                      <n-input
                        v-model:value="properties.script"
                        type="textarea"
                        :rows="5"
                        placeholder="return approvalResult == 'approve';"
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
                  </n-collapse-item>
                </n-collapse>
              </template>

              <n-form-item>
                <n-checkbox v-model:checked="properties.isDefault" @update:checked="toggleDefault">
                  其它情况默认走这条线
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

<script>
import { nodePropertiesPanelLocalComponents } from './nodePropertiesPanelLocalComponents'
import { useNodePropertiesPanel } from './composables/useNodePropertiesPanel'

export default {
  name: 'NodePropertiesPanel',
  components: {
    ...nodePropertiesPanelLocalComponents,
  },
  props: {
  element: {
    type: Object,
    default: null,
  },
  modeler: {
    type: Object,
    default: null,
  },
  fieldCatalog: {
    type: Array,
    default: () => [],
  },
},
  emits: ['update'],
  setup(props, { emit }) {
    return useNodePropertiesPanel(props, emit)
  },
}
</script>

<style scoped src="./nodePropertiesPanel.css"></style>

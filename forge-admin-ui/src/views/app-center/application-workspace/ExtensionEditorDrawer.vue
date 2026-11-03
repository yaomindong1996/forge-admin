<template>
  <div v-if="show" class="extension-editor-workspace">
    <header class="workspace-header">
      <div class="workspace-header__identity">
        <n-button quaternary circle aria-label="返回增强列表" @click="closeWorkspace">
          <template #icon>
            <i class="i-material-symbols:arrow-back-rounded" />
          </template>
        </n-button>
        <div>
          <div class="workspace-header__eyebrow">
            增强配置
          </div>
          <h2>{{ isEdit ? (form.extensionName || '编辑增强') : '新建增强' }}</h2>
          <p>{{ isEdit ? `编辑锁有效至 ${form.lockExpireTime || '稍后'}；保存追加新版本，不覆盖运行中版本。` : '选择能力后配置作用范围与内容，测试通过后再启用。' }}</p>
        </div>
      </div>
      <div class="workspace-header__meta">
        <n-tag v-if="form.status" size="small" :bordered="false" :type="statusTone">
          {{ statusLabel }}
        </n-tag>
        <span class="workspace-version-hint">{{ isEdit ? `草稿 v${extension?.draftVersion || '-'}` : '保存后生成 v1 草稿' }}</span>
      </div>
    </header>

    <div class="workspace-body">
      <div class="workspace-main">
        <section v-if="!isEdit && showTypeGuide" class="extension-type-guide">
          <div class="type-guide-heading">
            <strong>先选择你要实现的效果</strong>
            <span>选择后再配置作用对象、触发时机和具体内容。</span>
          </div>
          <div class="type-guide-grid">
            <button
              v-for="item in extensionTypeChoices"
              :key="item.value"
              type="button"
              class="type-guide-card"
              :class="{ active: form.extensionType === item.value }"
              @click="selectExtensionType(item.value)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
              <small>{{ item.scene }}</small>
            </button>
          </div>
          <n-alert type="info" :show-icon="false" class="sql-boundary-alert">
            当前不开放任意 SQL 文本增强。数据库逻辑请先使用业务规则或 Java 服务增强。
          </n-alert>
        </section>

        <div v-if="!showTypeGuide || isEdit" class="selected-type-bar">
          <div class="selected-extension-type">
            <DictTag dict-type="ai_business_extension_type" :value="form.extensionType" :bordered="false" />
            <span>{{ selectedExtensionType?.description }}</span>
          </div>
          <n-button v-if="!isEdit" text type="primary" size="tiny" @click="showTypeGuide = true">
            更换类型
          </n-button>
        </div>

        <n-form
          v-show="isEdit || !showTypeGuide"
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="top"
          class="editor-form"
        >
          <section class="editor-stage">
            <header class="editor-stage__header">
              <i>1</i>
              <div>
                <strong>身份与范围</strong>
                <span>这条增强叫什么、作用在哪个对象或入口</span>
              </div>
            </header>
            <div class="editor-stage__body form-grid two-columns">
              <n-form-item label="增强名称" path="extensionName">
                <n-input v-model:value="form.extensionName" placeholder="例如：客户提交校验" />
              </n-form-item>
              <n-form-item label="增强编码" path="extensionCode">
                <n-input v-model:value="form.extensionCode" :disabled="isEdit" placeholder="validate_customer" />
              </n-form-item>
              <n-form-item label="业务对象">
                <n-select
                  v-model:value="form.objectId"
                  clearable
                  filterable
                  :options="objectOptions"
                  placeholder="可不选，表示应用级"
                />
              </n-form-item>
              <n-form-item label="页面入口">
                <n-select
                  v-model:value="form.entryId"
                  clearable
                  filterable
                  :options="entryOptions"
                  placeholder="可进一步限定到入口"
                />
              </n-form-item>
            </div>
          </section>

          <section class="editor-stage">
            <header class="editor-stage__header">
              <i>2</i>
              <div>
                <strong>触发时机</strong>
                <span>同一条增强只绑定一个触发点</span>
              </div>
            </header>
            <div class="editor-stage__body hook-editor-section">
              <n-form-item path="hookCode" :show-label="false">
                <ExtensionHookMatrix
                  v-model="form.hookCode"
                  compact
                  :allowed-hooks="allowedHooksForType"
                />
              </n-form-item>
            </div>
          </section>

          <section class="editor-stage editor-stage--content">
            <header class="editor-stage__header">
              <i>3</i>
              <div>
                <strong>增强内容</strong>
                <span>{{ contentStageHint }}</span>
              </div>
              <n-radio-group
                v-if="form.extensionType === 'VISUAL_RULE'"
                v-model:value="visualRule.match"
                size="small"
              >
                <n-radio-button value="ALL">
                  满足全部
                </n-radio-button>
                <n-radio-button value="ANY">
                  满足任一
                </n-radio-button>
              </n-radio-group>
            </header>
            <div class="editor-stage__body">
              <template v-if="form.extensionType === 'VISUAL_RULE'">
                <n-alert
                  v-if="clientContextCatalogLoading"
                  type="info"
                  :bordered="false"
                  class="rule-catalog-alert"
                >
                  正在读取业务字段，请稍候…
                </n-alert>
                <n-alert
                  v-else-if="clientContextCatalogError"
                  type="error"
                  :bordered="false"
                  class="rule-catalog-alert"
                >
                  {{ clientContextCatalogError }}
                  <n-button text type="primary" size="tiny" @click="loadClientContextCatalog(form.objectId)">
                    重新加载
                  </n-button>
                </n-alert>
                <n-alert
                  v-else-if="!form.objectId"
                  type="warning"
                  :bordered="false"
                  class="rule-catalog-alert"
                >
                  请先在「身份与范围」选择业务对象，条件字段会自动带出。
                </n-alert>
                <n-alert
                  v-else-if="!clientFieldCatalog.length"
                  type="warning"
                  :bordered="false"
                  class="rule-catalog-alert"
                >
                  当前对象还没有可用字段，请先在“数据”中完成字段设计。
                </n-alert>

                <div class="rule-block">
                  <div class="rule-block-title">
                    <strong>条件</strong>
                    <n-button size="tiny" secondary @click="addCondition">
                      添加条件
                    </n-button>
                  </div>
                  <div v-if="!visualRule.conditions.length" class="inline-empty">
                    没有条件时始终执行动作
                  </div>
                  <div v-for="(condition, index) in visualRule.conditions" :key="`condition-${index}`" class="rule-row condition-row">
                    <n-select
                      v-model:value="condition.field"
                      filterable
                      :loading="clientContextCatalogLoading"
                      :options="visualRuleFieldOptions(condition.field)"
                      placeholder="选择条件字段"
                      @update:value="condition.value = ''"
                    />
                    <DictSelect
                      v-model:value="condition.operator"
                      dict-type="ai_business_extension_rule_operator"
                      :clearable="false"
                      quick-create
                    />
                    <DictSelect
                      v-if="conditionValueKind(condition) === 'DICT' && operatorNeedsValue(condition.operator)"
                      v-model:value="condition.value"
                      :dict-type="conditionField(condition)?.dictType"
                      clearable
                      quick-create
                    />
                    <n-select
                      v-else-if="conditionValueKind(condition) === 'BOOLEAN' && operatorNeedsValue(condition.operator)"
                      v-model:value="condition.value"
                      :options="booleanRuleOptions"
                      clearable
                      placeholder="选择是或否"
                    />
                    <n-input-number
                      v-else-if="conditionValueKind(condition) === 'NUMBER' && operatorNeedsValue(condition.operator)"
                      v-model:value="condition.value"
                      clearable
                      placeholder="输入比较值"
                    />
                    <n-date-picker
                      v-else-if="['DATE', 'DATETIME'].includes(conditionValueKind(condition)) && operatorNeedsValue(condition.operator)"
                      v-model:formatted-value="condition.value"
                      :type="conditionValueKind(condition) === 'DATETIME' ? 'datetime' : 'date'"
                      :value-format="conditionValueKind(condition) === 'DATETIME' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd'"
                      clearable
                    />
                    <n-input
                      v-else
                      v-model:value="condition.value"
                      :disabled="!operatorNeedsValue(condition.operator)"
                      :placeholder="operatorNeedsValue(condition.operator) ? '输入比较值' : '无需填写比较值'"
                    />
                    <n-button quaternary type="error" @click="visualRule.conditions.splice(index, 1)">
                      移除
                    </n-button>
                  </div>
                </div>

                <div class="rule-block">
                  <div class="rule-block-title">
                    <strong>动作</strong>
                    <n-button size="tiny" secondary @click="addAction">
                      添加动作
                    </n-button>
                  </div>
                  <div v-if="!visualRule.actions.length" class="inline-empty error">
                    至少需要一个动作
                  </div>
                  <div v-for="(action, index) in visualRule.actions" :key="`action-${index}`" class="rule-row action-row">
                    <DictSelect
                      v-model:value="action.actionType"
                      dict-type="ai_business_extension_rule_action"
                      :clearable="false"
                      quick-create
                    />
                    <n-select
                      v-if="action.actionType === 'SET_FIELD'"
                      v-model:value="action.field"
                      filterable
                      :loading="clientContextCatalogLoading"
                      :options="visualRuleWritableFieldOptions(action.field)"
                      placeholder="选择要设置的字段"
                      @update:value="action.value = ''"
                    />
                    <DictSelect
                      v-if="action.actionType === 'SET_FIELD' && actionValueKind(action) === 'DICT'"
                      v-model:value="action.value"
                      :dict-type="actionField(action)?.dictType"
                      clearable
                      quick-create
                    />
                    <n-select
                      v-else-if="action.actionType === 'SET_FIELD' && actionValueKind(action) === 'BOOLEAN'"
                      v-model:value="action.value"
                      :options="booleanRuleOptions"
                      clearable
                      placeholder="选择是或否"
                    />
                    <n-input-number
                      v-else-if="action.actionType === 'SET_FIELD' && actionValueKind(action) === 'NUMBER'"
                      v-model:value="action.value"
                      clearable
                      placeholder="输入设置值"
                    />
                    <n-date-picker
                      v-else-if="action.actionType === 'SET_FIELD' && ['DATE', 'DATETIME'].includes(actionValueKind(action))"
                      v-model:formatted-value="action.value"
                      :type="actionValueKind(action) === 'DATETIME' ? 'datetime' : 'date'"
                      :value-format="actionValueKind(action) === 'DATETIME' ? 'yyyy-MM-dd HH:mm:ss' : 'yyyy-MM-dd'"
                      clearable
                    />
                    <n-input
                      v-else-if="action.actionType === 'SET_FIELD'"
                      v-model:value="action.value"
                      placeholder="输入设置值"
                    />
                    <n-input
                      v-else-if="action.actionType === 'SHOW_MESSAGE'"
                      v-model:value="action.message"
                      placeholder="提示内容"
                    />
                    <n-select
                      v-else
                      v-model:value="action.actionCode"
                      filterable
                      :options="visualRuleActionOptions(action.actionCode)"
                      placeholder="选择页面动作"
                    />
                    <n-button quaternary type="error" @click="visualRule.actions.splice(index, 1)">
                      移除
                    </n-button>
                  </div>
                </div>
              </template>

              <template v-else-if="form.extensionType === 'CLIENT_JS'">
                <ExtensionCodeWorkbench
                  v-model="form.content"
                  mode="javascript"
                  :hook-code="form.hookCode"
                  :application-code="application?.applicationCode"
                  :application-name="application?.applicationName"
                  :page-code="selectedEntryLabel"
                  :page-name="selectedEntryLabel"
                  @example-applied="applyCodeExampleContext"
                />
                <div class="test-context-section">
                  <div class="test-context-heading">
                    <div>
                      <strong>准备一条测试数据</strong>
                      <span>字段和页面动作会从脚本自动识别，你只需要确认测试值。</span>
                    </div>
                    <n-button
                      size="tiny"
                      secondary
                      :loading="clientContextCatalogLoading"
                      @click="syncClientContextFromScript(false)"
                    >
                      重新识别脚本
                    </n-button>
                  </div>

                  <div class="test-scene-summary">
                    <div>
                      <span class="test-scene-kicker">当前模拟</span>
                      <strong>{{ clientContextScene.title }}</strong>
                      <p>{{ clientContextScene.description }}</p>
                    </div>
                    <n-tag size="small" :bordered="false">
                      {{ selectedEntryLabel }}
                    </n-tag>
                  </div>

                  <div class="test-data-block">
                    <div class="test-data-toolbar">
                      <div>
                        <strong>业务字段值</strong>
                        <span>{{ clientDetectedSummary }}</span>
                      </div>
                      <div class="test-data-actions">
                        <n-button size="tiny" quaternary @click="applyClientValuePreset('SAMPLE')">
                          填充示例值
                        </n-button>
                        <n-button size="tiny" quaternary @click="applyClientValuePreset('EMPTY')">
                          模拟空值
                        </n-button>
                        <n-button size="tiny" secondary @click="addClientTestField()">
                          添加字段
                        </n-button>
                      </div>
                    </div>

                    <div class="record-id-row">
                      <span>测试记录 ID</span>
                      <n-input
                        v-model:value="clientRecordId"
                        size="small"
                        placeholder="例如：1"
                      />
                      <small>用于模拟当前表单或列表行，不会查询数据库。</small>
                    </div>

                    <div v-if="clientTestFields.length" class="test-field-table">
                      <div class="test-field-row test-field-head">
                        <span>业务字段</span>
                        <span>值类型</span>
                        <span>本次测试值</span>
                        <span />
                      </div>
                      <div
                        v-for="(field, index) in clientTestFields"
                        :key="field.key"
                        class="test-field-row"
                      >
                        <n-select
                          v-model:value="field.fieldCode"
                          filterable
                          tag
                          :options="clientFieldOptions"
                          placeholder="选择字段或输入字段编码"
                          @update:value="value => handleClientTestFieldChange(field, value)"
                        />
                        <n-select
                          v-model:value="field.valueType"
                          :options="clientValueTypeOptions"
                          :clearable="false"
                          @update:value="value => handleClientValueTypeChange(field, value)"
                        />
                        <n-select
                          v-if="field.valueType === 'BOOLEAN'"
                          v-model:value="field.value"
                          :options="clientBooleanOptions"
                          :clearable="false"
                        />
                        <span v-else-if="field.valueType === 'NULL'" class="null-value-placeholder">
                          空值 null
                        </span>
                        <n-input
                          v-else
                          v-model:value="field.value"
                          :placeholder="clientValuePlaceholder(field.valueType)"
                        />
                        <n-button quaternary type="error" @click="clientTestFields.splice(index, 1)">
                          移除
                        </n-button>
                      </div>
                    </div>
                    <div v-else class="test-fields-empty">
                      当前脚本没有读取或修改业务字段，可以直接测试；如需补充数据，请点击“添加字段”。
                    </div>

                    <n-form-item
                      v-if="clientAllowedActions.length"
                      label="允许脚本触发的页面动作"
                      class="test-actions-field"
                    >
                      <n-select
                        v-model:value="clientAllowedActions"
                        multiple
                        filterable
                        tag
                        :options="clientActionOptions"
                        placeholder="脚本未调用页面动作时无需选择"
                      />
                      <template #feedback>
                        已从 triggerAction 自动识别；只有这里列出的动作会在测试中放行。
                      </template>
                    </n-form-item>
                  </div>

                  <details class="test-context-advanced">
                    <summary>查看沙箱实际接收的内容（高级）</summary>
                    <pre>{{ clientContextPreview }}</pre>
                  </details>
                </div>
                <ExtensionSandboxHost ref="sandboxRef" />
              </template>

              <template v-else-if="form.extensionType === 'SCOPED_CSS'">
                <div class="content-toolbar">
                  <n-form-item label="作用页面" class="page-code-field" :show-feedback="false">
                    <n-select
                      v-model:value="form.scopeKey"
                      clearable
                      filterable
                      :options="scopedCssPageOptions"
                      placeholder="全部页面"
                    />
                  </n-form-item>
                </div>
                <ExtensionCodeWorkbench
                  v-model="form.content"
                  mode="css"
                  :hook-code="form.hookCode"
                  :application-code="application?.applicationCode"
                  :application-name="application?.applicationName"
                  :page-code="form.scopeKey || 'default'"
                  :page-name="scopedCssPageLabel"
                />
                <n-alert v-if="cssError" type="error" :show-icon="false" class="code-feedback">
                  {{ cssError }}
                </n-alert>
                <ScopedCssPreview
                  v-else-if="cssResult"
                  class="code-feedback"
                  :css="cssResult.css"
                  :scope-selector="cssResult.scopeSelector"
                  :application-code="application?.applicationCode"
                  :application-name="application?.applicationName"
                  :page-code="form.scopeKey || 'default'"
                  :page-name="scopedCssPageLabel"
                />
              </template>

              <template v-else-if="form.extensionType === 'SERVER_BINDING'">
                <p class="security-note">
                  选择已注册的 Java 处理器；不在线编译，也不填写 Bean 或 Class 名。
                </p>
                <n-alert v-if="!handlerOptions.length" type="warning" :show-icon="false" class="handler-empty-alert">
                  暂无已注册的 Java 增强处理器。请先在后端实现并注册 LowcodeExtensionHandler，然后重启服务。
                </n-alert>
                <n-form-item label="注册处理器" path="handlerCode">
                  <n-select
                    v-model:value="handlerCode"
                    filterable
                    :options="handlerOptions"
                    placeholder="请选择管理员已注册处理器"
                  />
                </n-form-item>
                <div v-if="selectedHandler" class="handler-contract">
                  <span>允许触发点：{{ selectedHandler.allowedHooks?.length || 0 }} 个</span>
                  <span>输入字段：{{ Object.keys(selectedHandler.inputSchema || {}).join('、') || '无' }}</span>
                  <span>超时：{{ selectedHandler.timeoutMs }}ms</span>
                  <span>风险：{{ riskLevelLabel(selectedHandler.riskLevel) }}</span>
                  <span>所需权限：{{ selectedHandler.requiredPermission || '无额外权限' }}</span>
                </div>
                <n-form-item label="测试输入 JSON">
                  <n-input v-model:value="serverTestInput" type="textarea" :autosize="{ minRows: 6, maxRows: 12 }" />
                </n-form-item>
              </template>
            </div>
          </section>

          <section class="editor-stage editor-stage--governance">
            <button type="button" class="editor-stage__header is-toggle" @click="governanceOpen = !governanceOpen">
              <i>4</i>
              <div>
                <strong>治理与版本</strong>
                <span>失败策略、风险级别和变更说明</span>
              </div>
              <em>{{ governanceOpen ? '收起' : '展开' }}</em>
            </button>
            <div v-show="governanceOpen" class="editor-stage__body form-grid two-columns">
              <n-form-item label="失败策略" path="failurePolicy">
                <DictSelect
                  v-model:value="form.failurePolicy"
                  dict-type="ai_business_extension_failure_policy"
                  :clearable="false"
                  quick-create
                />
              </n-form-item>
              <n-form-item label="风险级别" path="riskLevel">
                <DictSelect
                  v-model:value="form.riskLevel"
                  dict-type="ai_business_extension_risk_level"
                  :clearable="false"
                  quick-create
                />
              </n-form-item>
              <n-form-item label="本次变更说明" class="form-grid__wide">
                <n-input v-model:value="form.changeSummary" placeholder="说明这次调整的原因和影响" />
              </n-form-item>
              <n-form-item label="备注" class="form-grid__wide">
                <n-input v-model:value="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
              </n-form-item>
            </div>
          </section>
        </n-form>
      </div>

      <aside class="workspace-aside">
        <section class="config-summary">
          <h3>配置进度</h3>
          <ol class="stage-progress">
            <li :class="{ done: Boolean(form.extensionName && form.extensionCode) }">
              <span>身份</span>
              <small>{{ form.extensionName || '未命名' }}</small>
            </li>
            <li :class="{ done: Boolean(form.hookCode) }">
              <span>时机</span>
              <small>{{ hookSummaryLabel }}</small>
            </li>
            <li :class="{ done: contentStageReady }">
              <span>内容</span>
              <small>{{ selectedExtensionType?.title || '未选择' }}</small>
            </li>
            <li :class="{ done: Boolean(form.failurePolicy && form.riskLevel) }">
              <span>治理</span>
              <small>{{ governanceOpen ? '已展开' : '可稍后填写' }}</small>
            </li>
          </ol>
          <dl>
            <div>
              <dt>对象</dt>
              <dd>{{ objectSummaryLabel }}</dd>
            </div>
            <div>
              <dt>范围</dt>
              <dd>{{ scopeSummaryLabel }}</dd>
            </div>
          </dl>
        </section>

        <section class="test-console-section">
          <div class="test-console-heading">
            <div>
              <h3>增强测试</h3>
              <p>保存草稿后校验并受限运行；不会直接启用。</p>
            </div>
            <span>{{ testStage === 'IDLE' ? '等待测试' : testSummary }}</span>
          </div>
          <div class="test-step-list">
            <div
              v-for="step in testSteps"
              :key="step.code"
              class="test-step"
              :class="testStepClass(step.code)"
            >
              <i>{{ testStepIcon(step) }}</i>
              <span>{{ step.label }}</span>
            </div>
          </div>
          <n-alert
            v-if="testStage === 'FAILED'"
            type="error"
            :show-icon="false"
            class="test-result-alert"
          >
            {{ testSummary }}
          </n-alert>
          <n-alert
            v-else-if="testStage === 'PASSED'"
            type="success"
            :show-icon="false"
            class="test-result-alert"
          >
            草稿已通过测试。启用后预览刷新即可执行；正式环境需重新发布。
          </n-alert>
        </section>
      </aside>
    </div>

    <footer class="workspace-footer">
      <span>{{ isEdit ? `草稿 v${extension?.draftVersion || '-'}` : '保存后生成 v1 草稿' }}</span>
      <n-space>
        <n-button @click="closeWorkspace">
          返回列表
        </n-button>
        <n-button :loading="saving" secondary @click="saveCurrent">
          保存草稿
        </n-button>
        <n-button :loading="testing" type="primary" @click="saveAndTest">
          保存并测试
        </n-button>
        <n-button
          v-if="testStage === 'PASSED' && form.status !== 'ENABLED'"
          :loading="enabling"
          type="success"
          @click="enableCurrentVersion"
        >
          启用当前版本
        </n-button>
      </n-space>
    </footer>
  </div>
</template>

<script>
import { extensionEditorDrawerLocalComponents } from './extensionEditorDrawerLocalComponents'
import { useExtensionEditorDrawer } from './composables/useExtensionEditorDrawer'

export default {
  name: 'ExtensionEditorDrawer',
  components: {
    ...extensionEditorDrawerLocalComponents,
  },
  props: {
  show: Boolean,
  application: {
    type: Object,
    default: null,
  },
  extension: {
    type: Object,
    default: null,
  },
  createDefaults: {
    type: Object,
    default: null,
  },
  objects: {
    type: Array,
    default: () => [],
  },
  entries: {
    type: Array,
    default: () => [],
  },
  pages: {
    type: Array,
    default: () => [],
  },
  handlers: {
    type: Array,
    default: () => [],
  },
  startWithTest: Boolean,
},
  emits: ['update:show', 'saved', 'closed'],
  setup(props, { emit }) {
    return useExtensionEditorDrawer(props, emit)
  },
}
</script>

<style scoped src="./extensionEditorDrawer.css"></style>

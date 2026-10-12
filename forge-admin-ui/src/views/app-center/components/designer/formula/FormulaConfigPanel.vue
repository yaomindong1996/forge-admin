<template>
  <section class="formula-setting-workspace">
    <div class="formula-setting-head">
      <strong>公式设置</strong>
      <n-switch
        :value="formulaEnabled"
        :disabled="disabled"
        size="small"
        @update:value="value => emit('toggleEnabled', value)"
      />
    </div>

    <template v-if="formulaEnabled">
      <div class="formula-command-bar">
        <n-form-item label="公式名称" class="formula-form-item formula-name-item">
          <n-input
            v-model:value="formModel.formulaName"
            :disabled="disabled"
            placeholder="例如：金额计算"
          />
        </n-form-item>

        <n-form-item label="公式类型" class="formula-form-item formula-type-item">
          <n-select
            v-model:value="formModel.formulaType"
            :options="formulaTypeOptions"
            :disabled="disabled"
            placeholder="选择公式类型"
            @update:value="value => emit('typeChange', value)"
          />
        </n-form-item>

        <div class="formula-trigger-row">
          <span>触发方式</span>
          <n-button-group size="small">
            <n-button
              class="formula-trigger-option"
              :class="{ selected: formModel.formulaTriggerMode === 'REALTIME' }"
              :disabled="disabled"
              @click="emit('triggerMode', 'REALTIME')"
            >
              实时
            </n-button>
            <n-button
              class="formula-trigger-option"
              :class="{ selected: formModel.formulaTriggerMode === 'ON_SAVE' }"
              :disabled="disabled"
              @click="emit('triggerMode', 'ON_SAVE')"
            >
              保存时
            </n-button>
            <n-button disabled>
              手动
            </n-button>
          </n-button-group>
        </div>

        <div class="formula-command-actions">
          <n-button
            type="primary"
            size="small"
            :loading="saving"
            :disabled="saving || disabled"
            @click="emit('save')"
          >
            保存
          </n-button>
          <n-button
            secondary
            size="small"
            :loading="formulaValidating"
            :disabled="disabled"
            @click="emit('validate')"
          >
            校验
          </n-button>
          <n-button
            secondary
            size="small"
            :loading="formulaPreviewing"
            :disabled="previewDisabled || disabled"
            @click="emit('preview')"
          >
            预览
          </n-button>
        </div>

        <div class="formula-observe-row">
          <n-button
            size="small"
            secondary
            :disabled="!canOpenFormulaDebugger"
            @click="emit('openDebugger')"
          >
            <template #icon>
              <n-icon><BugOutline /></n-icon>
            </template>
            调试
          </n-button>
          <n-button
            size="small"
            secondary
            @click="emit('openLog')"
          >
            <template #icon>
              <n-icon><DocumentTextOutline /></n-icon>
            </template>
            日志
          </n-button>
          <n-button
            size="small"
            secondary
            :disabled="!hasFormulaToolFields"
            @click="emit('openGraph')"
          >
            <template #icon>
              <n-icon><GitNetworkOutline /></n-icon>
            </template>
            依赖
          </n-button>
        </div>
      </div>

      <FormulaLookupPanel
        v-if="formModel.formulaType === 'LOOKUP'"
        :form="formModel"
        :field="field"
        :relations="relations"
        :all-fields="allFields"
        :disabled="disabled"
      />

      <FormulaCrossObjectPanel
        v-if="showCrossObjectPanel"
        :form="formModel"
        :relations="relations"
        :disabled="disabled"
      />

      <section
        v-if="!['LOOKUP', 'CONDITIONAL'].includes(formModel.formulaType) && !formModel.formulaCrossObjectEnabled"
        class="formula-editor-block"
      >
        <div class="formula-editor-block-head">
          <span>表达式</span>
        </div>
        <FormulaExpressionEditor
          v-model:value="formModel.formulaExpression"
          v-model:depends-on="formModel.formulaDependsOn"
          :field-options="dependFieldOptions"
          :all-fields="allFields"
          :disabled="disabled || formModel.formulaCrossObjectEnabled"
          :placeholder="formModel.formulaCrossObjectEnabled ? '由跨对象路径自动生成' : '例如: unitPrice * quantity'"
          compact
        />
      </section>

      <div v-if="formModel.formulaType === 'AGGREGATE'" class="formula-extra-grid">
        <n-form-item label="聚合函数">
          <n-select
            v-model:value="formModel.formulaAggregateFunction"
            :options="aggregateFunctionOptions"
            :disabled="disabled"
            placeholder="选择聚合函数"
          />
        </n-form-item>
        <n-form-item label="关联对象">
          <n-select
            v-model:value="formModel.formulaAggregateRelationCode"
            :options="aggregateRelationOptions"
            :disabled="disabled"
            filterable
            clearable
            placeholder="选择关联对象"
          />
        </n-form-item>
        <n-form-item label="目标字段">
          <n-input
            v-model:value="formModel.formulaAggregateTargetField"
            :disabled="disabled"
            placeholder="目标对象的字段名，如 amount"
          />
        </n-form-item>
        <n-form-item label="过滤条件">
          <n-input
            v-model:value="formModel.formulaAggregateFilter"
            :disabled="disabled"
            placeholder="可选，如 status == 'active'"
          />
        </n-form-item>
      </div>

      <section v-if="formModel.formulaType === 'CONDITIONAL' && !formModel.formulaCrossObjectEnabled" class="condition-mode-panel">
        <div class="condition-mode-head">
          <span>条件配置</span>
          <n-radio-group
            v-model:value="formModel.formulaConditionMode"
            :disabled="disabled || formModel.formulaCrossObjectEnabled"
            size="small"
            @update:value="value => emit('conditionModeChange', value)"
          >
            <n-radio-button value="EXPRESSION">
              表达式
            </n-radio-button>
            <n-radio-button value="RULE">
              规则设计
            </n-radio-button>
          </n-radio-group>
        </div>

        <section
          v-if="formModel.formulaConditionMode !== 'RULE'"
          class="formula-editor-block"
        >
          <div class="formula-editor-block-head">
            <span>条件表达式</span>
          </div>
          <FormulaExpressionEditor
            v-model:value="formModel.formulaConditionExpression"
            v-model:depends-on="formModel.formulaDependsOn"
            :field-options="dependFieldOptions"
            :all-fields="allFields"
            :disabled="disabled || formModel.formulaCrossObjectEnabled"
            placeholder="例如: amount > 1000"
            compact
          />
        </section>

        <FormulaConditionRuleDesigner
          v-else
          :form="formModel"
          :all-fields="allFields"
          :disabled="disabled || formModel.formulaCrossObjectEnabled"
          @compiled="value => emit('conditionRuleCompiled', value)"
          @validation="value => emit('conditionRuleValidation', value)"
        />

        <div class="formula-extra-grid condition-value-grid">
          <n-form-item label="条件成立值">
            <n-input
              v-model:value="formModel.formulaConditionTrueValue"
              :disabled="disabled"
              placeholder="如 VIP 或 1"
            />
          </n-form-item>
          <n-form-item label="条件不成立值">
            <n-input
              v-model:value="formModel.formulaConditionFalseValue"
              :disabled="disabled"
              placeholder="如 NORMAL 或 0"
            />
          </n-form-item>
        </div>
      </section>

      <div v-if="formulaValidateResult" class="formula-feedback" :class="formulaValidateResult.valid ? 'success' : 'error'">
        <div v-if="formulaValidateResult.valid" class="feedback-success">
          <n-icon><CheckmarkCircleOutline /></n-icon>
          <span>配置有效</span>
          <span v-if="formulaValidateResult.variables?.length" class="vars">
            变量: {{ formulaValidateResult.variables.join(', ') }}
          </span>
        </div>
        <div v-else class="feedback-error">
          <div v-for="(line, index) in formulaFeedbackLines" :key="`${index}-${line}`" class="feedback-error-line">
            <n-icon><AlertCircleOutline /></n-icon>
            <span>{{ line }}</span>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import {
  AlertCircleOutline,
  BugOutline,
  CheckmarkCircleOutline,
  DocumentTextOutline,
  GitNetworkOutline,
} from '@vicons/ionicons5'
import { computed } from 'vue'
import FormulaConditionRuleDesigner from './FormulaConditionRuleDesigner.vue'
import FormulaCrossObjectPanel from './FormulaCrossObjectPanel.vue'
import FormulaExpressionEditor from './FormulaExpressionEditor.vue'
import FormulaLookupPanel from './FormulaLookupPanel.vue'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  field: {
    type: Object,
    default: null,
  },
  allFields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  saving: {
    type: Boolean,
    default: false,
  },
  formulaValidating: {
    type: Boolean,
    default: false,
  },
  formulaPreviewing: {
    type: Boolean,
    default: false,
  },
  formulaValidateResult: {
    type: Object,
    default: null,
  },
  formulaFeedbackLines: {
    type: Array,
    default: () => [],
  },
  canOpenFormulaDebugger: {
    type: Boolean,
    default: false,
  },
  hasFormulaToolFields: {
    type: Boolean,
    default: false,
  },
  previewDisabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'toggleEnabled',
  'typeChange',
  'insertToken',
  'insertStringToken',
  'conditionExpressionChange',
  'conditionModeChange',
  'conditionRuleCompiled',
  'conditionRuleValidation',
  'triggerMode',
  'save',
  'validate',
  'preview',
  'openDebugger',
  'openLog',
  'openGraph',
])

const formulaTypeOptions = [
  { label: '基础运算 (price * qty)', value: 'CALC' },
  { label: '聚合函数 (SUM / COUNT)', value: 'AGGREGATE' },
  { label: '条件判断 (IF)', value: 'CONDITIONAL' },
  { label: 'LOOKUP 关联取值', value: 'LOOKUP' },
]
const aggregateFunctionOptions = [
  { label: 'SUM - 求和', value: 'SUM' },
  { label: 'COUNT - 计数', value: 'COUNT' },
  { label: 'AVG - 平均值', value: 'AVG' },
  { label: 'MAX - 最大值', value: 'MAX' },
  { label: 'MIN - 最小值', value: 'MIN' },
]
const disabled = computed(() => Boolean(props.field?.systemField))
const formModel = computed(() => props.form)
const formulaEnabled = computed(() => Boolean(formModel.value.formulaType))
const showCrossObjectPanel = computed(() => {
  return formModel.value.formulaType && !['LOOKUP', 'AGGREGATE'].includes(formModel.value.formulaType)
})

const dependFieldOptions = computed(() => {
  return (props.allFields || [])
    .filter(item => item && item.fieldCode !== formModel.value.fieldCode && item.fieldStatus !== 'HIDDEN')
    .map(item => ({
      label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
      value: item.fieldCode || item.field,
    }))
})

const aggregateRelationOptions = computed(() => {
  return (props.relations || [])
    .filter(item => item && item.id && item.status !== 0 && ['DETAIL', 'CHILD_LIST'].includes(String(item.relationType || '').toUpperCase()))
    .map(item => ({
      label: `${item.relationName || item.targetObjectName || item.targetObjectCode}（${item.targetObjectCode || '关联对象'}）`,
      value: String(item.id),
    }))
})
</script>

<style scoped>
.formula-setting-workspace {
  display: grid;
  align-content: start;
  gap: 8px;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
  margin-top: 0;
  border: 0;
  border-radius: 0;
  background: var(--bg-primary);
  padding: 0;
}

.formula-setting-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  border-bottom: 1px solid #eef2f7;
  background: var(--bg-primary);
}

.formula-setting-head strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0;
}

.formula-command-bar {
  display: grid;
  grid-template-columns: minmax(180px, 0.88fr) minmax(220px, 1fr) minmax(210px, 0.9fr) auto auto;
  align-items: end;
  gap: 8px;
  border: 1px solid #dce4ef;
  border-radius: 6px;
  background: var(--bg-secondary);
  padding: 10px;
}

.formula-name-item,
.formula-type-item {
  min-width: 0;
}

.formula-form-item {
  margin-bottom: 0;
}

.formula-form-item :deep(.n-form-item-label) {
  min-height: auto;
  padding-bottom: 5px;
}

.formula-form-item :deep(.n-form-item-label__text) {
  color: #202938;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.formula-form-item :deep(.n-input),
.formula-form-item :deep(.n-base-selection) {
  --n-border-radius: 6px;
  --n-height: 32px;
  background: var(--bg-secondary);
}

.formula-editor-block {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 6px;
  min-width: 0;
  min-height: 500px;
  width: 100%;
  height: clamp(500px, calc(100vh - 320px), 650px);
}

.formula-editor-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 18px;
}

.formula-editor-block-head span {
  color: #202938;
  font-size: 12px;
  font-weight: 700;
}

.formula-editor-block :deep(.expression-workbench) {
  height: 100%;
  min-height: 0;
  background: var(--bg-primary);
}

.formula-extra-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
}

.condition-mode-panel {
  display: grid;
  gap: 8px;
}

.condition-mode-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.condition-mode-head > span {
  color: #202938;
  font-size: 13px;
  font-weight: 600;
}

.condition-mode-head :deep(.n-radio-group) {
  display: inline-flex;
  min-width: 184px;
}

.condition-value-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.formula-trigger-row {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.formula-trigger-row > span {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
}

.formula-trigger-row :deep(.n-button-group) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
}

.formula-trigger-row :deep(.n-button) {
  justify-content: center;
  min-width: 0;
  height: 32px;
  border-radius: 0;
  background: #e7ebf1;
  color: #202938;
  font-size: 12px;
  font-weight: 600;
}

.formula-trigger-row :deep(.n-button:first-child) {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.formula-trigger-row :deep(.n-button:last-child) {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.formula-trigger-row :deep(.formula-trigger-option.selected) {
  border-color: #2f58d6;
  background: #edf3ff;
  color: #274ec8;
}

.formula-command-actions,
.formula-observe-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.formula-command-actions {
  justify-content: flex-end;
}

.formula-feedback {
  margin: 0;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.formula-feedback.success {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.formula-feedback.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.feedback-success,
.feedback-error-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.feedback-success .vars {
  color: #047857;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.feedback-error {
  display: grid;
  gap: 6px;
}

@media (max-width: 1280px) {
  .formula-command-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .formula-command-actions,
  .formula-observe-row {
    justify-content: flex-start;
  }

  .formula-extra-grid,
  .condition-value-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .formula-command-bar,
  .formula-extra-grid,
  .condition-value-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>

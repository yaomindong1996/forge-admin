<template>
  <view class="flow-business-form">
    <view v-if="businessProviderUnavailable" class="form-provider-notice">
      <text>流程服务未加载该业务表单 Provider，当前仅能展示表单字段结构；部署 Provider 后会自动加载实际数据和节点权限。</text>
    </view>
    <PageSectionRenderer
      v-if="hasLowcodeForm"
      :sections="pageSections"
      :main-fields="mainFields"
      :main-nodes="mainNodes"
      :main-data="mainData"
      :children="allChildren"
      :child-data="childData"
      :mode="formMode"
      :dict-options="dictOptions"
      :runtime-context="runtimeContext"
      :flow-interaction="flowInteraction"
      :current-flow-node-key="currentFlowNodeKey"
      @set-main-form-ref="form.setMainFormRef"
      @set-child-form-ref="form.setChildFormRef"
      @add-child-row="form.addBusinessChildRow"
      @remove-child-row="form.removeBusinessChildRow"
    />
    <view v-else-if="formSchemaUnavailable" class="form-schema-notice">
      <text>该流程未返回可展示的业务字段配置，已隐藏内部字段和技术标识。</text>
    </view>
  </view>
</template>

<script setup>
import { toRefs } from 'vue'
import PageSectionRenderer from '@/components/lowcode/PageSectionRenderer.vue'

const props = defineProps({
  /** reactive(useFlowBusinessForm(...)) */
  form: { type: Object, required: true },
})

const {
  businessProviderUnavailable, hasLowcodeForm, pageSections, mainFields, mainNodes, mainData, allChildren,
  childData, formMode, dictOptions, runtimeContext, flowInteraction, currentFlowNodeKey, formSchemaUnavailable,
} = toRefs(props.form)
</script>

<style lang="scss" scoped>
.form-provider-notice { margin-bottom: 12px; padding: 8px 12px; border-radius: 10px; color: var(--forge-tone-orange); font-size: 13px; line-height: 1.55; background: var(--forge-tone-orange-bg); }
.form-provider-notice text { display: block; }
.form-schema-notice { padding: 16px 12px; border-radius: 10px; color: var(--forge-text-secondary); font-size: 13px; line-height: 1.6; text-align: center; background: var(--forge-surface-subtle); }
.form-schema-notice text { display: block; }

/* 可编辑控件统一白底加描边，只读字段为纯文本，一眼区分哪些能改 */
.flow-business-form :deep(.ai-field__control:not(.is-disabled)),
.flow-business-form :deep(.ai-textarea:not(.is-disabled)),
.flow-business-form :deep(.wd-select-picker__cell),
.flow-business-form :deep(.wd-datetime-picker__cell),
.flow-business-form :deep(.lowcode-selector__trigger:not([disabled])) { border-color: var(--forge-border-strong, #e3e4e6); background: var(--forge-surface, #fff); }
.flow-business-form :deep(.ai-field__control.is-focused),
.flow-business-form :deep(.ai-textarea.is-focused) { border-color: var(--forge-color-primary, #0066ff); }

/*
 * 审批移动表单：分组直接平铺在外层白卡里，只读字段是左标签、右值的描述列表，
 * 子表每条记录是浅灰小卡片。行间距统一由字段行的上下内边距控制，
 * 所以布局容器和低代码表单自带的 gap / margin 在这里都要归零。
 */
@media (max-width: 1023px) {
  .flow-business-form :deep(.card-section) { margin: 0; padding: 0; border: 0; border-radius: 0; background: transparent; }
  .flow-business-form :deep(.card-section + .card-section),
  .flow-business-form :deep(.card-section + .section-child-tabs) { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--forge-border, #f0f1f2); }
  .flow-business-form :deep(.section-child-tabs) { margin-bottom: 4px; }
  .flow-business-form :deep(.card-section__head) { min-height: 0; margin: 0 0 4px; padding: 0; }
  .flow-business-form :deep(.card-section__title) { color: var(--forge-text-primary, #171a1d); font-size: 15px; font-weight: 600; line-height: 22px; }

  .flow-business-form :deep(.lowcode-layout-container.is-grid),
  .flow-business-form :deep(.lowcode-layout-container.is-box),
  .flow-business-form :deep(.lowcode-layout-container.is-table) { margin-bottom: 0; }
  .flow-business-form :deep(.lowcode-layout-container.is-grid > .lowcode-layout),
  .flow-business-form :deep(.lowcode-form.lowcode-form--inline-grid) { gap: 0; }

  .flow-business-form :deep(.lowcode-field.lowcode-field) { margin: 0; padding: 8px 0; }
  .flow-business-form :deep(.lowcode-field .lowcode-field__label) { min-height: 0; margin: 0 0 6px; color: var(--forge-text-secondary, #747677); font-size: 14px; line-height: 22px; }
  .flow-business-form :deep(.lowcode-field__control) { min-height: 0; }
  .flow-business-form :deep(.lowcode-field--compact-row),
  .flow-business-form :deep(.lowcode-field--readonly-row) { display: grid; grid-template-columns: 78px minmax(0, 1fr); align-items: center; gap: 12px; }
  .flow-business-form :deep(.lowcode-field.lowcode-field--readonly-row) { align-items: start; padding: 7px 0; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__label),
  .flow-business-form :deep(.lowcode-field--readonly-row .lowcode-field__label) { margin: 0; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__control),
  .flow-business-form :deep(.lowcode-field--readonly-row .lowcode-field__control) { min-width: 0; }
  .flow-business-form :deep(.lowcode-field--readonly-row .lowcode-field__readonly) { min-height: 0; padding: 0; font-size: 15px; line-height: 22px; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__error),
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__hint) { grid-column: 2; margin-top: -4px; }

  .flow-business-form :deep(.section-child-head) { margin: 4px 0 0; }
  .flow-business-form :deep(.section-child-count) { color: var(--forge-text-tertiary, #a2a3a5); font-size: 12px; font-weight: 400; }
  .flow-business-form :deep(.section-child-row),
  .flow-business-form :deep(.section-child-row:first-child),
  .flow-business-form :deep(.section-child-row:last-child),
  .flow-business-form :deep(.section-card-row),
  .flow-business-form :deep(.section-card-row:first-child),
  .flow-business-form :deep(.section-card-row:last-child) { margin-top: 8px; padding: 8px 12px; border: 0; border-radius: 10px; background: var(--forge-surface-subtle, #f7f8fa); }
  .flow-business-form :deep(.section-child-row__head) { min-height: 22px; margin: 2px 0 0; }
  .flow-business-form :deep(.section-child-row__title) { color: var(--forge-text-secondary, #747677); font-size: 13px; font-weight: 500; }
  .flow-business-form :deep(.section-child-row .lowcode-field--readonly-row),
  .flow-business-form :deep(.section-card-row .lowcode-field--readonly-row) { grid-template-columns: 72px minmax(0, 1fr); padding: 5px 0; }
  .flow-business-form :deep(.section-child-row .lowcode-field--compact-row),
  .flow-business-form :deep(.section-card-row .lowcode-field--compact-row) { grid-template-columns: 72px minmax(0, 1fr); }
}
</style>

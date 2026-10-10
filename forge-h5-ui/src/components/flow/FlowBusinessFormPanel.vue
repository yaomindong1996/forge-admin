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

/* 审批移动表单：单行控件使用左标签、右控件，避免桌面式纵向堆叠占满首屏。 */
@media (max-width: 1023px) {
  .flow-business-form :deep(.card-section) { margin-bottom: 10px; padding: 12px; border-color: transparent; border-radius: 12px; background: var(--forge-surface-subtle); }
  .flow-business-form :deep(.card-section:last-child) { margin-bottom: 0; }
  .flow-business-form :deep(.card-section__head) { min-height: 34px; margin: 0 0 8px; padding: 0; }
  .flow-business-form :deep(.card-section__title) { font-size: 14px; font-weight: 700; }
  .flow-business-form :deep(.lowcode-field) { margin-bottom: 12px; }
  .flow-business-form :deep(.lowcode-field:last-child) { margin-bottom: 0; }
  .flow-business-form :deep(.lowcode-field__label) { margin-bottom: 5px; font-size: 13px; line-height: 1.4; }
  .flow-business-form :deep(.lowcode-field__control) { min-height: 44px; }
  .flow-business-form :deep(.lowcode-field--compact-row) { display: grid; grid-template-columns: 78px minmax(0, 1fr); align-items: center; gap: 10px; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__label) { margin: 0; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__control) { min-width: 0; }
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__error),
  .flow-business-form :deep(.lowcode-field--compact-row .lowcode-field__hint) { grid-column: 2; margin-top: -4px; }
}
</style>

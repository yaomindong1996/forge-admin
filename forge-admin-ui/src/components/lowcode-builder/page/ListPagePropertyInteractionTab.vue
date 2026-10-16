<template>
            <div v-show="propertyPanelTab === 'interaction'" class="property-tab-content">
              <div class="property-search-anchor" data-property-search="交互 事件 联动动作 条件规则 显示与状态规则 显示 隐藏 只读 禁用 必填 生命周期 回调 点击 加载完成 提交成功 行点击 跳转 刷新 过滤 接口请求 自定义脚本 参数 目标页面 目标表单" />
              <n-divider>联动动作</n-divider>
              <p class="section-hint">
                配置“什么时候触发 → 做什么”的自动化动作，例如点击按钮后跳转页面、行点击后打开详情表单。
              </p>
              <div class="event-editor">
                <div v-if="!selectedBlockEvents.length" class="copy-empty-state">
                  <span class="copy-empty-icon">+</span>
                  <strong>暂未配置事件</strong>
                  <small>可为点击、加载完成、行点击、提交成功等时机添加跳转、刷新、弹窗或接口请求动作。</small>
                </div>
                <div
                  v-for="(eventItem, eventIdx) in selectedBlockEvents"
                  :key="eventItem.id || eventIdx"
                  class="event-row"
                >
                  <div class="event-row-head">
                    <span>{{ eventTriggerText(eventItem.trigger) }} · {{ eventActionText(eventItem.action) }}</span>
                    <n-button size="tiny" quaternary type="error" @click="removeBlockEvent(eventIdx)">
                      删除
                    </n-button>
                  </div>
                  <div class="event-grid">
                    <label class="event-setting-field">
                      <span>什么时候触发</span>
                      <n-select
                        :value="eventItem.trigger"
                        :options="eventTriggerOptions"
                        size="small"
                        placeholder="触发时机"
                        @update:value="updateBlockEvent(eventIdx, { trigger: $event })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>触发后做什么</span>
                      <n-select
                        :value="eventItem.action"
                        :options="blockEventActionOptions"
                        size="small"
                        placeholder="执行动作"
                        @update:value="updateBlockEvent(eventIdx, { action: $event })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>影响哪个区块</span>
                      <n-select
                        :value="eventItem.targetBlockId"
                        :options="blockTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetBlockId: $event || '' })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>跳转到页面</span>
                      <n-select
                        :value="eventItem.targetPageKey"
                        :options="pageTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetPageKey: $event || '' })"
                      />
                    </label>
                    <label v-if="formTargetOptions.length" class="event-setting-field">
                      <span>打开哪个表单</span>
                      <n-select
                        :value="eventItem.targetFormKey"
                        :options="formTargetOptions"
                        size="small"
                        clearable
                        placeholder="可选"
                        @update:value="updateBlockEvent(eventIdx, { targetFormKey: $event || '' })"
                      />
                    </label>
                    <!-- 提示消息：专用文案，运行态优先于备注/参数 -->
                    <label v-if="eventItem.action === 'message' || eventItem.action === 'toast' || eventItem.action === 'notify'" class="event-setting-field">
                      <span>消息内容</span>
                      <n-input
                        :value="eventItem.message || eventItem.content || ''"
                        size="small"
                        placeholder="例如：操作成功"
                        @update:value="updateBlockEvent(eventIdx, { message: $event || '', content: $event || '' })"
                      />
                    </label>
                    <label class="event-setting-field">
                      <span>备注说明</span>
                      <n-input
                        :value="eventItem.description"
                        size="small"
                        placeholder="例如点击行打开详情"
                        @update:value="updateBlockEvent(eventIdx, { description: $event })"
                      />
                    </label>
                  </div>
                  <div class="event-param-list">
                    <div
                      v-for="(param, paramIdx) in (eventItem.params || [])"
                      :key="paramIdx"
                      class="event-param-row"
                    >
                      <n-input
                        :value="param.name"
                        size="tiny"
                        placeholder="参数名"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, { name: normalizeParamName($event) })"
                      />
                      <n-select
                        :value="param.sourceType || 'static'"
                        :options="paramSourceOptions"
                        size="tiny"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, normalizeParamSourcePatch($event, param))"
                      />
                      <n-select
                        v-if="param.sourceType === 'rowField'"
                        :value="param.sourceField || ''"
                        :options="rowFieldOptions"
                        size="tiny"
                        filterable
                        clearable
                        placeholder="当前行字段"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'rowField' }, $event))"
                      />
                      <n-select
                        v-else-if="param.sourceType === 'routeQuery'"
                        :value="param.sourceField || ''"
                        :options="routeParamOptions"
                        size="tiny"
                        filterable
                        tag
                        clearable
                        placeholder="路由参数"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'routeQuery' }, $event))"
                      />
                      <n-select
                        v-else-if="param.sourceType === 'system'"
                        :value="param.sourceField || ''"
                        :options="systemVariableOptions"
                        size="tiny"
                        clearable
                        placeholder="系统变量"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, buildParamValuePatch({ ...param, sourceType: 'system' }, $event))"
                      />
                      <n-input
                        v-else
                        :value="param.value"
                        size="tiny"
                        placeholder="固定值"
                        @update:value="updateBlockEventParam(eventIdx, paramIdx, { value: $event })"
                      />
                      <n-button size="tiny" quaternary @click="removeBlockEventParam(eventIdx, paramIdx)">
                        删
                      </n-button>
                    </div>
                    <n-button size="tiny" dashed block @click="addBlockEventParam(eventIdx)">
                      + 添加参数
                    </n-button>
                  </div>
                </div>
                <n-button size="small" dashed block @click="addBlockEvent">
                  + 添加事件
                </n-button>
              </div>

              <n-divider>显示与状态规则</n-divider>
              <p class="section-hint">
                按条件控制组件的显示 / 隐藏 / 只读 / 禁用，例如金额大于 1 万时才显示审批按钮。
              </p>
              <RuntimeRulesEditor
                title="条件规则"
                :rules="selectedBlock.props?.runtimeRules || []"
                :field-options="runtimeRuleFieldOptions"
                @update:rules="patchBlockProps(selectedBlock.id, { runtimeRules: $event })"
              />
            </div>

</template>

<script>
import { useListPageDesignerApi } from './listPageDesignerContext'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  components: { ...listPageDesignerLocalComponents },
  name: 'ListPagePropertyInteractionTab',
  setup() {
    return useListPageDesignerApi()
  },
}
</script>

<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>

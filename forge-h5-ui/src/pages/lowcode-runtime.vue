<template>
  <AiLayoutPage
    :title="pageChrome.title"
    :subtitle="pageChrome.subtitle"
    :show-nav="pageChrome.showNav"
    :show-back="pageChrome.showBack"
    :back-url="pageChrome.backUrl"
    :safe-bottom="pageChrome.safeBottom"
    :padded="pageChrome.padded"
  >
    <view v-if="loading" class="runtime-state">
      <AiListSkeleton :rows="4" />
    </view>
    <view v-else-if="errorMessage" class="runtime-state">
      <AiResult type="error" :title="errorMessage" description="请检查应用发布状态或联系管理员" />
      <AiButton block variant="secondary" @click="loadRuntime">重新加载</AiButton>
    </view>

    <LowcodeRuntimeList
      v-else-if="mode === 'list'"
      :config="config" :title="title" :total="total" :page="page" :page-size="pageSize"
      :search-expanded="searchExpanded" :search-fields="searchFields" :search-data="searchData"
      :records="records" :columns="visibleColumns" :dict-options="dictOptions"
      :context="searchRuntimeContext" :permissions="authStore.permissions"
      @toggle-search="toggleSearch" @create="openCreate" @search-change="loadListDebounced"
      @reset-search="resetSearch" @search="loadList" @open-row="openDetail"
      @action="runAction" @change-page="changePage"
    />

    <template v-else>
      <LowcodeFlowTimeline
        v-if="flowInteraction.timeline.enabled && (flowHistoryLoading || flowHistory.length)"
        :title="flowInteraction.timeline.title" :loading="flowHistoryLoading" :items="flowHistory"
      />
      <template v-if="hasComposedPageZones">
        <template v-for="zone in runtimePageZones" :key="zone.zoneId">
          <PageSectionRenderer
            v-if="zone.zoneType === 'form' && runtimeZoneHasSections(zone)"
            :sections="runtimeZoneFormSchema(zone).pageSections"
            :main-fields="runtimeZoneMainFields(zone)"
            :main-data="mainData"
            :children="allChildren"
            :child-data="childData"
            :mode="mode"
            :dict-options="dictOptions"
            :runtime-context="runtimeContext"
            :bottom-bar="runtimeZoneBottomBar(zone)"
            :bottom-action-loading="bottomActionLoading"
            :permissions="authStore.permissions"
            :field-linkages="runtimeZoneFieldLinkages(zone)"
            :flow-interaction="flowInteraction"
            :current-flow-node-key="currentFlowNodeKey"
            @main-field-event="payload => handleRuntimeZoneFieldEvent(zone, payload)"
            @child-field-event="payload => handleChildFieldEvent(payload.child, payload.row, payload.payload)"
            @add-child-row="addChildRow"
            @remove-child-row="payload => removeChildRow(payload.child, payload.index)"
            @set-child-form-ref="payload => setChildFormRef(payload.child, payload.row, payload.rowIndex, payload.instance)"
            @set-main-form-ref="payload => setMainSectionFormRef(zone, payload)"
            @bottom-action="handleBottomAction"
            @child-action="payload => runAction(payload.action, payload.row, payload.child)"
            @child-toolbar-action="payload => handleToolbarAction(payload.action, payload.child)"
          />
          <view v-else-if="zone.zoneType === 'form'" class="runtime-form-card">
            <view class="runtime-form-card__head">
              <text class="runtime-form-card__title">{{ mode === 'create' ? '新建' : mode === 'detail' ? '详情' : '编辑' }}{{ title }}</text>
              <text class="runtime-form-card__desc">填写后保存，字段会按配置自动联动</text>
            </view>
            <LowcodeForm
              :ref="instance => setRuntimeFormRef(zone, instance)"
              :fields="runtimeZoneMainFields(zone)"
              :nodes="runtimeZoneNodes(zone)"
              :data="mainData"
              :dict-options="dictOptions"
              :readonly="mode === 'detail'"
              :context="runtimeContext"
              :field-linkages="runtimeZoneFieldLinkages(zone)"
              @field-event="payload => handleRuntimeZoneFieldEvent(zone, payload)"
              @action="handleInlineNodeAction"
            />
          </view>
          <view v-else-if="zone.zoneType === 'actions'" class="runtime-zone-actions">
            <AiButton
              v-for="action in runtimeZoneActions(zone)"
              :key="action.actionCode || action.key || action.type || action.label"
              :variant="action.variant || 'secondary'"
              size="sm"
              :disabled="action.disabled === true"
              @click="handleRuntimeZoneAction(action)"
            >
              {{ action.label || action.actionName || action.actionCode || action.type }}
            </AiButton>
          </view>
          <LowcodeChildCards
            v-else-if="zone.zoneType === 'list'" :children="runtimeZoneChildren(zone)" :mode="mode"
            :child-data="childData" :dict-options="dictOptions" :context="runtimeContext"
            :field-linkages="runtimeZoneFieldLinkages(zone)" :config="config" :permissions="authStore.permissions"
            @toolbar-action="payload => handleToolbarAction(payload.action, payload.child)" @add-row="addChildRow"
            @remove-row="payload => removeChildRow(payload.child, payload.index)"
            @set-form-ref="payload => setChildFormRef(payload.child, payload.row, payload.rowIndex, payload.instance)"
            @field-event="payload => handleChildFieldEvent(payload.child, payload.row, payload.payload)"
            @row-action="payload => runAction(payload.action, payload.row, payload.child)"
          />
        </template>
        <LowcodeRuntimeFooter v-if="!hasComposedBottomBar" :mode="mode" :saving="saving" :can-edit="canEdit" @cancel="goList" @save="save" @edit="openEdit" />
      </template>

      <template v-else-if="hasPageSections">
        <PageSectionRenderer
          :sections="pageSections"
          :main-fields="mainFields"
          :main-data="mainData"
          :children="allChildren"
          :child-data="childData"
          :mode="mode"
          :dict-options="dictOptions"
          :runtime-context="runtimeContext"
          :bottom-bar="bottomBar"
          :bottom-action-loading="bottomActionLoading"
          :permissions="authStore.permissions"
          :field-linkages="fieldLinkages"
          :flow-interaction="flowInteraction"
          :current-flow-node-key="currentFlowNodeKey"
          @main-field-event="handleMainFieldEvent"
          @child-field-event="payload => handleChildFieldEvent(payload.child, payload.row, payload.payload)"
          @add-child-row="addChildRow"
          @remove-child-row="payload => removeChildRow(payload.child, payload.index)"
          @set-child-form-ref="payload => setChildFormRef(payload.child, payload.row, payload.rowIndex, payload.instance)"
          @set-main-form-ref="payload => setMainSectionFormRef(null, payload)"
          @bottom-action="handleBottomAction"
          @child-action="payload => runAction(payload.action, payload.row, payload.child)"
          @child-toolbar-action="payload => handleToolbarAction(payload.action, payload.child)"
        />
        <LowcodeRuntimeFooter v-if="!hasConfiguredBottomBar" :mode="mode" :saving="saving" :can-edit="canEdit" @cancel="goList" @save="save" @edit="openEdit" />
      </template>

      <template v-else>
        <view class="runtime-form-card">
          <view class="runtime-form-card__head">
            <text class="runtime-form-card__title">{{ mode === 'create' ? '新建' : mode === 'detail' ? '详情' : '编辑' }}{{ title }}</text>
            <text class="runtime-form-card__desc">{{ mode === 'detail' ? '只读查看已保存信息' : '填写后保存，字段会按配置自动联动' }}</text>
          </view>
          <LowcodeForm
            ref="mainFormRef"
            :fields="mainFields"
            :nodes="mainNodes"
            :data="mainData"
            :dict-options="dictOptions"
            :readonly="mode === 'detail'"
            :context="runtimeContext"
            :field-linkages="fieldLinkages"
            @field-event="handleMainFieldEvent"
            @action="handleInlineNodeAction"
          />
        </view>

        <LowcodeChildCards
          :children="visibleChildren" :mode="mode" :child-data="childData" :dict-options="dictOptions"
          :context="runtimeContext" :field-linkages="fieldLinkages" :config="config" :permissions="authStore.permissions"
          @toolbar-action="payload => handleToolbarAction(payload.action, payload.child)" @add-row="addChildRow"
          @remove-row="payload => removeChildRow(payload.child, payload.index)"
          @set-form-ref="payload => setChildFormRef(payload.child, payload.row, payload.rowIndex, payload.instance)"
          @field-event="payload => handleChildFieldEvent(payload.child, payload.row, payload.payload)"
          @row-action="payload => runAction(payload.action, payload.row, payload.child)"
        />

        <LowcodeRuntimeFooter :mode="mode" :saving="saving" :can-edit="canEdit" @cancel="goList" @save="save" @edit="openEdit" />
      </template>
    </template>
  </AiLayoutPage>
</template>

<script setup>
import { computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import AiButton from '@/components/AiButton.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiResult from '@/components/AiResult.vue'
import LowcodeChildCards from '@/components/lowcode/LowcodeChildCards.vue'
import LowcodeFlowTimeline from '@/components/lowcode/LowcodeFlowTimeline.vue'
import LowcodeForm from '@/components/lowcode/LowcodeForm.vue'
import LowcodeRuntimeList from '@/components/lowcode/LowcodeRuntimeList.vue'
import LowcodeRuntimeFooter from '@/components/lowcode/LowcodeRuntimeFooter.vue'
import PageSectionRenderer from '@/components/lowcode/PageSectionRenderer.vue'
import { useLowcodeFieldEvents } from '@/composables/lowcode/useLowcodeFieldEvents'
import { useLowcodeFlowRuntime } from '@/composables/lowcode/useLowcodeFlowRuntime'
import { useLowcodeFormRegistry } from '@/composables/lowcode/useLowcodeFormRegistry'
import { useLowcodeRuntimeData } from '@/composables/lowcode/useLowcodeRuntimeData'
import api from '@/api'
import { useAuthStore, useLowcodeRuntimeStore } from '@/store'
import { toast } from '@/utils/notify'
import {
  actionInputSchema,
  actionVisible,
  buildActionPayload,
  hasActionPermission,
  resolveActionDefinition,
  resolveActionPermission,
} from '@/utils/lowcode-runtime'

const authStore = useAuthStore()
const runtimeStore = useLowcodeRuntimeStore()
const {
  configKey, title, loading, saving, errorMessage, config, mode, currentId, records, total, page,
  searchExpanded, bottomActionLoading, formDesignerSchema, runtimePageZones, hasComposedPageZones,
  mainFields, mainNodes, searchFields, visibleColumns, hasPageSections, pageSections, flowInteraction,
  bottomBar, fieldLinkages, allChildren, visibleChildren, currentFlowNodeKey, currentFlowTaskId,
  currentProcessInstanceId, hasConfiguredBottomBar, canEdit, pageChrome,
} = storeToRefs(runtimeStore)
const { routeQuery, searchData, mainData, childData, dictOptions } = runtimeStore
const pageSize = runtimeStore.pageSize
const hasComposedBottomBar = computed(() => runtimePageZones.value.some(zone => {
  const schema = runtimeZoneFormSchema(zone)
  const hasBar = Array.isArray(schema?.bottomBar?.actions) && schema.bottomBar.actions.length > 0
  const hasSave = zone.zoneType === 'actions' && runtimeZoneActions(zone).some(action => String(action.type || '').toLowerCase() === 'save')
  return hasBar || hasSave
}))
const runtimeContext = computed(() => ({
  routeQuery,
  user: authStore.userInfo || {},
  currentUser: authStore.userInfo || {},
}))
const formRegistry = useLowcodeFormRegistry({
  isComposedPage: () => hasComposedPageZones.value,
  hasPageSections: () => hasPageSections.value,
  notify: toast,
})
const {
  mainFormRef, childRowScope, setChildFormRef, setRuntimeFormRef, setMainSectionFormRef,
  validateForms, clearChildForms, dispose: disposeFormRegistry,
} = formRegistry
const { dispatchFieldEvent, cancelFieldEvents } = useLowcodeFieldEvents({
  api,
  routeQuery,
  getContext: () => runtimeContext.value,
  getRowScope: row => childRowScope(row),
  notify: toast,
})
const searchRuntimeContext = computed(() => ({ ...runtimeContext.value, isSearch: true }))
const { flowHistory, flowHistoryLoading, loadFlowHistoryIfNeeded, runFlowAction } = useLowcodeFlowRuntime({
  api,
  getInteraction: () => flowInteraction.value,
  getTaskContext: () => ({
    taskId: currentFlowTaskId.value,
    processInstanceId: currentProcessInstanceId.value,
    taskDefKey: currentFlowNodeKey.value,
    objectCode: config.value.objectCode,
    recordId: mainData.id || currentId.value,
  }),
  confirmAction,
  promptActionInput,
  notify: toast,
  reload: id => runtimeData.loadDetail(id),
})
const runtimeData = useLowcodeRuntimeData({
  runtimeStore,
  api,
  getMainFields: () => mainFields.value,
  getChildren: () => allChildren.value,
  onFormLoad: dispatchFormLoad,
  onDetailLoaded: loadFlowHistoryIfNeeded,
  onFormReset: () => { flowHistory.value = []; clearChildForms() },
  handleError,
})
const { loadRuntime, loadList, loadListDebounced, resetSearch, loadDetail, initializeForm, dispose: disposeRuntimeData } = runtimeData

onLoad(async query => {
  runtimeStore.initializeRoute(query || {})
  await loadRuntime()
})

onUnload(() => {
  disposeRuntimeData()
  cancelFieldEvents()
  disposeFormRegistry()
})

function childRows(child) { return runtimeStore.childRows(child) }
function addChildRow(child) { runtimeStore.addChildRow(child) }
function removeChildRow(child, index) { runtimeStore.removeChildRow(child, index) }

function runtimeZoneFormSchema(zone) { return runtimeStore.runtimeZoneFormSchema(zone) }
function runtimeZoneMainFields(zone) { return runtimeStore.runtimeZoneMainFields(zone) }
function runtimeZoneNodes(zone) { return runtimeStore.runtimeZoneNodes(zone) }
function runtimeZoneHasSections(zone) { return runtimeStore.runtimeZoneHasSections(zone) }
function runtimeZoneBottomBar(zone) { return runtimeStore.runtimeZoneBottomBar(zone) }
function runtimeZoneFieldLinkages(zone) { return runtimeStore.runtimeZoneFieldLinkages(zone) }

function runtimeZoneActions(zone = {}) {
  const configured = Array.isArray(zone?.props?.actions)
    ? zone.props.actions
    : Array.isArray(zone?.actions) ? zone.actions : []
  const fallback = Array.isArray(config.value?.options?.actions) ? config.value.options.actions : []
  const source = configured.length ? configured : fallback
  return source
    .map(action => typeof action === 'string' ? resolveActionDefinition(config.value, { actionCode: action }) : resolveActionDefinition(config.value, action))
    .filter(action => action && actionVisible(action, mainData))
    .map(action => resolveActionPermission(action, authStore.permissions))
    .filter(Boolean)
}

function runtimeZoneChildren(zone) { return runtimeStore.runtimeZoneChildren(zone) }

async function handleRuntimeZoneAction(action) {
  const type = String(action?.type || '').toLowerCase()
  if (['save', 'reset', 'action', 'cancel', 'flow_action'].includes(type)) {
    await handleBottomAction(action)
    return
  }
  await runAction(action, mainData)
}

async function handleInlineNodeAction(action = {}) {
  const resolved = { ...(action.props || {}), ...action }
  if (String(resolved.type || '').toLowerCase() === 'back') {
    goBack()
    return
  }
  if (!resolved.actionCode && !resolved.key && !resolved.type) {
    toast('该页面操作尚未绑定业务动作', { type: 'warning' })
    return
  }
  await handleRuntimeZoneAction(resolved)
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({
    url: '/pages/index/index',
    fail: () => uni.reLaunch({ url: '/pages/index/index' }),
  })
}

function openCreate() { runtimeStore.openCreate(); initializeForm() }
function openDetail(row) { runtimeStore.openDetail(row); loadDetail(currentId.value).catch(handleError) }
function openEdit() { runtimeStore.openEdit() }
function goList() { runtimeStore.goList(); loadList().catch(handleError) }
function changePage(delta) { runtimeStore.changePage(delta); loadList().catch(handleError) }
function toggleSearch() { runtimeStore.toggleSearch() }

async function save() {
  await persistRecord()
}

async function persistRecord({ validate = true, navigate = true, notify = true, requireRecordId = false } = {}) {
  if (validate && !validateForms())
    return null
  saving.value = true
  try {
    const childrenPayload = runtimeStore.buildChildrenPayload()
    // DynamicCrud accepts the nested main/children envelope for master-detail
    // objects, while a plain object expects writable fields at the top level.
    // Sending `{ main: ... }` for the latter makes the server see no writable
    // fields at all.
    const payload = Object.keys(childrenPayload).length
      ? { main: { ...mainData }, children: childrenPayload }
      : { ...mainData }
    const response = mode.value === 'create'
      ? await api.createLowcodeRecord(configKey.value, payload)
      : await api.updateLowcodeRecord(configKey.value, payload)
    const savedMain = response?.data?.main || response?.data || null
    if (savedMain && typeof savedMain === 'object')
      Object.assign(mainData, savedMain)
    const recordId = mainData.id || mainData[config.value.rowKey || 'id']
    if (requireRecordId && !recordId)
      throw new Error('新建记录未返回主键，无法继续执行业务动作')
    if (recordId) {
      currentId.value = String(recordId)
      mode.value = 'edit'
    }
    if (notify)
      toast('保存成功', { type: 'success' })
    if (navigate)
      goList()
    return savedMain || mainData
  }
  catch (error) {
    handleError(error)
    return null
  }
  finally { saving.value = false }
}

async function handleMainFieldEvent(payload = {}) {
  await dispatchFieldEvent({ trigger: payload.trigger, field: payload.field || payload.action || payload.node || {}, data: payload.data, fields: mainFields.value, rules: formDesignerSchema.value?.settings?.governance?.fieldEvents, scan: payload.scan })
}
async function handleRuntimeZoneFieldEvent(zone, payload = {}) {
  const schema = runtimeZoneFormSchema(zone)
  await dispatchFieldEvent({ trigger: payload.trigger, field: payload.field || payload.action || payload.node || {}, data: payload.data, fields: runtimeZoneMainFields(zone), rules: schema?.settings?.governance?.fieldEvents, scan: payload.scan })
}
async function handleChildFieldEvent(child, row, { trigger, field, data, scan }) { await dispatchFieldEvent({ trigger, field, data, fields: child.fields, rules: child.fieldEvents, scan, child }) }

async function dispatchFormLoad(data, fields) {
  const rules = formDesignerSchema.value?.settings?.governance?.fieldEvents || []
  await dispatchFieldEvent({ trigger: 'FORM_LOAD', field: { field: '' }, data, fields, rules })
}

async function runAction(action, row, child, { confirmed = false } = {}) {
  const resolved = resolveActionDefinition(config.value, action)
  if (!actionVisible(resolved, row)) return
  if (!hasActionPermission(resolved, authStore.permissions)) {
    toast('当前账号无权执行此操作', { type: 'warning' })
    return
  }
  if (!confirmed && !await confirmAction(resolved)) return
  const inputs = actionInputSchema(resolved)
  const formData = {}
  for (const input of inputs) {
    const value = await promptActionInput(input)
    if (value === null) return
    formData[input.name] = input.type === 'INTEGER' || input.type === 'NUMBER' ? Number(value) : value
  }
  try {
    const objectCode = resolveActionObjectCode(child)
    const parentId = child ? (mainData.id || mainData[config.value.rowKey || 'id']) : undefined
    const response = await api.executeBusinessAction(buildActionPayload({
      action: resolved,
      config: config.value,
      objectCode,
      recordId: row?.id || row?.[config.value.rowKey || 'id'] || mainData.id,
      parentRecordId: parentId,
      childRecordId: child ? (row?.id || '') : undefined,
      relationKey: child?.relationKey,
      formData,
      routeQuery,
    }))
    const result = response?.data || {}
    if (String(result.executeStatus || '').toUpperCase() === 'FAILED') throw new Error(result.message || resolved.failureMessage || '动作执行失败')
    toast(resolved.successMessage || result.message || '操作成功', { type: 'success' })
    if (child && parentId) await loadDetail(parentId)
    else if (mode.value === 'list') await loadList()
    else if (mainData.id) await loadDetail(mainData.id)
  }
  catch (error) { handleError(error) }
}

async function handleBottomAction(action) {
  if (bottomActionLoading.value)
    return
  if (!hasActionPermission(action, authStore.permissions)) {
    toast('当前账号无权执行此操作', { type: 'warning' })
    return
  }
  bottomActionLoading.value = bottomActionKey(action)
  try {
    switch (String(action.type || '').toLowerCase()) {
      case 'save':
        await persistRecord()
        break
      case 'reset':
        initializeForm()
        toast('已清空', { type: 'info' })
        break
      case 'action':
        await runBottomBusinessAction(action)
        break
      case 'flow_action':
        await runFlowAction(action)
        break
      case 'cancel':
        goList()
        break
    }
  }
  finally {
    bottomActionLoading.value = ''
  }
}

async function runBottomBusinessAction(action) {
  const resolved = resolveActionDefinition(config.value, action)
  if (!actionVisible(resolved, mainData))
    return
  if (mode.value !== 'create') {
    await runAction(resolved, mainData)
    return
  }
  if (!validateForms())
    return
  if (!await confirmAction(resolved))
    return
  const saved = await persistRecord({ validate: false, navigate: false, notify: false, requireRecordId: true })
  if (saved)
    await runAction(resolved, mainData, null, { confirmed: true })
}

async function handleToolbarAction(action, child) {
  const resolved = resolveActionDefinition(config.value, action)
  if (!hasActionPermission(resolved, authStore.permissions)) {
    toast('当前账号无权执行此操作', { type: 'warning' })
    return
  }
  const actionRelationKey = resolved?.relationKey || child?.relationKey
  const targetChild = allChildren.value.find(c =>
    c.relationKey === actionRelationKey || c.key === actionRelationKey) || child
  if (!targetChild) {
    toast('未找到操作目标子表配置', { type: 'warning' })
    return
  }
  const items = childRows(targetChild).filter(row => row?.id)
  if (!items.length) {
    toast('请先添加并保存子表数据', { type: 'warning' })
    return
  }
  if (items.length === 1) {
    await runAction(resolved, items[0], targetChild)
    return
  }
  const labels = items.map(item => {
    const titleField = targetChild?.titleField
    const labelVal = titleField && item[titleField]
      ? item[titleField]
      : item.name || item.productName || item[config.value.rowKey || 'id'] || '记录'
    return String(labelVal)
  })
  const tapIndex = await new Promise(resolve => {
    uni.showActionSheet({
      itemList: labels,
      success: res => resolve(res.tapIndex),
      fail: () => resolve(-1),
    })
  })
  if (tapIndex < 0 || tapIndex >= items.length)
    return
  await runAction(resolved, items[tapIndex], targetChild)
}
function resolveActionObjectCode(child) {
  return child?.businessObjectCode
    || config.value?.objectCode
    || child?.objectCode
    || child?.targetObjectCode
}
function handleError(error) { toast(error?.message || '操作失败，请稍后重试', { type: 'error' }) }
function confirmAction(action) { return new Promise(resolve => uni.showModal({ title: action.label || action.actionName || '确认操作', content: action.confirmText || `确认执行“${action.label || action.actionName || '操作'}”吗？`, success: result => resolve(result.confirm) })) }
function promptActionInput(input) { return new Promise(resolve => uni.showModal({ title: input.label || input.name, editable: true, placeholderText: input.placeholder || `请输入${input.label || input.name}`, success: result => resolve(result.confirm ? result.content : null) })) }
function bottomActionKey(action) { return `${action.type}:${action.actionCode || action.label || ''}` }
</script>

<style lang="scss" scoped src="./styles/lowcode-runtime.scss"></style>

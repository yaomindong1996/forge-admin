<template>
  <AiLayoutPage :title="title" :subtitle="subtitle" :show-back="true">
    <view v-if="loading" class="runtime-state">
      <AiListSkeleton :rows="4" />
    </view>
    <view v-else-if="errorMessage" class="runtime-state">
      <AiResult type="error" :title="errorMessage" description="请检查应用发布状态或联系管理员" />
      <AiButton block variant="secondary" @click="loadRuntime">重新加载</AiButton>
    </view>

    <template v-else-if="mode === 'list'">
      <view class="runtime-list-head">
        <view class="runtime-toolbar__copy">
          <text class="runtime-toolbar__title">{{ config.tableComment || config.objectName || title }}</text>
          <text class="runtime-toolbar__desc">共 {{ total }} 条记录，按卡片浏览和办理</text>
        </view>
        <view class="runtime-list-head__actions">
          <AiButton v-if="searchFields.length" size="sm" variant="secondary" @click="toggleSearch">
            {{ searchExpanded ? '收起筛选' : `筛选${activeSearchCount ? `(${activeSearchCount})` : ''}` }}
          </AiButton>
          <AiButton size="sm" @click="openCreate">新增</AiButton>
        </view>
      </view>

      <view v-if="searchFields.length" class="runtime-filter-shell">
        <view v-if="!searchExpanded" class="runtime-filter-summary" @click="toggleSearch">
          <view>
            <text class="runtime-filter-summary__title">筛选条件</text>
            <text class="runtime-filter-summary__desc">{{ searchSummary }}</text>
          </view>
          <text class="runtime-filter-summary__arrow">展开</text>
        </view>
        <view v-else class="runtime-search-card">
          <LowcodeForm :fields="searchFields" :data="searchData" :dict-options="dictOptions" @update:data="loadListDebounced" />
          <view class="runtime-search-actions">
            <AiButton size="sm" variant="secondary" @click="resetSearch">重置</AiButton>
            <AiButton size="sm" @click="loadList">查询</AiButton>
          </view>
        </view>
      </view>

      <view v-if="records.length" class="runtime-record-list">
        <view v-for="row in records" :key="String(row[config.rowKey || 'id'])" class="runtime-record-card" @click="openDetail(row)">
          <view class="runtime-record-card__head">
            <text class="runtime-record-card__title">{{ rowTitle(row) }}</text>
            <text v-if="displayStatus(row)" class="runtime-record-card__status">{{ displayStatus(row) }}</text>
          </view>
          <view class="runtime-record-card__grid">
            <view v-for="column in visibleColumns" :key="column.prop || column.field" class="runtime-record-card__item">
              <text class="runtime-record-card__label">{{ column.label || column.title || column.prop }}</text>
              <text class="runtime-record-card__value">{{ formatValue(row[column.prop || column.field], column) }}</text>
            </view>
          </view>
          <view v-if="mainActions(row).length" class="runtime-actions" @click.stop>
            <AiButton v-for="action in mainActions(row)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="runAction(action, row)">
              {{ action.label || action.actionName || action.actionCode }}
            </AiButton>
          </view>
        </view>
      </view>
      <AiEmpty v-else title="暂无记录" description="点击右上角新增一条记录" />
      <view v-if="total > pageSize" class="runtime-pagination">
        <AiButton size="sm" variant="secondary" :disabled="page <= 1" @click="changePage(-1)">上一页</AiButton>
        <text>第 {{ page }} / {{ pageCount }} 页</text>
        <AiButton size="sm" variant="secondary" :disabled="page >= pageCount" @click="changePage(1)">下一页</AiButton>
      </view>
    </template>

    <template v-else>
      <view v-if="flowInteraction.timeline.enabled && (flowHistoryLoading || flowHistory.length)" class="runtime-flow-timeline">
        <text class="runtime-flow-timeline__title">{{ flowInteraction.timeline.title }}</text>
        <view v-if="flowHistoryLoading" class="runtime-flow-timeline__empty">正在加载审批记录</view>
        <view v-else v-for="(item, index) in flowHistory" :key="flowHistoryKey(item, index)" class="runtime-flow-timeline__item">
          <view class="runtime-flow-timeline__dot" />
          <view class="runtime-flow-timeline__copy">
            <text>{{ item.activityName || item.taskName || item.name || '流程节点' }}</text>
            <text>{{ item.assigneeName || item.userName || item.operatorName || '-' }} · {{ item.endTime || item.createTime || item.startTime || '-' }}</text>
            <text v-if="item.comment">{{ item.comment }}</text>
          </view>
        </view>
      </view>
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
          <template v-else-if="zone.zoneType === 'list'">
            <view v-for="child in runtimeZoneChildren(zone)" :key="child.key" class="runtime-child-card">
              <view class="runtime-child-card__head">
                <view>
                  <text class="runtime-child-card__title">{{ childTitle(child) }}</text>
                  <text class="runtime-child-card__count">{{ childSubtitle(child) || `${childRows(child).length} 条` }}</text>
                </view>
                <view class="runtime-child-card__tools">
                  <AiButton v-for="action in childToolbarActions(child)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="handleToolbarAction(action, child)">
                    {{ action.label || action.actionName || action.actionCode }}
                  </AiButton>
                  <AiButton v-if="mode !== 'detail' && child.inlineCreateEnabled !== false && child.readonly !== true" size="sm" variant="secondary" @click="addChildRow(child)">添加</AiButton>
                </view>
              </view>
              <view v-if="childRows(child).length" class="runtime-child-list">
                <view v-for="(row, rowIndex) in childRows(child)" :key="String(row.id || rowIndex)" class="runtime-child-row">
                  <view class="runtime-child-row__head">
                    <text class="runtime-child-row__title">第 {{ rowIndex + 1 }} 条</text>
                    <AiButton v-if="mode !== 'detail' && child.readonly !== true" size="sm" variant="danger" @click="removeChildRow(child, rowIndex)">删除</AiButton>
                  </view>
                  <LowcodeForm
                    :ref="instance => setChildFormRef(child, row, rowIndex, instance)"
                    :fields="child.fields"
                    :data="row"
                    :dict-options="dictOptions"
                    :current-children="childData"
                    :readonly="mode === 'detail' || child.readonly === true"
                    :context="runtimeContext"
                    :field-linkages="runtimeZoneFieldLinkages(zone)"
                    @field-event="payload => handleChildFieldEvent(child, row, payload)"
                  />
                </view>
              </view>
              <view v-else class="runtime-child-empty">暂无明细</view>
            </view>
          </template>
        </template>
        <view v-if="!hasComposedBottomBar && mode !== 'detail'" class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">取消</AiButton>
          <AiButton :loading="saving" @click="save">保存</AiButton>
        </view>
        <view v-else-if="!hasComposedBottomBar" class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">返回列表</AiButton>
          <AiButton v-if="canEdit" @click="openEdit">编辑</AiButton>
        </view>
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
        <view v-if="!hasConfiguredBottomBar && mode !== 'detail'" class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">取消</AiButton>
          <AiButton :loading="saving" @click="save">保存</AiButton>
        </view>
        <view v-else-if="!hasConfiguredBottomBar" class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">返回列表</AiButton>
          <AiButton v-if="canEdit" @click="openEdit">编辑</AiButton>
        </view>
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
          />
        </view>

        <view v-for="child in visibleChildren" :key="child.key" class="runtime-child-card">
          <view class="runtime-child-card__head">
            <view>
              <text class="runtime-child-card__title">{{ childTitle(child) }}</text>
              <text class="runtime-child-card__count">{{ childSubtitle(child) || `${childRows(child).length} 条` }}</text>
            </view>
            <view class="runtime-child-card__tools">
              <AiButton v-for="action in childToolbarActions(child)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="handleToolbarAction(action, child)">
                {{ action.label || action.actionName || action.actionCode }}
              </AiButton>
              <AiButton v-if="mode !== 'detail' && child.inlineCreateEnabled !== false && child.readonly !== true" size="sm" variant="secondary" @click="addChildRow(child)">添加</AiButton>
            </view>
          </view>
          <view v-if="childRows(child).length" class="runtime-child-list">
            <view v-for="(row, rowIndex) in childRows(child)" :key="String(row.id || rowIndex)" class="runtime-child-row">
              <view class="runtime-child-row__head">
                <text class="runtime-child-row__title">第 {{ rowIndex + 1 }} 条</text>
                <view v-if="mode !== 'detail' && child.readonly !== true" class="runtime-child-row__tools">
                  <AiButton size="sm" variant="danger" @click="removeChildRow(child, rowIndex)">删除</AiButton>
                </view>
              </view>
              <view class="runtime-child-row__body">
                <LowcodeForm
                  :ref="instance => setChildFormRef(child, row, rowIndex, instance)"
                  :fields="child.fields"
                  :data="row"
                  :dict-options="dictOptions"
                  :current-children="childData"
                  :readonly="mode === 'detail' || child.readonly === true"
                  :context="runtimeContext"
                  :field-linkages="fieldLinkages"
                  @field-event="payload => handleChildFieldEvent(child, row, payload)"
                />
              </view>
              <view v-if="childActions(child, row).length" class="runtime-actions runtime-actions--child">
                <AiButton v-for="action in childActions(child, row)" :key="action.actionCode || action.key" size="sm" variant="secondary" :disabled="action.disabled === true" @click="runAction(action, row, child)">
                  {{ action.label || action.actionName || action.actionCode }}
                </AiButton>
              </view>
            </view>
          </view>
          <view v-else class="runtime-child-empty">暂无明细</view>
        </view>

        <view v-if="mode !== 'detail'" class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">取消</AiButton>
          <AiButton :loading="saving" @click="save">保存</AiButton>
        </view>
        <view v-else class="runtime-footer-actions">
          <AiButton variant="secondary" @click="goList">返回列表</AiButton>
          <AiButton v-if="canEdit" @click="openEdit">编辑</AiButton>
        </view>
      </template>
    </template>
  </AiLayoutPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import AiButton from '@/components/AiButton.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import AiResult from '@/components/AiResult.vue'
import LowcodeForm from '@/components/lowcode/LowcodeForm.vue'
import PageSectionRenderer from '@/components/lowcode/PageSectionRenderer.vue'
import api from '@/api'
import { useAuthStore } from '@/store'
import { toast } from '@/utils/notify'
import {
  actionInputSchema,
  actionVisible,
  applyEventMappings,
  buildActionPayload,
  buildDefaultData,
  buildEventClearPatch,
  buildEventParams,
  ensureChildRows,
  hasActionPermission,
  normalizeActions,
  normalizeChildrenConfig,
  normalizeDesignerComponents,
  normalizeDictOptions,
  normalizeMainFields,
  normalizeField,
  normalizeScanContext,
  normalizeRuntimeFlowInteraction,
  hasComposedRuntimePageSchema,
  parseRuntimeConfig,
  resolveActionDefinition,
  resolveActionPermission,
  resolveChildRows,
  resolveChildTitle,
  resolveChildSubtitle,
  resolveFieldLinkages,
  mergeFlowActionsIntoBottomBar,
  resolveRuntimeFormDesignerSchema,
  resolveRuntimePageZones,
  resolveRuntimeZoneFormDesignerSchema,
  safeEventRules,
  shouldSkipFieldEvent,
  syncChildRowAliases,
} from '@/utils/lowcode-runtime'

const authStore = useAuthStore()
const routeQuery = reactive({})
const configKey = ref('')
const title = ref('低代码应用')
const subtitle = ref('移动端运行页')
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')
const config = ref({})
const mode = ref('list')
const currentId = ref('')
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const searchExpanded = ref(false)
const searchData = reactive({})
const mainData = reactive({})
const childData = reactive({})
const dictOptions = reactive({})
const mainFormRef = ref(null)
const mainSectionFormRefs = new Map()
const runtimeFormRefs = new Map()
const childFormRefs = new Map()
const childRowScopes = new WeakMap()
const fieldEventTimers = new Map()
const fieldEventControllers = new Map()
const fieldEventSequences = new Map()
const bottomActionLoading = ref('')
const flowHistory = ref([])
const flowHistoryLoading = ref(false)
let loadTimer
let childRowScopeSequence = 0

const formDesignerSchema = computed(() => resolveRuntimeFormDesignerSchema(config.value, mode.value))
const runtimePageZones = computed(() => hasComposedRuntimePageSchema(config.value)
  ? resolveRuntimePageZones(config.value, mode.value)
  : [])
const hasComposedPageZones = computed(() => runtimePageZones.value.length > 0)
const mainFields = computed(() => normalizeMainFields(config.value, formDesignerSchema.value))
const mainNodes = computed(() => normalizeDesignerComponents(config.value, formDesignerSchema.value))
const searchFields = computed(() => (Array.isArray(config.value.searchSchema) ? config.value.searchSchema : []).map(normalizeField).filter(field => field.field))
const visibleColumns = computed(() => (Array.isArray(config.value.columnsSchema) ? config.value.columnsSchema : []).filter(column => column?.prop || column?.field))
const hasPageSections = computed(() => Array.isArray(formDesignerSchema.value?.pageSections) && formDesignerSchema.value.pageSections.length > 0)
const pageSections = computed(() => formDesignerSchema.value?.pageSections || [])
const flowInteraction = computed(() => normalizeRuntimeFlowInteraction(config.value?.options?.flowInteraction))
const bottomBar = computed(() => mergeFlowActionsIntoBottomBar(formDesignerSchema.value?.bottomBar || {}, flowInteraction.value))
const fieldLinkages = computed(() => resolveFieldLinkages(formDesignerSchema.value, config.value))
const currentFlowNodeKey = computed(() => String(
  routeQuery.taskDefKey
  || routeQuery.taskDefinitionKey
  || routeQuery.nodeKey
  || mainData.taskDefKey
  || mainData.taskDefinitionKey
  || '',
))
const currentFlowTaskId = computed(() => String(routeQuery.taskId || mainData.taskId || ''))
const currentProcessInstanceId = computed(() => String(
  routeQuery.processInstanceId || mainData.processInstanceId || mainData.flowInstanceId || '',
))
const hasConfiguredBottomBar = computed(() => Array.isArray(bottomBar.value?.actions) && bottomBar.value.actions.length > 0)
const hasComposedBottomBar = computed(() => runtimePageZones.value.some(zone => {
  const schema = runtimeZoneFormSchema(zone)
  const hasBar = Array.isArray(schema?.bottomBar?.actions) && schema.bottomBar.actions.length > 0
  const hasSave = zone.zoneType === 'actions' && runtimeZoneActions(zone).some(action => String(action.type || '').toLowerCase() === 'save')
  return hasBar || hasSave
}))
const allChildren = computed(() => normalizeChildrenConfig(config.value))
const visibleChildren = computed(() => allChildren.value.filter(childVisibleInCurrentMode))
const runtimeContext = computed(() => ({
  routeQuery,
  user: authStore.userInfo || {},
  currentUser: authStore.userInfo || {},
}))
const canEdit = computed(() => mode.value === 'detail' && String(mainData.status || '').toUpperCase() === 'DRAFT')
const pageCount = computed(() => Math.max(1, Math.ceil(Number(total.value || 0) / pageSize)))
const activeSearchCount = computed(() => Object.values(searchData).filter(value => value !== undefined && value !== null && value !== '' && !(Array.isArray(value) && !value.length)).length)
const searchSummary = computed(() => activeSearchCount.value ? `已设置 ${activeSearchCount.value} 个条件` : '默认收起，点击后输入条件')

onLoad(async query => {
  Object.assign(routeQuery, query || {})
  configKey.value = String(query?.configKey || query?.config || resolveConfigKey(query?.path) || '').trim()
  title.value = String(query?.title || '低代码应用')
  subtitle.value = String(query?.subtitle || '移动端运行页')
  const requestedMode = String(query?.mode || '').toLowerCase()
  mode.value = requestedMode === 'create' ? 'create' : requestedMode === 'detail' ? 'detail' : requestedMode === 'edit' ? 'edit' : 'list'
  currentId.value = String(query?.recordId || query?.id || '')
  await loadRuntime()
})

onUnload(() => {
  clearTimeout(loadTimer)
  cancelFieldEvents()
  mainSectionFormRefs.clear()
  runtimeFormRefs.clear()
  childFormRefs.clear()
})

async function loadRuntime() {
  if (!configKey.value) {
    errorMessage.value = '缺少低代码配置标识'
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await api.getLowcodeRenderConfig(configKey.value, {
      appId: routeQuery.appId || undefined,
    })
    config.value = parseRuntimeConfig(response?.data || {})
    title.value = String(routeQuery.title || config.value.appName || config.value.objectName || title.value)
    await loadDictionaries()
    if (mode.value === 'list') await loadList()
    else if (currentId.value) await loadDetail(currentId.value)
    else initializeForm()
  }
  catch (error) {
    errorMessage.value = error?.message || '低代码运行配置加载失败'
  }
  finally {
    loading.value = false
  }
}

async function loadDictionaries() {
  const types = new Set()
  const collect = fields => fields.forEach(field => {
    if ((field.type === 'dictSelect' || field.type === 'pillSelect') && (field.dictType || field.props?.dictType)) types.add(field.dictType || field.props.dictType)
  })
  collect(mainFields.value)
  normalizeChildrenConfig(config.value).forEach(child => collect(child.fields))
  await Promise.all([...types].map(async type => {
    try { dictOptions[type] = normalizeDictOptions((await api.getDictOptions(type))?.data) }
    catch { dictOptions[type] = [] }
  }))
}

async function loadList() {
  const response = await api.getLowcodePage(configKey.value, { pageNum: page.value, pageSize, ...searchData })
  const data = response?.data || {}
  records.value = data.records || data.list || data.rows || []
  total.value = Number(data.total || records.value.length || 0)
}

function loadListDebounced() {
  clearTimeout(loadTimer)
  loadTimer = setTimeout(() => { page.value = 1; loadList().catch(handleError) }, 350)
}

function resetSearch() {
  Object.keys(searchData).forEach(key => delete searchData[key])
  page.value = 1
  loadList().catch(handleError)
}

async function loadDetail(id) {
  const response = await api.getLowcodeDetail(configKey.value, id)
  const data = response?.data || {}
  Object.keys(mainData).forEach(key => delete mainData[key])
  Object.assign(mainData, data.main || data)
  Object.keys(childData).forEach(key => delete childData[key])
  Object.assign(childData, data.children || {})
  normalizeChildrenConfig(config.value).forEach(child => { syncChildRowAliases(child, childData) })
  await dispatchFormLoad(mainData, mainFields.value)
  await loadFlowHistoryIfNeeded()
}

function initializeForm() {
  flowHistory.value = []
  childFormRefs.clear()
  Object.keys(mainData).forEach(key => delete mainData[key])
  Object.assign(mainData, buildMainDefaultData())
  Object.keys(childData).forEach(key => delete childData[key])
  normalizeChildrenConfig(config.value).forEach(child => { ensureChildRows(child, childData) })
  dispatchFormLoad(mainData, mainFields.value).catch(handleError)
}

function buildMainDefaultData() {
  const defaults = buildDefaultData(mainFields.value)
  const storedFields = Array.isArray(config.value.editSchema) ? config.value.editSchema : []
  storedFields.forEach((field) => {
    const fieldCode = String(field?.field || field?.sourceField || '').trim()
    const defaultValue = field?.defaultValue ?? field?.props?.defaultValue
    if (fieldCode && defaultValue !== undefined && defaults[fieldCode] === undefined)
      defaults[fieldCode] = defaultValue
  })
  return defaults
}

function childRows(child) { return resolveChildRows(child, childData) }
function addChildRow(child) { ensureChildRows(child, childData).push(buildDefaultData(child.fields)) }
function removeChildRow(child, index) { childRows(child).splice(index, 1) }
function childTitle(child) { return resolveChildTitle(child) }

function runtimeZoneFormSchema(zone = {}) {
  return resolveRuntimeZoneFormDesignerSchema(zone) || {}
}

function runtimeZoneMainFields(zone = {}) {
  return normalizeMainFields(config.value, runtimeZoneFormSchema(zone))
}

function runtimeZoneNodes(zone = {}) {
  return normalizeDesignerComponents(config.value, runtimeZoneFormSchema(zone))
}

function runtimeZoneHasSections(zone = {}) {
  return Array.isArray(runtimeZoneFormSchema(zone)?.pageSections)
    && runtimeZoneFormSchema(zone).pageSections.length > 0
}

function runtimeZoneBottomBar(zone = {}) {
  const schema = runtimeZoneFormSchema(zone)
  return mergeFlowActionsIntoBottomBar(schema?.bottomBar || bottomBar.value || {}, flowInteraction.value)
}

function runtimeZoneFieldLinkages(zone = {}) {
  return resolveFieldLinkages(runtimeZoneFormSchema(zone), config.value)
}

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

function runtimeZoneChildren(zone = {}) {
  const props = zone.props || {}
  const keys = [
    props.relationKey,
    props.childRelationKey,
    props.modelCode,
    ...(Array.isArray(props.relationKeys) ? props.relationKeys : []),
  ].filter(Boolean).map(String)
  const children = visibleChildren.value
  if (!keys.length)
    return children
  return children.filter(child => [child.key, child.relationKey, child.modelCode].filter(Boolean).some(key => keys.includes(String(key))))
}

async function handleRuntimeZoneAction(action) {
  const type = String(action?.type || '').toLowerCase()
  if (['save', 'reset', 'action', 'cancel', 'flow_action'].includes(type)) {
    await handleBottomAction(action)
    return
  }
  await runAction(action, mainData)
}

function setChildFormRef(child, row, rowIndex, instance) {
  if (!child)
    return
  const key = childFormRefKey(child, row, rowIndex)
  if (instance) childFormRefs.set(key, instance)
  else childFormRefs.delete(key)
}

function setRuntimeFormRef(zone, instance) {
  const key = String(zone?.zoneId || 'form')
  if (instance) runtimeFormRefs.set(key, instance)
  else runtimeFormRefs.delete(key)
}

function setMainSectionFormRef(zone, { sectionId, instance }) {
  const key = `${zone?.zoneId || 'legacy'}:${sectionId}`
  if (instance) mainSectionFormRefs.set(key, instance)
  else mainSectionFormRefs.delete(key)
}

function childFormRefKey(child, row, rowIndex) {
  return `${child.modelCode}:${row?.id || childRowScope(row, rowIndex)}`
}

function childRowScope(row, fallback = 0) {
  if (!row || typeof row !== 'object') return String(fallback)
  if (!childRowScopes.has(row)) childRowScopes.set(row, `new_${++childRowScopeSequence}`)
  return childRowScopes.get(row)
}

function openCreate() { mode.value = 'create'; currentId.value = ''; initializeForm() }
function openDetail(row) { currentId.value = String(row[config.value.rowKey || 'id']); mode.value = 'detail'; loadDetail(currentId.value).catch(handleError) }
function openEdit() { mode.value = 'edit' }
function goList() { mode.value = 'list'; currentId.value = ''; loadList().catch(handleError) }
function changePage(delta) { page.value = Math.min(pageCount.value, Math.max(1, page.value + delta)); loadList().catch(handleError) }
function toggleSearch() { searchExpanded.value = !searchExpanded.value }

function validateForms() {
  const mainForms = hasComposedPageZones.value
    ? [...runtimeFormRefs.values(), ...mainSectionFormRefs.values()]
    : hasPageSections.value ? [...mainSectionFormRefs.values()] : [mainFormRef.value]
  const mainValid = mainForms.length > 0 && mainForms.every(form => form?.validate?.() !== false)
  const childrenValid = [...childFormRefs.values()].every(form => form?.validate?.() !== false)
  if (!mainValid || !childrenValid) {
    toast('请完善必填字段', { type: 'warning' })
    return false
  }
  return true
}

async function save() {
  await persistRecord()
}

async function persistRecord({ validate = true, navigate = true, notify = true, requireRecordId = false } = {}) {
  if (validate && !validateForms())
    return null
  saving.value = true
  try {
    const childrenPayload = buildChildrenPayload()
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

function childVisibleInCurrentMode(child = {}) {
  if (mode.value === 'detail')
    return child.showInDetail !== false
  if (mode.value === 'edit')
    return child.showInEdit !== false
  return child.showInCreate !== false
}

function buildChildrenPayload() {
  return Object.fromEntries(normalizeChildrenConfig(config.value)
    .map(child => [child.modelCode, childRows(child)])
    .filter(([key]) => key))
}

async function handleMainFieldEvent({ trigger, field, data, scan }) { await dispatchFieldEvent(trigger, field, data, mainFields.value, formDesignerSchema.value?.settings?.governance?.fieldEvents, scan) }
async function handleRuntimeZoneFieldEvent(zone, { trigger, field, data, scan }) {
  const schema = runtimeZoneFormSchema(zone)
  await dispatchFieldEvent(trigger, field, data, runtimeZoneMainFields(zone), schema?.settings?.governance?.fieldEvents, scan)
}
async function handleChildFieldEvent(child, row, { trigger, field, data, scan }) { await dispatchFieldEvent(trigger, field, data, child.fields, child.fieldEvents, scan, child) }

async function dispatchFieldEvent(trigger, field, data, fields, rules, scan, child) {
  const normalizedTrigger = String(trigger || '').toUpperCase()
  const normalizedRules = safeEventRules(rules, fields).filter((rule) => {
    const ruleTrigger = String(rule.trigger || '').toUpperCase()
    return ruleTrigger === normalizedTrigger && (ruleTrigger === 'FORM_LOAD' || rule.sourceField === field.field)
  })
  const scope = child ? `${child.modelCode}:${childRowScope(data)}` : 'main'
  await Promise.all(normalizedRules.map(rule => scheduleFieldEvent(rule, data, scan, scope, normalizedTrigger)))
}

function scheduleFieldEvent(rule, data, scan, scope, trigger) {
  const key = `${scope}:${rule.id || rule.sourceKey}:${rule.sourceField || 'form'}`
  clearFieldEventTimer(key)
  if (shouldSkipFieldEvent(rule, data)) {
    cancelFieldEventRequest(key)
    Object.assign(data, buildEventClearPatch(rule))
    return Promise.resolve({ status: 'skipped' })
  }
  const delay = trigger === 'CHANGE' ? Math.max(0, Math.min(5000, Number(rule.debounceMs) || 0)) : 0
  if (!delay) return executeFieldEvent(rule, data, scan, key)
  cancelFieldEventRequest(key)
  return new Promise((resolve) => {
    const timer = setTimeout(async () => {
      fieldEventTimers.delete(key)
      resolve(await executeFieldEvent(rule, data, scan, key))
    }, delay)
    fieldEventTimers.set(key, { timer, resolve })
  })
}

async function executeFieldEvent(rule, data, scan, key) {
  cancelFieldEventRequest(key)
  const sequence = (fieldEventSequences.get(key) || 0) + 1
  fieldEventSequences.set(key, sequence)
  if (shouldSkipFieldEvent(rule, data)) {
    Object.assign(data, buildEventClearPatch(rule))
    return { status: 'skipped' }
  }
  if (rule.clearTargetsOnTrigger === true)
    Object.assign(data, buildEventClearPatch(rule))

  const controller = typeof AbortController === 'undefined' ? null : new AbortController()
  if (controller) fieldEventControllers.set(key, controller)
  try {
    const normalizedScan = normalizeScanContext(scan)
    const context = normalizedScan ? { ...runtimeContext.value, scan: normalizedScan } : runtimeContext.value
    const params = buildEventParams(rule, data, context, routeQuery)
    const response = await api.executeLowcodeQuerySource(
      { sourceType: rule.sourceType, sourceKey: rule.sourceKey, params },
      controller ? { signal: controller.signal } : {},
    )
    if (fieldEventSequences.get(key) !== sequence) return { status: 'stale' }
    const mapped = applyEventMappings(rule, unwrapQueryResult(response), data)
    if (mapped.found || Object.keys(mapped.patch).length) Object.assign(data, mapped.patch)
    if (!mapped.found && rule.notFoundMessage && String(rule.errorMode || 'MESSAGE').toUpperCase() !== 'SILENT')
      toast(rule.notFoundMessage, { type: 'warning' })
    return { status: mapped.found ? 'success' : 'not_found' }
  }
  catch (error) {
    if (fieldEventSequences.get(key) !== sequence || controller?.signal.aborted) return { status: 'cancelled' }
    if (rule.errorMessage && String(rule.errorMode || 'MESSAGE').toUpperCase() !== 'SILENT')
      toast(rule.errorMessage, { type: 'error' })
    console.warn('[lowcode h5] field event failed', error)
    return { status: 'error' }
  }
  finally {
    if (fieldEventControllers.get(key) === controller) fieldEventControllers.delete(key)
  }
}

function clearFieldEventTimer(key) {
  const pending = fieldEventTimers.get(key)
  if (!pending) return
  clearTimeout(pending.timer)
  fieldEventTimers.delete(key)
  pending.resolve({ status: 'cancelled' })
}

function cancelFieldEventRequest(key) {
  const controller = fieldEventControllers.get(key)
  if (controller && !controller.signal.aborted) controller.abort()
  fieldEventControllers.delete(key)
  fieldEventSequences.set(key, (fieldEventSequences.get(key) || 0) + 1)
}

function cancelFieldEvents() {
  for (const key of [...fieldEventTimers.keys()]) clearFieldEventTimer(key)
  for (const key of [...fieldEventControllers.keys()]) cancelFieldEventRequest(key)
}

async function dispatchFormLoad(data, fields) {
  const rules = formDesignerSchema.value?.settings?.governance?.fieldEvents || []
  await dispatchFieldEvent('FORM_LOAD', { field: '' }, data, fields, rules)
}

async function loadFlowHistoryIfNeeded() {
  if (!flowInteraction.value.timeline.enabled || !currentProcessInstanceId.value) {
    flowHistory.value = []
    return
  }
  flowHistoryLoading.value = true
  try {
    const response = await api.getFlowTaskHistory(currentProcessInstanceId.value)
    const data = response?.data || []
    flowHistory.value = Array.isArray(data) ? data : data.records || data.list || data.rows || []
  }
  catch (error) {
    flowHistory.value = []
    console.warn('[lowcode h5] flow history failed', error?.message || error)
  }
  finally {
    flowHistoryLoading.value = false
  }
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

async function runFlowAction(action) {
  if (!currentFlowTaskId.value) {
    toast('当前记录没有可办理的流程任务', { type: 'warning' })
    return
  }
  const operation = String(action.operation || 'approve')
  if (!await confirmAction(action))
    return
  const comment = await promptActionInput({ label: '审批意见', name: 'comment', placeholder: '可填写审批意见' })
  if (comment === null)
    return
  let targetUserId = ''
  if (operation === 'delegate') {
    targetUserId = await promptActionInput({ label: '委派用户 ID', name: 'targetUserId', placeholder: '请输入目标用户 ID' })
    if (!targetUserId)
      return
  }
  const payload = {
    action: operation,
    taskId: currentFlowTaskId.value,
    processInstanceId: currentProcessInstanceId.value || undefined,
    taskDefKey: currentFlowNodeKey.value || undefined,
    objectCode: config.value.objectCode || undefined,
    recordId: mainData.id || currentId.value || undefined,
    comment: String(comment || '').trim(),
    targetUserId: targetUserId || undefined,
  }
  const operationApi = {
    approve: api.approveFlowTask,
    reject: api.rejectFlowTask,
    return: api.returnFlowTask,
    delegate: api.delegateFlowTask,
  }[operation]
  if (!operationApi) {
    toast('不支持的流程操作', { type: 'warning' })
    return
  }
  await operationApi(payload)
  toast(`${action.label || '流程操作'}成功`, { type: 'success' })
  if (currentId.value)
    await loadDetail(currentId.value)
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

function mainActions(row) {
  return normalizeActions(config.value)
    .filter(action => !action.relationKey && actionVisible(action, row))
    .map(action => resolveActionPermission(action, authStore.permissions))
    .filter(Boolean)
}
function childActions(child, row) {
  return child.rowActions
    .map(action => resolveActionDefinition(config.value, action))
    .filter(action => actionVisible(action, row))
    .map(action => resolveActionPermission(action, authStore.permissions))
    .filter(Boolean)
}
function childToolbarActions(child) {
  return (Array.isArray(child.toolbarActions) ? child.toolbarActions : [])
    .map(action => resolveActionDefinition(config.value, action))
    .filter(action => action.actionCode || action.key)
    .map(action => resolveActionPermission(action, authStore.permissions))
    .filter(Boolean)
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
function childSubtitle(child) { return resolveChildSubtitle(child) }
function resolveActionObjectCode(child) {
  return child?.businessObjectCode
    || config.value?.objectCode
    || child?.objectCode
    || child?.targetObjectCode
}
function rowTitle(row) {
  const titleField = config.value?.options?.titleField
  if (titleField && row[titleField]) return String(row[titleField])
  const firstCol = visibleColumns.value[0]
  if (firstCol) {
    const val = row[firstCol.prop || firstCol.field]
    if (val !== undefined && val !== null && val !== '') return String(val)
  }
  return row[config.value.rowKey || 'id'] || config.value.objectName || '记录'
}
function displayStatus(row) {
  const statusField = config.value?.options?.statusField
  if (!statusField) return ''
  const val = row[statusField]
  if (val === undefined || val === null || val === '') return ''
  const dictType = config.value?.options?.statusDictType
  if (dictType)
    return dictOptions[dictType]?.find(item => String(item.value) === String(val))?.label || String(val)
  return String(val)
}
function formatValue(value, column = {}) { if (value === undefined || value === null || value === '') return '-'; if (column.dictType) return dictOptions[column.dictType]?.find(item => String(item.value) === String(value))?.label || value; return String(value) }
function handleError(error) { toast(error?.message || '操作失败，请稍后重试', { type: 'error' }) }
function confirmAction(action) { return new Promise(resolve => uni.showModal({ title: action.label || action.actionName || '确认操作', content: action.confirmText || `确认执行“${action.label || action.actionName || '操作'}”吗？`, success: result => resolve(result.confirm) })) }
function promptActionInput(input) { return new Promise(resolve => uni.showModal({ title: input.label || input.name, editable: true, placeholderText: input.placeholder || `请输入${input.label || input.name}`, success: result => resolve(result.confirm ? result.content : null) })) }
function unwrapQueryResult(response) { const value = response?.data; return value?.data !== undefined ? value.data : value }
function resolveConfigKey(path = '') { const match = String(path || '').match(/(?:crud-page|crud)\/([^/?]+)/); return match?.[1] || '' }
function queryString(query = {}) { return Object.entries(query).map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`).join('&') }
function bottomActionKey(action) { return `${action.type}:${action.actionCode || action.label || ''}` }
function flowHistoryKey(item, index) { return item.id || item.taskId || `${item.activityName || item.taskName || 'node'}:${index}` }
</script>

<style lang="scss" scoped>
.runtime-state { padding: 36rpx 0; }
.runtime-flow-timeline { margin-bottom: 24rpx; padding: 24rpx; border: 1rpx solid #e7edf5; border-radius: 16rpx; background: #fff; }
.runtime-flow-timeline__title { display: block; margin-bottom: 20rpx; color: #334155; font-size: 28rpx; font-weight: 700; }
.runtime-flow-timeline__empty { color: #94a3b8; font-size: 23rpx; }
.runtime-flow-timeline__item { position: relative; display: flex; gap: 18rpx; padding-bottom: 22rpx; }
.runtime-flow-timeline__item:not(:last-child)::before { content: ''; position: absolute; top: 16rpx; bottom: 0; left: 7rpx; width: 2rpx; background: #e5e7eb; }
.runtime-flow-timeline__dot { position: relative; z-index: 1; width: 16rpx; height: 16rpx; margin-top: 7rpx; border-radius: 50%; background: #2563eb; }
.runtime-flow-timeline__copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 6rpx; color: #64748b; font-size: 22rpx; line-height: 1.5; }
.runtime-flow-timeline__copy text:first-child { color: #334155; font-size: 25rpx; font-weight: 600; }
.runtime-list-head { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; padding: 22rpx 24rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-list-head__actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10rpx; }
.runtime-toolbar__copy { display: flex; flex-direction: column; gap: 6rpx; }
.runtime-toolbar__title, .runtime-form-card__title, .runtime-child-card__title { color: var(--text-strong); font-size: 30rpx; font-weight: 850; }
.runtime-toolbar__desc, .runtime-form-card__desc, .runtime-child-card__count { color: #94a3b8; font-size: 22rpx; }
.runtime-filter-shell { margin-bottom: 18rpx; }
.runtime-filter-summary { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; padding: 18rpx 22rpx; border: 1rpx solid #e7edf5; border-radius: 16rpx; color: #334155; background: #fff; }
.runtime-filter-summary view { display: flex; min-width: 0; flex-direction: column; gap: 5rpx; }
.runtime-filter-summary__title { color: #475569; font-size: 24rpx; font-weight: 750; }
.runtime-filter-summary__desc { overflow: hidden; color: #94a3b8; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-filter-summary__arrow { flex: 0 0 auto; color: #2563eb; font-size: 22rpx; font-weight: 700; }
.runtime-search-card, .runtime-form-card, .runtime-child-card { margin-bottom: 24rpx; padding: 26rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-search-actions, .runtime-actions, .runtime-footer-actions, .runtime-child-row__tools { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-zone-actions { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 12rpx; margin-bottom: 18rpx; padding: 18rpx 22rpx; border: 1rpx solid #e7edf5; border-radius: 16rpx; background: #fff; }
.runtime-search-actions { padding-top: 6rpx; }
.runtime-record-list, .runtime-child-list { display: flex; flex-direction: column; gap: 18rpx; }
.runtime-record-card { padding: 24rpx; border: 1rpx solid #e7edf5; border-radius: 18rpx; background: #fff; box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, .04); }
.runtime-record-card__head, .runtime-child-card__head, .runtime-form-card__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; margin-bottom: 18rpx; }
.runtime-record-card__status { padding: 5rpx 12rpx; border-radius: 999rpx; color: #2563eb; font-size: 21rpx; background: #eff6ff; }
.runtime-record-card__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16rpx 20rpx; }
.runtime-record-card__item { min-width: 0; display: flex; flex-direction: column; gap: 5rpx; }
.runtime-record-card__label { color: #94a3b8; font-size: 21rpx; }
.runtime-record-card__value { overflow: hidden; color: #334155; font-size: 24rpx; text-overflow: ellipsis; white-space: nowrap; }
.runtime-actions { margin-top: 20rpx; }
.runtime-actions--child { justify-content: flex-start; margin-top: 16rpx; }
.runtime-pagination { display: flex; align-items: center; justify-content: center; gap: 18rpx; padding: 28rpx 0; color: #64748b; font-size: 23rpx; }
.runtime-form-card__head { flex-direction: column; gap: 6rpx; }
.runtime-child-card__head { align-items: center; }
.runtime-child-card__head > view { display: flex; align-items: baseline; gap: 12rpx; }
.runtime-child-card__tools { display: flex; align-items: center; gap: 12rpx; flex-wrap: wrap; justify-content: flex-end; }
.runtime-child-row { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 16rpx; background: #fbfdff; }
.runtime-child-row__head { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; margin-bottom: 14rpx; }
.runtime-child-row__title { color: #334155; font-size: 24rpx; font-weight: 700; }
.runtime-child-row__tools { display: flex; align-items: center; justify-content: flex-end; gap: 12rpx; }
.runtime-child-row__body { padding: 4rpx 0 2rpx; }
.runtime-child-empty { padding: 30rpx 0; color: #94a3b8; font-size: 24rpx; text-align: center; }
.runtime-footer-actions { position: sticky; bottom: 0; z-index: 2; justify-content: stretch; padding: 18rpx 0 calc(18rpx + env(safe-area-inset-bottom)); background: rgba(248, 250, 252, .95); }
.runtime-footer-actions > * { flex: 1; }
</style>

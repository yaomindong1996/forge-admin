<template>
  <div class="application-runtime-page">
    <n-spin :show="loading">
      <template v-if="application">
        <header class="runtime-header">
          <div class="runtime-brand">
            <n-button quaternary circle aria-label="返回应用工作台" @click="openWorkspace">
              ←
            </n-button>
            <div class="runtime-brand-copy">
              <strong>{{ application.applicationName }}</strong>
              <span>{{ editing ? '正在编辑草稿' : currentNode?.title || '首页' }}</span>
            </div>
          </div>
          <n-space size="small">
            <n-button v-if="editing" :disabled="!dirty" :loading="saving" type="primary" @click="saveDraft">
              保存草稿
            </n-button>
            <n-button v-if="editing" @click="editing = false">
              退出编辑
            </n-button>
            <n-button v-else-if="canEditApplication" secondary @click="editing = true">
              编辑应用
            </n-button>
          </n-space>
        </header>

        <div class="runtime-body" :class="{ editing }">
          <aside class="runtime-navigation">
            <div class="navigation-head">
              <span>应用页面</span>
              <n-button v-if="editing" text size="small" @click="createVisible = true">
                + 添加
              </n-button>
            </div>
            <div class="navigation-list">
              <template v-for="item in navigationNodes" :key="item.id">
                <button
                  v-if="item.type === 'page'"
                  class="navigation-page"
                  :class="{ active: item.id === selectedNodeId }"
                  type="button"
                  :style="{ paddingLeft: `${14 + item.depth * 16}px` }"
                  @click="selectNode(item.id)"
                >
                  <span class="page-dot" />
                  <span>{{ item.title }}</span>
                </button>
                <div v-else class="navigation-group" :style="{ paddingLeft: `${14 + item.depth * 16}px` }">
                  <span>{{ item.title }}</span>
                  <n-button v-if="editing" text size="tiny" @click="openCreateInGroup(item.id)">
                    添加页面
                  </n-button>
                </div>
              </template>
            </div>
          </aside>

          <main class="runtime-main">
            <section v-if="currentNode" class="page-surface">
              <div class="page-heading">
                <div>
                  <span class="page-eyebrow">{{ pageTypeLabel }}</span>
                  <h1>{{ currentNode.title }}</h1>
                  <p>{{ currentPage?.description || '在这里开始搭建这个页面。' }}</p>
                </div>
                <n-button v-if="editing" secondary @click="insertComponent('page-title')">
                  添加标题
                </n-button>
              </div>

              <section v-if="currentNode.pageType === 'object'" class="object-page-card">
                <strong>{{ currentNode.objectRef?.objectCode || '未绑定业务对象' }}</strong>
                <p v-if="currentNode.objectRef?.valid === false">
                  绑定的业务对象已不可用，请重新选择。
                </p>
                <p v-else>
                  该页面复用已有对象的列表、表单、详情和 CRUD 运行配置。
                </p>
                <n-button v-if="editing && currentNode.objectRef?.objectCode" type="primary" secondary @click="openObjectDesigner">
                  配置数据页面
                </n-button>
              </section>

              <div v-if="!currentItems.length" class="empty-page-guide">
                <span>从一个简单区块开始</span>
                <h2>{{ currentNode.pageType === 'home' ? '把常用工作放到首页' : '这是一个新的页面' }}</h2>
                <p>选择一个常用组件，之后仍可在右侧调整标题和说明。</p>
                <div v-if="editing" class="recommendation-list">
                  <n-button v-for="item in recommendedComponents" :key="item.key" secondary @click="insertComponent(item.key)">
                    {{ item.label }}
                  </n-button>
                </div>
              </div>

              <div v-else class="page-component-list">
                <article
                  v-for="item in currentItems"
                  :key="item.id"
                  class="page-component"
                  :class="{ selected: item.id === selectedComponentId && editing }"
                  @click="editing && (selectedComponentId = item.id)"
                >
                  <strong>{{ item.props?.title || item.label }}</strong>
                  <p>{{ item.props?.description || item.props?.subtitle || item.props?.content || item.label }}</p>
                </article>
              </div>

              <n-button v-if="editing" class="insert-button" dashed block @click="componentVisible = true">
                + 添加组件
              </n-button>
            </section>
          </main>

          <aside v-if="editing" class="runtime-inspector">
            <div class="inspector-head">
              {{ selectedComponent ? '组件属性' : '页面设置' }}
            </div>
            <n-form label-placement="top" size="small">
              <template v-if="selectedComponent">
                <n-form-item label="组件标题">
                  <n-input :value="selectedComponent.props?.title || selectedComponent.label" @update:value="patchSelectedComponent({ title: $event })" />
                </n-form-item>
                <n-form-item label="说明文字">
                  <n-input type="textarea" :value="selectedComponent.props?.description || selectedComponent.props?.subtitle || ''" @update:value="patchSelectedComponent({ description: $event })" />
                </n-form-item>
                <n-button type="error" secondary block @click="removeSelectedComponent">
                  删除组件
                </n-button>
              </template>
              <template v-else>
                <n-form-item label="页面名称">
                  <n-input :value="currentNode?.title" @update:value="patchCurrentPage({ title: $event })" />
                </n-form-item>
                <n-form-item label="页面说明">
                  <n-input type="textarea" :value="currentPage?.description" @update:value="patchCurrentPage({ description: $event })" />
                </n-form-item>
                <n-form-item label="页面类型">
                  <n-input :value="pageTypeLabel" disabled />
                </n-form-item>
              </template>
            </n-form>
          </aside>
        </div>
      </template>
    </n-spin>

    <n-modal v-model:show="createVisible" preset="card" title="添加到应用" style="width: 440px">
      <n-form label-placement="top">
        <n-form-item label="类型">
          <n-radio-group v-model:value="createForm.type">
            <n-radio-button value="page">
              页面
            </n-radio-button>
            <n-radio-button value="group">
              页面组
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="名称">
          <n-input v-model:value="createForm.title" placeholder="例如：销售管理" />
        </n-form-item>
        <n-form-item v-if="createForm.type === 'page'" label="页面类型">
          <n-select v-model:value="createForm.pageType" :options="pageTypeOptions" />
        </n-form-item>
        <n-form-item label="所属页面组">
          <n-select v-model:value="createForm.parentId" clearable :options="groupOptions" placeholder="顶级菜单" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="createVisible = false">
            取消
          </n-button><n-button type="primary" @click="createNode">
            创建
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="componentVisible" preset="card" title="添加组件" style="width: 620px">
      <n-input v-model:value="componentKeyword" clearable placeholder="搜索组件" />
      <div class="component-picker">
        <button v-for="item in filteredComponents" :key="item.key" type="button" @click="insertComponent(item.key)">
          <strong>{{ item.label }}</strong><span>{{ item.description }}</span>
        </button>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessApplicationWorkspaceByCode, updateBusinessApplication } from '@/api/business-application'
import { useUserStore } from '@/store'
import {
  createNavigationNode,
  inAppPageTypes,
  insertPageComponent,
  mergeInAppBuilderOptions,
  normalizeInAppBuilder,
  updatePageComponent,
} from './in-app-builder/in-app-builder-schema'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const application = ref(null)
const objects = ref([])
const builder = ref(null)
const loading = ref(false)
const saving = ref(false)
const editing = ref(route.query.edit === '1')
const selectedNodeId = ref('page_home')
const selectedComponentId = ref('')
const createVisible = ref(false)
const componentVisible = ref(false)
const componentKeyword = ref('')
const savedSignature = ref('')
const createForm = ref({ type: 'page', title: '', pageType: 'content', parentId: null })

const recommendedComponents = [
  { key: 'intro', label: '介绍区', description: '标题和简短说明' },
  { key: 'metric-card', label: '指标卡', description: '展示核心数据' },
  { key: 'business-list', label: '业务列表', description: '展示常用记录' },
  { key: 'todo', label: '我的待办', description: '聚焦待办事项' },
]
const allComponents = [...recommendedComponents, { key: 'chart', label: '趋势图', description: '查看趋势变化' }, { key: 'text', label: '文本', description: '补充说明内容' }, { key: 'divider', label: '分隔线', description: '整理页面区块' }]
const pageTypeOptions = computed(() => inAppPageTypes.filter(item => item.value !== 'home'))
const groupOptions = computed(() => (builder.value?.nodes || []).filter(item => item.type === 'group').map(item => ({ label: item.title, value: item.id })))
const navigationNodes = computed(() => flattenNodes(builder.value?.nodes || []))
const currentNode = computed(() => builder.value?.nodes.find(item => item.id === selectedNodeId.value) || builder.value?.nodes.find(item => item.id === builder.value?.homePageId) || null)
const currentPage = computed(() => currentNode.value ? builder.value?.pages[currentNode.value.id] : null)
const currentItems = computed(() => currentPage.value?.layout?.items || [])
const selectedComponent = computed(() => currentItems.value.find(item => item.id === selectedComponentId.value) || null)
const pageTypeLabel = computed(() => pageTypeOptions.value.find(item => item.value === currentNode.value?.pageType)?.label || '首页')
const dirty = computed(() => JSON.stringify(builder.value || {}) !== savedSignature.value)
const canEditApplication = computed(() => {
  return userStore.isAdmin || hasPermission(userStore.permissions, 'ai:businessApplication:edit')
    || hasPermission(userStore.apiPermissions, 'ai:businessApplication:edit')
    || hasPermission(userStore.getDataPermission, 'ai:businessApplication:edit')
})
const filteredComponents = computed(() => {
  const keyword = componentKeyword.value.trim().toLowerCase()
  return allComponents.filter(item => !keyword || `${item.label}${item.description}`.toLowerCase().includes(keyword))
})

watch(() => route.params.applicationCode, load, { immediate: true })
watch(editing, (value) => {
  router.replace({ query: { ...route.query, edit: value ? '1' : undefined } })
})
onMounted(load)

async function load() {
  const code = String(route.params.applicationCode || '')
  if (!code)
    return
  loading.value = true
  try {
    const response = await businessApplicationWorkspaceByCode(code)
    const workspace = response.data || {}
    application.value = workspace.application || null
    objects.value = workspace.objects || []
    builder.value = normalizeInAppBuilder(application.value?.options, application.value, objects.value)
    savedSignature.value = JSON.stringify(builder.value)
    selectedNodeId.value = String(route.query.pageId || builder.value.homePageId)
  }
  finally {
    loading.value = false
  }
}

function selectNode(nodeId) {
  selectedNodeId.value = nodeId
  selectedComponentId.value = ''
  router.replace({ query: { ...route.query, pageId: nodeId, edit: editing.value ? '1' : undefined } })
}

function openCreateInGroup(groupId) {
  createForm.value = { type: 'page', title: '', pageType: 'content', parentId: groupId }
  createVisible.value = true
}

function createNode() {
  if (!createForm.value.title.trim()) {
    message.warning('请输入名称')
    return
  }
  const previousIds = new Set(builder.value.nodes.map(item => item.id))
  builder.value = createNavigationNode(builder.value, createForm.value)
  const created = builder.value.nodes.find(item => !previousIds.has(item.id))
  createVisible.value = false
  createForm.value = { type: 'page', title: '', pageType: 'content', parentId: null }
  if (created?.type === 'page')
    selectNode(created.id)
}

function insertComponent(componentKey) {
  if (!currentNode.value)
    return
  const component = allComponents.find(item => item.key === componentKey) || { key: componentKey, label: componentKey, description: '' }
  const result = insertPageComponent(builder.value, currentNode.value.id, { componentKey: component.key, label: component.label })
  builder.value = result.schema
  selectedComponentId.value = result.selectedComponentId
  componentVisible.value = false
}

function patchSelectedComponent(props) {
  if (!selectedComponent.value)
    return
  builder.value = updatePageComponent(builder.value, currentNode.value.id, selectedComponent.value.id, { props })
}

function patchCurrentPage(patch) {
  const node = builder.value.nodes.find(item => item.id === currentNode.value?.id)
  if (!node)
    return
  if (patch.title !== undefined)
    node.title = patch.title
  builder.value.pages[node.id] = { ...builder.value.pages[node.id], ...patch }
}

function removeSelectedComponent() {
  builder.value.pages[currentNode.value.id].layout.items = currentItems.value.filter(item => item.id !== selectedComponentId.value)
  selectedComponentId.value = ''
}

async function saveDraft() {
  if (!application.value || saving.value)
    return
  saving.value = true
  try {
    await updateBusinessApplication({
      id: application.value.id,
      applicationCode: application.value.applicationCode,
      applicationName: application.value.applicationName,
      suiteCode: application.value.suiteCode,
      icon: application.value.icon,
      description: application.value.description,
      status: application.value.status,
      options: JSON.stringify(mergeInAppBuilderOptions(application.value.options, builder.value)),
    })
    application.value.options = JSON.stringify(mergeInAppBuilderOptions(application.value.options, builder.value))
    savedSignature.value = JSON.stringify(builder.value)
    message.success('应用草稿已保存')
  }
  finally {
    saving.value = false
  }
}

function openWorkspace() {
  router.push({ name: 'BusinessApplicationWorkspace', params: { applicationCode: application.value.applicationCode } })
}

function openObjectDesigner() {
  router.push({
    name: 'BusinessObjectDesigner',
    params: { objectCode: currentNode.value.objectRef.objectCode },
    query: { objectId: currentNode.value.objectRef.objectId, returnTo: route.fullPath },
  })
}

function flattenNodes(nodes, parentId = null, depth = 0) {
  return nodes.filter(item => item.parentId === parentId).sort((a, b) => a.sort - b.sort).flatMap(item => [
    { ...item, depth },
    ...flattenNodes(nodes, item.id, depth + 1),
  ])
}

function hasPermission(source, permission) {
  return Array.isArray(source) && (source.includes(permission) || source.includes('**') || source.includes('*:*:*'))
}
</script>

<style scoped>
.application-runtime-page {
  min-height: 100%;
  background: #f7f8fa;
  color: #1d2129;
}
.runtime-header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}
.runtime-brand,
.runtime-brand-copy {
  display: flex;
  align-items: center;
}
.runtime-brand {
  gap: 8px;
  min-width: 0;
}
.runtime-brand-copy {
  gap: 9px;
}
.runtime-brand-copy strong {
  font-size: 15px;
}
.runtime-brand-copy span {
  color: #86909c;
  font-size: 12px;
}
.runtime-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: calc(100vh - 58px);
}
.runtime-body.editing {
  grid-template-columns: 240px minmax(0, 1fr) 280px;
}
.runtime-navigation,
.runtime-inspector {
  background: #fff;
}
.runtime-navigation {
  border-right: 1px solid #e5e6eb;
}
.runtime-inspector {
  border-left: 1px solid #e5e6eb;
  padding: 16px;
}
.navigation-head,
.navigation-group,
.navigation-page {
  display: flex;
  align-items: center;
}
.navigation-head {
  justify-content: space-between;
  padding: 15px 14px 8px;
  color: #4e5969;
  font-size: 12px;
  font-weight: 650;
}
.navigation-list {
  padding: 4px 6px;
}
.navigation-page {
  width: 100%;
  min-height: 34px;
  gap: 8px;
  border: 0;
  border-radius: 5px;
  color: #4e5969;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.navigation-page:hover,
.navigation-page.active {
  color: #165dff;
  background: #f2f3f5;
}
.page-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.navigation-group {
  justify-content: space-between;
  min-height: 34px;
  color: #86909c;
  font-size: 12px;
  font-weight: 600;
}
.runtime-main {
  min-width: 0;
  padding: 18px;
}
.page-surface {
  min-height: calc(100vh - 94px);
  padding: 24px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
}
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f2f3f5;
}
.page-eyebrow {
  color: #86909c;
  font-size: 12px;
}
.page-heading h1 {
  margin: 5px 0;
  font-size: 22px;
}
.page-heading p,
.object-page-card p {
  margin: 0;
  color: #86909c;
  font-size: 13px;
}
.object-page-card,
.empty-page-guide {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #e5e6eb;
  border-radius: 7px;
  background: #fbfcfd;
}
.object-page-card {
  display: grid;
  gap: 10px;
}
.empty-page-guide {
  text-align: center;
}
.empty-page-guide > span {
  color: #165dff;
  font-size: 12px;
}
.empty-page-guide h2 {
  margin: 7px 0;
  font-size: 18px;
}
.empty-page-guide p {
  margin: 0;
  color: #86909c;
}
.recommendation-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}
.page-component-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}
.page-component {
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 7px;
  cursor: pointer;
}
.page-component.selected {
  border-color: #165dff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1);
}
.page-component p {
  margin: 6px 0 0;
  color: #86909c;
  font-size: 13px;
}
.insert-button {
  margin-top: 14px;
}
.inspector-head {
  margin-bottom: 14px;
  font-size: 13px;
  font-weight: 650;
}
.component-picker {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.component-picker button {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}
.component-picker button:hover {
  border-color: #165dff;
}
.component-picker span {
  color: #86909c;
  font-size: 12px;
}
@media (max-width: 980px) {
  .runtime-body.editing {
    grid-template-columns: 200px minmax(0, 1fr);
  }
  .runtime-inspector {
    grid-column: 1 / -1;
    border-top: 1px solid #e5e6eb;
    border-left: 0;
  }
  .runtime-header {
    height: auto;
    min-height: 58px;
    padding-block: 8px;
  }
}
</style>

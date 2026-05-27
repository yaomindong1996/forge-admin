<template>
  <div class="object-detail-page">
    <header class="object-head">
      <n-button text @click="backToSuite">
        返回套件
      </n-button>
      <div class="object-title">
        <span>
          <n-icon><CubeOutline /></n-icon>
        </span>
        <div>
          <h1>{{ object?.objectName || objectCode }}</h1>
          <p>{{ object?.description || '业务对象详情' }}</p>
        </div>
        <DictTag v-if="object" dict-type="sys_enable_disable" :value="object.status" :bordered="false" />
      </div>
      <div class="object-actions">
        <n-button type="primary" :disabled="!runtimeInfo?.canOpen" @click="openRuntime">
          <template #icon>
            <n-icon><OpenOutline /></n-icon>
          </template>
          打开业务入口
        </n-button>
        <n-button secondary @click="activeTab = 'capability'">
          接入能力
        </n-button>
      </div>
    </header>

    <n-tabs v-model:value="activeTab" type="line" animated class="object-tabs">
      <n-tab-pane name="relation" tab="关系">
        <ObjectRelationPanel :object-id="object?.id" />
      </n-tab-pane>
      <n-tab-pane name="capability" tab="能力">
        <BusinessBindingPanel
          v-if="object"
          target-type="OBJECT"
          :target-id="object.id"
          :target-code="object.objectCode"
        />
      </n-tab-pane>
      <n-tab-pane name="layout" tab="布局">
        <div class="placeholder-grid">
          <div v-for="item in layoutItems" :key="item.title" class="placeholder-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </n-tab-pane>
      <n-tab-pane name="automation" tab="自动化">
        <BusinessBindingPanel
          v-if="object"
          target-type="OBJECT"
          :target-id="object.id"
          :target-code="object.objectCode"
        />
      </n-tab-pane>
      <n-tab-pane name="developer" tab="开发者信息">
        <div class="developer-info">
          <div><span>对象编码</span><code>{{ object?.objectCode || '-' }}</code></div>
          <div><span>模型编码</span><code>{{ object?.modelCode || '-' }}</code></div>
          <div><span>运行配置</span><code>{{ runtimeInfo?.configKey || '-' }}</code></div>
          <div><span>运行路由</span><code>{{ runtimeInfo?.routePath || '-' }}</code></div>
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup>
import { CubeOutline, OpenOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessObjectList, businessObjectRuntimeInfo } from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'
import { useTabStore } from '@/store'
import BusinessBindingPanel from './components/BusinessBindingPanel.vue'
import ObjectRelationPanel from './components/ObjectRelationPanel.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabStore = useTabStore()
const activeTab = ref('relation')
const object = ref(null)
const runtimeInfo = ref(null)
const objectCode = computed(() => route.params.objectCode)
const suiteCode = computed(() => route.query.suiteCode || object.value?.suiteCode)
const pageTitle = computed(() => object.value?.objectName || objectCode.value || '业务对象详情')

const layoutItems = [
  { title: '列表布局', desc: '维护业务对象在列表中的字段展示、筛选和排序方式。' },
  { title: '表单布局', desc: '维护新增、编辑时的字段分组、校验和交互规则。' },
  { title: '详情布局', desc: '组织基本信息、关联列表、审批和记录等详情区域。' },
  { title: '导入导出', desc: '从对象进入导入模板、导出字段和报表模板配置。' },
]

onMounted(loadObject)

watch(pageTitle, (title) => {
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${import.meta.env.VITE_TITLE}`
  tabStore.updateTabTitle(route.fullPath, title)
}, { immediate: true })

async function loadObject() {
  const res = await businessObjectList({
    suiteCode: suiteCode.value,
    objectCode: objectCode.value,
  })
  object.value = (res.data || [])[0] || null
  if (object.value) {
    const runtimeRes = await businessObjectRuntimeInfo(object.value.id)
    runtimeInfo.value = runtimeRes.data || null
  }
}

function backToSuite() {
  if (suiteCode.value)
    router.push(`/app-center/suite/${suiteCode.value}`)
  else
    router.push('/app-center')
}

function openRuntime() {
  if (!runtimeInfo.value?.canOpen) {
    message.warning(runtimeInfo.value?.message || '业务入口暂不可打开')
    return
  }
  router.push(runtimeInfo.value.routePath)
}
</script>

<style scoped>
.object-detail-page {
  min-height: 100%;
  background: #f6f8fb;
  padding: 20px;
}

.object-head,
.object-tabs {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.object-head {
  margin-bottom: 16px;
  padding: 16px;
}

.object-title {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-top: 12px;
}

.object-title > span {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 24px;
}

.object-title h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  letter-spacing: 0;
}

.object-title p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.object-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.object-tabs {
  padding: 8px 16px 16px;
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.placeholder-item,
.developer-info > div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.placeholder-item strong {
  color: #111827;
  font-size: 14px;
}

.placeholder-item p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.developer-info {
  display: grid;
  gap: 10px;
}

.developer-info > div {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.developer-info span {
  color: #6b7280;
  font-size: 13px;
}

.developer-info code {
  overflow-wrap: anywhere;
  color: #111827;
  font-size: 13px;
}

@media (max-width: 620px) {
  .object-detail-page {
    padding: 12px;
  }

  .object-title {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .object-title :deep(.n-tag) {
    grid-column: 2;
    justify-self: start;
  }

  .object-actions {
    flex-direction: column;
  }
}
</style>

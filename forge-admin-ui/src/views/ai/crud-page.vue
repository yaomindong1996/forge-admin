<template>
  <div class="ai-crud-page-wrapper">
    <component
      :is="currentTemplate"
      v-if="configLoaded && currentTemplate"
      :crud-props="crudProps"
    />
    <AiCrudPage
      v-else-if="configLoaded && !currentTemplate"
      v-bind="crudProps"
    />
    <div v-else-if="loading" class="loading-wrapper">
      <n-spin size="large" description="加载配置中..." />
    </div>
    <div v-else-if="errorMsg" class="error-wrapper">
      <n-result status="error" :title="errorMsg">
        <template #footer>
          <n-button @click="loadConfig">
            重新加载
          </n-button>
        </template>
      </n-result>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, h, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import catalog from '@/catalog'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getDictData } from '@/composables/useDict'
import { useTabStore } from '@/store'

const route = useRoute()
const tabStore = useTabStore()

const loading = ref(false)
const configLoaded = ref(false)
const errorMsg = ref('')
const renderConfig = ref(null)
const dictCache = ref({})

/** 当前加载的模板组件（null 表示降级到 AiCrudPage） */
const currentTemplate = ref(null)

/**
 * 转换表格列配置：将 JSON 格式的 render 对象转为 Vue render 函数
 * 如果配置了 transConfig，则使用翻译后的 xxxName 字段直接显示文本
 */
function transformColumns(columns, transConfig) {
  // 构建翻译映射: { field -> targetField }
  const transMap = {}
  if (transConfig && typeof transConfig === 'object') {
    for (const [field, conf] of Object.entries(transConfig)) {
      transMap[field] = conf.targetField || (`${field}Name`)
    }
  }

  const result = (columns || []).map((col) => {
    // 统一提取字段名，优先级：prop > key > dataIndex
    const key = col.prop || col.key || col.dataIndex
    // 统一补prop字段，AiTable需要这个字段来匹配数据
    const newCol = { ...col, prop: key }

    // 如果该字段有翻译配置，优先显示翻译后的值，没有则显示原字段值
    if (transMap[key]) {
      const targetField = transMap[key]
      newCol.render = row => row[targetField] ?? row[key]
      return newCol
    }
    // dictTag 渲染
    if (col.render && typeof col.render === 'object' && col.render.type === 'dictTag') {
      newCol.render = row => h(DictTag, {
        dictType: col.render.dictType,
        value: row[key],
        size: 'small',
      })
      return newCol
    }
    return newCol
  })

  return result
}

/**
 * 转换表单字段配置：为 dictType 字段注入字典选项，为日期字段配置格式化
 */
function transformFields(fields) {
  return (fields || []).map((field) => {
    const newField = { ...field }

    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      const options = dictCache.value[field.dictType] || []
      newField.props = {
        ...(newField.props || {}),
        options,
      }
    }

    // 数字类型字段自动转换类型
    if (['number', 'inputNumber'].includes(field.type)) {
      newField.onMounted = (vm) => {
        if (vm.field && vm.value) {
          // 如果值是字符串，尝试转换成数字
          if (typeof vm.value === 'string') {
            const num = Number.parseFloat(vm.value)
            if (!isNaN(num)) {
              vm.value = num
            }
          }
        }
      }
    }

    // 日期/时间类型字段配置格式化，直接返回标准时间字符串
    if (['date', 'datetime', 'time'].includes(field.type?.toLowerCase())) {
      newField.props = {
        ...(newField.props || {}),
        format: 'yyyy-MM-dd HH:mm:ss',
        valueFormat: 'yyyy-MM-dd HH:mm:ss',
      }
    }

    return newField
  })
}

const crudProps = computed(() => {
  if (!renderConfig.value)
    return {}
  const cfg = renderConfig.value
  return {
    searchSchema: transformFields(cfg.searchSchema),
    columns: transformColumns(cfg.columnsSchema, cfg.transConfig),
    editSchema: transformFields(cfg.editSchema),
    apiConfig: cfg.apiConfig || {},
    options: cfg.options || {},
    rowKey: cfg.rowKey || 'id',
    modalType: cfg.modalType || 'drawer',
    modalWidth: cfg.modalWidth || '800px',
    editGridCols: cfg.editGridCols || 1,
    searchGridCols: cfg.searchGridCols || 4,
  }
})

/**
 * 预加载所有用到的字典数据
 */
async function preloadDicts(cfg) {
  const types = new Set()

  // 从 columnsSchema 提取 dictType
  ;(cfg.columnsSchema || []).forEach((col) => {
    if (col.render?.dictType)
      types.add(col.render.dictType)
  })

  // 从 searchSchema / editSchema 提取 dictType
  ;[...(cfg.searchSchema || []), ...(cfg.editSchema || [])].forEach((field) => {
    if (field.dictType)
      types.add(field.dictType)
  })

  for (const type of types) {
    if (!dictCache.value[type]) {
      try {
        dictCache.value[type] = await getDictData(type)
      }
      catch (e) {
        console.warn(`[crud-page] 加载字典 ${type} 失败`, e)
      }
    }
  }
}

async function loadConfig() {
  // 支持三种格式：
  // 1. /ai/crud-page/:configKey （route.params，unplugin-vue-router 动态路由）
  // 2. /ai/crud-page/order_manage （从 route.path 解析，permission.js 静态路由）
  // 3. /ai/crud-page?configKey=xxx （旧的 query 格式）
  const configKey = route.params?.configKey
    || route.path.replace(/^\/ai\/crud-page\//, '') || route.query.configKey
  if (!configKey || configKey === '/ai/crud-page' || configKey.startsWith('/')) {
    errorMsg.value = '缺少 configKey 参数'
    return
  }

  loading.value = true
  errorMsg.value = ''
  configLoaded.value = false

  try {
    const res = await crudConfigRender(configKey)
    console.log('[DEBUG] 后端返回的CRUD配置原始响应：', res)
    const cfg = res.data
    console.log('[DEBUG] 解析后的CRUD配置：', cfg)
    console.log('[DEBUG] 列配置columnsSchema：', cfg.columnsSchema)
    console.log('[DEBUG] 翻译配置transConfig：', cfg.transConfig)
    renderConfig.value = cfg
    // 更新当前 Tab 标题为菜单名/表描述
    const title = cfg.menuName || cfg.tableComment
    if (title) {
      const currentPath = route.fullPath
      const existingTab = tabStore.tabs.find(t => t.path === currentPath)
      if (existingTab) {
        existingTab.title = title
      }
    }
    await preloadDicts(cfg)
    // 加载模板组件
    const layoutType = cfg.layoutType || 'simple-crud'
    const catalogEntry = catalog[layoutType]
    if (catalogEntry) {
      currentTemplate.value = defineAsyncComponent(catalogEntry.component)
    }
    else {
      // 未注册的模板，降级使用 AiCrudPage
      currentTemplate.value = null
    }
    configLoaded.value = true
  }
  catch (e) {
    errorMsg.value = e.message || '加载配置失败'
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  loadConfig()
})

// 监听 configKey 变化，兼容各种路由方式
watch(
  () => route.params?.configKey || route.path.replace(/^\/ai\/crud-page\//, '') || route.query.configKey,
  (newKey, oldKey) => {
    if (newKey && newKey !== oldKey) {
      loadConfig()
    }
  },
)
</script>

<style scoped>
.ai-crud-page-wrapper {
  height: 100%;
}

.loading-wrapper,
.error-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}
</style>

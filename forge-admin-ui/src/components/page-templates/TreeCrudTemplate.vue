<template>
  <!--
    TreeCrudTemplate - 树形 CRUD 页面模板
    适合具有父子层级结构的数据（部门、分类等）
    布局：左侧树 + 右侧标准 CRUD 表格
    当 treeConfig 未配置时，降级为标准 CRUD
  -->
  <div v-if="hasTree" class="tree-crud-layout">
    <!-- 左侧树形导航 -->
    <div class="tree-crud-left">
      <div class="tree-header">
        {{ treeTitle }}
      </div>
      <n-spin :show="treeLoading">
        <n-tree
          :data="treeData"
          :key-field="treeConfig.keyField || 'id'"
          :label-field="treeConfig.labelField || 'name'"
          :children-field="treeConfig.childrenField || 'children'"
          block-line
          default-expand-all
          :selected-keys="selectedKeys"
          @update:selected-keys="handleTreeSelect"
        />
      </n-spin>
    </div>
    <!-- 右侧 CRUD 表格 -->
    <div class="tree-crud-right">
      <AiCrudPage v-bind="mergedCrudProps" />
    </div>
  </div>
  <!-- 无树形配置时降级为标准 CRUD -->
  <AiCrudPage v-else v-bind="crudProps" />
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import { request } from '@/utils'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

// 从 options 中读取 treeConfig
const treeConfig = computed(() => {
  return props.crudProps?.options?.treeConfig || null
})

const hasTree = computed(() => !!treeConfig.value)
const treeTitle = computed(() => treeConfig.value?.treeTitle || '树形分类')

const treeLoading = ref(false)
const treeData = ref([])
const selectedKeys = ref([])
const selectedId = ref(null)

// 合并 crudProps，注入 parentId 过滤
const mergedCrudProps = computed(() => {
  if (!selectedId.value)
    return props.crudProps
  const apiConfig = { ...(props.crudProps.apiConfig || {}) }
  // 将 parentId 注入到 list 接口的默认参数中
  return {
    ...props.crudProps,
    apiConfig,
    defaultSearchParams: {
      ...(props.crudProps.defaultSearchParams || {}),
      [treeConfig.value?.parentField || 'parentId']: selectedId.value,
    },
  }
})

async function loadTreeData() {
  if (!treeConfig.value)
    return
  const apiConfig = props.crudProps?.apiConfig || {}
  // 优先使用 tree 接口，fallback 到 list 接口
  const treeApi = apiConfig.tree || apiConfig.list
  if (!treeApi)
    return
  const [method, url] = treeApi.split('@')
  if (!url)
    return
  treeLoading.value = true
  try {
    const res = await request[method.toLowerCase()](url)
    if (res.code === 200) {
      treeData.value = res.data || []
    }
  }
  catch (e) {
    console.warn('[TreeCrudTemplate] 加载树形数据失败:', e.message)
  }
  finally {
    treeLoading.value = false
  }
}

function handleTreeSelect(keys) {
  selectedKeys.value = keys
  selectedId.value = keys[0] ?? null
}

onMounted(() => {
  if (hasTree.value) {
    loadTreeData()
  }
})

watch(() => props.crudProps?.apiConfig, () => {
  if (hasTree.value) {
    loadTreeData()
  }
}, { deep: true })
</script>

<style scoped>
.tree-crud-layout {
  display: flex;
  height: 100%;
  gap: 0;
}

.tree-crud-left {
  width: 220px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  padding: 12px 8px;
  overflow-y: auto;
  background: #fafafa;
}

.tree-header {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  padding: 0 6px 10px;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 8px;
}

.tree-crud-right {
  flex: 1;
  overflow: hidden;
}
</style>

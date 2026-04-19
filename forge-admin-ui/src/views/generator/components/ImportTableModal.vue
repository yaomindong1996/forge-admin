<template>
  <n-modal
    v-model:show="visible"
    title="导入表结构"
    preset="card"
    style="width: 1000px"
    :mask-closable="false"
  >
    <div class="import-table-modal">
      <!-- 数据源选择和搜索 -->
      <div class="search-bar">
        <n-space :size="16" align="center" style="width: 100%">
          <n-form-item label="数据源" label-placement="left" :show-feedback="false">
            <n-select
              v-model:value="selectedDatasourceId"
              :options="datasourceOptions"
              placeholder="请选择数据源"
              style="width: 250px"
              :loading="datasourceLoading"
              @update:value="handleDatasourceChange"
            />
          </n-form-item>
          <n-form-item label="表名" label-placement="left" :show-feedback="false">
            <n-input
              v-model:value="searchKeyword"
              placeholder="搜索表名或描述"
              clearable
              style="width: 250px"
              @input="handleSearch"
            >
              <template #prefix>
                <i class="i-material-symbols:search" />
              </template>
            </n-input>
          </n-form-item>
          <n-space>
            <n-tag v-if="checkedKeys.length > 0" type="primary" :bordered="false">
              已选择 {{ checkedKeys.length }} 张表
            </n-tag>
            <n-button
              v-if="checkedKeys.length > 0"
              text
              @click="handleClearSelection"
            >
              清空选择
            </n-button>
          </n-space>
        </n-space>
      </div>

      <!-- 表列表 -->
      <div class="table-list">
        <n-spin :show="loading">
          <n-empty
            v-if="!selectedDatasourceId"
            description="请先选择数据源"
            style="padding: 60px 0"
          />
          <n-empty
            v-else-if="!loading && filteredTableList.length === 0"
            description="暂无数据"
            style="padding: 60px 0"
          />
          <n-data-table
            v-else
            :columns="columns"
            :data="filteredTableList"
            :row-key="row => row.tableName"
            :checked-row-keys="checkedKeys"
            max-height="450px"
            @update:checked-row-keys="handleCheckedChange"
          />
        </n-spin>
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">
          取消
        </n-button>
        <n-button
          type="primary"
          :loading="submitLoading"
          :disabled="checkedKeys.length === 0"
          @click="handleSubmit"
        >
          导入 ({{ checkedKeys.length }})
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'success'])

const visible = ref(false)
const loading = ref(false)
const datasourceLoading = ref(false)
const submitLoading = ref(false)
const selectedDatasourceId = ref(null)
const datasourceOptions = ref([])
const tableList = ref([])
const checkedKeys = ref([])
const searchKeyword = ref('')

// 过滤后的表列表
const filteredTableList = computed(() => {
  if (!searchKeyword.value) {
    return tableList.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return tableList.value.filter((item) => {
    return (
      item.tableName?.toLowerCase().includes(keyword)
      || item.tableComment?.toLowerCase().includes(keyword)
    )
  })
})

// 表格列配置
const columns = [
  {
    type: 'selection',
  },
  {
    title: '表名',
    key: 'tableName',
    width: 220,
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: '表描述',
    key: 'tableComment',
    ellipsis: {
      tooltip: true,
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
  },
]

// 监听 show 变化
watch(() => props.show, (val) => {
  visible.value = val
  if (val) {
    loadDatasources()
    checkedKeys.value = []
    tableList.value = []
    selectedDatasourceId.value = null
    searchKeyword.value = ''
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

// 加载数据源列表
async function loadDatasources() {
  try {
    datasourceLoading.value = true
    const res = await request.get('/generator/datasource/enabled')
    if (res.code === 200) {
      datasourceOptions.value = (res.data || []).map(item => ({
        label: `${item.datasourceName} (${item.dbType})`,
        value: item.datasourceId,
      }))
      // 如果有默认数据源，自动选中
      const defaultDs = res.data?.find(item => item.isDefault === 1)
      if (defaultDs) {
        selectedDatasourceId.value = defaultDs.datasourceId
        handleDatasourceChange(defaultDs.datasourceId)
      }
    }
  }
  catch (error) {
    console.error('加载数据源列表失败:', error)
    window.$message.error('加载数据源列表失败')
  }
  finally {
    datasourceLoading.value = false
  }
}

// 数据源变化时加载表列表
async function handleDatasourceChange(datasourceId) {
  if (!datasourceId) {
    tableList.value = []
    return
  }

  try {
    loading.value = true
    checkedKeys.value = []
    const res = await request.get(`/generator/datasource/${datasourceId}/tables`)
    if (res.code === 200) {
      tableList.value = res.data || []
      if (tableList.value.length === 0) {
        window.$message.warning('该数据源下没有可导入的表')
      }
    }
  }
  catch (error) {
    console.error('加载表列表失败:', error)
    window.$message.error('加载表列表失败，请检查数据源配置')
  }
  finally {
    loading.value = false
  }
}

// 搜索处理
function handleSearch() {
  // 由于使用了 computed，这里不需要额外处理
}

// 清空选择
function handleClearSelection() {
  checkedKeys.value = []
}

// 选中状态变化
function handleCheckedChange(keys) {
  checkedKeys.value = keys
}

// 关闭弹窗
function handleClose() {
  visible.value = false
}

// 提交导入
async function handleSubmit() {
  if (checkedKeys.value.length === 0) {
    window.$message.warning('请选择要导入的表')
    return
  }

  if (!selectedDatasourceId.value) {
    window.$message.warning('请选择数据源')
    return
  }

  try {
    submitLoading.value = true
    const res = await request.post(
      `/generator/importTable/${selectedDatasourceId.value}`,
      checkedKeys.value,
    )
    if (res.code === 200) {
      window.$message.success(`成功导入 ${checkedKeys.value.length} 张表`)
      emit('success')
      handleClose()
    }
    else {
      window.$message.error(res.msg || '导入失败')
    }
  }
  catch (error) {
    console.error('导入失败:', error)
    window.$message.error(error.message || '导入失败')
  }
  finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.import-table-modal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--n-border-color);
}

.table-list {
  min-height: 350px;
}
</style>

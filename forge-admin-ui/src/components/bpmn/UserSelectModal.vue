<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="title"
    style="width: 800px; max-width: 90vw"
    :mask-closable="false"
    @after-leave="handleClose"
  >
    <div class="user-select-container">
      <!-- 搜索条件 -->
      <div class="search-bar">
        <n-space>
          <n-input
            v-model:value="searchForm.keyword"
            placeholder="用户名/姓名"
            clearable
            @keyup.enter="handleSearch"
            style="width: 200px"
          />
          <n-tree-select
            v-model:value="searchForm.deptId"
            :options="deptTreeOptions"
            placeholder="选择部门"
            clearable
            style="width: 200px"
          />
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <i class="i-material-symbols:search" />
            </template>
            搜索
          </n-button>
          <n-button @click="handleReset">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            重置
          </n-button>
        </n-space>
      </div>

      <!-- 用户列表 -->
      <n-data-table
        :columns="columns"
        :data="userList"
        :loading="loading"
        :row-key="row => row.id"
        :checked-row-keys="checkedKeys"
        :single-line="false"
        :max-height="400"
        @update:checked-row-keys="handleCheck"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          @update:page="loadUserList"
          @update:page-size="loadUserList"
        />
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleClose">取消</n-button>
        <n-button type="primary" @click="handleConfirm" :disabled="checkedKeys.length === 0">
          确定
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { ref, reactive, watch, h, computed } from 'vue'
import { NTag } from 'naive-ui'
import { request } from '@/utils/http'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '选择用户'
  },
  multiple: {
    type: Boolean,
    default: false
  },
  selectedUsers: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:show', 'confirm'])

// 状态
const visible = ref(false)
const loading = ref(false)
const userList = ref([])
const checkedKeys = ref([])
const deptTreeOptions = ref([])

// 搜索条件
const searchForm = reactive({
  keyword: '',
  deptId: null
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 表格列 - 使用 computed 确保响应式
const columns = computed(() => [
  {
    type: 'selection',
    multiple: props.multiple
  },
  {
    title: '用户名',
    key: 'username',
    width: 120
  },
  {
    title: '姓名',
    key: 'name',
    width: 120
  },
  {
    title: '部门',
    key: 'deptName',
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.userStatus === 1 ? 'success' : 'default',
        size: 'small'
      }, {
        default: () => row.userStatus === 1 ? '正常' : '禁用'
      })
    }
  }
])

// 监听显示状态
watch(() => props.show, (val) => {
  visible.value = val
  if (val) {
    loadDeptTree()
    loadUserList()
    // 初始化已选中的用户
    checkedKeys.value = props.selectedUsers.map(u => u.id)
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

// 加载部门树
async function loadDeptTree() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200 && res.data) {
      deptTreeOptions.value = formatDeptTree(res.data)
    }
  } catch (e) {
    console.error('加载部门树失败', e)
  }
}

// 格式化部门树
function formatDeptTree(depts) {
  return depts.map(dept => ({
    label: dept.name || dept.orgName,
    key: dept.id,
    value: dept.id,
    children: dept.children ? formatDeptTree(dept.children) : undefined
  }))
}

// 加载用户列表
async function loadUserList() {
  loading.value = true
  try {
    const res = await request.get('/system/user/page', {
      params: {
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        userName: searchForm.keyword || undefined,
        deptId: searchForm.deptId || undefined
      }
    })
    if (res.code === 200 && res.data) {
      userList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('加载用户列表失败', e)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadUserList()
}

// 重置
function handleReset() {
  searchForm.keyword = ''
  searchForm.deptId = null
  pagination.page = 1
  loadUserList()
}

// 选择处理
function handleCheck(keys) {
  if (props.multiple) {
    checkedKeys.value = keys
  } else {
    // 单选模式
    checkedKeys.value = keys.length > 0 ? [keys[keys.length - 1]] : []
  }
}

// 确认
function handleConfirm() {
  const selectedUsers = userList.value.filter(u => checkedKeys.value.includes(u.id))
  emit('confirm', props.multiple ? selectedUsers : selectedUsers[0])
  handleClose()
}

// 关闭
function handleClose() {
  visible.value = false
  checkedKeys.value = []
  searchForm.keyword = ''
  searchForm.deptId = null
}
</script>

<style scoped>
.user-select-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-bar {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
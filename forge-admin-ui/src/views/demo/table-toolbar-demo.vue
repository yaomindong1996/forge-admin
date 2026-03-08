<!--
  AiTable 工具栏功能演示
  展示表格工具栏的各种功能
-->

<template>
  <div class="table-toolbar-demo">
    <n-card title="AiTable 工具栏功能演示">
      <n-space vertical :size="24">
        <!-- 功能说明 -->
        <n-alert type="info" title="功能说明">
          <ul>
            <li>✅ 刷新：点击刷新按钮重新加载数据</li>
            <li>✅ 密度调整：紧凑/默认/宽松三种显示密度</li>
            <li>✅ 列设置：通过复选框控制列的显示/隐藏</li>
            <li>✅ 自定义工具栏：支持左右两侧自定义插槽</li>
            <li>✅ 工具栏开关：可通过配置完全隐藏工具栏</li>
          </ul>
        </n-alert>

        <!-- 示例1：完整工具栏 -->
        <n-card title="示例1：完整工具栏功能" embedded>
          <AiTable
            ref="tableRef"
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            :show-toolbar="true"
            :show-refresh="true"
            :show-density="true"
            :show-column-filter="true"
            v-model:checked-row-keys="selectedKeys"
            @refresh="handleRefresh"
            @density-change="handleDensityChange"
            @filter-change="handleFilterChange"
          >
            <!-- 工具栏左侧 -->
            <template #toolbar-left>
              <n-button type="primary" @click="handleAdd">
                <template #icon>
                  <n-icon><AddOutline /></n-icon>
                </template>
                新增
              </n-button>
              <n-button 
                type="error" 
                :disabled="selectedKeys.length === 0"
                @click="handleBatchDelete"
              >
                批量删除 ({{ selectedKeys.length }})
              </n-button>
            </template>
            
            <!-- 工具栏右侧额外操作 -->
            <template #toolbar-extra>
              <n-button @click="handleExport">
                <template #icon>
                  <n-icon><DownloadOutline /></n-icon>
                </template>
                导出
              </n-button>
            </template>
          </AiTable>
        </n-card>

        <!-- 示例2：简化工具栏 -->
        <n-card title="示例2：简化工具栏（只显示刷新和列设置）" embedded>
          <AiTable
            :columns="columns"
            :data-source="dataSource"
            :show-toolbar="true"
            :show-refresh="true"
            :show-density="false"
            :show-column-filter="true"
            :hide-selection="true"
            @refresh="handleRefresh2"
          />
        </n-card>

        <!-- 示例3：无工具栏 -->
        <n-card title="示例3：无工具栏（传统模式）" embedded>
          <n-space style="margin-bottom: 16px">
            <n-button @click="handleRefresh3">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              刷新
            </n-button>
          </n-space>
          
          <AiTable
            :columns="columns"
            :data-source="dataSource"
            :show-toolbar="false"
          />
        </n-card>
      </n-space>
    </n-card>

    <!-- 操作日志 -->
    <n-card title="操作日志" style="margin-top: 16px">
      <n-scrollbar style="max-height: 200px">
        <n-space vertical>
          <n-text v-for="(log, index) in logs" :key="index" depth="3">
            {{ log }}
          </n-text>
          <n-text v-if="logs.length === 0" depth="3">暂无操作日志</n-text>
        </n-space>
      </n-scrollbar>
    </n-card>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { AiTable } from '@/components/ai-form'
import { NButton, NSpace, NTag } from 'naive-ui'
import { AddOutline, DownloadOutline, RefreshOutline } from '@vicons/ionicons5'

// 操作日志
const logs = ref([])

function addLog(message) {
  const time = new Date().toLocaleTimeString()
  logs.value.unshift(`[${time}] ${message}`)
  if (logs.value.length > 20) {
    logs.value.pop()
  }
}

// 表格引用
const tableRef = ref(null)

// 选中的行
const selectedKeys = ref([])

// 加载状态
const loading = ref(false)

// 分页配置
const pagination = ref({
  page: 1,
  pageSize: 5,
  itemCount: 20,
  showSizePicker: true,
  pageSizes: [5, 10, 20]
})

// 列配置
const columns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80
  },
  {
    prop: 'name',
    label: '姓名',
    width: 120
  },
  {
    prop: 'age',
    label: '年龄',
    width: 80,
    align: 'center'
  },
  {
    prop: 'email',
    label: '邮箱',
    width: 200
  },
  {
    prop: 'phone',
    label: '电话',
    width: 150
  },
  {
    prop: 'department',
    label: '部门',
    width: 120
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error'
      }, {
        default: () => row.status === 1 ? '在职' : '离职'
      })
    }
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, null, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            onClick: () => handleEdit(row)
          }, { default: () => '编辑' }),
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row)
          }, { default: () => '删除' })
        ]
      })
    }
  }
]

// 数据源（模拟数据）
const dataSource = ref([
  {
    id: 1,
    name: '张三',
    age: 28,
    email: 'zhangsan@example.com',
    phone: '13800138001',
    department: '技术部',
    status: 1
  },
  {
    id: 2,
    name: '李四',
    age: 32,
    email: 'lisi@example.com',
    phone: '13800138002',
    department: '产品部',
    status: 1
  },
  {
    id: 3,
    name: '王五',
    age: 25,
    email: 'wangwu@example.com',
    phone: '13800138003',
    department: '设计部',
    status: 0
  },
  {
    id: 4,
    name: '赵六',
    age: 30,
    email: 'zhaoliu@example.com',
    phone: '13800138004',
    department: '运营部',
    status: 1
  },
  {
    id: 5,
    name: '孙七',
    age: 27,
    email: 'sunqi@example.com',
    phone: '13800138005',
    department: '市场部',
    status: 1
  }
])

// 刷新
function handleRefresh() {
  addLog('刷新表格数据')
  loading.value = true
  setTimeout(() => {
    loading.value = false
    window.$message?.success('刷新成功')
  }, 500)
}

function handleRefresh2() {
  addLog('刷新表格数据（示例2）')
  window.$message?.success('刷新成功')
}

function handleRefresh3() {
  addLog('刷新表格数据（示例3）')
  window.$message?.success('刷新成功')
}

// 密度变化
function handleDensityChange(size) {
  const sizeMap = {
    small: '紧凑',
    medium: '默认',
    large: '宽松'
  }
  addLog(`密度变化: ${sizeMap[size]}`)
  window.$message?.success(`已切换为${sizeMap[size]}模式`)
}

// 列筛选变化
function handleFilterChange(columns) {
  const columnNames = columns.map(col => col.label || col.title).join('、')
  addLog(`列筛选变化: 显示 ${columns.length} 列`)
  console.log('显示的列:', columnNames)
}

// 新增
function handleAdd() {
  addLog('点击新增按钮')
  window.$message?.info('新增功能')
}

// 批量删除
function handleBatchDelete() {
  const rows = tableRef.value?.getCheckedRows()
  addLog(`批量删除: ${selectedKeys.value.length} 条记录`)
  console.log('选中的行:', rows)
  window.$message?.warning(`即将删除 ${selectedKeys.value.length} 条记录`)
}

// 导出
function handleExport() {
  addLog('导出数据')
  window.$message?.info('导出功能')
}

// 编辑
function handleEdit(row) {
  addLog(`编辑: ${row.name}`)
  window.$message?.info(`编辑 ${row.name}`)
}

// 删除
function handleDelete(row) {
  addLog(`删除: ${row.name}`)
  window.$message?.warning(`确定删除 ${row.name}？`)
}
</script>

<style scoped>
.table-toolbar-demo {
  padding: 24px;
}
</style>


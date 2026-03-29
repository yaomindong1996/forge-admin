<!--
  CRUD 页面组件
  基于 Naive UI 的完整 CRUD 解决方案
  参考 LxBasePage 设计，集成搜索、表格、新增、编辑、删除、导入导出等功能

  @author AI Form Team
  @version 1.0.0
-->

<template>
  <div class="ai-crud-page">
    <!-- 搜索表单区域 -->
    <div v-if="showSearch && searchSchema.length > 0" class="ai-crud-search">
      <AiSearch
        ref="searchRef"
        v-model="searchParams"
        :schema="searchSchema"
        :grid-cols="searchGridCols"
        :label-width="searchLabelWidth"
        :enable-collapse="searchEnableCollapse"
        :max-visible-fields="searchMaxVisibleFields"
        :y-gap="searchYGap"
        :before-reset="beforeRenderReset"
        @search="handleSearch"
        @reset="handleReset"

      >
        <!-- 透传搜索表单插槽 -->
        <template v-for="slotName in searchSlots" #[slotName]="slotProps">
          <slot :name="`search-${slotName}`" v-bind="slotProps" />
        </template>

        <!-- 搜索表单额外操作按钮 -->
        <template #extra-actions="{ formData }">
          <slot name="search-extra-actions" :form-data="formData" />
        </template>
      </AiSearch>
    </div>

    <!-- 主内容区域 -->
    <div class="ai-crud-main">
      <!-- 数据表格区域 -->
      <div class="ai-crud-table">
        <AiTable
          ref="tableRef"
          :columns="tableColumns"
          :data-source="dataSource"
          :loading="tableLoading"
          :pagination="paginationConfig"
          :row-key="rowKeyFn"
          :hide-selection="hideSelection"
          :striped="striped"
          :bordered="bordered"
          :size="tableSize"
          :max-height="computedMaxHeight"
          :scroll-x="computedScrollX"
          v-model:checked-row-keys="selectedKeys"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
          @refresh="handleRefresh"
          v-bind="tableProps"
        >
          <template #toolbar-left>
            <!-- 工具栏区域 -->
            <div v-if="!hideToolbar" class="ai-crud-toolbar">
              <slot name="toolbar">
                <div class="toolbar-left">
                  <slot name="toolbar-start" />
                  <!-- 新增按钮 -->
                  <n-button
                      v-if="!hideAdd"
                      type="primary"
                      @click="handleAdd"
                      size="small"
                  >
                    <template #icon>
                      <n-icon><Add /></n-icon>
                    </template>
                    {{ addButtonText }}
                  </n-button>
                  <!-- 批量导入按钮 -->
                  <n-button
                      size="small"
                      v-if="showImport"
                      @click="handleShowImport"
                  >
                    <template #icon>
                      <n-icon><CloudUploadOutline /></n-icon>
                    </template>
                    批量导入
                  </n-button>

                  <!-- 导出按钮 -->
                  <n-button
                      size="small"
                      v-if="showExport"
                      @click="handleExport"
                      strong
                      secondary
                  >
                    <template #icon>
                      <n-icon><DownloadOutline /></n-icon>
                    </template>
                    {{ exportButtonText }}
                  </n-button>

                  <slot name="toolbar-end" />
                </div>

                <!-- 右侧操作按钮 -->
                <div class="toolbar-right">
                  <slot name="toolbar-right-start" />
                  <slot name="toolbar-right-end" />
                </div>
              </slot>
            </div>
          </template>
          <!-- 透传表格插槽 -->
          <template v-for="slotName in tableSlots" #[slotName]="slotProps">
            <slot :name="`table-${slotName}`" v-bind="slotProps" />
          </template>
        </AiTable>
      </div>
    </div>

    <!-- 新增/编辑弹窗 - Modal 模式 -->
    <n-modal
      v-if="modalType === 'modal'"
      v-model:show="modalVisible"
      :title="modalTitle"
      preset="card"
      :style="{ width: modalWidth }"
      :segmented="{ content: 'soft', footer: 'soft' }"
      :closable="true"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <AiForm
        ref="formRef"
        v-model:value="formData"
        :schema="editSchema"
        :grid-cols="editGridCols"
        :label-width="editLabelWidth"
        :show-actions="false"
        :context="formContext"
      >
        <!-- 透传表单插槽 -->
        <template v-for="slotName in formSlots" #[slotName]="slotProps">
          <slot :name="`form-${slotName}`" v-bind="slotProps" />
        </template>
      </AiForm>

      <!-- 弹窗底部按钮 -->
      <template v-if="!hideModalFooter" #footer>
        <n-space justify="end">
          <n-button @click="handleModalCancel">取消</n-button>
          <n-button
            type="primary"
            :loading="confirmLoading"
            @click="handleModalConfirm"
          >
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 新增/编辑抽屉 - Drawer 模式 -->
    <n-drawer
      v-else
      v-model:show="modalVisible"
      :width="modalWidth"
      :placement="drawerPlacement"
      :mask-closable="false"
      @after-leave="handleModalClose"
    >
      <n-drawer-content :title="modalTitle" :closable="true">
        <AiForm
          ref="formRef"
          v-model:value="formData"
          :schema="editSchema"
          :grid-cols="editGridCols"
          :label-width="editLabelWidth"
          :show-actions="false"
          :context="formContext"
        >
          <!-- 透传表单插槽 -->
          <template v-for="slotName in formSlots" #[slotName]="slotProps">
            <slot :name="`form-${slotName}`" v-bind="slotProps" />
          </template>
        </AiForm>

        <!-- 抽屉底部按钮 -->
        <template v-if="!hideModalFooter" #footer>
          <n-space justify="end">
            <n-button @click="handleModalCancel">取消</n-button>
            <n-button
              type="primary"
              :loading="confirmLoading"
              @click="handleModalConfirm"
            >
              确定
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 导入弹窗 -->
    <n-modal
      v-model:show="importModalVisible"
      title="批量导入"
      preset="card"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-upload
        :action="importApi"
        :headers="importHeaders"
        :data="importData"
        :max="1"
        accept=".xlsx,.xls"
        @finish="handleImportFinish"
        @error="handleImportError"
      >
        <n-upload-dragger>
          <div style="margin-bottom: 12px">
            <n-icon size="48" :depth="3">
              <CloudUploadOutline />
            </n-icon>
          </div>
          <n-text style="font-size: var(--font-size-lg)">
            点击或者拖动文件到该区域来上传
          </n-text>
          <n-p depth="3" style="margin: 8px 0 0 0">
            请上传 .xlsx 或 .xls 格式的文件
          </n-p>
        </n-upload-dragger>
      </n-upload>

      <template v-if="importTemplateUrl" #footer>
        <n-space justify="space-between">
          <n-button text @click="handleDownloadTemplate">
            <template #icon>
              <n-icon><DownloadOutline /></n-icon>
            </template>
            下载导入模板
          </n-button>
          <n-button @click="importModalVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick, h } from 'vue'
import {
  Add,
  TrashOutline,
  CloudUploadOutline,
  DownloadOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import AiSearch from './AiSearch.vue'
import AiTable from './AiTable.vue'
import AiForm from './AiForm.vue'
import { request } from '@/utils'
import { postEncrypt } from '@/utils/encrypt-request'
import { aiCrudPageProps } from './AiCrudPageProps'

/**
 * ==================== Props 定义 ====================
 */
const props = defineProps(aiCrudPageProps)

/**
 * ==================== Emits 定义 ====================
 */
const emit = defineEmits([
  'load-list-success',    // 列表加载成功
  'load-list-error',      // 列表加载失败
  'add',                  // 点击新增
  'edit',                 // 点击编辑
  'delete',               // 删除成功
  'submit-success',       // 提交成功
  'submit-error',         // 提交失败
  'selection-change',     // 选中项变化
  'modal-open',           // 弹窗打开
  'modal-close'           // 弹窗关闭
])

/**
 * ==================== Refs ====================
 */
const message = window.$message
const dialog = window.$dialog

const searchRef = ref(null)
const tableRef = ref(null)
const formRef = ref(null)

/**
 * ==================== 响应式数据 ====================
 */
// 搜索参数
const searchParams = ref({})

// 表格数据
const dataSource = ref([])
const tableLoading = ref(false)
const selectedKeys = ref([])

// 分页
const pagination = ref({
  page: props.pageNum,
  pageSize: props.pageSize,
  itemCount: 0
})

// 弹窗
const modalVisible = ref(false)
const modalTitle = ref('')
const modalStatus = ref('') // 'add' | 'edit'
const formData = ref({})
const confirmLoading = ref(false)
const currentRow = ref(null)

// 导入
const importModalVisible = ref(false)

/**
 * ==================== 计算属性 ====================
 */

/**
 * 行键函数
 */
const rowKeyFn = computed(() => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey
  }
  return (row) => row[props.rowKey]
})

/**
 * 表格列配置（添加操作列）
 */
const tableColumns = computed(() => {
  const cols = [...props.columns]

  // 如果没有操作列且没有隐藏，添加默认操作列
  const hasActionColumn = cols.some(col => col.prop === 'action' || col.key === 'action')

  if (!hasActionColumn) {
    cols.push({
      prop: 'action',
      label: '操作',
      width: 200,
      fixed: 'right',
      render: (row) => {
        return h('div', {
          class: 'table-action-column'
        }, [
          h(
            'n-button',
            {
              size: 'small',
              type: 'primary',
              onClick: () => handleEdit(row)
            },
            { default: () => '编辑' }
          ),
          h(
            'n-button',
            {
              size: 'small',
              type: 'error',
              style: { marginLeft: '8px' },
              onClick: () => handleDelete(row)
            },
            { default: () => '删除' }
          )
        ])
      }
    })
  }

  return cols
})

/**
 * 分页配置
 */
const paginationConfig = computed(() => {
  if (!props.showPagination) {
    return false
  }

  return {
    page: pagination.value.page,
    pageSize: pagination.value.pageSize,
    itemCount: pagination.value.itemCount,
    pageCount: Math.ceil(pagination.value.itemCount / pagination.value.pageSize),
    showSizePicker: true,
    pageSizes: props.pageSizes,
    showQuickJumper: true,
    prefix:({itemCount}) => `共${itemCount}条`
  }
})

/**
 * 搜索表单插槽名称
 */
const searchSlots = computed(() => {
  return props.searchSchema
    .filter(field => field.type === 'slot')
    .map(field => field.slotName || field.field)
})

/**
 * 表格插槽名称
 */
const tableSlots = computed(() => {
  return props.columns
    .filter(col => col.slot || col._slot)
    .map(col => col.slot || col._slot)
})

/**
 * 表单插槽名称
 */
const formSlots = computed(() => {
  return props.editSchema
    .filter(field => field.type === 'slot')
    .map(field => field.slotName || field.field)
})

/**
 * 表单上下文（传递 modalStatus 等信息）
 */
const formContext = computed(() => {
  return {
    modalStatus: modalStatus.value,  // 'add' | 'edit'
    isEdit: modalStatus.value === 'edit',
    isAdd: modalStatus.value === 'add',
    currentRow: currentRow.value
  }
})

/**
 * 计算横向滚动宽度
 * 如果没有设置 scrollX，自动计算所有列的宽度总和
 */
const computedScrollX = computed(() => {
  if (props.scrollX !== undefined) {
    return props.scrollX
  }

  // 自动计算所有列的宽度
  let totalWidth = 0
  let hasWidth = false

  tableColumns.value.forEach(col => {
    if (col.width) {
      totalWidth += col.width
      hasWidth = true
    }
  })

  // 如果所有列都设置了宽度，返回总宽度，否则返回 undefined
  return hasWidth ? totalWidth : undefined
})

/**
 * 计算最大高度
 * 如果没有设置 maxHeight，使用默认值
 */
const computedMaxHeight = computed(() => {
  if (props.maxHeight !== undefined) {
    return props.maxHeight
  }

  // 默认使用 calc 计算高度，减去搜索区域和工具栏的高度
  return 'calc(100vh - 280px)'
})

/**
 * ==================== 方法 ====================
 */

/**
 * 执行钩子函数
 * @param {String} hookName - 钩子函数名
 * @param {*} params - 参数
 * @param {Function} success - 成功回调
 * @returns {Promise}
 */
async function callHook(hookName, params, success) {
  if (props[hookName] && typeof props[hookName] === 'function') {
    const result = props[hookName](params)

    // 如果是 Promise
    if (result instanceof Promise) {
      try {
        const data = await result
        return success ? success(data) : data
      } catch (error) {
        console.error(`Hook ${hookName} error:`, error)
        return success ? success(params) : params
      }
    } else {
      return success ? success(result) : result
    }
  } else {
    return success ? success(params) : params
  }
}

/**
 * 解析 API 配置
 * @param {String} key - API 配置键名
 * @param {String} defaultApi - 默认 API
 * @param {String} defaultMethod - 默认请求方法
 * @returns {Object} { method, url }
 */
function parseApiConfig(key, defaultApi, defaultMethod = 'get') {
  const apiConfigValue = props.apiConfig[key]

  if (apiConfigValue) {
    const [method, url] = apiConfigValue.split('@')
    // 支持 postEncrypt 方法
    const finalMethod = method === 'postEncrypt' ? method : method.toLowerCase()
    return { method: finalMethod, url }
  }

  return { method: defaultMethod, url: defaultApi }
}

/**
 * 加载列表数据
 */
async function loadList() {
  if (!props.api && !props.apiConfig.list) {
    console.warn('未配置 API 地址')
    return
  }

  tableLoading.value = true

  try {
    // 构建请求参数
    let params = {
      ...props.publicParams,
      ...searchParams.value
    }

    // 分页参数
    if (props.showPagination) {
      if (props.listMethod === 'get') {
        params = {
          ...params,
          ...props.publicQuery,
          pageNum: pagination.value.page,
          pageSize: pagination.value.pageSize
        }
      } else {
        params.pageNum = pagination.value.page
        params.pageSize = pagination.value.pageSize
      }
    }

    // 调用 beforeLoadList 钩子
    params = await callHook('beforeLoadList', params, (data) => data)

    // 解析 API
    const { method, url } = parseApiConfig('list', props.api, props.listMethod)

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    } else {
      requestMethod = method.toLowerCase()
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      response = await postEncrypt(url, params)
    } else {
      // 使用普通请求
      const requestConfig = {
        method: requestMethod,
        url
      }

      if (requestMethod === 'get') {
        requestConfig.params = params
      } else {
        requestConfig.data = params
        requestConfig.params = props.publicQuery
      }

      response = await request(requestConfig)
    }

    // 提取数据
    let list = []
    let total = 0

    if (Array.isArray(response.data)) {
      // 后端直接返回数组（不分页）
      list = response.data
      total = response.data.length
    } else if (response.data && typeof response.data === 'object') {

      // 后端返回对象（分页数据）
      list = response.data[props.listDataField] || []
      total = response.data[props.listTotalField] || 0

    }

    // 调用 beforeRenderList 钩子
    list = await callHook('beforeRenderList', list, (data) => data)

    // 更新数据
    dataSource.value = list
    pagination.value.itemCount = total

    emit('load-list-success', { list, total })
  } catch (error) {
    console.error('加载列表失败:', error)
    window.$message.error('加载数据失败')
    emit('load-list-error', error)
  } finally {
    tableLoading.value = false
  }
}

/**
 * 搜索
 */
async function handleSearch(params) {
  // 调用 beforeSearch 钩子
  const processedParams = await callHook('beforeSearch', params, (data) => data)

  // 如果钩子返回 false，中断搜索
  if (processedParams === false) {
    return
  }

  searchParams.value = { ...processedParams }
  pagination.value.page = 1
  loadList()
}

/**
 * 重置
 */
function handleReset() {
  searchParams.value = {}
  pagination.value.page = 1
  loadList()
}

/**
 * 刷新列表
 */
function handleRefresh() {
  loadList()
}

/**
 * 翻页
 */
function handlePageChange(page) {
  pagination.value.page = page
  loadList()
}

/**
 * 改变每页条数
 */
function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadList()
}

/**
 * 选中项变化
 */
watch(selectedKeys, (newKeys) => {
  const rows = tableRef.value?.getCheckedRows() || []
  emit('selection-change', { keys: newKeys, rows })
})

/**
 * 新增
 */
async function handleAdd() {
  modalTitle.value = props.addButtonText
  modalStatus.value = 'add'
  currentRow.value = null

  // 先打开弹窗
  modalVisible.value = true

  // 使用 nextTick 确保弹窗已渲染
  await nextTick()

  // 初始化表单数据，设置默认值
  const initialData = {}
  props.editSchema.forEach(field => {
    if (field.field && field.defaultValue !== undefined) {
      initialData[field.field] = field.defaultValue
    }
  })

  // 调用 beforeRenderForm 钩子（新增时）
  const formDataFromHook = await callHook('beforeRenderForm', null, (data) => data)

  console.log('AiCrudPage handleAdd - initialData:', initialData)
  console.log('AiCrudPage handleAdd - formDataFromHook:', formDataFromHook)

  // 合并默认值和钩子返回的数据
  if (formDataFromHook && typeof formDataFromHook === 'object') {
    formData.value = { ...initialData, ...formDataFromHook }
  } else {
    formData.value = initialData
  }

  console.log('AiCrudPage handleAdd - 最终 formData:', formData.value)

  emit('add')
  emit('modal-open', { status: 'add', row: null })
}

/**
 * 编辑
 */
async function handleEdit(row) {
  modalTitle.value = '编辑'
  modalStatus.value = 'edit'
  currentRow.value = row

  // 调用 beforeRenderForm 钩子（编辑时）
  const processedRow = await callHook('beforeRenderForm', row, (data) => data)

  // 如果需要加载详情
  if (props.loadDetailOnEdit) {
    window.$loading.show("加载中...")
    await loadDetail(processedRow || row)
    window.$loading.close()
  } else {
    // 调用 beforeRenderDetail 钩子
    const data = await callHook('beforeRenderDetail', processedRow || row, (data) => data)
    formData.value = { ...data }
  }

  modalVisible.value = true

  emit('edit', row)
  emit('modal-open', { status: 'edit', row })
}

/**
 * 加载详情
 */
async function loadDetail(row) {
  confirmLoading.value = true

  try {
    const { method, url } = parseApiConfig(
      'detail',
      `${props.api}/${row[props.rowKey]}`,
      'get'
    )

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    } else {
      requestMethod = method.toLowerCase()
    }

    // 构建请求参数
    const requestConfig = {
      method: requestMethod,
      url
    }

    // 对于 POST 请求，将主键作为 query 参数传递
    if (requestMethod === 'post' || requestMethod === 'postEncrypt') {
      // 获取主键字段名（可能是 id, dictId, dictCode 等）
      const idKey = props.rowKey
      requestConfig.params = { [idKey]: row[idKey] }
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      response = await postEncrypt(url, {}, { params: requestConfig.params })
    } else {
      // 使用普通请求
      response = await request(requestConfig)
    }

    // 调用 beforeRenderDetail 钩子
    const data = await callHook('beforeRenderDetail', response.data, (data) => data)

    formData.value = { ...data }
  } catch (error) {
    console.error('加载详情失败:', error)
    window.$message.error('加载详情失败')
  } finally {
    confirmLoading.value = false
  }
}

/**
 * 删除
 */
async function handleDelete(row) {
  const rows = [row]
  const keys = [rowKeyFn.value(row)]

  await performDelete(rows, keys)
}

/**
 * 批量删除
 */
async function handleBatchDelete() {
  const rows = tableRef.value?.getCheckedRows() || []
  const keys = selectedKeys.value

  if (rows.length === 0) {
    window.$message.warning('请先选择要删除的数据')
    return
  }

  await performDelete(rows, keys)
}

/**
 * 执行删除
 */
async function performDelete(rows, keys) {
  // 调用 beforeDelete 钩子
  const shouldContinue = await callHook('beforeDelete', rows, (result) => result)

  if (shouldContinue === false) {
    return
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${keys.length} 条数据吗？此操作不可恢复！`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const { method, url } = parseApiConfig('delete', props.api, 'delete')

        // 确定使用哪种请求方法
        let requestMethod = method
        // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
        const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
        if (useEncrypt) {
          requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
        } else {
          requestMethod = method.toLowerCase()
        }

        // 发送请求
        if (useEncrypt && requestMethod === 'postEncrypt') {
          // 使用加密请求
          await postEncrypt(url, keys)
        } else {
          // 使用普通请求
          await request({
            method: requestMethod,
            url,
            data: keys
          })
        }

        window.$message.success('删除成功')
        selectedKeys.value = []
        loadList()
      } catch (error) {
        console.error('删除失败:', error)
        window.$message.error('删除失败')
      }
    }
  })
}

/**
 * 提交表单
 */
async function handleModalConfirm() {
  try {
    await formRef.value?.validate()

    // 调用 beforeSubmit 钩子
    let data = await callHook('beforeSubmit', formData.value, (data) => data)

    if (data === false) {
      return
    }

    confirmLoading.value = true

    const isEdit = modalStatus.value === 'edit'

    // 新增时优先使用 apiConfig.add,其次使用 apiConfig.create
    let createKey = 'add'
    if (!isEdit) {
      if (props.apiConfig.create) {
        createKey = 'create'
      } else if (props.apiConfig.add) {
        createKey = 'add'
      }
    }

    const { method, url } = parseApiConfig(
      isEdit ? 'update' : createKey,
      isEdit ? `${props.api}/${currentRow.value[props.rowKey]}` : props.api,
      isEdit ? 'put' : 'post'
    )

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    } else {
      requestMethod = method.toLowerCase()
    }

    // 发送请求
    let response
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      response = await postEncrypt(url, data)
    } else {
      // 使用普通请求
      response = await request({ method: requestMethod, url, data })
    }

    window.$message.success(`${isEdit ? '编辑' : '新增'}成功`)
    modalVisible.value = false

    // 触发提交成功事件
    emit('submit-success', { data, response, isEdit })

    loadList()
  } catch (error) {
    console.error('提交失败:', error)
    window.$message.error('提交失败')

    // 触发提交失败事件
    emit('submit-error', { error, data: formData.value })
  } finally {
    confirmLoading.value = false
  }
}

/**
 * 弹窗取消
 */
function handleModalCancel() {
  modalVisible.value = false
}

/**
 * 弹窗关闭后
 */
function handleModalClose() {
  formData.value = {}
  currentRow.value = null
  formRef.value?.restoreValidation()

  emit('modal-close')
}

/**
 * 显示导入弹窗
 */
function handleShowImport() {
  importModalVisible.value = true
}

/**
 * 导入完成
 */
function handleImportFinish({ event }) {
  const response = JSON.parse(event.target.response)

  if (response.code === 200) {
    window.$message.success('导入成功')
    importModalVisible.value = false
    loadList()
  } else {
    window.$message.error(response.msg || '导入失败')
  }
}

/**
 * 导入失败
 */
function handleImportError() {
  window.$message.error('导入失败')
}

/**
 * 下载导入模板
 */
function handleDownloadTemplate() {
  window.open(props.importTemplateUrl, '_blank')
}

/**
 * 导出
 */
async function handleExport() {
  try {
    const { method, url } = parseApiConfig('export', props.exportApi || props.api, 'post')

    const params = {
      ...searchParams.value,
      ...props.publicParams
    }

    // 确定使用哪种请求方法
    let requestMethod = method
    // 如果方法明确指定为 postEncrypt，则使用加密请求，不管 isEncrypt 属性
    const useEncrypt = method === 'postEncrypt' || (props.isEncrypt && method !== 'get')
    if (useEncrypt) {
      requestMethod = method === 'postEncrypt' ? 'postEncrypt' : method.toLowerCase()
    } else {
      requestMethod = method.toLowerCase()
    }

    // 发送请求
    if (useEncrypt && requestMethod === 'postEncrypt') {
      // 使用加密请求
      await postEncrypt(url, params)
    } else {
      // 使用普通请求
      await request({
        method: requestMethod,
        url,
        data: requestMethod === 'get' ? undefined : params,
        params: requestMethod === 'get' ? params : undefined
      })
    }

    window.$message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    window.$message.error('导出失败')
  }
}

/**
 * ==================== 暴露方法 ====================
 */
defineExpose({
  /**
   * 刷新列表
   */
  refresh: loadList,

  /**
   * 加载列表
   */
  loadList,

  /**
   * 获取选中的行
   */
  getSelectedRows: () => tableRef.value?.getCheckedRows() || [],

  /**
   * 获取选中的键
   */
  getSelectedKeys: () => selectedKeys.value,

  /**
   * 清除选中
   */
  clearSelection: () => {
    selectedKeys.value = []
    tableRef.value?.clearSelection()
  },

  /**
   * 设置选中的键
   */
  setSelectedKeys: (keys) => {
    selectedKeys.value = keys
    tableRef.value?.setCheckedKeys(keys)
  },

  /**
   * 获取表格数据
   */
  getTableData: () => dataSource.value,

  /**
   * 设置表格数据
   */
  setTableData: (data) => {
    dataSource.value = data
  },

  /**
   * 打开新增弹窗
   */
  showAdd: handleAdd,

  /**
   * 打开编辑弹窗
   */
  showEdit: handleEdit,

  /**
   * 关闭弹窗
   */
  closeModal: () => {
    modalVisible.value = false
  },

  /**
   * 获取搜索参数
   */
  getSearchParams: () => searchParams.value,

  /**
   * 设置搜索参数
   */
  setSearchParams: (params) => {
    searchParams.value = params
  },

  /**
   * 重置搜索
   */
  resetSearch: () => {
    searchRef.value?.handleReset()
  }
})

/**
 * ==================== 生命周期 ====================
 */
onMounted(() => {
  if (!props.lazy) {
    loadList()
  }
})

// 监听公共参数变化
watch(() => props.publicParams, () => {
  pagination.value.page = 1
  loadList()
}, { deep: true })

watch(() => props.publicQuery, () => {
  pagination.value.page = 1
  loadList()
}, { deep: true })
</script>

<style scoped>
.ai-crud-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  background: var(--bg-primary);
}

/* 搜索区域 */
.ai-crud-search {
  background: var(--bg-primary);
  flex-shrink: 0;
}

/* 主内容区域 */
.ai-crud-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  overflow: auto;
  min-height: 0;
}

/* 工具栏区域 */
.ai-crud-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  flex-shrink: 0;
}

/* 表格区域 */
.ai-crud-table {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ai-crud-table :deep(.ai-table-wrapper) {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ai-crud-table :deep(.n-data-table-wrapper) {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.ai-crud-table :deep(.n-data-table) {
  flex: 0 0 auto;
}

/* 表格工具栏 - 不需要额外内边距因为 AiTable 已处理 */
.ai-crud-table :deep(.ai-table-toolbar) {
  padding: 10px 16px;
}

/* 操作列样式 */
.table-action-column {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 按钮操作链接样式 */
:deep(.table-action-link) {
  display: inline-flex;
  align-items: center;
  font-size: var(--font-size-sm);
  color: var(--primary-600);
  cursor: pointer;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  text-decoration: none;
  line-height: 1.5;
}

:deep(.table-action-link:hover) {
  background: var(--primary-50);
  color: var(--primary-700);
}

:deep(.table-action-link.danger) {
  color: var(--error-600);
}

:deep(.table-action-link.danger:hover) {
  background: var(--error-50);
  color: var(--error-700);
}

:deep(.table-action-divider) {
  color: var(--border-strong);
  user-select: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: flex-start;
  }
}

/* 表格内容优化 */
:deep(.n-data-table) {
  border-radius: 0;
}

:deep(.n-data-table .n-data-table-th) {
  background: var(--bg-secondary) !important;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

:deep(.n-data-table .n-data-table-td) {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

/* 分页器样式 */
:deep(.n-pagination) {
  padding: 12px 16px;
  justify-content: flex-end;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  :deep(.n-pagination) {
    justify-content: center;
    padding: 12px;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 32px;
    height: 32px;
    font-size: 13px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 6px;
  }

  :deep(.n-pagination-size-picker) {
    font-size: 13px;
  }

  :deep(.n-pagination-quick-jumper) {
    font-size: 13px;
  }
}

@media (max-width: 576px) {
  :deep(.n-pagination) {
    padding: 10px 4px;
    gap: 4px;
  }

  :deep(.n-pagination .n-pagination-item) {
    min-width: 28px;
    height: 28px;
    font-size: 12px;
  }

  :deep(.n-pagination .n-pagination-item__button) {
    padding: 0 4px;
  }
}
</style>

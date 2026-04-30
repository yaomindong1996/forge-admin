<template>
  <n-modal
    v-model:show="visible"
    :title="`字段配置 - ${tableName}`"
    preset="card"
    style="width: 1300px"
    :mask-closable="false"
  >
    <div class="column-config-modal">
      <n-spin :show="loading">
        <div style="margin-bottom: 12px; display: flex; justify-content: flex-end;">
          <n-button
            type="primary"
            :loading="aiLoading"
            @click="handleAiRecommend"
          >
            AI 推荐
          </n-button>
        </div>
        <n-data-table
          :columns="columns"
          :data="columnList"
          :row-key="row => row.columnId"
          max-height="500px"
          :scroll-x="1800"
          :row-class-name="row => row.aiRecommended ? 'ai-recommended-row' : ''"
        />
      </n-spin>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleReset">
          重置配置
        </n-button>
        <n-button @click="handleClose">
          取消
        </n-button>
        <n-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          保存
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NCheckbox, NInput, NSelect } from 'naive-ui'
import { h, ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  tableId: {
    type: [Number, String],
    default: null,
  },
  tableName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:show', 'success'])

const visible = ref(false)
const loading = ref(false)
const submitLoading = ref(false)
const aiLoading = ref(false)
const columnList = ref([])
const originalColumnMap = ref({})

// Java类型选项
const javaTypeOptions = [
  { label: 'String', value: 'String' },
  { label: 'Long', value: 'Long' },
  { label: 'Integer', value: 'Integer' },
  { label: 'BigDecimal', value: 'BigDecimal' },
  { label: 'LocalDateTime', value: 'LocalDateTime' },
  { label: 'LocalDate', value: 'LocalDate' },
  { label: 'Boolean', value: 'Boolean' },
  { label: 'Double', value: 'Double' },
]

// 查询方式选项
const queryTypeOptions = [
  { label: '等于', value: 'EQ' },
  { label: '模糊', value: 'LIKE' },
  { label: '区间', value: 'BETWEEN' },
  { label: '大于', value: 'GT' },
  { label: '小于', value: 'LT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于等于', value: 'LE' },
  { label: '不等于', value: 'NE' },
]

// 显示类型选项
const htmlTypeOptions = [
  { label: '文本框', value: 'INPUT' },
  { label: '文本域', value: 'TEXTAREA' },
  { label: '下拉框', value: 'SELECT' },
  { label: '单选框', value: 'RADIO' },
  { label: '复选框', value: 'CHECKBOX' },
  { label: '日期控件', value: 'DATETIME' },
  { label: '图片上传', value: 'IMAGE' },
  { label: '文件上传', value: 'FILE' },
  { label: '富文本', value: 'EDITOR' },
]

// 表格列配置
const columns = [
  {
    title: '字段名',
    key: 'columnName',
    width: 140,
    fixed: 'left',
  },
  {
    title: '字段描述',
    key: 'columnComment',
    width: 140,
    render: row => h(NInput, {
      value: row.columnComment,
      size: 'small',
      onUpdateValue: (val) => { row.columnComment = val },
    }),
  },
  {
    title: '数据库类型',
    key: 'columnType',
    width: 120,
  },
  {
    title: 'Java类型',
    key: 'javaType',
    width: 130,
    render: row => h(NSelect, {
      value: row.javaType,
      options: javaTypeOptions,
      size: 'small',
      onUpdateValue: (val) => { row.javaType = val },
    }),
  },
  {
    title: 'Java字段',
    key: 'javaField',
    width: 130,
    render: row => h(NInput, {
      value: row.javaField,
      size: 'small',
      onUpdateValue: (val) => { row.javaField = val },
    }),
  },
  {
    title: '主键',
    key: 'isPk',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isPk === 1,
      onUpdateChecked: (val) => { row.isPk = val ? 1 : 0 },
    }),
  },
  {
    title: '必填',
    key: 'isRequired',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isRequired === 1,
      onUpdateChecked: (val) => { row.isRequired = val ? 1 : 0 },
    }),
  },
  {
    title: '插入',
    key: 'isInsert',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isInsert === 1,
      onUpdateChecked: (val) => { row.isInsert = val ? 1 : 0 },
    }),
  },
  {
    title: '编辑',
    key: 'isEdit',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isEdit === 1,
      onUpdateChecked: (val) => { row.isEdit = val ? 1 : 0 },
    }),
  },
  {
    title: '列表',
    key: 'isList',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isList === 1,
      onUpdateChecked: (val) => { row.isList = val ? 1 : 0 },
    }),
  },
  {
    title: '查询',
    key: 'isQuery',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: row.isQuery === 1,
      onUpdateChecked: (val) => { row.isQuery = val ? 1 : 0 },
    }),
  },
  {
    title: '查询方式',
    key: 'queryType',
    width: 100,
    render: row => h(NSelect, {
      value: row.queryType,
      options: queryTypeOptions,
      size: 'small',
      onUpdateValue: (val) => { row.queryType = val },
    }),
  },
  {
    title: '显示类型',
    key: 'htmlType',
    width: 110,
    render: row => h(NSelect, {
      value: row.htmlType,
      options: htmlTypeOptions,
      size: 'small',
      onUpdateValue: (val) => { row.htmlType = val },
    }),
  },
  {
    title: '字典类型',
    key: 'dictType',
    width: 120,
    render: row => h(NInput, {
      value: row.dictType,
      size: 'small',
      placeholder: '字典编码',
      onUpdateValue: (val) => { row.dictType = val },
    }),
  },
]

// 监听 show 变化
watch(() => props.show, (val) => {
  visible.value = val
  if (val && props.tableId) {
    loadColumns()
  }
})

watch(visible, (val) => {
  emit('update:show', val)
})

// 加载字段列表
async function loadColumns() {
  try {
    loading.value = true
    const res = await request.get(`/generator/column/list/${props.tableId}`)
    if (res.code === 200) {
      columnList.value = res.data || []
    }
  }
  catch (error) {
    console.error('加载字段列表失败:', error)
    window.$message.error('加载字段列表失败')
  }
  finally {
    loading.value = false
  }
}

// 重置配置
async function handleReset() {
  window.$dialog.warning({
    title: '确认重置',
    content: '确定要重置所有字段配置为默认值吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        loading.value = true
        const res = await request.post(`/generator/column/resetConfig/${props.tableId}`)
        if (res.code === 200) {
          window.$message.success('重置成功')
          loadColumns()
        }
      }
      catch (error) {
        window.$message.error('重置失败')
      }
      finally {
        loading.value = false
      }
    },
  })
}

// 关闭弹窗
function handleClose() {
  visible.value = false
}

// 保存
async function handleSubmit() {
  try {
    submitLoading.value = true
    const res = await request.post('/generator/column/batchUpdate', columnList.value)
    if (res.code === 200) {
      window.$message.success('保存成功')
      emit('success')
      handleClose()
    }
  }
  catch (error) {
    console.error('保存失败:', error)
    window.$message.error('保存失败')
  }
  finally {
    submitLoading.value = false
  }
}

// AI 推荐字段配置
async function handleAiRecommend() {
  if (!columnList.value.length) {
    window.$message.warning('暂无字段可推荐')
    return
  }
  try {
    aiLoading.value = true
    const res = await request.post('/generator/ai/recommendColumns', {
      tableId: props.tableId,
      columns: columnList.value,
    })
    if (res.code === 200 && res.data) {
      columnList.value = res.data
      window.$message.success('AI 推荐完成，请审核后保存')
    }
    else {
      window.$message.warning(res.msg || 'AI 推荐不可用，已使用基础规则推断')
    }
  }
  catch (error) {
    console.error('AI推荐失败:', error)
    window.$message.warning('AI 推荐不可用，已使用基础规则推断')
  }
  finally {
    aiLoading.value = false
  }
}
</script>

<style scoped>
.column-config-modal {
  min-height: 400px;
}
:deep(.ai-recommended-row td) {
  background-color: #e8f5e9 !important;
}
</style>

<template>
  <div class="employee-page">
    <!--
      员工信息表 页面
      生成日期: 2026-04-26
      如需自定义功能，可在 AiCrudPage 上添加插槽，如 #toolbar-end、#form-xxx 等
    -->
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="computedSearchSchema"
      :columns="computedColumns"
      :edit-schema="computedEditSchema"
      row-key="id"
    />
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { getDictData } from '@/composables/useDict'

defineOptions({ name: 'Employee' })

const crudRef = ref(null)

// ── 接口配置（基于生成的 Controller 接口）──────────────────────────────────
const apiConfig = {
  list:   'get@/employee/page',
  detail: 'post@/employee/getById',
  add:    'post@/employee/add',
  update: 'post@/employee/edit',
  delete: 'post@/employee/remove/:id',
}

// ── 字典缓存 ──────────────────────────────────────────────────────────────
const dictCache = ref({})

// ── 搜索条件配置（原始） ──────────────────────────────────────────────────
const searchSchema = [
  {
    field: 'empName',
    label: '姓名',
    type: 'input'
,
  },
  {
    field: 'empNo',
    label: '工号',
    type: 'input'
,
  },
  {
    field: 'deptId',
    label: '部门',
    type: 'select',
    dictType: 'sys_dept'
,
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    dictType: 'sys_normal_disable'
,
  },
]

// ── 表格列配置（原始） ────────────────────────────────────────────────────
const columnsSchema = [
  {
    prop: 'empName',
    label: '姓名'
,
  },
  {
    prop: 'empNo',
    label: '工号'
,
  },
  {
    prop: 'deptId',
    label: '部门'
,
    _transName: 'deptName',
    _dictType: 'sys_dept',
  },
  {
    prop: 'position',
    label: '职位'
,
  },
  {
    prop: 'hireDate',
    label: '入职日期'
,
  },
  {
    prop: 'phone',
    label: '手机号'
,
  },
  {
    prop: 'email',
    label: '邮箱'
,
  },
  {
    prop: 'status',
    label: '状态'
,
    _transName: 'statusName',
    _dictType: 'sys_normal_disable',
  },
]

// ── 新增/编辑表单配置（原始） ─────────────────────────────────────────────
const editSchema = [
  {
    field: 'empName',
    label: '姓名',
    type: 'input',
    required: true
,
  },
  {
    field: 'empNo',
    label: '工号',
    type: 'input',
    required: true
,
  },
  {
    field: 'deptId',
    label: '部门',
    type: 'select',
    required: true,
    dictType: 'sys_dept'
,
  },
  {
    field: 'position',
    label: '职位',
    type: 'input',
    required: true
,
  },
  {
    field: 'hireDate',
    label: '入职日期',
    type: 'date',
    required: true
,
  },
  {
    field: 'phone',
    label: '手机号',
    type: 'input',
    required: true
,
  },
  {
    field: 'email',
    label: '邮箱',
    type: 'input',
    required: true
,
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    required: true,
    dictType: 'sys_normal_disable'
,
  },
]

// ── 字典预加载 ────────────────────────────────────────────────────────────
async function preloadDicts() {
  const types = new Set()
  columnsSchema.forEach(col => col._dictType && types.add(col._dictType))
  ;[...searchSchema, ...editSchema].forEach(f => f.dictType && types.add(f.dictType))
  for (const type of types) {
    try {
      dictCache.value[type] = await getDictData(type)
    } catch (e) {
      console.warn(`[Employee] 加载字典 ${type} 失败`, e)
    }
  }
}
onMounted(preloadDicts)

// ── 表格列转换（注入 dictTag render 函数） ────────────────────────────────
const computedColumns = computed(() =>
  columnsSchema.map((col) => {
    // 字典翻译字段：直接显示后端返回的 xxxName 字段
    if (col._transName) {
      return {
        ...col,
        render: row => row[col._transName] ?? row[col.prop],
      }
    }
    // 字典标签：用 DictTag 组件渲染
    if (col._dictType) {
      return {
        ...col,
        render: row => h(DictTag, { dictType: col._dictType, value: row[col.prop], size: 'small' }),
      }
    }
    return col
  }),
)

// ── 表单字段转换（注入字典选项） ──────────────────────────────────────────
function transformFields(fields) {
  return fields.map((field) => {
    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      return {
        ...field,
        props: { ...(field.props || {}), options: dictCache.value[field.dictType] || [] },
      }
    }
    return field
  })
}

const computedSearchSchema = computed(() => transformFields(searchSchema))
const computedEditSchema = computed(() => transformFields(editSchema))
</script>

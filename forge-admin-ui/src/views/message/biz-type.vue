<template>
  <div class="biz-type-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :form-schema="formSchema"
    />
  </div>
</template>

<script setup>
import { h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'MessageBizType' })

const crudRef = ref(null)

const apiConfig = {
  list: 'get@/api/message/bizType/page',
  detail: 'get@/api/message/bizType/{id}',
  create: 'post@/api/message/bizType',
  update: 'put@/api/message/bizType',
  delete: 'delete@/api/message/bizType/{id}'
}

const searchSchema = [
  {
    field: 'bizType',
    label: '业务类型编码',
    type: 'input',
    props: {
      placeholder: '请输入业务类型编码'
    }
  },
  {
    field: 'bizName',
    label: '业务类型名称',
    type: 'input',
    props: {
      placeholder: '请输入业务类型名称'
    }
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 }
      ]
    }
  }
]

const tableColumns = [
  {
    prop: 'bizType',
    label: '业务类型编码',
    width: 150
  },
  {
    prop: 'bizName',
    label: '业务类型名称',
    width: 150
  },
  {
    prop: 'jumpUrl',
    label: '跳转URL模板',
    ellipsis: { tooltip: true }
  },
  {
    prop: 'jumpTarget',
    label: '跳转方式',
    width: 100,
    render: (row) => {
      return h(NTag, { size: 'small' }, { 
        default: () => row.jumpTarget === '_blank' ? '新窗口' : '当前页' 
      })
    }
  },
  {
    prop: 'icon',
    label: '图标',
    width: 100
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.enabled === 1 ? 'success' : 'error',
        size: 'small'
      }, { default: () => row.enabled === 1 ? '启用' : '禁用' })
    }
  },
  {
    prop: 'remark',
    label: '备注',
    width: 150,
    ellipsis: { tooltip: true }
  }
]

const formSchema = {
  formConfig: {
    labelWidth: '120px',
    labelPlacement: 'left'
  },
  items: [
    {
      field: 'bizType',
      label: '业务类型编码',
      component: 'NInput',
      required: true,
      props: {
        placeholder: '请输入业务类型编码（如：ORDER、APPROVAL）'
      },
      rules: [
        { required: true, message: '请输入业务类型编码' }
      ]
    },
    {
      field: 'bizName',
      label: '业务类型名称',
      component: 'NInput',
      required: true,
      props: {
        placeholder: '请输入业务类型名称'
      },
      rules: [
        { required: true, message: '请输入业务类型名称' }
      ]
    },
    {
      field: 'jumpUrl',
      label: '跳转URL模板',
      component: 'NInput',
      props: {
        placeholder: '支持变量：${bizKey}、${messageId}，如：/order/detail?id=${bizKey}'
      }
    },
    {
      field: 'jumpTarget',
      label: '跳转方式',
      component: 'NSelect',
      defaultValue: '_self',
      props: {
        options: [
          { label: '当前页', value: '_self' },
          { label: '新窗口', value: '_blank' }
        ]
      }
    },
    {
      field: 'icon',
      label: '图标',
      component: 'NInput',
      props: {
        placeholder: '请输入图标名称'
      }
    },
    {
      field: 'sort',
      label: '排序',
      component: 'NInputNumber',
      defaultValue: 0,
      props: {
        min: 0,
        placeholder: '数值越小越靠前'
      }
    },
    {
      field: 'enabled',
      label: '状态',
      component: 'NSwitch',
      defaultValue: 1,
      props: {
        checkedValue: 1,
        uncheckedValue: 0
      }
    },
    {
      field: 'remark',
      label: '备注',
      component: 'NInput',
      props: {
        type: 'textarea',
        placeholder: '请输入备注说明',
        rows: 3
      }
    }
  ]
}
</script>

<style scoped>
.biz-type-page {
  height: 100%;
}
</style>
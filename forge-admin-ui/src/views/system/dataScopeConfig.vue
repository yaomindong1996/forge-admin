<template>
  <div class="data-scope-config-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/dataScopeConfig"
      :api-config="{
        list: 'get@/system/dataScopeConfig/page',
        detail: 'post@/system/dataScopeConfig/getById',
        add: 'post@/system/dataScopeConfig/add',
        update: 'post@/system/dataScopeConfig/edit',
        delete: 'post@/system/dataScopeConfig/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      add-button-text="新增配置"
      :load-detail-on-edit="true"
    >
      <!-- 自定义操作列 -->
      <template #table-action="{ row }">
        <div class="flex items-center gap-8">
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleEdit(row)"
          >
            编辑
          </a>
          <span class="text-gray-300">|</span>
          <a
            class="text-error cursor-pointer hover:text-error-hover"
            @click="handleDelete(row)"
          >
            删除
          </a>
        </div>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'DataScopeConfig' })

const crudRef = ref(null)

// 是否启用选项
const enabledOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'resourceCode',
    label: '资源编码',
    type: 'input',
    props: {
      placeholder: '请输入资源编码'
    }
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    props: {
      placeholder: '请输入资源名称'
    }
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'select',
    props: {
      placeholder: '请选择',
      options: enabledOptions
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80
  },
  {
    prop: 'resourceCode',
    label: '资源编码',
    width: 200,
    showOverflowTooltip: true
  },
  {
    prop: 'resourceName',
    label: '资源名称',
    width: 150
  },
  {
    prop: 'mapperMethod',
    label: 'Mapper方法',
    width: 300,
    showOverflowTooltip: true
  },
  {
    prop: 'tableAlias',
    label: '表别名',
    width: 100
  },
  {
    prop: 'userIdColumn',
    label: '用户ID字段',
    width: 150,
    showOverflowTooltip: true
  },
  {
    prop: 'orgIdColumn',
    label: '组织ID字段',
    width: 150,
    showOverflowTooltip: true
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.enabled === 1 ? 'success' : 'default', size: 'small' },
        { default: () => row.enabled === 1 ? '启用' : '禁用' }
      )
    }
  },
  {
    prop: 'remark',
    label: '备注',
    width: 200,
    showOverflowTooltip: true
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    _slot: 'action'
  }
]

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'resourceCode',
    label: '资源编码',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入资源编码，如：system:user:list'
    }
  },
  {
    field: 'resourceName',
    label: '资源名称',
    type: 'input',
    rules: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入资源名称'
    }
  },
  {
    field: 'mapperMethod',
    label: 'Mapper方法',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入Mapper方法', trigger: 'blur' }],
    props: {
      placeholder: '请输入Mapper方法，如：com.mdframe.forge.plugin.system.mapper.SysUserMapper.selectList',
      rows: 2
    }
  },
  {
    type: 'divider',
    label: '字段配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'tableAlias',
    label: '主表别名',
    type: 'input',
    defaultValue: 't',
    props: {
      placeholder: '请输入主表别名，如：t、lc、o'
    }
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'select',
    defaultValue: '1',
    props: {
      options: enabledOptions,
      clearable: false
    }
  },
  {
    field: 'userIdColumn',
    label: '用户ID字段',
    type: 'textarea',
    span: 2,
    defaultValue: 'user_id',
    rules: [{ required: true, message: '请输入用户ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：user_id、create_by）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea'
    }
  },
  {
    field: 'orgIdColumn',
    label: '组织ID字段',
    type: 'textarea',
    span: 2,
    defaultValue: 'org_id',
    rules: [{ required: true, message: '请输入组织ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：org_id、dept_id）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea'
    }
  },
  {
    field: 'tenantIdColumn',
    label: '租户ID字段',
    type: 'textarea',
    span: 2,
    defaultValue: 'tenant_id',
    rules: [{ required: true, message: '请输入租户ID字段', trigger: 'blur' }],
    props: {
      placeholder: '简单模式：字段名（如：tenant_id）\n复杂模式：<sql>开头，支持占位符#{userId}、#{tenantId}、#{orgIds}、#{customOrgIds}',
      rows: 3,
      type: 'textarea'
    }
  },
  {
    type: 'divider',
    label: '其他信息',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3
    }
  }
]

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该数据权限配置吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dataScopeConfig/remove', null, { 
          params: { id: row.id } 
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}
</script>

<style scoped>
.data-scope-config-page {
  height: 100%;
}
</style>

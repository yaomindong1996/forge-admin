<template>
  <div class="social-config-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/socialConfig"
      :api-config="{
        list: 'get@/system/socialConfig/page',
        detail: 'post@/system/socialConfig/getById',
        add: 'post@/system/socialConfig/add',
        update: 'post@/system/socialConfig/edit',
        delete: 'post@/system/socialConfig/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      add-button-text="新增三方登录配置"
      :load-detail-on-edit="true"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      :before-render-detail="handleBeforeRenderDetail"
      :before-submit="handleBeforeSubmit"
    >
      <template #toolbar-end>
        <n-button
          type="warning"
          @click="handleRefreshCache"
          :loading="refreshLoading"
        >
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          刷新缓存
        </n-button>
      </template>

      <template #table-action="{ row }">
        <div class="flex items-center gap-8">
          <a
            class="text-primary cursor-pointer hover:text-primary-hover"
            @click="handleView(row)"
          >
            查看
          </a>
          <span class="text-gray-300">|</span>
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

    <n-modal
      v-model:show="detailVisible"
      title="三方登录配置详情"
      preset="card"
      style="width: 800px"
      :mask-closable="false"
    >
      <div v-if="currentConfig" class="detail-content">
        <n-descriptions bordered :column="2">
          <n-descriptions-item label="ID">
            {{ currentConfig.id }}
          </n-descriptions-item>
           <n-descriptions-item label="平台类型">
             <DictTag dictType="sys_social_platform" :value="currentConfig.platform" size="small" />
           </n-descriptions-item>
          <n-descriptions-item label="平台名称">
            {{ currentConfig.platformName }}
          </n-descriptions-item>
          <n-descriptions-item label="平台Logo">
            <AuthImage
              v-if="currentConfig.platformLogo"
              :src="currentConfig.platformLogo"
              :img-style="{ width: '60px', height: '60px', objectFit: 'cover', borderRadius: '4px' }"
            />
            <span v-else>-</span>
          </n-descriptions-item>
          <n-descriptions-item label="应用ID">
            {{ currentConfig.clientId }}
          </n-descriptions-item>
          <n-descriptions-item label="应用Secret">
            ******
          </n-descriptions-item>
          <n-descriptions-item label="回调地址" :span="2">
            {{ currentConfig.redirectUri || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="AgentId">
            {{ currentConfig.agentId || '-' }}
          </n-descriptions-item>
          <n-descriptions-item label="授权范围">
            {{ currentConfig.scope || '-' }}
          </n-descriptions-item>
<n-descriptions-item label="状态">
            <DictTag dictType="sys_normal_disable" :value="String(currentConfig.status)" size="small" />
          </n-descriptions-item>
          <n-descriptions-item label="创建时间">
            {{ currentConfig.createTime }}
          </n-descriptions-item>
<n-descriptions-item label="更新时间">
            {{ currentConfig.updateTime }}
          </n-descriptions-item>
          <n-descriptions-item label="备注说明" :span="2">
            {{ currentConfig.remark || '-' }}
          </n-descriptions-item>
        </n-descriptions>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, computed } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'
import AuthImage from '@/components/common/AuthImage.vue'

defineOptions({ name: 'SocialConfig' })

const crudRef = ref(null)
const detailVisible = ref(false)
const currentConfig = ref(null)
const refreshLoading = ref(false)

const { dict } = useDict('sys_social_platform', 'sys_normal_disable')

// 使用 computed 从字典获取选项，响应式更新
const platformOptions = computed(() => dict.value['sys_social_platform'] || [])
const statusOptions = computed(() => dict.value['sys_normal_disable'] || [])

const searchSchema = computed(() => [
  {
    field: 'platform',
    label: '平台类型',
    type: 'select',
    props: {
      placeholder: '请选择平台类型',
      options: platformOptions.value,
      clearable: true
    }
  },
  {
    field: 'platformName',
    label: '平台名称',
    type: 'input',
    props: {
      placeholder: '请输入平台名称'
    }
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择',
      options: statusOptions.value,
      clearable: true
    }
  }
])

const tableColumns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80
  },
  {
    prop: 'platform',
    label: '平台类型',
    width: 120,
    render: (row) => {
      return h(DictTag,
        { dictType: 'sys_social_platform', value: row.platform, size: 'small' }
      )
    }
  },
  {
    prop: 'platformName',
    label: '平台名称',
    width: 120
  },
  {
    prop: 'platformLogo',
    label: 'Logo',
    width: 80,
    render: (row) => {
      if (row.platformLogo) {
        return h(AuthImage,
          {
            src: row.platformLogo,
            imgStyle: { width: '40px', height: '40px', objectFit: 'cover', borderRadius: '4px' }
          }
        )
      }
      return '-'
    }
  },
  {
    prop: 'clientId',
    label: '应用ID',
    width: 180,
    showOverflowTooltip: true
  },
  {
    prop: 'redirectUri',
    label: '回调地址',
    width: 200,
    showOverflowTooltip: true
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag,
        { dictType: 'sys_normal_disable', value: String(row.status), size: 'small' }
      )
    }
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 160
  },
  {
    prop: 'action',
    label: '操作',
    width: 200,
    fixed: 'right',
    _slot: 'action'
  }
]

const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'platform',
    label: '平台类型',
    type: 'select',
    rules: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
    props: {
      options: platformOptions.value,
      placeholder: '请选择平台类型',
      clearable: false
    }
  },
  {
    field: 'platformName',
    label: '平台名称',
    type: 'input',
    rules: [{ required: true, message: '请输入平台名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入平台名称，如：微信登录'
    }
  },
  {
    field: 'platformLogo',
    label: '平台Logo',
    type: 'imageUpload',
    span: 2,
    businessType: 'social-logo',
    limit: 1,
    fileSize: 2,
    valueType: 'string',
    props: {
      showTip: true
    }
  },
  {
    type: 'divider',
    label: '应用配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'clientId',
    label: '应用ID/Key',
    type: 'input',
    rules: [{ required: true, message: '请输入应用ID', trigger: 'blur' }],
    props: {
      placeholder: '请输入应用ID或Key'
    }
  },
  {
    field: 'clientSecret',
    label: '应用Secret',
    type: 'input',
    rules: [{ required: true, message: '请输入应用Secret', trigger: 'blur' }],
    props: {
      placeholder: '请输入应用Secret',
      type: 'password',
      showPasswordOn: 'click'
    }
  },
  {
    field: 'redirectUri',
    label: '回调地址',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入回调地址，如：https://example.com/auth/callback'
    }
  },
  {
    field: 'agentId',
    label: 'AgentId',
    type: 'input',
    props: {
      placeholder: '企业微信等需要时填写'
    }
  },
  {
    field: 'scope',
    label: '授权范围',
    type: 'input',
    props: {
      placeholder: '请输入授权范围，多个用逗号分隔'
    }
  },
  {
    type: 'divider',
    label: '其他配置',
    props: {
      titlePlacement: 'left'
    },
    span: 2
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: '0',
    rules: [{ required: true, message: '请选择状态', trigger: 'change' }],
    props: {
      options: statusOptions.value,
      clearable: false
    }
  },
  {
    field: 'remark',
    label: '备注说明',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3
    }
  }
])

async function handleView(row) {
  try {
    const res = await request.post('/system/socialConfig/getById',null, {
      params: { id: row.id }
    })
    if (res.code === 200) {
      currentConfig.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    window.$message.error('获取详情失败')
  }
}

function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该三方登录配置吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/socialConfig/remove', null, {
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

async function handleRefreshCache() {
  window.$dialog.warning({
    title: '确认刷新',
    content: '确定要刷新三方登录配置缓存吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        refreshLoading.value = true
        const res = await request.post('/system/socialConfig/refreshCache')
        if (res.code === 200) {
          window.$message.success('缓存刷新成功')
        }
      } catch (error) {
        window.$message.error('缓存刷新失败')
      } finally {
        refreshLoading.value = false
      }
    }
  })
}

function handleBeforeRenderDetail(data) {
  if (!data) return data

  if (data.status !== null && data.status !== undefined) {
    data.status = String(data.status)
  }

  return data
}

function handleBeforeSubmit(formData) {
  console.log('提交的表单数据:', formData)

  if (formData.status !== null && formData.status !== undefined) {
    formData.status = Number(formData.status)
  }

  return formData
}
</script>

<style scoped>
.social-config-page {
  height: 100%;
}

.detail-content {
  padding: 8px 0;
}
</style>

<template>
  <div class="storage-config-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/system/storage/config/page',
        detail: 'post@/system/storage/config/detail',
        add: 'post@/system/storage/config',
        update: 'put@/system/storage/config',
        delete: 'delete@/system/storage/config/{ids}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      add-button-text="新增存储配置"
      :load-detail-on-edit="true"
      :edit-grid-cols="2"
      modal-width="900px"
    >
      <!-- 自定义表单项：允许的文件类型 -->
      <template #form-allowedTypes="{ value, updateValue }">
        <div class="file-types-input">
          <NDynamicTags
            :value="parseAllowedTypes(value)"
            @update:value="(tags) => handleFileTypesChange(tags, updateValue)"
          >
            <template #input="{ submit, deactivate }">
              <NInput
                ref="inputRef"
                v-model:value="fileTypeInput"
                type="text"
                size="small"
                placeholder="输入文件类型，如 jpg"
                @keyup.enter="() => handleSubmitFileType(submit)"
                @blur="deactivate()"
              />
            </template>
            <template #trigger="{ activate, disabled }">
              <NButton
                size="small"
                type="primary"
                dashed
                :disabled="disabled"
                @click="activate()"
              >
                <template #icon>
                  <i class="i-material-symbols:add" />
                </template>
                添加类型
              </NButton>
            </template>
          </NDynamicTags>
          <div class="text-12 mt-4 text-gray-400">
            常用类型：jpg、png、gif、pdf、doc、docx、xls、xlsx、zip、rar
          </div>
        </div>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NButton, NDynamicTags, NInput, NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'StorageConfig' })

const crudRef = ref(null)
const inputRef = ref(null)
const fileTypeInput = ref('')

// 存储类型选项
const storageTypeOptions = [
  { label: '本地存储', value: 'local' },
  { label: 'RustFS', value: 'rustfs' },
  { label: 'MinIO', value: 'minio' },
  { label: '阿里云OSS', value: 'aliyun' },
  { label: '腾讯云COS', value: 'tencent' },
  { label: '七牛云', value: 'qiniu' },
  { label: 'AWS S3', value: 's3' },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'configName',
    label: '配置名称',
    type: 'input',
    props: {
      placeholder: '请输入配置名称',
    },
  },
  {
    field: 'storageType',
    label: '存储类型',
    type: 'select',
    props: {
      placeholder: '请选择存储类型',
      options: storageTypeOptions,
    },
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: [
        { label: '启用', value: true },
        { label: '禁用', value: false },
      ],
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'configName',
    label: '配置名称',
    minWidth: 150,
    render: (row) => {
      return h('div', { class: 'flex items-center gap-8' }, [
        row.isDefault ? h(NTag, { type: 'success', size: 'small' }, { default: () => '默认' }) : null,
        h('span', row.configName),
      ])
    },
  },
  {
    prop: 'storageType',
    label: '存储类型',
    width: 120,
    render: (row) => {
      const typeMap = {
        local: { text: '本地存储', type: 'default' },
        rustfs: { text: 'RustFS存储', type: 'success' },
        minio: { text: 'MinIO', type: 'info' },
        aliyun: { text: '阿里云OSS', type: 'warning' },
        tencent: { text: '腾讯云COS', type: 'success' },
        qiniu: { text: '七牛云', type: 'error' },
        s3: { text: 'AWS S3', type: 'primary' },
      }
      const config = typeMap[row.storageType] || { text: row.storageType, type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'endpoint',
    label: '访问端点',
    minWidth: 200,
    showOverflowTooltip: true,
  },
  {
    prop: 'bucketName',
    label: '存储桶',
    width: 150,
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.enabled ? 'success' : 'default',
        size: 'small',
      }, {
        default: () => row.enabled ? '启用' : '禁用',
      })
    },
  },
  {
    prop: 'maxFileSize',
    label: '最大文件(MB)',
    width: 120,
  },
  {
    prop: 'orderNum',
    label: '排序',
    width: 80,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '设为默认', key: 'setDefault', onClick: handleSetDefault, visible: row => !row.isDefault },
      { label: '测试连接', key: 'testConnection', onClick: handleTestConnection },
      { label: '禁用', key: 'disable', onClick: handleToggleEnabled, visible: row => row.enabled },
      { label: '启用', key: 'enable', onClick: handleToggleEnabled, visible: row => !row.enabled },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = [
  // ==================== 基础信息 ====================
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'configName',
    label: '配置名称',
    type: 'input',
    rules: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入配置名称，如：阿里云OSS生产环境',
    },
  },
  {
    field: 'storageType',
    label: '存储类型',
    type: 'select',
    defaultValue: 'local',
    rules: [{ required: true, message: '请选择存储类型', trigger: 'change' }],
    props: {
      placeholder: '请选择存储类型',
      options: storageTypeOptions,
    },
  },
  {
    field: 'enabled',
    label: '启用状态',
    type: 'radio',
    defaultValue: true,
    props: {
      options: [
        { label: '启用', value: true },
        { label: '禁用', value: false },
      ],
    },
  },
  {
    field: 'orderNum',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '数字越小越靠前',
      min: 0,
    },
  },

  // ==================== 连接配置 ====================
  {
    type: 'divider',
    label: '连接配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
    vIf: formData => formData.storageType !== 'local',
  },
  {
    field: 'endpoint',
    label: '访问端点',
    type: 'input',
    span: 2,
    rules: [
      {
        required: true,
        message: '请输入访问端点',
        trigger: 'blur',
        validator: (rule, value, formData) => {
          if (formData.storageType !== 'local' && !value) {
            return new Error('请输入访问端点')
          }
          return true
        },
      },
    ],
    props: {
      placeholder: '如：http://127.0.0.1:9000 或 https://oss-cn-hangzhou.aliyuncs.com',
    },
    vIf: formData => formData.storageType !== 'local',
  },
  {
    field: 'bucketName',
    label: '存储桶',
    type: 'input',
    rules: [
      {
        required: true,
        message: '请输入存储桶名称',
        trigger: 'blur',
        validator: (rule, value, formData) => {
          if (formData.storageType !== 'local' && !value) {
            return new Error('请输入存储桶名称')
          }
          return true
        },
      },
    ],
    props: {
      placeholder: '请输入存储桶名称',
    },
    vIf: formData => formData.storageType !== 'local',
  },
  {
    field: 'region',
    label: '区域',
    type: 'input',
    props: {
      placeholder: '如：cn-hangzhou、ap-guangzhou',
    },
    vIf: formData => ['aliyun', 'tencent', 's3'].includes(formData.storageType),
  },
  {
    field: 'accessKey',
    label: 'AccessKey',
    type: 'input',
    rules: [
      {
        required: true,
        message: '请输入AccessKey',
        trigger: 'blur',
        validator: (rule, value, formData) => {
          if (formData.storageType !== 'local' && !value) {
            return new Error('请输入AccessKey')
          }
          return true
        },
      },
    ],
    props: {
      placeholder: '请输入访问密钥ID',
    },
    vIf: formData => formData.storageType !== 'local',
  },
  {
    field: 'secretKey',
    label: 'SecretKey',
    type: 'input',
    rules: [
      {
        required: true,
        message: '请输入SecretKey',
        trigger: 'blur',
        validator: (rule, value, formData) => {
          if (formData.storageType !== 'local' && !value) {
            return new Error('请输入SecretKey')
          }
          return true
        },
      },
    ],
    props: {
      placeholder: '请输入访问密钥Secret',
      type: 'password',
      showPasswordOn: 'click',
    },
    vIf: formData => formData.storageType !== 'local',
  },
  {
    field: 'useHttps',
    label: '使用HTTPS',
    type: 'switch',
    defaultValue: true,
    span: 2,
    vIf: formData => formData.storageType !== 'local',
  },

  // ==================== 路径配置 ====================
  {
    type: 'divider',
    label: '路径配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'basePath',
    label: '基础路径',
    type: 'input',
    props: {
      placeholder: '文件存储的基础路径，如：/upload 或 /files',
    },
  },
  {
    field: 'domain',
    label: '访问域名',
    type: 'input',
    props: {
      placeholder: '文件访问域名，如：https://cdn.example.com',
    },
  },

  // ==================== 限制配置 ====================
  {
    type: 'divider',
    label: '限制配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'maxFileSize',
    label: '最大文件(MB)',
    type: 'input-number',
    defaultValue: 100,
    props: {
      placeholder: '单位：MB',
      min: 1,
      max: 10240,
    },
  },
  {
    field: 'allowedTypes',
    label: '允许类型',
    type: 'slot',
    slotName: 'allowedTypes',
    span: 2,
  },
  {
    field: 'extraConfig',
    label: '扩展配置',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: 'JSON格式的扩展配置（可选）',
      rows: 3,
    },
  },
]

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 解析允许的文件类型（字符串 -> 数组）
function parseAllowedTypes(value) {
  if (!value)
    return []
  return value.split(',').map(type => type.trim()).filter(Boolean)
}

// 处理文件类型标签变化（数组 -> 字符串）
function handleFileTypesChange(tags, updateValue) {
  // 将标签数组转换为逗号分隔的字符串
  const typesString = tags.join(',')
  updateValue(typesString)
}

// 处理文件类型提交
function handleSubmitFileType(submit) {
  if (fileTypeInput.value && fileTypeInput.value.trim()) {
    submit(fileTypeInput.value.trim())
    fileTypeInput.value = ''
  }
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该存储配置吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/storage/config/${row.id}`)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}

// 设为默认
function handleSetDefault(row) {
  window.$dialog.info({
    title: '确认操作',
    content: `确定要将"${row.configName}"设置为默认存储配置吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.put(`/system/storage/config/default/${row.id}`)
        if (res.code === 200) {
          window.$message.success('设置成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('设置失败')
      }
    },
  })
}

// 启用/禁用
function handleToggleEnabled(row) {
  const action = row.enabled ? '禁用' : '启用'
  window.$dialog.info({
    title: '确认操作',
    content: `确定要${action}"${row.configName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.put(`/system/storage/config/enabled/${row.id}/${!row.enabled}`)
        if (res.code === 200) {
          window.$message.success(`${action}成功`)
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error(`${action}失败`)
      }
    },
  })
}

// 测试连接
async function handleTestConnection(row) {
  try {
    const res = await request.post(`/system/storage/config/test/${row.id}`)
    if (res.code === 200 && res.data) {
      window.$message.success('连接测试成功')
    }
    else {
      window.$message.error('连接测试失败')
    }
  }
  catch (error) {
    window.$message.error(`连接测试失败：${error.message || '未知错误'}`)
  }
}
</script>

<style scoped>
.storage-config-page {
  height: 100%;
}

.file-types-input {
  width: 100%;
}

.file-types-input :deep(.n-dynamic-tags) {
  width: 100%;
}

.file-types-input :deep(.n-dynamic-tags .n-tag) {
  margin-right: 8px;
  margin-bottom: 8px;
}
</style>

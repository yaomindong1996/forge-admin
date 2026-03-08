<template>
  <div class="system-tenant-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/tenant"
      :api-config="{
        list: 'get@/system/tenant/page',
        detail: 'post@/system/tenant/getById',
        add: 'post@/system/tenant/add',
        update: 'post@/system/tenant/edit',
        delete: 'post@/system/tenant/remove'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'900px'"
      add-button-text="新增租户"
      :hide-selection="false"
      :before-submit="handleBeforeSubmit"
      :before-render-detail="handleBeforeRenderDetail"
      @selection-change="handleSelectionChange"
      @submit-success="handleSubmitSuccess"
    >
      <!-- 批量删除按钮 -->
      <template #toolbar-end>
        <n-button
          type="error"
          @click="handleBatchDelete"
          :disabled="selectedKeys.length === 0"
          size="small"
        >
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
          批量删除
        </n-button>
      </template>

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

      <!-- 系统布局选择器 -->
      <template #form-systemLayout="{ value, updateValue }">
        <div class="layout-selector">
          <n-radio-group :value="value || 'normal'" @update:value="updateValue">
            <n-space>
              <n-radio
                v-for="layout in systemLayoutOptions"
                :key="layout.value"
                :value="layout.value"
              >
                <div class="layout-card" :class="{ active: value === layout.value }">
                  <img :src="layout.preview" :alt="layout.label" class="layout-img" />
                  <span class="layout-name">{{ layout.label }}</span>
                </div>
              </n-radio>
            </n-space>
          </n-radio-group>
        </div>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { NTag, NIcon } from 'naive-ui'
import { TrashOutline } from '@vicons/ionicons5'
import { AiCrudPage } from '@/components/ai-form'
import { request, getFileUrl } from '@/utils'

defineOptions({ name: 'SystemTenant' })

const crudRef = ref(null)
const selectedKeys = ref([])

// 租户状态选项
const tenantStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 }
]

// 系统布局选项（与布局设置保持一致）
const systemLayoutOptions = [
  {
    label: '简约',
    value: 'simple',
    description: '简单布局，仅包含基本元素',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNjAiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4='
  },
  {
    label: '通用',
    value: 'normal',
    description: '侧边栏菜单布局',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iMTAiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTQiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0NiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg=='
  },
  {
    label: '顶部菜单',
    value: 'top-menu',
    description: '一级菜单在顶部，二级及以下菜单在左侧',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjEwNCIgaGVpZ2h0PSI0OCIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg=='
  },
  {
    label: '顶部加侧面菜单',
    value: 'top-side-menu',
    description: '顶部+左侧混合布局',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRTBFMEUwIi8+PHJlY3QgeD0iMjIiIHk9IjEyIiB3aWR0aD0iODIiIGhlaWdodD0iNDgiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4='
  },
  {
    label: '全屏',
    value: 'full',
    description: '无边框全屏布局',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNiIgZmlsbD0iI0Y1RjVGNSIvPjxyZWN0IHg9IjI0IiB5PSIxMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjQiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTgiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0MiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg=='
  }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'tenantName',
    label: '租户名称',
    type: 'input',
    props: {
      placeholder: '请输入租户名称'
    }
  },
  {
    field: 'contactPerson',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人'
    }
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    type: 'input',
    props: {
      placeholder: '请输入联系电话'
    }
  },
  {
    field: 'tenantStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: tenantStatusOptions
    }
  }
]

// 表格列配置
const tableColumns = [
  {
    prop: 'tenantName',
    label: '租户名称',
    width: 180
  },
  {
    prop: 'contactPerson',
    label: '负责人',
    width: 120
  },
  {
    prop: 'contactPhone',
    label: '联系电话',
    width: 130
  },
  {
    prop: 'userLimit',
    label: '人员上限',
    width: 100,
    render: (row) => {
      return row.userLimit === 0 ? '无限制' : row.userLimit
    }
  },
  {
    prop: 'expireTime',
    label: '过期时间',
    width: 180
  },
  {
    prop: 'tenantStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag,
        { type: row.tenantStatus === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.tenantStatus === 1 ? '正常' : '禁用' }
      )
    }
  },
  {
    prop: 'tenantDesc',
    label: '描述',
    minWidth: 150
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
const editSchema = ref([
  // ==================== 基础信息 ====================
  {
    type: 'divider',
    label: '基础信息',
    props: { titlePlacement: 'left' },
    span: 2
  },
  {
    field: 'tenantName',
    label: '租户名称',
    type: 'input',
    rules: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
    props: { placeholder: '请输入租户名称' }
  },
  {
    field: 'tenantStatus',
    label: '租户状态',
    type: 'radio',
    defaultValue: 1,
    props: { options: tenantStatusOptions }
  },
  {
    field: 'contactPerson',
    label: '负责人',
    type: 'input',
    rules: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
    props: { placeholder: '请输入负责人' }
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    type: 'input',
    rules: [
      { required: true, message: '请输入联系电话', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    props: { placeholder: '请输入联系电话' }
  },
  {
    field: 'userLimit',
    label: '人员上限',
    type: 'input-number',
    defaultValue: 0,
    props: { placeholder: '0表示无限制', min: 0 }
  },
  {
    field: 'expireTime',
    label: '过期时间',
    type: 'datetime',
    props: { placeholder: '请选择过期时间', clearable: true }
  },
  {
    field: 'tenantDesc',
    label: '租户描述',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入租户描述', rows: 2 }
  },

  // ==================== 品牌设置 ====================
  {
    type: 'divider',
    label: '品牌设置',
    props: { titlePlacement: 'left' },
    span: 2
  },
  {
    field: 'systemName',
    label: '系统名称',
    type: 'input',
    props: { placeholder: '显示在系统左上角' }
  },
  {
    field: 'browserTitle',
    label: '浏览器标签',
    type: 'input',
    props: { placeholder: '浏览器标签页显示的名称' }
  },
  {
    field: 'systemLogo',
    label: '系统Logo',
    type: 'imageUpload',
    businessType: 'tenant-logo',
    limit: 1,
    fileSize: 2,
    valueType: 'string',
    props: { showTip: true }
  },
  {
    field: 'browserIcon',
    label: '浏览器图标',
    type: 'imageUpload',
    businessType: 'tenant-icon',
    limit: 1,
    fileSize: 1,
    fileType: ['png', 'ico', 'jpg'],
    valueType: 'string',
    props: { showTip: true }
  },
  {
    field: 'systemIntro',
    label: '系统介绍',
    type: 'textarea',
    span: 2,
    props: { placeholder: '登录页显示的系统介绍', rows: 2 }
  },
  {
    field: 'copyrightInfo',
    label: '版权信息',
    type: 'textarea',
    span: 2,
    props: { placeholder: '页面底部显示的版权信息', rows: 2 }
  },

  // ==================== 界面风格 ====================
  {
    type: 'divider',
    label: '界面风格',
    props: { titlePlacement: 'left' },
    span: 2
  },
  {
    field: 'systemLayout',
    label: '系统布局',
    type: 'slot',
    slotName: 'systemLayout',
    span: 2
  },
  {
    field: 'systemTheme',
    label: '主题色',
    type: 'color',
    defaultValue: '#d12723',
    props: { showAlpha: false }
  },

  // ==================== 高级主题配置（可选） ====================
  {
    type: 'divider',
    label: '高级主题配置（可选）',
    props: { titlePlacement: 'left', dashed: true },
    span: 2
  },
  // Header 配置
  {
    field: 'theme_header_backgroundColor',
    label: 'Header背景',
    type: 'color',
    defaultValue: '#d12723FF',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_header_textColor',
    label: 'Header文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    props: { showAlpha: false, modes: ['hex'] }
  },
  // 侧边菜单配置
  {
    field: 'theme_sideMenu_backgroundColor',
    label: '侧边栏背景',
    type: 'color',
    defaultValue: '#ffffff',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_textColor',
    label: '侧边栏文字',
    type: 'color',
    defaultValue: '#333333',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_textColorActive',
    label: '菜单选中色',
    type: 'color',
    defaultValue: '#316cfa',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_backgroundColorActive',
    label: '菜单选中背景',
    type: 'color',
    defaultValue: '#f6eded',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_iconColor',
    label: '侧边栏图标',
    type: 'color',
    defaultValue: '#666666',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_iconColorActive',
    label: '选中图标色',
    type: 'color',
    defaultValue: '#d12723FF',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_sideMenu_width',
    label: '菜单宽度',
    type: 'input',
    defaultValue: '220px',
    props: { placeholder: '例如: 220px' }
  },
  {
    field: 'theme_sideMenu_collapsedWidth',
    label: '折叠宽度',
    type: 'input',
    defaultValue: '64px',
    props: { placeholder: '例如: 64px' }
  },
  {
    field: 'theme_header_height',
    label: 'Header高度',
    type: 'input',
    defaultValue: '60px',
    props: { placeholder: '例如: 60px' }
  },

  // ==================== 顶部菜单配置 ====================
  {
    type: 'divider',
    label: '顶部菜单配置',
    props: { titlePlacement: 'left', dashed: true },
    span: 2
  },
  {
    field: 'theme_topMenu_textColor',
    label: '菜单文字色',
    type: 'color',
    defaultValue: '#FFFFFF',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_topMenu_textColorActive',
    label: '选中文字色',
    type: 'color',
    defaultValue: '#FFFFFF',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_topMenu_backgroundColorActive',
    label: '选中背景色',
    type: 'color',
    defaultValue: '#ffffff',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_topMenu_iconColor',
    label: '图标颜色',
    type: 'color',
    defaultValue: '#ffffff',
    props: { showAlpha: false, modes: ['hex'] }
  },
  {
    field: 'theme_topMenu_iconActiveColor',
    label: '选中图标色',
    type: 'color',
    defaultValue: '#333333',
    props: { showAlpha: false, modes: ['hex'] }
  }
])

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除租户"${row.tenantName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/tenant/remove', null, { params: { id: row.id } })
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

// 批量删除
function handleBatchDelete() {
  if (selectedKeys.value.length === 0) {
    window.$message.warning('请先选择要删除的租户')
    return
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedKeys.value.length} 个租户吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/tenant/removeBatch', selectedKeys.value)
        if (res.code === 200) {
          window.$message.success('批量删除成功')
          selectedKeys.value = []
          crudRef.value?.refresh()
        }
      } catch (error) {
        window.$message.error('批量删除失败')
      }
    }
  })
}

// 监听选中项变化
function handleSelectionChange({ keys }) {
  selectedKeys.value = keys
}

// 提交成功后处理 - 重新加载租户配置并应用
async function handleSubmitSuccess() {
  console.log('租户配置保存成功，重新加载配置...')

  // 重新加载租户配置
  const { useTenantStore, useUserStore, useAppStore } = await import('@/store')
  const tenantStore = useTenantStore()
  const userStore = useUserStore()
  const appStore = useAppStore()

  // 获取最新的租户配置
  const tenantConfig = await tenantStore.loadTenantConfig(userStore.userInfo?.tenantId)

  if (tenantConfig) {
    console.log('最新租户配置:', tenantConfig)

    // 应用系统布局
    if (tenantConfig.systemLayout) {
      appStore.setLayout(tenantConfig.systemLayout)
    }

    // 应用完整的主题配置
    const themeConfigObj = tenantStore.themeConfig
    if (themeConfigObj) {
      // 导入默认配置
      const { defaultThemeConfig } = await import('@/config/theme.config')

      // 深度合并：租户配置覆盖默认配置
      // 优先使用 systemTheme，如果没有才使用 themeConfig.primaryColor
      const primaryColor = tenantConfig.systemTheme || themeConfigObj.primaryColor || defaultThemeConfig.primaryColor

      const mergedConfig = {
        primaryColor: primaryColor,
        header: {
          ...defaultThemeConfig.header,
          ...themeConfigObj.header
        },
        headerDark: {
          ...defaultThemeConfig.headerDark,
          ...themeConfigObj.headerDark
        },
        topMenu: {
          ...defaultThemeConfig.topMenu,
          ...themeConfigObj.topMenu
        },
        topMenuDark: {
          ...defaultThemeConfig.topMenuDark,
          ...themeConfigObj.topMenuDark
        },
        sideMenu: {
          ...defaultThemeConfig.sideMenu,
          ...themeConfigObj.sideMenu
        },
        sideMenuDark: {
          ...defaultThemeConfig.sideMenuDark,
          ...themeConfigObj.sideMenuDark
        }
      }

      console.log('合并后的主题配置:', mergedConfig)
      appStore.setThemeConfig(mergedConfig)
    }

    // 应用浏览器标题
    if (tenantConfig.browserTitle) {
      document.title = tenantConfig.browserTitle
    }

    // 应用浏览器图标
    if (tenantConfig.browserIcon) {
      const link = document.querySelector("link[rel*='icon']") || document.createElement('link')
      link.type = 'image/x-icon'
      link.rel = 'shortcut icon'
      // 使用 getFileUrl 转换 fileId 为完整的下载 URL
      const iconUrl = tenantConfig.browserIcon
      if (iconUrl.startsWith('http://') || iconUrl.startsWith('https://') || iconUrl.startsWith('data:')) {
        link.href = iconUrl
      } else {
        link.href = getFileUrl(iconUrl)
      }
      document.getElementsByTagName('head')[0].appendChild(link)
    }

    window.$message.success('主题配置已更新')
  }
}

// 提交前处理 - 将主题配置字段组装成 JSON
function handleBeforeSubmit(formData) {
  console.log('提交前的表单数据:', formData)

  // 检查是否有任何主题配置字段
  const hasThemeConfig = formData.theme_header_backgroundColor
    || formData.theme_header_textColor
    || formData.theme_header_height
    || formData.theme_topMenu_textColor
    || formData.theme_topMenu_textColorActive
    || formData.theme_topMenu_backgroundColorActive
    || formData.theme_topMenu_iconColor
    || formData.theme_topMenu_iconActiveColor
    || formData.theme_sideMenu_backgroundColor
    || formData.theme_sideMenu_textColor
    || formData.theme_sideMenu_textColorActive
    || formData.theme_sideMenu_backgroundColorActive
    || formData.theme_sideMenu_iconColor
    || formData.theme_sideMenu_iconColorActive
    || formData.theme_sideMenu_width
    || formData.theme_sideMenu_collapsedWidth

  // 只有配置了主题字段时才组装 themeConfig
  if (hasThemeConfig) {
    const themeConfig = {
      primaryColor: formData.systemTheme || formData.theme_header_backgroundColor || '#d12723',
      header: {
        backgroundColor: formData.theme_header_backgroundColor || '#d12723FF',
        textColor: formData.theme_header_textColor || '#FFFFFF',
        height: formData.theme_header_height || '60px',
      },
      topMenu: {
        textColor: formData.theme_topMenu_textColor || '#FFFFFF',
        textColorActive: formData.theme_topMenu_textColorActive || '#FFFFFF',
        backgroundColorActive: formData.theme_topMenu_backgroundColorActive || '#ffffff',
        iconColor: formData.theme_topMenu_iconColor || '#ffffff',
        iconActiveColor: formData.theme_topMenu_iconActiveColor || '#333333',
      },
      sideMenu: {
        backgroundColor: formData.theme_sideMenu_backgroundColor || '#ffffff',
        textColor: formData.theme_sideMenu_textColor || '#333333',
        textColorActive: formData.theme_sideMenu_textColorActive || '#316cfa',
        backgroundColorActive: formData.theme_sideMenu_backgroundColorActive || '#f6eded',
        iconColor: formData.theme_sideMenu_iconColor || '#666666',
        iconColorActive: formData.theme_sideMenu_iconColorActive || '#d12723FF',
        width: formData.theme_sideMenu_width || '220px',
        collapsedWidth: formData.theme_sideMenu_collapsedWidth || '64px',
      }
    }

    // 将主题配置转换为 JSON 字符串
    formData.themeConfig = JSON.stringify(themeConfig)
    console.log('组装的主题配置:', formData.themeConfig)
  }

  // 删除临时字段
  delete formData.theme_header_backgroundColor
  delete formData.theme_header_textColor
  delete formData.theme_header_height
  delete formData.theme_topMenu_textColor
  delete formData.theme_topMenu_textColorActive
  delete formData.theme_topMenu_backgroundColorActive
  delete formData.theme_topMenu_iconColor
  delete formData.theme_topMenu_iconActiveColor
  delete formData.theme_sideMenu_backgroundColor
  delete formData.theme_sideMenu_textColor
  delete formData.theme_sideMenu_textColorActive
  delete formData.theme_sideMenu_backgroundColorActive
  delete formData.theme_sideMenu_iconColor
  delete formData.theme_sideMenu_iconColorActive
  delete formData.theme_sideMenu_width
  delete formData.theme_sideMenu_collapsedWidth

  console.log('处理后的表单数据:', formData)
  return formData
}

// 详情渲染前处理 - 将 JSON 拆解到各个字段
function handleBeforeRenderDetail(data) {
  console.log('详情数据:', data)

  if (data.themeConfig) {
    try {
      const themeConfig = typeof data.themeConfig === 'string'
        ? JSON.parse(data.themeConfig)
        : data.themeConfig

      console.log('解析的主题配置:', themeConfig)

      // 拆解到各个字段
      data.systemTheme = themeConfig.primaryColor  // 主题颜色
      data.theme_header_backgroundColor = themeConfig.header?.backgroundColor || themeConfig.primaryColor
      data.theme_header_textColor = themeConfig.header?.textColor
      data.theme_header_height = themeConfig.header?.height
      data.theme_topMenu_textColor = themeConfig.topMenu?.textColor
      data.theme_topMenu_textColorActive = themeConfig.topMenu?.textColorActive
      data.theme_topMenu_backgroundColorActive = themeConfig.topMenu?.backgroundColorActive
      data.theme_topMenu_iconColor = themeConfig.topMenu?.iconColor
      data.theme_topMenu_iconActiveColor = themeConfig.topMenu?.iconActiveColor
      data.theme_sideMenu_backgroundColor = themeConfig.sideMenu?.backgroundColor
      data.theme_sideMenu_textColor = themeConfig.sideMenu?.textColor
      data.theme_sideMenu_textColorActive = themeConfig.sideMenu?.textColorActive
      data.theme_sideMenu_backgroundColorActive = themeConfig.sideMenu?.backgroundColorActive
      data.theme_sideMenu_iconColor = themeConfig.sideMenu?.iconColor
      data.theme_sideMenu_iconColorActive = themeConfig.sideMenu?.iconColorActive
      data.theme_sideMenu_width = themeConfig.sideMenu?.width
      data.theme_sideMenu_collapsedWidth = themeConfig.sideMenu?.collapsedWidth

      console.log('拆解后的数据:', data)
    } catch (error) {
      console.error('解析主题配置失败:', error)
    }
  }

  return data
}
</script>

<style scoped>
.system-tenant-page {
  height: 100%;
}

.layout-selector {
  width: 100%;
}

.layout-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.layout-card:hover {
  border-color: #c0c4cc;
}

.layout-card.active,
:deep(.n-radio:has(input:checked)) .layout-card {
  border-color: var(--primary-color, #18a058);
  background: rgba(24, 160, 88, 0.05);
}

.layout-img {
  width: 80px;
  height: 50px;
  object-fit: contain;
  margin-bottom: 4px;
}

.layout-name {
  font-size: 12px;
  color: #606266;
}
</style>

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
        delete: 'post@/system/tenant/removeBatch',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      :edit-x-gap="14"
      :edit-y-gap="4"
      edit-form-class="tenant-config-form"
      modal-width="960px"
      add-button-text="新增租户"
      :hide-add="!userStore.isAdmin"
      :hide-batch-delete="!userStore.isAdmin"
      :hide-selection="!userStore.isAdmin"
      :before-submit="handleBeforeSubmit"
      :before-delete="handleBeforeDelete"
      :before-render-detail="handleBeforeRenderDetail"
      @submit-success="handleSubmitSuccess"
    >
      <!-- 系统布局选择器 -->
      <template #form-systemLayout="{ value, updateValue }">
        <div class="layout-selector">
          <section
            v-for="group in layoutOptionGroups"
            :key="group.key"
            class="layout-group"
          >
            <div class="layout-group-header">
              <span class="layout-group-title">{{ group.title }}</span>
              <span class="layout-group-desc">{{ group.description }}</span>
            </div>
            <div class="layout-group-grid">
              <button
                v-for="layout in group.options"
                :key="layout.value"
                class="layout-option"
                :class="{ active: (value || 'normal') === layout.value }"
                type="button"
                @click="updateValue(layout.value)"
              >
                <span class="layout-preview-card">
                  <img :src="layout.preview" :alt="layout.label" class="layout-img">
                </span>
                <span class="layout-option-main">
                  <span class="layout-option-title">
                    {{ layout.label }}
                    <span v-if="layout.recommended" class="layout-option-badge">推荐</span>
                  </span>
                  <span class="layout-option-desc">{{ layout.description }}</span>
                </span>
                <span
                  v-if="(value || 'normal') === layout.value"
                  class="layout-option-check"
                  aria-hidden="true"
                >
                  <i class="i-material-symbols:check-small-rounded" />
                </span>
              </button>
            </div>
          </section>
        </div>
      </template>

      <template #form-themePreset="{ value, updateValue, formData }">
        <div class="theme-preset-selector">
          <button
            v-for="preset in tenantThemePresets"
            :key="preset.key"
            class="theme-preset-option"
            :class="{ active: value === preset.key }"
            type="button"
            @click="applyTenantThemePreset(preset, formData, updateValue)"
          >
            <span class="theme-preset-swatches">
              <span :style="{ background: preset.colors.headerBackground }" />
              <span :style="{ background: preset.colors.sideBackground }" />
              <span :style="{ background: preset.colors.sideActiveBackground }" />
              <span :style="{ background: preset.colors.primary }" />
            </span>
            <span class="theme-preset-main">
              <span class="theme-preset-title">{{ preset.name }}</span>
              <span class="theme-preset-desc">{{ preset.description }}</span>
            </span>
            <span v-if="value === preset.key" class="theme-preset-check" aria-hidden="true">
              <i class="i-material-symbols:check-small-rounded" />
            </span>
          </button>
        </div>
      </template>
    </AiCrudPage>

    <n-modal
      v-model:show="usersModalVisible"
      :title="`租户用户 - ${currentTenant.tenantName || ''}`"
      preset="card"
      style="width: 920px"
      :mask-closable="false"
    >
      <div class="tenant-users-modal">
        <div class="tenant-users-search">
          <n-space>
            <n-input
              v-model:value="userSearchParams.username"
              placeholder="用户名"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-input
              v-model:value="userSearchParams.realName"
              placeholder="真实姓名"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-input
              v-model:value="userSearchParams.phone"
              placeholder="手机号"
              clearable
              size="small"
              style="width: 150px"
              @clear="handleUserSearch"
              @keyup.enter="handleUserSearch"
            />
            <n-select
              v-model:value="userSearchParams.userStatus"
              placeholder="用户状态"
              clearable
              size="small"
              style="width: 120px"
              :options="userStatusOptions"
            />
            <n-button size="small" type="primary" @click="handleUserSearch">
              <template #icon>
                <i class="i-material-symbols:search" />
              </template>
              查询
            </n-button>
            <n-button size="small" @click="handleUserSearchReset">
              重置
            </n-button>
          </n-space>
        </div>

        <div class="tenant-users-toolbar">
          <n-space justify="space-between">
            <NTag type="info" size="small">
              共 {{ userPagination.itemCount }} 个用户
            </NTag>
            <n-button size="small" @click="loadTenantUsers">
              <template #icon>
                <i class="i-material-symbols:refresh" />
              </template>
              刷新
            </n-button>
          </n-space>
        </div>

        <n-spin :show="usersLoading">
          <n-data-table
            :columns="userTableColumns"
            :data="tenantUsers"
            :pagination="userPaginationConfig"
            :row-key="row => row.id"
            remote
            striped
            size="small"
            @update:page="handleUserPageChange"
            @update:page-size="handleUserPageSizeChange"
          />
        </n-spin>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="usersModalVisible = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import { resolveTenantPublicAssetUrl, setDocumentFavicon } from '@/utils/tenant-config'

defineOptions({ name: 'SystemTenant' })

const DEFAULT_TENANT_ID = 1
const NORMAL_DISABLE_DICT = 'sys_normal_disable'
const USER_TYPE_DICT = 'sys_user_type'
const USER_STATUS_DICT = 'sys_user_status'

const crudRef = ref(null)
const userStore = useUserStore()
const usersModalVisible = ref(false)
const usersLoading = ref(false)
const currentTenant = ref({})
const tenantUsers = ref([])
const businessDatasources = ref([])
const userSearchParams = ref({
  username: '',
  realName: '',
  phone: '',
  userStatus: null,
})
const userPagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})

const { dict } = useDict(NORMAL_DISABLE_DICT, USER_TYPE_DICT, USER_STATUS_DICT)

const tenantStatusOptions = computed(() => toNumberOptions(dict.value[NORMAL_DISABLE_DICT]))
const userStatusOptions = computed(() => toNumberOptions(dict.value[USER_STATUS_DICT]))
const businessDatasourceOptions = computed(() => businessDatasources.value.map(item => ({
  label: `${item.datasourceName} (${item.datasourceCode || item.dbType})`,
  value: item.datasourceId,
})))
const userPaginationConfig = computed(() => ({
  page: userPagination.value.page,
  pageSize: userPagination.value.pageSize,
  itemCount: userPagination.value.itemCount,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
}))

// 系统布局选项（与布局设置保持一致）
const systemLayoutOptions = [
  {
    label: '简约',
    value: 'simple',
    description: '简单布局，仅包含基本元素',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNjAiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4=',
  },
  {
    label: '通用',
    value: 'normal',
    description: '侧边栏菜单布局',
    recommended: true,
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iMTAiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTQiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0NiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    label: '顶部菜单',
    value: 'top-menu',
    description: '一级菜单在顶部，二级及以下菜单在左侧',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjEwNCIgaGVpZ2h0PSI0OCIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    label: '顶部加侧面菜单',
    value: 'top-side-menu',
    description: '顶部+左侧混合布局',
    recommended: true,
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iMTAiIGZpbGw9IiNFMEUwRTAiLz48cmVjdCB5PSIxMiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRTBFMEUwIi8+PHJlY3QgeD0iMjIiIHk9IjEyIiB3aWR0aD0iODIiIGhlaWdodD0iNDgiIGZpbGw9IiNGNUY1RjUiLz48L3N2Zz4=',
  },
  {
    label: '全屏',
    value: 'full',
    description: '无边框全屏布局',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyMCIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjI0IiB3aWR0aD0iODAiIGhlaWdodD0iNiIgZmlsbD0iI0Y1RjVGNSIvPjxyZWN0IHg9IjI0IiB5PSIxMCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjQiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB4PSIyNCIgeT0iMTgiIHdpZHRoPSI4MCIgaGVpZ2h0PSI0MiIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    label: '沉浸式',
    value: 'immersive',
    description: '无侧边栏，抽屉式菜单，最大化内容展示区域',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDQiIGhlaWdodD0iOCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHk9IjEyIiB3aWR0aD0iMTA0IiBoZWlnaHQ9IjQiIGZpbGw9IiNGNUY1RjUiLz48cmVjdCB5PSIyMCIgd2lkdGg9IjEwNCIgaGVpZ2h0PSI0MCIgZmlsbD0iI0Y1RjVGNSIvPjwvc3ZnPg==',
  },
  {
    label: '便当盒',
    value: 'bento',
    description: '超窄图标导航栏，右侧抽屉菜单，极致内容空间',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMiIgaGVpZ2h0PSI2MCIgZmlsbD0iI0UwRTBFMCIvPjxyZWN0IHg9IjE2IiB3aWR0aD0iODgiIGhlaWdodD0iOCIgZmlsbD0iI0Y1RjVGNSIvPjxyZWN0IHg9IjE2IiB5PSIxMiIgd2lkdGg9Ijg4IiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRjVGNUY1Ii8+PC9zdmc+',
  },
  {
    label: 'Nexus 浮岛',
    value: 'nexus',
    description: '浮岛卡片式侧边栏与内容区，现代商务风格',
    preview: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTA0IiBoZWlnaHQ9IjYwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHg9IjMiIHk9IjMiIHdpZHRoPSIxNiIgaGVpZ2h0PSI1NCIgZmlsbD0iI0YwRjBGMCIgc3Ryb2tlPSIjRDVENTVFIiByeD0iMiIvPjxyZWN0IHg9IjIyIiB5PSIzIiB3aWR0aD0iNzkiIGhlaWdodD0iOCIgZmlsbD0iI0YwRjBGMCIgc3Ryb2tlPSIjRDVENTVFIiByeD0iMiIvPjxyZWN0IHg9IjIyIiB5PSIxNCIgd2lkdGg9Ijc5IiBoZWlnaHQ9IjQzIiBmaWxsPSIjRjBGMEYwIiBzdHJva2U9IiNENUQ1RTUiIHJ4PSIyIi8+PC9zdmc+',
  },
]

const tenantThemePresets = [
  {
    key: 'forge-blue',
    name: 'Forge 蓝',
    description: '稳重科技风，适合默认后台和平台型系统。',
    colors: {
      primary: '#4242F7',
      headerBackground: '#4242F7',
      headerText: '#FFFFFF',
      brandTitleText: '#FFFFFF',
      topText: 'rgba(255, 255, 255, 0.78)',
      topHoverText: '#FFFFFF',
      topActiveText: '#FFFFFF',
      sideBackground: '#FFFFFF',
      sideText: '#334155',
      sideHoverText: '#4242F7',
      sideHoverBackground: '#F3F6FF',
      sideActiveText: '#4242F7',
      sideActiveBackground: '#EEF2FF',
      parentText: '#3730A3',
      parentBackground: '#F3F4FF',
      sideIcon: '#64748B',
      sideIconActive: '#4242F7',
    },
  },
  {
    key: 'enterprise-red',
    name: '企业红',
    description: '品牌感更强，适合政企、运营和门户后台。',
    colors: {
      primary: '#D12723',
      headerBackground: '#B91C1C',
      headerText: '#FFFFFF',
      brandTitleText: '#FFFFFF',
      topText: 'rgba(255, 255, 255, 0.8)',
      topHoverText: '#FFFFFF',
      topActiveText: '#FFFFFF',
      sideBackground: '#FFFFFF',
      sideText: '#3F3333',
      sideHoverText: '#B91C1C',
      sideHoverBackground: '#FEF2F2',
      sideActiveText: '#B91C1C',
      sideActiveBackground: '#FEE2E2',
      parentText: '#991B1B',
      parentBackground: '#FFF1F2',
      sideIcon: '#7F5D5D',
      sideIconActive: '#D12723',
    },
  },
  {
    key: 'mist-blue-unified',
    name: '雾蓝同色',
    description: '顶部和侧栏同色浅蓝，清爽统一，适合通用业务后台。',
    colors: {
      primary: '#2F6FED',
      headerBackground: '#EEF4FF',
      headerText: '#1E3A5F',
      brandTitleText: '#1E3A5F',
      topText: '#4E668A',
      topHoverText: '#1D4ED8',
      topActiveText: '#1D4ED8',
      sideBackground: '#EEF4FF',
      sideText: '#334155',
      sideHoverText: '#1D4ED8',
      sideHoverBackground: '#DBEAFE',
      sideActiveText: '#1D4ED8',
      sideActiveBackground: '#DCEBFF',
      parentText: '#1D4ED8',
      parentBackground: '#E4EEFF',
      sideIcon: '#64748B',
      sideIconActive: '#2F6FED',
    },
  },
  {
    key: 'mint-unified',
    name: '浅青同色',
    description: '顶部和侧栏同色浅青绿，轻量柔和，适合协作和数据管理。',
    colors: {
      primary: '#0E8F7E',
      headerBackground: '#ECFDF5',
      headerText: '#134E4A',
      brandTitleText: '#134E4A',
      topText: '#4B6863',
      topHoverText: '#0F766E',
      topActiveText: '#0F766E',
      sideBackground: '#ECFDF5',
      sideText: '#2F4F4A',
      sideHoverText: '#0F766E',
      sideHoverBackground: '#D1FAE5',
      sideActiveText: '#0F766E',
      sideActiveBackground: '#CCFBF1',
      parentText: '#0F766E',
      parentBackground: '#DFFCF1',
      sideIcon: '#5B7772',
      sideIconActive: '#0E8F7E',
    },
  },
  {
    key: 'teal-business',
    name: '松石绿',
    description: '清爽耐看，适合业务系统、数据管理和协作场景。',
    colors: {
      primary: '#0F9F8F',
      headerBackground: '#0F766E',
      headerText: '#FFFFFF',
      brandTitleText: '#FFFFFF',
      topText: 'rgba(255, 255, 255, 0.78)',
      topHoverText: '#FFFFFF',
      topActiveText: '#FFFFFF',
      sideBackground: '#FFFFFF',
      sideText: '#28413E',
      sideHoverText: '#0F766E',
      sideHoverBackground: '#ECFDF5',
      sideActiveText: '#0F766E',
      sideActiveBackground: '#CCFBF1',
      parentText: '#0F766E',
      parentBackground: '#F0FDFA',
      sideIcon: '#5B7772',
      sideIconActive: '#0F9F8F',
    },
  },
  {
    key: 'graphite',
    name: '石墨灰',
    description: '低饱和、干净克制，适合高频操作型后台。',
    colors: {
      primary: '#334155',
      headerBackground: '#1F2937',
      headerText: '#F8FAFC',
      brandTitleText: '#F8FAFC',
      topText: 'rgba(248, 250, 252, 0.76)',
      topHoverText: '#FFFFFF',
      topActiveText: '#FFFFFF',
      sideBackground: '#FFFFFF',
      sideText: '#334155',
      sideHoverText: '#1F2937',
      sideHoverBackground: '#F1F5F9',
      sideActiveText: '#1F2937',
      sideActiveBackground: '#E2E8F0',
      parentText: '#334155',
      parentBackground: '#F8FAFC',
      sideIcon: '#64748B',
      sideIconActive: '#334155',
    },
  },
]

const layoutOptionGroups = computed(() => {
  const commonValues = ['normal', 'simple', 'top-side-menu', 'top-menu', 'full']
  const commonOptions = systemLayoutOptions.filter(item => commonValues.includes(item.value))
  const advancedOptions = systemLayoutOptions.filter(item => !commonValues.includes(item.value))

  return [
    {
      key: 'common',
      title: '常用布局',
      description: '推荐给大多数租户，结构稳定、学习成本低。',
      options: commonOptions,
    },
    {
      key: 'advanced',
      title: '扩展布局',
      description: '适合大屏展示、极简导航或偏定制化的租户。',
      options: advancedOptions,
    },
  ].filter(group => group.options.length > 0)
})

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'tenantName',
    label: '租户名称',
    type: 'input',
    props: {
      placeholder: '请输入租户名称',
    },
  },
  {
    field: 'contactPerson',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人',
    },
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    type: 'input',
    props: {
      placeholder: '请输入联系电话',
    },
  },
  {
    field: 'tenantStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: tenantStatusOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'tenantName',
    label: '租户',
    minWidth: 200,
    render: row => h(SystemTableCell, {
      title: row.tenantName,
      subtitle: row.contactPerson ? `负责人：${row.contactPerson}` : '',
      interactive: true,
      avatar: true,
      tooltip: `查看租户：${row.tenantName || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'contactPhone',
    label: '联系电话',
    width: 130,
  },
  {
    prop: 'userLimit',
    label: '人员上限',
    width: 100,
    render: (row) => {
      return row.userLimit === 0 ? '无限制' : row.userLimit
    },
  },
  {
    prop: 'expireTime',
    label: '过期时间',
    width: 180,
  },
  {
    prop: 'tenantStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: NORMAL_DISABLE_DICT, value: row.tenantStatus, size: 'small' })
    },
  },
  {
    prop: 'defaultBusinessDatasourceCode',
    label: '业务数据源',
    minWidth: 140,
    render: (row) => {
      const datasource = findBusinessDatasource(row.defaultBusinessDatasourceId)
      return h(NTag, {
        size: 'small',
        type: datasource ? 'info' : 'default',
      }, {
        default: () => datasource?.datasourceName || row.defaultBusinessDatasourceCode || '主库回退',
      })
    },
  },
  {
    prop: 'tenantDesc',
    label: '描述',
    minWidth: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 160,
    fixed: 'right',
    actions: [
      { label: '用户', key: 'users', onClick: handleViewUsers },
      { label: '编辑', key: 'edit', onClick: handleEdit },
      {
        label: '删除',
        key: 'delete',
        type: 'error',
        onClick: handleDelete,
        visible: () => userStore.isAdmin,
        disabled: isDefaultTenant,
        disabledReason: '默认租户不能删除',
      },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  // ==================== 基础信息 ====================
  {
    type: 'divider',
    label: '基础信息',
    props: { titlePlacement: 'left', description: '租户的联系人、容量和有效期，优先保证业务信息完整。' },
    span: 2,
  },
  {
    field: 'tenantName',
    label: '租户名称',
    type: 'input',
    rules: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
    props: { placeholder: '请输入租户名称' },
  },
  {
    field: 'tenantStatus',
    label: '租户状态',
    type: 'radio',
    defaultValue: 1,
    props: { options: tenantStatusOptions.value },
  },
  {
    field: 'contactPerson',
    label: '负责人',
    type: 'input',
    rules: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
    props: { placeholder: '请输入负责人' },
  },
  {
    field: 'contactPhone',
    label: '联系电话',
    type: 'input',
    rules: [
      { required: true, message: '请输入联系电话', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    props: { placeholder: '请输入联系电话' },
  },
  {
    field: 'userLimit',
    label: '人员上限',
    type: 'number',
    defaultValue: 0,
    props: { placeholder: '0表示无限制', min: 0 },
  },
  {
    field: 'expireTime',
    label: '过期时间',
    type: 'datetime',
    props: { placeholder: '请选择过期时间', clearable: true },
  },
  {
    field: 'tenantDesc',
    label: '租户描述',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入租户描述', rows: 2 },
  },

  // ==================== 业务数据源 ====================
  {
    type: 'divider',
    label: '业务数据源',
    props: { titlePlacement: 'left', description: '未配置时 forge-business 模块按主库回退执行。' },
    span: 2,
  },
  {
    field: 'defaultBusinessDatasourceId',
    label: '默认业务库',
    type: 'select',
    span: 2,
    props: {
      placeholder: '未选择时回退主库',
      clearable: true,
      options: businessDatasourceOptions.value,
    },
  },

  // ==================== 品牌设置 ====================
  {
    type: 'divider',
    label: '品牌设置',
    props: { titlePlacement: 'left', description: '控制浏览器、登录页和系统左上角展示，建议先完成名称和图标。' },
    span: 2,
  },
  {
    field: 'systemName',
    label: '系统名称',
    type: 'input',
    props: { placeholder: '显示在系统左上角' },
  },
  {
    field: 'browserTitle',
    label: '浏览器标签',
    type: 'input',
    props: { placeholder: '浏览器标签页显示的名称' },
  },
  {
    field: 'systemLogo',
    label: '系统Logo',
    type: 'imageUpload',
    businessType: 'tenant-logo',
    limit: 1,
    fileSize: 2,
    valueType: 'string',
    props: { showTip: true },
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
    props: { showTip: true },
  },
  {
    field: 'systemIntro',
    label: '系统介绍',
    type: 'textarea',
    span: 2,
    props: { placeholder: '登录页显示的系统介绍', rows: 2 },
  },
  {
    field: 'copyrightInfo',
    label: '版权信息',
    type: 'textarea',
    span: 2,
    props: { placeholder: '页面底部显示的版权信息', rows: 2 },
  },

  // ==================== 外观方案 ====================
  {
    type: 'divider',
    label: '外观方案',
    props: { titlePlacement: 'left', description: '先确定租户默认布局和品牌主色，后面的导航配色只在需要深度定制时调整。', badge: '推荐' },
    span: 2,
  },
  {
    field: 'systemLayout',
    label: '默认布局',
    type: 'slot',
    slotName: 'systemLayout',
    span: 2,
  },
  {
    field: 'systemTheme',
    label: '主题色',
    type: 'color',
    defaultValue: '#d12723',
    props: { showAlpha: false },
  },
  {
    field: 'themePreset',
    label: '配色推荐',
    type: 'slot',
    slotName: 'themePreset',
    span: 2,
  },

  // ==================== 顶栏外观 ====================
  {
    type: 'divider',
    label: '顶栏外观',
    props: { titlePlacement: 'left', dashed: true, description: '控制页面顶部 Header 和顶部菜单。顶部菜单选中态使用下划线，不配置选中背景。', badge: '可选' },
    span: 2,
  },
  {
    field: 'theme_header_backgroundColor',
    label: 'Header背景',
    type: 'color',
    defaultValue: '#4242F7',
    gridClass: 'tenant-config-field tenant-config-field--header',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_header_textColor',
    label: 'Header文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    gridClass: 'tenant-config-field tenant-config-field--header',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_brand_titleTextColor',
    label: '品牌标题文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    gridClass: 'tenant-config-field tenant-config-field--header',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_header_height',
    label: 'Header高度',
    type: 'input',
    defaultValue: '60px',
    gridClass: 'tenant-config-field tenant-config-field--header tenant-config-field--size',
    props: { placeholder: '例如: 60px' },
  },
  {
    field: 'theme_topMenu_textColor',
    label: '顶部菜单文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    gridClass: 'tenant-config-field tenant-config-field--top',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_topMenu_textColorHover',
    label: '顶部悬停文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    gridClass: 'tenant-config-field tenant-config-field--top',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_topMenu_textColorActive',
    label: '顶部选中文字',
    type: 'color',
    defaultValue: '#FFFFFF',
    gridClass: 'tenant-config-field tenant-config-field--top',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_topMenu_iconColor',
    label: '顶部图标',
    type: 'color',
    defaultValue: '#ffffff',
    gridClass: 'tenant-config-field tenant-config-field--top',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_topMenu_iconActiveColor',
    label: '顶部选中图标',
    type: 'color',
    defaultValue: '#ffffff',
    gridClass: 'tenant-config-field tenant-config-field--top',
    props: { showAlpha: false, modes: ['hex'] },
  },

  // ==================== 侧边导航 ====================
  {
    type: 'divider',
    label: '侧边导航',
    props: { titlePlacement: 'left', dashed: true, description: '控制左侧菜单的默认、悬停和选中状态，适用于简约、通用、全屏、顶部+侧面布局。' },
    span: 2,
  },
  {
    field: 'theme_sideMenu_backgroundColor',
    label: '侧边栏背景',
    type: 'color',
    defaultValue: '#ffffff',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_textColor',
    label: '侧边栏文字',
    type: 'color',
    defaultValue: '#333333',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_textColorActive',
    label: '选中文字色',
    type: 'color',
    defaultValue: '#316cfa',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_parentTextColorActive',
    label: '父级提示文字',
    type: 'color',
    defaultValue: '#1d4ed8',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_parentBackgroundColorActive',
    label: '父级提示背景',
    type: 'color',
    defaultValue: '#eef4ff',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_textColorHover',
    label: '菜单悬停色',
    type: 'color',
    defaultValue: '#316cfa',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_backgroundColorHover',
    label: '悬停背景色',
    type: 'color',
    defaultValue: '#f5f5f5',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_backgroundColorActive',
    label: '菜单选中背景',
    type: 'color',
    defaultValue: '#f6eded',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_iconColor',
    label: '侧边栏图标',
    type: 'color',
    defaultValue: '#666666',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_iconColorActive',
    label: '选中图标色',
    type: 'color',
    defaultValue: '#4242F7',
    gridClass: 'tenant-config-field tenant-config-field--side',
    props: { showAlpha: false, modes: ['hex'] },
  },
  {
    field: 'theme_sideMenu_width',
    label: '菜单宽度',
    type: 'input',
    defaultValue: '220px',
    gridClass: 'tenant-config-field tenant-config-field--side tenant-config-field--size',
    props: { placeholder: '例如: 220px' },
  },
  {
    field: 'theme_sideMenu_collapsedWidth',
    label: '折叠宽度',
    type: 'input',
    defaultValue: '64px',
    gridClass: 'tenant-config-field tenant-config-field--side tenant-config-field--size',
    props: { placeholder: '例如: 64px' },
  },
])

const userTableColumns = [
  {
    title: '用户名',
    key: 'username',
    width: 150,
  },
  {
    title: '真实姓名',
    key: 'realName',
    width: 120,
  },
  {
    title: '用户类型',
    key: 'userType',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: USER_TYPE_DICT, value: row.userType, size: 'small' })
    },
  },
  {
    title: '手机号',
    key: 'phone',
    width: 130,
  },
  {
    title: '邮箱',
    key: 'email',
    width: 180,
  },
  {
    title: '状态',
    key: 'userStatus',
    width: 90,
    render: (row) => {
      return h(DictTag, { dictType: USER_STATUS_DICT, value: row.userStatus, size: 'small' })
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 170,
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) => {
      if (isCurrentTenantUser(row)) {
        return h('span', { class: 'text-disabled cursor-not-allowed' }, '当前登录用户')
      }
      return h(
        'a',
        {
          class: 'text-error cursor-pointer hover:text-error-hover',
          onClick: () => handleRemoveTenantUser(row),
        },
        '移出租户',
      )
    },
  },
]

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function findBusinessDatasource(datasourceId) {
  if (datasourceId === null || datasourceId === undefined)
    return null
  return businessDatasources.value.find(item => String(item.datasourceId) === String(datasourceId)) || null
}

function applyTenantThemePreset(preset, formData, updateValue) {
  const colors = preset?.colors
  if (!colors || !formData)
    return

  Object.assign(formData, {
    systemTheme: colors.primary,
    theme_header_backgroundColor: colors.headerBackground,
    theme_header_textColor: colors.headerText,
    theme_brand_titleTextColor: colors.brandTitleText || colors.headerText,
    theme_topMenu_textColor: colors.topText,
    theme_topMenu_textColorHover: colors.topHoverText || colors.topActiveText || colors.topText,
    theme_topMenu_textColorActive: colors.topActiveText,
    theme_topMenu_iconColor: colors.topText,
    theme_topMenu_iconActiveColor: colors.topActiveText,
    theme_sideMenu_backgroundColor: colors.sideBackground,
    theme_sideMenu_textColor: colors.sideText,
    theme_sideMenu_textColorHover: colors.sideHoverText,
    theme_sideMenu_backgroundColorHover: colors.sideHoverBackground,
    theme_sideMenu_textColorActive: colors.sideActiveText,
    theme_sideMenu_backgroundColorActive: colors.sideActiveBackground,
    theme_sideMenu_parentTextColorActive: colors.parentText,
    theme_sideMenu_parentBackgroundColorActive: colors.parentBackground,
    theme_sideMenu_iconColor: colors.sideIcon,
    theme_sideMenu_iconColorActive: colors.sideIconActive,
  })

  updateValue?.(preset.key)
}

async function loadBusinessDatasources() {
  try {
    const res = await request.get('/generator/datasource/enabled', {
      params: { usageScope: 'TENANT_BUSINESS' },
    })
    businessDatasources.value = res.data || []
  }
  catch (error) {
    console.error('加载租户业务数据源失败:', error)
    window.$message?.warning?.('租户业务数据源加载失败')
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

function isDefaultTenant(row) {
  return Number(row?.id) === DEFAULT_TENANT_ID
}

function handleBeforeDelete(rows = []) {
  if (rows.some(isDefaultTenant)) {
    window.$message.warning('默认租户不能删除')
    return false
  }
  return true
}

// 删除
function handleDelete(row) {
  if (isDefaultTenant(row)) {
    window.$message.warning('默认租户不能删除')
    return
  }
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
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}

function isCurrentTenantUser(row) {
  return String(row?.id) === String(userStore.userId)
    && String(currentTenant.value?.id) === String(userStore.userInfo?.tenantId)
}

function handleRemoveTenantUser(row) {
  if (!currentTenant.value?.id || !row?.id)
    return
  window.$dialog.warning({
    title: '确认移除',
    content: `确定将用户"${row.username}"移出租户"${currentTenant.value.tenantName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post(`/system/tenant/${currentTenant.value.id}/users/${row.id}/remove`)
        if (res.code === 200) {
          window.$message.success('用户已移出租户')
          await loadTenantUsers()
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        console.error('移出租户用户失败:', error)
        window.$message.error(error?.message || '移出租户用户失败')
      }
    },
  })
}

async function handleViewUsers(row) {
  currentTenant.value = row
  usersModalVisible.value = true
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  await loadTenantUsers()
}

async function loadTenantUsers() {
  if (!currentTenant.value?.id)
    return
  try {
    usersLoading.value = true
    const params = {
      ...userSearchParams.value,
      pageNum: userPagination.value.page,
      pageSize: userPagination.value.pageSize,
    }
    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    const res = await request.get(`/system/tenant/${currentTenant.value.id}/users`, { params })
    if (res.code === 200) {
      tenantUsers.value = res.data?.records || []
      userPagination.value.itemCount = res.data?.total || 0
    }
  }
  catch (error) {
    console.error('加载租户用户失败:', error)
    window.$message.error('加载租户用户失败')
  }
  finally {
    usersLoading.value = false
  }
}

function handleUserSearch() {
  userPagination.value.page = 1
  loadTenantUsers()
}

function handleUserSearchReset() {
  userSearchParams.value = {
    username: '',
    realName: '',
    phone: '',
    userStatus: null,
  }
  userPagination.value.page = 1
  loadTenantUsers()
}

function handleUserPageChange(page) {
  userPagination.value.page = page
  loadTenantUsers()
}

function handleUserPageSizeChange(pageSize) {
  userPagination.value.pageSize = pageSize
  userPagination.value.page = 1
  loadTenantUsers()
}

// 提交成功后处理 - 重新加载租户配置并应用
async function handleSubmitSuccess() {
  // 重新加载租户配置
  const { useTenantStore, useUserStore, useAppStore } = await import('@/store')
  const tenantStore = useTenantStore()
  const userStore = useUserStore()
  const appStore = useAppStore()

  // 获取最新的租户配置
  const tenantConfig = await tenantStore.loadTenantConfig(userStore.userInfo?.tenantId)

  if (tenantConfig) {
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
        primaryColor,
        header: {
          ...defaultThemeConfig.header,
          ...themeConfigObj.header,
        },
        headerDark: {
          ...defaultThemeConfig.headerDark,
          ...themeConfigObj.headerDark,
        },
        topMenu: {
          ...defaultThemeConfig.topMenu,
          ...themeConfigObj.topMenu,
        },
        topMenuDark: {
          ...defaultThemeConfig.topMenuDark,
          ...themeConfigObj.topMenuDark,
        },
        sideMenu: {
          ...defaultThemeConfig.sideMenu,
          ...themeConfigObj.sideMenu,
        },
        sideMenuDark: {
          ...defaultThemeConfig.sideMenuDark,
          ...themeConfigObj.sideMenuDark,
        },
      }

      appStore.setThemeConfig(mergedConfig)
    }

    // 应用浏览器标题。未单独配置浏览器标题时，使用系统名称作为网页基础标题。
    const pageBaseTitle = tenantConfig.browserTitle || tenantConfig.systemName
    if (pageBaseTitle) {
      document.title = pageBaseTitle
    }

    // 应用浏览器图标
    const iconUrl = resolveTenantPublicAssetUrl(tenantConfig, 'icon')
    setDocumentFavicon(iconUrl || tenantConfig.browserIcon)

    window.$message.success('主题配置已更新')
  }
}

// 提交前处理 - 将主题配置字段组装成 JSON
function handleBeforeSubmit(formData) {
  const businessDatasource = findBusinessDatasource(formData.defaultBusinessDatasourceId)
  if (businessDatasource) {
    formData.defaultBusinessDatasourceCode = businessDatasource.datasourceCode || null
  }
  else {
    formData.defaultBusinessDatasourceId = null
    formData.defaultBusinessDatasourceCode = null
  }

  // 检查是否有任何主题配置字段
  const hasThemeConfig = formData.theme_header_backgroundColor
    || formData.theme_header_textColor
    || formData.theme_brand_titleTextColor
    || formData.theme_header_height
    || formData.theme_topMenu_textColor
    || formData.theme_topMenu_textColorHover
    || formData.theme_topMenu_textColorActive
    || formData.theme_topMenu_iconColor
    || formData.theme_topMenu_iconActiveColor
    || formData.theme_sideMenu_backgroundColor
    || formData.theme_sideMenu_textColor
    || formData.theme_sideMenu_textColorActive
    || formData.theme_sideMenu_parentTextColorActive
    || formData.theme_sideMenu_parentBackgroundColorActive
    || formData.theme_sideMenu_textColorHover
    || formData.theme_sideMenu_backgroundColorHover
    || formData.theme_sideMenu_backgroundColorActive
    || formData.theme_sideMenu_iconColor
    || formData.theme_sideMenu_iconColorActive
    || formData.theme_sideMenu_width
    || formData.theme_sideMenu_collapsedWidth

  // 只有配置了主题字段时才组装 themeConfig
  if (hasThemeConfig) {
    const themeConfig = {
      primaryColor: formData.systemTheme || formData.theme_header_backgroundColor || '#4242F7',
      header: {
        backgroundColor: formData.theme_header_backgroundColor || '#4242F7',
        textColor: formData.theme_header_textColor || '#FFFFFF',
        brandTitleTextColor: formData.theme_brand_titleTextColor || formData.theme_header_textColor || '#FFFFFF',
        height: formData.theme_header_height || '60px',
      },
      topMenu: {
        textColor: formData.theme_topMenu_textColor || '#FFFFFF',
        textColorHover: formData.theme_topMenu_textColorHover || formData.theme_topMenu_textColorActive || formData.theme_topMenu_textColor || '#FFFFFF',
        textColorActive: formData.theme_topMenu_textColorActive || '#FFFFFF',
        backgroundColorHover: 'transparent',
        backgroundColorActive: 'transparent',
        iconColor: formData.theme_topMenu_iconColor || '#ffffff',
        iconActiveColor: formData.theme_topMenu_iconActiveColor || '#ffffff',
      },
      sideMenu: {
        backgroundColor: formData.theme_sideMenu_backgroundColor || '#ffffff',
        textColor: formData.theme_sideMenu_textColor || '#333333',
        textColorHover: formData.theme_sideMenu_textColorHover || '#316cfa',
        textColorActive: formData.theme_sideMenu_textColorActive || '#316cfa',
        parentTextColorActive: formData.theme_sideMenu_parentTextColorActive || '#1d4ed8',
        backgroundColorHover: formData.theme_sideMenu_backgroundColorHover || '#f5f5f5',
        backgroundColorActive: formData.theme_sideMenu_backgroundColorActive || '#f6eded',
        parentBackgroundColorActive: formData.theme_sideMenu_parentBackgroundColorActive || '#eef4ff',
        iconColor: formData.theme_sideMenu_iconColor || '#666666',
        iconColorActive: formData.theme_sideMenu_iconColorActive || '#4242F7',
        width: formData.theme_sideMenu_width || '220px',
        collapsedWidth: formData.theme_sideMenu_collapsedWidth || '64px',
      },
    }

    // 将主题配置转换为 JSON 字符串
    formData.themeConfig = JSON.stringify(themeConfig)
  }

  // 删除临时字段
  delete formData.theme_header_backgroundColor
  delete formData.theme_header_textColor
  delete formData.theme_brand_titleTextColor
  delete formData.theme_header_height
  delete formData.themePreset
  delete formData.theme_topMenu_textColor
  delete formData.theme_topMenu_textColorHover
  delete formData.theme_topMenu_textColorActive
  delete formData.theme_topMenu_iconColor
  delete formData.theme_topMenu_iconActiveColor
  delete formData.theme_sideMenu_backgroundColor
  delete formData.theme_sideMenu_textColor
  delete formData.theme_sideMenu_textColorActive
  delete formData.theme_sideMenu_parentTextColorActive
  delete formData.theme_sideMenu_parentBackgroundColorActive
  delete formData.theme_sideMenu_textColorHover
  delete formData.theme_sideMenu_backgroundColorHover
  delete formData.theme_sideMenu_backgroundColorActive
  delete formData.theme_sideMenu_iconColor
  delete formData.theme_sideMenu_iconColorActive
  delete formData.theme_sideMenu_width
  delete formData.theme_sideMenu_collapsedWidth

  return formData
}

// 详情渲染前处理 - 将 JSON 拆解到各个字段
function handleBeforeRenderDetail(data) {
  if (data.themeConfig) {
    try {
      const themeConfig = typeof data.themeConfig === 'string'
        ? JSON.parse(data.themeConfig)
        : data.themeConfig
      // 拆解到各个字段
      data.systemTheme = themeConfig.primaryColor // 主题颜色
      data.theme_header_backgroundColor = themeConfig.header?.backgroundColor || themeConfig.primaryColor
      data.theme_header_textColor = themeConfig.header?.textColor
      data.theme_brand_titleTextColor = themeConfig.header?.brandTitleTextColor || themeConfig.header?.textColor
      data.theme_header_height = themeConfig.header?.height
      data.theme_topMenu_textColor = themeConfig.topMenu?.textColor
      data.theme_topMenu_textColorHover = themeConfig.topMenu?.textColorHover
      data.theme_topMenu_textColorActive = themeConfig.topMenu?.textColorActive
      data.theme_topMenu_iconColor = themeConfig.topMenu?.iconColor
      data.theme_topMenu_iconActiveColor = themeConfig.topMenu?.iconActiveColor
      data.theme_sideMenu_backgroundColor = themeConfig.sideMenu?.backgroundColor
      data.theme_sideMenu_textColor = themeConfig.sideMenu?.textColor
      data.theme_sideMenu_textColorHover = themeConfig.sideMenu?.textColorHover
      data.theme_sideMenu_textColorActive = themeConfig.sideMenu?.textColorActive
      data.theme_sideMenu_parentTextColorActive = themeConfig.sideMenu?.parentTextColorActive
      data.theme_sideMenu_parentBackgroundColorActive = themeConfig.sideMenu?.parentBackgroundColorActive
      data.theme_sideMenu_backgroundColorHover = themeConfig.sideMenu?.backgroundColorHover
      data.theme_sideMenu_backgroundColorActive = themeConfig.sideMenu?.backgroundColorActive
      data.theme_sideMenu_iconColor = themeConfig.sideMenu?.iconColor
      data.theme_sideMenu_iconColorActive = themeConfig.sideMenu?.iconColorActive
      data.theme_sideMenu_width = themeConfig.sideMenu?.width
      data.theme_sideMenu_collapsedWidth = themeConfig.sideMenu?.collapsedWidth
    }
    catch (error) {
      console.error('解析主题配置失败:', error)
    }
  }

  return data
}

onMounted(() => {
  loadBusinessDatasources()
})
</script>

<style scoped>
.system-tenant-page {
  height: 100%;
}

.tenant-users-modal {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 420px;
}

.tenant-users-search {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.tenant-users-toolbar {
  padding: 0 2px;
}

:deep(.tenant-config-form) {
  padding: 2px 2px 0;
}

:deep(.tenant-config-form .ai-form-section-title) {
  margin-top: 4px;
}

:deep(.tenant-config-form .tenant-config-field) {
  position: relative;
  padding: 10px 12px 8px;
  border: 1px solid #e5edf8;
  border-radius: 8px;
  background: linear-gradient(180deg, rgb(255 255 255 / 92%), rgb(248 250 252 / 88%)), #f8fafc;
}

:deep(.tenant-config-form .tenant-config-field::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 999px 999px 0;
}

:deep(.tenant-config-form .tenant-config-field--header::before) {
  background: linear-gradient(180deg, #60a5fa, #38bdf8);
}

:deep(.tenant-config-form .tenant-config-field--side::before) {
  background: linear-gradient(180deg, #22c55e, #14b8a6);
}

:deep(.tenant-config-form .tenant-config-field--top::before) {
  background: linear-gradient(180deg, #f59e0b, #fb7185);
}

:deep(.tenant-config-form .tenant-config-field .n-form-item) {
  --n-label-padding: 0 0 4px 0 !important;
}

:deep(.tenant-config-form .tenant-config-field .n-form-item-label) {
  min-height: 22px !important;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

:deep(.tenant-config-form .tenant-config-field .n-form-item-blank) {
  min-height: 32px;
}

:deep(.tenant-config-form .tenant-config-field .n-color-picker-trigger),
:deep(.tenant-config-form .tenant-config-field .n-input) {
  border-radius: 6px;
}

:deep(.tenant-config-form .tenant-config-field--size) {
  background: linear-gradient(180deg, rgb(255 255 255 / 94%), rgb(241 245 249 / 90%)), #f1f5f9;
}

:deep(.tenant-config-form .tenant-config-field--size .n-input .n-input__input-el) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}

.layout-selector {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.layout-group {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}

.layout-group-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.layout-group-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.layout-group-desc {
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  text-align: right;
}

.layout-group-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.layout-option {
  position: relative;
  min-height: 82px;
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #334155;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.layout-option:hover {
  border-color: #93c5fd;
  background: #fff;
  box-shadow: 0 8px 18px rgb(37 99 235 / 8%);
  transform: translateY(-1px);
}

.layout-option.active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff, #ffffff);
  box-shadow: inset 0 0 0 1px rgb(37 99 235 / 12%);
}

.layout-preview-card {
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.layout-img {
  width: 64px;
  height: 40px;
  object-fit: contain;
}

.layout-option-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.layout-option-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.layout-option-badge {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 11px;
  font-weight: 700;
}

.layout-option-desc {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.layout-option-check {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #fff;
  background: #2563eb;
  font-size: 16px;
}

.theme-preset-selector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.theme-preset-option {
  position: relative;
  min-height: 70px;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff, #f8fafc);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.theme-preset-option:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgb(37 99 235 / 8%);
  transform: translateY(-1px);
}

.theme-preset-option.active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff, #fff);
  box-shadow: inset 0 0 0 1px rgb(37 99 235 / 12%);
}

.theme-preset-swatches {
  height: 46px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.theme-preset-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.theme-preset-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.theme-preset-desc {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.theme-preset-check {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #fff;
  background: #2563eb;
  font-size: 16px;
}

@media (max-width: 760px) {
  .layout-group-header {
    display: block;
  }

  .layout-group-desc {
    display: block;
    margin-top: 3px;
    text-align: left;
  }

  .layout-group-grid {
    grid-template-columns: 1fr;
  }

  .theme-preset-selector {
    grid-template-columns: 1fr;
  }
}
</style>

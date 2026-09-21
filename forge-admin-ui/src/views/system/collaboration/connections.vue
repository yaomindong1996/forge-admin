<template>
  <div class="collaboration-console-page">
    <div class="console-shell">
      <!-- 控制台页头：标题 + 配置指南入口 -->
      <div class="console-header">
        <div class="console-header__main">
          <h2 class="console-header__title">
            企业协同
          </h2>
          <p class="console-header__subtitle">
            对接企业微信、钉钉、飞书等平台，统一管理连接配置、通讯录同步与消息投递
          </p>
        </div>
        <n-button size="small" tertiary @click="guideVisible = true">
          <template #icon>
            <span class="console-guide-icon">?</span>
          </template>
          配置指南
        </n-button>
      </div>

      <!-- 全功能页签：连接配置 + 同步/投递/回调全链路运维，按权限控制可见性 -->
      <n-tabs v-model:value="activeTab" type="line" class="console-tabs" pane-class="console-tab-pane">
        <n-tab-pane name="connections" tab="连接管理" display-directive="show">
          <AiCrudPage
            ref="crudRef"
            api="/system/collaboration/connections"
            :api-config="{
              list: 'get@/system/collaboration/connections/page',
              add: 'post@/system/collaboration/connections',
              update: 'put@/system/collaboration/connections',
              delete: 'delete@/system/collaboration/connections/:id',
            }"
            :search-schema="searchSchema"
            :columns="tableColumns"
            :edit-schema="editSchema"
            row-key="id"
            add-button-text="新建连接"
            :edit-grid-cols="2"
            modal-width="900px"
            :hide-batch-delete="true"
            :hide-selection="true"
            :before-render-detail="handleBeforeRenderDetail"
            :before-submit="handleBeforeSubmit"
          />
        </n-tab-pane>
        <n-tab-pane v-if="canViewTab('system:collaboration:sync:view')" name="sync" tab="同步批次" display-directive="show:lazy">
          <SyncPanel />
        </n-tab-pane>
        <n-tab-pane v-if="canViewTab('system:collaboration:issue:view')" name="issues" tab="问题单" display-directive="show:lazy">
          <IssuesPanel />
        </n-tab-pane>
        <n-tab-pane v-if="canViewTab('system:collaboration:mapping:view')" name="mappings" tab="映射查询" display-directive="show:lazy">
          <MappingsPanel />
        </n-tab-pane>
        <n-tab-pane v-if="canViewTab('system:collaboration:delivery:view')" name="deliveries" tab="投递记录" display-directive="show:lazy">
          <DeliveriesPanel />
        </n-tab-pane>
        <n-tab-pane v-if="canViewTab('system:collaboration:callback:view')" name="callbackEvents" tab="回调事件" display-directive="show:lazy">
          <CallbackEventsPanel />
        </n-tab-pane>
      </n-tabs>
    </div>

    <ConnectionGuidePanel v-model:show="guideVisible" />

    <ConnectionSetupPanel
      v-model:show="detailVisible"
      :connection-id="currentConnectionId"
      :manage-mode="manageMode"
      :can-manage="canManageConnection"
      @saved="crudRef?.refresh?.()"
      @edit="handleEditFromSetup"
    />

    <ConnectionTestPanel
      v-model:test-visible="testModalVisible"
      v-model:message-visible="msgTestVisible"
      :test-connection-id="testConnectionId"
      :message-connection-id="msgTestConnectionId"
      :capability-options="capabilityOptions"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { useRoute } from 'vue-router'
import { triggerConnectionSync, updateConnection } from '@/api/collaboration'
import { AiCrudPage } from '@/components/ai-form'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { request } from '@/utils'
import CallbackEventsPanel from './callback-events.vue'
import ConnectionGuidePanel from './components/ConnectionGuidePanel.vue'
import ConnectionSetupPanel from './components/ConnectionSetupPanel.vue'
import ConnectionStatusBadge from './components/ConnectionStatusBadge.vue'
import ConnectionTestPanel from './components/ConnectionTestPanel.vue'
import DeliveriesPanel from './deliveries.vue'
import IssuesPanel from './issues.vue'
import MappingsPanel from './mappings.vue'
import SyncPanel from './sync.vue'

defineOptions({ name: 'CollaborationConnections' })

/**
 * 企业型平台：具备企业ID、通讯录同步与应用消息能力。
 * 其余平台（Gitee/GitHub/QQ 等）仅支持 OAuth 登录，不具备企业ID/通讯录/消息投递概念。
 */
const ENTERPRISE_PLATFORMS = new Set(['WECHAT_ENTERPRISE', 'DINGTALK', 'DINGTALK_ACCOUNT', 'FEISHU'])
const WECOM_API_BASE_URL = 'https://qyapi.weixin.qq.com'

function isEnterprisePlatform(platform) {
  return ENTERPRISE_PLATFORMS.has(platform)
}

const crudRef = ref(null)
const route = useRoute()
const userStore = useUserStore()
const detailVisible = ref(false)
const currentConnectionId = ref(null)
const manageMode = ref(false)

function handleManage(row) {
  manageMode.value = true
  currentConnectionId.value = row.id
  detailVisible.value = true
}

function handleEditFromSetup(row) {
  if (row)
    crudRef.value?.showEdit(row)
}

// 控制台页签与配置指南
const activeTab = ref('connections')
const guideVisible = ref(false)

// 超管直接放行；普通用户按登录权限 + 当前路由按钮编码判断（v-permission 指令无超管旁路，不适用弹窗内按钮）
const grantedPerms = computed(() => {
  const routeBtns = (route.meta?.btns || []).map(item => item.code)
  return new Set([...(userStore.permissions || []), ...routeBtns])
})

const canManageConnection = computed(() => {
  if (userStore.isAdmin)
    return true
  return grantedPerms.value.has('system:collaboration:connection:update')
    || grantedPerms.value.has('**')
    || grantedPerms.value.has('*:*:*')
})

// 运维页签可见性：原独立菜单的 view 权限已降级为本菜单下的按钮资源
function canViewTab(perm) {
  if (userStore.isAdmin)
    return true
  return grantedPerms.value.has(perm)
    || grantedPerms.value.has('**')
    || grantedPerms.value.has('*:*:*')
}

const { dict, getLabel } = useDict(
  'sys_collab_platform',
  'sys_collab_capability',
  'sys_collab_identity_policy',
  'sys_collab_directory_authority',
  'sys_collab_connection_type',
  'sys_normal_disable',
)

const platformOptions = computed(() => dict.value.sys_collab_platform || [])
const identityPolicyOptions = computed(() => dict.value.sys_collab_identity_policy || [])
const directoryAuthorityOptions = computed(() => dict.value.sys_collab_directory_authority || [])
const connectionTypeOptions = computed(() => dict.value.sys_collab_connection_type || [])
const statusOptions = computed(() => dict.value.sys_normal_disable || [])
// 一期只放开 LOGIN/DIRECTORY/MESSAGE，TODO 待办联动二期开放
const capabilityOptions = computed(() =>
  (dict.value.sys_collab_capability || []).filter(item => item.value !== 'TODO'),
)

// ==================== 角色选项（默认角色配置用） ====================
const roleOptions = ref([])

async function loadRoleOptions() {
  try {
    const res = await request.get('/system/role/page', {
      params: { pageNum: 1, pageSize: 200 },
    })
    if (res.code === 200) {
      roleOptions.value = (res.data?.records || []).map(item => ({
        label: item.roleName,
        value: item.id,
      }))
    }
  }
  catch (e) {
    console.warn('加载角色列表失败:', e)
  }
}
loadRoleOptions()

// ==================== 主表 ====================

const searchSchema = computed(() => [
  {
    field: 'platform',
    label: '平台',
    type: 'select',
    props: { placeholder: '请选择平台', options: platformOptions.value, clearable: true },
  },
  {
    field: 'connectionName',
    label: '连接名称',
    type: 'input',
    props: { placeholder: '请输入连接名称' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择', options: statusOptions.value, clearable: true },
  },
])

const LOGO_STYLE = 'width:28px;height:28px;border-radius:6px;object-fit:contain;flex:0 0 auto;background:#f8fafc;border:1px solid #e2e8f0'
const LOGO_PLACEHOLDER_STYLE = 'width:28px;height:28px;border-radius:6px;flex:0 0 auto;display:flex;align-items:center;justify-content:center;background:#eff6ff;color:#2563eb;font-size:13px;font-weight:600'

const tableColumns = computed(() => [
  {
    prop: 'connectionName',
    label: '连接',
    minWidth: 280,
    render: row => h('div', { style: 'display:flex;align-items:center;gap:8px' }, [
      row.platformLogo
        ? h(AuthImage, { src: row.platformLogo, imgStyle: LOGO_STYLE, lazy: false })
        : h('div', { style: LOGO_PLACEHOLDER_STYLE }, (row.platformName || row.platform || '?').slice(0, 1)),
      h('div', { style: 'min-width:0;line-height:1.4' }, [
        h('a', {
          class: 'text-primary cursor-pointer hover:text-primary-hover',
          style: 'display:block;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap',
          title: `配置连接：${row.connectionName || row.connectionCode || '-'}`,
          onClick: () => handleManage(row),
        }, row.connectionName || '-'),
        h('div', { style: 'font-size:12px;color:#94a3b8;overflow:hidden;text-overflow:ellipsis;white-space:nowrap' }, [
          h('span', null, getLabel('sys_collab_platform', row.platform) || row.platform || '-'),
          h('span', { style: 'margin:0 5px;color:#cbd5e1' }, '·'),
          h('span', { style: 'font-family:ui-monospace,SFMono-Regular,Menlo,monospace' }, row.connectionCode || '自动生成'),
        ]),
      ]),
    ]),
  },
  {
    prop: 'purpose',
    label: '主要用途',
    width: 180,
    render: row => h('span', { style: 'color:#475569;font-size:13px' }, connectionPurpose(row)),
  },
  {
    prop: 'configurationStatus',
    label: '配置状态',
    width: 130,
    render: row => h(ConnectionStatusBadge, { row }),
  },
  {
    prop: 'status',
    label: '运行状态',
    width: 80,
    render: row => h(DictTag, { dictType: 'sys_normal_disable', value: String(row.status ?? ''), size: 'small' }),
  },
  { prop: 'updateTime', label: '最近更新', width: 160 },
  {
    prop: 'action',
    label: '操作',
    width: 180,
    fixed: 'right',
    maxActionButtons: 2,
    actions: [
      { label: '配置', key: 'manage', type: 'primary', onClick: handleManage },
      {
        label: '测试',
        key: 'test',
        type: 'primary',
        // 连通测试依赖 AccessToken 换取，纯 OAuth 登录平台无该能力
        visible: row => isEnterprisePlatform(row.platform),
        onClick: handleOpenTest,
      },
      {
        label: '手动同步',
        key: 'sync',
        type: 'primary',
        visible: row => isEnterprisePlatform(row.platform) && row.directoryAuthority === 'EXTERNAL',
        onClick: handleTriggerSync,
      },
      {
        label: '消息测试',
        key: 'msgTest',
        type: 'info',
        // 消息推送走应用 Token，仅企业型平台具备该能力
        visible: row => isEnterprisePlatform(row.platform),
        onClick: handleOpenMsgTest,
      },
      // 显式启用/停用入口：按当前状态互斥展示
      {
        label: '停用',
        key: 'disable',
        type: 'warning',
        visible: row => Number(row.status) === 1,
        onClick: handleToggleStatus,
      },
      {
        label: '启用',
        key: 'enable',
        type: 'success',
        visible: row => Number(row.status) === 0,
        onClick: handleToggleStatus,
      },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

function connectionPurpose(row) {
  if (!isEnterprisePlatform(row.platform))
    return '扫码登录'
  const purposes = ['扫码登录']
  if (row.directoryAuthority === 'EXTERNAL')
    purposes.push('通讯录同步')
  if (Number(row.todoPushEnabled) === 1)
    purposes.push('待办推送')
  return purposes.join('、')
}

const editSchema = computed(() => [
  { type: 'divider', label: '基础信息', props: { titlePlacement: 'left' }, span: 2 },
  {
    field: 'platform',
    label: '平台',
    type: 'select',
    labelTip: `选择要对接的第三方平台。
企业微信/钉钉/飞书属于企业型平台，支持通讯录同步和消息推送；
Gitee/GitHub 等仅支持扫码登录。`,
    rules: [{ required: true, message: '请选择平台', trigger: 'change' }],
    props: { options: platformOptions.value, clearable: false },
  },
  {
    field: 'platformName',
    label: '平台名称',
    type: 'input',
    labelTip: '展示给用户看的名称，会出现在登录页扫码入口和系统内平台标识上，如「企业微信」。',
    rules: [{ required: true, message: '请输入平台显示名称', trigger: 'blur' }],
    props: { placeholder: '展示给用户的名称，如：企业微信' },
  },
  {
    field: 'platformLogo',
    label: '平台Logo',
    type: 'imageUpload',
    span: 2,
    businessType: 'platform-logo',
    limit: 1,
    fileSize: 2,
    valueType: 'string',
    labelTip: `上传平台Logo图片，用于登录页扫码入口和连接列表展示。
留空时列表以平台名首字占位展示。`,
    props: { showTip: true },
  },
  {
    field: 'connectionCode',
    label: '连接标识',
    type: 'input',
    labelTip: `连接的全局唯一标识，用于拼接免登链接和事件回调地址，创建后不建议修改。
留空将按「平台-随机后缀」自动生成，如 wecom-a1b2。`,
    disabled: ({ formData }) => Boolean(formData?.id),
    props: { placeholder: '新建时留空自动生成，如 wecom-a1b2' },
  },
  {
    field: 'connectionName',
    label: '连接名称',
    type: 'input',
    labelTip: '区分不同连接的内部名称，建议包含公司/组织名，如「XX科技企业微信」。',
    rules: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
    props: { placeholder: '如：XX科技企业微信' },
  },
  {
    field: 'enterpriseId',
    label: '企业标识',
    type: 'input',
    vIf: formData => isEnterprisePlatform(formData.platform),
    labelTip: `外部平台分配给企业的唯一标识，企业型平台必填。获取位置：
・企业微信：管理后台 → 我的企业 → 企业信息 → 企业ID
・钉钉：开放平台首页右上角 CorpId
・飞书：管理后台 → 设置 → 企业信息
・Gitee/GitHub 等纯登录平台可留空`,
    props: { placeholder: '企业微信 CorpID、钉钉 CorpId 或飞书企业 ID' },
  },
  {
    field: 'connectionType',
    label: '接入方式',
    type: 'select',
    defaultValue: 'CORP_INTERNAL',
    vIf: formData => isEnterprisePlatform(formData.platform),
    labelTip: `・自建应用：企业在平台管理后台自己创建的应用（最常见）
・第三方应用：通过服务商市场安装的应用
・仅OAuth登录：只用扫码登录，不涉及通讯录和消息
不确定时选「自建应用」即可。`,
    props: { options: connectionTypeOptions.value, clearable: false },
  },
  { type: 'divider', label: '目录与身份', props: { titlePlacement: 'left' }, span: 2, vIf: formData => isEnterprisePlatform(formData.platform) },
  {
    field: 'identityPolicy',
    label: '未匹配用户处理',
    type: 'select',
    defaultValue: 'BIND_ONLY',
    vIf: formData => isEnterprisePlatform(formData.platform),
    labelTip: `外部用户首次扫码登录时如何对应到本系统账号：
・仅绑定：只允许绑定已有账号，未绑定无法登录（最安全）
・自动建号：无匹配账号时自动创建新用户
・人工处理：无法匹配时生成问题单，由管理员在「问题单」页签处理`,
    rules: [{ required: true, message: '请选择身份匹配策略', trigger: 'change' }],
    props: { options: identityPolicyOptions.value, clearable: false },
  },
  {
    field: 'defaultRoleIds',
    label: '默认角色',
    type: 'select',
    span: 2,
    vIf: formData => isEnterprisePlatform(formData.platform) && formData.identityPolicy === 'AUTO_CREATE',
    labelTip: '自动建号时为新用户分配的角色（可多选）。留空时跟随全局默认角色配置。',
    props: {
      options: roleOptions.value,
      multiple: true,
      clearable: true,
      placeholder: '自动建号时分配的默认角色（可多选），为空走全局配置',
    },
  },
  {
    field: 'directoryAuthority',
    label: '通讯录同步方式',
    type: 'select',
    defaultValue: 'NONE',
    vIf: formData => isEnterprisePlatform(formData.platform),
    labelTip: `决定组织架构和人员名单以哪边为准：
・外部平台：以企微/钉钉通讯录为准，定期同步到本系统（选此项才能触发同步）
・本系统：以本系统组织架构为准，不从外部拉取
・不同步：不做目录同步，仅用于登录/消息`,
    rules: [{ required: true, message: '请选择目录权威来源', trigger: 'change' }],
    props: { options: directoryAuthorityOptions.value, clearable: false },
  },
  {
    field: 'defaultOrgId',
    label: '同步到组织',
    type: 'orgTreeSelect',
    vIf: formData => isEnterprisePlatform(formData.platform) && formData.directoryAuthority === 'EXTERNAL',
    labelTip: `同步过来的外部部门会挂在本系统的这个组织节点下。
组织ID可在「系统管理 → 部门管理」中查看，留空时挂在根节点下。`,
    props: { placeholder: '请选择外部通讯录在本系统中的挂载组织', filterable: true, clearable: true },
  },
  {
    field: 'apiBaseUrl',
    label: 'API基础地址',
    type: 'input',
    span: 2,
    vIf: formData => isEnterprisePlatform(formData.platform),
    labelTip: `调用平台接口的基础地址。绝大多数情况留空即可（自动使用官方地址）；
仅平台私有化部署时填写自建网关地址。`,
    props: { placeholder: '留空使用平台官方地址，私有化部署可自定义' },
  },
  // ── OAuth 平台提示（Gitee/GitHub 等纯登录平台，凭据在连接配置中维护） ──
  {
    type: 'divider',
    label: 'OAuth 凭据说明',
    span: 2,
    vIf: formData => !isEnterprisePlatform(formData.platform),
    props: {
      description: '纯 OAuth 登录平台（如 Gitee、GitHub）的凭据在连接配置中维护。保存连接后，打开「配置」，在平台应用中填写 Client ID、Secret 和回调地址等 OAuth 参数。',
    },
  },
  // ── 企业型平台高级功能 ──
  { type: 'divider', label: '登录设置', props: { titlePlacement: 'left' }, span: 2, vIf: formData => isEnterprisePlatform(formData.platform) },
  {
    field: 'ssoWorkbenchEnabled',
    label: '工作台免登',
    type: 'switch',
    defaultValue: 0,
    checkedValue: 1,
    uncheckedValue: 0,
    span: 2,
    labelTip: `开启后，用户在企业客户端（如企业微信）工作台点击本应用可自动登录，无需手动扫码。
仅企业型平台且已正确配置 OAuth 网页授权可信域名时生效；同平台多个连接只能开启一个。
前端会用本连接的「连接标识」发起免登，无需再在前端写死 connectionCode。`,
    vIf: formData => isEnterprisePlatform(formData.platform),
    props: { },
  },
  { type: 'divider', label: '待办推送', props: { titlePlacement: 'left' }, span: 2, vIf: formData => isEnterprisePlatform(formData.platform) },
  {
    field: 'todoPushEnabled',
    label: '待办卡片推送',
    type: 'switch',
    defaultValue: 0,
    checkedValue: 1,
    uncheckedValue: 0,
    span: 2,
    labelTip: `开启后，流程待办任务会以卡片消息推送到外部平台（如企微），点击卡片可直达待办H5页面。
需先在能力绑定中配置「消息推送」能力。`,
    vIf: formData => isEnterprisePlatform(formData.platform),
    props: { },
  },
  {
    field: 'todoPushH5Url',
    label: '待办H5访问地址',
    type: 'input',
    span: 2,
    vIf: formData => isEnterprisePlatform(formData.platform) && formData.todoPushEnabled === 1,
    labelTip: `待办卡片点击后跳转的移动端H5地址，开启推送时必填。
填到H5应用根路径即可（如 https://h5.example.com/forge-h5），无需带 #/ 路由前缀，系统会自动拼接待办详情路径。
注意：该域名需在平台后台登记为可信域名（企微：应用详情 → 网页授权及 JS-SDK）。`,
    props: { placeholder: '如 https://h5.example.com/forge-h5，无需带 #/，开启推送时必填' },
  },
  { type: 'divider', label: '自动同步通讯录', props: { titlePlacement: 'left' }, span: 2, vIf: formData => isEnterprisePlatform(formData.platform) && formData.directoryAuthority === 'EXTERNAL' },
  {
    field: 'syncScheduleEnabled',
    label: '自动同步通讯录',
    type: 'switch',
    defaultValue: 0,
    checkedValue: 1,
    uncheckedValue: 0,
    span: 2,
    labelTip: `开启后系统会按下方 Cron 周期自动全量同步该连接的组织与成员，无需再去「定时任务」模块手工配置。
关闭后自动移除对应定时任务；连接停用时定时同步同样暂停。`,
    vIf: formData => isEnterprisePlatform(formData.platform) && formData.directoryAuthority === 'EXTERNAL',
    props: { },
  },
  {
    field: 'syncCron',
    label: '同步频率',
    type: 'input',
    span: 2,
    vIf: formData => isEnterprisePlatform(formData.platform)
      && formData.directoryAuthority === 'EXTERNAL'
      && formData.syncScheduleEnabled === 1,
    labelTip: `标准 Quartz Cron 表达式（秒 分 时 日 月 周），开启定时同步时必填。
示例：0 0 2 * * ?（每天凌晨2点）、0 0/30 * * * ?（每30分钟）、0 0 1 * * ?（每天1点）。`,
    props: { placeholder: '如 0 0 2 * * ?（每天凌晨2点），开启定时同步时必填' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: '1',
    labelTip: '停用后该连接的扫码登录、目录同步、消息推送全部暂停，配置保留可随时重新启用。',
    rules: [{ required: true, message: '请选择状态', trigger: 'change' }],
    props: { options: statusOptions.value, clearable: false },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '备注说明', rows: 2 },
  },
])

function handleBeforeRenderDetail(data) {
  if (!data)
    return data
  if (data.status !== null && data.status !== undefined)
    data.status = String(data.status)
  if (data.defaultOrgId !== null && data.defaultOrgId !== undefined)
    data.defaultOrgId = String(data.defaultOrgId)
  // 待办推送开关：后端 Integer → 数值（switch 组件 checked/unchecked 值）
  data.todoPushEnabled = Number(data.todoPushEnabled) === 1 ? 1 : 0
  // 工作台免登开关：后端 Integer → 数值
  data.ssoWorkbenchEnabled = Number(data.ssoWorkbenchEnabled) === 1 ? 1 : 0
  // 定时同步开关：后端 Integer → 数值
  data.syncScheduleEnabled = Number(data.syncScheduleEnabled) === 1 ? 1 : 0
  // 默认角色：逗号分隔字符串 → 字符串数组，避免雪花 ID 转 Number 丢失精度。
  if (data.defaultRoleIds && typeof data.defaultRoleIds === 'string') {
    data.defaultRoleIds = data.defaultRoleIds.split(',').map(id => id.trim()).filter(Boolean)
  }
  else {
    data.defaultRoleIds = []
  }
  return data
}

/**
 * 连接标识自动生成：平台简称 + 4位随机后缀，如 wecom-a1b2
 */
const PLATFORM_CODE_PREFIX = {
  WECHAT_ENTERPRISE: 'wecom',
  DINGTALK: 'dingtalk',
  DINGTALK_ACCOUNT: 'dingtalk',
  FEISHU: 'feishu',
}

function genConnectionCode(platform) {
  const prefix = PLATFORM_CODE_PREFIX[platform] || String(platform || 'conn').toLowerCase().replace(/[^a-z0-9]+/g, '-')
  const suffix = Math.random().toString(36).slice(2, 6)
  return `${prefix}-${suffix}`
}

function handleBeforeSubmit(formData) {
  // 编码留空时自动生成，降低新建门槛
  if (!String(formData.connectionCode || '').trim())
    formData.connectionCode = genConnectionCode(formData.platform)
  // 企业型平台依赖企业ID换取凭据，纯 OAuth 登录平台无此概念，故按平台条件校验而非静态必填
  if (isEnterprisePlatform(formData.platform) && !formData.enterpriseId) {
    window.$message.warning('该平台为企业型连接，请填写企业标识')
    return false
  }
  if (formData.status !== null && formData.status !== undefined)
    formData.status = Number(formData.status)
  // 开启待办卡片推送时 H5 地址必填且须为 http/https 地址（企微 textcard 要求）
  formData.todoPushEnabled = Number(formData.todoPushEnabled) === 1 ? 1 : 0
  if (formData.todoPushEnabled === 1) {
    const h5Url = (formData.todoPushH5Url || '').trim()
    if (!h5Url) {
      window.$message.warning('开启待办卡片推送后，请填写待办H5访问地址')
      return false
    }
    if (!/^https?:\/\//i.test(h5Url)) {
      window.$message.warning('待办H5访问地址须以 http:// 或 https:// 开头')
      return false
    }
    // 地址栏复制来的地址常带 hash 路由前缀（如 /forge-h5/#/），
    // 与后端拼接的 /#/pages/todo-detail 叠加会产生两个 # 构成非法链接，这里统一存根路径
    const normalizedH5Url = h5Url.split('#')[0].replace(/\/+$/, '')
    if (!normalizedH5Url) {
      window.$message.warning('待办H5访问地址不能只填 # 路由部分，请填写H5应用根路径')
      return false
    }
    if (normalizedH5Url !== h5Url) {
      window.$message.info('已自动去除待办H5地址中的 # 路由部分，仅保留应用根路径')
    }
    formData.todoPushH5Url = normalizedH5Url
  }
  // 定时同步：开启时必须填写 Cron，且校验为 6/7 段表达式，避免脏配置导致建任务失败
  formData.syncScheduleEnabled = Number(formData.syncScheduleEnabled) === 1 ? 1 : 0
  if (formData.syncScheduleEnabled === 1) {
    const cron = (formData.syncCron || '').trim()
    if (!cron) {
      window.$message.warning('开启定时目录同步后，请填写同步周期 Cron 表达式')
      return false
    }
    const segments = cron.split(/\s+/)
    if (segments.length < 6 || segments.length > 7) {
      window.$message.warning('Cron 表达式格式不正确，应为 6 或 7 段（秒 分 时 日 月 周 [年]）')
      return false
    }
    formData.syncCron = cron
  }
  else {
    // 关闭时清空 Cron，避免残留旧值
    formData.syncCron = null
  }
  // 雪花 ID 保持字符串传输，避免 Number 超出安全整数范围后丢失精度。
  formData.defaultOrgId = formData.defaultOrgId ? String(formData.defaultOrgId) : null
  // 默认角色：数值数组 → 逗号分隔字符串（后端存储格式）
  if (Array.isArray(formData.defaultRoleIds) && formData.defaultRoleIds.length > 0) {
    formData.defaultRoleIds = formData.defaultRoleIds.join(',')
  }
  else {
    formData.defaultRoleIds = null
  }
  // 企微留空时兜底官方地址，其余平台交由后端按平台默认地址处理
  if (!formData.apiBaseUrl && formData.platform === 'WECHAT_ENTERPRISE')
    formData.apiBaseUrl = WECOM_API_BASE_URL
  // 纯 OAuth 平台不存在自建/第三方应用形态，统一归一为仅登录连接类型
  if (!isEnterprisePlatform(formData.platform)) {
    formData.connectionType = 'OAUTH_ONLY'
    // OAuth 凭据在连接配置的平台应用中维护，清除连接维度的残留旧值
    delete formData.clientId
    delete formData.clientSecret
    delete formData.redirectUri
    delete formData.scope
    delete formData.agentId
  }
  return formData
}

function handleDelete(row) {
  crudRef.value?.handleDelete(row)
}

function handleTriggerSync(row) {
  window.$dialog.warning({
    title: '触发全量同步',
    content: `确定要对「${row.connectionName}」执行全量目录同步吗？同步结果可在「同步批次」页查看。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await triggerConnectionSync(row.id, { syncType: 'FULL' })
        if (res.code === 200)
          window.$message.success('同步已完成，请到「同步批次」页查看结果')
      }
      catch {
        window.$message.error('触发同步失败')
      }
    },
  })
}

/**
 * 启用/停用切换：VO 字段完整，直接用行数据构造保存入参（不含任何凭据字段）
 */
function handleToggleStatus(row) {
  const enabling = Number(row.status) !== 1
  window.$dialog.warning({
    title: enabling ? '确认启用' : '确认停用',
    content: enabling
      ? `确定要启用连接「${row.connectionName}」吗？启用后扫码登录、目录同步、消息推送恢复可用。`
      : `确定要停用连接「${row.connectionName}」吗？停用后扫码登录、目录同步、消息推送全部暂停，配置保留可随时重新启用。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await updateConnection({
          id: row.id,
          platform: row.platform,
          platformName: row.platformName,
          platformLogo: row.platformLogo,
          connectionCode: row.connectionCode,
          connectionName: row.connectionName,
          enterpriseId: row.enterpriseId,
          connectionType: row.connectionType,
          identityPolicy: row.identityPolicy,
          defaultRoleIds: row.defaultRoleIds,
          directoryAuthority: row.directoryAuthority,
          defaultOrgId: row.defaultOrgId,
          apiBaseUrl: row.apiBaseUrl,
          ssoWorkbenchEnabled: row.ssoWorkbenchEnabled,
          todoPushEnabled: row.todoPushEnabled,
          todoPushH5Url: row.todoPushH5Url,
          syncScheduleEnabled: row.syncScheduleEnabled,
          syncCron: row.syncCron,
          status: enabling ? 1 : 0,
          remark: row.remark,
        })
        if (res.code === 200) {
          window.$message.success(enabling ? '已启用' : '已停用')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error(enabling ? '启用失败' : '停用失败')
      }
    },
  })
}

// ==================== 测试入口 ====================

const testModalVisible = ref(false)
const testConnectionId = ref(null)
const msgTestVisible = ref(false)
const msgTestConnectionId = ref(null)

function handleOpenTest(row) {
  testConnectionId.value = row.id
  testModalVisible.value = true
}

function handleOpenMsgTest(row) {
  msgTestConnectionId.value = row.id
  msgTestVisible.value = true
}
</script>

<style scoped src="./components/connections.css"></style>

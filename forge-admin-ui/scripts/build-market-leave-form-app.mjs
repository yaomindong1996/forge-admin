/**
 * 市场请假表单/应用包：列表 + 发起 + 审批 + 首页说明，可对接打印字段。
 */
import { writeFileSync, copyFileSync, mkdirSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const websiteRoot = join(__dirname, '../../../forge-website/forge-website-official-ui')
const website = join(websiteRoot, 'public/market/packages')
const fixtures = join(__dirname, '../src/utils/__tests__/fixtures/market')
const writeWebsite = existsSync(websiteRoot)

function field(id, componentKey, label, fieldCode, columnName, layoutSpan, extra = {}) {
  const { props = {}, validation = { required: true, requiredMessage: `请填写${label}` }, options } = extra
  return {
    id,
    componentKey,
    label,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName,
      createIfMissing: true,
      source: 'field_asset',
    },
    props: options ? { ...props, options } : props,
    layout: { span: layoutSpan },
    validation,
    visibility: {},
  }
}

const leaveTypeOptions = [
  { label: '年假', value: 'annual' },
  { label: '事假', value: 'personal' },
  { label: '病假', value: 'sick' },
  { label: '调休', value: 'comp' },
  { label: '婚假', value: 'marriage' },
  { label: '产假/陪产假', value: 'maternity' },
  { label: '丧假', value: 'bereavement' },
]

const approvalStatusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
]

const formPayload = {
  schemaVersion: 1,
  formKey: 'market_leave_form',
  formName: '请假申请',
  usage: ['create', 'edit', 'approve'],
  layout: {
    labelPlacement: 'left',
    labelAlign: 'right',
    labelWidth: '110px',
    size: 'medium',
    modalType: 'drawer',
    showFeedback: true,
    gridColumns: 2,
    rowGap: 16,
    columnGap: 16,
  },
  components: [
    field('cmp_applicant', 'input', '申请人', 'applicantName', 'applicant_name', 12, { props: { placeholder: '姓名' } }),
    field('cmp_emp_no', 'input', '工号', 'employeeNo', 'employee_no', 12, { props: { placeholder: '员工工号' }, validation: { required: false } }),
    field('cmp_dept', 'input', '所属部门', 'department', 'department', 12, { props: { placeholder: '部门全称' } }),
    field('cmp_phone', 'input', '联系电话', 'mobile', 'mobile', 12, { props: { placeholder: '请假期间可联系号码' } }),
    field('cmp_leave_type', 'select', '请假类型', 'leaveType', 'leave_type', 12, {
      props: { placeholder: '请选择类型' },
      options: leaveTypeOptions,
      validation: { required: true, requiredMessage: '请选择请假类型' },
    }),
    field('cmp_days', 'number', '请假天数', 'leaveDays', 'leave_days', 12, {
      props: { placeholder: '按制度折算', min: 0.5, step: 0.5 },
    }),
    field('cmp_start', 'date', '开始日期', 'startDate', 'start_date', 12, {
      props: { type: 'date', clearable: true },
      validation: { required: true, requiredMessage: '请选择开始日期' },
    }),
    field('cmp_end', 'date', '结束日期', 'endDate', 'end_date', 12, {
      props: { type: 'date', clearable: true },
      validation: { required: true, requiredMessage: '请选择结束日期' },
    }),
    field('cmp_reason', 'textarea', '请假事由', 'reason', 'reason', 24, {
      props: { placeholder: '请说明事由；病假请注明是否已附证明', rows: 3 },
    }),
    field('cmp_deputy', 'input', '职务代理人', 'deputyName', 'deputy_name', 12, {
      props: { placeholder: '请假期间工作代理人' },
      validation: { required: false },
    }),
    field('cmp_handover', 'textarea', '工作交接', 'handover', 'handover', 24, {
      props: { placeholder: '待办事项、资料位置、对接人', rows: 3 },
      validation: { required: true, requiredMessage: '请填写工作交接说明' },
    }),
    field('cmp_status', 'select', '审批状态', 'approvalStatus', 'approval_status', 12, {
      props: { placeholder: '流程状态' },
      options: approvalStatusOptions,
      validation: { required: false },
    }),
    field('cmp_remark', 'textarea', '备注', 'remark', 'remark', 24, {
      props: { rows: 2, placeholder: '补充说明（选填）' },
      validation: { required: false },
    }),
  ],
}

const fieldCodes = formPayload.components.map(c => c.fieldBinding.fieldCode)

const modelFields = formPayload.components.map((c) => {
  const code = c.fieldBinding.fieldCode
  const dataType = c.componentKey === 'number'
    ? 'DECIMAL'
    : c.componentKey === 'date'
      ? 'DATE'
      : c.componentKey === 'textarea'
        ? 'TEXT'
        : 'STRING'
  return {
    fieldCode: code,
    fieldName: c.label,
    columnName: c.fieldBinding.columnName,
    dataType,
    required: !!c.validation?.required,
    formVisible: true,
  }
})

const formPack = {
  kind: 'form',
  name: '请假申请表单',
  version: '1.2.0',
  payload: formPayload,
}

function objectRef(pageMode) {
  return {
    objectCode: 'leave_request',
    objectName: '请假单',
    pageMode,
    pageKey: pageMode === 'form' ? 'form' : 'list',
  }
}

function crudBlock({ id, title, pageMode, formOnly = false }) {
  const layoutType = formOnly ? 'form' : pageMode === 'list-form' ? 'list-form' : 'list'
  return {
    id,
    blockType: 'AiCrudPage',
    label: title,
    gridX: 0,
    gridY: 0,
    gridW: 12,
    gridH: formOnly ? 18 : 22,
    props: {
      title,
      formAssetId: 'form_leave_request',
      formAssetFieldsInitialized: true,
      fieldRefs: fieldCodes,
      objectRef: objectRef(formOnly ? 'form' : pageMode === 'list-form' ? 'list' : 'list'),
      layoutType,
      ...(formOnly
        ? {
            formOnly: true,
            hideToolbar: true,
            hideBatchDelete: true,
            showSearch: false,
            formOpenMode: 'flat',
            modalType: 'flat',
          }
        : {
            formOnly: false,
            hideToolbar: false,
            hideBatchDelete: false,
            showSearch: true,
            formOpenMode: pageMode === 'list-form' ? 'flat' : 'drawer',
            modalType: pageMode === 'list-form' ? 'flat' : 'drawer',
          }),
    },
  }
}

function pageLayout(items, layoutType = 'list') {
  return {
    gridLayout: {
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType,
      items,
    },
  }
}

function pageNode({ id, title, pageType, pageShape, pageTemplate, sort, objectRef: ref }) {
  return {
    id,
    type: 'page',
    title,
    pageType,
    pageShape,
    pageTemplate,
    parentId: null,
    sort,
    navigationVisible: true,
    systemMenuVisible: false,
    ...(ref ? { objectRef: ref } : {}),
  }
}

const listBlock = crudBlock({
  id: 'blk_leave_list_crud',
  title: '请假记录',
  pageMode: 'list',
})
const applyBlock = crudBlock({
  id: 'blk_leave_apply_crud',
  title: '发起请假',
  pageMode: 'form',
  formOnly: true,
})
const approveBlock = crudBlock({
  id: 'blk_leave_approve_crud',
  title: '审批办理',
  pageMode: 'list-form',
})

/**
 * 首页演示自由布局：快捷入口 + 指标 + 柱状/折线图 + 请假列表。
 * 入口跳转走 targetPageId（与导航页 id 一致），避免再用散落的裸按钮。
 */
const homeLayout = pageLayout([
  {
    id: 'blk_home_quick_entry',
    blockType: 'quick-entry',
    label: '快捷入口',
    gridX: 0,
    gridY: 0,
    gridW: 12,
    gridH: 3,
    props: {
      columns: 3,
      items: [
        {
          key: 'go_apply',
          title: '发起请假',
          description: '填写申请并提交审批',
          icon: 'CalendarOutline',
          tone: 'primary',
          targetPageId: 'page_leave_apply',
        },
        {
          key: 'go_list',
          title: '请假记录',
          description: '查询、导出与跟进',
          icon: 'ListOutline',
          tone: 'info',
          targetPageId: 'page_leave_list',
        },
        {
          key: 'go_approve',
          title: '审批办理',
          description: '处理待审请假单',
          icon: 'CheckmarkCircleOutline',
          tone: 'success',
          targetPageId: 'page_leave_approve',
        },
      ],
      style: {
        widthMode: 'full',
        heightMode: 'auto',
        height: 120,
      },
    },
  },
  {
    id: 'blk_home_stats',
    blockType: 'stats-strip',
    label: '请假统计',
    gridX: 0,
    gridY: 3,
    gridW: 12,
    gridH: 2,
    props: {
      metrics: [
        { label: '本月申请', value: '36', trend: '+12%' },
        { label: '待审批', value: '8', trend: '+2' },
        { label: '已通过', value: '24', trend: '+9%' },
        { label: '平均天数', value: '2.4', trend: '-0.3' },
      ],
      style: {
        widthMode: 'full',
        heightMode: 'auto',
        height: 88,
      },
    },
  },
  {
    id: 'blk_home_bar_chart',
    blockType: 'bar-chart',
    label: '请假类型分布',
    gridX: 0,
    gridY: 5,
    gridW: 6,
    gridH: 8,
    props: {
      title: '请假类型分布',
      height: 260,
      showLegend: false,
      categories: ['年假', '事假', '病假', '调休', '婚假'],
      series: [{ name: '申请数', data: [14, 9, 6, 4, 3] }],
      style: {
        widthMode: 'full',
        heightMode: 'fixed',
        height: 300,
      },
    },
  },
  {
    id: 'blk_home_line_chart',
    blockType: 'line-chart',
    label: '近六月趋势',
    gridX: 6,
    gridY: 5,
    gridW: 6,
    gridH: 8,
    props: {
      title: '近六月请假趋势',
      height: 260,
      showLegend: true,
      smooth: true,
      showArea: true,
      categories: ['1月', '2月', '3月', '4月', '5月', '6月'],
      series: [
        { name: '申请', data: [8, 12, 10, 16, 14, 18] },
        { name: '通过', data: [6, 9, 8, 12, 11, 15] },
      ],
      style: {
        widthMode: 'full',
        heightMode: 'fixed',
        height: 300,
      },
    },
  },
  {
    ...listBlock,
    id: 'blk_home_list_crud',
    gridX: 0,
    gridY: 13,
    gridW: 12,
    gridH: 18,
    props: {
      ...listBlock.props,
      title: '请假记录',
    },
  },
], 'list')

const workbenchPage = {
  title: '个人工作台',
  pageType: 'content',
  layout: {
    workbenchLayoutVersion: 4,
    gridLayout: {
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType: 'simple-crud',
      items: [
        {
          id: 'blk_wb_metrics',
          blockType: 'workspace-summary-metrics',
          label: '工作台统计',
          gridX: 0,
          gridY: 0,
          gridW: 12,
          gridH: 3,
          props: {
            visibleKeys: ['todo', 'done', 'started', 'cc'],
            columns: 4,
            showHeader: false,
            style: {
              widthMode: 'full',
              heightMode: 'auto',
              pageFlowX: 24,
              pageFlowY: 20,
              pageFlowHeight: 200,
              height: 200,
            },
          },
        },
        {
          id: 'blk_wb_tip',
          blockType: 'info-panel',
          label: '使用提示',
          gridX: 0,
          gridY: 2,
          gridW: 12,
          gridH: 2,
          props: {
            type: 'info',
            title: '使用提示',
            content: '点击上方统计可进入待办/已办/发起中/抄送。日常请假请从左侧「请假中心」进入。',
            style: {
              widthMode: 'full',
              heightMode: 'auto',
              pageFlowX: 24,
              pageFlowY: 236,
              pageFlowHeight: 96,
              height: 96,
            },
          },
        },
      ],
    },
  },
}

const appPack = {
  format: 'forge-application-bundle',
  schemaVersion: 2,
  exportedAt: new Date().toISOString(),
  source: {
    applicationCode: 'market_leave_app',
    applicationName: '请假申请',
    suiteCode: 'HR',
  },
  selection: {
    mode: 'FULL',
    pageIds: ['page_leave_home', 'page_leave_list', 'page_leave_apply', 'page_leave_approve'],
    objectCodes: ['leave_request'],
  },
  application: {
    applicationName: '请假申请',
    applicationCode: 'market_leave_app',
    suiteCode: 'HR',
    // 应用卡片走 IconRenderer：须用 ionicons5，勿用 i-lucide（动态 class 不会进 UnoCSS，会显示空色块）
    icon: 'ionicons5:CalendarOutline',
    description: '人事请假完整样例：请假中心、记录列表、发起表单与审批办理四页，含交接/代理人/审批状态，可对接打印单。',
    status: 1,
    options: {
      inAppBuilder: {
        schemaVersion: 2,
        homePageId: 'page_leave_home',
        nodes: [
          pageNode({
            id: 'page_leave_home',
            title: '请假中心',
            pageType: 'object',
            pageShape: 'list',
            pageTemplate: 'list',
            sort: 0,
            objectRef: objectRef('list'),
          }),
          pageNode({
            id: 'page_leave_list',
            title: '请假记录',
            pageType: 'object',
            pageShape: 'list',
            pageTemplate: 'list',
            sort: 10,
            objectRef: objectRef('list'),
          }),
          pageNode({
            id: 'page_leave_apply',
            title: '发起请假',
            pageType: 'object',
            pageShape: 'form',
            pageTemplate: 'form',
            sort: 20,
            objectRef: objectRef('form'),
          }),
          pageNode({
            id: 'page_leave_approve',
            title: '审批办理',
            pageType: 'object',
            pageShape: 'list-form',
            pageTemplate: 'list-form',
            sort: 30,
            objectRef: objectRef('list'),
          }),
        ],
        pages: {
          page_leave_home: {
            title: '请假中心',
            description: '查询与管理请假记录',
            layout: homeLayout,
          },
          page_leave_list: {
            title: '请假记录',
            description: '查询、编辑与导出历史请假单',
            layout: pageLayout([listBlock], 'list'),
          },
          page_leave_apply: {
            title: '发起请假',
            description: '员工填写并提交请假申请',
            layout: pageLayout([applyBlock], 'form'),
          },
          page_leave_approve: {
            title: '审批办理',
            description: '主管查看单据并维护审批状态',
            layout: pageLayout([approveBlock], 'list-form'),
          },
          'system:workbench': workbenchPage,
        },
        formAssets: [
          {
            id: 'form_leave_request',
            formKey: 'form_leave_request',
            name: '请假申请',
            formDesignerSchema: formPayload,
          },
        ],
      },
    },
    portalConfig: {},
  },
  objects: [
    {
      objectCode: 'leave_request',
      objectName: '请假单',
      objectType: 'MASTER',
      role: 'PRIMARY',
      displayField: 'applicantName',
      icon: 'ionicons5:DocumentTextOutline',
      description: '请假申请主对象（含交接与审批状态）',
      modelSchema: {
        object: { code: 'leave_request', name: '请假单' },
        fields: modelFields,
      },
      formDesignerSchema: formPayload,
      relations: [],
    },
  ],
  entries: [],
  bindings: [],
  processes: [],
  extensions: [],
  triggers: [],
  printing: {},
}

mkdirSync(fixtures, { recursive: true })
writeFileSync(join(fixtures, 'leave-request.form.json'), `${JSON.stringify(formPack, null, 2)}\n`)
writeFileSync(join(fixtures, 'leave-request.forge-app.json'), `${JSON.stringify(appPack, null, 2)}\n`)
let websiteWritten = false
if (writeWebsite) {
  try {
    mkdirSync(join(website, 'form'), { recursive: true })
    mkdirSync(join(website, 'app'), { recursive: true })
    copyFileSync(join(fixtures, 'leave-request.form.json'), join(website, 'form/leave-request.form.json'))
    copyFileSync(join(fixtures, 'leave-request.forge-app.json'), join(website, 'app/leave-request.forge-app.json'))
    websiteWritten = true
  }
  catch (error) {
    console.warn(`skip website package write: ${error.message}`)
  }
}

const builder = appPack.application.options.inAppBuilder
console.log(JSON.stringify({
  formComponents: formPayload.components.length,
  modelFields: modelFields.length,
  nodes: builder.nodes.length,
  pages: Object.keys(builder.pages),
  writeWebsite: websiteWritten,
}))

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

const homeLayout = {
  gridLayout: {
    cols: 12,
    rowHeight: 32,
    gap: 12,
    designWidth: 1366,
    layoutType: 'custom',
    items: [
      {
        id: 'blk_home_title',
        blockType: 'page-title',
        label: '页面标题',
        gridX: 0,
        gridY: 0,
        gridW: 12,
        gridH: 2,
        props: {
          title: '请假中心',
          subtitle: '从发起申请、查看记录到审批办理，一套页面覆盖日常人事请假。',
        },
      },
      {
        id: 'blk_home_intro',
        blockType: 'intro',
        label: '介绍',
        gridX: 0,
        gridY: 2,
        gridW: 12,
        gridH: 3,
        props: {
          title: '怎么用这套应用',
          description: '员工在「发起请假」填写并提交；本人与人事在「请假记录」查询与导出；主管在「审批办理」处理待审单据。可对接请假审批打印单。',
        },
      },
      {
        id: 'blk_home_metric_1',
        blockType: 'metric-card',
        label: '指标',
        gridX: 0,
        gridY: 5,
        gridW: 4,
        gridH: 3,
        props: { title: '发起入口', value: '表单页', trend: '一键填写提交' },
      },
      {
        id: 'blk_home_metric_2',
        blockType: 'metric-card',
        label: '指标',
        gridX: 4,
        gridY: 5,
        gridW: 4,
        gridH: 3,
        props: { title: '记录查询', value: '列表页', trend: '搜索 / 编辑 / 导出' },
      },
      {
        id: 'blk_home_metric_3',
        blockType: 'metric-card',
        label: '指标',
        gridX: 8,
        gridY: 5,
        gridW: 4,
        gridH: 3,
        props: { title: '审批办理', value: '列表+表单', trend: '状态与交接一屏处理' },
      },
      {
        id: 'blk_home_tip',
        blockType: 'text',
        label: '说明',
        gridX: 0,
        gridY: 8,
        gridW: 12,
        gridH: 2,
        props: {
          content: '导入后请先发布应用与对象，再在门户使用。字段已含交接说明、代理人与审批状态，便于接流程与打印。',
        },
      },
    ],
  },
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
    icon: 'i-lucide:calendar-days',
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
            pageType: 'content',
            pageShape: 'custom',
            pageTemplate: 'custom',
            sort: 0,
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
            description: '应用使用说明与入口导览',
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
      icon: 'i-lucide:file-text',
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

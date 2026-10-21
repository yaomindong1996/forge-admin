/**
 * 市场员工入职应用/表单包：多页 + 更全字段，作为付费进阶样例。
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
  const {
    props = {},
    validation = { required: true, requiredMessage: `请填写${label}` },
    options,
  } = extra
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

const optional = { required: false }

const formPayload = {
  schemaVersion: 1,
  formKey: 'market_onboard_form',
  formName: '员工入职登记',
  usage: ['create', 'edit', 'submit'],
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
    field('cmp_name', 'input', '姓名', 'fullName', 'full_name', 12, { props: { placeholder: '姓名' } }),
    field('cmp_gender', 'select', '性别', 'gender', 'gender', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '男', value: 'male' },
        { label: '女', value: 'female' },
        { label: '其他', value: 'other' },
      ],
    }),
    field('cmp_mobile', 'input', '手机号', 'mobile', 'mobile', 12, { props: { placeholder: '11 位手机号' } }),
    field('cmp_id', 'input', '身份证号', 'idNo', 'id_no', 12, { props: { placeholder: '身份证号' } }),
    field('cmp_birth', 'date', '出生日期', 'birthDate', 'birth_date', 12, {
      props: { type: 'date', clearable: true },
      validation: optional,
    }),
    field('cmp_emp_no', 'input', '工号', 'employeeNo', 'employee_no', 12, {
      props: { placeholder: '预分配工号' },
      validation: optional,
    }),
    field('cmp_hire', 'date', '入职日期', 'hireDate', 'hire_date', 12, {
      props: { type: 'date' },
      validation: { required: true, requiredMessage: '请选择入职日期' },
    }),
    field('cmp_dept', 'input', '所属部门', 'department', 'department', 12, { props: { placeholder: '部门全称' } }),
    field('cmp_job', 'input', '岗位', 'jobTitle', 'job_title', 12, { props: { placeholder: '岗位名称' } }),
    field('cmp_level', 'select', '职级', 'jobLevel', 'job_level', 12, {
      props: { placeholder: '职级' },
      options: [
        { label: 'P1', value: 'P1' },
        { label: 'P2', value: 'P2' },
        { label: 'P3', value: 'P3' },
        { label: 'P4', value: 'P4' },
        { label: 'M1', value: 'M1' },
        { label: 'M2', value: 'M2' },
      ],
      validation: optional,
    }),
    field('cmp_email', 'input', '工作邮箱', 'workEmail', 'work_email', 12, {
      props: { placeholder: 'name@company.com' },
      validation: optional,
    }),
    field('cmp_contract_type', 'select', '合同类型', 'contractType', 'contract_type', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '劳动合同', value: 'labor' },
        { label: '劳务协议', value: 'service' },
        { label: '实习协议', value: 'intern' },
        { label: '外包', value: 'outsource' },
      ],
    }),
    field('cmp_contract_start', 'date', '合同开始', 'contractStart', 'contract_start', 12, {
      props: { type: 'date' },
      validation: { required: true, requiredMessage: '请选择合同开始日期' },
    }),
    field('cmp_contract_end', 'date', '合同结束', 'contractEnd', 'contract_end', 12, {
      props: { type: 'date', clearable: true },
      validation: optional,
    }),
    field('cmp_probation', 'number', '试用期(月)', 'probationMonths', 'probation_months', 12, {
      props: { min: 0, max: 12, step: 1, placeholder: '0-12' },
      validation: optional,
    }),
    field('cmp_edu', 'select', '学历', 'education', 'education', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '高中及以下', value: 'high_school' },
        { label: '大专', value: 'college' },
        { label: '本科', value: 'bachelor' },
        { label: '硕士', value: 'master' },
        { label: '博士', value: 'phd' },
      ],
      validation: optional,
    }),
    field('cmp_school', 'input', '毕业院校', 'school', 'school', 12, {
      props: { placeholder: '院校名称' },
      validation: optional,
    }),
    field('cmp_major', 'input', '专业', 'major', 'major', 12, {
      props: { placeholder: '专业名称' },
      validation: optional,
    }),
    field('cmp_emergency', 'input', '紧急联系人', 'emergencyContact', 'emergency_contact', 12, {
      props: { placeholder: '姓名' },
    }),
    field('cmp_emergency_phone', 'input', '紧急联系电话', 'emergencyPhone', 'emergency_phone', 12, {
      props: { placeholder: '联系电话' },
    }),
    field('cmp_addr', 'textarea', '现居住地址', 'address', 'address', 24, {
      props: { rows: 2, placeholder: '详细地址' },
      validation: optional,
    }),
    field('cmp_bank', 'input', '银行卡号', 'bankAccount', 'bank_account', 12, {
      props: { placeholder: '工资卡号' },
      validation: optional,
    }),
    field('cmp_bank_name', 'input', '开户行', 'bankName', 'bank_name', 12, {
      props: { placeholder: '开户行' },
      validation: optional,
    }),
    field('cmp_bank_branch', 'input', '开户支行', 'bankBranch', 'bank_branch', 12, {
      props: { placeholder: '支行名称' },
      validation: optional,
    }),
    field('cmp_mentor', 'input', '入职导师', 'mentorName', 'mentor_name', 12, {
      props: { placeholder: '导师姓名' },
      validation: optional,
    }),
    field('cmp_status', 'select', '入职状态', 'onboardStatus', 'onboard_status', 12, {
      props: { placeholder: '状态' },
      options: [
        { label: '待办材料', value: 'pending_docs' },
        { label: '待开通账号', value: 'pending_account' },
        { label: '已入职', value: 'active' },
        { label: '已取消', value: 'cancelled' },
      ],
      validation: optional,
    }),
    field('cmp_docs', 'select', '材料是否齐全', 'docsReady', 'docs_ready', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '未齐', value: 'no' },
        { label: '已齐', value: 'yes' },
      ],
      validation: optional,
    }),
    field('cmp_account', 'select', '账号是否开通', 'accountReady', 'account_ready', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '未开通', value: 'no' },
        { label: '已开通', value: 'yes' },
      ],
      validation: optional,
    }),
    field('cmp_equipment', 'select', '设备是否就绪', 'equipmentReady', 'equipment_ready', 12, {
      props: { placeholder: '请选择' },
      options: [
        { label: '未就绪', value: 'no' },
        { label: '已就绪', value: 'yes' },
      ],
      validation: optional,
    }),
    field('cmp_remark', 'textarea', '备注', 'remark', 'remark', 24, {
      props: { rows: 2, placeholder: '补充说明（选填）' },
      validation: optional,
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
  name: '员工入职登记表',
  version: '1.2.0',
  payload: formPayload,
}

function objectRef(pageMode) {
  return {
    objectCode: 'employee_onboard',
    objectName: '入职登记',
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
    gridH: formOnly ? 22 : 24,
    props: {
      title,
      formAssetId: 'form_employee_onboard',
      formAssetFieldsInitialized: true,
      fieldRefs: fieldCodes,
      objectRef: objectRef(formOnly ? 'form' : 'list'),
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

const listBlock = crudBlock({ id: 'blk_onboard_list', title: '入职台账', pageMode: 'list' })
const applyBlock = crudBlock({ id: 'blk_onboard_apply', title: '办理入职', pageMode: 'form', formOnly: true })
const docsBlock = crudBlock({ id: 'blk_onboard_docs', title: '材料核对', pageMode: 'list-form' })
const unlockBlock = crudBlock({ id: 'blk_onboard_unlock', title: '开通清单', pageMode: 'list' })

/**
 * 首页 = 入职台账列表（满高单块）。
 * 办理/核对/开通已在左侧导航，不再堆重复快捷按钮。
 */
const homeLayout = pageLayout([{
  ...listBlock,
  id: 'blk_home_list',
  props: {
    ...listBlock.props,
    title: '入职台账',
  },
}], 'list')

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
            content: '点击上方统计可进入待办/已办/发起中/抄送。入职办理请从左侧「入职中心」进入。',
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
    applicationCode: 'market_onboard_app',
    applicationName: '员工入职',
    suiteCode: 'HR',
  },
  selection: {
    mode: 'FULL',
    pageIds: [
      'page_onboard_home',
      'page_onboard_list',
      'page_onboard_apply',
      'page_onboard_docs',
      'page_onboard_unlock',
    ],
    objectCodes: ['employee_onboard'],
  },
  application: {
    applicationName: '员工入职',
    applicationCode: 'market_onboard_app',
    suiteCode: 'HR',
    // 应用卡片走 IconRenderer：须用 ionicons5，勿用 i-lucide（动态 class 不会进 UnoCSS）
    icon: 'ionicons5:PersonAddOutline',
    description: '人事入职进阶样例：入职中心、台账、办理入职、材料核对与开通清单五页，字段覆盖合同、银行卡与账号准备。',
    status: 1,
    options: {
      inAppBuilder: {
        schemaVersion: 2,
        homePageId: 'page_onboard_home',
        nodes: [
          pageNode({
            id: 'page_onboard_home',
            title: '入职中心',
            pageType: 'object',
            pageShape: 'list',
            pageTemplate: 'list',
            sort: 0,
            objectRef: objectRef('list'),
          }),
          pageNode({
            id: 'page_onboard_list',
            title: '入职台账',
            pageType: 'object',
            pageShape: 'list',
            pageTemplate: 'list',
            sort: 10,
            objectRef: objectRef('list'),
          }),
          pageNode({
            id: 'page_onboard_apply',
            title: '办理入职',
            pageType: 'object',
            pageShape: 'form',
            pageTemplate: 'form',
            sort: 20,
            objectRef: objectRef('form'),
          }),
          pageNode({
            id: 'page_onboard_docs',
            title: '材料核对',
            pageType: 'object',
            pageShape: 'list-form',
            pageTemplate: 'list-form',
            sort: 30,
            objectRef: objectRef('list'),
          }),
          pageNode({
            id: 'page_onboard_unlock',
            title: '开通清单',
            pageType: 'object',
            pageShape: 'list',
            pageTemplate: 'list',
            sort: 40,
            objectRef: objectRef('list'),
          }),
        ],
        pages: {
          page_onboard_home: {
            title: '入职中心',
            description: '查询与管理入职台账',
            layout: homeLayout,
          },
          page_onboard_list: {
            title: '入职台账',
            description: '查询、编辑与导出入职登记',
            layout: pageLayout([listBlock], 'list'),
          },
          page_onboard_apply: {
            title: '办理入职',
            description: '采集新员工完整入职信息',
            layout: pageLayout([applyBlock], 'form'),
          },
          page_onboard_docs: {
            title: '材料核对',
            description: '核对证件材料与入职状态',
            layout: pageLayout([docsBlock], 'list-form'),
          },
          page_onboard_unlock: {
            title: '开通清单',
            description: '跟进账号开通与设备就绪',
            layout: pageLayout([unlockBlock], 'list'),
          },
          'system:workbench': workbenchPage,
        },
        formAssets: [
          {
            id: 'form_employee_onboard',
            formKey: 'form_employee_onboard',
            name: '员工入职登记',
            formDesignerSchema: formPayload,
          },
        ],
      },
    },
    portalConfig: {},
  },
  objects: [
    {
      objectCode: 'employee_onboard',
      objectName: '入职登记',
      objectType: 'MASTER',
      role: 'PRIMARY',
      displayField: 'fullName',
      icon: 'ionicons5:RibbonOutline',
      description: '员工入职登记对象（含合同、材料与开通状态）',
      modelSchema: {
        object: { code: 'employee_onboard', name: '入职登记' },
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
writeFileSync(join(fixtures, 'employee-onboarding.form.json'), `${JSON.stringify(formPack, null, 2)}\n`)
writeFileSync(join(fixtures, 'employee-onboarding.forge-app.json'), `${JSON.stringify(appPack, null, 2)}\n`)
let websiteWritten = false
if (writeWebsite) {
  try {
    mkdirSync(join(website, 'form'), { recursive: true })
    mkdirSync(join(website, 'app'), { recursive: true })
    copyFileSync(join(fixtures, 'employee-onboarding.form.json'), join(website, 'form/employee-onboarding.form.json'))
    copyFileSync(join(fixtures, 'employee-onboarding.forge-app.json'), join(website, 'app/employee-onboarding.forge-app.json'))
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

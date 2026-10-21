/**
 * 生成可商用的官网市场打印包，并做 forge-print 协议校验。
 * 输出到 website official-ui 与本仓 fixtures。
 */
import { mkdirSync, writeFileSync, copyFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const websiteRoot = join(__dirname, '../../../forge-website/forge-website-official-ui/public/market/packages')
const fixturesRoot = join(__dirname, '../src/utils/__tests__/fixtures/market')
if (!existsSync(dirname(websiteRoot))) {
  console.error('website packages root missing:', websiteRoot)
  process.exit(1)
}

let seq = 0
const id = (prefix = 'e') => `${prefix}_${++seq}`

function cell(row, column, value, opts = {}) {
  const {
    rowSpan = 1,
    colSpan = 1,
    binding,
    style,
    format,
  } = opts
  const out = {
    id: id('c'),
    row,
    column,
    rowSpan,
    colSpan,
    binding: binding || { source: 'CONSTANT', value: value ?? '' },
  }
  if (style)
    out.style = style
  if (format)
    out.format = format
  return out
}

const labelStyle = { fontWeight: 700, textAlign: 'center', backgroundColor: '#f1f5f9', verticalAlign: 'middle' }
const valueStyle = { paddingMm: 1.2, verticalAlign: 'middle' }
const areaStyle = { paddingMm: 2, verticalAlign: 'top' }

function buildLeaveSlip() {
  seq = 0
  const widthMm = 186
  const columns = [
    { id: id('col'), widthMm: 28 },
    { id: id('col'), widthMm: 65 },
    { id: id('col'), widthMm: 28 },
    { id: id('col'), widthMm: 65 },
  ]
  const rowHeights = [11, 11, 11, 28, 18, 26, 26, 26, 14]
  const rows = rowHeights.map(heightMm => ({ id: id('row'), heightMm }))
  const cells = [
    cell(0, 0, '申请人', { style: labelStyle }),
    cell(0, 1, '', { binding: { source: 'FIELD', path: 'main.applicantName' }, style: valueStyle }),
    cell(0, 2, '所属部门', { style: labelStyle }),
    cell(0, 3, '', { binding: { source: 'FIELD', path: 'main.department' }, style: valueStyle }),

    cell(1, 0, '请假类型', { style: labelStyle }),
    cell(1, 1, '', { binding: { source: 'FIELD', path: 'main.leaveType' }, style: valueStyle }),
    cell(1, 2, '请假天数', { style: labelStyle }),
    cell(1, 3, '', { binding: { source: 'FIELD', path: 'main.leaveDays' }, style: valueStyle, format: { type: 'NUMBER' } }),

    cell(2, 0, '开始日期', { style: labelStyle }),
    cell(2, 1, '', { binding: { source: 'FIELD', path: 'main.startDate' }, style: valueStyle, format: { type: 'DATE' } }),
    cell(2, 2, '结束日期', { style: labelStyle }),
    cell(2, 3, '', { binding: { source: 'FIELD', path: 'main.endDate' }, style: valueStyle, format: { type: 'DATE' } }),

    cell(3, 0, '请假事由', { style: labelStyle }),
    cell(3, 1, '', { colSpan: 3, binding: { source: 'FIELD', path: 'main.reason' }, style: areaStyle }),

    cell(4, 0, '工作交接', { style: labelStyle }),
    cell(4, 1, '', { colSpan: 3, binding: { source: 'FIELD', path: 'main.handover' }, style: areaStyle }),

    cell(5, 0, '直属主管\n意见', { style: { ...labelStyle, fontSizePt: 9 } }),
    cell(5, 1, '意见：\n\n签名：__________  日期：____年__月__日', { colSpan: 3, style: areaStyle }),

    cell(6, 0, '部门负责人\n意见', { style: { ...labelStyle, fontSizePt: 9 } }),
    cell(6, 1, '意见：\n\n签名：__________  日期：____年__月__日', { colSpan: 3, style: areaStyle }),

    cell(7, 0, '分管领导\n意见', { style: { ...labelStyle, fontSizePt: 9 } }),
    cell(7, 1, '意见：\n\n签名：__________  日期：____年__月__日', { colSpan: 3, style: areaStyle }),

    cell(8, 0, '人事备案', { style: labelStyle }),
    cell(8, 1, '', { binding: { source: 'FIELD', path: 'main.hrRecord' }, style: valueStyle }),
    cell(8, 2, '审批状态', { style: labelStyle }),
    cell(8, 3, '', { binding: { source: 'FIELD', path: 'main.approvalStatus' }, style: valueStyle }),
  ]
  const tableHeight = rowHeights.reduce((a, b) => a + b, 0)
  return {
    kind: 'print',
    name: '请假审批单',
    version: '1.1.0',
    payload: {
      protocol: 'forge-print',
      schemaVersion: 1,
      paper: {
        widthMm: 210,
        heightMm: 297,
        orientation: 'PORTRAIT',
        marginMm: { top: 12, right: 12, bottom: 12, left: 12 },
      },
      header: { heightMm: 0, repeat: true, elements: [] },
      body: [
        {
          id: 'sec_head',
          kind: 'FIXED',
          heightMm: 28,
          gapAfterMm: 3,
          elements: [
            {
              id: 'company',
              type: 'TEXT',
              xMm: 0,
              yMm: 0,
              widthMm: 140,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: 'Forge 示例企业 · 人力资源部' },
              style: { fontSizePt: 10, color: '#64748b' },
            },
            {
              id: 'doc_no_label',
              type: 'TEXT',
              xMm: 0,
              yMm: 9,
              widthMm: 18,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '单号' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'doc_no',
              type: 'TEXT',
              xMm: 18,
              yMm: 9,
              widthMm: 70,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.docNo' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
            {
              id: 'title',
              type: 'TEXT',
              xMm: 0,
              yMm: 16,
              widthMm: 150,
              heightMm: 12,
              binding: { source: 'CONSTANT', value: '请 假 审 批 单' },
              style: { fontSizePt: 20, fontWeight: 700, textAlign: 'center' },
            },
            {
              id: 'qr',
              type: 'QRCODE',
              xMm: 158,
              yMm: 0,
              widthMm: 28,
              heightMm: 28,
              binding: { source: 'FIELD', path: 'main.docNo' },
              showCodeText: false,
            },
          ],
        },
        {
          id: 'sec_table',
          kind: 'FIXED',
          heightMm: tableHeight + 2,
          gapAfterMm: 4,
          elements: [
            {
              id: 'leave_table',
              type: 'STATIC_TABLE',
              xMm: 0,
              yMm: 0,
              widthMm,
              heightMm: tableHeight,
              headerStyle: { backgroundColor: '#f1f5f9', fontWeight: 700 },
              style: { borderWidthMm: 0.25, borderColor: '#334155', fontSizePt: 10 },
              table: { columns, rows, cells },
            },
          ],
        },
        {
          id: 'sec_note',
          kind: 'TEXT',
          gapAfterMm: 2,
          binding: {
            source: 'CONSTANT',
            value: '填写说明：1. 请假天数按自然日/工作日以公司制度为准；2. 病假需附证明；3. 跨部门请先完成工作交接；4. 本单打印后与流程归档一致。',
          },
          style: { fontSizePt: 8, lineHeight: 1.5, color: '#64748b', paddingMm: 1 },
        },
      ],
      footer: {
        heightMm: 10,
        repeat: true,
        elements: [
          {
            id: 'ft_tip',
            type: 'TEXT',
            xMm: 0,
            yMm: 1,
            widthMm: 120,
            heightMm: 8,
            binding: { source: 'CONSTANT', value: '打印时间以系统为准 · 受控文件' },
            style: { fontSizePt: 8, color: '#94a3b8' },
          },
          {
            id: 'pn',
            type: 'PAGE_NUMBER',
            xMm: 150,
            yMm: 1,
            widthMm: 36,
            heightMm: 8,
            pageNumberFormat: 'CURRENT_TOTAL',
            style: { textAlign: 'right', fontSizePt: 8 },
          },
        ],
      },
      resources: [],
    },
  }
}

function buildPurchaseOrder() {
  seq = 0
  const widthMm = 190
  const columns = [
    { id: id('col'), widthMm: 14 },
    { id: id('col'), widthMm: 42 },
    { id: id('col'), widthMm: 38 },
    { id: id('col'), widthMm: 24 },
    { id: id('col'), widthMm: 28 },
    { id: id('col'), widthMm: 22 },
    { id: id('col'), widthMm: 22 },
  ]
  const headerLabels = ['序号', '物料名称', '规格型号', '单位', '数量', '单价', '金额']
  const rowHeights = [10, 10, 10, 10, 10, 10, 10, 12]
  const rows = rowHeights.map(heightMm => ({ id: id('row'), heightMm }))
  const cells = []
  headerLabels.forEach((label, column) => {
    cells.push(cell(0, column, label, { style: { ...labelStyle, fontSizePt: 9 } }))
  })
  for (let r = 1; r <= 5; r++) {
    for (let c = 0; c < 7; c++) {
      const value = c === 0 ? String(r) : ''
      cells.push(cell(r, c, value, { style: valueStyle }))
    }
  }
  cells.push(
    cell(6, 0, '合计（大写）', { colSpan: 4, style: labelStyle }),
    cell(6, 4, '', { colSpan: 3, binding: { source: 'FIELD', path: 'main.totalAmountUpper' }, style: valueStyle }),
    cell(7, 0, '合计（小写）', { colSpan: 4, style: labelStyle }),
    cell(7, 4, '', {
      colSpan: 3,
      binding: { source: 'FIELD', path: 'main.totalAmount' },
      style: { ...valueStyle, fontWeight: 700 },
      format: { type: 'MONEY' },
    }),
  )
  const tableHeight = rowHeights.reduce((a, b) => a + b, 0)
  return {
    kind: 'print',
    name: '采购订单打印模板',
    version: '1.1.0',
    payload: {
      protocol: 'forge-print',
      schemaVersion: 1,
      paper: {
        widthMm: 210,
        heightMm: 297,
        orientation: 'PORTRAIT',
        marginMm: { top: 10, right: 10, bottom: 10, left: 10 },
      },
      header: { heightMm: 0, repeat: true, elements: [] },
      body: [
        {
          id: 'po_head',
          kind: 'FIXED',
          heightMm: 36,
          gapAfterMm: 3,
          elements: [
            {
              id: 'po_title',
              type: 'TEXT',
              xMm: 0,
              yMm: 0,
              widthMm: 150,
              heightMm: 12,
              binding: { source: 'CONSTANT', value: '采 购 订 单' },
              style: { fontSizePt: 20, fontWeight: 700, textAlign: 'center' },
            },
            {
              id: 'po_company',
              type: 'TEXT',
              xMm: 0,
              yMm: 12,
              widthMm: 150,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '需方（盖章）：____________________' },
              style: { fontSizePt: 10 },
            },
            {
              id: 'a1',
              type: 'TEXT',
              xMm: 0,
              yMm: 22,
              widthMm: 22,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '订单号' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'b1',
              type: 'TEXT',
              xMm: 22,
              yMm: 22,
              widthMm: 50,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.orderNo' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
            {
              id: 'a2',
              type: 'TEXT',
              xMm: 76,
              yMm: 22,
              widthMm: 22,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '供应商' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'b2',
              type: 'TEXT',
              xMm: 98,
              yMm: 22,
              widthMm: 52,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.vendorName' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
            {
              id: 'qr',
              type: 'QRCODE',
              xMm: 162,
              yMm: 2,
              widthMm: 28,
              heightMm: 28,
              binding: { source: 'FIELD', path: 'main.orderNo' },
              showCodeText: false,
            },
          ],
        },
        {
          id: 'po_meta',
          kind: 'FIXED',
          heightMm: 20,
          gapAfterMm: 2,
          elements: [
            {
              id: 'm1',
              type: 'TEXT',
              xMm: 0,
              yMm: 0,
              widthMm: 22,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '采购员' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'm2',
              type: 'TEXT',
              xMm: 22,
              yMm: 0,
              widthMm: 50,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.buyerName' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
            {
              id: 'm3',
              type: 'TEXT',
              xMm: 76,
              yMm: 0,
              widthMm: 22,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '交货日' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'm4',
              type: 'TEXT',
              xMm: 98,
              yMm: 0,
              widthMm: 52,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.deliveryDate' },
              format: { type: 'DATE' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
            {
              id: 'm5',
              type: 'TEXT',
              xMm: 0,
              yMm: 10,
              widthMm: 22,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '收货地址' },
              style: { fontWeight: 700, fontSizePt: 10 },
            },
            {
              id: 'm6',
              type: 'TEXT',
              xMm: 22,
              yMm: 10,
              widthMm: 168,
              heightMm: 8,
              binding: { source: 'FIELD', path: 'main.shipAddress' },
              style: { fontSizePt: 10, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 1 },
            },
          ],
        },
        {
          id: 'po_lines',
          kind: 'FIXED',
          heightMm: tableHeight + 2,
          gapAfterMm: 3,
          elements: [
            {
              id: 'lines_table',
              type: 'STATIC_TABLE',
              xMm: 0,
              yMm: 0,
              widthMm,
              heightMm: tableHeight,
              style: { borderWidthMm: 0.25, borderColor: '#334155', fontSizePt: 9 },
              table: { columns, rows, cells },
            },
          ],
        },
        {
          id: 'po_terms',
          kind: 'TEXT',
          gapAfterMm: 3,
          binding: {
            source: 'CONSTANT',
            value: '条款：1. 交货须附质检报告；2. 逾期按日万分之三计违约金；3. 本订单经双方盖章后生效，传真/扫描件同等效力；4. 争议提交需方所在地法院。',
          },
          style: { fontSizePt: 8, lineHeight: 1.55, paddingMm: 2, borderWidthMm: 0.2, borderColor: '#cbd5e1' },
        },
        {
          id: 'po_sign',
          kind: 'FIXED',
          heightMm: 42,
          gapAfterMm: 2,
          elements: [
            {
              id: 's1',
              type: 'TEXT',
              xMm: 0,
              yMm: 0,
              widthMm: 90,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '需方确认' },
              style: { fontWeight: 700 },
            },
            {
              id: 's2',
              type: 'RECTANGLE',
              xMm: 0,
              yMm: 10,
              widthMm: 85,
              heightMm: 30,
              style: { borderWidthMm: 0.3, borderColor: '#94a3b8', borderStyle: 'dashed' },
            },
            {
              id: 's3',
              type: 'TEXT',
              xMm: 100,
              yMm: 0,
              widthMm: 90,
              heightMm: 8,
              binding: { source: 'CONSTANT', value: '供方确认' },
              style: { fontWeight: 700 },
            },
            {
              id: 's4',
              type: 'RECTANGLE',
              xMm: 100,
              yMm: 10,
              widthMm: 85,
              heightMm: 30,
              style: { borderWidthMm: 0.3, borderColor: '#94a3b8', borderStyle: 'dashed' },
            },
          ],
        },
      ],
      footer: {
        heightMm: 10,
        repeat: true,
        elements: [
          {
            id: 'pn',
            type: 'PAGE_NUMBER',
            xMm: 150,
            yMm: 1,
            widthMm: 40,
            heightMm: 8,
            pageNumberFormat: 'CURRENT_TOTAL',
            style: { textAlign: 'right', fontSizePt: 8 },
          },
        ],
      },
      resources: [],
    },
  }
}

function buildApprovalStatus() {
  seq = 0
  const steps = [
    { title: '提交申请', role: '申请人', field: 'main.applicantName', status: '已提交' },
    { title: '部门审批', role: '直属主管', field: 'main.deptApprover', status: '待审批' },
    { title: '分管领导', role: '分管领导', field: 'main.leaderApprover', status: '—' },
    { title: '人事备案', role: '人事专员', field: 'main.hrApprover', status: '—' },
  ]
  const elements = [
    {
      id: id('t'),
      type: 'TEXT',
      xMm: 0,
      yMm: 0,
      widthMm: 186,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '审批流程' },
      style: { fontWeight: 700, fontSizePt: 11 },
    },
  ]
  steps.forEach((step, index) => {
    const x = index * 47
    elements.push(
      {
        id: id('box'),
        type: 'RECTANGLE',
        xMm: x,
        yMm: 10,
        widthMm: 44,
        heightMm: 36,
        style: { borderWidthMm: 0.35, borderColor: '#64748b', borderStyle: 'solid', backgroundColor: '#f8fafc' },
      },
      {
        id: id('step'),
        type: 'TEXT',
        xMm: x + 2,
        yMm: 12,
        widthMm: 40,
        heightMm: 7,
        binding: { source: 'CONSTANT', value: `${index + 1}. ${step.title}` },
        style: { fontWeight: 700, fontSizePt: 9, textAlign: 'center' },
      },
      {
        id: id('role'),
        type: 'TEXT',
        xMm: x + 2,
        yMm: 20,
        widthMm: 40,
        heightMm: 6,
        binding: { source: 'CONSTANT', value: step.role },
        style: { fontSizePt: 8, color: '#64748b', textAlign: 'center' },
      },
      {
        id: id('who'),
        type: 'TEXT',
        xMm: x + 2,
        yMm: 27,
        widthMm: 40,
        heightMm: 7,
        binding: { source: 'FIELD', path: step.field },
        style: { fontSizePt: 9, textAlign: 'center', borderWidthMm: 0.15, borderColor: '#cbd5e1', paddingMm: 0.5 },
      },
      {
        id: id('st'),
        type: 'TEXT',
        xMm: x + 2,
        yMm: 36,
        widthMm: 40,
        heightMm: 7,
        binding: { source: 'CONSTANT', value: step.status },
        style: { fontSizePt: 9, fontWeight: 700, textAlign: 'center', color: index === 0 ? '#15803d' : '#b45309' },
      },
    )
    if (index < steps.length - 1) {
      elements.push({
        id: id('arrow'),
        type: 'TEXT',
        xMm: x + 42,
        yMm: 24,
        widthMm: 6,
        heightMm: 8,
        binding: { source: 'CONSTANT', value: '→' },
        style: { fontSizePt: 12, textAlign: 'center', color: '#94a3b8' },
      })
    }
  })
  elements.push(
    {
      id: id('cur'),
      type: 'TEXT',
      xMm: 0,
      yMm: 50,
      widthMm: 28,
      heightMm: 9,
      binding: { source: 'CONSTANT', value: '当前状态' },
      style: { fontWeight: 700, fontSizePt: 10 },
    },
    {
      id: id('curv'),
      type: 'TEXT',
      xMm: 28,
      yMm: 50,
      widthMm: 50,
      heightMm: 9,
      binding: { source: 'FIELD', path: 'main.approvalStatus' },
      style: { fontSizePt: 10, borderWidthMm: 0.2, borderColor: '#94a3b8', paddingMm: 1 },
    },
    {
      id: id('tm'),
      type: 'TEXT',
      xMm: 86,
      yMm: 50,
      widthMm: 28,
      heightMm: 9,
      binding: { source: 'CONSTANT', value: '更新时间' },
      style: { fontWeight: 700, fontSizePt: 10 },
    },
    {
      id: id('tmv'),
      type: 'TEXT',
      xMm: 114,
      yMm: 50,
      widthMm: 72,
      heightMm: 9,
      binding: { source: 'FIELD', path: 'main.approvalTime' },
      format: { type: 'DATE' },
      style: { fontSizePt: 10, borderWidthMm: 0.2, borderColor: '#94a3b8', paddingMm: 1 },
    },
  )
  return {
    kind: 'print-component',
    name: '审批流程状态条',
    version: '1.1.0',
    payload: { elements },
  }
}

function buildContractSeal() {
  seq = 0
  const mkParty = (x, title) => ([
    {
      id: id('t'),
      type: 'TEXT',
      xMm: x,
      yMm: 0,
      widthMm: 90,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: title },
      style: { fontWeight: 700, fontSizePt: 11 },
    },
    {
      id: id('n'),
      type: 'TEXT',
      xMm: x,
      yMm: 10,
      widthMm: 22,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '单位名称' },
      style: { fontSizePt: 9, fontWeight: 700 },
    },
    {
      id: id('nv'),
      type: 'TEXT',
      xMm: x + 22,
      yMm: 10,
      widthMm: 68,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '' },
      style: { fontSizePt: 9, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 0.8 },
    },
    {
      id: id('p'),
      type: 'TEXT',
      xMm: x,
      yMm: 20,
      widthMm: 22,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '法定代表人' },
      style: { fontSizePt: 9, fontWeight: 700 },
    },
    {
      id: id('pv'),
      type: 'TEXT',
      xMm: x + 22,
      yMm: 20,
      widthMm: 68,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '' },
      style: { fontSizePt: 9, borderWidthMm: 0.15, borderColor: '#94a3b8', paddingMm: 0.8 },
    },
    {
      id: id('box'),
      type: 'RECTANGLE',
      xMm: x,
      yMm: 32,
      widthMm: 50,
      heightMm: 36,
      style: { borderWidthMm: 0.35, borderColor: '#b45309', borderStyle: 'dashed' },
    },
    {
      id: id('tip'),
      type: 'TEXT',
      xMm: x + 2,
      yMm: 46,
      widthMm: 46,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '（盖章处）' },
      style: { fontSizePt: 9, textAlign: 'center', color: '#b45309' },
    },
    {
      id: id('d'),
      type: 'TEXT',
      xMm: x + 54,
      yMm: 32,
      widthMm: 36,
      heightMm: 8,
      binding: { source: 'CONSTANT', value: '签署日期' },
      style: { fontSizePt: 9, fontWeight: 700 },
    },
    {
      id: id('dv'),
      type: 'TEXT',
      xMm: x + 54,
      yMm: 42,
      widthMm: 36,
      heightMm: 18,
      binding: { source: 'CONSTANT', value: '____年__月__日' },
      style: { fontSizePt: 9, lineHeight: 1.4 },
    },
  ])
  return {
    kind: 'print-component',
    name: '合同甲乙方签章区',
    version: '1.1.0',
    payload: {
      elements: [
        ...mkParty(0, '甲方（盖章）'),
        ...mkParty(100, '乙方（盖章）'),
        {
          id: id('note'),
          type: 'TEXT',
          xMm: 0,
          yMm: 74,
          widthMm: 190,
          heightMm: 8,
          binding: { source: 'CONSTANT', value: '备注：骑缝章需覆盖合同骑缝；电子章须可核验；签字人应与授权书一致。' },
          style: { fontSizePt: 8, color: '#64748b' },
        },
      ],
    },
  }
}

function writePack(relPath, data) {
  const abs = join(websiteRoot, relPath)
  mkdirSync(dirname(abs), { recursive: true })
  writeFileSync(abs, `${JSON.stringify(data, null, 2)}\n`)
  return abs
}

const leave = buildLeaveSlip()
const po = buildPurchaseOrder()
const approval = buildApprovalStatus()
const seal = buildContractSeal()

writePack('print/leave-slip.print.json', leave)
writePack('print/purchase-order.print.json', po)
writePack('print-component/approval-status.print-component.json', approval)
writePack('print-component/contract-seal.print-component.json', seal)

mkdirSync(fixturesRoot, { recursive: true })
copyFileSync(join(websiteRoot, 'print/leave-slip.print.json'), join(fixturesRoot, 'leave-slip.print.json'))
copyFileSync(join(websiteRoot, 'print-component/approval-status.print-component.json'), join(fixturesRoot, 'approval-status.print-component.json'))

console.log(JSON.stringify({
  websiteRoot,
  leaveSections: leave.payload.body.length,
  leaveTableCells: leave.payload.body[1].elements[0].table.cells.length,
  poTableCells: po.payload.body[2].elements[0].table.cells.length,
  approvalElements: approval.payload.elements.length,
  sealElements: seal.payload.elements.length,
}))

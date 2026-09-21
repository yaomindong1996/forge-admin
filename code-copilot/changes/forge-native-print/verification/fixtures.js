import { createPrintDocument } from '../../../../forge-admin-ui/src/components/print/protocol/types'

export function fixture(count = 100) {
  const template = createPrintDocument()
  template.header = { heightMm: 12, repeat: true, elements: [{ id: 'title', type: 'TEXT', xMm: 0, yMm: 0, widthMm: 190, heightMm: 12, binding: { source: 'CONSTANT', value: '采购明细单 · 合成验证数据' }, style: { fontSizePt: 16, fontWeight: 700, textAlign: 'center' } }] }
  template.footer = { heightMm: 8, repeat: true, elements: [{ id: 'pages', type: 'PAGE_NUMBER', xMm: 160, yMm: 1, widthMm: 30, heightMm: 6, pageNumberFormat: 'CURRENT_TOTAL', style: { textAlign: 'right' } }] }
  template.body = [
    { id: 'caption', kind: 'TEXT', binding: { source: 'CONSTANT', value: '当前已保存数据 / 生成时间 2026-09-18 12:00:00\n含零金额、中文标点、多行明细和长整数。此处数据全部为合成样本。' }, gapAfterMm: 3 },
    { id: 'codes', kind: 'FIXED', heightMm: 30, gapAfterMm: 3, elements: [
      { id: 'barcode', type: 'BARCODE', xMm: 0, yMm: 0, widthMm: 70, heightMm: 20, binding: { source: 'CONSTANT', value: 'FORGE-0001' } },
      { id: 'qrcode', type: 'QRCODE', xMm: 80, yMm: 0, widthMm: 25, heightMm: 25, binding: { source: 'CONSTANT', value: 'forge-print-verification' } },
      { id: 'synthetic-image', type: 'IMAGE', xMm: 120, yMm: 0, widthMm: 20, heightMm: 20, binding: { source: 'CONSTANT', value: 'synthetic-image' } },
    ] },
    { id: 'items', kind: 'TABLE', collectionPath: 'children.items', repeatHeader: true, gapAfterMm: 3,
      columns: [
        { id: 'seq', field: 'seq', title: '序号', widthMm: 20 },
        { id: 'name', field: 'name', title: '名称 / 说明', widthMm: 115 },
        { id: 'amount', field: 'amount', title: '金额（元）', widthMm: 55, format: { type: 'MONEY' }, style: { textAlign: 'right' } },
      ],
      headerRows: [{ cells: [{ span: 3, text: '采购项目明细', style: { fontWeight: 700, textAlign: 'center' } }] }, { cells: [{ span: 1, text: '序号' }, { span: 1, text: '名称 / 说明' }, { span: 1, text: '金额（元）' }] }],
      footer: { cells: [{ span: 2, binding: { source: 'CONSTANT', value: '合计' } }, { span: 1, binding: { source: 'FIELD', path: 'main.total' }, format: { type: 'MONEY' }, style: { textAlign: 'right' } }] },
    },
    { id: 'notes', kind: 'TEXT', binding: { source: 'CONSTANT', value: '中文长文本验证：括号（测试）、逗号，句号。English words preserve readable content.\n'.repeat(8) }, style: { fontSizePt: 10 } },
  ]
  const rows = Array.from({ length: count }, (_, i) => ({ seq: String(i + 1), name: `合成物料 ${i + 1}${i % 17 === 0 ? '\n第二行规格说明，用于验证明细整行换页。' : ''}`, amount: String(i * 123) }))
  const context = { main: { total: String(rows.reduce((sum, row) => sum + BigInt(row.amount), 0n)) }, children: { items: rows }, system: { generatedAt: '2026-09-18 12:00:00' } }
  const catalog = [{ path: 'main.total', type: 'MONEY' }, { path: 'children.items', type: 'COLLECTION' }, ...['seq', 'name', 'amount'].map(field => ({ path: `children.items.${field}`, type: 'TEXT' }))]
  return { template, context, catalog }
}

export async function resolveSyntheticImage() {
  const canvas = document.createElement('canvas')
  canvas.width = canvas.height = 100
  const context = canvas.getContext('2d')
  context.fillStyle = '#dce8fa'
  context.fillRect(0, 0, 100, 100)
  context.fillStyle = '#173d75'
  context.fillRect(20, 20, 60, 60)
  return new Promise(resolve => canvas.toBlob(resolve, 'image/png'))
}

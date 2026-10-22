import { describe, expect, it } from 'vitest'
import {
  collectLayoutSubTableKeys,
  filterChildrenConfigNotCoveredByLayout,
  schemaHasSubTableNode,
  stripChildrenCoveredSubTables,
} from '../strip-children-covered-subtables'

describe('filterChildrenConfigNotCoveredByLayout (prefer layout position)', () => {
  /** 复现企业档案：联系信息标题 → 子表 → 银行账号标题（扁平叶子，非子节点包裹） */
  const companyFormSchema = [
    { componentKey: 'title', label: '基础信息', layout: { span: 3 } },
    { componentKey: 'title', label: '联系信息', layout: { span: 3 } },
    {
      componentKey: 'subTable',
      label: '关联子表',
      props: {
        header: '企业联系人',
        relationKey: 'biz_company_contact',
        modelCode: 'biz_company_contact',
        columns: [{ fieldCode: 'contactName', fieldLabel: '姓名' }],
      },
      layout: { span: 3 },
    },
    { componentKey: 'title', label: '银行账号', layout: { span: 3 } },
  ]

  const childrenConfig = [
    {
      key: 'biz_company_contact',
      relationKey: 'biz_company_contact',
      title: '企业联系人',
      fields: [{ field: 'contactName', label: '姓名' }],
    },
    {
      key: 'bank_account',
      relationKey: 'bank_account',
      title: '银行账号',
      fields: [{ field: 'accountNo', label: '账号' }],
    },
  ]

  it('keeps layout subTable in schema order (联系信息后、银行账号前)', () => {
    expect(schemaHasSubTableNode(companyFormSchema)).toBe(true)
    const keys = collectLayoutSubTableKeys(companyFormSchema)
    expect(keys.has('biz_company_contact')).toBe(true)
    const subIndex = companyFormSchema.findIndex(n => n.componentKey === 'subTable')
    const bankIndex = companyFormSchema.findIndex(n => n.label === '银行账号')
    const contactTitleIndex = companyFormSchema.findIndex(n => n.label === '联系信息')
    expect(contactTitleIndex).toBeLessThan(subIndex)
    expect(subIndex).toBeLessThan(bankIndex)
  })

  it('drops bottom childrenConfig entries already inlined in layout', () => {
    const bottom = filterChildrenConfigNotCoveredByLayout(childrenConfig, companyFormSchema)
    expect(bottom).toHaveLength(1)
    expect(bottom[0].key).toBe('bank_account')
  })

  it('keeps all childrenConfig when layout has no subTable', () => {
    const bottom = filterChildrenConfigNotCoveredByLayout(childrenConfig, [
      { componentKey: 'title', label: '基础信息' },
    ])
    expect(bottom).toEqual(childrenConfig)
  })
})

describe('stripChildrenCoveredSubTables (legacy)', () => {
  const childrenConfig = [
    {
      key: 'product_detail',
      relationKey: 'product_detail',
      title: '产品明细',
      fields: [{ field: 'productName', label: '产品名称' }],
    },
  ]

  it('removes layout subTable nodes covered by childrenConfig', () => {
    const schema = [
      { nodeType: 'field', field: 'orderNo', label: '平台订单号' },
      {
        nodeType: 'divider',
        label: '产品明细',
        children: [
          {
            nodeType: 'subTable',
            key: 'product_detail',
            props: {
              relationKey: 'product_detail',
              header: '产品明细',
              columns: [
                { field: 'fieldInput', label: 'fieldInput' },
                { field: 'fieldInput2', label: 'fieldInput2' },
              ],
            },
          },
        ],
      },
    ]

    const next = stripChildrenCoveredSubTables(schema, childrenConfig)
    expect(schemaHasSubTableNode(next)).toBe(false)
    expect(next).toHaveLength(1)
    expect(next[0]).toMatchObject({ field: 'orderNo' })
  })

  it('matches childrenConfig key against layout subTable modelCode', () => {
    const schema = [
      {
        nodeType: 'subTable',
        props: { modelCode: 'product_detail', columns: [{ field: 'fieldInput' }] },
      },
    ]
    const next = stripChildrenCoveredSubTables(schema, [
      { key: 'product_detail', fields: [{ field: 'productName' }] },
    ])
    expect(next).toEqual([])
  })

  it('keeps unrelated layout subTables when childrenConfig does not cover them', () => {
    const schema = [
      {
        nodeType: 'subTable',
        props: { relationKey: 'other_items', columns: [{ field: 'name' }] },
      },
    ]
    const next = stripChildrenCoveredSubTables(schema, childrenConfig)
    expect(next).toHaveLength(1)
    expect(next[0].props.relationKey).toBe('other_items')
  })

  it('returns original schema when childrenConfig is empty', () => {
    const schema = [
      { nodeType: 'subTable', props: { relationKey: 'product_detail' } },
    ]
    expect(stripChildrenCoveredSubTables(schema, [])).toBe(schema)
  })

  it('keeps leaf section titles that were never wrappers of a subTable', () => {
    const schema = [
      { nodeType: 'divider', label: '基本信息' },
      { nodeType: 'field', field: 'orderNo' },
    ]
    const next = stripChildrenCoveredSubTables(schema, childrenConfig)
    expect(next).toEqual(schema)
  })
})

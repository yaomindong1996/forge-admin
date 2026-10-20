import { describe, expect, it } from 'vitest'
import {
  schemaHasSubTableNode,
  stripChildrenCoveredSubTables,
} from '../strip-children-covered-subtables'

describe('stripChildrenCoveredSubTables', () => {
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

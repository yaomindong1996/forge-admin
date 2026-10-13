import { describe, expect, it } from 'vitest'
import {
  patchCrudGridLayoutItems,
  repairAiCrudTableFieldRefs,
  resolveCrudPreviewFieldBindings,
  resolvePageSchemaTableFieldRefs,
  resolvePreferredTableFieldRefs,
  resolveTableColumnFieldRefs,
} from '../list-page-field-bindings'

describe('list-page-field-bindings', () => {
  it('keeps table and search field refs separated for crud preview', () => {
    expect(resolveCrudPreviewFieldBindings({
      tableFieldRefs: ['id', 'customerName'],
      searchFieldRefs: ['fieldMoney'],
      fallbackTableFields: [{ field: 'id' }],
    })).toEqual({
      fieldRefs: ['id', 'customerName'],
      searchFieldRefs: ['fieldMoney'],
    })

    // 表格区未选字段时回退默认列表字段，不能被查询字段顶替
    expect(resolveCrudPreviewFieldBindings({
      tableFieldRefs: [],
      searchFieldRefs: ['fieldMoney'],
      fallbackTableFields: [{ field: 'id' }, { field: 'customerName' }],
    })).toEqual({
      fieldRefs: ['id', 'customerName'],
      searchFieldRefs: ['fieldMoney'],
    })
  })

  it('does not overwrite AiCrudPage table fieldRefs when syncing search zone', () => {
    const items = patchCrudGridLayoutItems([
      {
        blockType: 'AiCrudPage',
        fieldRefs: ['id', 'customerName'],
        props: { searchFieldRefs: ['oldQuery'], fieldSettings: { id: { visible: true } } },
      },
      {
        blockType: 'search-form',
        fieldRefs: ['oldQuery'],
        props: {},
      },
    ], {
      zoneKey: 'search',
      blockTypes: ['AiCrudPage', 'search-form'],
      fieldRefs: ['fieldMoney'],
      props: { fieldSettings: { fieldMoney: { queryType: 'like' } } },
    })

    expect(items[0].fieldRefs).toEqual(['id', 'customerName'])
    expect(items[0].props.searchFieldRefs).toEqual(['fieldMoney'])
    expect(items[0].props.fieldSettings).toEqual({ id: { visible: true } })
    expect(items[0].props.searchFieldSettings).toEqual({ fieldMoney: { queryType: 'like' } })
    expect(items[1].fieldRefs).toEqual(['fieldMoney'])
  })

  it('repairs table fieldRefs that were overwritten by search refs', () => {
    expect(repairAiCrudTableFieldRefs(
      ['fieldMoney'],
      ['fieldMoney'],
      ['id', 'customerName'],
    )).toEqual(['id', 'customerName'])

    expect(repairAiCrudTableFieldRefs(
      ['id', 'customerName'],
      ['fieldMoney'],
      ['id'],
    )).toEqual(['id', 'customerName'])
  })

  it('keeps explicit list selection and does not expand to wider tableZone columns', () => {
    expect(resolveTableColumnFieldRefs({
      fieldRefs: ['fieldInput'],
      searchFieldRefs: ['fieldMoney'],
      columns: [
        { field: 'id', title: 'ID' },
        { field: 'fieldInput', title: '输入框' },
        { field: 'createTime', title: '创建时间' },
      ],
      designPreview: true,
    })).toEqual(['fieldInput'])
  })

  it('uses pageSchema table refs when page block has extra polluted columns like fieldMoney', () => {
    expect(resolvePreferredTableFieldRefs({
      configuredFieldRefs: ['fieldInput', 'fieldMoney'],
      pageSchemaTableRefs: ['fieldInput'],
      searchFieldRefs: ['fieldMoney'],
    })).toEqual(['fieldInput'])

    // 块是表格区子集时保留更窄勾选
    expect(resolvePreferredTableFieldRefs({
      configuredFieldRefs: ['fieldInput'],
      pageSchemaTableRefs: ['id', 'fieldInput', 'createTime'],
      searchFieldRefs: ['fieldMoney'],
    })).toEqual(['fieldInput'])
  })

  it('reads table fieldRefs from published pageSchema for portal runtime', () => {
    expect(resolvePageSchemaTableFieldRefs({
      zones: [
        { zoneKey: 'search', fieldRefs: ['fieldMoney'] },
        { zoneKey: 'table', fieldRefs: ['id', 'fieldInput'] },
      ],
    })).toEqual(['id', 'fieldInput'])

    // 无 table zone、且 fieldRefs 与 search 撞车又无默认列时，保留原值（由上层再兜底）
    expect(resolvePageSchemaTableFieldRefs({
      listGridLayout: {
        items: [{
          blockType: 'AiCrudPage',
          fieldRefs: ['fieldInput', 'fieldMoney'],
          props: { searchFieldRefs: ['fieldMoney'] },
        }],
      },
    })).toEqual(['fieldInput', 'fieldMoney'])
  })
})

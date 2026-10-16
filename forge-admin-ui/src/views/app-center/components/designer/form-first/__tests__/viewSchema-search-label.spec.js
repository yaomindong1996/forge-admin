import { describe, expect, it } from 'vitest'
import { createViewSchemaFromPageSchema } from '../viewSchema'

describe('createViewSchemaFromPageSchema search/list labels', () => {
  const fields = [
    { field: 'id', fieldCode: 'id', label: 'ID', searchable: true, listVisible: true },
    { field: 'fieldInput', fieldCode: 'fieldInput', label: 'E2E_input', searchable: true, listVisible: true },
  ]

  it('keeps custom searchFieldSettings.label when building viewSchema', () => {
    const pageSchema = {
      listGridLayout: {
        items: [{
          blockType: 'AiCrudPage',
          fieldRefs: ['id', 'fieldInput'],
          props: {
            searchFieldRefs: ['id', 'fieldInput'],
            searchFieldSettings: {
              id: { label: '主键ID_测', queryType: 'eq', componentType: 'input' },
              fieldInput: { label: '输入框_测', queryType: 'like', componentType: 'input' },
            },
            fieldSettings: {
              id: { title: '列_主键', align: 'left' },
              fieldInput: { title: '列_输入', align: 'left' },
            },
          },
        }],
      },
      zones: [],
    }

    const view = createViewSchemaFromPageSchema(pageSchema, fields, {})
    expect(view.search.fields.map(item => [item.fieldCode, item.label])).toEqual([
      ['id', '主键ID_测'],
      ['fieldInput', '输入框_测'],
    ])
    expect(view.list.columns.map(item => [item.fieldCode, item.label])).toEqual([
      ['id', '列_主键'],
      ['fieldInput', '列_输入'],
    ])
  })
})

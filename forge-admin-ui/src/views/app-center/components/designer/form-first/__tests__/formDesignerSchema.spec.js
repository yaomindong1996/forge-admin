import { describe, expect, it } from 'vitest'
import { createForgeFieldTemplateComponent } from '../../forge-form-designer/designerLayoutFactory'
import { buildAutoFieldAssets } from '../autoFieldRegistry'
import {
  appendDesignerLayoutChild,
  createComponentFromField,
  createDefaultFormDesignerSchema,
  getDesignerComponent,
  insertDesignerComponent,
  normalizeFormDesignerSchema,
  normalizeFormDesignerSchemaForSave,
  presentFormDesignerSchema,
  updateDesignerComponent,
} from '../formDesignerSchema'

describe('formDesignerSchema', () => {
  it('keeps template component ids unique after editing the first component field binding', () => {
    let schema = normalizeFormDesignerSchema({
      formKey: 'test_form',
      formName: '测试表单',
      layout: { gridColumns: 2 },
      components: [],
    })

    const first = createForgeFieldTemplateComponent({ componentKey: 'input', label: '输入框' }, schema)
    schema = insertDesignerComponent(schema, { parentId: '', index: 0 }, first)
    const firstId = schema.components[0].id

    schema = updateDesignerComponent(schema, firstId, {
      label: '客户名称',
      fieldBinding: {
        ...schema.components[0].fieldBinding,
        fieldCode: 'customerName',
        columnName: 'customer_name',
      },
    })

    const second = createForgeFieldTemplateComponent({ componentKey: 'input', label: '输入框' }, schema)
    expect(second.id).not.toBe(firstId)

    schema = insertDesignerComponent(schema, { parentId: '', index: 1 }, second)
    schema = updateDesignerComponent(schema, second.id, { label: '第二个输入框' })

    expect(getDesignerComponent(schema, firstId).label).toBe('客户名称')
    expect(getDesignerComponent(schema, second.id).label).toBe('第二个输入框')
  })

  it('deduplicates repeated explicit component ids during normalization', () => {
    const schema = normalizeFormDesignerSchema({
      components: [
        {
          id: 'cmp_duplicate',
          componentKey: 'input',
          label: '字段一',
          fieldBinding: { fieldCode: 'fieldOne' },
        },
        {
          id: 'cmp_duplicate',
          componentKey: 'input',
          label: '字段二',
          fieldBinding: { fieldCode: 'fieldTwo' },
        },
      ],
    })

    expect(schema.components.map(item => item.id)).toEqual(['cmp_duplicate', 'cmp_duplicate_2'])
  })

  it('creates field assets for newly dragged bound field components', () => {
    const { fields, createdFields } = buildAutoFieldAssets({
      components: [
        {
          id: 'cmp_input',
          componentKey: 'input',
          label: '编码',
          fieldBinding: {
            mode: 'field',
            fieldCode: 'code',
            columnName: 'code',
            createIfMissing: true,
          },
        },
      ],
    }, [])

    expect(createdFields).toHaveLength(1)
    expect(fields[0]).toMatchObject({
      fieldName: '编码',
      fieldCode: 'code',
      columnName: 'code',
      fieldType: 'TEXT',
      dataType: 'varchar',
      componentType: 'input',
    })
  })

  it('treats snake_case binds as the same field as existing camelCase codes', () => {
    const { fields, createdFields, formDesignerSchema } = buildAutoFieldAssets({
      components: [
        {
          id: 'cmp_ent_name',
          componentKey: 'input',
          label: '企业全称',
          fieldBinding: {
            mode: 'field',
            fieldCode: 'ent_name',
            columnName: 'ent_name',
            createIfMissing: true,
          },
        },
        {
          id: 'cmp_credit',
          componentKey: 'input',
          label: '统一社会信用代码',
          fieldBinding: {
            mode: 'field',
            fieldCode: 'unified_credit_code',
            columnName: 'unified_credit_code',
            createIfMissing: true,
          },
        },
      ],
    }, [
      { fieldCode: 'entName', fieldName: '企业全称', columnName: 'ent_name' },
      { fieldCode: 'unifiedCreditCode', fieldName: '统一社会信用代码', columnName: 'unified_credit_code' },
    ])

    expect(createdFields).toHaveLength(0)
    expect(fields.map(field => field.fieldCode)).toEqual(['entName', 'unifiedCreditCode'])
    expect(formDesignerSchema.components.map(item => item.fieldBinding.fieldCode))
      .toEqual(['entName', 'unifiedCreditCode'])
  })

  it('canonicalizes newly created snake_case binds to camelCase field codes', () => {
    const { fields, createdFields, formDesignerSchema } = buildAutoFieldAssets({
      components: [{
        id: 'cmp_scope',
        componentKey: 'textarea',
        label: '经营范围',
        fieldBinding: {
          mode: 'field',
          fieldCode: 'business_scope',
          columnName: 'business_scope',
          createIfMissing: true,
        },
      }],
    }, [])

    expect(createdFields).toHaveLength(1)
    expect(fields[0].fieldCode).toBe('businessScope')
    expect(formDesignerSchema.components[0].fieldBinding.fieldCode).toBe('businessScope')
    expect(formDesignerSchema.components[0].fieldBinding.columnName).toBe('business_scope')
  })

  it('preserves the business-data structure lock when reusing an object field', () => {
    const component = createComponentFromField({
      field: 'customerName',
      fieldName: '客户名称',
      componentType: 'input',
      fieldBinding: { locked: true },
    })

    expect(component.fieldBinding).toMatchObject({
      fieldCode: 'customerName',
      createIfMissing: false,
      locked: true,
    })
  })

  it('adds every new tab pane to the selected tabs component', () => {
    let schema = normalizeFormDesignerSchema({
      components: [{
        id: 'tabs_main',
        componentKey: 'tabs',
        label: '信息页签',
        children: [{
          id: 'tab_first',
          componentKey: 'tabPane',
          label: '标签 1',
          props: { label: '标签 1', name: 'tab_first' },
          children: [],
        }],
      }],
    })

    schema = appendDesignerLayoutChild(schema, 'tabs_main', 'tabPane')
    schema = appendDesignerLayoutChild(schema, 'tabs_main', 'tabPane')

    const tabs = getDesignerComponent(schema, 'tabs_main')
    expect(tabs.children).toHaveLength(3)
    expect(tabs.children.map(item => item.componentKey)).toEqual(['tabPane', 'tabPane', 'tabPane'])
    expect(new Set(tabs.children.map(item => item.id)).size).toBe(3)
    expect(tabs.children.every(item => Array.isArray(item.children))).toBe(true)
  })

  it('preserves managed offline draft governance through schema normalization', () => {
    const schema = normalizeFormDesignerSchema({
      formKey: 'presale_form',
      settings: {
        governance: {
          offlineDraft: {
            enabled: true,
            formCode: 'presale_form',
            replayActionCode: 'submit_presale',
            recordVersionField: 'updateTime',
          },
        },
      },
    })

    expect(schema.settings.governance.offlineDraft).toEqual({
      enabled: true,
      formCode: 'presale_form',
      replayActionCode: 'submit_presale',
      recordVersionField: 'updateTime',
    })
  })

  it('preserves the presale page sections and bottom bar without mutating the source', () => {
    const source = {
      formKey: 'ps_presale_order_form',
      components: [
        { id: 'pay_method', componentKey: 'dictSelect', label: '收款方式', fieldBinding: { fieldCode: 'payMethod' } },
      ],
      pageSections: [
        {
          sectionId: 'payment',
          sectionType: 'card',
          title: '收款信息',
          fields: ['payMethod', 'cashAmount'],
          fieldOverrides: {
            payMethod: { componentKey: 'pillSelect', props: { clearable: false } },
          },
          collapsible: false,
          visibleInModes: ['create', 'edit', 'detail'],
        },
        {
          sectionId: 'presale_items',
          sectionType: 'child_table',
          relationKey: 'presale_items',
          title: '商品明细',
          displayMode: 'inline_grid',
          visibleInModes: ['create', 'edit', 'detail'],
        },
      ],
      bottomBar: {
        sticky: true,
        actions: [
          { type: 'reset', label: '清空', variant: 'secondary' },
          {
            type: 'action',
            actionCode: 'submit_presale',
            label: '提交',
            variant: 'primary',
            displayCondition: 'status == DRAFT',
            confirmText: '确认提交当前预售单？',
            successMessage: '预售单已提交',
          },
        ],
      },
    }
    const snapshot = structuredClone(source)

    const schema = normalizeFormDesignerSchema(source)

    expect(source).toEqual(snapshot)
    expect(schema.pageSections).toEqual(snapshot.pageSections)
    expect(schema.bottomBar).toEqual(snapshot.bottomBar)
  })

  it('keeps section protocol in every form when normalizing for save', () => {
    const saved = normalizeFormDesignerSchemaForSave({
      formKey: 'main_form',
      defaultFormKey: 'main_form',
      components: [],
      pageSections: [{ sectionId: 'main', sectionType: 'card', title: '主表', fields: ['code'] }],
      bottomBar: { actions: [{ type: 'save', label: '保存' }] },
      settings: {
        formAssets: [{
          formKey: 'detail_form',
          formName: '明细表单',
          schema: {
            formKey: 'detail_form',
            components: [],
            pageSections: [{ sectionId: 'detail', sectionType: 'card', title: '明细', fields: ['name'] }],
            bottomBar: { actions: [{ type: 'cancel', label: '返回' }] },
          },
        }],
      },
    })

    expect(saved.pageSections[0]).toMatchObject({ sectionId: 'main', fields: ['code'] })
    expect(saved.bottomBar.actions[0]).toMatchObject({ type: 'save', label: '保存' })
    const detail = saved.forms.find(form => form.formKey === 'detail_form')
    expect(detail.schema.pageSections[0]).toMatchObject({ sectionId: 'detail', fields: ['name'] })
    expect(detail.schema.bottomBar.actions[0]).toMatchObject({ type: 'cancel', label: '返回' })
  })

  it('migrates legacy show-hide interactions to target runtime rules', () => {
    const schema = normalizeFormDesignerSchema({
      components: [
        {
          id: 'source',
          componentKey: 'select',
          fieldBinding: { fieldCode: 'deliveryMode' },
          props: {
            __events: [{ id: 'show_pickup', action: 'showHide', targetId: 'pickup', whenValue: 'PICKUP', value: 'true' }],
          },
        },
        {
          id: 'pickup',
          componentKey: 'input',
          fieldBinding: { fieldCode: 'pickupAddress' },
        },
      ],
    })

    expect(schema.components[1].props.runtimeRules).toEqual([
      expect.objectContaining({
        legacyEventId: 'legacy:source:show_pickup:showHide',
        conditions: [{ source: 'formData', field: 'deliveryMode', operator: 'eq', value: 'PICKUP' }],
        effect: { visible: true, whenUnmatched: 'hidden' },
      }),
    ])

    schema.components[0].props.__events = []
    const normalizedAfterRemoval = normalizeFormDesignerSchema(schema)
    expect(normalizedAfterRemoval.components[1].props.runtimeRules).toEqual([])
  })

  it('keeps hidden field assets when they carry visibility rules', () => {
    const schema = createDefaultFormDesignerSchema({
      fields: [{
        field: 'pickupAddress',
        fieldName: '取货地址',
        formVisible: false,
        basicProps: {
          runtimeRules: [{ conditions: [{ field: 'deliveryMode', value: 'PICKUP' }], effect: { visible: true } }],
        },
      }],
    })

    expect(schema.components.map(component => component.fieldBinding.fieldCode)).toContain('pickupAddress')
  })

  it('lifts the default form components when the root canvas is empty', () => {
    const schema = presentFormDesignerSchema({
      formKey: 'main_form',
      formName: '主表单',
      components: [],
      defaultFormKey: 'detail_form',
      forms: [{
        formKey: 'detail_form',
        formName: '明细表单',
        schema: {
          components: [{ id: 'field_input', componentKey: 'input', fieldBinding: { fieldCode: 'fieldInput' } }],
        },
      }],
    })

    expect(schema.components.map(component => component.fieldBinding.fieldCode)).toEqual(['fieldInput'])
    expect(schema.formName).toBe('明细表单')
  })
})

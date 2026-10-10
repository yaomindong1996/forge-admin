import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { buildBusinessTaskFormData } from '../business-task-form-adapter.js'
import { resolveTaskUiDocument } from '../task-ui-document.js'
import { readTodoDetailSource } from './todo-detail-source.js'

test('uiDocument component tree is the rendered and submitted main form', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    formType: 'business-object',
    fields: [
      { field: 'title', type: 'input', label: '标题', writable: true },
      { field: 'amount', type: 'number', label: '金额', writable: true },
      { field: 'removed', type: 'input', writable: true },
    ],
    fieldPermissions: [
      { field: 'title', readable: true, writable: true },
      { field: 'amount', readable: true, writable: false },
    ],
    uiDocument: {
      version: '1',
      sections: [{ fields: ['removed', 'title', 'amount'] }],
      components: [{ type: 'card', label: '申请内容', children: [
        { type: 'input', field: 'title', editable: true },
        { type: 'number', field: 'amount', editable: true },
      ] }],
    },
  })

  assert.equal(resolved.hasComponentTree, true)
  assert.equal(resolved.nodes[0].nodeType, 'card')
  assert.deepEqual(resolved.fields.map(field => field.field), ['title', 'amount'])
  assert.equal(resolved.fields[1].readonly, true)
  assert.deepEqual(buildBusinessTaskFormData({
    formType: 'business-object',
    fields: resolved.fields,
    mainData: { title: '采购', amount: 10, removed: '旧字段' },
  }).main, { title: '采购' })
})

test('document component metadata restores strong selector type and data source', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [{ field: 'ownerId', type: 'input', label: '负责人', writable: true, props: { placeholder: '请选择' } }],
    uiDocument: { components: [{
      type: 'userSelect', componentKey: 'userSelect', field: 'ownerId', editable: true,
      span: 12, props: { optionSource: { type: 'QUERY_SOURCE', sourceKey: 'users' }, fieldMappings: [{ sourceField: 'name', targetField: 'ownerName' }] },
    }] },
  })
  const [field] = resolved.fields
  assert.equal(field.type, 'userSelect')
  assert.equal(field.span, 12)
  assert.equal(field.props.placeholder, '请选择')
  assert.equal(field.props.optionSource.sourceKey, 'users')
  assert.equal(field.props.fieldMappings[0].targetField, 'ownerName')
})

test('document keeps PC field, validation, default and layout properties intact', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [{ field: 'amount', writable: true, readable: true, type: 'input' }],
    uiDocument: { version: '1', components: [{
      componentKey: 'card',
      props: { bordered: false, collapsible: true, size: 'small' },
      children: [{
        componentKey: 'money',
        field: 'amount',
        label: '申请金额',
        editable: true,
        defaultValue: 10,
        validation: { required: true, min: 1, requiredMessage: '请输入申请金额' },
        props: { precision: 2, currencySymbol: '¥', showChinese: true, futureDeclarativeProp: 'kept' },
      }],
    }] },
  })

  assert.equal(resolved.nodes[0].props.bordered, false)
  assert.equal(resolved.nodes[0].props.collapsible, true)
  assert.equal(resolved.fields[0].type, 'money')
  assert.equal(resolved.fields[0].required, true)
  assert.equal(resolved.fields[0].validation.min, 1)
  assert.equal(resolved.fields[0].defaultValue, 10)
  assert.equal(resolved.fields[0].props.precision, 2)
  assert.equal(resolved.fields[0].props.futureDeclarativeProp, 'kept')
})

test('document-only field renders while explicit readonly and invisible nodes stay protected', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1', fields: [],
    uiDocument: { components: [{ type: 'tabs', children: [
      { type: 'tabPane', label: '基本', children: [
        { type: 'switch', field: 'fieldSwitch', label: '开关', editable: true },
        { type: 'input', field: 'secret', visible: false, editable: true },
        { type: 'input', field: 'locked', editable: false },
      ] },
    ] }] },
  })
  assert.deepEqual(resolved.fields.map(field => field.field), ['fieldSwitch', 'locked'])
  assert.equal(resolved.fields[0].type, 'switch')
  assert.equal(resolved.fields[1].readonly, true)
  assert.equal(resolved.nodes[0].nodeType, 'tabs')
  assert.equal(resolved.nodes[0].children[0].nodeType, 'tabPane')
})

test('unreadable permission excludes a document field even if the node says editable', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [{ field: 'salary', type: 'money', writable: true }],
    fieldPermissions: [{ field: 'salary', readable: false, writable: false }],
    uiDocument: { components: [
      { type: 'money', field: 'salary', editable: true },
      { type: 'input', field: 'title', editable: true },
    ] },
  })
  assert.deepEqual(resolved.fields.map(field => field.field), ['title'])
})

test('designer readonly props cannot be bypassed by writable approval permission', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [{ field: 'systemCode', type: 'input', writable: true }],
    fieldPermissions: [{ field: 'systemCode', readable: true, writable: true }],
    uiDocument: { components: [{ type: 'input', field: 'systemCode', editable: true, props: { readonly: true } }] },
  })
  assert.equal(resolved.fields[0].readonly, true)
  assert.deepEqual(buildBusinessTaskFormData({ fields: resolved.fields, mainData: { systemCode: 'A001' } }), {})
})

test('uiDocument editable flags remain authoritative when approval permissions are writable', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [
      { field: 'fieldMoney', type: 'money', readable: true, writable: true, readonly: false, props: { readonly: false, disabled: false } },
      { field: 'fieldInput', type: 'input', readable: true, writable: true, readonly: false, props: { readonly: false, disabled: false } },
      { field: 'fieldNumber', type: 'number', readable: true, writable: true, readonly: false, props: { readonly: false, disabled: false } },
    ],
    fieldPermissions: [
      { field: 'fieldMoney', readable: true, writable: true, editable: true },
      { field: 'fieldInput', readable: true, writable: true, editable: true },
      { field: 'fieldNumber', readable: true, writable: true, editable: true },
    ],
    uiDocument: {
      version: '1',
      sections: [
        { sectionId: 'section_default', sectionType: 'card', title: '基本信息', fields: ['fieldInput', 'fieldNumber'] },
        { sectionId: 'child_business_object_hl92', sectionType: 'child_table', title: '指标汇总', fields: [] },
      ],
      components: [
        { componentKey: 'money', field: 'fieldMoney', editable: true },
        { componentKey: 'input', field: 'fieldInput', editable: false },
        { componentKey: 'number', field: 'fieldNumber', editable: false },
      ],
    },
  })

  assert.deepEqual(resolved.fields.map(field => [field.field, field.readonly]), [
    ['fieldMoney', false],
    ['fieldInput', true],
    ['fieldNumber', true],
  ])
  assert.deepEqual(buildBusinessTaskFormData({
    fields: resolved.fields,
    mainData: { fieldMoney: 100, fieldInput: '只读文本', fieldNumber: 2 },
  }), { fieldMoney: 100 })
  assert.deepEqual(resolved.sections.map(section => section.sectionId), [
    'section_default',
    'child_business_object_hl92',
  ])
})

test('empty visible document tree never reintroduces off-canvas fields', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [{ field: 'removed', writable: true }],
    uiDocument: { components: [{ type: 'card', children: [{ type: 'input', field: 'hidden', visible: false }] }] },
  })
  assert.equal(resolved.hasComponentTree, true)
  assert.deepEqual(resolved.fields, [])
})

test('sections order is used when components are absent, then remaining fields follow', () => {
  const resolved = resolveTaskUiDocument({
    protocolVersion: '1',
    fields: [
      { field: 'amount', writable: true },
      { field: 'title', writable: true },
      { field: 'extra', writable: true },
    ],
    uiDocument: { version: '1', sections: [{ sectionId: 'summary', title: '概要', fields: ['title', 'amount'] }], components: [] },
  })
  assert.deepEqual(resolved.fields.map(field => field.field), ['title', 'amount', 'extra'])
  assert.deepEqual(resolved.sections.map(section => section.sectionId), ['summary', 'main:remaining'])
  assert.deepEqual(resolved.nodes, [])
})

test('legacy forms keep the original fields and form-create fallback', () => {
  assert.equal(resolveTaskUiDocument({ fields: [{ field: 'title' }] }), null)
  assert.equal(resolveTaskUiDocument({ protocolVersion: '2', uiDocument: { version: '2' } }), null)
})

test('approval page sends the JSON tree through the shared layout renderer', () => {
  const page = readTodoDetailSource()
  const sections = readFileSync(new URL('../../components/lowcode/PageSectionRenderer.vue', import.meta.url), 'utf8')
  const layout = readFileSync(new URL('../../components/lowcode/LowcodeLayoutNodes.vue', import.meta.url), 'utf8')
  assert.match(page, /:main-nodes="mainNodes"/)
  assert.match(sections, /:nodes="mainNodes\.length && String\(section\.sectionId\) === mainNodeSectionId \? mainNodes : \[\]"/)
  assert.match(sections, /const mainNodeSectionId = computed/)
  assert.match(sections, /<AiTabs[\s\S]*childSections\.length > 1/)
  assert.match(layout, /function isFieldNode\(node\)/)
  assert.doesNotMatch(layout, /isCardLikeNode/)
  assert.match(layout, /:current-children="currentChildren"/)
})

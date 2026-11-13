import assert from 'node:assert/strict'
import { test } from 'node:test'
import { completePageSectionFields } from '../page-section-fields.js'

const fields = [{ field: 'reason' }, { field: 'amount' }, { field: 'secret', hidden: true }]

test('stale section reference still renders every visible field', () => {
  const sections = [{ sectionId: 'section_default', sectionType: 'card', title: '基本信息', fields: ['fieldInput'] }]
  const [section] = completePageSectionFields(sections, fields)
  assert.deepEqual(section.fields, ['fieldInput', 'reason', 'amount'])
  assert.deepEqual(sections[0].fields, ['fieldInput'], 'must not mutate input')
})

test('complete sections are returned unchanged', () => {
  const sections = [{ sectionId: 'a', sectionType: 'card', fields: ['reason', 'amount'] }]
  assert.equal(completePageSectionFields(sections, fields), sections)
})

test('missing fields skip child table sections', () => {
  const sections = [
    { sectionId: 'child_items', sectionType: 'child_table', relationKey: 'items' },
    { sectionId: 'b', sectionType: 'card', fields: ['reason'] },
  ]
  const result = completePageSectionFields(sections, fields)
  assert.equal(result[0], sections[0])
  assert.deepEqual(result[1].fields, ['reason', 'amount'])
})

test('only child sections get a leading default section', () => {
  const sections = [{ sectionId: 'child_items', sectionType: 'child_table', relationKey: 'items' }]
  const result = completePageSectionFields(sections, fields)
  assert.equal(result.length, 2)
  assert.deepEqual(result[0].fields, ['reason', 'amount'])
})

test('no configured sections keeps caller fallback', () => {
  assert.deepEqual(completePageSectionFields([], fields), [])
})

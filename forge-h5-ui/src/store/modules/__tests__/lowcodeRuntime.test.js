import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { useLowcodeRuntimeStore } from '../lowcodeRuntime.js'

function createStore() {
  setActivePinia(createPinia())
  return useLowcodeRuntimeStore()
}

test('initializes isolated route state and keeps Snowflake ids as strings', () => {
  const store = createStore()
  store.initializeRoute({ path: '/crud-page/purchase_order', mode: 'detail', recordId: '9223372036854775807', taskId: '9007199254740993' })

  assert.equal(store.configKey, 'purchase_order')
  assert.equal(store.mode, 'detail')
  assert.equal(store.currentId, '9223372036854775807')
  assert.equal(store.currentFlowTaskId, '9007199254740993')
})

test('derives shared fields, children and page chrome from one runtime protocol', () => {
  const store = createStore()
  store.initializeRoute({ configKey: 'purchase_order' })
  store.applyConfig({
    objectName: '采购单',
    editSchema: [{ field: 'subject', label: '主题', type: 'input' }],
    options: {
      mobilePage: { subtitle: '移动审批页', safeAreaBottom: true },
      masterDetailConfig: {
        children: [{ relationKey: 'items', modelCode: 'purchase_item', fields: [{ field: 'productName', type: 'input' }] }],
      },
    },
  })

  assert.equal(store.mainFields[0].field, 'subject')
  assert.equal(store.allChildren[0].fields[0].field, 'productName')
  assert.equal(store.pageChrome.title, '采购单')
  assert.equal(store.pageChrome.subtitle, '移动审批页')
  assert.equal(store.pageChrome.safeBottom, true)
})

test('resets list and record state when a standalone page is remounted', () => {
  const store = createStore()
  store.initializeRoute({ configKey: 'first' })
  store.applyList({ records: [{ id: '1' }], total: 1 })
  store.applyDetail({ id: '1', subject: '旧数据' })
  store.searchData.keyword = 'old'

  store.initializeRoute({ configKey: 'second', mode: 'create' })

  assert.equal(store.configKey, 'second')
  assert.equal(store.mode, 'create')
  assert.deepEqual(store.records, [])
  assert.deepEqual(store.mainData, {})
  assert.deepEqual(store.searchData, {})
})

import assert from 'node:assert/strict'
import test from 'node:test'
import { createPinia, setActivePinia } from 'pinia'
import { CREATE_SUBMIT_KEY, useDocumentFlowStore } from '../documentFlow.js'

function createStore(permissions = { start: true, withdraw: true }) {
  setActivePinia(createPinia())
  const store = useDocumentFlowStore()
  store.permissions = permissions
  return store
}

test('runtime of a non-document object is not shown', () => {
  const store = createStore()
  store.setRuntime({ documentEnabled: false, runtimeActions: [{ actionType: 'START_FLOW' }] })
  assert.equal(store.visible, false)
  assert.deepEqual(store.buttons, [])
})

test('detail footer uses runtime buttons filtered by permissions', () => {
  const store = createStore({ start: false, withdraw: true })
  store.setRuntime({
    documentEnabled: true,
    documentStatusLabel: '审批中',
    message: '流程流转中',
    flowStatus: 'RUNNING',
    runtimeActions: [{ actionType: 'START_FLOW' }, { actionType: 'WITHDRAW_FLOW' }],
  })
  assert.deepEqual(store.footerButtons('detail').map(button => button.actionType), ['WITHDRAW_FLOW'])
  assert.equal(store.hint, '暂无提交审批权限，请联系管理员')
  assert.equal(store.statusText, '审批中')
  assert.equal(store.statusTone, 'primary')
  assert.equal(store.message, '流程流转中')
})

test('create footer offers submit only when entered from approval start with permission', () => {
  const store = createStore()
  assert.deepEqual(store.footerButtons('create'), [])
  store.createSubmit = true
  assert.deepEqual(store.footerButtons('create').map(button => button.key), [CREATE_SUBMIT_KEY])
  store.permissions = { start: false, withdraw: false }
  assert.deepEqual(store.footerButtons('create'), [])
})

const reimburseProcess = {
  key: 'startProcess:reimburse',
  actionType: 'START_PROCESS',
  label: '报销审批',
  applicationCode: 'hr_e2_e',
  processCode: 'reimburse',
  objectCode: 'apply_kdcp',
}

test('business process buttons show on non-document objects and hide once started', () => {
  const store = createStore({ start: true, withdraw: true, processStart: true })
  store.setProcessActions([reimburseProcess])
  store.setRuntime({ documentEnabled: false, activeProcessCodes: [], startedProcessCodes: [] })
  assert.equal(store.visible, false)
  assert.deepEqual(store.footerButtons('detail').map(button => button.key), ['START_PROCESS:reimburse'])
  store.setRuntime({ documentEnabled: false, activeProcessCodes: ['reimburse'] })
  assert.deepEqual(store.buttons, [])
})

test('business process buttons merge after document flow buttons and respect permission', () => {
  const store = createStore({ start: true, withdraw: true, processStart: false })
  store.setProcessActions([reimburseProcess])
  store.setRuntime({ documentEnabled: true, runtimeActions: [{ actionType: 'START_FLOW' }] })
  assert.deepEqual(store.buttons.map(button => button.actionType), ['START_FLOW'])
  assert.equal(store.hint, '暂无发起业务流程权限，请联系管理员')
  store.permissions = { start: true, withdraw: true, processStart: true }
  assert.deepEqual(store.buttons.map(button => button.actionType), ['START_FLOW', 'START_PROCESS'])
  store.reset()
  assert.deepEqual(store.buttons, [])
})

test('disabled reason surfaces and reset clears runtime and loading', () => {
  const store = createStore()
  store.setRuntime({ runtimeActions: [{ actionType: 'HANDLE_TASK', disabled: true, disabledReason: '任务已被他人处理' }] })
  store.loadingKey = 'HANDLE_TASK'
  assert.equal(store.disabledReason, '任务已被他人处理')
  store.reset()
  assert.equal(store.runtime, null)
  assert.equal(store.loadingKey, '')
})

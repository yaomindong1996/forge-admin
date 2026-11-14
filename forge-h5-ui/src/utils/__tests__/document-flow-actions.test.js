import assert from 'node:assert/strict'
import test from 'node:test'
import {
  NO_PROCESS_START_PERMISSION_HINT,
  NO_START_PERMISSION_HINT,
  buildDocumentFlowButtons,
  buildProcessStartButtons,
  collectProcessStartActions,
  resolveProcessRunFailure,
  resolveDocumentFlowTone,
  resolveDocumentStatusText,
} from '../document-flow-actions.js'

const runtime = actions => ({ runtimeActions: actions })

test('runtime actions map to buttons in backend order', () => {
  const { buttons, hint } = buildDocumentFlowButtons(runtime([
    { key: 'START_FLOW', actionType: 'START_FLOW', label: '发起主流程' },
    { key: 'WITHDRAW_FLOW', actionType: 'WITHDRAW_FLOW' },
  ]))
  assert.deepEqual(buttons.map(button => [button.actionType, button.label, button.variant]), [
    ['START_FLOW', '提交审批', 'primary'],
    ['WITHDRAW_FLOW', '撤回', 'secondary'],
  ])
  assert.equal(hint, '')
})

test('hidden actions are skipped and disabled actions keep the reason', () => {
  const { buttons } = buildDocumentFlowButtons(runtime([
    { actionType: 'START_FLOW', visible: false },
    { actionType: 'HANDLE_TASK', disabled: true, disabledReason: '任务已被认领' },
  ]))
  assert.deepEqual(buttons.map(button => [button.actionType, button.disabled, button.disabledReason]), [
    ['HANDLE_TASK', true, '任务已被认领'],
  ])
})

test('missing permissions hide buttons and explain why', () => {
  const can = key => key !== 'start' && key !== 'withdraw'
  const { buttons, hint } = buildDocumentFlowButtons(runtime([
    { actionType: 'RESUBMIT_FLOW' },
    { actionType: 'WITHDRAW_FLOW' },
    { actionType: 'HANDLE_TASK' },
  ]), can)
  assert.deepEqual(buttons.map(button => button.actionType), ['HANDLE_TASK'])
  assert.equal(hint, NO_START_PERMISSION_HINT)
})

test('view and app-process actions are not document flow buttons', () => {
  const { buttons, hint } = buildDocumentFlowButtons(runtime([
    { actionType: 'VIEW_FLOW' },
    { key: 'START_PROCESS' },
    { key: 'startProcess:approval_x', actionType: 'START_PROCESS' },
    { actionType: 'UNKNOWN' },
  ]))
  assert.equal(buttons.length, 0)
  assert.equal(hint, '')
  assert.deepEqual(buildDocumentFlowButtons(null), { buttons: [], hint: '' })
})

const processAction = (processCode, extra = {}) => ({
  key: `startProcess:${processCode}`,
  actionType: 'START_PROCESS',
  label: '报销审批',
  applicationCode: 'hr_e2_e',
  processCode,
  objectCode: 'apply_kdcp',
  ...extra,
})

test('process start actions are collected from record-level positions only', () => {
  const config = {
    options: {
      rowActions: [processAction('p1'), { key: 'edit' }],
      detailActions: [processAction('p2')],
      formActions: [processAction('p1')],
      toolbarActions: [processAction('p3')],
    },
  }
  assert.deepEqual(collectProcessStartActions(config).map(action => action.processCode), ['p1', 'p2', 'p1'])
  assert.deepEqual(collectProcessStartActions({}), [])
})

test('process start buttons dedupe by process and hide started ones', () => {
  const actions = [processAction('p1'), processAction('p1', { key: 'startProcess:p1:form' }), processAction('p2'), processAction('p3')]
  const { buttons, hint } = buildProcessStartButtons(actions, { activeProcessCodes: ['p2'], startedProcessCodes: ['p3'] })
  assert.deepEqual(buttons.map(button => [button.processCode, button.applicationCode, button.label]), [['p1', 'hr_e2_e', '报销审批']])
  assert.equal(buttons[0].actionType, 'START_PROCESS')
  assert.equal(hint, '')
})

test('process start buttons respect permission and display condition', () => {
  const actions = [processAction('p1'), processAction('p2', { visibleCondition: 'flowStatus == DRAFT' })]
  const denied = buildProcessStartButtons(actions, {}, { canStart: false })
  assert.equal(denied.buttons.length, 0)
  assert.equal(denied.hint, NO_PROCESS_START_PERMISSION_HINT)
  const filtered = buildProcessStartButtons(actions, {}, { isVisible: action => !action.visibleCondition })
  assert.deepEqual(filtered.buttons.map(button => button.processCode), ['p1'])
  assert.equal(buildProcessStartButtons([{ actionType: 'START_PROCESS', processCode: 'p9' }]).buttons.length, 0)
})

test('status text prefers the document label then the runtime message', () => {
  assert.equal(resolveDocumentStatusText({ documentStatusLabel: '审批中', message: '流程流转中' }), '审批中')
  assert.equal(resolveDocumentStatusText({ message: '已驳回，修改后可重新提交' }), '已驳回，修改后可重新提交')
  assert.equal(resolveDocumentStatusText(null), '')
})

test('flow status only changes the tag tone', () => {
  assert.equal(resolveDocumentFlowTone('running'), 'primary')
  assert.equal(resolveDocumentFlowTone('NEED_MODIFY'), 'warning')
  assert.equal(resolveDocumentFlowTone('APPROVED'), 'success')
  assert.equal(resolveDocumentFlowTone('REJECTED'), 'danger')
  assert.equal(resolveDocumentFlowTone('NOT_STARTED'), 'default')
  assert.equal(resolveDocumentFlowTone(null), 'default')
})

test('process run failure surfaces the failed node reason', () => {
  assert.equal(resolveProcessRunFailure({ status: 'PENDING' }), '')
  assert.equal(resolveProcessRunFailure(null), '')
  assert.equal(resolveProcessRunFailure({
    status: 'FAILED',
    errorSummary: null,
    timeline: [
      { status: 'FAILED', errorSummary: '当前单据已有主流程实例，不能重复发起' },
      { status: 'SUCCESS', errorSummary: null },
    ],
  }), '当前单据已有主流程实例，不能重复发起')
  assert.equal(resolveProcessRunFailure({ status: 'failed' }), '流程执行失败，请联系管理员')
})

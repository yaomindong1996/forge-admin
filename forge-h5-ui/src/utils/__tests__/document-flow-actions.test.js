import assert from 'node:assert/strict'
import test from 'node:test'
import {
  NO_START_PERMISSION_HINT,
  PC_ONLY_PROCESS_HINT,
  buildDocumentFlowButtons,
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

test('view and app-process actions never render buttons', () => {
  const { buttons, hint } = buildDocumentFlowButtons(runtime([
    { actionType: 'VIEW_FLOW' },
    { key: 'START_PROCESS' },
    { actionType: 'UNKNOWN' },
  ]))
  assert.equal(buttons.length, 0)
  assert.equal(hint, PC_ONLY_PROCESS_HINT)
  assert.deepEqual(buildDocumentFlowButtons(null), { buttons: [], hint: '' })
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

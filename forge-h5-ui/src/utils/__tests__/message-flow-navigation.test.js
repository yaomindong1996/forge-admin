import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  buildFlowTaskDetailUrl,
  extractFlowTaskId,
  isFlowTaskRoute,
  resolveFlowMessageMode,
  resolveFlowMessageTaskId,
  shouldAutoMarkMessageRead,
} from '../message-flow-navigation.js'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.resolve(testDir, '../..')

function readSource(relativePath) {
  return fs.readFileSync(path.join(srcDir, relativePath), 'utf8')
}

test('resolves a flow task id from route fields and FLOW_TODO bizKey in priority order', () => {
  const message = {
    bizType: 'FLOW_TODO',
    bizKey: '9223372036854775807',
    taskId: 'task-field',
  }
  assert.equal(resolveFlowMessageTaskId(message, '/flow/todo?taskId=route-task'), 'route-task')
  assert.equal(resolveFlowMessageTaskId(message), 'task-field')
  assert.equal(resolveFlowMessageTaskId({ bizType: 'FLOW_TODO', bizKey: '9223372036854775807' }), '9223372036854775807')
})

test('recognizes management and mobile todo routes without changing their protocol', () => {
  assert.equal(isFlowTaskRoute('/flow/todo?taskId=task-1'), true)
  assert.equal(isFlowTaskRoute('/pages/todo-detail?taskId=task-1'), true)
  assert.equal(isFlowTaskRoute('/pages/lowcode-runtime?configKey=demo'), false)
  assert.equal(extractFlowTaskId('/flow/todo?source=message&taskId=task%3A100'), 'task:100')
})

test('pending FLOW_TODO opens editable task mode while completed message opens readonly mode', () => {
  assert.equal(resolveFlowMessageMode({ bizType: 'FLOW_TODO', readFlag: 0 }), 'todo')
  assert.equal(resolveFlowMessageMode({ bizType: 'FLOW_TODO', readFlag: 1 }), 'readonly')
  assert.equal(buildFlowTaskDetailUrl('task:100', 'readonly'), '/pages/todo-detail?taskId=task%3A100&mode=readonly')
  assert.equal(
    buildFlowTaskDetailUrl('task:100', 'todo', '9223372036854775807'),
    '/pages/todo-detail?taskId=task%3A100&mode=todo&messageId=9223372036854775807',
  )
})

test('opening an active approval message does not mark it read before workflow completion', () => {
  assert.equal(shouldAutoMarkMessageRead({ bizType: 'FLOW_TODO', readFlag: 0 }), false)
  assert.equal(shouldAutoMarkMessageRead({ bizType: 'SYSTEM', readFlag: 0 }), true)
  assert.equal(shouldAutoMarkMessageRead({ bizType: 'SYSTEM', readFlag: 1 }), false)
})

test('message and todo pages keep the handling round trip continuous', () => {
  const messageSource = readSource('pages/message/index.vue')
  const todoSource = readSource('pages/todo.vue')
  const detailSource = readSource('pages/todo-detail.vue')
  const apiSource = readSource('api/index.js')

  assert.match(messageSource, /target \|\| \{ id: pendingOpenId\.value \}/)
  assert.match(messageSource, /resolveFlowMessageTaskId\(message, route\)/)
  assert.match(messageSource, /buildFlowTaskDetailUrl\(taskId, resolveFlowMessageMode\(message\), message\?\.id\)/)
  assert.match(messageSource, /api\.markMessagesReadBatch\(messageIds\)/)
  assert.match(todoSource, /onShow\(async \(\) => \{\s*await loadTasks\(\{ reset: true \}\)/)
  assert.match(detailSource, /await api\.markMessageRead\(sourceMessageId\.value\)/)
  assert.match(detailSource, /setTimeout\(goBack, 500\)/)
  assert.match(apiSource, /url: '\/api\/message\/read\/batch'/)
})

import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.resolve(testDir, '../..')

function readSource(relativePath) {
  return fs.readFileSync(path.join(srcDir, relativePath), 'utf8')
}

const pageFiles = [
  'pages/login/index.vue',
  'pages/index/index.vue',
  'pages/message/index.vue',
  'pages/todo.vue',
  'pages/todo-detail.vue',
  'pages/mine/index.vue',
  'pages/demo/loading/index.vue',
  'pages/app-entry.vue',
  'pages/lowcode-runtime.vue',
]

test('every registered page mounts the shared Wot feedback host', () => {
  for (const pageFile of pageFiles) {
    const source = readSource(pageFile)
    assert.match(source, /<AiFeedbackHost\s*\/>/, `${pageFile} should mount AiFeedbackHost`)
    assert.match(source, /feedback\/AiFeedbackHost\.vue/, `${pageFile} should import AiFeedbackHost`)
  }
})

test('feedback host exposes the complete Wot feedback component set', () => {
  const source = readSource('components/feedback/AiFeedbackHost.vue')
  assert.match(source, /<wd-toast\b/)
  assert.match(source, /<wd-notify\b/)
  assert.match(source, /<wd-message-box\b/)
  assert.match(source, /<wd-action-sheet\b/)
  assert.match(source, /useToast/)
  assert.match(source, /useNotify/)
  assert.match(source, /useMessage/)
})

test('shared feedback adapters do not fall back to custom DOM or uni native prompts', () => {
  const adapterSource = [
    readSource('utils/dialog.js'),
    readSource('utils/notify.js'),
  ].join('\n')

  assert.match(adapterSource, /runWithFeedbackHost/)
  assert.doesNotMatch(adapterSource, /document\.createElement/)
  assert.doesNotMatch(adapterSource, /uni\.show(?:Modal|ActionSheet|Toast)/)
  assert.equal(fs.existsSync(path.join(srcDir, 'styles/dialog.css')), false)
  assert.equal(fs.existsSync(path.join(srcDir, 'styles/notify.css')), false)
})

test('login uses Forge adapters backed by Wot instead of native form and modal markup', () => {
  const source = readSource('pages/login/index.vue')
  assert.match(source, /<AiField\b/)
  assert.match(source, /<AiButton\b/)
  assert.match(source, /<AiPopupSheet\b/)
  assert.doesNotMatch(source, /<input\b/)
  assert.doesNotMatch(source, /workspace-modal-overlay/)
})

test('shared search and textarea fields are backed by Wot components', () => {
  const searchSource = readSource('components/AiSearchBar.vue')
  const textareaSource = readSource('components/AiTextarea.vue')
  const lowcodeFieldSource = readSource('components/lowcode/LowcodeField.vue')
  const todoDetailSource = readSource('pages/todo-detail.vue')
  const homeSource = readSource('pages/index/index.vue')

  assert.match(searchSource, /<wd-search\b/)
  assert.doesNotMatch(searchSource, /<input\b/)
  assert.match(textareaSource, /<wd-textarea\b/)
  assert.doesNotMatch(textareaSource, /<textarea\b/)
  assert.match(lowcodeFieldSource, /<AiTextarea\b/)
  assert.doesNotMatch(lowcodeFieldSource, /<(?:input|textarea)\b/)
  assert.match(todoDetailSource, /<AiTextarea\b/)
  assert.doesNotMatch(todoDetailSource, /<textarea\b/)
  assert.match(homeSource, /<AiSearchBar\b/)
  assert.doesNotMatch(homeSource, /<input\b/)
})

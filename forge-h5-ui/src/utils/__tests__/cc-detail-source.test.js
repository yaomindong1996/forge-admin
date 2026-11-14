import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const srcDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const page = fs.readFileSync(path.join(srcDir, 'pages/flow/cc-detail.vue'), 'utf8')

test('cc detail only asks to open PC when a form exists but cannot render', () => {
  assert.match(page, /v-else-if="businessForm\.blockedReason" class="cc-blocked-panel"/)
  assert.doesNotMatch(page, /<view v-else class="cc-blocked-panel">/)
})

test('cc detail falls back to the diagram process name', () => {
  assert.match(page, /diagramInfo\.value\?\.processName/)
})

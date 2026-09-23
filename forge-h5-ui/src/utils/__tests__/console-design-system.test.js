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

function readProductionStyles() {
  const roots = ['components', 'pages', 'styles']
  const files = []
  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const entryPath = path.join(directory, entry.name)
      if (entry.isDirectory()) visit(entryPath)
      else if (/\.(?:vue|scss|css)$/.test(entry.name)) files.push(entryPath)
    }
  }
  roots.forEach(root => visit(path.join(srcDir, root)))
  return files.map(file => fs.readFileSync(file, 'utf8')).join('\n')
}

test('console theme exposes the exact Ark-style palette and sizing tokens', () => {
  const source = readSource('styles/theme.css').toLowerCase()
  assert.match(source, /--forge-color-primary:\s*#4266f7/)
  assert.match(source, /--forge-text-primary:\s*#1d2129/)
  assert.match(source, /--forge-text-secondary:\s*#4e5969/)
  assert.match(source, /--forge-text-tertiary:\s*#86909c/)
  assert.match(source, /--forge-border:\s*#c9cdd4/)
  assert.match(source, /--forge-page-bg:\s*#f2f3f5/)
  assert.match(source, /--forge-surface-muted:\s*#f7f8fa/)
  assert.match(source, /--forge-radius-control:\s*12rpx/)
  assert.match(source, /--forge-control-height:\s*88rpx/)
  assert.match(source, /--forge-shadow-soft:\s*none/)
})

test('global typography uses the cross-platform system font stack', () => {
  const source = readSource('styles/global.css')
  assert.match(source, /system-ui,\s*-apple-system,\s*BlinkMacSystemFont,\s*"Segoe UI",\s*Roboto,\s*sans-serif/)
})

test('shared controls retain 44px touch targets and the common six pixel radius', () => {
  const button = readSource('components/AiButton.vue')
  const field = readSource('components/AiField.vue')
  const tabs = readSource('components/AiTabs.vue')
  const popup = readSource('components/AiPopupSheet.vue')
  assert.match(button, /min-height:\s*88rpx/)
  assert.match(field, /min-height:\s*88rpx/)
  assert.match(tabs, /min-height:\s*88rpx/)
  assert.match(popup, /border-radius:\s*var\(--forge-radius-popup(?:,\s*12rpx)?\)/)
})

test('single-line controls vertically center values, placeholders and icons', () => {
  const field = readSource('components/AiField.vue')
  const search = readSource('components/AiSearchBar.vue')
  const select = readSource('components/AiSelect.vue')
  const datetime = readSource('components/AiDateTimePicker.vue')

  assert.match(field, /wd-input__inner[\s\S]*height:\s*86rpx[\s\S]*line-height:\s*86rpx/)
  assert.match(search, /wd-search__field[\s\S]*align-items:\s*center/)
  assert.match(search, /wd-search__input[\s\S]*line-height:\s*86rpx/)
  assert.match(select, /wd-picker__cell[\s\S]*align-items:\s*center/)
  assert.match(datetime, /wd-datetime-picker__cell[\s\S]*align-items:\s*center/)
})

test('home uses one stable dashboard hierarchy instead of metric card walls', () => {
  const source = readSource('pages/index/index.vue')
  const styles = readSource('pages/styles/home.scss')

  assert.match(source, /class="home-dashboard"/)
  assert.match(source, /class="overview-list"/)
  assert.match(source, /class="shortcut-section"/)
  assert.match(source, /class="feed-section"/)
  assert.doesNotMatch(source, /class="attention-grid"/)
  assert.match(styles, /grid-template-areas:\s*\n\s*"overview apps"\s*\n\s*"feed apps"/)
})

test('todo cards claim candidates and navigate directly without a task transit sheet', () => {
  const source = readSource('pages/todo.vue')
  const claimIndex = source.indexOf('await api.claimFlowTask(normalizedTaskId, userId.value)')
  const navigationIndex = source.indexOf('await navigateToTask(`/pages/todo-detail')

  assert.ok(claimIndex >= 0)
  assert.ok(navigationIndex > claimIndex)
  assert.match(source, /@click="openTask\(task\)"/)
  assert.doesNotMatch(source, /<AiPopupSheet\b/)
  assert.doesNotMatch(source, /@click\.stop="claimTask\(task\)"/)
})

test('all key workspaces expose the 1024px desktop breakpoint', () => {
  const files = [
    'pages/styles/login.scss',
    'pages/styles/home.scss',
    'pages/styles/message.scss',
    'pages/styles/todo.scss',
    'pages/styles/todo-detail.scss',
    'pages/styles/mine.scss',
    'pages/styles/lowcode-runtime.scss',
    'components/lowcode/LowcodeRuntimeList.vue',
  ]
  for (const file of files) {
    assert.match(readSource(file), /@media\s*\(min-width:\s*1024px\)/, `${file} should define the desktop H5 layout`)
  }
})

test('production page styles do not reintroduce gradients or legacy brand colors', () => {
  const source = readProductionStyles().toLowerCase()
  assert.doesNotMatch(source, /(?:linear|radial)-gradient\s*\(/)
  assert.doesNotMatch(source, /#(?:165dff|1677ff|1f5fbf)\b/)
})

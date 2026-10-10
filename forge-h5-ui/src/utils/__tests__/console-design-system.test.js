import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { readTodoDetailSource, readTodoDetailStyles } from './todo-detail-source.js'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.resolve(testDir, '../..')

function readSource(relativePath) {
  return fs.readFileSync(path.join(srcDir, relativePath), 'utf8')
}

// pages.json 允许 // 注释；只剥离不在字符串里的行尾注释（URL 中的 // 前面是引号内文本）
function readPagesJson() {
  const source = readSource('pages.json')
    .split('\n')
    .map(line => line.replace(/^(\s*(?:"(?:[^"\\]|\\.)*"|[^"/])*?)\s*\/\/.*$/, '$1'))
    .join('\n')
  return JSON.parse(source)
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

test('H5 theme exposes the mobile office palette and sizing tokens', () => {
  const source = readSource('styles/theme.css').toLowerCase()
  assert.match(source, /--forge-color-primary:\s*#0066ff/)
  assert.match(source, /--forge-color-primary-soft:\s*#e8f1ff/)
  assert.match(source, /--forge-color-danger:\s*#ff5219/)
  assert.match(source, /--forge-text-primary:\s*#171a1d/)
  assert.match(source, /--forge-text-secondary:\s*#747677/)
  assert.match(source, /--forge-text-tertiary:\s*#a2a3a5/)
  assert.match(source, /--forge-border:\s*#f0f1f2/)
  assert.match(source, /--forge-page-bg:\s*#f2f1f6/)
  assert.match(source, /--forge-surface:\s*#ffffff/)
  assert.match(source, /--forge-surface-muted:\s*#ebecf0/)
  assert.match(source, /--forge-radius-control:\s*10px/)
  assert.match(source, /--forge-radius-card:\s*16px/)
  assert.match(source, /--forge-radius-popup:\s*16px/)
  assert.match(source, /--forge-radius-icon:\s*14px/)
  assert.match(source, /--forge-space-page:\s*12px/)
  assert.match(source, /--forge-control-height:\s*44px/)
  assert.match(source, /--forge-shadow-float:\s*0 4px 16px rgba\(23, 26, 29, 0\.08\)/)
  // 卡片不再使用阴影，只有悬浮底栏和弹层使用 shadow-float
  assert.match(source, /--forge-shadow-soft:\s*none/)
  for (const tone of ['blue', 'orange', 'green', 'purple', 'cyan', 'red']) {
    assert.match(source, new RegExp(`--forge-tone-${tone}-bg:\\s*#[0-9a-f]{6}`))
    assert.match(source, new RegExp(`--forge-tone-${tone}:\\s*#[0-9a-f]{6}`))
  }
})

test('global typography uses the cross-platform system font stack', () => {
  const source = readSource('styles/global.css')
  assert.match(source, /system-ui,\s*-apple-system,\s*BlinkMacSystemFont,\s*"Segoe UI",\s*Roboto,\s*sans-serif/)
})

test('shared controls retain 44px touch targets and consistent radii', () => {
  const button = readSource('components/AiButton.vue')
  const field = readSource('components/AiField.vue')
  const tabs = readSource('components/AiTabs.vue')
  const popup = readSource('components/AiPopupSheet.vue')
  assert.match(button, /ai-button--md \{ min-height:\s*44px/)
  assert.match(field, /height:\s*44px/)
  assert.match(tabs, /min-height:\s*44px/)
  assert.match(popup, /border-radius:\s*var\(--forge-radius-popup,\s*24px\)/)
})

test('single-line controls vertically center values, placeholders and icons', () => {
  const field = readSource('components/AiField.vue')
  const search = readSource('components/AiSearchBar.vue')
  const select = readSource('components/AiSelect.vue')
  const datetime = readSource('components/AiDateTimePicker.vue')

  assert.match(field, /wd-input__inner[\s\S]*height:\s*42px[\s\S]*align-items:\s*center/)
  assert.match(field, /uni-input-placeholder[\s\S]*line-height:\s*42px/)
  assert.match(search, /wd-search__field[\s\S]*align-items:\s*center/)
  assert.match(search, /wd-search__search-left-icon[\s\S]*position:\s*static[\s\S]*flex:\s*0 0 16px/)
  assert.match(search, /wd-search__input \.uni-input-placeholder[\s\S]*align-items:\s*center/)
  assert.match(select, /wd-select-picker__cell[\s\S]*align-items:\s*center/)
  assert.match(datetime, /wd-datetime-picker__cell[\s\S]*align-items:\s*center/)
  assert.match(search, /wd-search__block[\s\S]*height:\s*44px/)
  assert.match(select, /wd-select-picker__cell[\s\S]*min-height:\s*44px/)
})

test('only login and tab pages draw their own header; brand resources come from the backend', () => {
  const pages = readPagesJson()
  const brand = readSource('utils/tenant-brand.js')
  const login = readSource('pages/login/index.vue')
  const home = readSource('pages/index/index.vue')
  const mine = readSource('pages/mine/index.vue')
  const auth = readSource('store/modules/auth.js')

  const customNavPages = pages.pages
    .filter(page => page.style?.navigationStyle === 'custom')
    .map(page => page.path)
    .sort()
  assert.deepEqual(customNavPages, [
    'pages/contacts/index',
    'pages/index/index',
    'pages/login/index',
    'pages/message/index',
    'pages/mine/index',
    'pages/todo',
  ])
  assert.ok(pages.pages.some(page => page.path === 'pages/message/detail'))
  assert.match(brand, /\/auth\/tenant\/assets\/\$\{encodeURIComponent\(String\(tenantId\)\)\}\/logo/)
  assert.match(auth, /api\.getLoginConfig/)
  assert.match(auth, /state\.userInfo\?\.avatar/)
  assert.match(login, /brandLogoSrc/)
  assert.match(login, /\/static\/images\/login-bg\.png/)
  assert.match(login, /mode="scaleToFill"/)
  assert.doesNotMatch(login, /AiCheckboxGroup|agreementOptions|this\.agreed/)
  assert.match(home, /v-if="rawAvatarUrl"[\s\S]*:src="rawAvatarUrl" :fallback="brandLogoUrl \|\| '\/static\/logo\.png'"/)
  assert.match(home, /v-else class="avatar-image" :src="brandLogoUrl \|\| '\/static\/logo\.png'"/)
  assert.match(mine, /v-if="rawAvatarUrl"[\s\S]*:src="rawAvatarUrl" :fallback="brandLogoUrl \|\| '\/static\/logo\.png'"/)
  assert.match(mine, /v-else class="avatar-large-image" :src="brandLogoUrl \|\| '\/static\/logo\.png'"/)
})

test('authenticated images retry once per file id and deduplicate access-url requests', () => {
  const authImage = readSource('components/AiAuthImage.vue')
  const file = readSource('utils/file.js')

  assert.match(authImage, /let retriedInputKey = ''/)
  assert.match(authImage, /const inputKey = sourceKey\(props\.src\)/)
  assert.match(authImage, /failedSource !== fallbackSource/)
  assert.match(authImage, /retriedInputKey !== inputKey/)
  assert.doesNotMatch(authImage, /retriedSource !== failedSource/)
  assert.match(file, /const fileAccessUrlPending = new Map\(\)/)
  assert.match(file, /fileAccessUrlPending\.has\(rawValue\)/)
  assert.match(file, /fileAccessUrlPending\.set\(rawValue, pending\)/)
  assert.match(file, /fileAccessUrlPending\.delete\(rawValue\)/)
})

test('home uses real metrics, frequent apps and grouped app tabs', () => {
  const source = readSource('pages/index/index.vue')
  const skeleton = readSource('components/home/HomeWorkspaceSkeleton.vue')
  const styles = readSource('pages/styles/home.scss')

  assert.match(source, /<HomeWorkspaceSkeleton v-if="workspaceLoading"/)
  assert.match(source, /workspaceLoading\.value = false/)
  assert.match(skeleton, /aria-busy="true"/)
  assert.match(source, /class="home-dashboard"/)
  assert.match(source, /class="overview-list"/)
  assert.match(source, /class="shortcut-section"/)
  assert.match(source, /class="shortcut-item shortcut-more" @click="openMenuSheet"/)
  assert.match(source, /allMenuItems\.value\.slice\(0, 7\)/)
  assert.match(source, /<AiPopupSheet[\s\S]*title="全部应用"/)
  assert.match(source, /class="group-section"/)
  // 最新提醒已由消息页签承担，工作台不再重复展示
  assert.doesNotMatch(source, /class="feed-section"/)
  assert.doesNotMatch(source, /class="attention-grid"/)
  assert.match(styles, /grid-template-areas:\s*\n\s*"banner groups"\s*\n\s*"overview groups"\s*\n\s*"apps groups"/)
  assert.match(styles, /\.shortcut-grid,[\s\S]*?\{[^}]*grid-template-columns:\s*repeat\(4,/)
  assert.match(styles, /\n\.group-grid\s*\{[^}]*grid-template-columns:\s*repeat\(5,/)
})

test('approval validation scrolls to comments and login is single-flight', () => {
  const detail = readTodoDetailSource()
  const actionLoading = readSource('components/AiLoadingOverlay.vue')
  const login = readSource('pages/login/index.vue')
  const auth = readSource('store/modules/auth.js')

  assert.match(detail, /:scroll-into-view="scrollTarget"/)
  assert.match(detail, /id="approval-comment-panel"/)
  assert.match(detail, /scrollToApprovalComment\(\)/)
  assert.match(detail, /<AiLoadingOverlay :visible="actionLoading" :text="actionLoadingText"/)
  assert.match(detail, /:loading="actionLoading && pendingAction === 'reject'"/)
  assert.match(actionLoading, /<wd-loading/)
  assert.match(login, /if \(this\.loading\)\s*return/)
  assert.match(login, /:disabled="loading"/)
  assert.match(auth, /let passwordLoginPromise = null/)
  assert.match(auth, /if \(passwordLoginPromise\)\s*return passwordLoginPromise/)
  assert.match(auth, /loginConfigRequests/)
})

test('interactive H5 primitives use Wot components without uView or uni-ui fallbacks', () => {
  const main = readSource('main.js')
  const app = readSource('App.vue')
  const uniStyles = readSource('uni.scss')
  const pages = readSource('pages.json')
  const packageJson = readSource('../package.json')
  const popup = readSource('components/AiPopupSheet.vue')
  const select = readSource('components/AiSelect.vue')
  const dropdown = readSource('components/AiDropdownMenu.vue')
  const legacyPage = readSource('components/AiPage.vue')
  const mine = readSource('pages/mine/index.vue')

  for (const source of [main, app, uniStyles, pages, packageJson]) assert.doesNotMatch(source, /uview-plus/)
  assert.match(popup, /<wd-popup/)
  assert.match(select, /<wd-select-picker/)
  assert.match(dropdown, /<wd-select-picker/)
  assert.doesNotMatch(dropdown, /dropdown-overlay/)
  assert.match(legacyPage, /<wd-navbar/)
  assert.doesNotMatch(legacyPage, /<uni-nav-bar/)
  assert.match(mine, /<wd-switch/)
  assert.doesNotMatch(mine, /<switch\b/)
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

test('production page styles use only the reference palette for decorative gradients', () => {
  const source = readProductionStyles().toLowerCase()
  assert.doesNotMatch(source, /#(?:4266f7|165dff|1f5fbf)\b/)
  assert.match(source, /linear-gradient\s*\(/)
})

test('five-tab navigation with badges and multi-condition filters follow the mobile contract', () => {
  const pages = readPagesJson()
  const tabbar = readSource('components/AiTabBar.vue')
  const todo = readSource('pages/todo.vue')
  const message = readSource('pages/message/index.vue')
  const runtime = readSource('components/lowcode/LowcodeRuntimeList.vue')

  assert.deepEqual(pages.tabBar.list.map(item => item.pagePath), [
    'pages/message/index',
    'pages/todo',
    'pages/index/index',
    'pages/contacts/index',
    'pages/mine/index',
  ])
  for (const key of ['message', 'todo', 'home', 'contacts', 'mine']) assert.match(tabbar, new RegExp(`key: '${key}'`))
  assert.match(tabbar, /ai-tabbar__badge/)
  assert.match(tabbar, /useBadgeStore\(\)/)
  assert.match(todo, /<AiFilterSheet[\s\S]*draftCategoryFilter[\s\S]*draftStatusFilter/)
  for (const tone of ['blue', 'orange', 'emerald', 'purple', 'cyan', 'rose']) {
    assert.match(todo, new RegExp(`tone-${tone}`))
  }
  assert.match(todo, /statusToneClass\(task\)/)
  assert.match(message, /<AiFilterSheet[\s\S]*draftReadFilter/)
  assert.match(runtime, /searchFields\.length > 1[\s\S]*<AiFilterSheet/)
  assert.match(message, /buildFlowTaskDetailUrl\(taskId, resolveFlowMessageMode\(message\), message\.id\)/)
})

test('authenticated workspaces keep controls compact and avoid clipped nested sheets', () => {
  const todo = readSource('pages/todo.vue')
  const todoStyle = readSource('pages/styles/todo.scss')
  const detail = readTodoDetailSource()
  const summary = readSource('components/flow/TodoTaskSummary.vue')
  const detailStyle = readTodoDetailStyles()
  const select = readSource('components/AiSelect.vue')
  const datetime = readSource('components/AiDateTimePicker.vue')
  const homeStyle = readSource('pages/styles/home.scss')
  const global = readSource('styles/global.css')

  assert.match(todo, /class="todo-tools"[\s\S]*class="todo-list"/)
  assert.doesNotMatch(todo, /class="todo-header"/)
  assert.doesNotMatch(todo, /class="todo-title"/)
  assert.match(todo, /<AiSelect[\s\S]*v-model="draftCategoryFilter"[\s\S]*v-model="draftStatusFilter"/)
  assert.match(select, /<wd-select-picker[\s\S]*root-portal/)
  assert.doesNotMatch(todo, /task-card__meta-grid/)
  assert.match(todoStyle, /\.task-card__footer[\s\S]*min-height:\s*44px/)
  assert.match(todoStyle, /\.claim-button[\s\S]*width:\s*64px[\s\S]*height:\s*36px[\s\S]*line-height:\s*1/)
  assert.match(detail, /<TodoTaskSummary :task="task" \/>/)
  assert.doesNotMatch(summary, /task-summary__refresh|refresh-cw/)
  assert.match(detail, /class="detail-scroll"[\s\S]*refresher-enabled[\s\S]*:refresher-triggered="pullRefreshing"[\s\S]*@refresherrefresh="refreshByPull"/)
  assert.match(detail, /usePullRefresh\(refresh\)/)
  assert.match(detailStyle, /\.user-row\s*\{[^}]*line-height:\s*1\.4/)
  assert.match(detailStyle, /\.user-meta\s*\{[^}]*overflow-wrap:\s*anywhere/)
  assert.match(detailStyle, /\.lowcode-field--compact-row[\s\S]*grid-template-columns:\s*78px minmax\(0, 1fr\)/)
  assert.match(select, /:z-index="10010"/)
  assert.match(datetime, /:z-index="10010"/)
  assert.match(homeStyle, /\.overview-item\s*\{[^}]*line-height:\s*1\.4/)
  assert.match(global, /body::-webkit-scrollbar-thumb/)
})

test('query pages refresh their actual scroll surface and approval loads managed comment phrases', () => {
  const pages = readSource('pages.json')
  const home = readSource('pages/index/index.vue')
  const todo = readSource('pages/todo.vue')
  const message = readSource('pages/message/index.vue')
  const runtime = readSource('pages/lowcode-runtime.vue')
  const layout = readSource('components/AiLayoutPage.vue')
  const detail = readTodoDetailSource()
  const phraseInput = readSource('components/flow/FlowCommentPhraseInput.vue')
  assert.match(pages, /"path": "pages\/index\/index"[\s\S]*?"enablePullDownRefresh": true/)
  assert.match(home, /onPullDownRefresh[\s\S]*refreshWorkspace/)
  assert.doesNotMatch(home, /fallbackMenuItems/)
  assert.match(todo, /refresher-enabled[\s\S]*@refresherrefresh="refreshByPull"/)
  assert.match(message, /onPullDownRefresh[\s\S]*await refresh\(\)/)
  assert.match(runtime, /:refresher-enabled="mode === 'list'"[\s\S]*@refresh="refreshListByPull"/)
  assert.match(layout, /:refresher-triggered="refreshing"/)
  assert.match(detail, /class="detail-scroll"[\s\S]*scroll-y[\s\S]*:show-scrollbar="true"/)
  assert.match(detail, /<FlowCommentPhraseInput/)
  assert.match(phraseInput, /api\.listUsableCommentPhrases/)
  assert.match(phraseInput, /api\.listMyCommentPhrases/)
  assert.match(phraseInput, /api\.createCommentPhrase/)
  assert.match(phraseInput, /api\.deleteCommentPhrase/)
})

test('approval detail combines progress and history with localized display helpers', () => {
  const detail = readTodoDetailSource()
  const trace = readSource('components/flow/TodoFlowTrace.vue')
  const summary = readSource('components/flow/TodoTaskSummary.vue')
  const todo = readSource('pages/todo.vue')
  const lowcodeTrace = readSource('components/lowcode/LowcodeFlowTimeline.vue')
  assert.match(detail, /class="detail-section-title">审批流程/)
  assert.match(detail, /<TodoFlowTrace mode="process"[\s\S]*<TodoFlowTrace mode="history"/)
  assert.doesNotMatch(detail, /<AiTabs|<AiTab/)
  assert.match(trace, /formatFlowStatus\(item\.status \|\| item\.statusText\)/)
  assert.match(trace, /formatFlowDateTime\(time\)/)
  assert.match(summary, /formatFlowDateTime\(task\.createTime \|\| task\.startTime\)/)
  assert.match(todo, /formatFlowDateTime\(task\.createTime \|\| task\.startTime\)/)
  assert.match(lowcodeTrace, /formatFlowDateTime\(item\.completeTime/)
})

test('approval detail only renders business fields returned by task form context', () => {
  const api = readSource('api/index.js')
  const detail = readTodoDetailSource()

  assert.match(detail, /context\?\.taskFormInfo[\s\S]*formInfo\.value = context\.taskFormInfo/)
  assert.match(detail, /businessContextError\.value = resolveErrorMessage/)
  assert.match(detail, /已停止渲染动态表单/)
  assert.match(detail, /const mainFields = computed\([\s\S]*return \[\]/)
  assert.match(detail, /v-else-if="showBusinessFormPanel" class="content-panel"/)
  assert.equal((detail.match(/await loadBusinessContext\(/g) || []).length, 1)
  assert.equal((detail.match(/await loadReadonlyBusinessContext\(/g) || []).length, 1)
  assert.doesNotMatch(detail, /hydrateBusinessContextFromAssets/)
  assert.doesNotMatch(detail, /replaceMainData\(formInfo\.value\?\.variables\)/)
  assert.doesNotMatch(api, /getBusinessFlowFormAssets/)
})

test('approval actions, message cards and mine scrolling follow the reference mobile layout', () => {
  const api = readSource('api/index.js')
  const detail = readTodoDetailSource()
  const detailStyle = readTodoDetailStyles()
  const detailSkeleton = readSource('components/flow/TodoDetailSkeleton.vue')
  const taskSummary = readSource('components/flow/TodoTaskSummary.vue')
  const textarea = readSource('components/AiTextarea.vue')
  const message = readSource('pages/message/index.vue')
  const messageStyle = readSource('pages/styles/message.scss')
  const mine = readSource('pages/mine/index.vue')
  const mineStyle = readSource('pages/styles/mine.scss')

  assert.match(detail, /class="detail-section-title">审批意见<text/)
  assert.doesNotMatch(detail, /class="form-label">处理意见/)
  assert.match(detail, /class="more-action-grid"/)
  assert.match(detail, /canRejectToStart[\s\S]*退回发起人修改/)
  assert.match(detail, /\['approve', 'reject', 'rejectToStart', 'return'\]/)
  assert.match(api, /rejectToStartFlowTask:[\s\S]*\/api\/flow\/task\/reject-to-start/)
  assert.match(detail, /class="more-cancel-button"/)
  assert.match(detail, /<TodoDetailSkeleton v-if="loading"/)
  assert.match(detail, /file-text\.svg[\s\S]*check-circle\.svg[\s\S]*edit-3\.svg/)
  assert.match(detailSkeleton, /detail-skeleton-summary[\s\S]*detail-skeleton-form[\s\S]*detail-skeleton-flow/)
  assert.match(taskSummary, /file-text\.svg[\s\S]*user\.svg[\s\S]*briefcase\.svg[\s\S]*folder\.svg[\s\S]*clock\.svg/)
  assert.match(detailStyle, /\.more-action-grid[^}]*grid-template-columns:\s*repeat\(4,/)
  assert.match(textarea, /font-size:\s*14px !important/)
  assert.doesNotMatch(message, /aria-label="刷新消息"/)
  assert.match(message, /class="message-tools"[\s\S]*aria-label="筛选消息"/)
  assert.match(message, /class="message-scope-tabs"[\s\S]*message-scope-tab/)
  assert.match(messageStyle, /\.message-scope-tab\.active/)
  assert.match(messageStyle, /\.message-query-row[\s\S]*align-items:\s*center/)
  assert.match(messageStyle, /\.message-query-row :deep\(\.ai-search-bar\)[^}]*flex:\s*1/)
  // 消息改为会话式列表行：分类图标 + 未读红点，分隔线从文字起始处开始
  assert.match(message, /class="message-row"[\s\S]*class="message-icon"[\s\S]*class="message-dot"/)
  assert.match(messageStyle, /\.message-row\s*\{[^}]*min-height:\s*72px/)
  assert.match(messageStyle, /\.message-row \+ \.message-row::before\s*\{[^}]*left:\s*72px/)
  assert.match(mine, /class="mine-scroll" scroll-y :show-scrollbar="true"/)
  assert.match(mineStyle, /\.mine-scroll[^}]*overflow-y:\s*auto/)
})

test('message list is a tab page and is never pushed onto the page stack', () => {
  const offenders = []
  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const entryPath = path.join(directory, entry.name)
      if (entry.isDirectory()) {
        if (entry.name !== '__tests__') visit(entryPath)
      }
      else if (/\.(?:vue|js)$/.test(entry.name)) {
        const source = fs.readFileSync(entryPath, 'utf8')
        if (/navigateTo\(\{\s*url:\s*['"`]\/pages\/message\/index/.test(source)) offenders.push(entryPath)
      }
    }
  }
  ;['components', 'pages', 'utils', 'store'].forEach(root => visit(path.join(srcDir, root)))
  assert.deepEqual(offenders, [])
})

test('contacts pages call the login-only contacts API and dial only full numbers', () => {
  const api = readSource('api/index.js')
  const index = readSource('pages/contacts/index.vue')
  const org = readSource('pages/contacts/org.vue')
  const member = readSource('pages/contacts/member.vue')

  for (const url of ['summary', 'orgs', 'members']) {
    assert.match(api, new RegExp(`url: '/system/contacts/${url}'`))
  }
  assert.match(api, /url: `\/system\/contacts\/members\/\$\{/)
  assert.match(index, /<AiTabBar active="contacts"/)
  assert.match(index, /api\.getContactsSummary\(\)/)
  assert.match(org, /api\.getContactOrgs\(current\.value\.id\)/)
  assert.match(member, /api\.getContactMember\(userId\)/)
  assert.match(member, /if \(!dialable\.value\)[\s\S]*uni\.makePhoneCall/)
  assert.match(member, /const dialable = computed\(\(\) => canDial\(member\.value\?\.phone\)\)/)
})

test('approval start and notice pages are native secondary pages', () => {
  const pages = readPagesJson()
  for (const path of ['pages/approval/start', 'pages/notice/index', 'pages/notice/detail']) {
    const page = pages.pages.find(item => item.path === path)
    assert.ok(page, `${path} should be registered`)
    assert.notEqual(page.style?.navigationStyle, 'custom', `${path} should keep the native navigation bar`)
  }
})

test('notice and document-flow APIs keep their paths and encrypt write calls', () => {
  const api = readSource('api/index.js')
  const apiBlock = name => api.match(new RegExp(`${name}: [^\\n]*request\\(\\{[\\s\\S]*?\\}\\),`))?.[0] || ''
  for (const url of ['/system/notice/user/page', '/system/notice/user/unread-count', '/system/notice/markAsRead']) {
    assert.match(api, new RegExp(`url: '${url}'`))
  }
  assert.match(api, /url: `\/system\/notice\/user\/\$\{/)
  assert.match(api, /url: '\/ai\/business\/flow\/startable-objects'/)
  assert.match(api, /url: `\/ai\/business\/document\/\$\{[\s\S]*?\}\/runtime`/)
  assert.match(api, /url: `\/ai\/business\/flow\/start-config\/\$\{/)
  const writes = {
    markNoticeRead: '/system/notice/markAsRead',
    startBusinessDocumentFlow: '/ai/business/flow/start',
    resubmitBusinessDocumentFlow: '/ai/business/flow/resubmit',
    withdrawBusinessDocumentFlow: '/ai/business/flow/withdraw',
  }
  for (const [name, url] of Object.entries(writes)) {
    const block = apiBlock(name)
    assert.ok(block.includes(`url: '${url}'`), `${name} should call ${url}`)
    assert.match(block, /method: 'post'/, `${name} should be a POST`)
    assert.match(block, /encrypt: true/, `${name} should be encrypted`)
  }
})

test('workbench and todo both open the approval start page', () => {
  const home = readSource('pages/index/index.vue')
  const todo = readSource('pages/todo.vue')
  const start = readSource('pages/approval/start.vue')
  assert.match(home, /url: '\/pages\/approval\/start'/)
  assert.match(todo, /url: '\/pages\/approval\/start'/)
  assert.match(start, /authStore\.hasPermission\(FLOW_PERMISSIONS\.start\)/)
  assert.match(start, /api\.getStartableObjects\(\)/)
})

test('notice content is sanitized and notice unread counts toward the message tab', () => {
  const detail = readSource('pages/notice/detail.vue')
  const message = readSource('pages/message/index.vue')
  const tabBar = readSource('components/AiTabBar.vue')
  const badge = readSource('store/modules/badge.js')
  assert.match(detail, /sanitizeMessageHtml\(/)
  assert.doesNotMatch(detail, /v-html/)
  assert.match(detail, /noticeStore\.markRead\(/)
  assert.match(message, /<NoticeEntryRow \/>/)
  assert.match(tabBar, /key: 'message'[^\n]*badge: 'messageTabText'/)
  assert.match(badge, /messageTabText: state => formatBadgeCount\(state\.unreadCount \+ state\.noticeUnreadCount\)/)
})

test('lowcode runtime delegates document approval to a dedicated composable', () => {
  const runtime = readSource('pages/lowcode-runtime.vue')
  const flow = readSource('composables/lowcode/useLowcodeDocumentFlow.js')
  const footer = readSource('components/lowcode/LowcodeRuntimeFooter.vue')
  assert.ok(runtime.split('\n').length <= 640, 'lowcode-runtime.vue should stay within 640 lines')
  assert.match(runtime, /useLowcodeDocumentFlow\(/)
  assert.match(runtime, /<InitiatorSelectSheet \/>/)
  assert.equal((runtime.match(/@flow-action="runDocumentFlowAction"/g) || []).length, 3)
  for (const action of ['START', 'RESUBMIT', 'WITHDRAW', 'HANDLE']) {
    assert.match(flow, new RegExp(`DOCUMENT_FLOW_ACTION\\.${action}\\b`))
  }
  assert.match(flow, /api\.getBusinessFlowStartConfig\(/)
  assert.match(flow, /initiatorStore\.open\(nodes\)/)
  assert.match(flow, /comment: '申请人撤回'/)
  assert.match(footer, /documentFlowStore\.footerButtons\(props\.mode\)/)
})

test('flow collaboration registers cc detail with native navigation', () => {
  const page = readPagesJson().pages.find(item => item.path === 'pages/flow/cc-detail')
  assert.ok(page, 'pages/flow/cc-detail should be registered')
  assert.notEqual(page.style?.navigationStyle, 'custom')
})

test('flow collaboration APIs use flow service paths and encrypted writes', () => {
  const api = readSource('api/index.js')
  const expectations = [
    ['getMyCcPage', '/api/flow/cc/my', 'get'],
    ['getCcUnreadCount', '/api/flow/cc/unread/count', 'get'],
    ['getCcFormInfo', '/api/flow/cc/form/', 'get'],
    ['markCcRead', '/api/flow/cc/read/', 'post'],
    ['markAllCcRead', '/api/flow/cc/read/all', 'post'],
    ['remindFlowTask', '/api/flow/task/remind', 'post'],
    ['addFlowTaskSign', '/api/flow/task/add-sign', 'post'],
    ['reduceFlowTaskSign', '/api/flow/task/reduce-sign', 'post'],
    ['getFlowTaskSignRelations', '/sign-relations', 'get'],
  ]
  for (const [name, url, method] of expectations) {
    const start = api.indexOf(`${name}:`)
    assert.ok(start >= 0, `${name} should be defined`)
    const block = api.slice(start, api.indexOf('}),', start))
    assert.ok(block.includes(url), `${name} should call ${url}`)
    assert.match(block, new RegExp(`method: '${method}'`))
    assert.match(block, /encrypt: true/)
    assert.match(block, /needTip: false/)
  }
})

test('todo detail stays split after adding sign and remind actions', () => {
  const page = readSource('pages/todo-detail.vue')
  assert.ok(page.split('\n').length <= 600, 'todo-detail.vue should stay within 600 lines')
  for (const component of ['TodoSignSheet', 'TodoSignRelations', 'TodoRemindBar']) {
    assert.match(page, new RegExp(`<${component}\\b`))
    assert.ok(fs.existsSync(path.join(srcDir, `components/flow/${component}.vue`)))
  }
})

test('cc unread count stays on the todo page tab only', () => {
  const todo = readSource('pages/todo.vue')
  const tabBar = readSource('components/AiTabBar.vue')
  const badge = readSource('store/modules/badge.js')
  assert.match(todo, /\{ label: '抄送我的', value: 'cc' \}/)
  assert.match(todo, /scope\.value === 'cc' && ccStore\.unreadText/)
  assert.match(todo, /<CcListPanel v-if="isCcScope"/)
  assert.doesNotMatch(`${tabBar}\n${badge}`, /getCcUnreadCount|useCcStore|ccStore/)
})

test('colored app icons and transparent illustrations replace line icon tiles', () => {
  const menu = readSource('utils/mobile-menu.js')
  const appIcon = readSource('components/AiAppIcon.vue')
  const empty = readSource('components/AiEmpty.vue')
  const message = readSource('pages/message/index.vue')
  for (const key of ['approval', 'seal', 'shop', 'tool', 'layout', 'more', 'org', 'users', 'system', 'notice', 'sms', 'email']) {
    assert.ok(fs.existsSync(path.join(srcDir, `static/app-icons/${key}.png`)), `missing app icon ${key}`)
  }
  for (const type of ['empty', 'error', 'search']) {
    assert.ok(fs.existsSync(path.join(srcDir, `static/illustrations/${type}.png`)), `missing illustration ${type}`)
    assert.match(empty, new RegExp(`@/static/illustrations/${type}\\.png`))
  }
  assert.doesNotMatch(empty, /no-data\.png/)
  assert.match(menu, /if \(icon && isImageIcon\(icon\)\) return icon/)
  assert.match(appIcon, /<image v-if="imageSrc"/)
  assert.match(message, /<AiAppIcon :icon="messageCategoryIcon\(item\)" \/>/)
  assert.match(message, /'is-plain': tab\.key !== 'unread'/)
})

test('contacts header avoids a second logo and approval forms separate editable from readonly fields', () => {
  const contacts = readSource('pages/contacts/index.vue')
  const header = readSource('components/AiTabHeader.vue')
  const field = readSource('components/lowcode/LowcodeField.vue')
  const panel = readSource('components/flow/FlowBusinessFormPanel.vue')
  assert.match(contacts, /<AiTabHeader title="通讯录" :searchable="false" :show-org="false" \/>/)
  assert.match(contacts, /<AiButton v-if="all\.failed\.value"[^>]*@click="refreshAll">重新加载<\/AiButton>/)
  assert.match(header, /<template v-if="showOrg">/)
  const readonlyRule = field.match(/\.lowcode-field__readonly \{[^}]*\}/)[0]
  assert.doesNotMatch(readonlyRule, /background|border:/)
  assert.match(panel, /\.flow-business-form :deep\(\.ai-field__control:not\(\.is-disabled\)\)[\s\S]*background: var\(--forge-surface, #fff\)/)
})

test('refresh buttons are replaced by pull-down refresh on the real scroll surface', () => {
  const home = readSource('pages/index/index.vue')
  const contacts = readSource('pages/contacts/index.vue')
  const org = readSource('pages/contacts/org.vue')
  const pull = readSource('composables/usePullRefresh.js')
  assert.doesNotMatch(home, /refresh-cw|aria-label="刷新/)
  assert.match(home, /<AiTabHeader title="工作台"[^>]*\/>/)
  for (const [page, task] of [[contacts, 'refreshAll'], [org, 'reloadLevel']]) {
    assert.match(page, /class="contacts-scroll[\s\S]*?refresher-enabled[\s\S]*?@refresherrefresh="refreshByPull"/)
    assert.match(page, new RegExp(`usePullRefresh\\(${task}\\)`))
    assert.doesNotMatch(page, /onPullDownRefresh/)
  }
  assert.match(pull, /if \(refreshing\.value\) return/)
  assert.match(pull, /finally \{ refreshing\.value = false \}/)
})

test('mini program tab header leaves room for the capsule menu button', () => {
  const header = readSource('components/AiTabHeader.vue')
  assert.match(header, /\/\/ #ifdef MP[\s\S]*uni\.getMenuButtonBoundingClientRect[\s\S]*\/\/ #endif/)
  assert.match(header, /barHeight = \(capsule\.top - metrics\.statusBarHeight\) \* 2 \+ capsule\.height/)
  assert.match(header, /capsuleSpace = Number\(system\.windowWidth \|\| 0\) - capsule\.left \+ 8/)
  assert.match(header, /paddingRight: `\$\{navMetrics\.capsuleSpace\}px`/)
  assert.match(header, /class="ai-tab-header__bar" :style="barStyle"/)
})

test('workbench overview uses tinted stat tiles with icons', () => {
  const home = readSource('pages/index/index.vue')
  const homeStyle = readSource('pages/styles/home.scss')
  assert.match(home, /class="overview-item" :class="`is-\$\{item\.tone\}`"/)
  assert.match(home, /<image class="overview-icon" :src="item\.icon"/)
  for (const [key, tone] of [['approval', 'blue'], ['notice', 'orange'], ['file', 'green']])
    assert.match(home, new RegExp(`icon: overviewIcon\\('${key}'\\), tone: '${tone}'`))
  assert.match(homeStyle, /\.overview-list \{[^}]*gap: 8px;/)
  assert.doesNotMatch(homeStyle, /\.overview-item \+ \.overview-item::before/)
  for (const tone of ['blue', 'orange', 'green'])
    assert.match(homeStyle, new RegExp(`\\.overview-item\\.is-${tone} \\{ background: linear-gradient`))
})

test('workbench shows a two-slide banner above the overview card', () => {
  const home = readSource('pages/index/index.vue')
  const banner = readSource('components/home/HomeBanner.vue')
  assert.match(home, /<HomeBanner class="home-banner-slot" \/>[\s\S]*class="overview-card"/)
  assert.match(banner, /<swiper[\s\S]*:autoplay="banners\.length > 1"[\s\S]*circular/)
  assert.match(banner, /import approvalBanner from '@\/static\/banners\/approval\.jpg'/)
  assert.match(banner, /import teamBanner from '@\/static\/banners\/team\.jpg'/)
  assert.match(banner, /image: bannerSrc\(approvalBanner\)[\s\S]*image: bannerSrc\(teamBanner\)/)
  assert.match(banner, /\/\/ #ifdef H5\s+return resolveStaticUrl\(url\)\s+\/\/ #endif/)
  assert.match(banner, /url: '\/pages\/approval\/start'/)
  assert.match(banner, /uni\.switchTab\(\{ url: '\/pages\/contacts\/index' \}\)/)
  assert.match(banner, /padding-top: 45\.45%/)
  for (const name of ['approval', 'team'])
    assert.ok(fs.existsSync(path.join(srcDir, `static/banners/${name}.jpg`)), `missing banner ${name}`)
})

test('approval forms show readonly fields as a description list and child rows as cards', () => {
  const field = readSource('components/lowcode/LowcodeField.vue')
  const panel = readSource('components/flow/FlowBusinessFormPanel.vue')
  assert.match(field, /'lowcode-field--readonly-row': showsReadonlyText\.value/)
  assert.match(field, /v-else-if="showsReadonlyText"/)
  assert.match(panel, /:deep\(\.card-section\) \{[^}]*padding: 0;[^}]*background: transparent;/)
  assert.match(panel, /:deep\(\.lowcode-field--readonly-row\) \{ display: grid; grid-template-columns: 78px minmax\(0, 1fr\)/)
  assert.match(panel, /:deep\(\.lowcode-field--readonly-row \.lowcode-field__readonly\) \{ min-height: 0; padding: 0;/)
  assert.match(panel, /:deep\(\.section-card-row:last-child\) \{[^}]*border-radius: 10px;[^}]*background: var\(--forge-surface-subtle/)
  assert.match(panel, /:deep\(\.lowcode-form\.lowcode-form--inline-grid\) \{ gap: 0; \}/)
})

test('add sign is parallel and idempotent', () => {
  const actions = readSource('composables/flow/useTodoSignActions.js')
  const sign = readSource('utils/flow-sign.js')
  assert.match(sign, /SIGN_MODE_PARALLEL = 'PARALLEL'/)
  assert.match(actions, /signMode: SIGN_MODE_PARALLEL/)
  assert.match(actions, /createFlowActionCredentials\(action, payload\.taskId/)
  assert.match(actions, /api\.addFlowTaskSign\(payload\)/)
  assert.match(actions, /api\.reduceFlowTaskSign\(payload\)/)
})

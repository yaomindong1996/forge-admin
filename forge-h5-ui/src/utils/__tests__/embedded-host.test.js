import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { EMBEDDED_HOST_CLASS, isEmbeddedHost, markEmbeddedHost } from '../embedded-host.js'

const srcDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const readSource = relativePath => fs.readFileSync(path.join(srcDir, relativePath), 'utf8')

function withGlobals(globals, run) {
  const saved = Object.keys(globals).map(key => [key, Object.getOwnPropertyDescriptor(globalThis, key)])
  for (const [key, value] of Object.entries(globals))
    Object.defineProperty(globalThis, key, { value, configurable: true, writable: true })
  try {
    return run()
  }
  finally {
    for (const [key, descriptor] of saved) {
      if (descriptor) Object.defineProperty(globalThis, key, descriptor)
      else delete globalThis[key]
    }
  }
}

function fakeDocument() {
  const classes = new Set()
  return { documentElement: { classList: { add: name => classes.add(name), contains: name => classes.has(name) } } }
}

test('embedded host is detected from the in-app browser user agent', () => {
  const embeddedAgents = [
    'Mozilla/5.0 (iPhone) AppleWebKit/605.1.15 Mobile/15E148 AliApp(DingTalk/7.6.0) com.laiwang.DingTalk/1',
    'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Mobile Safari/537.36 wxwork/4.1.30',
  ]
  for (const userAgent of embeddedAgents)
    withGlobals({ navigator: { userAgent } }, () => assert.equal(isEmbeddedHost(), true, userAgent))
  const plain = 'Mozilla/5.0 (iPhone) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1'
  withGlobals({ navigator: { userAgent: plain } }, () => assert.equal(isEmbeddedHost(), false))
})

test('markEmbeddedHost only tags the html element inside an embedded host', () => {
  const embeddedDoc = fakeDocument()
  withGlobals({ navigator: { userAgent: 'AliApp(DingTalk/7.6.0)' }, document: embeddedDoc }, () => {
    assert.equal(markEmbeddedHost(), true)
  })
  assert.equal(embeddedDoc.documentElement.classList.contains(EMBEDDED_HOST_CLASS), true)

  const plainDoc = fakeDocument()
  withGlobals({ navigator: { userAgent: 'Safari/604.1' }, document: plainDoc }, () => {
    assert.equal(markEmbeddedHost(), false)
  })
  assert.equal(plainDoc.documentElement.classList.contains(EMBEDDED_HOST_CLASS), false)
})

test('embedded host hides the native H5 nav bar before the app mounts', () => {
  const main = readSource('main.js')
  const global = readSource('styles/global.css')
  const tabHeader = readSource('components/AiTabHeader.vue')
  assert.match(main, /\/\/ #ifdef H5\s+markEmbeddedHost\(\)\s+\/\/ #endif[\s\S]*createSSRApp\(App\)/)
  assert.match(global, /html\.forge-embedded-host \{\s*--window-top: 0px !important;\s*--forge-page-height: 100vh;/)
  assert.match(global, /html\.forge-embedded-host uni-page-head \{\s*display: none !important;/)
  assert.match(tabHeader, /import \{ isEmbeddedHost \} from '@\/utils\/embedded-host'/)
  assert.doesNotMatch(tabHeader, /function isEmbeddedHost/)
})

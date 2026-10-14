import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildLoginUrl,
  HOME_PAGE,
  LOGIN_PAGE,
  resolveBrowserRouteUrl,
  sanitizeLoginRedirect,
} from '../route.js'

test('resolveBrowserRouteUrl keeps absolute http urls', () => {
  assert.equal(
    resolveBrowserRouteUrl('https://example.com/forge-h5/#/pages/todo'),
    'https://example.com/forge-h5/#/pages/todo',
  )
})

test('resolveBrowserRouteUrl uses root hash on local base', () => {
  assert.equal(resolveBrowserRouteUrl('/pages/todo'), '/#/pages/todo')
})

test('sanitizeLoginRedirect unwraps nested login redirects', () => {
  const nested = `${LOGIN_PAGE}?redirect=${encodeURIComponent(`${LOGIN_PAGE}?redirect=${encodeURIComponent('/pages/todo')}`)}`
  assert.equal(sanitizeLoginRedirect(nested), '/pages/todo')
  assert.equal(sanitizeLoginRedirect(LOGIN_PAGE), HOME_PAGE)
  assert.equal(sanitizeLoginRedirect('/pages/mine/index'), '/pages/mine/index')
})

test('buildLoginUrl never nests login page as redirect target', () => {
  assert.equal(buildLoginUrl(LOGIN_PAGE), LOGIN_PAGE)
  assert.equal(
    buildLoginUrl(`${LOGIN_PAGE}?redirect=${encodeURIComponent(LOGIN_PAGE)}`),
    LOGIN_PAGE,
  )
  assert.equal(
    buildLoginUrl('/pages/todo'),
    `${LOGIN_PAGE}?redirect=${encodeURIComponent('/pages/todo')}`,
  )
})

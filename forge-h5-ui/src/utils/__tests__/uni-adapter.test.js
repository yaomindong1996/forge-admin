import assert from 'node:assert/strict'
import test from 'node:test'
import { resolvePlatformRequestBaseURL, resolveRequestUrl } from '../http/uni-adapter.js'

test('keeps H5 relative proxy prefixes unchanged', () => {
  assert.equal(resolvePlatformRequestBaseURL('/dev-api', ''), '/dev-api')
  assert.equal(resolveRequestUrl('/dev-api', '/auth/login'), '/dev-api/auth/login')
})

test('builds absolute mini-program gateway URLs without changing API paths', () => {
  const baseURL = resolvePlatformRequestBaseURL('/forge-h5-api', 'https://gateway.example.com/')
  assert.equal(baseURL, 'https://gateway.example.com/forge-h5-api')
  assert.equal(resolveRequestUrl(baseURL, '/api/flow/task/todo'), 'https://gateway.example.com/forge-h5-api/api/flow/task/todo')
})

test('does not rewrite already absolute request URLs', () => {
  assert.equal(resolveRequestUrl('https://gateway.example.com/base', 'https://files.example.com/a.png'), 'https://files.example.com/a.png')
})

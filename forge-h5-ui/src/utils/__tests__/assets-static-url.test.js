import assert from 'node:assert/strict'
import test from 'node:test'

/**
 * 与 src/utils/assets.js 子路径部署分支保持同步的契约测试。
 * uni-app <image> 会给以 / 开头的路径再拼 Vite base，因此子路径部署必须返回 ./static/...
 */
function createResolver(publicPath) {
  function getPublicPath() {
    const value = String(publicPath || '/').trim()
    if (!value || value === '/')
      return '/'
    if (value === './' || value === '.')
      return './'
    return `/${value.replace(/^\/+|\/+$/g, '')}/`
  }

  function resolveStaticUrl(pathValue) {
    const value = String(pathValue || '').trim()
    if (!value)
      return ''
    if (/^(https?:|data:|blob:)/i.test(value))
      return value

    const pub = getPublicPath()
    let normalizedPath = value.replace(/^\/+/, '')
    const normalizedPublicPath = pub.replace(/^\/+|\/+$/g, '')

    if (normalizedPublicPath && normalizedPath.startsWith(`${normalizedPublicPath}/`))
      normalizedPath = normalizedPath.slice(normalizedPublicPath.length + 1)

    if (pub === './')
      return `./${normalizedPath}`
    if (!normalizedPublicPath)
      return `/${normalizedPath}`
    return `./${normalizedPath}`
  }

  return { resolveStaticUrl }
}

test('resolveStaticUrl uses relative path under subpath deploy', () => {
  const { resolveStaticUrl } = createResolver('/forge-h5')
  assert.equal(resolveStaticUrl('/static/logo.png'), './static/logo.png')
  assert.equal(resolveStaticUrl('static/images/login-bg.png'), './static/images/login-bg.png')
  assert.equal(resolveStaticUrl('/forge-h5/static/logo.png'), './static/logo.png')
  assert.equal(resolveStaticUrl('/forge-h5/assets/approval-H07ztBwC.jpg'), './assets/approval-H07ztBwC.jpg')
})

test('resolveStaticUrl keeps root-absolute path on root deploy', () => {
  const { resolveStaticUrl } = createResolver('/')
  assert.equal(resolveStaticUrl('/static/logo.png'), '/static/logo.png')
})

test('resolveStaticUrl keeps external urls', () => {
  const { resolveStaticUrl } = createResolver('/forge-h5')
  assert.equal(resolveStaticUrl('https://cdn.example/logo.png'), 'https://cdn.example/logo.png')
  assert.equal(resolveStaticUrl('data:image/png;base64,abc'), 'data:image/png;base64,abc')
})

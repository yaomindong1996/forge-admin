/**
 * 第五轮冒烟：数据权限 / 幂等 / 文件上传下载鉴权
 */
import http from 'node:http'
import fs from 'node:fs'
import { createRequire } from 'node:module'
import { randomUUID } from 'node:crypto'

const require = createRequire(import.meta.url)
const JSEncryptModule = require('jsencrypt-ext')
const JSEncrypt = JSEncryptModule.JSEncrypt || JSEncryptModule.default?.JSEncrypt || JSEncryptModule

const ADMIN = { host: '127.0.0.1', port: 8580 }
const FLOW = { host: '127.0.0.1', port: 8081 }
const report = { passed: [], failed: [], notes: [] }

function ok(name, detail) { report.passed.push({ name, detail }); console.log('PASS', name, detail || '') }
function fail(name, detail) { report.failed.push({ name, detail }); console.error('FAIL', name, detail || '') }
function note(msg) { report.notes.push(msg); console.log('NOTE', msg) }

function req(target, method, pathName, body, headers = {}) {
  return new Promise((resolve, reject) => {
    let data = null
    if (body != null) {
      data = Buffer.isBuffer(body) ? body : (typeof body === 'string' ? body : JSON.stringify(body))
    }
    const r = http.request({
      hostname: target.host, port: target.port, path: pathName, method,
      headers: {
        ...(data && !headers['Content-Type'] ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(data && headers['Content-Type'] ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...headers,
      },
    }, (res) => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        const text = buf.toString('utf8')
        let json = null
        try { json = JSON.parse(text) } catch {}
        resolve({ status: res.statusCode, headers: res.headers, json, text, buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function wrapPem(publicKey) {
  if (publicKey.includes('BEGIN')) return publicKey
  return `-----BEGIN PUBLIC KEY-----\n${publicKey.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
}

async function login(username = 'admin', password = '123456') {
  const pk = (await req(ADMIN, 'GET', '/crypto/public-key')).json.data.publicKey
  const enc = new JSEncrypt(); enc.setPublicKey(wrapPem(pk))
  const L = (await req(ADMIN, 'POST', '/auth/login', {
    username, password: enc.encrypt(password), userClient: 'pc', appId: 'pc', authType: 'password', tenantId: 1,
  })).json
  if (L.code !== 200) throw new Error(`${username} login fail: ${L.message}`)
  return {
    username,
    userId: L.data.userInfo.userId,
    token: L.data.accessToken,
    auth: { Authorization: `Bearer ${L.data.accessToken}` },
  }
}

// —— 1 数据权限 ——
async function testDataScope(admin) {
  console.log('\n=== 1) 数据权限 datascope ===')
  const configs = (await req(ADMIN, 'GET', '/system/dataScopeConfig/list', null, admin.auth)).json
  const rows = configs.data || []
  const userMapper = rows.find(r => String(r.mapperMethod || '').includes('SysUserMapper.selectUserPage'))
  const orgMapper = rows.find(r => String(r.mapperMethod || '').includes('SysOrgMapper.selectOrgPage'))
  if (rows.length >= 1) ok('数据权限-配置存在', `n=${rows.length}`)
  else fail('数据权限-配置存在', JSON.stringify(configs))
  if (userMapper && Number(userMapper.enabled ?? userMapper.status ?? 1) === 1) {
    ok('数据权限-用户分页已启用', userMapper.mapperMethod)
  } else fail('数据权限-用户分页已启用', JSON.stringify(userMapper))
  if (orgMapper && Number(orgMapper.enabled ?? orgMapper.status ?? 1) === 1) {
    ok('数据权限-组织分页已启用', orgMapper.mapperMethod)
  } else fail('数据权限-组织分页已启用', JSON.stringify(orgMapper))

  const refresh = (await req(ADMIN, 'POST', '/system/dataScopeConfig/refreshCache', null, admin.auth)).json
  if (refresh.code === 200) ok('数据权限-刷新缓存', '')
  else note(`refreshCache: ${refresh.code} ${refresh.message}`)

  const adminOrg = (await req(ADMIN, 'GET', '/system/org/page?pageNum=1&pageSize=100', null, admin.auth)).json
  const adminTotal = Number(adminOrg.data?.total || 0)
  ok('数据权限-管理员组织全量', `total=${adminTotal}`)

  // test 用户无菜单角色但仍绑定组织，组织分页走 datascope 拦截
  const limited = await login('test')
  const limitedOrg = (await req(ADMIN, 'GET', '/system/org/page?pageNum=1&pageSize=100', null, limited.auth)).json
  const limitedTotal = Number(limitedOrg.data?.total || 0)
  const limitedIds = (limitedOrg.data?.records || []).map(r => r.id)
  note(`test org ids=${limitedIds.join(',')}`)

  if (limitedOrg.code === 200 && limitedTotal > 0 && limitedTotal < adminTotal) {
    ok('数据权限-受限用户组织收敛', `admin=${adminTotal} test=${limitedTotal}`)
  } else if (limitedOrg.code === 200 && limitedTotal === adminTotal) {
    fail('数据权限-受限用户组织收敛', `未收敛 admin=${adminTotal} test=${limitedTotal}`)
  } else {
    fail('数据权限-受限用户组织查询', JSON.stringify(limitedOrg).slice(0, 250))
  }

  // 用户分页：admin 全量；h5_admin 有 user:list 但角色范围更窄时可能少见部分用户
  const adminUsers = (await req(ADMIN, 'GET', '/system/user/page?pageNum=1&pageSize=100', null, admin.auth)).json
  const h5 = await login('h5_admin')
  const h5Users = (await req(ADMIN, 'GET', '/system/user/page?pageNum=1&pageSize=100', null, h5.auth)).json
  const aTotal = Number(adminUsers.data?.total || 0)
  const hTotal = Number(h5Users.data?.total || 0)
  if (h5Users.code === 200 && aTotal >= hTotal) {
    ok('数据权限-用户分页范围', `admin=${aTotal} h5_admin=${hTotal}`)
  } else {
    fail('数据权限-用户分页范围', `admin=${aTotal} h5=${hTotal} code=${h5Users.code}`)
  }
}

// —— 2 幂等 ——
async function testIdempotent(admin) {
  console.log('\n=== 2) 幂等 ===')
  const title = `R5-IDEM-${Date.now()}`
  const created = (await req(ADMIN, 'POST', '/business/sample-purchase-order/add', {
    title, supplierName: '幂等供应商', amountCent: 1500, purchaseItems: 'idem', needDate: '2026-12-31',
  }, admin.auth)).json
  if (created.code !== 200) { fail('幂等-创建采购单', JSON.stringify(created)); return }
  const id = created.data
  ok('幂等-创建采购单', `id=${id}`)

  const body = {
    deptLeaderId: 1, engineeringManagerId: 1,
    countersignUserIds: [1, 70], ccRoleKeys: ['admin'],
  }
  const s1 = (await req(ADMIN, 'POST', `/business/sample-purchase-order/submit/${id}`, body, admin.auth)).json
  const s2 = (await req(ADMIN, 'POST', `/business/sample-purchase-order/submit/${id}`, body, admin.auth)).json
  console.log('submit1', s1.code, s1.data || s1.message)
  console.log('submit2', s2.code, s2.message)
  if (s1.code === 200 && s1.data) ok('幂等-首次发起流程', `pi=${s1.data}`)
  else fail('幂等-首次发起流程', JSON.stringify(s1))
  if (s2.code !== 200) ok('幂等-重复发起拒绝', s2.message)
  else fail('幂等-重复发起拒绝', '第二次仍成功')

  await new Promise(r => setTimeout(r, 800))
  const todo = (await req(FLOW, 'GET', '/api/flow/task/todo?pageNum=1&pageSize=50', null, admin.auth)).json
  const task = (todo.data?.records || []).find(t => String(t.processInstanceId) === String(s1.data))
  if (!task) { fail('幂等-无待办', ''); return }
  note(`taskId=${task.taskId}`)

  const ik = randomUUID()
  const digest = `smoke-r5-${ik.slice(0, 8)}`
  const a1 = (await req(FLOW, 'POST', '/api/flow/task/approve', {
    taskId: String(task.taskId), comment: 'r5-idem', idempotencyKey: ik, requestDigest: digest,
  }, admin.auth)).json
  const a2 = (await req(FLOW, 'POST', '/api/flow/task/approve', {
    taskId: String(task.taskId), comment: 'r5-idem', idempotencyKey: ik, requestDigest: digest,
  }, admin.auth)).json
  const a3 = (await req(FLOW, 'POST', '/api/flow/task/approve', {
    taskId: String(task.taskId), comment: 'r5-other', idempotencyKey: ik, requestDigest: 'other-digest',
  }, admin.auth)).json
  console.log('approve1', a1.code, a1.message)
  console.log('approve2', a2.code, a2.message)
  console.log('approve3', a3.code, a3.message)

  if (a1.code === 200) ok('幂等-审批首次', '')
  else fail('幂等-审批首次', JSON.stringify(a1))
  if (a2.code === 200) ok('幂等-同键同摘要重放', '返回成功（幂等命中）')
  else fail('幂等-同键同摘要重放', JSON.stringify(a2))
  if (a3.code !== 200 && String(a3.message || '').includes('幂等')) {
    ok('幂等-同键异摘要冲突', a3.message)
  } else {
    fail('幂等-同键异摘要冲突', JSON.stringify(a3))
  }
}

// —— 3 文件鉴权 ——
async function testFileAuth(admin) {
  console.log('\n=== 3) 文件上传/下载鉴权 ===')
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
    'base64',
  )
  const boundary = '----ForgeSmokeR5'
  const body = Buffer.concat([
    Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="smoke-r5.png"\r\nContent-Type: image/png\r\n\r\n`),
    png,
    Buffer.from(`\r\n--${boundary}\r\nContent-Disposition: form-data; name="businessType"\r\n\r\ncommon\r\n`),
    Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="isPrivate"\r\n\r\ntrue\r\n`),
    Buffer.from(`--${boundary}--\r\n`),
  ])
  const uploaded = (await req(ADMIN, 'POST', '/api/file/upload', body, {
    ...admin.auth,
    'Content-Type': `multipart/form-data; boundary=${boundary}`,
  })).json
  console.log('upload', uploaded.code, uploaded.data?.fileId || uploaded.message)
  if (uploaded.code !== 200 || !uploaded.data?.fileId) {
    fail('文件-上传私有', JSON.stringify(uploaded))
    return
  }
  const fileId = uploaded.data.fileId
  ok('文件-上传私有', `fileId=${fileId}`)

  const noAuth = await req(ADMIN, 'GET', `/api/file/download/${fileId}`)
  if (noAuth.status === 401 || noAuth.json?.code === 401) ok('文件-无Token拒绝', noAuth.json?.message || `status=${noAuth.status}`)
  else fail('文件-无Token拒绝', noAuth.text.slice(0, 160))

  const withAuth = await req(ADMIN, 'GET', `/api/file/download/${fileId}`, null, admin.auth)
  const looksPng = withAuth.status === 200
    && (String(withAuth.headers['content-type'] || '').includes('image') || withAuth.buf?.[0] === 0x89)
  if (looksPng) {
    ok('文件-有Token可下载', `bytes=${withAuth.buf.length} cache=${withAuth.headers['cache-control'] || ''}`)
  } else {
    fail('文件-有Token可下载', withAuth.text.slice(0, 160))
  }
  if (String(withAuth.headers['cache-control'] || '').includes('no-store')) {
    ok('文件-私有下载禁缓存', withAuth.headers['cache-control'])
  } else {
    note(`cache-control=${withAuth.headers['cache-control']}`)
  }

  const other = await login('xiaomi')
  const otherDl = await req(ADMIN, 'GET', `/api/file/download/${fileId}`, null, other.auth)
  if (otherDl.status === 403 || otherDl.json?.code === 403) {
    ok('文件-他人无权读取', otherDl.json?.message || '403')
  } else {
    fail('文件-他人无权读取', otherDl.text.slice(0, 160))
  }

  const url = (await req(ADMIN, 'GET', `/api/file/url/${fileId}`, null, admin.auth)).json
  if (url.code === 200 && url.data) ok('文件-获取访问URL', String(url.data).slice(0, 80))
  else fail('文件-获取访问URL', JSON.stringify(url))

  const beforeLog = (await req(ADMIN, 'GET', '/system/file/download-log/page?pageNum=1&pageSize=5', null, admin.auth)).json
  // 再下一遍确保日志
  await req(ADMIN, 'GET', `/api/file/download/${fileId}`, null, admin.auth)
  await new Promise(r => setTimeout(r, 800))
  const afterLog = (await req(ADMIN, 'GET', '/system/file/download-log/page?pageNum=1&pageSize=10', null, admin.auth)).json
  const beforeTotal = Number(beforeLog.data?.total || 0)
  const afterTotal = Number(afterLog.data?.total || 0)
  const hit = (afterLog.data?.records || []).find(r => String(r.fileId || '') === String(fileId)
    || String(r.requestPath || '').includes(fileId))
  if (afterTotal > beforeTotal || hit) {
    ok('文件-下载日志', `total ${beforeTotal}->${afterTotal} hit=${Boolean(hit)}`)
  } else {
    note(`下载日志未写入（可能仅记 attachment 导出） total ${beforeTotal}->${afterTotal}`)
  }

  const del = (await req(ADMIN, 'DELETE', `/api/file/${fileId}`, null, admin.auth)).json
  if (del.code === 200) ok('文件-删除清理', '')
  else note(`delete: ${del.code} ${del.message}`)
}

async function main() {
  console.log('=== Forge 第五轮冒烟：数据权限 / 幂等 / 文件鉴权 ===')
  const admin = await login()
  await testDataScope(admin)
  await testIdempotent(admin)
  await testFileAuth(admin)

  console.log('\n=== 汇总 ===')
  console.log(`PASS=${report.passed.length} FAIL=${report.failed.length}`)
  report.notes.forEach(n => console.log('NOTE:', n))
  fs.writeFileSync('/tmp/forge_smoke_round5.json', JSON.stringify(report, null, 2))
  if (report.failed.length) process.exit(1)
}

main().catch((e) => { console.error(e); process.exit(1) })

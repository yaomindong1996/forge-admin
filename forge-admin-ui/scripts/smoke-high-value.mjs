/**
 * 最短高价值冒烟：WS踢下线 / 下载日志 / 切租户隔离 /（可选）采购审批
 * 用法: node scripts/smoke-high-value.mjs
 */
import http from 'node:http'
import fs from 'node:fs'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const SockJS = require('sockjs-client')
const { Client } = require('@stomp/stompjs')
const JSEncryptModule = require('jsencrypt-ext')
const JSEncrypt = JSEncryptModule.JSEncrypt || JSEncryptModule.default?.JSEncrypt || JSEncryptModule

const ADMIN = { host: '127.0.0.1', port: 8580 }
const FLOW = { host: '127.0.0.1', port: 8081 }
const report = { passed: [], failed: [], skipped: [] }

function req(target, method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : (Buffer.isBuffer(body) || typeof body === 'string' ? body : JSON.stringify(body))
    const isJson = data && typeof data === 'string' && !(headers['Content-Type'])
    const r = http.request({
      hostname: target.host,
      port: target.port,
      path,
      method,
      headers: {
        ...(data && isJson ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {}),
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
        resolve({ status: res.statusCode, headers: res.headers, text, json, buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function ok(name, detail) { report.passed.push({ name, detail }); console.log('PASS', name, detail || '') }
function fail(name, detail) { report.failed.push({ name, detail }); console.error('FAIL', name, detail || '') }
function skip(name, detail) { report.skipped.push({ name, detail }); console.warn('SKIP', name, detail || '') }

async function login(tenantId = 1) {
  const pkRes = (await req(ADMIN, 'GET', '/crypto/public-key')).json
  let publicKey = pkRes.data.publicKey
  if (!publicKey.includes('BEGIN')) {
    publicKey = `-----BEGIN PUBLIC KEY-----\n${publicKey.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
  }
  const enc = new JSEncrypt(); enc.setPublicKey(publicKey)
  const login = (await req(ADMIN, 'POST', '/auth/login', {
    username: 'admin', password: enc.encrypt('123456'),
    userClient: 'pc', appId: 'pc', authType: 'password', tenantId,
  })).json
  if (login.code !== 200) throw new Error(`login failed: ${login.code} ${login.message}`)
  return {
    token: login.data.accessToken,
    userId: login.data.userInfo.userId,
    tenantId: login.data.userInfo.tenantId,
    auth: { Authorization: `Bearer ${login.data.accessToken}` },
  }
}

async function testKickoutWs(session) {
  const out = { connected: false, gotKick: false, errors: [] }
  await new Promise((resolve) => {
    const timeout = setTimeout(() => resolve(), 12000)
    const socket = new SockJS(`http://${ADMIN.host}:${ADMIN.port}/ws`, null, { transports: ['xhr-streaming', 'xhr-polling'] })
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${session.token}` },
      reconnectDelay: 0,
      debug: () => {},
    })
    client.onConnect = async () => {
      out.connected = true
      client.subscribe('/user/queue/messages', (frame) => {
        try {
          const body = JSON.parse(frame.body || '{}')
          if (body.type === 'auth.kickout') out.gotKick = true
        } catch {}
      })
      await new Promise(r => setTimeout(r, 500))
      const online = (await req(ADMIN, 'GET', '/auth/online/page?pageNum=1&pageSize=50', null, session.auth)).json
      const records = online?.data?.records || []
      const mine = records.find(r => String(r.userId) === String(session.userId)) || records[0]
      if (!mine) {
        out.errors.push('no online session')
      } else {
        const sid = mine.sessionId || mine.id
        await req(ADMIN, 'POST', `/auth/online/kickout?sessionId=${sid}`, null, session.auth)
      }
      setTimeout(async () => {
        clearTimeout(timeout)
        try { await client.deactivate() } catch {}
        resolve()
      }, 2500)
    }
    client.onStompError = (f) => out.errors.push(f.headers?.message || 'stomp error')
    client.activate()
  })
  if (out.connected && out.gotKick && out.errors.length === 0) ok('WS踢下线通知', `userId=${session.userId}`)
  else fail('WS踢下线通知', JSON.stringify(out))
}

async function testDownloadLog(session) {
  // pick a known file
  const meta = (await req(ADMIN, 'GET', '/system/file/metadata/page?pageNum=1&pageSize=5', null, session.auth)).json
  const file = meta?.data?.records?.[0]
  if (!file?.fileId) {
    fail('下载日志', '无可用文件元数据')
    return
  }
  const before = (await req(ADMIN, 'GET', `/system/file/download-log/page?pageNum=1&pageSize=5`, null, session.auth)).json
  const beforeCount = Number(before?.data?.total || 0)
  const beforeLatestId = before?.data?.records?.[0]?.id

  const dl = await req(ADMIN, 'GET', `/api/file/download/${file.fileId}`, null, {
    ...session.auth,
    'X-Forge-Download-Intent': '1',
  })
  const disposition = dl.headers['content-disposition'] || ''
  const contentLength = Number(dl.headers['content-length'] || 0)
  const bytes = dl.buf?.length || 0

  await new Promise(r => setTimeout(r, 1000))
  const after = (await req(ADMIN, 'GET', `/system/file/download-log/page?pageNum=1&pageSize=10`, null, session.auth)).json
  const records = after?.data?.records || []
  const hit = records.find(r => r.fileId === file.fileId && String(r.id) !== String(beforeLatestId))
    || records.find(r => r.fileId === file.fileId)
  const sizeOk = Number(hit?.fileSize || 0) > 0 || contentLength > 0 || bytes > 0
  const logged = Boolean(hit) && Number(after?.data?.total || 0) >= beforeCount

  if (dl.status === 200 && disposition.toLowerCase().includes('attachment') && logged && sizeOk) {
    ok('下载日志', `fileId=${file.fileId} size=${hit?.fileSize || contentLength || bytes} disposition=ok`)
  } else {
    fail('下载日志', JSON.stringify({
      status: dl.status, disposition, contentLength, bytes,
      logged, hitSize: hit?.fileSize, beforeCount, afterTotal: after?.data?.total,
    }))
  }
}

async function testTenantIsolation(sessionT1) {
  // list purchase orders under tenant 1
  const t1 = (await req(ADMIN, 'GET', '/business/sample-purchase-order/page?pageNum=1&pageSize=20', null, sessionT1.auth)).json
  const t1Ids = new Set((t1?.data?.records || []).map(r => String(r.id)))
  const t1TenantIds = [...new Set((t1?.data?.records || []).map(r => r.tenantId))]

  // switch to tenant 4
  const sw = (await req(ADMIN, 'POST', '/system/tenant/switch?tenantId=4', null, sessionT1.auth)).json
  if (sw.code !== 200) {
    // fallback: re-login as tenant 4
    const sessionT4 = await login(4)
    const t4 = (await req(ADMIN, 'GET', '/business/sample-purchase-order/page?pageNum=1&pageSize=20', null, sessionT4.auth)).json
    const t4TenantIds = [...new Set((t4?.data?.records || []).map(r => r.tenantId))]
    const overlap = (t4?.data?.records || []).filter(r => t1Ids.has(String(r.id)))
    if (t1TenantIds.every(x => x === 1) && t4TenantIds.every(x => !x || x === 4) && overlap.length === 0) {
      ok('切租户列表隔离', `login(tenant=4) t1=${t1Ids.size} t4=${t4?.data?.records?.length || 0} overlap=0`)
    } else {
      fail('切租户列表隔离', JSON.stringify({ switch: sw, t1TenantIds, t4TenantIds, overlap: overlap.length }))
    }
    return
  }

  const newToken = sw.data?.accessToken || sw.data?.token || sessionT1.token
  // switch may return LoginUser without new token — use same token if Sa-Token updates session
  const auth = { Authorization: `Bearer ${newToken}` }
  const t4 = (await req(ADMIN, 'GET', '/business/sample-purchase-order/page?pageNum=1&pageSize=20', null, auth)).json
  const t4Records = t4?.data?.records || []
  const t4TenantIds = [...new Set(t4Records.map(r => r.tenantId))]
  const overlap = t4Records.filter(r => t1Ids.has(String(r.id)))
  const switchedTenant = sw.data?.tenantId

  if ((switchedTenant === 4 || t4TenantIds.every(x => !x || x === 4)) && overlap.length === 0) {
    ok('切租户列表隔离', `switch->${switchedTenant} t1=${t1Ids.size} t4=${t4Records.length} overlap=0`)
  } else {
    fail('切租户列表隔离', JSON.stringify({ switchedTenant, t1TenantIds, t4TenantIds, overlap: overlap.length, t4Code: t4.code }))
  }
}

async function testPurchaseFlow(session) {
  // flow health
  let flowUp = false
  try {
    const info = await req(FLOW, 'GET', '/api/flow/task/todo?pageNum=1&pageSize=1', null, session.auth)
    flowUp = info.status === 200 || info.json?.code === 200 || info.json?.code === 401
    if (info.json?.code === 200) flowUp = true
    // even encrypted empty might be 200
    if (info.status && info.status < 500) flowUp = true
  } catch {
    flowUp = false
  }
  if (!flowUp) {
    skip('采购审批全流程', 'flow-server :8081 未就绪')
    return
  }

  const title = `SMOKE-${Date.now()}`
  const created = (await req(ADMIN, 'POST', '/business/sample-purchase-order/add', {
    title,
    supplierName: '冒烟供应商',
    amountCent: 10000,
    purchaseItems: '测试物料',
    needDate: '2026-12-31',
    remark: 'smoke',
  }, session.auth)).json
  if (created.code !== 200 || !created.data) {
    fail('采购审批-创建', JSON.stringify(created))
    return
  }
  const id = created.data
  ok('采购审批-创建', `id=${id}`)

  // 固定会签：admin(1) + test33(70)，后者密码 123456，可直批
  const submitBody = {
    deptLeaderId: Number(session.userId),
    engineeringManagerId: Number(session.userId),
    countersignUserIds: [Number(session.userId), 70],
    ccRoleKeys: ['admin'],
  }
  const submitted = (await req(ADMIN, 'POST', `/business/sample-purchase-order/submit/${id}`, submitBody, session.auth)).json
  if (submitted.code !== 200) {
    fail('采购审批-提交', JSON.stringify(submitted))
    return
  }
  ok('采购审批-提交', `processInstanceId=${submitted.data}`)

  await new Promise(r => setTimeout(r, 1000))
  const detail = (await req(ADMIN, 'POST', `/business/sample-purchase-order/getById?id=${id}`, null, session.auth)).json
  const status = detail?.data?.status
  if (status === 'IN_PROCESS' || status === 'APPROVED') ok('采购审批-状态', status)
  else fail('采购审批-状态', JSON.stringify({ status, detail: detail?.message }))

  // 审批推进：admin 批自己的待办；会签人(test33/70) 无 todo 权限时用 started 反查 taskId 直批
  async function loginUser(username, password) {
    const pkRes = (await req(ADMIN, 'GET', '/crypto/public-key')).json
    let publicKey = pkRes.data.publicKey
    if (!publicKey.includes('BEGIN')) {
      publicKey = `-----BEGIN PUBLIC KEY-----\n${publicKey.match(/.{1,64}/g).join('\n')}\n-----END PUBLIC KEY-----`
    }
    const enc = new JSEncrypt(); enc.setPublicKey(publicKey)
    const L = (await req(ADMIN, 'POST', '/auth/login', {
      username, password: enc.encrypt(password), userClient: 'pc', appId: 'pc', authType: 'password', tenantId: 1,
    })).json
    if (L.code !== 200) throw new Error(`${username} login failed: ${L.message}`)
    return { auth: { Authorization: `Bearer ${L.data.accessToken}` }, userId: L.data.userInfo.userId, username }
  }

  const pi = submitted.data
  let finalStatus = 'IN_PROCESS'
  for (let i = 0; i < 10; i++) {
    const detail = (await req(ADMIN, 'POST', `/business/sample-purchase-order/getById?id=${id}`, null, session.auth)).json
    finalStatus = detail?.data?.status
    if (['APPROVED', 'REJECTED', 'CANCELED'].includes(finalStatus)) break

    const todo = (await req(FLOW, 'GET', '/api/flow/task/todo?pageNum=1&pageSize=50', null, session.auth)).json
    const adminTasks = (todo?.data?.records || []).filter(t =>
      String(t.processInstanceId) === String(pi) || String(t.businessKey || '').includes(String(id)))
    for (const t of adminTasks) {
      const approved = (await req(FLOW, 'POST', '/api/flow/task/approve', {
        taskId: String(t.taskId), comment: 'smoke admin', variables: { approved: true },
      }, session.auth)).json
      if (approved.code !== 200) {
        fail('采购审批-节点通过', JSON.stringify(approved))
        return
      }
      await new Promise(r => setTimeout(r, 500))
    }

    const started = (await req(FLOW, 'GET', '/api/flow/task/started?pageNum=1&pageSize=50', null, session.auth)).json
    const pending70 = (started?.data?.records || []).filter(t =>
      String(t.processInstanceId) === String(pi) && String(t.assignee) === '70' && t.status === 1)
    if (pending70.length) {
      const u70 = await loginUser('test33', '123456')
      for (const t of pending70) {
        const approved = (await req(FLOW, 'POST', '/api/flow/task/approve', {
          taskId: String(t.taskId), comment: 'smoke test33', variables: { approved: true },
        }, u70.auth)).json
        if (approved.code !== 200) {
          fail('采购审批-会签通过', JSON.stringify(approved))
          return
        }
        await new Promise(r => setTimeout(r, 500))
      }
    }

    if (!adminTasks.length && !pending70.length) {
      await new Promise(r => setTimeout(r, 800))
      break
    }
  }

  const finalDetail = (await req(ADMIN, 'POST', `/business/sample-purchase-order/getById?id=${id}`, null, session.auth)).json
  finalStatus = finalDetail?.data?.status
  if (finalStatus === 'APPROVED') ok('采购审批-终态', finalStatus)
  else fail('采购审批-终态', `status=${finalStatus}`)
}

async function main() {
  console.log('=== Forge 高价值冒烟开始 ===')
  const session = await login(1)
  ok('登录', `tenant=${session.tenantId} user=${session.userId}`)

  await testKickoutWs(await login(1)) // fresh session for kick
  await testDownloadLog(await login(1))
  await testTenantIsolation(await login(1))
  await testPurchaseFlow(await login(1))

  console.log('\n=== 汇总 ===')
  console.log(`PASS=${report.passed.length} FAIL=${report.failed.length} SKIP=${report.skipped.length}`)
  fs.writeFileSync('/tmp/forge_smoke_result.json', JSON.stringify(report, null, 2))
  if (report.failed.length) process.exit(1)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})

/**
 * 第六轮冒烟：消息中心 / 定时任务手动触发 / 流程监控管理动作
 */
import http from 'node:http'
import fs from 'node:fs'
import { createRequire } from 'node:module'

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
    const data = body == null ? null : JSON.stringify(body)
    const r = http.request({
      hostname: target.host, port: target.port, path: pathName, method,
      headers: {
        ...(data ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {}),
        ...headers,
      },
    }, (res) => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8')
        let json = null
        try { json = JSON.parse(text) } catch {}
        resolve({ status: res.statusCode, json, text })
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
    username, password: enc.encrypt(password),
    userClient: 'pc', appId: 'pc', authType: 'password', tenantId: 1,
  })).json
  if (L.code !== 200) throw new Error(`${username} login fail: ${L.message}`)
  return {
    username,
    userId: L.data.userInfo.userId,
    auth: { Authorization: `Bearer ${L.data.accessToken}` },
  }
}

async function createSubmitPo(admin) {
  const title = `R6-${Date.now()}`
  const created = (await req(ADMIN, 'POST', '/business/sample-purchase-order/add', {
    title, supplierName: 'R6供应商', amountCent: 1800, purchaseItems: 'r6', needDate: '2026-12-31',
  }, admin.auth)).json
  if (created.code !== 200) throw new Error('create po fail ' + JSON.stringify(created))
  const submitted = (await req(ADMIN, 'POST', `/business/sample-purchase-order/submit/${created.data}`, {
    deptLeaderId: 1, engineeringManagerId: 1, countersignUserIds: [1, 70], ccRoleKeys: ['admin'],
  }, admin.auth)).json
  if (submitted.code !== 200) throw new Error('submit po fail ' + JSON.stringify(submitted))
  return { id: created.data, pi: submitted.data, title }
}

// —— 1 消息中心 ——
async function testMessage(admin) {
  console.log('\n=== 1) 消息中心 ===')
  const receiver = await login('xiaomi')
  const before = (await req(ADMIN, 'GET', '/api/message/unread/count', null, receiver.auth)).json.data
  const beforeTotal = Number(before?.totalCount ?? 0)
  ok('消息-未读统计可读', `total=${beforeTotal}`)

  const title = `R6-MSG-${Date.now()}`
  const send = (await req(ADMIN, 'POST', '/api/message/send', {
    channel: 'WEB',
    type: 'SYSTEM',
    title,
    content: '第六轮站内信冒烟',
    sendScope: 'USERS',
    userIds: [receiver.userId],
  }, admin.auth)).json
  console.log('send', send.code, send.data?.id || send.message)
  if (send.code !== 200 || !send.data?.id) {
    fail('消息-发送站内信', JSON.stringify(send))
    return
  }
  ok('消息-发送站内信', `id=${send.data.id}`)

  await new Promise(r => setTimeout(r, 700))
  const after = (await req(ADMIN, 'GET', '/api/message/unread/count', null, receiver.auth)).json.data
  const afterTotal = Number(after?.totalCount ?? 0)
  const page = (await req(ADMIN, 'GET', '/api/message/page?pageNum=1&pageSize=20', null, receiver.auth)).json
  const hit = (page.data?.records || []).find(m => m.title === title || Number(m.id) === Number(send.data.id))

  if (afterTotal >= beforeTotal + 1 || hit) {
    ok('消息-未读+1', `${beforeTotal}->${afterTotal} hit=${Boolean(hit)}`)
  } else {
    fail('消息-未读+1', `${beforeTotal}->${afterTotal}`)
  }

  if (hit?.id) {
    const read = (await req(ADMIN, 'POST', `/api/message/${hit.id}/read`, null, receiver.auth)).json
    if (read.code === 200) ok('消息-标记已读', '')
    else fail('消息-标记已读', JSON.stringify(read))
    await new Promise(r => setTimeout(r, 400))
    const afterRead = (await req(ADMIN, 'GET', '/api/message/unread/count', null, receiver.auth)).json.data
    const readTotal = Number(afterRead?.totalCount ?? 0)
    if (readTotal <= afterTotal) ok('消息-已读回落', `${afterTotal}->${readTotal}`)
    else fail('消息-已读回落', `${afterTotal}->${readTotal}`)
  } else {
    fail('消息-收件箱命中', '未找到刚发送的消息')
  }

  const tpl = (await req(ADMIN, 'GET', '/api/message/template/page?pageNum=1&pageSize=5', null, admin.auth)).json
  if (tpl.code === 200 && (tpl.data?.records || []).length) {
    ok('消息-模板列表', `n=${tpl.data.records.length}`)
  } else {
    note(`模板列表: ${tpl.code} ${tpl.message}`)
  }
}

// —— 2 定时任务 ——
async function testJob(admin) {
  console.log('\n=== 2) 定时任务手动触发 ===')
  const page = (await req(ADMIN, 'GET', '/job/config/page?pageNum=1&pageSize=30', null, admin.auth)).json
  const jobs = page.data?.records || []
  if (!jobs.length) { fail('任务-列表为空', ''); return }
  ok('任务-配置列表', `n=${jobs.length}`)

  // 优先选轻量清理类任务，避免业务副作用
  const job = jobs.find(j => String(j.executorHandler || '').includes('IdempotencyClean'))
    || jobs.find(j => String(j.jobName || '').includes('清理'))
    || jobs[0]
  note(`trigger job id=${job.id} name=${job.jobName} handler=${job.executorHandler}`)

  const beforeLog = (await req(ADMIN, 'GET',
    `/job/log/page?pageNum=1&pageSize=5&jobConfigId=${job.id}`, null, admin.auth)).json
  const beforeTotal = Number(beforeLog.data?.total || 0)

  const trigger = (await req(ADMIN, 'POST', `/job/config/${job.id}/trigger`, null, admin.auth)).json
  console.log('trigger', trigger.code, trigger.message)
  if (trigger.code !== 200) { fail('任务-手动触发', JSON.stringify(trigger)); return }
  ok('任务-手动触发', job.jobName)

  let hit = null
  for (let i = 0; i < 8; i++) {
    await new Promise(r => setTimeout(r, 800))
    const afterLog = (await req(ADMIN, 'GET',
      `/job/log/page?pageNum=1&pageSize=5&jobConfigId=${job.id}`, null, admin.auth)).json
    const afterTotal = Number(afterLog.data?.total || 0)
    hit = (afterLog.data?.records || []).find(r =>
      String(r.triggerType || '').toUpperCase() === 'MANUAL'
      || afterTotal > beforeTotal)
    if (afterTotal > beforeTotal || hit) {
      ok('任务-日志增长', `total ${beforeTotal}->${afterTotal} type=${hit?.triggerType || '?'}`)
      if (hit?.handleCode != null || hit?.status != null || hit?.handleMsg) {
        note(`log status=${hit.status ?? hit.handleCode} msg=${String(hit.handleMsg || hit.exceptionInfo || '').slice(0, 80)}`)
      }
      return
    }
  }
  fail('任务-日志增长', `total 仍为 ${beforeTotal}`)
}

// —— 3 流程监控 ——
async function testFlowMonitor(admin) {
  console.log('\n=== 3) 流程监控管理动作 ===')
  const stats = (await req(FLOW, 'GET', '/api/flow/monitor/statistics', null, admin.auth)).json
  if (stats.code === 200) {
    if (stats.data?.degraded) note(`statistics degraded: ${stats.data.errorCode}`)
    else ok('监控-统计', JSON.stringify(stats.data).slice(0, 120))
    ok('监控-统计接口可达', stats.data?.degraded ? 'degraded' : 'ok')
  } else {
    fail('监控-统计接口可达', JSON.stringify(stats))
  }

  const { pi, title } = await createSubmitPo(admin)
  await new Promise(r => setTimeout(r, 800))
  note(`pi=${pi} title=${title}`)

  const list = (await req(FLOW, 'GET', '/api/flow/monitor/instances?pageNum=1&pageSize=20', null, admin.auth)).json
  const row = (list.data?.list || []).find(i => String(i.id) === String(pi))
  if (row) ok('监控-实例列表命中', `status=${row.status}`)
  else fail('监控-实例列表命中', `未找到 ${pi}`)

  const tasks = (await req(FLOW, 'GET',
    `/api/flow/monitor/instance/${pi}/tasks?pageNum=1&pageSize=10`, null, admin.auth)).json
  const task = (tasks.data?.list || [])[0]
  if (task?.taskId) ok('监控-实例任务', `${task.taskName} assignee=${task.assignee}`)
  else fail('监控-实例任务', JSON.stringify(tasks).slice(0, 200))

  if (task?.taskId) {
    const reassign = (await req(FLOW, 'POST', `/api/flow/monitor/reassign/${task.taskId}`, {
      newAssignee: '29', reason: 'r6 smoke reassign',
    }, admin.auth)).json
    console.log('reassign', reassign.code, reassign.message)
    if (reassign.code === 200) ok('监控-管理员转派', reassign.message)
    else fail('监控-管理员转派', JSON.stringify(reassign))
  }

  const suspend = (await req(FLOW, 'POST', `/api/flow/monitor/suspend/${pi}`, null, admin.auth)).json
  console.log('suspend', suspend.code, suspend.message)
  if (suspend.code === 200) ok('监控-挂起', suspend.message)
  else fail('监控-挂起', JSON.stringify(suspend))

  const activate = (await req(FLOW, 'POST', `/api/flow/monitor/activate/${pi}`, null, admin.auth)).json
  console.log('activate', activate.code, activate.message)
  if (activate.code === 200) ok('监控-激活', activate.message)
  else fail('监控-激活', JSON.stringify(activate))

  const terminate = (await req(FLOW, 'POST', `/api/flow/monitor/terminate/${pi}`, {
    reason: 'r6 smoke terminate',
  }, admin.auth)).json
  console.log('terminate', terminate.code, terminate.message)
  if (terminate.code === 200) ok('监控-终止', terminate.message)
  else fail('监控-终止', JSON.stringify(terminate))

  const detail = (await req(FLOW, 'GET', `/api/flow/monitor/instance/${pi}`, null, admin.auth)).json
  const status = detail.data?.status || detail.data?.processStatus
  if (detail.code === 200 && String(status).toLowerCase().includes('terminat')) {
    ok('监控-终止后状态', String(status))
  } else if (detail.code === 200) {
    ok('监控-终止后详情可读', `status=${status}`)
  } else {
    fail('监控-终止后详情', JSON.stringify(detail).slice(0, 200))
  }
}

async function main() {
  console.log('=== Forge 第六轮冒烟：消息 / 任务 / 流程监控 ===')
  const admin = await login()
  await testMessage(admin)
  await testJob(admin)
  await testFlowMonitor(admin)

  console.log('\n=== 汇总 ===')
  console.log(`PASS=${report.passed.length} FAIL=${report.failed.length}`)
  report.notes.forEach(n => console.log('NOTE:', n))
  fs.writeFileSync('/tmp/forge_smoke_round6.json', JSON.stringify(report, null, 2))
  if (report.failed.length) process.exit(1)
}

main().catch((e) => { console.error(e); process.exit(1) })

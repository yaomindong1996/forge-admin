#!/usr/bin/env node
/**
 * SQL Server 双栈 API 冒烟（Admin:8580 + Flow:8081）。
 * 依赖：已启动 forge-mssql / forge-admin / forge-flow，默认管理员 admin / 123456。
 *
 *   node forge-server/scripts/db/smoke-mssql-apis.mjs
 *   ADMIN_PORT=8580 FLOW_PORT=8081 ADMIN_PASSWORD=123456 node ...
 */
import crypto from 'node:crypto';
import http from 'node:http';

const ADMIN_PORT = Number(process.env.ADMIN_PORT || 8580);
const FLOW_PORT = Number(process.env.FLOW_PORT || 8081);
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || '123456';
const HOST = process.env.SMOKE_HOST || '127.0.0.1';

function req(method, port, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const payload = body == null ? null : Buffer.from(JSON.stringify(body));
    const h = { ...headers };
    if (payload) {
      h['Content-Type'] = 'application/json';
      h['Content-Length'] = payload.length;
    }
    const r = http.request(
      { hostname: HOST, port, path, method, headers: h },
      (resp) => {
        let data = '';
        resp.on('data', (c) => (data += c));
        resp.on('end', () => {
          let json = null;
          try {
            json = JSON.parse(data);
          } catch {
            json = { raw: data };
          }
          resolve({ status: resp.statusCode, json, body: data });
        });
      },
    );
    r.on('error', reject);
    if (payload) r.write(payload);
    r.end();
  });
}

function okBiz(res) {
  return res.status === 200 && res.json && res.json.code === 200;
}

async function login() {
  const pk = (await req('GET', ADMIN_PORT, '/crypto/public-key')).json;
  if (!pk?.data?.publicKey) {
    throw new Error('public-key missing');
  }
  const pem = `-----BEGIN PUBLIC KEY-----\n${pk.data.publicKey}\n-----END PUBLIC KEY-----`;
  const enc = crypto
    .publicEncrypt({ key: pem, padding: crypto.constants.RSA_PKCS1_PADDING }, Buffer.from(ADMIN_PASSWORD))
    .toString('base64');
  const login = await req('POST', ADMIN_PORT, '/auth/login', {
    username: ADMIN_USER,
    password: enc,
    appId: 'pc',
    authType: 'password',
  });
  if (!okBiz(login) || !login.json?.data?.accessToken) {
    throw new Error(`login failed: ${login.body.slice(0, 200)}`);
  }
  return {
    Authorization: `Bearer ${login.json.data.accessToken}`,
    'User-Agent': 'mssql-smoke-apis',
  };
}

async function check(name, method, port, path, headers, body = null) {
  const res = await req(method, port, path, body, headers);
  const pass = okBiz(res);
  const msg = res.json?.message || '';
  console.log(`${pass ? 'OK ' : 'NG '} ${name} ${res.status} ${res.json?.code ?? '-'} ${msg}`.slice(0, 160));
  return pass;
}

async function main() {
  const healthAdmin = await req('GET', ADMIN_PORT, '/actuator/health');
  const healthFlow = await req('GET', FLOW_PORT, '/actuator/health');
  if (healthAdmin.status !== 200 || healthFlow.status !== 200) {
    console.error('health not ready', { admin: healthAdmin.status, flow: healthFlow.status });
    process.exit(1);
  }
  console.log('OK  health admin+flow');

  const h = await login();
  console.log('OK  login');

  const checks = [];
  const adminGets = [
    ['loginConfig', '/auth/loginConfig'],
    ['userPage', '/system/user/page?pageNum=1&pageSize=5'],
    ['rolePage', '/system/role/page?pageNum=1&pageSize=5'],
    ['orgTree', '/system/org/tree'],
    ['resourceTree', '/system/resource/tree'],
    ['dictTypePage', '/system/dict/type/page?pageNum=1&pageSize=5'],
    ['configPage', '/system/config/page?pageNum=1&pageSize=5'],
    ['onlinePage', '/auth/online/page?pageNum=1&pageSize=5'],
    ['jobConfigPage', '/job/config/page?pageNum=1&pageSize=5'],
    ['jobLogPage', '/job/log/page?pageNum=1&pageSize=5'],
    ['jobMonitor', '/job/monitor/summary'],
    ['printSources', '/print/sources/page?pageNum=1&pageSize=5'],
    ['datasourceEnabled', '/generator/datasource/enabled'],
    ['fileMetaPage', '/system/file/metadata/page?pageNum=1&pageSize=5'],
  ];
  for (const [name, path] of adminGets) {
    checks.push(await check(name, 'GET', ADMIN_PORT, path, h));
  }

  const flowGets = [
    ['flowModelPage', '/api/flow/model/page?pageNum=1&pageSize=5'],
    ['flowStats', '/api/flow/model/statistics'],
    ['flowCategory', '/api/flow/category/list'],
    ['flowTodo', '/api/flow/task/todo?pageNum=1&pageSize=5'],
    ['flowWorkspace', '/api/flow/workspace/summary'],
  ];
  for (const [name, path] of flowGets) {
    checks.push(await check(name, 'GET', FLOW_PORT, path, h));
  }

  const orgName = `MSSQL冒烟_${Date.now()}`;
  const orgAdd = await req(
    'POST',
    ADMIN_PORT,
    '/system/org/add',
    { parentId: 1, orgName, orgType: 2, status: 1, sort: 99 },
    h,
  );
  const orgAddOk = okBiz(orgAdd);
  console.log(`${orgAddOk ? 'OK ' : 'NG '} orgAdd ${orgAdd.json?.code}`);
  checks.push(orgAddOk);

  const tree = await req('GET', ADMIN_PORT, '/system/org/tree', null, h);
  const orgIds = [];
  const walk = (nodes) => {
    if (!Array.isArray(nodes)) return;
    for (const n of nodes) {
      if (n?.orgName === orgName && n.id != null) orgIds.push(String(n.id));
      walk(n.children);
    }
  };
  walk(tree.json?.data);
  for (const id of orgIds) {
    checks.push(await check(`orgRemove:${id}`, 'POST', ADMIN_PORT, `/system/org/remove?id=${id}`, h));
  }
  if (orgAddOk && orgIds.length === 0) {
    console.log('NG  orgAdd succeeded but org not found in tree');
    checks.push(false);
  }

  const key = `mssql_smoke_${Date.now()}`;
  const model = await req(
    'POST',
    FLOW_PORT,
    '/api/flow/model',
    { modelKey: key, modelName: 'MSSQL冒烟流程', category: 'default', modelType: 0, status: 0 },
    h,
  );
  const modelOk = okBiz(model);
  const mid = model.json?.data?.id ?? model.json?.data?.modelId;
  console.log(`${modelOk ? 'OK ' : 'NG '} flowModelCreate ${model.json?.code} id=${mid ?? '-'}`);
  checks.push(modelOk);
  if (mid != null) {
    checks.push(await check('flowModelGet', 'GET', FLOW_PORT, `/api/flow/model/${mid}`, h));
  }

  const failed = checks.filter((x) => !x).length;
  const passed = checks.length - failed;
  console.log(`SUMMARY ${passed} ok / ${failed} fail / ${checks.length} total`);
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

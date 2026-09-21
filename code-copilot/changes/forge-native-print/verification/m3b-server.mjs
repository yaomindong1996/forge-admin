import fs from 'node:fs'
const source = { applicationId: '2', sourceType: 'LOWCODE', pageId: '3', formKey: null, objectCode: 'purchase' }
export function printMockPlugin(root, sourceOverride = null) {
  const activeSource = sourceOverride || source
  const received = []
  const wire = JSON.parse(fs.readFileSync(new URL('./m3b-wire.json', import.meta.url))).data
  const initial = { id: '1', source: activeSource, templateCode: 'synthetic', templateName: '合成采购单', draftRevision: 2, designStatus: 'PUBLISHED', publishedVersionId: '1', status: 1, schemaJson: wire.schemaJson }
  const rows = new Map([['1', initial]])
  const versions = [{ id: '1', templateId: '1', versionNo: 1, schemaJson: wire.schemaJson, publishTime: '2026-09-19T06:00:00' }]
  const bindings = []
  let next = 2; let execution = 1; let conflict = false; let delay = false; let unavailable = false
  const fail = (code, message) => { throw Object.assign(new Error(message), { code }) }
  const row = id => rows.get(String(id)) || fail(404, '模板不存在')
  const revision = (item, dto) => { if (item.draftRevision !== dto.expectedRevision || conflict) { conflict = false; fail(409, '修订号冲突，请重新载入后重试；当前修改尚未保存') } }
  const actions = {
    received: () => received,
    list: () => ({ records: [...rows.values()].map(r => ({ ...r, schemaJson: null })), total: rows.size }),
    detail: id => row(id),
    catalog: () => wire.catalog,
    create: dto => { const value = { ...initial, ...dto, id: String(next++), source: { applicationId: dto.applicationId || activeSource.applicationId, sourceType: dto.sourceType || activeSource.sourceType, pageId: dto.pageId || activeSource.pageId, formKey: dto.formKey || null, objectCode: dto.objectCode || activeSource.objectCode }, draftRevision: 1, designStatus: 'DRAFT', publishedVersionId: null }; rows.set(value.id, value); return value },
    copy: (id, dto) => { revision(row(id), dto); return actions.create({ ...dto, schemaJson: row(id).schemaJson }) },
    save: async (id, dto) => { if (delay) { delay = false; await new Promise(resolve => setTimeout(resolve, 2500)) } const item = row(id); revision(item, dto); Object.assign(item, { schemaJson: dto.schemaJson, templateName: dto.templateName, draftRevision: item.draftRevision + 1, designStatus: item.publishedVersionId ? 'CHANGED' : 'DRAFT' }); return item },
    status: (id, dto) => { const item = row(id); revision(item, dto); item.status = dto.status; item.draftRevision++; return item },
    delete: (id, rev) => { const item = row(id); revision(item, { expectedRevision: rev }); if (id === '1' || bindings.some(b => b.templateId === id)) fail(409, '模板存在已发布应用引用或绑定'); rows.delete(String(id)); return null },
    publish: (id, dto) => { const item = row(id); revision(item, dto); let version = versions.find(v => v.templateId === id && v.schemaJson === item.schemaJson); if (!version) { version = { id: String(next++), templateId: id, versionNo: versions.filter(v => v.templateId === id).length + 1, schemaJson: item.schemaJson, publishTime: '2026-09-19T06:00:00' }; versions.push(version) } item.publishedVersionId = version.id; item.designStatus = 'PUBLISHED'; item.draftRevision++; return { template: item, version } },
    versions: id => versions.filter(v => v.templateId === id),
    version: (id, version) => versions.find(v => v.templateId === id && v.id === version) || fail(404, '版本不存在'),
    bindings: dto => bindings.filter(b => b.scene === dto.scene),
    bind: dto => { let item = bindings.find(b => b.id === dto.id); if (item && item.bindingRevision !== dto.expectedRevision) fail(409, '绑定已变更'); if (dto.isDefault) bindings.filter(b => b.scene === dto.scene && b.isDefault && b !== item).forEach(b => { b.isDefault = false; b.bindingRevision++ }); if (!item) { item = { ...dto, id: String(next++), bindingRevision: 0 }; bindings.push(item) } Object.assign(item, dto, { id: item.id, bindingRevision: item.bindingRevision + 1 }); return item },
    unbind: id => { const index = bindings.findIndex(b => b.id === id); if (index >= 0) bindings.splice(index, 1); return null },
    available: () => { if (unavailable) fail(503, '当前表单的打印数据适配器尚未接入'); return row('1').status === 1 ? [{ id: '1', templateName: row('1').templateName, templateVersionId: '1', versionNo: 1, isDefault: true, sortOrder: 0 }] : [] },
    prepare: () => { if (unavailable) fail(503, '当前表单的打印数据适配器尚未接入'); if (row('1').status !== 1) fail(404, '模板已停用'); return { ...wire, executionId: String(execution++) } },
    event: (id, dto) => ({ executionId: id, ...dto }),
    control: mode => { if (mode === 'conflict') conflict = true; if (mode === 'delay') delay = true; if (mode === 'provider') unavailable = !unavailable; return { unavailable } },
  }
  return { name: 'print-synthetic-http', configureServer(server) {
    server.middlewares.use('/__print/', async (req, res) => {
      try {
        const chunks = []; for await (const chunk of req) chunks.push(chunk)
        const args = JSON.parse(Buffer.concat(chunks).toString() || '[]')
        const action = actions[req.url.split('?')[0].replace(/^\//, '')]
        if (!action) fail(404, '未知验证动作')
        if (!req.url.includes('received')) received.push({ action: req.url, args })
        const result = await action(...args)
        res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(result))
      } catch (error) { res.statusCode = error.code || 500; res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify({ message: error.message })) }
    })
  } }
}

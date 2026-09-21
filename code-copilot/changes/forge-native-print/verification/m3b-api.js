// 仅验证服务器通过 Vite alias 注入；生产构建从不导入此文件。
async function call(action, ...args) {
  const response = await fetch('/__print/' + action, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(args) })
  const data = await response.json()
  if (!response.ok) throw Object.assign(new Error(data.message), { code: response.status })
  return { data }
}
export const printTemplates = (...args) => call('list', ...args)
export const printTemplate = (...args) => call('detail', ...args)
export const createPrintTemplate = (...args) => call('create', ...args)
export const updatePrintTemplate = (...args) => call('save', ...args)
export const copyPrintTemplate = (...args) => call('copy', ...args)
export const publishPrintTemplate = (...args) => call('publish', ...args)
export const changePrintTemplateStatus = (...args) => call('status', ...args)
export const deletePrintTemplate = (...args) => call('delete', ...args)
export const printVersions = (...args) => call('versions', ...args)
export const printVersion = (...args) => call('version', ...args)
export const printBindings = (...args) => call('bindings', ...args)
export const savePrintBinding = (...args) => call('bind', ...args)
export const deletePrintBinding = (...args) => call('unbind', ...args)
export const printCatalog = (...args) => call('catalog', ...args)
export const availablePrintTemplates = (...args) => call('available', ...args)
export const preparePrint = (...args) => call('prepare', ...args)
export const recordPrintEvent = (...args) => call('event', ...args)
export async function loadPrintFile() { throw new Error('合成验证未提供附件') }
export const control = (...args) => call('control', ...args)

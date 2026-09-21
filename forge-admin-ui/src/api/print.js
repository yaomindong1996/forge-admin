import { getFileUrl } from '@/utils/file'
import { request } from '@/utils/request'

const encrypted = { encrypt: true, needTip: false }
const params = value => ({ ...encrypted, params: value })
function id(value) {
  if (!/^[1-9]\d*$/.test(String(value)))
    throw new Error('打印资源标识无效')
  return encodeURIComponent(String(value))
}
export const printTemplates = query => request.get('/print/templates/page', params(query))
export const printTemplate = value => request.get(`/print/templates/${id(value)}`, encrypted)
export const createPrintTemplate = dto => request.post('/print/templates', dto, encrypted)
export const updatePrintTemplate = (value, dto) => request.put(`/print/templates/${id(value)}`, dto, encrypted)
export const copyPrintTemplate = (value, dto) => request.post(`/print/templates/${id(value)}/copy`, dto, encrypted)
export const publishPrintTemplate = (value, dto) => request.post(`/print/templates/${id(value)}/publish`, dto, encrypted)
export const changePrintTemplateStatus = (value, dto) => request.put(`/print/templates/${id(value)}/status`, dto, encrypted)
export const deletePrintTemplate = (value, revision) => request.delete(`/print/templates/${id(value)}`, params({ expectedRevision: revision }))
export const printVersions = value => request.get(`/print/templates/${id(value)}/versions`, encrypted)
export const printVersion = (value, version) => request.get(`/print/templates/${id(value)}/versions/${id(version)}`, encrypted)
export const printBindings = query => request.get('/print/bindings', params(query))
export const savePrintBinding = dto => request.put('/print/bindings', dto, encrypted)
export const deletePrintBinding = (value, revision) => request.delete(`/print/bindings/${id(value)}`, params({ expectedRevision: revision }))
export const printCatalog = dto => request.post('/print/catalog', dto, encrypted)
export const availablePrintTemplates = record => request.post('/print/available-templates', { record }, encrypted)
export const preparePrint = (record, templateId) => request.post('/print/prepare', { record, templateId }, encrypted)
export const recordPrintEvent = (value, dto) => request.post(`/print/executions/${id(value)}/events`, dto, encrypted)

export function loadPrintFile(fileId, { signal } = {}) {
  if (!/^[\w-]{1,128}$/.test(String(fileId)))
    throw new Error('打印图片标识无效')
  // Canvas preview must not use the global "download" loading gate — the URL path
  // contains /download/ and would otherwise flash/re-enter loading on every select.
  return request.get(getFileUrl(String(fileId)), {
    baseURL: '',
    responseType: 'blob',
    encrypt: false,
    needTip: false,
    signal,
    timeout: 10000,
    skipGlobalLoading: true,
  })
}

import { request } from '@/utils/request'

export function listPluginTasks(params) {
  return request.get('/system/plugin-task/page', { params, needTip: false })
}

export function getPluginTask(id) {
  return request.get(`/system/plugin-task/${encodeURIComponent(id)}`, { needTip: false })
}

export function uploadPluginPackage(file, requestId) {
  const form = new FormData()
  form.append('file', file)
  form.append('requestId', requestId)
  // multipart 不可交给 JSON 加密器 stringify；使用 HTTPS，响应仍走统一解密链路。
  return request.post('/system/plugin-task/upload', form, { needTip: false, encrypt: false })
}

export function confirmPluginTask(task) {
  return request.post(`/system/plugin-task/${encodeURIComponent(task.id)}/confirm`, {
    revision: task.revision,
    sha256: task.sha256,
  }, { needTip: false })
}

export function cancelPluginTask(task) {
  return request.post(`/system/plugin-task/${encodeURIComponent(task.id)}/cancel`, {
    revision: task.revision,
    sha256: task.sha256,
  }, { needTip: false })
}

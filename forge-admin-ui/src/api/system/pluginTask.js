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

export function reviewPluginTask(command) {
  // 只发送白名单字段；不接收调用者身份、部署地址或机器命令。
  return request.post(`/system/plugin-task/${encodeURIComponent(command.taskId)}/review`, {
    requestId: command.requestId,
    revision: command.revision,
    sha256: command.sha256,
    resultSha256: command.resultSha256,
    decision: command.decision,
    executorStopped: command.executorStopped,
    notDeployed: command.notDeployed,
    artifactsReviewed: command.artifactsReviewed,
    migrationsReviewed: command.migrationsReviewed,
    note: command.note,
  }, { needTip: false })
}

export function registerPluginArtifact(command) {
  return request.post(`/system/plugin-task/${encodeURIComponent(command.taskId)}/artifact`, {
    requestId: command.requestId,
    metadataJson: command.metadataJson,
    localVerified: command.localVerified,
    notDeployed: command.notDeployed,
    note: command.note,
  }, { needTip: false })
}

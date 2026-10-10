import { request } from '@/utils/request'

export const listDeliveryTargets = () => request.get('/system/plugin-delivery/targets', { needTip: false })
export const listDeliveryCandidates = () => request.get('/system/plugin-delivery/candidates', { needTip: false })
export const listDeliveries = () => request.get('/system/plugin-delivery/list', { needTip: false })
export function createDelivery(command) {
  const {
    requestId,
    taskId,
    targetId,
    action,
    releaseId,
    note,
    backupReference,
    migrationsReviewed,
    backwardCompatible,
  } = command
  return request.post('/system/plugin-delivery/add', {
    requestId,
    taskId,
    targetId,
    action,
    releaseId,
    note,
    backupReference: backupReference || null,
    migrationsReviewed,
    backwardCompatible,
  }, { needTip: false })
}
export function reconcileDelivery(id, command) {
  return request.post(`/system/plugin-delivery/${encodeURIComponent(id)}/reconcile`, {
    executorStopped: command.executorStopped,
    note: command.note,
  }, { needTip: false })
}

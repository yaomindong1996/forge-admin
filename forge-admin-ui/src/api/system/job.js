import { postEncrypt, request } from '@/utils'

export function getJobConfig(id) {
  return request.get(`/job/config/${id}`)
}

export function createJobConfig(data) {
  return request.post('/job/config', data)
}

export function updateJobConfig(data) {
  return request.put('/job/config', data)
}

export function getJobExecutors() {
  return request.get('/job/config/executors')
}

export function getJobTimezones() {
  return request.get('/job/config/timezones')
}

export function getPublishedJobFlowModels(params = {}) {
  return request.get('/api/flow/model/list', { params })
}

export function getJobFlowModelVersions(modelKey) {
  return request.get(`/api/flow/model/${encodeURIComponent(modelKey)}/versions`)
}

export function getJobOverview(id) {
  return request.get(`/job/config/${id}/overview`)
}

export function getJobMonitorSummary() {
  return request.get('/job/monitor/summary')
}

export function getJobLogDetail(id) {
  return request.get(`/job/log/${id}`)
}

export function exportJobLogs(data) {
  return request({
    method: 'post',
    url: '/job/log/export',
    data,
    responseType: 'blob',
    rawResponse: true,
    encrypt: false,
  })
}

export function previewJobCron(cronExpression, timezone) {
  return request.post('/job/config/cron/preview', { cronExpression, timezone })
}

export function getJobApiTokenPage(params) {
  return request.get('/job/api-token/page', { params, encrypt: true })
}

export function getJobApiTokenResources() {
  return request.get('/job/api-token/resources', { encrypt: true })
}

export function createJobApiToken(data) {
  return postEncrypt('/job/api-token', data)
}

export function revokeJobApiToken(id) {
  return postEncrypt(`/job/api-token/${id}/revoke`)
}

export function rotateJobApiToken(id) {
  return postEncrypt(`/job/api-token/${id}/rotate`)
}

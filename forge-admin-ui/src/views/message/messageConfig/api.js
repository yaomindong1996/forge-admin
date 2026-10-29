import { request } from '@/utils/index.js'

export default {
  getSmsConfig: () => request.get('/system/message-config/sms'),

  listSmsConfigs: () => request.get('/system/message-config/sms/list'),

  getSmsConfigById: id => request.get(`/system/message-config/sms/${id}`),

  saveSmsConfig: data => request.post('/system/message-config/sms/save', data),

  deleteSmsConfig: id => request.delete(`/system/message-config/sms/${id}`),

  updateSmsStatus: (id, status) => request.post('/system/message-config/sms/updateStatus', null, {
    params: { id, status },
  }),

  setSmsPrimary: id => request.post('/system/message-config/sms/set-primary', null, {
    params: { id },
  }),

  getSmsRoutePolicy: () => request.get('/system/message-config/sms/route-policy'),

  saveSmsRoutePolicy: data => request.put('/system/message-config/sms/route-policy', data),

  pingSmsNow: () => request.post('/system/message-config/sms/ping/test'),

  listSmsPingLogs: (limit = 50) => request.get('/system/message-config/sms/ping/logs', {
    params: { limit },
  }),

  getEmailConfig: () => request.get('/system/message-config/email'),

  saveEmailConfig: data => request.post('/system/message-config/email/save', data),

  updateEmailStatus: (id, status) => request.post('/system/message-config/email/updateStatus', null, {
    params: { id, status },
  }),

  getSmsSuppliers: () => request.get('/system/message-config/sms/suppliers'),
}

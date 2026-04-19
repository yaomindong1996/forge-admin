import { request } from '@/utils/index.js'

export default {
  getSmsConfig: () => request.get('/system/message-config/sms'),

  saveSmsConfig: data => request.post('/system/message-config/sms/save', data),

  updateSmsStatus: (id, status) => request.post('/system/message-config/sms/updateStatus', null, {
    params: { id, status },
  }),

  getEmailConfig: () => request.get('/system/message-config/email'),

  saveEmailConfig: data => request.post('/system/message-config/email/save', data),

  updateEmailStatus: (id, status) => request.post('/system/message-config/email/updateStatus', null, {
    params: { id, status },
  }),

  getSmsSuppliers: () => request.get('/system/message-config/sms/suppliers'),
}

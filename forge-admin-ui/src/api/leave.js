import { request } from '@/utils/http'

/**
 * 请假申请 API
 */
export default {
  /**
   * 提交请假申请
   */
  submit(data) {
    return request.post('/leave/submit', data)
  },

  /**
   * 保存草稿
   */
  saveDraft(data) {
    return request.post('/leave/draft', data)
  },

  /**
   * 更新草稿
   */
  updateDraft(data) {
    return request.put('/leave/draft', data)
  },

  /**
   * 获取请假详情
   */
  getDetail(businessKey) {
    return request.get(`/leave/detail/${businessKey}`)
  },

  /**
   * 分页查询我的请假列表
   */
  getPage(params) {
    return request.get('/leave/page', { params })
  },

  /**
   * 撤销申请
   */
  cancel(businessKey) {
    return request.post(`/leave/cancel/${businessKey}`)
  },

  /**
   * 删除草稿
   */
  delete(businessKey) {
    return request.delete(`/leave/${businessKey}`)
  },
}

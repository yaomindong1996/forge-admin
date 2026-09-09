import { request } from '@/utils'

export default {
  getVersionList: (modelId, pageNum = 1, pageSize = 20) =>
    request.get('/api/flow/model/version/list', { params: { modelId, pageNum, pageSize } }),

  getVersionDetail: versionId =>
    request.get(`/api/flow/model/version/${versionId}`),

  compareVersions: data =>
    request.post('/api/flow/model/version/compare', data),

  revertVersion: data =>
    request.post('/api/flow/model/version/revert', data),

  updateVersionTag: (versionId, versionTag) =>
    request.put(`/api/flow/model/version/${versionId}/tag`, { versionTag }),

  deleteVersion: versionId =>
    request.delete(`/api/flow/model/version/${versionId}`),

  cleanupVersions: (modelId, retainLatest = 10) =>
    request.post('/api/flow/model/version/cleanup', { modelId, retainLatest }),

  downloadVersion: versionId =>
    request.get(`/api/flow/model/version/download/${versionId}`, { responseType: 'blob' }),
}

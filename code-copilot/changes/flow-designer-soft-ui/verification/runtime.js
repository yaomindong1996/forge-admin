// 仅供本地组件验收。禁止真实网络请求，也不替代生产端权限和真实数据。
export const useAuthStore = () => ({ accessToken: '' })
export const useUserStore = () => ({ userId: 'local-fixture', tenantId: 'local-fixture' })
export const isNullOrUndef = value => value === null || value === undefined
const response = async () => ({ code: 200, data: [], message: '本地夹具' })
export const request = Object.assign(response, { get: response, post: response, put: response, delete: response })
export const postEncrypt = response
export const getEncrypt = response
export default request

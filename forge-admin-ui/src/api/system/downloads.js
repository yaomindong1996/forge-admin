import { request } from '@/utils/http'

/** 身份与租户范围由服务端决定，禁止在此拼接用户范围。 */
export async function getRecentDownloads(pageNum = 1) {
  const response = await request.get('/system/file/download-log/page', {
    params: { pageNum, pageSize: 20 },
    needTip: false,
    skipGlobalLoading: true,
  })
  if (Number(response?.code) !== 200 || !Array.isArray(response?.data?.records))
    throw new Error('下载记录加载失败，请重试')
  return response.data
}

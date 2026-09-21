import { request } from '@/utils/http'

/** Upload a print cell image and return fileId. */
export async function uploadPrintCellImage(file, businessType = 'print') {
  if (!(file instanceof Blob))
    throw new Error('无效的图片文件')
  const formData = new FormData()
  const name = file.name || `print-cell-${Date.now()}.png`
  formData.append('file', file, name)
  formData.append('businessType', businessType)
  formData.append('isPrivate', 'true')
  const res = await request({
    method: 'post',
    url: '/api/file/upload',
    data: formData,
    encrypt: false,
    timeout: 30000,
  })
  const fileId = res?.data?.fileId
  if (!fileId)
    throw new Error('图片上传失败')
  return String(fileId)
}

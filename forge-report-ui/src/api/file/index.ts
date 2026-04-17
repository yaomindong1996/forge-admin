import { post } from '@/api/http'

export interface FileUploadResult {
  fileId: string
  fileName: string
  fileSize: number
  contentType: string
  businessType: string
  businessId?: string
  accessUrl: string
  storageType: string
  createTime?: string
}

export const uploadFileApi = async (file: File, businessType = 'project_screenshot', businessId?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', businessType)
  if (businessId) {
    formData.append('businessId', businessId)
  }
  return post('/goview-api/api/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }) as unknown as Promise<{ code: number; data: FileUploadResult; msg: string }>
}

export const getFileUrlApi = (fileId: string, expires = 3600) => {
  return `/goview-api/api/file/url/${fileId}?expires=${expires}`
}

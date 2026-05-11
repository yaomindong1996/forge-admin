import { get } from '@/api/http'

export interface ExternalApiVO {
  id: number
  systemId: number
  systemName?: string
  systemCode?: string
  apiName: string
  apiCode?: string
  apiPath: string
  apiMethod: string
  apiStatus: number
  requestContentType?: string
  responseDataPath?: string
  responseTotalPath?: string
}

export const getExternalApiListApi = () => {
  return get('/forge-report-api/external/api/list') as unknown as Promise<{
    code: number
    data: ExternalApiVO[]
    message: string
  }>
}

import { PackagesType, ConfigType } from '@/packages/index.d'

export { ConfigType }

export { PackagesType }
export interface PackagesStoreType {
  packagesList: PackagesType,
  newPhoto?: ConfigType,
  materialUploadVisible: boolean,
  materialUploadCategory: string,
  materialPhotosLoading: boolean,
  materialPhotosVersion: number
}

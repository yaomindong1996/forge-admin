import { PackagesType, ConfigType } from '@/packages'

export { ConfigType }

export { PackagesType }
export interface PackagesStoreType {
  packagesList: PackagesType,
  newPhoto?: ConfigType
}

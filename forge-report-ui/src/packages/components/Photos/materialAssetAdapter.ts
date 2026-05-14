import { ChartFrameEnum, ConfigType, PackagesCategoryEnum } from '@/packages/index.d'
import { ImageConfig } from '@/packages/components/Informations/Mores/Image/index'
import { ChatCategoryEnum, ChatCategoryEnumName } from './index.d'
import { createFileAssetRef } from '@/utils'
import type { MaterialAsset } from '@/api/file'
import { renameMaterialAssetApi } from '@/api/file'
import { usePackagesStore } from '@/store/modules/packagesStore/packagesStore'

const businessCategoryMap: Record<string, { category: ChatCategoryEnum; categoryName: ChatCategoryEnumName }> = {
  background: { category: ChatCategoryEnum.MATERIAL_BG, categoryName: ChatCategoryEnumName.MATERIAL_BG },
  panel: { category: ChatCategoryEnum.MATERIAL_PANEL, categoryName: ChatCategoryEnumName.MATERIAL_PANEL },
  icon: { category: ChatCategoryEnum.MATERIAL_ICON, categoryName: ChatCategoryEnumName.MATERIAL_ICON },
  illustration: { category: ChatCategoryEnum.MATERIAL_ILLUSTRATION, categoryName: ChatCategoryEnumName.MATERIAL_ILLUSTRATION }
}

const defaultCategory = businessCategoryMap.background

export const buildMaterialUploadEntry = (): ConfigType => ({
  ...ImageConfig,
  category: ChatCategoryEnum.MATERIAL_BG,
  categoryName: ChatCategoryEnumName.MATERIAL_BG,
  package: PackagesCategoryEnum.PHOTOS,
  chartFrame: ChartFrameEnum.STATIC,
  title: '上传图片',
  image: 'upload.png',
  redirectComponent: `${ImageConfig.package}/${ImageConfig.category}/${ImageConfig.key}`,
  disabled: true,
  configEvents: {
    addHandle: () => {
      const packagesStore = usePackagesStore()
      packagesStore.openMaterialUploadDialog('background')
    }
  }
})

export const mapMaterialAssetToPhotoConfig = (asset: MaterialAsset): ConfigType => {
  const fileRef = createFileAssetRef(asset.fileId)
  const catInfo = businessCategoryMap[asset.businessId || ''] || defaultCategory
  return {
    ...ImageConfig,
    category: catInfo.category,
    categoryName: catInfo.categoryName,
    package: PackagesCategoryEnum.PHOTOS,
    chartFrame: ChartFrameEnum.STATIC,
    title: asset.originalName,
    image: fileRef,
    dataset: fileRef,
    redirectComponent: `${ImageConfig.package}/${ImageConfig.category}/${ImageConfig.key}`,
    configEvents: {
      renameHandle: async (newName: string) => {
        await renameMaterialAssetApi(asset.fileId, newName)
        return newName
      }
    }
  }
}

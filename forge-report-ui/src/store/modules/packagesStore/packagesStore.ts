import { defineStore } from 'pinia'
import { ConfigType, PackagesStoreType, PackagesType } from './packagesStore.d'
import { packagesList } from '@/packages/index'
import { getMaterialAssetPageApi } from '@/api/file'
import type { MaterialAsset } from '@/api/file'
import { buildMaterialUploadEntry, mapMaterialAssetToPhotoConfig } from '@/packages/components/Photos/materialAssetAdapter'
import SharePhotos from '@/packages/components/Photos/Share'

// 组件 packages
export const usePackagesStore = defineStore({
  id: 'usePackagesStore',
  state: (): PackagesStoreType => ({
    packagesList: Object.freeze(packagesList),
    newPhoto: undefined,
    materialUploadVisible: false,
    materialUploadCategory: 'background',
    materialPhotosLoading: false,
    materialPhotosVersion: 0
  }),
  getters: {
    getPackagesList(): PackagesType {
      return this.packagesList
    }
  },
  actions: {
    setPhotos(nextPhotos: ConfigType[]) {
      this.packagesList.Photos.splice(0, this.packagesList.Photos.length, ...nextPhotos)
      this.materialPhotosVersion += 1
    },
    async loadMaterialPhotos() {
      this.materialPhotosLoading = true
      try {
        const response = await getMaterialAssetPageApi({
          pageNum: 1,
          pageSize: 200
        })
        const records = (response.data?.records || []) as MaterialAsset[]
        const nextPhotos = [
          buildMaterialUploadEntry(),
          ...records.map(mapMaterialAssetToPhotoConfig),
          ...SharePhotos
        ]
        this.setPhotos(nextPhotos)
      } finally {
        this.materialPhotosLoading = false
      }
    },
    addPhotos(newPhoto: ConfigType, index: number) {
      this.newPhoto = newPhoto
      this.packagesList.Photos.splice(index, 0, newPhoto)
      this.materialPhotosVersion += 1
    },
    openMaterialUploadDialog(category = 'background') {
      this.materialUploadCategory = category
      this.materialUploadVisible = true
    },
    closeMaterialUploadDialog() {
      this.materialUploadVisible = false
    }
  }
})

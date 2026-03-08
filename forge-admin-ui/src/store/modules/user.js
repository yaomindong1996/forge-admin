import { defineStore } from 'pinia'
const getStaffInfo = (user) => {
  let staffInfo = {}
  if(user.oaStaffInfo&&Object.keys(user.oaStaffInfo).length!==0 && user.oaStaffInfo.staffNo){
    staffInfo = user.oaStaffInfo
    staffInfo.eparchyCode = getTransitionEparchyCode(user.oaStaffInfo.eparchy)
    staffInfo.staffId = user.oaStaffInfo.staffNoOld
    staffInfo.roleId = user.staffInfo&&user.staffInfo.roleId || ""
    staffInfo.departId = user.oaStaffInfo.departCode
    staffInfo.cityCode = user.oaStaffInfo.cityCode || user.staffInfo&&user.staffInfo.cityCode || user.userInfo&&user.userInfo.cityCode
  }else{
    if(user.uacStaffInfo&&Object.keys(user.uacStaffInfo).length!==0 && user.uacStaffInfo.staffNo){
      staffInfo = user.uacStaffInfo
      staffInfo.eparchyCode = user.uacStaffInfo.eparchyCode || user.staffInfo&&user.staffInfo.eparchyCode || user.userInfo&&user.userInfo.eparchyCode
      staffInfo.roleId = user.uacStaffInfo.roleId || user.staffInfo&&user.staffInfo.roleId || ""
      staffInfo.staffId = user.uacStaffInfo.staffNo || user.staffInfo&&user.staffInfo.staffId
    }else{
      staffInfo = user.staffInfo || user
    }
  }
  return staffInfo
}
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    staffInfo: null,
    dataPermission: null
  }),
  getters: {
    userId() {
      return this.userInfo?.userId || this.userInfo?.id
    },
    username() {
      return this.userInfo?.username
    },
    realName() {
      return this.userInfo?.realName || this.userInfo?.nickName
    },
    avatar() {
      return this.userInfo?.avatar || this.staffInfo?.avatar
    },
    email() {
      return this.userInfo?.email
    },
    phone() {
      return this.userInfo?.phone
    },
    userType() {
      return this.userInfo?.userType
    },
    isAdmin() {
      return this.userInfo?.admin || this.userInfo?.isAdmin || false
    },
    isTenantAdmin() {
      return this.userInfo?.tenantAdmin || this.userInfo?.isTenantAdmin || false
    },
    // 兼容旧的 staffInfo 相关 getters
    staffId() {
      return this.staffInfo?.staffId
    },
    staffName() {
      return this.staffInfo?.staffName
    },
    eparchyCode() {
      return this.staffInfo?.eparchyCode
    },
    serialNumber() {
      return this.staffInfo?.serialNumber
    },
    getStaffInfo() {
      return this.staffInfo
    },
    getDataPermission() {
      return this.dataPermission
    },
    // 角色和权限相关 getters
    roles() {
      return this.userInfo?.roles || this.userInfo?.roleKeys || []
    },
    roleIds() {
      return this.userInfo?.roleIds || []
    },
    permissions() {
      return this.userInfo?.permissions || []
    },
    apiPermissions() {
      return this.userInfo?.apiPermissions || []
    }
  },
  actions: {
    setUser(user) {
      // 新的用户信息结构：直接保存完整的 LoginUser 对象
      if (user.userInfo) {
        this.userInfo = user.userInfo
      } else if (user.userId || user.id) {
        // 如果传入的就是用户信息对象本身
        this.userInfo = user
      }
      
      // 兼容旧的 staffInfo 结构
      if (user.staffInfo) {
        this.staffInfo = getStaffInfo(user.staffInfo)
      }
      
      // 兼容旧的 dataPermission 结构
      if (user.dataPermission) {
        this.dataPermission = user.dataPermission
      }
    },
    resetUser() {
      this.$reset()
    },
  },
  persist: {
    key: `${import.meta.env.VITE_TENANT || 'default'}_user`,
  },
})

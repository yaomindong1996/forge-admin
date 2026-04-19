import { cloneDeep } from 'lodash-es'
import api from '@/api'
import { basePermissions } from '@/settings'

export async function getUserInfo() {
  const res = await api.getUser()

  // 新的后端接口返回结构: { code: 200, data: LoginUser }
  if (res.code === 200 && res.data) {
    const loginUser = res.data

    return {
      id: loginUser.userId,
      username: loginUser.username,
      avatar: loginUser.avatar,
      nickName: loginUser.realName || loginUser.username,
      email: loginUser.email,
      phone: loginUser.phone,
      userType: loginUser.userType,
      userStatus: loginUser.userStatus,
      tenantId: loginUser.tenantId,
      roleIds: loginUser.roleIds || [],
      roleKeys: loginUser.roleKeys || [],
      permissions: loginUser.permissions || [],
      apiPermissions: loginUser.apiPermissions || [],
      orgIds: loginUser.orgIds || [],
      mainOrgId: loginUser.mainOrgId,
      loginTime: loginUser.loginTime,
      loginIp: loginUser.loginIp,
      isAdmin: loginUser.admin,
      isTenantAdmin: loginUser.tenantAdmin,
      // 保留原有的字段结构以兼容现有代码
      roles: loginUser.roleKeys ? Array.from(loginUser.roleKeys) : [],
      userInfo: loginUser, // 保存完整的用户信息
    }
  }

  // 兼容旧的响应结构
  const userData = res.data || {}
  const userInfo = userData.userInfo || {}

  return {
    id: userInfo.id,
    username: userInfo.id,
    avatar: null,
    nickName: userInfo.nickName || userInfo.name,
    email: userInfo.email,
    roles: [],
    userInfo,
  }
}

export async function getPermissions() {
  // 不再调用后端接口获取权限，直接返回基础权限
  return cloneDeep(basePermissions)
}

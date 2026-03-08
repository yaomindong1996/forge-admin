/**
 * v-hasPermi 操作权限处理
 * 适配当前项目结构的权限指令
 */
import { useUserStore } from '@/store'

export default {
  mounted(el, binding) {
    const { value } = binding
    
    if (value && value instanceof Array && value.length > 0) {
      const permissionFlag = value
      const userStore = useUserStore()
      const all_permission = "**"
      
      // 从用户store中获取权限数据
      const permissions = userStore.getDataPermission || []
      
      // 如果没有权限数据，暂时不删除元素，避免页面初始化时误删
      if (!permissions || permissions.length === 0) {
        console.warn('权限数据未加载，跳过权限检查')
        return
      }
      
      const hasPermissions = permissions.some(permission => {
        return all_permission === permission || permissionFlag.includes(permission)
      })

      if (!hasPermissions) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请设置操作权限标签值`)
    }
  },
  
  updated(el, binding) {
    // 更新时也执行权限检查
    this.mounted(el, binding)
  }
}
// 纯展示格式化，不触发首页数据请求或任务状态变更。
export function formatHomeTaskInitiator(task) {
  return [task.startUserName || '-', task.startDeptName].filter(Boolean).join(' / ')
}

// 保留原首页相对时间规则，避免视觉拆分改变业务展示口径。
export function formatHomeTaskTime(time) {
  if (!time) {
    return '-'
  }
  const date = new Date(time)
  const diff = new Date() - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) {
    return '刚刚'
  }
  if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  }
  if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  }
  if (diff < 7 * day) {
    return `${Math.floor(diff / day)}天前`
  }
  return String(time).split(' ')[0]
}

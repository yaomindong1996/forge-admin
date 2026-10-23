/** 预取门户系统页 chunk，降低首次进入白屏等待 */
export function prefetchPageManagementSystemViews() {
  return Promise.allSettled([
    import('@/views/workspace/summary.vue'),
    import('@/views/workspace/todo.vue'),
    import('@/views/workspace/done.vue'),
    import('@/views/workspace/started.vue'),
    import('@/views/workspace/cc.vue'),
    import('@/views/message/message-list.vue'),
  ])
}

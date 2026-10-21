// 使用说明属于插件源码，只从已编译目录加载，不接受服务端任意URL/组件路径。
const panels = import.meta.glob('/src/views/plugins/*/guide/index.vue')

export function getPluginGuideExtension(id) {
  if (typeof id !== 'string' || !/^[a-z0-9][a-z0-9-]{0,63}$/.test(id))
    return null
  return panels[`/src/views/plugins/${id}/guide/index.vue`] || null
}

// 示例只能来自已编译插件，不接受后端组件URL；卸载插件后宿主仍可构建。
const entries = import.meta.glob('/src/views/plugins/*/examples/entry.js', { eager: true, import: 'default' })
const panels = import.meta.glob('/src/views/plugins/*/examples/index.vue')

export function getPluginExamplesExtension(id) {
  if (typeof id !== 'string' || !/^[a-z0-9][a-z0-9-]{0,63}$/.test(id))
    return null
  const base = `/src/views/plugins/${id}/examples/`
  const entry = entries[`${base}entry.js`]
  const loader = panels[`${base}index.vue`]
  if (!entry || !loader || typeof entry.permission !== 'string' || !/^[\w:-]+$/.test(entry.permission))
    return null
  return { permission: entry.permission, loader }
}

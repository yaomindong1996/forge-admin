// 扩展只能来自当前前端已编译的可信插件目录，服务端ID不能变成任意import路径。
const metadata = import.meta.glob('/src/views/plugins/*/settings/entry.js', { eager: true, import: 'default' })
const panels = import.meta.glob('/src/views/plugins/*/settings/index.vue')

export function getPluginSettingsExtension(id) {
  if (typeof id !== 'string' || !/^[a-z0-9][a-z0-9-]{0,63}$/.test(id))
    return null
  const base = `/src/views/plugins/${id}/settings/`
  const entry = metadata[`${base}entry.js`]
  const loader = panels[`${base}index.vue`]
  if (!entry || !loader || typeof entry.permission !== 'string' || !/^[\w:-]+$/.test(entry.permission))
    return null
  return { permission: entry.permission, loader }
}

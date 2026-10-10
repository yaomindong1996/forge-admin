/** 只选择服务端已验证归属的当前插件授权，不以 ID 前缀或功能开关推断许可证。 */
export function pluginLicenseView(data, plugin) {
  const allEntries = Array.isArray(data?.report?.entries) ? data.report.entries : []
  const declared = new Set((plugin?.features || []).map(feature => feature.code))
  const entries = allEntries
    .filter(entry => plugin?.id && Array.isArray(entry?.scope?.pluginIds)
      && entry.scope.pluginIds.includes(plugin.id))
    .map(entry => ({
      position: entry.position,
      state: entry.state,
      terms: entry.terms,
      // 一张许可证可覆盖多个插件，界面不把其它插件功能混到当前详情。
      features: (entry.scope.featureCodes || []).filter(code => declared.has(code)),
    }))
  return {
    entries,
    hasUnattributedIssue: allEntries.some(entry => !entry?.scope),
    configurationInvalid: data?.report?.configuration?.filesConfigurationValid === false,
  }
}

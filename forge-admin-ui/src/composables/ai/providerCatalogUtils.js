export function normalizeProviderTemplates(records) {
  return (records || []).filter(item => item?.templateKey).map(item => ({
    ...item,
    providerType: item.providerType || item.templateKey,
    featured: item.featured ?? true,
    providerTypeAliases: item.providerTypeAliases || [],
    configuredCount: item.configuredCount == null ? null : Number(item.configuredCount),
  }))
}

export function matchesProviderTemplate(template, providerType) {
  return template.providerType === providerType || template.providerTypeAliases.includes(providerType)
}

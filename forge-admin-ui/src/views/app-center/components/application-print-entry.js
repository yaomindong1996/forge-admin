const SETTINGS_SECTIONS = new Set([
  'basic',
  'access',
  'navigation',
  'permission',
  'globalization',
  'advanced',
])

export function resolveApplicationSettingsSection(value) {
  const normalized = String(Array.isArray(value) ? value[0] : value || '').trim().toLowerCase()
  return SETTINGS_SECTIONS.has(normalized) ? normalized : 'basic'
}

export function buildApplicationPrintLocation(application, pageId) {
  const applicationCode = String(application?.applicationCode || '').trim()
  const scopedPageId = String(pageId || '').trim()
  if (!applicationCode || !scopedPageId)
    return null
  return {
    name: 'BusinessApplicationRuntime',
    params: { applicationCode },
    query: {
      edit: '1',
      pageId: scopedPageId,
      designTab: 'settings',
      settingsSection: 'printing',
    },
  }
}

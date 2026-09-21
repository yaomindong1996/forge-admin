const SETTINGS_SECTIONS = new Set([
  'basic',
  'access',
  'navigation',
  'permission',
  'globalization',
  'printing',
  'advanced',
])

export function resolveApplicationSettingsSection(value) {
  const normalized = String(Array.isArray(value) ? value[0] : value || '').trim().toLowerCase()
  return SETTINGS_SECTIONS.has(normalized) ? normalized : 'basic'
}

export function buildApplicationPrintLocation(application) {
  const applicationCode = String(application?.applicationCode || '').trim()
  if (!applicationCode)
    return null
  return {
    name: 'BusinessApplicationRuntime',
    params: { applicationCode },
    query: {
      view: 'settings',
      settingsSection: 'printing',
    },
  }
}

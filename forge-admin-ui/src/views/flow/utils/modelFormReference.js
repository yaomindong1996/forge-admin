const textValue = value => String(value ?? '').trim()

export function parseBusinessFormReference(formJson) {
  if (!formJson)
    return {}
  if (typeof formJson === 'object' && !Array.isArray(formJson)) {
    const nested = formJson.formRef && typeof formJson.formRef === 'object' ? formJson.formRef : {}
    return normalizeBusinessFormReference(nested, formJson)
  }
  try {
    const parsed = JSON.parse(formJson)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    const nested = parsed.formRef && typeof parsed.formRef === 'object' ? parsed.formRef : {}
    return normalizeBusinessFormReference(nested, parsed)
  }
  catch {
    return {}
  }
}

export function normalizeBusinessFormReference(nested = {}, root = {}) {
  const firstText = (...values) => values.map(textValue).find(Boolean) || ''
  return {
    ...nested,
    ...root,
    objectCode: firstText(nested.objectCode, root.objectCode),
    objectName: firstText(nested.objectName, root.objectName),
    applicationId: firstText(nested.applicationId, root.applicationId),
    applicationName: firstText(nested.applicationName, root.applicationName),
    formKey: firstText(nested.formKey, root.formKey),
    formName: firstText(nested.formName, root.formName),
    providerKey: firstText(nested.providerKey, root.providerKey),
    formMode: firstText(nested.formMode, nested.type, root.formMode, root.type),
    type: firstText(nested.type, nested.formMode, root.type, root.formMode),
  }
}

export function resolveApplicationIdFromFormKey(formKey) {
  const match = textValue(formKey).match(/^app_([^_]+)_page_/i)
  return match ? textValue(match[1]) : ''
}

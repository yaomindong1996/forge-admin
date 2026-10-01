export const DEFAULT_PRINT_SCENES = ['LIST', 'DETAIL']

export const FALLBACK_PRINT_SCENE_OPTIONS = [
  { label: '列表', value: 'LIST' },
  { label: '详情', value: 'DETAIL' },
  { label: '流程待办', value: 'FLOW_TODO' },
  { label: '流程已办', value: 'FLOW_DONE' },
  { label: '流程我发起', value: 'FLOW_STARTED' },
]

export function normalizePrintScenes(scenes) {
  return [...new Set((Array.isArray(scenes) ? scenes : [])
    .map(item => String(item || '').trim())
    .filter(Boolean))]
}

export function scenesOfTemplate(bindings, templateId) {
  return normalizePrintScenes(
    (Array.isArray(bindings) ? bindings : [])
      .filter(item => String(item.templateId) === String(templateId) && Number(item.status) === 1)
      .map(item => item.scene),
  )
}

function sameScenes(left, right) {
  const a = normalizePrintScenes(left)
  const b = normalizePrintScenes(right)
  return a.length === b.length && a.every(scene => b.includes(scene))
}

function unwrapSavedBinding(result) {
  if (!result || typeof result !== 'object')
    return null
  return result.data && typeof result.data === 'object' ? result.data : result
}

function sceneCode(value) {
  if (value == null)
    return ''
  if (typeof value === 'object' && value.code != null)
    return String(value.code)
  return String(value)
}

/**
 * Sync enabled scenes for a template. Returns the next bindings array (local patch),
 * so callers can skip a full list/bindings reload.
 */
export async function syncPrintTemplateScenes({
  source,
  templateId,
  templateVersionId,
  scenes,
  bindings = [],
  save,
  remove,
}) {
  const wanted = normalizePrintScenes(scenes)
  let next = (Array.isArray(bindings) ? bindings : []).map(item => ({ ...item }))
  const current = next.filter(item => String(item.templateId) === String(templateId))
  const enabled = current.filter(item => Number(item.status) === 1)
  const versionChanged = templateVersionId != null && enabled.some(item =>
    String(item.templateVersionId || '') !== String(templateVersionId))
  if (sameScenes(enabled.map(item => item.scene), wanted) && !versionChanged)
    return next

  for (const scene of wanted) {
    const existing = current.find(item => sceneCode(item.scene) === scene)
    const existingUsesVersion = templateVersionId == null
      || String(existing?.templateVersionId || '') === String(templateVersionId)
    if (existing && Number(existing.status) === 1 && existingUsesVersion)
      continue
    const saved = unwrapSavedBinding(await save({
      source,
      templateId,
      templateVersionId,
      scene,
      id: existing?.id,
      expectedRevision: existing?.bindingRevision,
      isDefault: true,
      status: 1,
      sortOrder: existing?.sortOrder ?? 0,
    }))
    if (saved?.id != null) {
      const patched = {
        ...existing,
        ...saved,
        templateId: saved.templateId ?? templateId,
        scene: sceneCode(saved.scene) || scene,
        status: saved.status ?? 1,
        templateVersionId: saved.templateVersionId ?? templateVersionId ?? existing?.templateVersionId,
        bindingRevision: saved.bindingRevision ?? existing?.bindingRevision,
      }
      const idx = next.findIndex(item =>
        String(item.id) === String(saved.id)
        || (String(item.templateId) === String(templateId) && sceneCode(item.scene) === scene))
      if (idx >= 0)
        next[idx] = { ...next[idx], ...patched }
      else
        next.push(patched)
    }
    else if (existing) {
      const idx = next.findIndex(item => String(item.id) === String(existing.id))
      if (idx >= 0)
        next[idx] = { ...next[idx], status: 1 }
    }
  }

  for (const binding of enabled) {
    if (!wanted.includes(sceneCode(binding.scene))) {
      await remove(binding.id, binding.bindingRevision)
      next = next.filter(item => String(item.id) !== String(binding.id))
    }
  }
  return next
}

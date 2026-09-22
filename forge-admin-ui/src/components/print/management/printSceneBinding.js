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

export async function syncPrintTemplateScenes({ source, templateId, scenes, bindings = [], save, remove }) {
  const wanted = normalizePrintScenes(scenes)
  const current = (Array.isArray(bindings) ? bindings : [])
    .filter(item => String(item.templateId) === String(templateId))
  const enabled = current.filter(item => Number(item.status) === 1)
  if (sameScenes(enabled.map(item => item.scene), wanted))
    return
  for (const scene of wanted) {
    const existing = current.find(item => item.scene === scene)
    if (existing && Number(existing.status) === 1)
      continue
    await save({
      source,
      templateId,
      scene,
      id: existing?.id,
      expectedRevision: existing?.bindingRevision,
      isDefault: true,
      status: 1,
      sortOrder: existing?.sortOrder ?? 0,
    })
  }
  for (const binding of enabled) {
    if (!wanted.includes(binding.scene))
      await remove(binding.id, binding.bindingRevision)
  }
}

import { ref } from 'vue'

export function useLowcodeFormRegistry({ isComposedPage, hasPageSections, notify }) {
  const mainFormRef = ref(null)
  const mainSectionFormRefs = new Map()
  const runtimeFormRefs = new Map()
  const childFormRefs = new Map()
  const childRowScopes = new WeakMap()
  let childRowScopeSequence = 0

  function setChildFormRef(child, row, rowIndex, instance) {
    if (!child) return
    const key = `${child.modelCode}:${row?.id || childRowScope(row, rowIndex)}`
    if (instance) childFormRefs.set(key, instance)
    else childFormRefs.delete(key)
  }

  function setRuntimeFormRef(zone, instance) {
    const key = String(zone?.zoneId || 'form')
    if (instance) runtimeFormRefs.set(key, instance)
    else runtimeFormRefs.delete(key)
  }

  function setMainSectionFormRef(zone, { sectionId, instance }) {
    const key = `${zone?.zoneId || 'legacy'}:${sectionId}`
    if (instance) mainSectionFormRefs.set(key, instance)
    else mainSectionFormRefs.delete(key)
  }

  function childRowScope(row, fallback = 0) {
    if (!row || typeof row !== 'object') return String(fallback)
    if (!childRowScopes.has(row)) childRowScopes.set(row, `new_${++childRowScopeSequence}`)
    return childRowScopes.get(row)
  }

  function validateForms() {
    const mainForms = isComposedPage()
      ? [...runtimeFormRefs.values(), ...mainSectionFormRefs.values()]
      : hasPageSections() ? [...mainSectionFormRefs.values()] : [mainFormRef.value]
    const mainValid = mainForms.length > 0 && mainForms.every(form => form?.validate?.() !== false)
    const childrenValid = [...childFormRefs.values()].every(form => form?.validate?.() !== false)
    if (!mainValid || !childrenValid) {
      notify('请完善必填字段', { type: 'warning' })
      return false
    }
    return true
  }

  function clearChildForms() { childFormRefs.clear() }
  function dispose() {
    mainSectionFormRefs.clear()
    runtimeFormRefs.clear()
    childFormRefs.clear()
  }

  return {
    mainFormRef,
    childRowScope,
    setChildFormRef,
    setRuntimeFormRef,
    setMainSectionFormRef,
    validateForms,
    clearChildForms,
    dispose,
  }
}

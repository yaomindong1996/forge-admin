import { extractChildData, extractMainData } from '@/utils/business-task-form-adapter'
import { buildDefaultData, ensureChildRows } from '@/utils/lowcode-runtime'

export function useBusinessTaskFormState({ mainData, childData, formInfo, seedApprovalPointChecks, getMode }) {
  function replaceMainData(source = {}) {
    clear(mainData)
    if (!source || typeof source !== 'object' || Array.isArray(source)) return
    Object.entries(source).forEach(([key, value]) => { mainData[key] = value == null ? '' : value })
  }

  function replaceChildData(source = {}) {
    clear(childData)
    if (!source || typeof source !== 'object' || Array.isArray(source)) return
    Object.entries(source).forEach(([key, value]) => {
      if (Array.isArray(value)) childData[key] = value
    })
  }

  function applyBusinessContext(context = {}) {
    const embedded = context?.taskFormInfo
    if (embedded && typeof embedded === 'object' && !Array.isArray(embedded) && Object.keys(embedded).length) {
      formInfo.value = embedded
      seedApprovalPointChecks(embedded)
    }
    replaceMainData(extractMainData(context?.recordData))
    replaceChildData(extractChildData(context?.recordData))
  }

  function resetBusinessData() {
    replaceMainData({})
    replaceChildData({})
  }

  function addBusinessChildRow(child) {
    if (!child || child.allowCreate !== true || getMode() === 'detail') return
    ensureChildRows(child, childData).push(buildDefaultData(child.fields || []))
  }

  function removeBusinessChildRow({ child, index } = {}) {
    if (!child || getMode() === 'detail') return
    const rows = ensureChildRows(child, childData)
    const row = rows[index]
    if (!row) return
    const id = row.id ?? row.ID
    const persisted = id !== undefined && id !== null && String(id).trim() !== ''
    if (persisted) {
      if (child.allowDelete === true) rows.splice(index, 1, { id: String(id), _deleted: true })
      return
    }
    if (child.allowCreate === true) rows.splice(index, 1)
  }

  return { applyBusinessContext, replaceMainData, resetBusinessData, addBusinessChildRow, removeBusinessChildRow }
}

function clear(target) {
  Object.keys(target).forEach(key => delete target[key])
}

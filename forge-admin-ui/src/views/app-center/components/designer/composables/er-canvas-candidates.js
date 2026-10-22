/**
 * 数据关系 ER 画布候选对象规则。
 * 画布模型数 = 当前对象 + 候选目标（不含当前对象自身）。
 */

/** 对象列表加载后：禁止用套件第一项自动塞画布（历史竞态根因）。 */
export function afterBusinessObjectsLoaded(existingCandidateCodes = []) {
  return Array.from(new Set((existingCandidateCodes || []).filter(Boolean)))
}

/** 关系加载后：把关系目标并入候选。 */
export function afterRelationsLoaded(existingCandidateCodes = [], relationTargetCodes = []) {
  return Array.from(new Set([
    ...(existingCandidateCodes || []).filter(Boolean),
    ...(relationTargetCodes || []).filter(Boolean),
  ]))
}

/** 切换业务对象时清空候选，避免串画布。 */
export function onObjectSwitched() {
  return []
}

/** 画布上展示的对象数量（含当前主对象）。 */
export function erCanvasObjectCount(candidateCodes = []) {
  return 1 + Array.from(new Set((candidateCodes || []).filter(Boolean))).length
}

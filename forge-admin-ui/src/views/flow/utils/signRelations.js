// 动态加签关系：status=1 表示仍有效，已撤回的不能再被减签
export function getSignRelationUserName(relation) {
  return relation?.targetUserName || relation?.targetUserId || '-'
}

export function buildReduceSignOptions(relations = []) {
  return (Array.isArray(relations) ? relations : [])
    .filter(relation => relation?.status === 1 && relation.targetUserId)
    .map(relation => ({ label: getSignRelationUserName(relation), value: String(relation.targetUserId) }))
}

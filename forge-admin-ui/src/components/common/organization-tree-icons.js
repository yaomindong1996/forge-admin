// 组织树使用统一线性语义，不再用厚重的结构图标重复占据每一行。
export function getOrganizationNodeIcon(node = {}) {
  if (!node.parentId || String(node.parentId) === '0') {
    return 'i-lucide:building-2'
  }
  // 懒加载分支尚未取得 children 时，仍按后端 hasChildren 识别组织层级。
  return node.children?.length || node.hasChildren ? 'i-lucide:folder-tree' : 'i-lucide:users'
}

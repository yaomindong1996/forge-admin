/**
 * 节点/边/分支 ID 生成器
 *
 * 设计目标：
 * - 同一会话内 ID 唯一
 * - ID 含可读前缀（Node_ / Flow_ / b），方便 BPMN XML 阅读
 * - 不依赖时间戳或随机数（避免单元测试不稳定）；通过 createIdGenerator() 创建独立计数器
 * - 同时提供全局便捷函数 nextNodeId / nextEdgeId / nextBranchId（用于设计器运行时）
 *
 * 与现有数据兼容：
 * - 加载已有 BPMN 模型时，nodeId / edgeId 沿用 XML 中的 id
 * - 新增节点/边时通过 ensureUnique 跳过已占用的 id
 */

const DEFAULTS = {
  NODE_PREFIX: 'Node_',
  EDGE_PREFIX: 'Flow_',
  BRANCH_PREFIX: 'b',
}

/**
 * 创建独立 ID 生成器（推荐在 useFlowDesigner 内部使用，便于测试）。
 *
 * @param {object} [options]
 * @param {Set<string>} [options.usedIds] 已占用 ID 集合（兼容已有 flowJson）
 * @returns {{ nextNodeId, nextEdgeId, nextBranchId, register, has, snapshot }}
 */
export function createIdGenerator(options = {}) {
  const used = options.usedIds instanceof Set ? new Set(options.usedIds) : new Set()
  const counters = {
    node: 0,
    edge: 0,
    branch: 0,
  }

  function nextWith(prefix, key) {
    counters[key] += 1
    let id = `${prefix}${counters[key]}`
    while (used.has(id)) {
      counters[key] += 1
      id = `${prefix}${counters[key]}`
    }
    used.add(id)
    return id
  }

  return {
    nextNodeId: () => nextWith(DEFAULTS.NODE_PREFIX, 'node'),
    nextEdgeId: () => nextWith(DEFAULTS.EDGE_PREFIX, 'edge'),
    nextBranchId: () => nextWith(DEFAULTS.BRANCH_PREFIX, 'branch'),

    /** 把已有 ID 注册到生成器，确保后续不冲突。 */
    register(id) {
      if (id)
        used.add(id)
    },

    /** 检查 ID 是否已使用。 */
    has(id) {
      return used.has(id)
    },

    /** 调试用：返回当前计数 + 已用 ID 数量。 */
    snapshot() {
      return { ...counters, usedSize: used.size }
    },
  }
}

/**
 * 收集 flowJson 中所有现有 ID（nodes / edges + edges.branchId），用于初始化生成器。
 */
export function collectExistingIds(flowJson) {
  const ids = new Set()
  if (!flowJson)
    return ids
  for (const n of flowJson.nodes || []) {
    if (n.id)
      ids.add(n.id)
  }
  for (const e of flowJson.edges || []) {
    if (e.id)
      ids.add(e.id)
    if (e.branchId)
      ids.add(e.branchId)
  }
  return ids
}

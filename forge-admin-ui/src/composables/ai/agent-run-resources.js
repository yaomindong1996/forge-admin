import { agentGetById, agentSkillPackages, knowledgePage } from '@/api/ai'

/** 解析智能体 knowledgeIds（JSON 数组字符串 / 数组 / 逗号分隔）为 ID 字符串列表。 */
export function parseKnowledgeIdList(raw) {
  if (Array.isArray(raw))
    return raw.map(String).map(s => s.trim()).filter(Boolean)
  if (typeof raw !== 'string' || !raw.trim())
    return []
  const text = raw.trim()
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed))
      return parsed.map(String).map(s => s.trim()).filter(Boolean)
  }
  catch {
    // fall through：兼容历史逗号分隔
  }
  return text.split(',').map(s => s.trim()).filter(Boolean)
}

function skillLabelsFromRows(rows) {
  return (rows || []).map((row) => {
    const name = row.packageName || row.name || row.packageCode || row.code
    const ver = row.versionName || row.version
    return ver ? `${name} v${ver}` : name
  }).filter(Boolean)
}

/**
 * 拉取本轮对话可用的技能资料 / 知识库名称，供 ChatRunTrace 展示。
 * @param {{ agentId?: string|number, knowledgeIds?: unknown }} opts
 * @returns {Promise<{ skills: string[], knowledge: string[] }>}
 */
export async function resolveAgentRunResources(opts = {}) {
  const agentId = opts.agentId
  let knowledgeIds = parseKnowledgeIdList(opts.knowledgeIds)
  let skills = []

  const tasks = []

  if (agentId) {
    tasks.push(
      agentSkillPackages(agentId)
        .then((res) => {
          const rows = res.code === 200 && Array.isArray(res.data) ? res.data : []
          skills = skillLabelsFromRows(rows)
        })
        .catch(() => {
          skills = []
        }),
    )
    // 调用方未带 knowledgeIds 时，按 agentId 补全详情
    if (!knowledgeIds.length && opts.knowledgeIds == null) {
      tasks.push(
        agentGetById(agentId)
          .then((res) => {
            if (res.code === 200 && res.data)
              knowledgeIds = parseKnowledgeIdList(res.data.knowledgeIds)
          })
          .catch(() => {}),
      )
    }
  }

  await Promise.all(tasks)

  let knowledge = []
  if (knowledgeIds.length) {
    try {
      const res = await knowledgePage({ pageNum: 1, pageSize: 200, status: '0' })
      const records = res.data?.records || []
      const idSet = new Set(knowledgeIds.map(String))
      knowledge = records
        .filter(item => idSet.has(String(item.id)))
        .map(item => item.knowledgeName || item.name || item.title)
        .filter(Boolean)
      // 名称映射不全时，用数量兜底，避免裸 ID
      if (!knowledge.length)
        knowledge = [`已绑定 ${knowledgeIds.length} 个`]
    }
    catch {
      knowledge = [`已绑定 ${knowledgeIds.length} 个`]
    }
  }

  return { skills, knowledge }
}

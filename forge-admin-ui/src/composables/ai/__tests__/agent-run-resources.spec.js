import { beforeEach, describe, expect, it, vi } from 'vitest'

const agentGetByIdMock = vi.fn()
const agentSkillPackagesMock = vi.fn()
const knowledgePageMock = vi.fn()

vi.mock('@/api/ai', () => ({
  agentGetById: (...args) => agentGetByIdMock(...args),
  agentSkillPackages: (...args) => agentSkillPackagesMock(...args),
  knowledgePage: (...args) => knowledgePageMock(...args),
}))

const { parseKnowledgeIdList, resolveAgentRunResources } = await import('../agent-run-resources')

describe('parseKnowledgeIdList', () => {
  it('parses json array string and arrays', () => {
    expect(parseKnowledgeIdList('[1,2]')).toEqual(['1', '2'])
    expect(parseKnowledgeIdList([3, '4'])).toEqual(['3', '4'])
    expect(parseKnowledgeIdList('10, 20')).toEqual(['10', '20'])
    expect(parseKnowledgeIdList('')).toEqual([])
  })
})

describe('resolveAgentRunResources', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('resolves skill package labels and knowledge names', async () => {
    agentSkillPackagesMock.mockResolvedValue({
      code: 200,
      data: [{ packageName: '销售手册', versionName: '1.0' }],
    })
    knowledgePageMock.mockResolvedValue({
      code: 200,
      data: {
        records: [
          { id: 1, knowledgeName: '产品知识库' },
          { id: 9, knowledgeName: '其它' },
        ],
      },
    })

    const result = await resolveAgentRunResources({
      agentId: 12,
      knowledgeIds: '[1]',
    })

    expect(result).toEqual({
      skills: ['销售手册 v1.0'],
      knowledge: ['产品知识库'],
    })
    expect(agentGetByIdMock).not.toHaveBeenCalled()
  })

  it('loads knowledgeIds from agent detail when omitted', async () => {
    agentSkillPackagesMock.mockResolvedValue({ code: 200, data: [] })
    agentGetByIdMock.mockResolvedValue({
      code: 200,
      data: { knowledgeIds: '[2]' },
    })
    knowledgePageMock.mockResolvedValue({
      code: 200,
      data: { records: [{ id: 2, knowledgeName: '人事制度' }] },
    })

    const result = await resolveAgentRunResources({ agentId: '88' })

    expect(result.knowledge).toEqual(['人事制度'])
    expect(agentGetByIdMock).toHaveBeenCalledWith('88')
  })
})

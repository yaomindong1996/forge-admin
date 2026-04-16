import { AIGenerateResponse } from '@/api/ai/ai'

/**
 * 从 LLM 响应中提取 JSON
 */
export function extractJSON(text: string): string {
  let cleaned = text.trim()
  const jsonStart = cleaned.indexOf('{')
  const jsonEnd = cleaned.lastIndexOf('}')
  if (jsonStart >= 0 && jsonEnd > jsonStart) {
    cleaned = cleaned.slice(jsonStart, jsonEnd + 1).trim()
  }
  const codeBlockMatch = cleaned.match(/^```(?:json)?\s*\n?([\s\S]*?)\n?\s*```$/)
  if (codeBlockMatch) {
    cleaned = codeBlockMatch[1].trim()
  }
  return cleaned
}

/**
 * 从流式输出的完整文本中解析 AIGenerateResponse
 */
export function parseStreamedResponse(fullText: string): AIGenerateResponse {
  const jsonStr = extractJSON(fullText)
  let parsed: AIGenerateResponse
  try {
    parsed = JSON.parse(jsonStr)
  } catch (e) {
    console.error('[LLM Client] 流式 JSON 解析失败，原始内容:', fullText)
    throw new Error('AI 返回的 JSON 格式无效，请重试')
  }
  if (!parsed.title || !Array.isArray(parsed.components)) {
    throw new Error('AI 返回的数据结构不完整')
  }
  return parsed
}

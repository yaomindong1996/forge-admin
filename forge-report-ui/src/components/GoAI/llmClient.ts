import { AIGenerateResponse, AIGenerateRequest } from '@/api/ai/ai.d'
import { getComponentCatalogText } from './componentRegistry'

// LLM 环境配置
const LLM_BASE_URL = import.meta.env.VITE_LLM_BASE_URL || '/llm/v1'
const LLM_API_KEY = import.meta.env.VITE_LLM_API_KEY || ''
const LLM_MODEL = import.meta.env.VITE_LLM_MODEL || 'qwen-plus'

/**
 * 构建 System Prompt —— 告诉 LLM 可用组件和输出格式
 */
function buildSystemPrompt(canvasWidth: number, canvasHeight: number): string {
  const catalog = getComponentCatalogText()

  return `你是一个数据可视化大屏设计专家。用户会描述他们想要的数据大屏，你需要根据描述选择合适的组件并设计布局，输出一个 JSON 对象。

## 画布信息
- 画布尺寸: ${canvasWidth}px × ${canvasHeight}px
- 坐标原点在左上角，x 向右增大，y 向下增大
- 组件使用绝对定位，请合理安排布局，不要重叠

## 可用组件列表（只能使用以下组件 key）
${catalog}

## 输出 JSON 格式
你必须严格输出以下格式的 JSON（不要输出任何其他文字，不要用 markdown 代码块包裹）：

{
  "title": "大屏标题",
  "canvasConfig": {
    "width": ${canvasWidth},
    "height": ${canvasHeight},
    "background": "#1e1e2e"
  },
  "components": [
    {
      "key": "组件key",
      "x": 左上角x坐标,
      "y": 左上角y坐标,
      "w": 宽度,
      "h": 高度,
      "title": "组件显示标题",
      "option": { ... 根据组件类型填入，见下方规则 ... }
    }
  ]
}

## 各组件 option 填写规则（非常重要！）

### 图表类组件（BarCommon, BarCrossrange, BarLine, CapsuleChart, LineCommon, LineGradientSingle, LineGradients, PieCommon, PieCircle, ScatterCommon, Heatmap, TreeMap, Funnel, MapBase 等）
这些是 ECharts 图表组件，option 中 **只需要填 dataset 字段**，不需要填 series/tooltip/xAxis/yAxis 等配置（系统会自动补充）。
- dataset 格式: { "dimensions": ["列名1", "列名2", ...], "source": [{"列名1": 值, "列名2": 值}, ...] }
- dimensions 的第一个元素是类目名（如"月份"、"地区"），其余是数据系列名
- 示例: { "option": { "dataset": { "dimensions": ["月份", "销售额", "利润"], "source": [{"月份": "1月", "销售额": 820, "利润": 320}] } } }

### 饼图 PieCommon / PieCircle
- dataset 只需要 2 个 dimensions: 类目名 + 数值
- 示例: { "option": { "dataset": { "dimensions": ["地区", "销售额"], "source": [{"地区": "华东", "销售额": 3350}] } } }

### 雷达图 Radar
- dataset 使用特殊格式: { "radarIndicator": [{ "name": "指标名", "max": 100 }], "seriesData": [{ "name": "系列名", "value": [80, 90] }] }

### 文本组件 TextCommon
- option.dataset 填入要显示的文字内容（字符串）
- 可选: option.fontSize, option.fontColor, option.fontWeight, option.textAlign, option.letterSpacing

### 数字翻牌器 FlipperNumber
- option.dataset 填入要显示的数字，可选 option.unit (单位文字)

### 时钟 Clock / 倒计时 CountDown
- 不需要 option

### 装饰边框 Border01-Border13
- 不需要 option，只需位置和尺寸，用于包裹图表

### 排行榜 TableList
- option.dataset 填入对象数组: [{ "name": "项目名", "value": 数值 }, ...]

### 滚动表格 TableScrollBoard
- option.header 填入表头数组: ["列1", "列2", "列3"]
- option.dataset 填入二维数组（每行是字符串数组）: [["行1列1", "行1列2", "行1列3"], ["行2列1", "行2列2", "行2列3"]]

## 布局设计规则
1. 大屏顶部居中放置一个 TextCommon 作为标题（y=20, fontSize=32-40, fontColor=#ffffff, fontWeight=bold）
2. 标题区高度约 80px，剩余区域分为 2-3 列排列图表
3. 同行组件之间留 20-30px 间距
4. 数据要真实合理，符合大屏主题
5. 只输出纯 JSON，不要有任何额外文字或注释`
}

/**
 * 构建 User Prompt
 */
function buildUserPrompt(request: AIGenerateRequest): string {
  let prompt = `请为我生成一个数据可视化大屏：${request.prompt}`
  if (request.style === 'light') {
    prompt += '\n\n风格要求：浅色主题，背景使用浅色，文字使用深色。'
  } else {
    prompt += '\n\n风格要求：深色主题，背景使用深色（如 #1e1e2e），文字使用白色或浅色。'
  }
  return prompt
}

/**
 * 从 LLM 响应中提取 JSON
 */
export function extractJSON(text: string): string {
  let cleaned = text.trim()
  const codeBlockMatch = cleaned.match(/^```(?:json)?\s*\n?([\s\S]*?)\n?\s*```$/)
  if (codeBlockMatch) {
    cleaned = codeBlockMatch[1].trim()
  }
  return cleaned
}

/**
 * 调用 OpenAI 兼容接口（如阿里百炼）- 非流式
 */
export async function callLLM(request: AIGenerateRequest): Promise<AIGenerateResponse> {
  const canvasW = request.canvasWidth || 1920
  const canvasH = request.canvasHeight || 1080

  const systemPrompt = buildSystemPrompt(canvasW, canvasH)
  const userPrompt = buildUserPrompt(request)

  const response = await fetch(`${LLM_BASE_URL}/chat/completions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${LLM_API_KEY}`
    },
    body: JSON.stringify({
      model: LLM_MODEL,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userPrompt }
      ],
      temperature: 0.7,
      response_format: { type: 'json_object' }
    })
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(`LLM API 请求失败 (${response.status}): ${errorText}`)
  }

  const data = await response.json()
  const content = data?.choices?.[0]?.message?.content
  if (!content) {
    throw new Error('LLM 返回内容为空')
  }

  const jsonStr = extractJSON(content)
  let parsed: AIGenerateResponse
  try {
    parsed = JSON.parse(jsonStr)
  } catch (e) {
    console.error('[LLM Client] JSON 解析失败，原始内容:', content)
    throw new Error('LLM 返回的 JSON 格式无效，请重试')
  }

  if (!parsed.title || !Array.isArray(parsed.components)) {
    throw new Error('LLM 返回的数据结构不完整，缺少 title 或 components')
  }

  return parsed
}

/**
 * 流式调用 LLM - 用于 AI 大屏生成（SSE 流）
 * @param request - AI 生成请求
 * @param onChunk - 每个内容 chunk 的回调
 * @param onDone - 流式完成回调，传入完整文本
 * @param onError - 错误回调
 * @param signal - AbortController 信号，用于中止请求
 */
export async function callLLMStream(
  request: AIGenerateRequest,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void,
  signal?: AbortSignal
): Promise<void> {
  const canvasW = request.canvasWidth || 1920
  const canvasH = request.canvasHeight || 1080

  const systemPrompt = buildSystemPrompt(canvasW, canvasH)
  const userPrompt = buildUserPrompt(request)

  let response: Response
  try {
    response = await fetch(`${LLM_BASE_URL}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${LLM_API_KEY}`
      },
      body: JSON.stringify({
        model: LLM_MODEL,
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt }
        ],
        temperature: 0.7,
        stream: true
      }),
      signal
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onDone('') // 用户主动中止
      return
    }
    onError(err instanceof Error ? err : new Error(String(err)))
    return
  }

  if (!response.ok) {
    const errorText = await response.text()
    onError(new Error(`LLM API 请求失败 (${response.status}): ${errorText}`))
    return
  }

  await consumeSSEStream(response, onChunk, onDone, onError)
}

/**
 * 普通对话流式调用 - 用于 AI 聊天模式（不生成画布）
 */
export async function callLLMChat(
  messages: Array<{ role: 'user' | 'assistant' | 'system'; content: string }>,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void,
  signal?: AbortSignal
): Promise<void> {
  let response: Response
  try {
    response = await fetch(`${LLM_BASE_URL}/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${LLM_API_KEY}`
      },
      body: JSON.stringify({
        model: LLM_MODEL,
        messages,
        temperature: 0.7,
        stream: true
      }),
      signal
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      onDone('') // 用户主动中止
      return
    }
    onError(err instanceof Error ? err : new Error(String(err)))
    return
  }

  if (!response.ok) {
    const errorText = await response.text()
    onError(new Error(`LLM API 请求失败 (${response.status}): ${errorText}`))
    return
  }

  await consumeSSEStream(response, onChunk, onDone, onError)
}

/**
 * 内部工具：消费 SSE 流
 */
async function consumeSSEStream(
  response: Response,
  onChunk: (chunk: string) => void,
  onDone: (fullText: string) => void,
  onError: (error: Error) => void
): Promise<void> {
  const reader = response.body?.getReader()
  if (!reader) {
    onError(new Error('无法获取响应流'))
    return
  }

  const decoder = new TextDecoder('utf-8')
  let fullText = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n')

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || trimmed === 'data: [DONE]') continue
        if (!trimmed.startsWith('data: ')) continue

        try {
          const jsonStr = trimmed.slice(6)
          const parsed = JSON.parse(jsonStr)
          const delta = parsed?.choices?.[0]?.delta?.content
          if (delta) {
            fullText += delta
            onChunk(delta)
          }
        } catch {
          // 忽略解析失败的行
        }
      }
    }
    onDone(fullText)
  } catch (err) {
    onError(err instanceof Error ? err : new Error(String(err)))
  } finally {
    reader.releaseLock()
  }
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

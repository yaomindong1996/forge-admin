/** 从 Naive/AiForm 校验异常中提取可读必填提示；普通 Error / 非校验结构返回空串。 */
export function formatStartTestValidationMessage(error) {
  if (error == null || error instanceof Error)
    return ''
  const messages = []
  const walk = (value) => {
    if (value == null)
      return
    if (Array.isArray(value)) {
      value.forEach(walk)
      return
    }
    if (typeof value !== 'object')
      return
    if (typeof value.message === 'string') {
      const text = value.message.trim()
      if (text)
        messages.push(text)
    }
    if (value.errors)
      walk(value.errors)
  }
  walk(error)
  const unique = [...new Set(messages.filter(Boolean))]
  if (!unique.length)
    return ''
  return `请完善必填项：${unique.slice(0, 3).join('；')}`
}

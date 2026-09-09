/**
 * @fileoverview 拖拽协议
 * @description 定义设计器间拖拽的 MIME 类型、数据序列化和解析工具。
 *   统一表单设计器、列表页设计器、页面设计器的拖拽交互协议。
 */

/**
 * 拖拽 MIME 类型常量
 */
export const DESIGNER_MIME = {
  /** 组件 spec 拖拽 */
  COMPONENT: 'application/x-forge-designer-component',
  /** 已有实例移动 */
  INSTANCE: 'application/x-forge-designer-instance',
  /** 字段引用拖拽 */
  FIELD_REF: 'application/x-forge-designer-field-ref',
}

/**
 * 创建拖拽数据载荷
 * @param {'component' | 'instance' | 'field-ref'} kind - 拖拽类型
 * @param {Object} data - 拖拽数据
 * @param {string} data.type - 组件 type
 * @param {string} [data.source] - 来源面板标识
 * @param {string} [data.instanceId] - 实例 ID（instance 类型）
 * @param {string} [data.fieldName] - 字段名（field-ref 类型）
 * @returns {Object<string, string>} MIME → JSON 映射，可直接用于 dataTransfer.setData
 */
export function createDragPayload(kind, data) {
  const payload = {
    kind,
    type: data.type || '',
    source: data.source || '',
    timestamp: Date.now(),
  }

  if (kind === 'instance') {
    payload.instanceId = data.instanceId || ''
  }
  if (kind === 'field-ref') {
    payload.fieldName = data.fieldName || ''
  }

  const mimeMap = {
    'component': DESIGNER_MIME.COMPONENT,
    'instance': DESIGNER_MIME.INSTANCE,
    'field-ref': DESIGNER_MIME.FIELD_REF,
  }

  const mime = mimeMap[kind] || DESIGNER_MIME.COMPONENT
  return { [mime]: JSON.stringify(payload) }
}

/**
 * 解析拖拽数据载荷
 * @param {DataTransfer} dataTransfer - 原生 DataTransfer 对象
 * @returns {{ kind: string, type: string, source: string, instanceId?: string, fieldName?: string, timestamp: number } | null}
 */
export function parseDragPayload(dataTransfer) {
  if (!dataTransfer) return null

  const mimeTypes = [
    DESIGNER_MIME.COMPONENT,
    DESIGNER_MIME.INSTANCE,
    DESIGNER_MIME.FIELD_REF,
  ]

  for (const mime of mimeTypes) {
    const raw = dataTransfer.getData(mime)
    if (raw) {
      try {
        return JSON.parse(raw)
      } catch {
        return null
      }
    }
  }

  return null
}

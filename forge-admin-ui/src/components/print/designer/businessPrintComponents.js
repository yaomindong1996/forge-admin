import { printComponentRegistry } from './printComponentRegistry'

function fieldBinding(catalog, paths, type) {
  const collections = catalog.filter(field => field.type === 'COLLECTION').map(field => field.path)
  const field = paths.map(path => catalog.find(item => item.path === path))
    .find(item => item && item.type !== 'COLLECTION' && (!type || item.type === type) && !collections.some(path => item.path.startsWith(`${path}.`)))
  return field ? { source: 'FIELD', path: field.path } : null
}

function approvalStatus({ catalog }) {
  return {
    kind: 'SECTION',
    section: {
      id: 'approval-status',
      kind: 'FIXED',
      heightMm: 14,
      gapAfterMm: 2,
      elements: [
        { id: 'approval-label', type: 'TEXT', xMm: 0, yMm: 2, widthMm: 25, heightMm: 10, binding: { source: 'CONSTANT', value: '审批状态' }, style: { fontWeight: 700 } },
        { id: 'approval-value', type: 'TEXT', xMm: 27, yMm: 2, widthMm: 55, heightMm: 10, binding: fieldBinding(catalog, ['flow.status', 'main.approvalStatus', 'main.status'], 'TEXT') || { source: 'CONSTANT', value: '待审批（示例）' }, style: { borderWidthMm: 0.2, borderColor: '#94a3b8', paddingMm: 1 } },
      ],
    },
  }
}

function signaturePosition({ catalog }) {
  return {
    kind: 'SECTION',
    section: {
      id: 'signature-position',
      kind: 'FIXED',
      heightMm: 38,
      gapAfterMm: 2,
      elements: [
        { id: 'signature-label', type: 'TEXT', xMm: 0, yMm: 1, widthMm: 35, heightMm: 8, binding: { source: 'CONSTANT', value: '签章 / 签名' }, style: { fontWeight: 700 } },
        { id: 'signature-image', type: 'IMAGE', xMm: 0, yMm: 10, widthMm: 45, heightMm: 25, binding: fieldBinding(catalog, ['main.signature', 'flow.signature'], 'IMAGE') || { source: 'CONSTANT', value: '' }, style: { borderWidthMm: 0.2, borderColor: '#94a3b8', borderStyle: 'dashed', objectFit: 'contain' } },
      ],
    },
  }
}

function contractTerms({ catalog }) {
  return {
    kind: 'SECTION',
    section: {
      id: 'contract-terms',
      kind: 'TEXT',
      gapAfterMm: 2,
      binding: fieldBinding(catalog, ['main.contractTerms', 'main.terms', 'main.content'], 'TEXT') || { source: 'CONSTANT', value: '合同条款：双方应按照约定履行各自义务。' },
      style: { fontSizePt: 10, lineHeight: 1.6, paddingMm: 2, borderWidthMm: 0.2, borderColor: '#cbd5e1' },
    },
  }
}

export function registerBuiltInPrintComponents(registry = printComponentRegistry) {
  registry.register({ key: 'forge.approval-status', label: '审批状态', icon: 'approval', factory: approvalStatus })
  registry.register({ key: 'forge.signature-position', label: '签章位置', icon: 'signature', factory: signaturePosition })
  registry.register({ key: 'forge.contract-terms', label: '合同条款', icon: 'contract', factory: contractTerms })
  return registry
}

registerBuiltInPrintComponents()

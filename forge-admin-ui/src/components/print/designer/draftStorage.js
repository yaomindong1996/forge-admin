import { PRINT_LIMITS } from '../protocol/types'
import { assertPrintDocument } from '../protocol/validate'

const KEY = 'forge:print:local-draft:v1'

export function parseDraft(text) {
  if (typeof text !== 'string' || new TextEncoder().encode(text).length > PRINT_LIMITS.documentBytes) {
    throw new Error('模板超出允许大小')
  }
  const document = JSON.parse(text)
  assertPrintDocument(document)
  return document
}

export function readDraft(storage = localStorage) {
  const text = storage.getItem(KEY)
  return text ? parseDraft(text) : null
}

export function writeDraft(document, storage = localStorage) {
  assertPrintDocument(document)
  storage.setItem(KEY, JSON.stringify(document))
}

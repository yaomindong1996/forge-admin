/** Forge's paper protocol. These are technical grammar tokens, not business dictionaries. */
export const PRINT_PROTOCOL = 'forge-print'
export const PRINT_SCHEMA_VERSION = 1
export const PRINT_LIMITS = Object.freeze({
  documentBytes: 1024 * 1024,
  sections: 200,
  elements: 1000,
  columns: 50,
  staticTableRows: 50,
  staticTableColumns: 20,
  rows: 500,
  pages: 50,
  textLength: 100000,
  resourceTimeoutMs: 10000,
  inlineImageBytes: 512 * 1024,
  paperSizeMm: 2000,
})
export const ELEMENT_TYPES = Object.freeze(['TEXT', 'IMAGE', 'HTML', 'LINE', 'RECTANGLE', 'ELLIPSE', 'BARCODE', 'QRCODE', 'PAGE_NUMBER', 'STATIC_TABLE', 'DATA_TABLE', 'DESCRIPTIONS'])
export const SECTION_KINDS = Object.freeze(['FIXED', 'TEXT', 'TABLE', 'PAGE_BREAK'])
export const FORMAT_TYPES = Object.freeze(['TEXT', 'MONEY', 'MONEY_UPPER', 'NUMBER', 'DATE', 'BOOLEAN'])
export const BINDING_SOURCES = Object.freeze(['CONSTANT', 'FIELD', 'SYSTEM', 'EXPRESSION'])
export const TEXT_FIT_MODES = Object.freeze(['CLIP', 'SHRINK', 'AUTO_HEIGHT'])
export const PAPER_KINDS = Object.freeze(['SHEET', 'CONTINUOUS'])

/**
 * @typedef {object} PrintBinding
 * @property {'FIELD'|'CONSTANT'|'SYSTEM'|'EXPRESSION'} source Binding category.
 * @property {string} [path] Own-property path in an authorized print context.
 * @property {string|number|boolean|null} [value] Literal value, never executable code.
 * @property {string} [expression] Whitelisted arithmetic/template expression.
 *
 * @typedef {object} PrintDocument
 * @property {'forge-print'} protocol Protocol identity.
 * @property {number} schemaVersion Compatible schema revision.
 * @property {object} paper Physical millimetres, independent of designer zoom.
 * @property {object} header Fixed band; repeat=false means first page only.
 * @property {Array<object>} body Ordered content sections and explicit page breaks.
 * @property {object} footer Fixed band; repeat=false means last page only.
 * @property {Array<object>} resources Logical file references, never access tokens.
 */

/** @returns {PrintDocument} A new independent empty paper document. */
export function createPrintDocument() {
  return {
    protocol: PRINT_PROTOCOL,
    schemaVersion: PRINT_SCHEMA_VERSION,
    paper: {
      widthMm: 210,
      heightMm: 297,
      orientation: 'PORTRAIT',
      marginMm: { top: 10, right: 10, bottom: 10, left: 10 },
    },
    header: { heightMm: 0, repeat: true, elements: [] },
    body: [],
    footer: { heightMm: 0, repeat: true, elements: [] },
    resources: [],
  }
}

export class PrintError extends Error {
  constructor(code, message, path = '', details = []) {
    super(message)
    this.name = 'PrintError'
    this.code = code
    this.path = path
    this.details = details
  }
}

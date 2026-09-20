import JsBarcode from 'jsbarcode'
import qrcode from 'qrcode-generator'

export function buildQrMatrix(value, level = 'Q') {
  const encoder = qrcode(0, normalizeQrLevel(level))
  encoder.addData(String(value || ''))
  encoder.make()
  const size = encoder.getModuleCount()
  return Array.from({ length: size }, (_, row) =>
    Array.from({ length: size }, (_, column) => encoder.isDark(row, column)),
  )
}

export function buildBarcodeBits(value, format = 'CODE128') {
  const target = {}
  JsBarcode(target, String(value || ''), {
    format: normalizeBarcodeFormat(format),
    displayValue: false,
    margin: 0,
  })
  return (target.encodings || []).map(encoding => encoding.data || '').join('')
}

function normalizeQrLevel(level) {
  const normalized = String(level || 'Q').toUpperCase()
  return ['L', 'M', 'Q', 'H'].includes(normalized) ? normalized : 'Q'
}

function normalizeBarcodeFormat(format) {
  const normalized = String(format || 'CODE128').toUpperCase()
  return ['CODE128', 'CODE39', 'EAN13', 'UPC'].includes(normalized) ? normalized : 'CODE128'
}

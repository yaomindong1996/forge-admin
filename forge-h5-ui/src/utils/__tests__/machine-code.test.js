import assert from 'node:assert/strict'
import test from 'node:test'
import { buildBarcodeBits, buildQrMatrix } from '../machine-code.js'

test('builds a scannable QR matrix from protocol text', () => {
  const matrix = buildQrMatrix('https://forge.local', 'Q')
  assert.ok(matrix.length >= 21)
  assert.equal(matrix.length, matrix[0].length)
  assert.equal(typeof matrix[0][0], 'boolean')
})

test('builds barcode bits for every format exposed by the designer', () => {
  const samples = {
    CODE128: 'FORGE-2026-0001',
    CODE39: 'FORGE-2026',
    EAN13: '5901234123457',
    UPC: '123456789999',
  }
  Object.entries(samples).forEach(([format, value]) => {
    assert.match(buildBarcodeBits(value, format), /^[01]+$/)
  })
})

import { describe, expect, it } from 'vitest'
import { isExportFileNamePattern } from '../../protocol/exportFileName'
import {
  buildExportFileNamePattern,
  buildWatermarkExpression,
  composeWatermarkPreview,
  describeExportFileName,
  nextWatermark,
  parseExportFileNameParts,
  parseWatermarkContent,
  watermarkDensityValue,
} from '../paperPanelModel'

const catalog = [
  { path: 'main.name', label: '名称', type: 'TEXT' },
  { path: 'main.code', label: '编码', type: 'TEXT' },
]

describe('paper panel watermark model', () => {
  it('turns selected fields into a CONCAT expression and reads them back', () => {
    expect(buildWatermarkExpression(['main.name', 'main.code'])).toBe('CONCAT(main.name, " · ", main.code)')
    expect(parseWatermarkContent({
      expression: 'CONCAT("内部资料", " · ", main.name, " · ", main.code)',
    })).toMatchObject({
      text: '内部资料',
      fields: ['main.name', 'main.code'],
      parsedOk: true,
    })
  })

  it('maps density presets and keeps style-only watermarks', () => {
    expect(watermarkDensityValue({ gapXMm: 40, gapYMm: 28 })).toBe('dense')
    expect(nextWatermark(undefined, { color: '#334155', fontSizePt: 18 })).toMatchObject({
      color: '#334155',
      fontSizePt: 18,
    })
    expect(nextWatermark({
      expression: 'CONCAT("内部资料", " · ", main.name)',
    }, { fontSizePt: 18 })).toMatchObject({
      text: '内部资料',
      expression: 'main.name',
      fontSizePt: 18,
    })
    expect(nextWatermark(undefined, { enabled: false })).toMatchObject({ enabled: false })
    expect(nextWatermark(undefined, { text: '' })).toBeNull()
  })

  it('previews watermark as readable labels instead of expressions', () => {
    expect(composeWatermarkPreview({
      text: '内部资料',
      expression: 'CONCAT(main.name, " · ", main.code)',
    }, catalog)).toBe('内部资料 · 名称 · 编码')
  })
})

describe('paper panel export file name model', () => {
  it('hides token syntax behind checkboxes and field picks', () => {
    expect(parseExportFileNameParts('{template}-{{main.name}}-{timestamp}')).toEqual({
      extraText: '',
      includeTemplate: true,
      fields: ['main.name'],
    })
    expect(buildExportFileNamePattern({
      extraText: '采购',
      includeTemplate: true,
      fields: ['main.code'],
    })).toBe('采购-{template}-{{main.code}}')
    expect(describeExportFileName({ extraText: '', includeTemplate: true, fields: ['main.name'] }, catalog))
      .toBe('下载时会自动加上时间，例如：模板名-名称-时间.pdf')
    expect(describeExportFileName({ extraText: '', includeTemplate: true, fields: [] }, catalog, { isDefault: true }))
      .toContain('模板名和单据名称')
    expect(isExportFileNamePattern(buildExportFileNamePattern({
      extraText: '采购',
      includeTemplate: true,
      fields: ['main.code'],
    }))).toBe(true)
  })
})

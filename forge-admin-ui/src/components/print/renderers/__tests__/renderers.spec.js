import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PrintPage from '../../runtime/PrintPage.vue'
import PrintImage from '../PrintImage.vue'
import PrintShape from '../PrintShape.vue'
import PrintStaticTable from '../PrintStaticTable.vue'
import PrintTable from '../PrintTable.vue'
import PrintText from '../PrintText.vue'
import { tableCellStyle } from '../style'

describe('safe paper rendering', () => {
  it('uses the same CSS border width on every table edge', () => {
    const outer = tableCellStyle({ borderWidthMm: 0.15, borderColor: '#111111', backgroundColor: '#f1f5f9' }, { top: true, left: true })
    const inner = tableCellStyle({ borderWidthMm: 0.15, borderColor: '#111111' }, { top: false, left: false })
    expect(outer.borderTop).toBe(outer.borderRight)
    expect(outer.borderLeft).toBe(outer.borderBottom)
    expect(outer.borderTop).toBe('0.15mm solid #111111')
    expect(inner.borderRight).toBe(outer.borderRight)
    expect(inner.borderTop).toBe('none')
    expect(inner.borderLeft).toBe('none')
    expect(outer.backgroundClip).toBe('padding-box')
    expect(outer.backgroundImage).toBeUndefined()
  })
  it('renders plain text with line preservation without interpreting markup', () => {
    const wrapper = mount(PrintText, { props: { node: { lines: ['<img src=x onerror=alert(1)>', '', '合计'], lineHeightMm: 5 } } })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
    expect(wrapper.findAll('div')[2].attributes('style')).toContain('height: 5mm')
  })
  it('uses the measured row height and treats headers as plain text', () => {
    const wrapper = mount(PrintTable, { props: { node: { id: 'items', widthMm: 100, rows: [{ kind: 'header', heightMm: 9, cells: [{ widthMm: 100, text: '<b>名称</b>' }] }] } } })
    expect(wrapper.find('[role=row]').attributes('style')).toContain('height: 9mm')
    expect(wrapper.find('[role=columnheader]').attributes('style')).toContain('align-items: center')
    expect(wrapper.find('[role=columnheader]').attributes('style')).toContain('border-top: 0.15mm')
    expect(wrapper.find('[role=columnheader]').attributes('style')).toContain('border-left: 0.15mm')
    expect(wrapper.find('[role=columnheader]').attributes('style')).toContain('border-right: 0.15mm')
    expect(wrapper.find('[role=columnheader]').attributes('style')).toContain('border-bottom: 0.15mm')
    expect(wrapper.find('b').exists()).toBe(false)
    expect(wrapper.find('[role=columnheader]').text()).toBe('<b>名称</b>')
  })
  it('renders an authenticated signature image inside a table cell', () => {
    const wrapper = mount(PrintTable, { props: { node: { id: 'history', widthMm: 80, rows: [{ kind: 'data', heightMm: 20, cells: [{ widthMm: 80, type: 'IMAGE', text: '', src: 'blob:signature', imageHeightMm: 18 }] }] } } })
    expect(wrapper.get('img').attributes('src')).toBe('blob:signature')
    expect(wrapper.text()).not.toContain('blob:signature')
  })
  it('uses the protocol image fitting mode', () => {
    const wrapper = mount(PrintImage, { props: { node: { src: 'data:image/png;base64,AA==', style: { objectFit: 'cover' } } } })
    expect(wrapper.get('img').attributes('style')).toContain('object-fit: cover')
  })
  it('paints line shapes with CSS border color and style', () => {
    const wrapper = mount(PrintShape, { props: { node: { type: 'LINE', widthMm: 50, heightMm: 0.5, style: { borderColor: '#d93838', borderStyle: 'dashed' } } } })
    const line = wrapper.get('.print-shape.line')
    expect(line.classes()).toEqual(expect.arrayContaining(['horizontal', 'dashed']))
    expect(line.element.style.borderTop).toContain('0.5mm')
    expect(line.element.style.borderTop).toContain('dashed')
    expect(line.element.style.borderTop).toMatch(/#d93838|rgb\(217,\s*56,\s*56\)/)
  })
  it('paints vertical lines with the left border', () => {
    const wrapper = mount(PrintShape, { props: { node: { type: 'LINE', widthMm: 0.5, heightMm: 40, style: { backgroundColor: '#111111', borderStyle: 'dotted' } } } })
    const line = wrapper.get('.print-shape.line')
    expect(line.classes()).toEqual(expect.arrayContaining(['vertical', 'dotted']))
    expect(line.element.style.borderLeft).toContain('0.5mm')
    expect(line.element.style.borderLeft).toContain('dotted')
    expect(line.element.style.borderLeft).toMatch(/#111111|rgb\(17,\s*17,\s*17\)/)
  })
  it('renders protocol rotation and mirror transforms on the print page', () => {
    const element = { id: 'rotated', type: 'TEXT', xMm: 5, yMm: 5, widthMm: 30, heightMm: 8, rotationDeg: 90, flipX: true, lines: ['旋转文本'] }
    const page = {
      number: 1,
      header: { xMm: 0, yMm: 0, widthMm: 190, heightMm: 15, elements: [element] },
      footer: { xMm: 0, yMm: 277, widthMm: 190, heightMm: 10, elements: [] },
      fragments: [],
    }
    const wrapper = mount(PrintPage, { props: { page, geometry: { widthMm: 210, heightMm: 297 } } })
    expect(wrapper.get('[data-print-element="rotated"]').attributes('style')).toContain('rotate(90deg) scaleX(-1) scaleY(1)')
  })
  it('renders native blank-table spans as plain text with physical tracks', () => {
    const node = {
      table: {
        columns: [{ id: 'a', widthMm: 25 }, { id: 'b', widthMm: 35 }],
        rows: [{ id: 'r', heightMm: 10 }],
        cells: [{ id: 'c', row: 0, column: 0, rowSpan: 1, colSpan: 2, text: '<script>alert(1)</script>' }],
      },
    }
    const wrapper = mount(PrintStaticTable, { props: { node } })
    expect(wrapper.get('[role=table]').attributes('style')).toContain('display: grid')
    expect(wrapper.get('[role=table]').attributes('style')).toContain('25mm 35mm')
    expect(wrapper.get('[role=table]').attributes('style')).not.toContain('background-image')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('span 2')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('display: flex')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('border-top: 0.15mm')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('border-left: 0.15mm')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('border-right: 0.15mm')
    expect(wrapper.get('[role=cell]').attributes('style')).toContain('border-bottom: 0.15mm')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.text()).toContain('<script>alert(1)</script>')
  })
})

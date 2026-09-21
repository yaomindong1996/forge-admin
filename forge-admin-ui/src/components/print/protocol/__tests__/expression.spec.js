import { describe, expect, it } from 'vitest'
import { amountToChinese } from '../amountChinese'
import { evaluateExpression, parseExpression } from '../expression'
import { formatValue } from '../formatters'

describe('print expression engine', () => {
  it('evaluates arithmetic, field paths and templates without executing JavaScript', () => {
    const context = { main: { qty: 3, price: 2500, amount: 128800 }, rows: [{ qty: 1, amount: 100 }, { qty: 2, amount: 200 }] }
    expect(evaluateExpression('main.qty * main.price', context)).toBe(7500)
    expect(evaluateExpression('MONEY(main.qty * main.price)', context)).toBe('75.00')
    expect(evaluateExpression('合计：{SUM(amount)}', context)).toBe('合计：300')
    expect(evaluateExpression('IF(main.qty > 1, "多件", "一件")', context)).toBe('多件')
    expect(() => parseExpression('eval(main.qty)')).toThrow()
    expect(() => parseExpression('qty; window.alert(1)')).toThrow()
  })

  it('aggregates current rows for subtotal and summary', () => {
    const rows = [{ amount: 100 }, { amount: 250 }, { amount: 50 }]
    expect(evaluateExpression('SUM(amount)', { rows })).toBe(400)
    expect(evaluateExpression('COUNT()', { rows })).toBe(3)
    expect(evaluateExpression('AVG(amount)', { rows })).toBeCloseTo(400 / 3)
  })
})

describe('chinese amount uppercase', () => {
  it('formats yuan decimals and integer cents', () => {
    expect(amountToChinese(100)).toBe('壹佰元整')
    expect(amountToChinese('1288.00')).toBe('壹仟贰佰捌拾捌元整')
    expect(amountToChinese('0.01')).toBe('零元壹分')
    expect(formatValue(128800, { type: 'MONEY_UPPER' })).toBe('壹仟贰佰捌拾捌元整')
  })
})

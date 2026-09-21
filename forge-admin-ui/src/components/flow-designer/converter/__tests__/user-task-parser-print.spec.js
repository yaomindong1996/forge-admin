import { describe, expect, it } from 'vitest'
import { parseUserTaskConfig } from '../user-task-parser.js'
import { writeUserTaskConfig } from '../user-task-writer.js'
import { findElementsByLocalName, parseBpmnXml } from '../xml-utils.js'

function task(xml) {
  return findElementsByLocalName(parseBpmnXml(xml), 'userTask')[0]
}

function wrap(attrs) {
  return `<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn"><bpmn:process id="P"><bpmn:userTask id="T" ${attrs}/></bpmn:process></bpmn:definitions>`
}

describe('审批节点打印模板策略', () => {
  it('限制策略和模板子集能从 BPMN 解析并原样写回', () => {
    const config = parseUserTaskConfig(task(wrap('flowable:printTemplatePolicy="RESTRICT" flowable:printTemplateIds="18, 20,18"')))
    expect(config.printTemplatePolicy).toBe('RESTRICT')
    expect(config.printTemplateIds).toEqual(['18', '20', '18'])

    const written = writeUserTaskConfig(config)
    expect(written.attrs).toContain('flowable:printTemplatePolicy="RESTRICT"')
    expect(written.attrs).toContain('flowable:printTemplateIds="18,20"')

    const roundTrip = parseUserTaskConfig(task(wrap(written.attrs)))
    expect(roundTrip.printTemplatePolicy).toBe('RESTRICT')
    expect(roundTrip.printTemplateIds).toEqual(['18', '20'])
  })

  it('旧模型和未知策略都按继承处理且不写冗余属性', () => {
    const legacy = parseUserTaskConfig(task(wrap('')))
    const unknown = parseUserTaskConfig(task(wrap('flowable:printTemplatePolicy="ALL" flowable:printTemplateIds="bad,0"')))

    expect(legacy.printTemplatePolicy).toBe('INHERIT')
    expect(legacy.printTemplateIds).toEqual([])
    expect(unknown.printTemplatePolicy).toBe('INHERIT')
    expect(unknown.printTemplateIds).toEqual([])
    expect(writeUserTaskConfig(legacy).attrs).not.toContain('printTemplate')
  })

  it('限制策略允许空子集，表示当前节点不提供模板', () => {
    const written = writeUserTaskConfig({ printTemplatePolicy: 'RESTRICT', printTemplateIds: [] })
    expect(written.attrs).toContain('flowable:printTemplatePolicy="RESTRICT"')
    expect(written.attrs).not.toContain('printTemplateIds')
  })
})

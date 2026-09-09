import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import SchemaTreeEditor from '../SchemaTreeEditor.vue'

function lastEmit(wrapper) {
  const events = wrapper.emitted('update:modelValue') || []
  return events.at(-1)?.[0]
}

function findButton(wrapper, label) {
  return wrapper.findAll('button').find(button => button.text().includes(label))
}

async function fillInput(input, value) {
  await input.setValue(value)
  await nextTick()
}

describe('schema tree editor', () => {
  it('rebuilds nested tree from flat schema with dotted paths', () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: {
        mode: 'output',
        modelValue: JSON.stringify([
          { name: 'Result', label: '结果', type: 'array', path: 'Result' },
          { name: 'companyName', label: '企业名称', type: 'string', path: 'Result.0.Name' },
        ]),
      },
    })

    const rows = wrapper.findAll('.ste-row')
    expect(rows).toHaveLength(2)
    // 子字段行缩进一层
    expect(rows[1].attributes('style')).toContain('padding-left: 18px')
  })

  it('computes child paths automatically for object and array parents', async () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: {
        mode: 'output',
        modelValue: JSON.stringify([{ name: 'Result', label: '结果', type: 'object', path: 'Result' }]),
      },
    })

    await findButton(wrapper, '+子').trigger('click')
    await nextTick()

    const childNameInput = wrapper.findAll('.ste-row')[1].findAll('input')[0]
    await fillInput(childNameInput, 'companyName')

    const emitted = JSON.parse(lastEmit(wrapper))
    expect(emitted).toHaveLength(2)
    expect(emitted[1]).toMatchObject({ name: 'companyName', path: 'Result.companyName' })
  })

  it('uses array index syntax for children of array nodes', async () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: {
        mode: 'output',
        modelValue: JSON.stringify([{ name: 'Items', label: '列表', type: 'array', path: 'Items' }]),
      },
    })

    await findButton(wrapper, '+子').trigger('click')
    await nextTick()

    const childNameInput = wrapper.findAll('.ste-row')[1].findAll('input')[0]
    await fillInput(childNameInput, 'Name')

    const emitted = JSON.parse(lastEmit(wrapper))
    expect(emitted[1]).toMatchObject({ name: 'Name', path: 'Items.0.Name' })
  })

  it('keeps custom paths untouched when serializing back', async () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: {
        mode: 'output',
        modelValue: JSON.stringify([
          { name: 'Result', label: '结果', type: 'array', path: 'Result' },
          { name: 'companyName', label: '企业名称', type: 'string', path: 'Result.0.Name' },
        ]),
      },
    })

    const labelInput = wrapper.findAll('.ste-row')[1].findAll('input')[1]
    await fillInput(labelInput, '企业全称')

    const emitted = JSON.parse(lastEmit(wrapper))
    expect(emitted[1]).toMatchObject({ name: 'companyName', label: '企业全称', path: 'Result.0.Name' })
  })

  it('applies edited JSON array back to the structured tree', async () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: { mode: 'output', modelValue: '[]' },
    })

    await findButton(wrapper, 'JSON 模式').trigger('click')
    await nextTick()

    const textarea = wrapper.find('textarea')
    await fillInput(textarea, JSON.stringify([{ name: 'code', label: '状态码', type: 'string', path: 'code' }]))

    await findButton(wrapper, '应用并回到结构化').trigger('click')
    await nextTick()

    const emitted = JSON.parse(lastEmit(wrapper))
    expect(emitted[0]).toMatchObject({ name: 'code', path: 'code' })
    expect(wrapper.findAll('.ste-row')).toHaveLength(1)
  })

  it('keeps input mode flat without path column', async () => {
    const wrapper = mount(SchemaTreeEditor, {
      props: {
        mode: 'input',
        modelValue: JSON.stringify([{ name: 'keyword', label: '关键词', type: 'string', required: true }]),
      },
    })

    const rows = wrapper.findAll('.ste-card')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('字段名')
    expect(rows[0].text()).toContain('显示名称')
    expect(rows[0].text()).not.toContain('取值路径')
    expect(findButton(wrapper, '+子')).toBeUndefined()

    const nameInput = rows[0].findAll('input')[0]
    await fillInput(nameInput, 'searchKey')
    const emitted = JSON.parse(lastEmit(wrapper))
    expect(emitted[0]).toMatchObject({ name: 'searchKey', type: 'string', required: true })
    expect(emitted[0].path).toBeUndefined()
  })
})

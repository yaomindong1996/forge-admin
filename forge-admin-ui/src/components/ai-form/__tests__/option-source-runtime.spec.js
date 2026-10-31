import { describe, expect, it } from 'vitest'
import {
  buildQuerySourceDisplayFields,
  buildTreeFromFlatRows,
  clampOptionPageSize,
  createDefaultTreeOptionSourcePatch,
  decorateLazyTreeNodes,
  decorateResolvedTreeLeafNodes,
  DEFAULT_TREE_OPTION_PAGE_SIZE,
  MAX_OPTION_PAGE_SIZE,
  readOptionRowField,
  REMOTE_OPTION_PENDING_LABEL,
  resolveFirstFilledOptionField,
  resolveFieldCascadeConfig,
  resolveOptionLoadMode,
  resolveOptionPageSize,
  resolvePendingOptionLabel,
  resolveStorageTypeFromOptionValueMeta,
  rowsHaveNestedChildren,
  shouldBuildTreeOptions,
} from '../option-source-runtime'

describe('option-source-runtime', () => {
  it('组装 valueField + labelField 到查询字段列表，保证显示字段会被返回', () => {
    expect(buildQuerySourceDisplayFields({
      valueField: 'id',
      labelField: 'customerName',
      displayFields: ['customerName:客户名称', 'code'],
    })).toEqual(['customerName', 'code', 'id'])
  })

  it('fieldMappings 源字段一并投影，保证选中后回填能读到源列', () => {
    expect(buildQuerySourceDisplayFields({
      valueField: 'id',
      labelField: 'name',
      fieldMappings: [
        { sourceField: 'metricName', targetField: 'fieldInput3' },
        { source: 'code', target: 'codeField' },
      ],
    }, ['extraCol'])).toEqual(['metricName', 'code', 'extraCol', 'id', 'name'])
  })

  it('树场景把 parentField 一并投影，避免扁平转树缺父级列', () => {
    expect(buildQuerySourceDisplayFields({
      valueField: 'id',
      labelField: 'name',
      parentField: 'parentId',
    })).toEqual(['id', 'name', 'parentId'])
  })

  it('从顶层或 _raw 读取显示字段，避免只回显 id', () => {
    expect(readOptionRowField({ id: '1', customerName: '张三' }, 'customerName')).toBe('张三')
    expect(readOptionRowField({
      id: '1',
      _raw: { id: '1', customerName: '李四' },
    }, 'customerName')).toBe('李四')
    expect(resolveFirstFilledOptionField({
      id: '9',
      _raw: { customerName: '王五' },
    }, ['customerName', 'name', 'id'])).toBe('王五')
  })

  it('远程加载中用占位标签，禁止把 id 当回显文案', () => {
    expect(resolvePendingOptionLabel({
      companionLabel: '已保存名称',
      remotePending: true,
    })).toBe('已保存名称')
    expect(resolvePendingOptionLabel({
      companionLabel: '',
      remotePending: true,
    })).toBe(REMOTE_OPTION_PENDING_LABEL)
    expect(resolvePendingOptionLabel({
      companionLabel: '',
      remotePending: false,
    })).toBe('')
  })

  it('扁平列表按 parentField 拼树，孤儿进根层', () => {
    const tree = buildTreeFromFlatRows([
      { value: 1, label: '总部', parentId: 0 },
      { value: 2, label: '研发', parentId: 1 },
      { value: 3, label: '市场', parentId: 1 },
      { value: 9, label: '游离', parentId: 99 },
    ], { parentField: 'parentId' })
    expect(tree).toHaveLength(2)
    expect(tree[0].label).toBe('总部')
    expect(tree[0].children.map(n => n.label)).toEqual(['研发', '市场'])
    expect(tree[1].label).toBe('游离')
  })

  it('pageSize 钳制与树/下拉默认值', () => {
    expect(clampOptionPageSize(0)).toBe(50)
    expect(clampOptionPageSize(99999)).toBe(MAX_OPTION_PAGE_SIZE)
    expect(resolveOptionPageSize({}, { isTree: true })).toBe(DEFAULT_TREE_OPTION_PAGE_SIZE)
    expect(resolveOptionPageSize({ pageSize: 20 })).toBe(20)
    expect(resolveOptionLoadMode({ loadMode: 'lazy' })).toBe('lazy')
    expect(resolveOptionLoadMode({})).toBe('full')
  })

  it('shouldBuildTreeOptions 识别组件类型与 structure/parentField', () => {
    expect(shouldBuildTreeOptions({}, 'treeSelect')).toBe(true)
    expect(shouldBuildTreeOptions({ structure: 'tree' }, 'select')).toBe(true)
    expect(shouldBuildTreeOptions({ parentField: 'parentId' }, 'select')).toBe(true)
    expect(shouldBuildTreeOptions({ type: 'QUERY_SOURCE' }, 'select')).toBe(false)
  })

  it('懒加载节点默认非叶子；嵌套检测', () => {
    expect(rowsHaveNestedChildren([{ children: [{ value: 1 }] }])).toBe(true)
    expect(rowsHaveNestedChildren([{ value: 1 }])).toBe(false)
    const decorated = decorateLazyTreeNodes([{ value: 1, label: '根' }])
    expect(decorated[0].isLeaf).toBe(false)
    const leaf = decorateLazyTreeNodes([{ value: 2, hasChildren: false, children: [] }])
    expect(leaf[0].isLeaf).toBe(true)
    expect(leaf[0].children).toBeUndefined()
    expect(createDefaultTreeOptionSourcePatch().parentField).toBe('parentId')
  })

  it('全量树无下级节点标记叶子并去掉空 children', () => {
    const decorated = decorateResolvedTreeLeafNodes([
      {
        value: 1,
        label: '根',
        children: [
          { value: 2, label: '叶子', children: [] },
          { value: 3, label: '枝', children: [{ value: 4, label: '孙' }] },
        ],
      },
    ])
    expect(decorated[0].isLeaf).toBe(false)
    expect(decorated[0].children[0]).toMatchObject({ value: 2, isLeaf: true })
    expect(decorated[0].children[0].children).toBeUndefined()
    expect(decorated[0].children[1].isLeaf).toBe(false)
    expect(decorated[0].children[1].children[0].isLeaf).toBe(true)
  })

  it('值字段存储类型严格跟随目标字段 dataType，不靠字段名猜测', () => {
    expect(resolveStorageTypeFromOptionValueMeta({ dataType: 'int', length: 11 }, 'status')).toMatchObject({
      dataType: 'int',
      length: 11,
    })
    expect(resolveStorageTypeFromOptionValueMeta({ dataType: 'bigint' }, 'id')).toMatchObject({
      dataType: 'bigint',
      length: null,
    })
    expect(resolveStorageTypeFromOptionValueMeta({ dataType: 'varchar', length: 32 }, 'code')).toMatchObject({
      dataType: 'varchar',
      length: 32,
    })
    // 没有真实类型时不瞎猜（即使字段名叫 id）
    expect(resolveStorageTypeFromOptionValueMeta({}, 'id')).toBeNull()
    expect(resolveStorageTypeFromOptionValueMeta({ type: 'string' }, 'id')).toMatchObject({
      dataType: 'varchar',
    })
  })

  it('子表列 sourceField=自身字段名时不误判为级联（避免选项被 emptyStrategy 滤空）', () => {
    expect(resolveFieldCascadeConfig({
      field: 'fieldSelect',
      sourceField: 'fieldSelect',
      type: 'select',
      props: {
        optionSource: { type: 'QUERY_SOURCE', querySourceCode: 'detail_lmd1' },
      },
    })).toBeNull()

    expect(resolveFieldCascadeConfig({
      field: 'fieldSelect',
      type: 'select',
      props: {
        sourceField: 'fieldSelect',
        cascade: { enabled: false, sourceField: 'fieldSelect', emptyStrategy: 'empty' },
      },
    })).toBeNull()
  })

  it('显式 cascade.sourceField 指向其它字段时仍启用级联', () => {
    expect(resolveFieldCascadeConfig({
      field: 'city',
      props: {
        cascade: { sourceField: 'province', emptyStrategy: 'empty', mode: 'param' },
      },
    })).toMatchObject({
      enabled: true,
      sourceField: 'province',
      emptyStrategy: 'empty',
      mode: 'param',
    })
  })

  it('旧版顶层 matchMode + sourceField 仍可解析为级联', () => {
    expect(resolveFieldCascadeConfig({
      field: 'city',
      sourceField: 'province',
      matchMode: 'linkedDict',
      linkedDictType: 'sys_city',
    })).toMatchObject({
      enabled: true,
      sourceField: 'province',
      mode: 'linkedDict',
      linkedDictType: 'sys_city',
    })
  })
})

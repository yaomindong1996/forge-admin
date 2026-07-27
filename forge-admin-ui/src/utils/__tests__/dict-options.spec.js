import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { computed, nextTick, ref } from 'vue'
import {
  mapDictOptionValues,
  normalizeDictOptionValue,
  toBooleanDictOptions,
  toNumberDictOptions,
} from '../dict-options'

const sourceOptions = [
  { label: '启用', value: '1', listClass: 'success' },
  { label: '禁用', value: '0', listClass: 'error' },
]

const datasetSource = readFileSync(
  resolve(process.cwd(), 'src/views/data/dataset.vue'),
  'utf8',
)
const frontendDictionaryMigrationSource = readFileSync(
  resolve(process.cwd(), '../forge-server/db/migration/V1.0.54__add_frontend_runtime_dicts.sql'),
  'utf8',
)

describe('dict option converters', () => {
  it('converts string values to numbers without dropping metadata', () => {
    expect(toNumberDictOptions(sourceOptions)).toEqual([
      { label: '启用', value: 1, listClass: 'success' },
      { label: '禁用', value: 0, listClass: 'error' },
    ])
  })

  it('converts common dictionary values to strict booleans', () => {
    expect(toBooleanDictOptions(sourceOptions).map(item => item.value)).toEqual([true, false])
    expect(toBooleanDictOptions([
      { label: '调试', value: 'true' },
      { label: '正式', value: false },
    ]).map(item => item.value)).toEqual([true, false])
  })

  it('maps protocol values and preserves unknown values', () => {
    const options = [
      { label: 'MySQL', value: 'MYSQL' },
      { label: 'Custom', value: 'CUSTOM' },
    ]

    expect(mapDictOptionValues(options, { MYSQL: 'MySQL' })).toEqual([
      { label: 'MySQL', value: 'MySQL' },
      { label: 'Custom', value: 'CUSTOM' },
    ])
  })

  it('returns an empty array for missing inputs', () => {
    expect(toNumberDictOptions()).toEqual([])
    expect(toBooleanDictOptions(null)).toEqual([])
    expect(mapDictOptionValues(undefined, {})).toEqual([])
  })

  it('preserves persisted values until dictionary options finish loading', () => {
    expect(normalizeDictOptionValue([], 'OR', 'AND')).toBe('OR')
    expect(normalizeDictOptionValue([{ label: '或', value: 'OR' }], 'OR', 'AND')).toBe('OR')
    expect(normalizeDictOptionValue([{ label: '且', value: 'AND' }], 'UNKNOWN', 'AND')).toBe('AND')
    expect(normalizeDictOptionValue([], '', 'OBJECT_LIST')).toBe('OBJECT_LIST')
  })

  it('updates computed options when dictionaries arrive asynchronously', async () => {
    const dictionary = ref([])
    const options = computed(() => toNumberDictOptions(dictionary.value))

    expect(options.value).toEqual([])
    dictionary.value = sourceOptions
    await nextTick()

    expect(options.value.map(item => item.value)).toEqual([1, 0])
  })

  it('keeps dataset publish status filters numeric', () => {
    expect(datasetSource).toContain(
      'computed(() => toNumberDictOptions(dict.value.data_dataset_publish_status))',
    )
  })

  it('does not mask unknown dataset states with hardcoded business labels', () => {
    expect(datasetSource).not.toContain('item?.label || \'未发布\'')
    expect(datasetSource).not.toContain('item?.label || \'公开\'')
    expect(datasetSource).not.toContain('item?.label || \'单表数据集\'')
    expect(datasetSource).toContain('getEnableStatusLabel(item.status)')
  })

  it('renders dataset types from dictionary metadata in the list', () => {
    const tableColumnsSource = datasetSource.slice(
      datasetSource.indexOf('const tableColumns = computed'),
      datasetSource.indexOf('const fieldColumns = computed'),
    )
    expect(tableColumnsSource).toContain('h(DictTag, {')
    expect(tableColumnsSource).toContain('options: datasetTypeOptions.value')
    expect(tableColumnsSource).toContain('value: row.datasetType')
    expect(tableColumnsSource).not.toContain('row.datasetType === \'TABLE\' ? \'单表\' : \'SQL\'')
  })

  it('hydrates row-scope dictionaries before assembling read-only details', () => {
    const detailHook = datasetSource.slice(
      datasetSource.indexOf('async function beforeRenderDetail'),
      datasetSource.indexOf('async function handleConnectionChange'),
    )
    expect(detailHook.indexOf('await ensureRowScopeDictOptions()')).toBeGreaterThanOrEqual(0)
    expect(detailHook.indexOf('await ensureRowScopeDictOptions()'))
      .toBeLessThan(detailHook.indexOf('prepareDatasetFormData'))
  })

  it('seeds every persisted business app entry mode', () => {
    expect(frontendDictionaryMigrationSource).toContain(
      "'访问入口打开方式', 'ai_business_app_entry_mode'",
    )
    for (const value of ['RUNTIME', 'ROUTE', 'IFRAME', 'EXTERNAL', 'H5', 'API']) {
      expect(frontendDictionaryMigrationSource).toContain(
        `'${value}', 'ai_business_app_entry_mode'`,
      )
    }
  })
})

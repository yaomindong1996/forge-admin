import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildMobileSelectionPatch,
  flattenMobileSelectorOptions,
  normalizeMobileSelectorConfig,
  normalizeMobileSelectorOptions,
  parseRegisteredOptionApi,
  resolveMobileSelectionLabels,
  resolveMobileSelectorKind,
  resolveMobileSelectorParams,
  serializeMobileSelectionValues,
} from '../mobile-selector-runtime.js'

test('normalizes record selector aliases and preserves string IDs', () => {
  const field = {
    field: 'customerId',
    type: 'recordSelector',
    props: {
      recordSelector: {
        targetObjectCode: 'customer',
        multiple: true,
        displayFields: ['customerName:客户名称'],
      },
    },
  }
  const config = normalizeMobileSelectorConfig(field)
  assert.equal(config.objectCode, 'customer')
  assert.equal(config.multiple, true)
  assert.equal(resolveMobileSelectorKind(field), 'record')
  const options = normalizeMobileSelectorOptions([
    { id: 9223372036854775807n.toString(), customerName: '超长编号客户' },
  ], config)
  assert.equal(options[0].value, '9223372036854775807')
  assert.equal(options[0].label, '超长编号客户')
})

test('flattens organization trees without losing hierarchy', () => {
  const config = normalizeMobileSelectorConfig({ type: 'orgTreeSelect' })
  const options = normalizeMobileSelectorOptions([
    { id: '1', orgName: '总部', children: [{ id: '2', orgName: '研发部' }] },
  ], config, { kind: 'org' })
  const flat = flattenMobileSelectorOptions(options)
  assert.deepEqual(flat.map(item => [item.value, item.level]), [['1', 0], ['2', 1]])
})

test('allows only registered same-origin option APIs', () => {
  assert.deepEqual(parseRegisteredOptionApi('get@/system/org/tree'), { method: 'get', url: '/system/org/tree' })
  assert.deepEqual(parseRegisteredOptionApi('/ai/options', 'POST'), { method: 'post', url: '/ai/options' })
  assert.equal(parseRegisteredOptionApi('get@https://evil.example/data'), null)
  assert.equal(parseRegisteredOptionApi('delete@/system/user/1'), null)
  assert.equal(resolveMobileSelectorKind({ type: 'customSelect', api: 'get@https://evil.example/data' }), 'blocked-api')
})

test('resolves declarative params from form and runtime context', () => {
  const params = resolveMobileSelectorParams({
    orgId: '$form.orgId',
    tenant: '${user.tenantId}',
    empty: '${formData.empty}',
  }, { orgId: '100', empty: '' }, { user: { tenantId: '1' } })
  assert.deepEqual(params, { orgId: '100', tenant: '1' })
})

test('recognizes managed query sources and linkage params', () => {
  const field = {
    type: 'customSelect',
    props: {
      optionSource: { type: 'QUERY_SOURCE', sourceType: 'DATASET', sourceKey: 'active_users' },
      linkageContext: { sourceField: 'orgId', sourceValue: '88', paramName: 'departmentId' },
    },
  }
  const config = normalizeMobileSelectorConfig(field)
  assert.equal(config.querySourceType, 'DATASET')
  assert.equal(config.querySourceKey, 'active_users')
  assert.equal(config.params.departmentId, '88')
  assert.equal(resolveMobileSelectorKind(field), 'query-source')
})

test('serializes multi values and applies label and record mappings', () => {
  const field = {
    field: 'customerId',
    type: 'recordSelector',
    props: {
      labelValueField: 'customerName',
      fieldMappings: [{ sourceField: 'credit.code', targetField: 'creditCode' }],
    },
  }
  assert.equal(serializeMobileSelectionValues(['9', '10'], true), '9,10')
  assert.deepEqual(buildMobileSelectionPatch(field, [{ credit: { code: 'A' } }], ['客户甲']), {
    customerName: '客户甲',
    creditCode: 'A',
  })
  assert.deepEqual(resolveMobileSelectionLabels('9', [], field, { customerName: '客户甲' }), ['客户甲'])
})

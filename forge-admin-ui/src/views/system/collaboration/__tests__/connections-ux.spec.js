import { readFileSync } from 'node:fs'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ConnectionStatusBadge from '../components/ConnectionStatusBadge.vue'
import { normalizeConnectionDetail } from '../connection-form'

function readSource(relativeUrl) {
  return readFileSync(new URL(relativeUrl, import.meta.url), 'utf8')
}

function mountStatus(row) {
  return mount(ConnectionStatusBadge, { props: { row } })
}

describe('collaboration connection status', () => {
  it.each([
    [{ status: '0', platform: 'WECHAT_ENTERPRISE', connectionName: '企业微信' }, '已停用'],
    [{ status: 1, platform: '', connectionName: '未完成连接' }, '待完善'],
    [{ status: 1, platform: 'WECHAT_ENTERPRISE', connectionName: '企业微信' }, '缺少企业标识'],
    [{ status: 1, platform: 'GITHUB', connectionName: 'GitHub 登录' }, '基础信息完整'],
    [{ status: '1', platform: 'DINGTALK', connectionName: '钉钉', enterpriseId: 'corp-id' }, '基础信息完整'],
  ])('shows %s as %s', (row, label) => {
    expect(mountStatus(row).text()).toContain(label)
  })
})

describe('collaboration connection editing contract', () => {
  it('keeps technical identifiers stable and reveals dependent fields from user choices', () => {
    const source = readSource('../connections.vue')

    expect(source).toContain('field: \'connectionCode\'')
    expect(source).toContain('disabled: ({ formData }) => Boolean(formData?.id)')
    expect(source).toContain('type: \'orgTreeSelect\'')
    expect(source).toContain('String(formData.defaultOrgId)')
    expect(source).toContain('normalizeConnectionDetail(data)')
    expect(source).toMatch(/field: 'todoPushEnabled',[\s\S]*?vIf: formData => isEnterprisePlatform\(formData\.platform\)/)
    expect(source).toMatch(/field: 'todoPushH5Url',[\s\S]*?formData\.todoPushEnabled === 1/)
    expect(source).toMatch(/field: 'syncCron',[\s\S]*?formData\.syncScheduleEnabled === 1/)
  })

  it('uses one task-oriented setup panel for applications and capabilities', () => {
    const source = readSource('../components/ConnectionSetupPanel.vue')

    expect(source).toContain('class="connection-workspace"')
    expect(source).not.toContain('<n-steps')
    expect(source).toContain('title="启用能力"')
    expect(source).toContain('还差一步：新增平台应用并填写凭据')
    expect(source).toContain('仍缺少有效的应用 ID 或 Secret')
    expect(source).toContain('编辑时留空表示保留原值')
    expect(source).toContain('编辑基础信息')
    expect(source).not.toContain('物理应用')
    // 配置内容区铺满工作台，避免宽屏右侧大块空白
    expect(source).not.toContain('max-width: 1280px')
    expect(source).toContain('.setup-panel {\n  width: 100%')
  })
})

describe('connection configuration round trip', () => {
  it('unwraps the detail VO and retains all switches and long IDs without changing server data', () => {
    const connection = { id: '9223372036854775801', status: 1, defaultOrgId: '9223372036854775802', defaultRoleIds: '9223372036854775803, 6', todoPushEnabled: '1', ssoWorkbenchEnabled: 1, syncScheduleEnabled: '1', syncCron: '0 0 2 * * ?', todoPushH5Url: 'https://example.test/h5' }
    const result = normalizeConnectionDetail({ connection, apps: [], bindings: [] })
    expect(result).toEqual({ ...connection, status: '1', defaultRoleIds: ['9223372036854775803', '6'], todoPushEnabled: 1, syncScheduleEnabled: 1 })
    expect(result.connection).toBeUndefined()
    expect(connection.status).toBe(1)
    expect(typeof connection.defaultRoleIds).toBe('string')
  })
  it('also supports list rows, array roles and absent optional configuration', () => {
    expect(normalizeConnectionDetail({ id: '8', defaultRoleIds: ['9'] })).toMatchObject({ id: '8', defaultRoleIds: ['9'], todoPushEnabled: 0, ssoWorkbenchEnabled: 0, syncScheduleEnabled: 0 })
    expect(normalizeConnectionDetail(null)).toBeNull()
  })
})

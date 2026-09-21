import { readFileSync } from 'node:fs'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ConnectionStatusBadge from '../components/ConnectionStatusBadge.vue'

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
    expect(source).toContain('map(id => id.trim()).filter(Boolean)')
    expect(source).toMatch(/field: 'todoPushEnabled',[\s\S]*?vIf: formData => isEnterprisePlatform\(formData\.platform\)/)
    expect(source).toMatch(/field: 'todoPushH5Url',[\s\S]*?formData\.todoPushEnabled === 1/)
    expect(source).toMatch(/field: 'syncCron',[\s\S]*?formData\.syncScheduleEnabled === 1/)
  })

  it('uses one task-oriented setup panel for applications and capabilities', () => {
    const source = readSource('../components/ConnectionSetupPanel.vue')

    expect(source).toContain('title="平台应用"')
    expect(source).toContain('title="启用能力"')
    expect(source).toContain('还差一步：新增平台应用并填写凭据')
    expect(source).toContain('仍缺少有效的应用 ID 或 Secret')
    expect(source).toContain('编辑时留空表示保留原值')
    expect(source).toContain('编辑基础信息')
    expect(source).not.toContain('物理应用')
  })
})

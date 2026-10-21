import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(path) {
  return readFileSync(resolve(path), 'utf8')
}

describe('forge home workbench applications', () => {
  it('loads authorized distributed applications from the backend protocol', () => {
    const homeSource = readSource('src/views/home/index.vue')
    const apiSource = readSource('src/api/business-application.js')

    expect(apiSource).toContain('request.get(\'/ai/business/application/workbench\'')
    expect(homeSource).toContain('businessApplicationWorkbench')
    expect(homeSource).toContain('loadWorkbenchApplications')
    expect(homeSource).toContain('/app/$' + '{encodeURIComponent(application.portalSlug || application.applicationCode)}')
  })

  it('shows an explicit empty state instead of fabricated application entries', () => {
    const homeSource = readSource('src/views/home/index.vue')

    expect(homeSource).toContain('暂无已投放应用')
    expect(homeSource).not.toContain('{ title: \'应用中心\', desc: \'创建和维护业务应用\'')
  })

  it('keeps snowflake role identifiers as strings and only exposes Forge home distribution', () => {
    const distributeSource = readSource('src/views/app-center/components/publish/AppPublishDistribute.vue')

    expect(distributeSource).toContain('normalizeRoleIds(workbench.roleIds)')
    expect(distributeSource).toContain('/system/role/page')
    expect(distributeSource).toContain('系统首页「我的应用」')
    expect(distributeSource).not.toContain('DINGTALK')
    expect(distributeSource).not.toContain('dingtalk')
    expect(distributeSource).not.toContain('.map(Number)')
  })
})

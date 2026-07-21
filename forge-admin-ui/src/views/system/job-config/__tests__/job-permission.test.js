import { describe, expect, it } from 'vitest'
import { hasJobPermission, JOB_PERMISSIONS } from '../job-permission'

describe('job management permissions', () => {
  it('allows platform administrators without explicit grants', () => {
    expect(hasJobPermission({ isAdmin: true }, null, JOB_PERMISSIONS.configDangerous)).toBe(true)
  })

  it('combines login permissions and current route buttons', () => {
    const userStore = {
      isAdmin: false,
      permissions: [JOB_PERMISSIONS.configEdit],
    }
    const route = {
      meta: {
        btns: [{ code: JOB_PERMISSIONS.logDetail, name: '日志详情' }],
      },
    }

    expect(hasJobPermission(userStore, route, JOB_PERMISSIONS.configEdit)).toBe(true)
    expect(hasJobPermission(userStore, route, JOB_PERMISSIONS.logDetail)).toBe(true)
    expect(hasJobPermission(userStore, route, JOB_PERMISSIONS.logClean)).toBe(false)
  })

  it('does not treat API URL patterns as button permissions', () => {
    const userStore = {
      isAdmin: false,
      permissions: [],
      apiPermissions: ['GET /job/config/**'],
    }

    expect(hasJobPermission(userStore, null, JOB_PERMISSIONS.configEdit)).toBe(false)
  })
})

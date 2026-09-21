import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { isApplicationPortalPath } from '@/router/guards/permission-guard'

function source(relativePath) {
  return readFileSync(resolve(relativePath), 'utf8')
}

describe('application portal first paint', () => {
  it('treats published /app routes as portals without matching app-center', () => {
    expect(isApplicationPortalPath('/app/BuySale')).toBe(true)
    expect(isApplicationPortalPath('/app-center')).toBe(false)
    expect(isApplicationPortalPath('/app-center/application/BuySale/runtime')).toBe(false)
  })

  it('skips the global loading bar and uses one portal skeleton while the runtime hydrates', () => {
    const appSource = source('src/App.vue')
    const guardSource = source('src/router/guards/page-loading-guard.js')
    const portalSource = source('src/views/app-center/application-portal.vue')

    expect(guardSource).toContain('route?.meta?.layout === \'app-portal\'')
    expect(appSource).toContain('isApplicationPortalPath(route.path)')
    expect(appSource).toContain('<ApplicationPortalSkeleton />')
    expect(portalSource).toContain('const loading = ref(true)')
    expect(portalSource).toContain('<ApplicationPortalSkeleton v-if="loading" />')
    expect(portalSource).not.toContain('n-spin :show="loading"')
  })
})

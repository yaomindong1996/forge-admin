import { describe, expect, it } from 'vitest'
import { normalizePortalConfig } from '../components/portal/portal-config'
import {
  resolvePortalShellChrome,
  resolvePortalShellLayout,
  syncPortalShellNavigationFields,
} from '../components/portal/portal-shell-layouts'

describe('portal shell layouts', () => {
  it('maps legacy navigation.style to shellLayout', () => {
    expect(resolvePortalShellLayout({ navigation: { style: 'side' } })).toBe('normal')
    expect(resolvePortalShellLayout({ navigation: { style: 'top' } })).toBe('top-menu')
    expect(resolvePortalShellLayout({ navigation: { style: 'collapsed' } })).toBe('bento')
  })

  it('prefers explicit shellLayout over legacy style', () => {
    expect(resolvePortalShellLayout({
      shellLayout: 'immersive',
      navigation: { style: 'side' },
    })).toBe('immersive')
  })

  it('resolves chrome for top-menu / immersive / workbench', () => {
    expect(resolvePortalShellChrome({ shellLayout: 'top-menu' })).toMatchObject({
      showTopNav: true,
      showPersistentSidebar: false,
      navigationStyle: 'top',
    })
    expect(resolvePortalShellChrome({ shellLayout: 'immersive' })).toMatchObject({
      showTopNav: false,
      showPersistentSidebar: false,
      showDrawerToggle: true,
    })
    expect(resolvePortalShellChrome({ shellLayout: 'business-workbench' })).toMatchObject({
      showPersistentSidebar: true,
      shellClass: 'shell-workbench',
    })
    expect(resolvePortalShellChrome({ shellLayout: 'side-flyout' })).toMatchObject({
      showPersistentSidebar: true,
      showTopNav: false,
      shellClass: 'shell-side-flyout',
    })
  })

  it('normalizes portal config with shellLayout and synced navigation.style', () => {
    const normalized = normalizePortalConfig({ shellLayout: 'top-menu' })
    expect(normalized.shellLayout).toBe('top-menu')
    expect(normalized.navigation.style).toBe('top')

    const legacy = normalizePortalConfig({ navigation: { style: 'collapsed' } })
    expect(legacy.shellLayout).toBe('bento')
    expect(legacy.navigation.style).toBe('collapsed')
  })

  it('syncs navigation.style when saving shellLayout', () => {
    const synced = syncPortalShellNavigationFields({
      shellLayout: 'business-workbench',
      navigation: { style: 'top', showLogo: true },
    })
    expect(synced.shellLayout).toBe('business-workbench')
    expect(synced.navigation.style).toBe('side')
    expect(synced.navigation.showLogo).toBe(true)
  })
})

<script>
import { HOME_PAGE } from '@/utils/route'
import { setupDebugConsole } from '@/utils/debug-console'
import { loadRuntimeCryptoConfig } from '@/utils/crypto/crypto-config'
import { isWeComBrowser, startWeComAutoLogin, consumeWeComLoginRedirect } from '@/utils/wecom'
import { ensureLaunchRouteAccess, setupNavigationGuard } from '@/utils/navigation-guard'

function hideNativeTabBar() {
  if (typeof uni === 'undefined' || typeof uni.hideTabBar !== 'function') {
    return
  }
  uni.hideTabBar({
    animation: false,
    fail: () => {},
  })
}

// 企业微信客户端内自动免登：授权跳转或回调换票完成后进入深链目标页或首页
async function bootstrapWeComAutoLogin() {
  if (!isWeComBrowser()) {
    return { status: 'skip', reason: 'not-wecom' }
  }
  const result = await startWeComAutoLogin()
  if (result?.status === 'logged-in') {
    const redirect = consumeWeComLoginRedirect()
    const url = redirect || HOME_PAGE
    uni.reLaunch({
      url,
      fail: () => {
        uni.reLaunch({ url: HOME_PAGE, fail: () => {} })
      },
    })
  }
  return result
}

export default {
  onLaunch: async function () {
    // 优先加载页内调试面板（?vdebug=1 开启），确保后续 console 可见
    await setupDebugConsole()
    await loadRuntimeCryptoConfig()
    setupNavigationGuard()
    hideNativeTabBar()
    const wecomResult = await bootstrapWeComAutoLogin()
    if (wecomResult?.status === 'redirecting') {
      return
    }
    if (wecomResult?.status === 'logged-in') {
      return
    }
    const launchAllowed = await ensureLaunchRouteAccess()
    if (launchAllowed === false) {
      return
    }
    console.log('App Launch')
  },
  onShow: function () {
    hideNativeTabBar()
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  },
}
</script>

<style lang="scss">
@import "uview-plus/index.scss";
@import "@/styles/theme.css";
@import "@/styles/global.css";

/*每个页面公共css */
</style>

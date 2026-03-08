import { router } from '@/router'
import hasPermi from './modules/hasPermi'
import loadingDirective, { loadingService } from './modules/loading'
import copyDirective from './modules/copy'
import previewDirective from './modules/preview'
import watermarkDirective from './modules/watermark'

const permission = {
  mounted(el, binding) {
    const currentRoute = unref(router.currentRoute)
    // 修复可选链语法问题
    const btns = (currentRoute.meta && currentRoute.meta.btns && currentRoute.meta.btns.map(item => item.code)) || []
    if (!btns.includes(binding.value)) {
      el.remove()
    }
  },
}

export function setupDirectives(app) {
  app.directive('permission', permission)
  // 注册新的权限指令
  app.directive('hasPermi', hasPermi)
  // 注册loading指令
  app.directive('loading', loadingDirective)
  // 注册复制指令
  app.directive('copy', copyDirective)
  // 注册图片预览指令
  app.directive('preview', previewDirective)
  // 注册水印指令
  app.directive('watermark', watermarkDirective)

  // 将loading服务添加到全局属性
  app.config.globalProperties.$loading = loadingService
}

// 导出loading服务，方便按需导入
export { loadingService }

/** Components for loginPage Options shell. */
import SlideVerify from 'vue3-slide-verify'
import CommunitySupportDialog from '@/components/common/CommunitySupportDialog.vue'
import LoginBrandPanel from './components/LoginBrandPanel.vue'

// Options API 模板只认 components；setup 里 import 的 SlideVerify 不会自动解析。
export const loginPageLocalComponents = {
  SlideVerify,
  CommunitySupportDialog,
  LoginBrandPanel,
}

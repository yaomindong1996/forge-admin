import { request } from '@/utils'

export default {
  // 登录接口 - 使用新的统一登录入口
  login: (data) => request.post('/auth/login', data, { needToken: false }),

  // 获取图文验证码
  getCaptcha: () => request.get('/auth/captcha', { needToken: false }),

  // 获取滑块验证码
  getSliderCaptcha: () => request.get('/auth/captcha/slider', { needToken: false }),

  // 发送短信验证码
  sendSmsCaptcha: (phone) => request.post('/auth/captcha/sms', null, {
    needToken: false,
    params: { phone }
  }),

  // 获取登录配置
  getLoginConfig: () => request.get('/auth/loginConfig', { needToken: false }),

  // 获取用户信息
  getUser: () => request.get('/auth/userInfo'),
}

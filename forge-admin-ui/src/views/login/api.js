import { request } from '@/utils'

export default {
  // 登录接口 - 使用新的统一登录入口
  login: data => request.post('/auth/login', data, { needToken: false, needTip: false }),

  // 获取图文验证码
  getCaptcha: () => request.get('/auth/captcha', { needToken: false }),

  // 获取滑块验证码
  getSliderCaptcha: () => request.get('/auth/captcha/slider', { needToken: false }),

  // 发送短信验证码
  sendSmsCaptcha: phone => request.post('/auth/captcha/sms', null, {
    needToken: false,
    params: { phone },
  }),

  sendResetPasswordCode: data => request.post('/auth/resetPassword/code', data, {
    needToken: false,
    needTip: true,
  }),

  resetPassword: data => request.post('/auth/resetPassword', data, {
    needToken: false,
    needTip: true,
  }),

  // 获取登录配置
  getLoginConfig: (userClient, tenantId) => request.get('/auth/loginConfig', {
    needToken: false,
    params: {
      ...(userClient ? { userClient } : {}),
      ...(tenantId ? { tenantId } : {}),
    },
  }),

  // 登录页交流群看板（服务端代理官网看板，失败时回退本地配置）
  getGroupQrcodeBoard: () => request.get('/auth/groupQrcodeBoard', { needToken: false, needTip: false }),

  // 获取登录页可选租户
  getLoginTenantOptions: () => request.get('/auth/tenant/options', { needToken: false }),

  // 获取用户信息
  getUser: () => request.get('/auth/userInfo'),

  // 获取已启用的三方登录平台列表
  getSocialPlatforms: tenantId => request.get('/social/platforms', {
    needToken: false,
    params: tenantId ? { tenantId } : {},
  }),

  getGiteeCommunityLogin: () => request.get('/social/gitee-community', { needToken: false }),

  // 获取三方登录授权链接
  getSocialAuthUrl: (platform, tenantId) => request.get(`/social/authUrl/${platform}`, {
    needToken: false,
    params: tenantId ? { tenantId } : {},
  }),

  // 三方登录回调处理
  socialCallback: data => request.post('/social/callback', data, { needToken: false }),

  // 企业微信PC客户端工作台免登：获取平台OAuth2授权地址
  getWecomAuthorize: params => request.get('/collaboration/login/authorize', {
    needToken: false,
    params,
  }),

  // 工作台免登发现：查询平台是否开启免登及其 connectionCode（替代前端写死配置）
  getWecomSsoConnection: platform => request.get('/collaboration/login/sso-connection', {
    needToken: false,
    params: platform ? { platform } : {},
  }),

  // 企业微信免登回调：code&state 换取一次性登录票据
  wecomLoginCallback: data => request.post('/collaboration/login/callback', data, { needToken: false }),
}

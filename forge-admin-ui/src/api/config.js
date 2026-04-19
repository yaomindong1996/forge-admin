import { request } from '@/utils/http'

// 获取指定分组的配置
export function getConfigByGroup(groupCode) {
  return request({
    url: `/api/config/group/byCode/${groupCode}`,
    method: 'get',
  })
}

// 更新指定分组的配置
export function updateConfigByGroup(groupCode, data) {
  return request({
    url: `/api/config/group`,
    method: 'put',
    data: {
      groupCode,
      configValue: JSON.stringify(data),
    },
  })
}

// 刷新配置
export function refreshConfig() {
  return request({
    url: `/api/config/manage/refresh`,
    method: 'post',
  })
}

// 以下是针对特定配置类型的便捷方法

// 登录配置
export function getLoginConfig() {
  return request({
    url: `/api/config/manage/login`,
    method: 'get',
  })
}

export function updateLoginConfig(data) {
  return request({
    url: `/api/config/manage/login`,
    method: 'put',
    data,
  })
}

// 水印配置
export function getWatermarkConfig() {
  return request({
    url: `/api/config/manage/watermark`,
    method: 'get',
  })
}

export function updateWatermarkConfig(data) {
  return request({
    url: `/api/config/manage/watermark`,
    method: 'put',
    data,
  })
}

// 安全配置
export function getSecurityConfig() {
  return request({
    url: `/api/config/manage/security`,
    method: 'get',
  })
}

export function updateSecurityConfig(data) {
  return request({
    url: `/api/config/manage/security`,
    method: 'put',
    data,
  })
}

// 加解密配置
export function getCryptoConfig() {
  return request({
    url: `/api/config/manage/crypto`,
    method: 'get',
  })
}

export function updateCryptoConfig(data) {
  return request({
    url: `/api/config/manage/crypto`,
    method: 'put',
    data,
  })
}

// 认证配置
export function getAuthConfig() {
  return request({
    url: `/api/config/manage/auth`,
    method: 'get',
  })
}

export function updateAuthConfig(data) {
  return request({
    url: `/api/config/manage/auth`,
    method: 'put',
    data,
  })
}

// 日志配置
export function getLogConfig() {
  return request({
    url: `/api/config/manage/log`,
    method: 'get',
  })
}

export function updateLogConfig(data) {
  return request({
    url: `/api/config/manage/log`,
    method: 'put',
    data,
  })
}
// 系统配置
export function getSystemConfig() {
  return request({
    url: `/api/config/manage/system`,
    method: 'get',
  })
}

export function updateSystemConfig(data) {
  return request({
    url: `/api/config/manage/system`,
    method: 'put',
    data,
  })
}

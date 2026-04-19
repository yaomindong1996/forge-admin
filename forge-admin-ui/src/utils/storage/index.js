import { createStorage } from './storage'

// 使用环境变量中的TENANT作为前缀，确保不同系统的存储不会冲突
const tenant = import.meta.env.VITE_TENANT || 'default'
const prefixKey = `vue-naive-admin_${tenant}_`

export function createLocalStorage(option = {}) {
  return createStorage({
    prefixKey: option.prefixKey || '',
    storage: localStorage,
  })
}

export function createSessionStorage(option = {}) {
  return createStorage({
    prefixKey: option.prefixKey || '',
    storage: sessionStorage,
  })
}

export const lStorage = createLocalStorage({ prefixKey })

export const sStorage = createSessionStorage({ prefixKey })

// 为了兼容性，导出直接的sessionStorage和localStorage操作函数
export function getSessionStorage(key) {
  try {
    const item = sessionStorage.getItem(`${prefixKey}${key}`)
    return item ? JSON.parse(item) : null
  }
  catch (error) {
    console.error(error)
    return null
  }
}

export function setSessionStorage(key, value) {
  try {
    sessionStorage.setItem(`${prefixKey}${key}`, JSON.stringify(value))
  }
  catch (error) {
    console.error(error)
  }
}

export function getLocalStorage(key) {
  try {
    const item = localStorage.getItem(`${prefixKey}${key}`)
    return item ? JSON.parse(item) : null
  }
  catch (error) {
    console.error(error)
    return null
  }
}

export function setLocalStorage(key, value) {
  try {
    localStorage.setItem(`${prefixKey}${key}`, JSON.stringify(value))
  }
  catch (error) {
    console.error(error)
  }
}

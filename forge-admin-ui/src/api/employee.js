/**
 * 员工信息表 API
 * 生成日期: 2026-04-26
 */
import { request } from '@/utils'

const BASE = '/employee'

/** 分页查询 */
export function getEmployeePage(params) {
  return request.get(`${BASE}/page`, { params })
}

/** 查询详情 */
export function getEmployeeDetail(id) {
  return request.post(`${BASE}/getById`, null, { params: { id } })
}

/** 新增 */
export function createEmployee(data) {
  return request.post(`${BASE}/add`, data)
}

/** 更新 */
export function updateEmployee(data) {
  return request.post(`${BASE}/edit`, data)
}

/** 删除 */
export function removeEmployee(id) {
  return request.post(`${BASE}/remove`, null, { params: { id } })
}

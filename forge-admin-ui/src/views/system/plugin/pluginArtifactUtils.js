// 共享纯JSON解析器已使用TextEncoder，同时用于CLI和浏览器，拒绝重复/保留键。
import { parseStrictJson } from '../../../../../scripts/forge-plugin/json.mjs'

const fields = [
  'protocolVersion',
  'taskId',
  'reviewId',
  'revision',
  'serverResultSha256',
  'releaseId',
  'manifestSha256',
  'repositoryId',
  'resultSha256',
  'pluginId',
  'pluginVersion',
  'coreVersion',
  'operation',
  'result',
]
const reportFields = [
  'success',
  'jobId',
  'packageSha256',
  'sourceCommit',
  'image',
  'phase',
  'failureCode',
  'sourceSha256',
  'artifactCount',
  'artifactBytes',
  'artifactManifestSha256',
]

function exact(value, allowed) {
  if (!value || typeof value !== 'object' || Array.isArray(value)
    || Object.keys(value).length !== allowed.length || allowed.some(key => !(key in value))) {
    throw new Error('登记文件字段不完整或包含未知字段')
  }
}

export function parseArtifactMetadata(text, task) {
  const value = parseStrictJson(text, 65536)
  exact(value, fields)
  exact(value.result, reportFields)
  if (value.protocolVersion !== 1 || !Number.isInteger(value.revision) || value.revision < 1
    || value.taskId !== task.id || value.revision !== task.revision
    || value.pluginId !== task.pluginId || value.pluginVersion !== task.version
    || value.operation !== task.operation || value.coreVersion !== task.preview?.coreVersion
    || value.serverResultSha256 !== task.execution?.resultSha256
    || value.releaseId !== `rel-${value.manifestSha256}`
    || typeof value.manifestSha256 !== 'string' || !/^[a-f0-9]{64}$/.test(value.manifestSha256)
    || typeof value.repositoryId !== 'string' || !/^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId)
    || typeof value.resultSha256 !== 'string' || !/^[a-f0-9]{64}$/.test(value.resultSha256)
    || !task.reviews?.some(review => review.id === value.reviewId && review.decision === 'approve_build')
    || value.result.success !== true || value.result.failureCode !== null) {
    throw new Error('登记元数据与当前任务/审批不一致，请刷新任务后重新导出')
  }
  for (const key of reportFields) {
    if (value.result[key] !== task.execution?.result?.[key])
      throw new Error('候选制品与已审查的构建报告不一致')
  }
  return value
}

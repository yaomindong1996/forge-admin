// PC 与移动端共用的展示规则，不包含 Node API，避免各端对“未知版本”的含义不一致。
export function localBuildInfo() {
  return typeof __FORGE_BUILD_INFO__ === 'undefined' ? {} : __FORGE_BUILD_INFO__
}

export function versionResponse(response) {
  const data = response?.data
  if (response?.code !== 200 || !data || typeof data !== 'object' || Array.isArray(data)
    || !('version' in data || 'coreVersion' in data)) {
    throw new Error('版本接口响应无效')
  }
  const { version, coreVersion, edition, build } = response.data
  // 只保留允许展示/复制的字段，避免未来接口扩展时意外复制敏感配置。
  const text = value => typeof value === 'string' && value.trim() ? value.trim() : null
  return {
    version: text(version), coreVersion: text(coreVersion), edition: text(edition),
    build: build ? { service: text(build.service), time: text(build.time), commit: text(build.commit) } : null,
  }
}

export function buildTime(value) {
  if (!value) return '未提供'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未提供'
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function versionWarning(local, backend) {
  if (!local?.version || !backend?.version) return ''
  if (local.version !== backend.version) return '前后端版本不一致，请联系管理员核对部署版本。'
  const front = local.commit?.toLowerCase()
  const back = backend.build?.commit?.toLowerCase()
  if (front && back && !front.startsWith(back) && !back.startsWith(front)) {
    return '版本号相同，但前后端源码提交不同，请管理员核对构建批次。'
  }
  return ''
}

export function versionSections(local, backend) {
  return [
    {
      title: '后端运行信息',
      rows: [
        { label: '发行版本', value: backend ? (backend.version || '未生成构建信息') : '未获取' },
        { label: '框架核心', value: backend?.coreVersion || '未获取' },
        { label: '服务', value: backend?.build?.service || '未提供' },
        { label: '构建时间', value: buildTime(backend?.build?.time) },
        { label: '源码提交', value: backend?.build?.commit || '未提供' },
      ],
    },
    {
      title: '当前前端',
      rows: [
        { label: '发行版本', value: local?.version || '未生成构建信息' },
        { label: '客户端', value: local?.client || '未提供' },
        { label: '构建时间', value: buildTime(local?.builtAt) },
        { label: '源码提交', value: local?.commit || '未提供' },
      ],
    },
  ]
}

export function versionDiagnostics(local, backend) {
  return versionSections(local, backend)
    .map(section => `${section.title}\n${section.rows.map(row => `${row.label}：${row.value}`).join('\n')}`)
    .join('\n\n')
}

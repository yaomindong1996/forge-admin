export const DOWNLOAD_EVENT = 'forge-download-state'
let taskSequence = 0

/** 只传输技术状态与文件 ID，不广播 Token、签名地址或后端异常正文。 */
export function createDownloadObserver({ scope, isCurrent, fileId, fileName }) {
  const id = `${Date.now()}-${++taskSequence}`
  const startedAt = Date.now()
  const publish = (phase, progress = {}) => {
    if (!scope || !isCurrent())
      return
    window.dispatchEvent(new CustomEvent(DOWNLOAD_EVENT, {
      detail: { id, scope, fileId, fileName: fileName || '未命名文件', startedAt, phase, ...progress },
    }))
  }
  publish('receiving')
  return {
    isCurrent,
    progress: progress => publish('receiving', progress),
    saved: () => publish('saved'),
    failed: () => publish('failed'),
  }
}

/** 无 Content-Length 不推算百分比；无流接口时保留浏览器原生 Blob 接收路径。 */
export async function receiveDownloadBlob(response, onProgress) {
  if (!onProgress || !response.body?.getReader)
    return response.blob()
  const declared = Number(response.headers.get('Content-Length'))
  const encoding = response.headers.get('Content-Encoding')
  // 流读取的是解压后字节，压缩响应的 Content-Length 不能作为进度分母。
  const total = (!encoding || encoding === 'identity') && Number.isFinite(declared) && declared > 0 ? declared : null
  const reader = response.body.getReader()
  const chunks = []
  let loaded = 0
  let lastUpdate = 0
  try {
    while (true) {
      const chunk = await reader.read()
      if (chunk.done)
        break
      chunks.push(chunk.value)
      loaded += chunk.value.byteLength
      if (Date.now() - lastUpdate >= 100) {
        onProgress({ loaded, total })
        lastUpdate = Date.now()
      }
    }
    onProgress({ loaded, total })
    return new Blob(chunks, { type: response.headers.get('Content-Type') || 'application/octet-stream' })
  }
  finally {
    reader.releaseLock()
  }
}

import { resolveRenderableFileUrl } from '@/utils/file'
import { toast } from '@/utils/notify'

// 附件下载接口需要 Bearer Token，不能直接把下载地址交给浏览器；先带鉴权取成 blob 再触发下载。
export async function openNoticeAttachment(attachment = {}) {
  const fileId = attachment.fileId || attachment.fileUrl
  if (!fileId) {
    toast('附件地址无效', { type: 'error' })
    return
  }
  try {
    const url = await resolveRenderableFileUrl({ fileId: attachment.fileId, url: attachment.fileUrl })
    if (!url) throw new Error('附件地址无效')
    if (typeof document !== 'undefined') {
      const link = document.createElement('a')
      link.href = url
      link.download = attachment.fileName || ''
      link.target = '_blank'
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      link.remove()
      return
    }
    uni.downloadFile({
      url,
      success: ({ tempFilePath }) => uni.openDocument({ filePath: tempFilePath, fail: () => toast('无法打开该附件') }),
      fail: () => toast('附件下载失败', { type: 'error' }),
    })
  }
  catch (error) {
    console.error('打开公告附件失败:', error)
    toast(error?.message || '附件下载失败', { type: 'error' })
  }
}

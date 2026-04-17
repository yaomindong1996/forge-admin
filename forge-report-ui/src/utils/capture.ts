import html2canvas from 'html2canvas'
import { uploadFileApi, FileUploadResult } from '@/api/file'

export interface CaptureOptions {
  scale?: number
  backgroundColor?: string
  logging?: boolean
  useCORS?: boolean
  allowTaint?: boolean
}

export const captureCanvasScreenshot = async (
  element: HTMLElement,
  options: CaptureOptions = {}
): Promise<FileUploadResult | null> => {
  try {
    console.log('[Capture] 开始截图, 元素尺寸:', element.offsetWidth, 'x', element.offsetHeight)

    const {
      scale = 1,
      backgroundColor = '#ffffff',
      logging = false,
      useCORS = true,
      allowTaint = false
    } = options

    const canvas = await html2canvas(element, {
      scale,
      backgroundColor,
      logging,
      useCORS,
      allowTaint,
      imageTimeout: 15000,
      removeContainer: true
    })

    console.log('[Capture] Canvas生成成功, 尺寸:', canvas.width, 'x', canvas.height)

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob)
          } else {
            reject(new Error('Canvas toBlob failed'))
          }
        },
        'image/png',
        0.9
      )
    })

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
    const fileName = `screenshot-${timestamp}.png`
    const file = new File([blob], fileName, { type: 'image/png' })

    console.log('[Capture] 文件生成成功, 名称:', fileName, '大小:', file.size, 'bytes')

    const res = await uploadFileApi(file, 'project_screenshot')
    console.log('[Capture] 上传结果:', res)

    if (res.code === 200 && res.data) {
      return res.data
    }
    console.warn('[Capture] 上传失败, 响应:', res)
    return null
  } catch (error) {
    console.error('[Capture] 截图生成失败:', error)
    return null
  }
}

export const captureProjectScreenshot = async (
  projectId: string | number,
  canvasElement: HTMLElement
): Promise<string | null> => {
  console.log('[CaptureProject] 开始项目截图, projectId:', projectId)
  const result = await captureCanvasScreenshot(canvasElement, {
    scale: 0.5,
    backgroundColor: undefined
  })
  if (result) {
    // 如果 accessUrl 存在且有效，直接使用
    if (result.accessUrl) {
      let accessUrl = result.accessUrl
      if (accessUrl.startsWith('/api/') && !accessUrl.startsWith('/goview-api/')) {
        accessUrl = '/goview-api' + accessUrl
      }
      console.log('[CaptureProject] 截图URL:', accessUrl)
      return accessUrl
    }
    // 本地存储返回 null 时，根据 fileId 构造下载 URL
    if (result.fileId) {
      const accessUrl = `/goview-api/api/file/download/${result.fileId}`
      console.log('[CaptureProject] 构造截图URL:', accessUrl)
      return accessUrl
    }
  }
  console.warn('[CaptureProject] 截图失败或无URL')
  return null
}

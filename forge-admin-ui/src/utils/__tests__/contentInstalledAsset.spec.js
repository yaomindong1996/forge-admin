import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/contentAsset', () => ({
  installContentAsset: vi.fn(),
  contentAssetDetail: vi.fn(),
}))

import { contentAssetDetail, installContentAsset } from '@/api/contentAsset'
import { installContentPackageFromFile, loadInstalledContentPackage } from '../contentInstalledAsset'

describe('content installed asset helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('installs market package into tenant library', async () => {
    installContentAsset.mockResolvedValue({
      data: { id: 9, kind: 'print', assetKey: 'leave-slip', assetName: '请假审批单' },
    })
    const body = JSON.stringify({
      kind: 'print',
      key: 'leave-slip',
      name: '请假审批单',
      version: '1.1.0',
      payload: { protocol: 'forge-print' },
    })
    const file = {
      name: 'leave-slip.print.json',
      text: async () => body,
    }

    const result = await installContentPackageFromFile({ file, kind: 'print' })

    expect(installContentAsset).toHaveBeenCalledWith(expect.objectContaining({
      kind: 'print',
      assetKey: 'leave-slip',
      assetName: '请假审批单',
      assetVersion: '1.1.0',
      sourceFileName: 'leave-slip.print.json',
    }))
    expect(result.pack.payload).toEqual({ protocol: 'forge-print' })
    expect(result.asset.id).toBe(9)
  })

  it('loads installed package for reuse', async () => {
    contentAssetDetail.mockResolvedValue({
      data: {
        id: 3,
        kind: 'form',
        assetKey: 'leave-form',
        assetName: '请假申请表单',
        assetVersion: '1.1.0',
        packageJson: JSON.stringify({
          kind: 'form',
          name: '请假申请表单',
          payload: { formKey: 'leave' },
        }),
      },
    })

    const result = await loadInstalledContentPackage(3)
    expect(result.pack.payload).toEqual({ formKey: 'leave' })
    expect(result.pack.name).toBe('请假申请表单')
  })
})

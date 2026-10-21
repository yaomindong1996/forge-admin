import { contentAssetDetail, installContentAsset } from '@/api/contentAsset'
import { readTextPackageFile, unwrapContentPackage, unwrapInstalledPackageJson } from '@/utils/contentPackage'

/**
 * 把本地市场 JSON 安装到当前租户「我的模板」库（同 kind+key 会覆盖更新）。
 */
export async function installContentPackageFromFile({ file, kind }) {
  const text = await readTextPackageFile(file)
  const pack = unwrapContentPackage(text)
  const resolvedKind = String(kind || pack.kind || '').trim()
  if (!resolvedKind)
    throw new Error('无法识别模板类型')
  if (pack.kind && pack.kind !== resolvedKind)
    throw new Error(`模板类型是 ${pack.kind}，与当前入口 ${resolvedKind} 不一致`)

  const name = pack.name || String(file.name || '').replace(/\.json$/i, '') || '未命名模板'
  const { data } = await installContentAsset({
    kind: resolvedKind,
    assetKey: pack.key || undefined,
    assetName: name,
    assetVersion: pack.version || undefined,
    packageJson: pack.packageJson || text,
    sourceFileName: file.name || undefined,
  })
  return {
    asset: data,
    pack: {
      ...pack,
      kind: resolvedKind,
      name,
    },
  }
}

/** 拉取已安装资产并解析出可用包 */
export async function loadInstalledContentPackage(assetId) {
  const { data } = await contentAssetDetail(assetId)
  if (!data?.packageJson)
    throw new Error('该模板包内容为空')
  const pack = unwrapInstalledPackageJson(data.packageJson)
  return {
    asset: data,
    pack: {
      ...pack,
      kind: data.kind || pack.kind,
      name: data.assetName || pack.name,
      version: data.assetVersion || pack.version,
      key: data.assetKey || pack.key,
    },
  }
}

export const CONTENT_ASSET_KIND_LABEL = {
  print: '打印模板',
  form: '表单模板',
  'print-component': '打印组件',
}

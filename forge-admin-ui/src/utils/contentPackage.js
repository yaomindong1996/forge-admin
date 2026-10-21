export async function readTextPackageFile(file) {
  if (!file)
    throw new Error('请选择要导入的模板包')
  if (/\.zip$/i.test(String(file.name || '')))
    throw new Error('请导入 JSON 模板包；Zip 请先解压')
  const text = await file.text()
  if (!String(text || '').trim())
    throw new Error('模板包是空的')
  return text
}

export function unwrapContentPackage(text) {
  const data = JSON.parse(text)
  if (!data || typeof data !== 'object')
    throw new Error('模板包格式无效')
  if (data.payload && typeof data.payload === 'object') {
    return {
      kind: String(data.kind || ''),
      key: String(data.key || data.id || ''),
      name: String(data.name || ''),
      version: String(data.version || ''),
      payload: data.payload,
      packageJson: JSON.stringify(data),
    }
  }
  return {
    kind: '',
    key: '',
    name: '',
    version: '',
    payload: data,
    packageJson: JSON.stringify({ kind: '', name: '', payload: data }),
  }
}

/** 从已安装资产的 packageJson 解析出可应用的 payload */
export function unwrapInstalledPackageJson(packageJson) {
  if (packageJson && typeof packageJson === 'object')
    return unwrapContentPackage(JSON.stringify(packageJson))
  return unwrapContentPackage(String(packageJson || ''))
}

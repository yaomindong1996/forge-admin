import { describe, expect, it } from 'vitest'
import { unwrapContentPackage, unwrapInstalledPackageJson } from '../contentPackage'

describe('content package', () => {
  it('unwraps kinded payload and raw json', () => {
    const kinded = unwrapContentPackage(JSON.stringify({
      kind: 'form',
      key: 'leave-form',
      name: '请假',
      version: '1.1.0',
      payload: { formKey: 'leave' },
    }))
    expect(kinded).toMatchObject({
      kind: 'form',
      key: 'leave-form',
      name: '请假',
      version: '1.1.0',
      payload: { formKey: 'leave' },
    })
    expect(JSON.parse(kinded.packageJson).payload).toEqual({ formKey: 'leave' })

    const raw = unwrapContentPackage('{"protocol":"forge-print"}')
    expect(raw).toMatchObject({ kind: '', name: '', payload: { protocol: 'forge-print' } })
    expect(JSON.parse(raw.packageJson).payload).toEqual({ protocol: 'forge-print' })
  })

  it('unwraps installed package json object or text', () => {
    expect(unwrapInstalledPackageJson({ kind: 'print', name: '单', payload: { a: 1 } }))
      .toMatchObject({ kind: 'print', name: '单', payload: { a: 1 } })
  })
})

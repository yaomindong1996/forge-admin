import { describe, expect, it } from 'vitest'
import {
  applyDbTypeDefaults,
  buildJdbcUrl,
  hydrateConnectionForm,
  parseJdbcUrl,
  suggestConnectionCode,
  syncAccessToJdbcUrl,
} from '../connection-form'

describe('data connection form helpers', () => {
  it('用主机、端口、库名拼出连接串，而不是让人手写 JDBC', () => {
    expect(buildJdbcUrl({
      dbType: 'MYSQL',
      host: '127.0.0.1',
      port: '3306',
      database: 'forge_admin',
    })).toContain('jdbc:mysql://127.0.0.1:3306/forge_admin')
    expect(parseJdbcUrl('jdbc:mysql://127.0.0.1:3306/forge_admin?useUnicode=true', 'MYSQL')).toMatchObject({
      host: '127.0.0.1',
      port: '3306',
      database: 'forge_admin',
      ok: true,
    })
    expect(parseJdbcUrl('jdbc:sqlserver://db.local:1433;databaseName=erp', 'SQLSERVER')).toMatchObject({
      host: 'db.local',
      database: 'erp',
      ok: true,
    })
  })

  it('切库类型时补端口和驱动，中文名称能生成编码', () => {
    const form = hydrateConnectionForm({ connectionName: '采购库' })
    applyDbTypeDefaults(form, 'POSTGRESQL')
    expect(form.driverClassName).toContain('postgresql')
    expect(form.port).toBe('5432')
    expect(form.jdbcUrl).toContain('jdbc:postgresql://')
    expect(suggestConnectionCode('采购 库')).toMatch(/^conn_/)
    expect(suggestConnectionCode('erp prod')).toBe('erp_prod')
  })

  it('改主机时保留已解析出的连接串参数', () => {
    const form = hydrateConnectionForm({
      dbType: 'MYSQL',
      jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/forge_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai',
    })
    form.host = '10.0.0.8'
    syncAccessToJdbcUrl(form)
    expect(form.jdbcUrl).toContain('jdbc:mysql://10.0.0.8:3306/forge_admin')
    expect(form.jdbcUrl).toContain('serverTimezone=Asia/Shanghai')
  })
})

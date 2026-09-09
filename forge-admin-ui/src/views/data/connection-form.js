export const DRIVER_CLASS_MAP = {
  MYSQL: 'com.mysql.cj.jdbc.Driver',
  ORACLE: 'oracle.jdbc.OracleDriver',
  POSTGRESQL: 'org.postgresql.Driver',
  SQLSERVER: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
}

export const DEFAULT_PORT_MAP = {
  MYSQL: '3306',
  ORACLE: '1521',
  POSTGRESQL: '5432',
  SQLSERVER: '1433',
}

export const DEFAULT_TEST_SQL_MAP = {
  MYSQL: 'SELECT 1',
  ORACLE: 'SELECT 1 FROM DUAL',
  POSTGRESQL: 'SELECT 1',
  SQLSERVER: 'SELECT 1',
}

const MYSQL_DEFAULT_QUERY = 'useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'

export function suggestConnectionCode(name) {
  const ascii = String(name || '')
    .trim()
    .replace(/\s+/g, '_')
    .replace(/\W/g, '')
  if (ascii && /[a-z0-9]/i.test(ascii))
    return ascii.toLowerCase().slice(0, 40)
  return `conn_${Date.now().toString().slice(-8)}`
}

export function parseJdbcUrl(jdbcUrl, dbType) {
  const url = String(jdbcUrl || '').trim()
  const type = String(dbType || '').toUpperCase()
  if (!url)
    return emptyAccess()

  if (type === 'SQLSERVER' || url.startsWith('jdbc:sqlserver:'))
    return parseSqlServerUrl(url)
  if (type === 'ORACLE' || url.startsWith('jdbc:oracle:'))
    return parseOracleUrl(url)
  return parseHostSlashUrl(url)
}

function emptyAccess() {
  return { host: '', port: '', database: '', extra: '', ok: false }
}

function parseHostSlashUrl(url) {
  const match = url.match(/^jdbc:[a-z0-9+]+:\/\/([^/:?]+)(?::(\d+))?(?:\/([^?;]*))?(?:\?([^#]*))?/i)
  if (!match)
    return emptyAccess()
  return {
    host: match[1] || '',
    port: match[2] || '',
    database: decodeURIComponent(match[3] || ''),
    extra: match[4] || '',
    ok: true,
  }
}

function parseSqlServerUrl(url) {
  const hostMatch = url.match(/^jdbc:sqlserver:\/\/([^/:;]+)(?::(\d+))?/i)
  if (!hostMatch)
    return emptyAccess()
  const databaseMatch = url.match(/databaseName=([^;]+)/i)
  return {
    host: hostMatch[1] || '',
    port: hostMatch[2] || '',
    database: databaseMatch?.[1] || '',
    extra: '',
    ok: true,
  }
}

function parseOracleUrl(url) {
  const serviceMatch = url.match(/^jdbc:oracle:thin:@\/\/([^/:]+)(?::(\d+))?\/([^?;]+)/i)
  if (serviceMatch) {
    return {
      host: serviceMatch[1] || '',
      port: serviceMatch[2] || '',
      database: serviceMatch[3] || '',
      extra: '',
      ok: true,
    }
  }
  const sidMatch = url.match(/^jdbc:oracle:thin:@([^/:]+)(?::(\d+))?(?::([^?;]+))?/i)
  if (!sidMatch)
    return emptyAccess()
  return {
    host: sidMatch[1] || '',
    port: sidMatch[2] || '',
    database: sidMatch[3] || '',
    extra: '',
    ok: true,
  }
}

export function buildJdbcUrl({ dbType, host, port, database, extra } = {}) {
  const type = String(dbType || 'MYSQL').toUpperCase()
  const safeHost = String(host || '').trim() || 'localhost'
  const safePort = String(port || DEFAULT_PORT_MAP[type] || '').trim()
  const safeDatabase = String(database || '').trim()
  const hostPart = safePort ? `${safeHost}:${safePort}` : safeHost

  if (type === 'SQLSERVER') {
    const dbPart = safeDatabase ? `;databaseName=${safeDatabase}` : ''
    return `jdbc:sqlserver://${hostPart}${dbPart}`
  }
  if (type === 'ORACLE') {
    const dbPart = safeDatabase ? `:${safeDatabase}` : ''
    return `jdbc:oracle:thin:@${hostPart}${dbPart}`
  }
  if (type === 'POSTGRESQL') {
    const dbPart = safeDatabase ? `/${encodeURIComponent(safeDatabase)}` : ''
    return `jdbc:postgresql://${hostPart}${dbPart}`
  }
  const dbPart = safeDatabase ? `/${encodeURIComponent(safeDatabase)}` : ''
  const query = extra || MYSQL_DEFAULT_QUERY
  return `jdbc:mysql://${hostPart}${dbPart}?${query}`
}

export function applyDbTypeDefaults(formData, dbType) {
  const type = String(dbType || 'MYSQL').toUpperCase()
  formData.dbType = type
  formData.driverClassName = DRIVER_CLASS_MAP[type] || formData.driverClassName
  formData.port = DEFAULT_PORT_MAP[type] || formData.port
  const currentTestSql = String(formData.testSql || '').trim()
  if (!currentTestSql || Object.values(DEFAULT_TEST_SQL_MAP).includes(currentTestSql))
    formData.testSql = DEFAULT_TEST_SQL_MAP[type] || 'SELECT 1'
  syncAccessToJdbcUrl(formData)
}

export function syncAccessToJdbcUrl(formData = {}) {
  formData.jdbcUrl = buildJdbcUrl({
    ...formData,
    extra: formData.jdbcExtra || formData.extra,
  })
  return formData.jdbcUrl
}

export function syncJdbcUrlToAccess(formData = {}) {
  const parsed = parseJdbcUrl(formData.jdbcUrl, formData.dbType)
  if (!parsed.ok)
    return parsed
  formData.host = parsed.host
  formData.port = parsed.port || DEFAULT_PORT_MAP[String(formData.dbType || '').toUpperCase()] || ''
  formData.database = parsed.database
  formData.jdbcExtra = parsed.extra
  return parsed
}

export function hydrateConnectionForm(formData = {}) {
  const next = {
    dbType: 'MYSQL',
    host: 'localhost',
    port: DEFAULT_PORT_MAP.MYSQL,
    database: '',
    username: '',
    password: '',
    status: 1,
    testSql: DEFAULT_TEST_SQL_MAP.MYSQL,
    driverClassName: DRIVER_CLASS_MAP.MYSQL,
    ...formData,
  }
  if (next.jdbcUrl)
    syncJdbcUrlToAccess(next)
  else
    syncAccessToJdbcUrl(next)
  if (!next.driverClassName)
    next.driverClassName = DRIVER_CLASS_MAP[String(next.dbType || 'MYSQL').toUpperCase()]
  return next
}

export function getJdbcEndpoint(jdbcUrl) {
  if (!jdbcUrl)
    return ''
  const match = jdbcUrl.match(/\/\/([^/?;]+)/) || jdbcUrl.match(/@\/\/?([^/:]+(?::\d+)?)/)
  return match?.[1] || jdbcUrl
}

export function getJdbcDatabaseName(jdbcUrl) {
  if (!jdbcUrl)
    return ''
  const parsed = parseJdbcUrl(jdbcUrl)
  return parsed.database || ''
}

const DEFAULT_JOB_API_BASE_URL = 'http://localhost:8580'
const DEFAULT_JOB_API_TOKEN = '$' + '{TOKEN}'
const JOB_ID_PLACEHOLDER = '$' + '{JOB_ID}'
const EXECUTION_ID_PLACEHOLDER = '$' + '{EXECUTION_ID}'

export function buildJobOpenApiExamples({
  baseUrl = DEFAULT_JOB_API_BASE_URL,
  token = DEFAULT_JOB_API_TOKEN,
  scopes,
} = {}) {
  const normalizedBaseUrl = String(baseUrl || DEFAULT_JOB_API_BASE_URL).trim().replace(/\/+$/, '')
    || DEFAULT_JOB_API_BASE_URL
  const normalizedToken = String(token || DEFAULT_JOB_API_TOKEN).trim() || DEFAULT_JOB_API_TOKEN
  const examples = [
    {
      key: 'list-jobs',
      label: '查询任务',
      description: '分页查询当前服务账号有权访问的任务摘要。',
      method: 'GET',
      path: '/openapi/v1/jobs?pageNum=1&pageSize=20',
      scope: 'jobs:read',
      command: [
        `curl --request GET "${normalizedBaseUrl}/openapi/v1/jobs?pageNum=1&pageSize=20" \\`,
        `  --header "Authorization: Bearer ${normalizedToken}"`,
      ].join('\n'),
    },
    {
      key: 'trigger-job',
      label: '触发任务',
      description: '触发一个已授权且处于启用、已同步状态的任务。',
      method: 'POST',
      path: `/openapi/v1/jobs/${JOB_ID_PLACEHOLDER}/executions`,
      scope: 'jobs:trigger',
      command: [
        `curl --request POST "${normalizedBaseUrl}/openapi/v1/jobs/${JOB_ID_PLACEHOLDER}/executions" \\`,
        `  --header "Authorization: Bearer ${normalizedToken}" \\`,
        '  --header "Idempotency-Key: $(uuidgen)"',
      ].join('\n'),
    },
    {
      key: 'execution-detail',
      label: '查询执行结果',
      description: '根据触发接口返回的执行 ID 查询任务执行状态。',
      method: 'GET',
      path: `/openapi/v1/executions/${EXECUTION_ID_PLACEHOLDER}`,
      scope: 'executions:read',
      command: [
        `curl --request GET "${normalizedBaseUrl}/openapi/v1/executions/${EXECUTION_ID_PLACEHOLDER}" \\`,
        `  --header "Authorization: Bearer ${normalizedToken}"`,
      ].join('\n'),
    },
  ]

  if (!Array.isArray(scopes))
    return examples

  const grantedScopes = new Set(scopes)
  return examples.filter(example => grantedScopes.has(example.scope))
}

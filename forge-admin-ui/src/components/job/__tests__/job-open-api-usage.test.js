import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { buildJobOpenApiExamples } from '../job-open-api-usage.js'
import JobOpenApiUsageGuide from '../JobOpenApiUsageGuide.vue'

describe('job open api usage guide', () => {
  it('builds copy-ready examples for the scopes granted to a token', () => {
    const jobIdPlaceholder = '$' + '{JOB_ID}'
    const examples = buildJobOpenApiExamples({
      baseUrl: 'https://forge.example.com/',
      token: 'fja_test_token',
      scopes: ['jobs:read', 'jobs:trigger'],
    })

    expect(examples.map(item => item.key)).toEqual(['list-jobs', 'trigger-job'])
    expect(examples[0].command).toContain('https://forge.example.com/openapi/v1/jobs?pageNum=1&pageSize=20')
    expect(examples[0].command).toContain('Authorization: Bearer fja_test_token')
    expect(examples[1].command).toContain(`/openapi/v1/jobs/${jobIdPlaceholder}/executions`)
    expect(examples[1].command).toContain('Idempotency-Key: $(uuidgen)')
    expect(examples[1].command).toContain('Authorization: Bearer fja_test_token')
    expect(examples.every(item => !item.command.includes('\n+'))).toBe(true)
  })

  it('returns the complete generic guide when no token scopes are supplied', () => {
    const tokenPlaceholder = '$' + '{TOKEN}'
    const examples = buildJobOpenApiExamples()

    expect(examples.map(item => item.scope)).toEqual([
      'jobs:read',
      'jobs:trigger',
      'executions:read',
    ])
    expect(examples.every(item => item.command.includes(`Authorization: Bearer ${tokenPlaceholder}`))).toBe(true)
  })

  it('explains how to provide the token in the persistent guide', () => {
    const wrapper = mount(JobOpenApiUsageGuide)

    expect(wrapper.text()).toContain('export TOKEN="创建或轮换时保存的明文 Token"')
    expect(wrapper.text()).toContain('查询任务')
    expect(wrapper.text()).toContain('查询执行结果')
    wrapper.unmount()
  })

  it('shows only granted scopes and refreshes curl after the base URL changes', async () => {
    const wrapper = mount(JobOpenApiUsageGuide, {
      props: {
        token: 'fja_test_token',
        scopes: ['jobs:trigger'],
        compact: true,
      },
    })

    expect(wrapper.text()).toContain('触发任务')
    expect(wrapper.text()).not.toContain('查询任务')
    expect(wrapper.find('pre').text()).toContain('Authorization: Bearer fja_test_token')
    expect(wrapper.find('pre').text()).toContain('Idempotency-Key: $(uuidgen)')

    await wrapper.find('input').setValue('https://forge.example.com/')

    const jobIdPlaceholder = '$' + '{JOB_ID}'
    expect(wrapper.find('pre').text())
      .toContain(`https://forge.example.com/openapi/v1/jobs/${jobIdPlaceholder}/executions`)
    wrapper.unmount()
  })
})

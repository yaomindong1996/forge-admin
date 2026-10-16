import { fileURLToPath } from 'node:url'

// H5 的其它用例使用 node:test；这份已有版本用例使用 Vitest，单独执行，不混用运行器。
const root = fileURLToPath(new URL('../../../../forge-h5-ui/', import.meta.url))
const admin = new URL('../../../../forge-admin-ui/', import.meta.url)

export default {
  root,
  resolve: {
    alias: {
      '@': `${root}src`,
      vitest: fileURLToPath(new URL('node_modules/vitest/dist/index.js', admin)),
    },
  },
  test: {
    environment: 'node',
    include: ['src/store/modules/__tests__/version.spec.js'],
    maxWorkers: 1,
    minWorkers: 1,
  },
}

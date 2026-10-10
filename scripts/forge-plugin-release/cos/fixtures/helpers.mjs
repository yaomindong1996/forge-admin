import { Readable } from 'node:stream'

// 仅隔离测试使用；fixtures目录不进入生成工程，正式执行器始终创建腾讯官方 SDK 实例。
export function memoryClient() {
  const objects = new Map()
  const puts = []
  const acl = { ACL: 'private', Grants: [{ Grantee: { ID: 'owner' }, Permission: 'FULL_CONTROL' }] }
  const client = {
    getBucketAcl: async () => acl,
    getObjectAcl: async () => acl,
    getBucketPolicy: async () => { throw { statusCode: 404 } },
    getBucketVersioning: async () => ({}),
    headObject: async ({ Key }) => {
      if (!objects.has(Key)) throw { statusCode: 404 }
      return { headers: { 'content-length': String(objects.get(Key).length) } }
    },
    putObject: async ({ Key, Body, Headers, ACL }) => {
      puts.push({ Key, Headers, ACL })
      if (objects.has(Key)) throw { statusCode: 409 }
      if (Buffer.isBuffer(Body)) objects.set(Key, Buffer.from(Body))
      else {
        const chunks = []
        for await (const chunk of Body) chunks.push(Buffer.from(chunk))
        objects.set(Key, Buffer.concat(chunks))
      }
      return {}
    },
    getObject: async ({ Key, Output }) => {
      if (!objects.has(Key)) throw { statusCode: 404 }
      await new Promise((resolve, reject) => {
        Output.on('finish', resolve).on('error', reject)
        Readable.from([objects.get(Key)]).pipe(Output)
      })
      return {}
    },
  }
  return { client, objects, puts, acl }
}

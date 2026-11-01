import {
  buildSystemClientEditSchema,
  createClientCodeAutoCode,
  resolveClientEditSchemaOptions,
  SYSTEM_CLIENT_EDIT_DICT_TYPES,
} from '@/views/system/schemas/systemClientEditSchema'
import { resolveCreatedClientRecord } from '../resolveCreatedRecord'
import { resolveDefaultsFromForm } from '../resolveDefaults'
import { submitQuickCreateApi } from '../submitApi'
import { SYSTEM_EDIT_FORM_LAYOUT } from '@/views/system/schemas/systemEditLayout'

const clientCodeAuto = createClientCodeAutoCode()

function normalizeClientPayload(payload = {}) {
  const next = { ...payload }
  if (Array.isArray(next.authTypes))
    next.authTypes = next.authTypes.join(',')
  return next
}

export function createSystemClientQuickCreatePreset() {
  return {
    key: 'systemClient',
    // 后端 create 仅平台管理员；无独立 menu perms 时靠 isAdmin 放行
    permission: 'system:client:add',
    linkLabel: '新增客户端',
    modalTitle: '新增客户端',
    submitApi: 'post@/system/client',
    valueField: 'clientCode',
    labelField: 'clientName',
    formLayout: { ...SYSTEM_EDIT_FORM_LAYOUT },
    editSchemaDictTypes: SYSTEM_CLIENT_EDIT_DICT_TYPES,
    buildSchema(ctx = {}) {
      return buildSystemClientEditSchema({
        clientCodeAuto,
        ...resolveClientEditSchemaOptions(ctx.dict),
        withFieldQuickCreate: false,
      })
    },
    buildInitialValues(ctx = {}) {
      clientCodeAuto.reset?.()
      const { parentFormData, config } = ctx
      const fromForm = resolveDefaultsFromForm(config?.defaultsFromForm, parentFormData)
      return {
        clientAuthMethod: 'client_secret',
        tokenTimeout: 7200,
        tokenActivityTimeout: -1,
        concurrentLogin: false,
        shareToken: false,
        status: 1,
        captchaType: '',
        ...fromForm,
      }
    },
    beforeOpen(ctx = {}) {
      if (!ctx.userStore?.isAdmin)
        return { ok: false, message: '仅超级管理员可维护客户端' }
      return { ok: true }
    },
    async submit(payload) {
      const body = normalizeClientPayload({
        clientAuthMethod: 'client_secret',
        tokenTimeout: 7200,
        tokenActivityTimeout: -1,
        concurrentLogin: false,
        shareToken: false,
        status: 1,
        ...payload,
      })
      await submitQuickCreateApi('post@/system/client', body)
      const record = await resolveCreatedClientRecord(body)
      return { submitted: body, record }
    },
  }
}

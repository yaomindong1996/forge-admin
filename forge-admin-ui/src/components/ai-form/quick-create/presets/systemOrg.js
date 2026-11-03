import {
  buildSystemOrgEditSchema,
  resolveOrgEditSchemaOptions,
  SYSTEM_ORG_EDIT_DICT_TYPES,
} from '@/views/system/schemas/systemOrgEditSchema'
import { SYSTEM_EDIT_FORM_LAYOUT } from '@/views/system/schemas/systemEditLayout'
import { resolveCreatedOrgRecord } from '../resolveCreatedRecord'
import { resolveDefaultsFromForm } from '../resolveDefaults'
import { submitQuickCreateApi } from '../submitApi'

export function createSystemOrgQuickCreatePreset() {
  return {
    key: 'systemOrg',
    permission: 'system:org:add',
    linkLabel: '新增组织',
    modalTitle: '新增组织',
    submitApi: 'post@/system/org/add',
    valueField: 'id',
    labelField: 'orgName',
    formLayout: { ...SYSTEM_EDIT_FORM_LAYOUT },
    editSchemaDictTypes: SYSTEM_ORG_EDIT_DICT_TYPES,
    buildSchema(ctx = {}) {
      return buildSystemOrgEditSchema({
        ...resolveOrgEditSchemaOptions(ctx.dict),
        withFieldQuickCreate: false,
      })
    },
    buildInitialValues(ctx = {}) {
      const { userStore, parentFormData, config } = ctx
      const fromForm = resolveDefaultsFromForm(config?.defaultsFromForm, parentFormData)
      return {
        parentId: fromForm.parentId ?? 0,
        orgType: '2',
        orgStatus: 1,
        sort: 0,
        tenantId: userStore?.userInfo?.tenantId ?? userStore?.tenantId,
        ...fromForm,
      }
    },
    beforeOpen() {
      return { ok: true }
    },
    async submit(payload, ctx) {
      const { userStore } = ctx
      const body = {
        parentId: 0,
        orgType: '2',
        orgStatus: 1,
        sort: 0,
        tenantId: userStore?.userInfo?.tenantId ?? userStore?.tenantId,
        ...payload,
      }
      if (body.parentId === null || body.parentId === undefined || body.parentId === '')
        body.parentId = 0
      await submitQuickCreateApi('post@/system/org/add', body)
      const record = await resolveCreatedOrgRecord(body, body.tenantId)
      return { submitted: body, record }
    },
  }
}

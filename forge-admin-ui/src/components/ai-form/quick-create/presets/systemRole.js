import {
  buildSystemRoleEditSchema,
  createRoleKeyAutoCode,
  resolveRoleEditSchemaOptions,
  SYSTEM_ROLE_EDIT_DICT_TYPES,
} from '@/views/system/schemas/systemRoleEditSchema'
import { resolveCreatedRoleRecord } from '../resolveCreatedRecord'
import { resolveDefaultsFromForm } from '../resolveDefaults'
import { submitQuickCreateApi } from '../submitApi'
import { SYSTEM_EDIT_FORM_LAYOUT } from '@/views/system/schemas/systemEditLayout'

const ROLE_ORG_SCOPE_GLOBAL = 1

const roleKeyAutoCode = createRoleKeyAutoCode()

function applyRoleQuickCreateBeforeSubmit(formData, ctx) {
  const { userStore } = ctx
  const next = { ...formData }
  if (!next.orgScopeType)
    next.orgScopeType = ROLE_ORG_SCOPE_GLOBAL
  next.tenantId = userStore?.userInfo?.tenantId ?? userStore?.tenantId
  if (!userStore?.isAdmin) {
    if (Number(userStore?.userType) === 2 && [1, 2].includes(Number(next.dataScope)))
      next.dataScope = 5
    else if (Number(next.dataScope) === 1)
      next.dataScope = 2
  }
  return next
}

export function createSystemRoleQuickCreatePreset() {
  return {
    key: 'systemRole',
    permission: 'system:role:add',
    linkLabel: '新增角色',
    modalTitle: '新增角色',
    submitApi: 'post@/system/role/add',
    valueField: 'id',
    labelField: 'roleName',
    formLayout: { ...SYSTEM_EDIT_FORM_LAYOUT },
    editSchemaDictTypes: SYSTEM_ROLE_EDIT_DICT_TYPES,
    buildSchema(ctx = {}) {
      const options = resolveRoleEditSchemaOptions(ctx.userStore, ctx.dict)
      return buildSystemRoleEditSchema({
        roleKeyAutoCode,
        ...options,
      })
    },
    buildInitialValues(ctx = {}) {
      roleKeyAutoCode.reset()
      const { userStore, parentFormData, config } = ctx
      const fromForm = resolveDefaultsFromForm(config?.defaultsFromForm, parentFormData)
      return {
        roleType: 1,
        dataScope: 2,
        sort: 0,
        roleStatus: 1,
        isSystem: 0,
        orgScopeType: ROLE_ORG_SCOPE_GLOBAL,
        tenantId: userStore?.userInfo?.tenantId ?? userStore?.tenantId,
        ...fromForm,
      }
    },
    beforeOpen() {
      return { ok: true }
    },
    async submit(payload, ctx) {
      const body = applyRoleQuickCreateBeforeSubmit({ ...payload }, ctx)
      await submitQuickCreateApi('post@/system/role/add', body)
      const record = await resolveCreatedRoleRecord(body, body.tenantId)
      return { submitted: body, record }
    },
  }
}

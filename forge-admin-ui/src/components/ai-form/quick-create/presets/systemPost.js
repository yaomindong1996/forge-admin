import {
  buildSystemPostEditSchema,
  createPostCodeAutoCode,
  resolvePostEditSchemaOptions,
  SYSTEM_POST_EDIT_DICT_TYPES,
} from '@/views/system/schemas/systemPostEditSchema'
import { resolveCreatedPostRecord } from '../resolveCreatedRecord'
import { resolveDefaultsFromForm, resolveUserFormOrgId } from '../resolveDefaults'
import { submitQuickCreateApi } from '../submitApi'
import { SYSTEM_EDIT_FORM_LAYOUT } from '@/views/system/schemas/systemEditLayout'

const postCodeAutoCode = createPostCodeAutoCode()

export function createSystemPostQuickCreatePreset() {
  return {
    key: 'systemPost',
    permission: 'system:post:add',
    linkLabel: '新增岗位',
    modalTitle: '新增岗位',
    submitApi: 'post@/system/post/add',
    valueField: 'id',
    labelField: 'postName',
    formLayout: { ...SYSTEM_EDIT_FORM_LAYOUT },
    editSchemaDictTypes: SYSTEM_POST_EDIT_DICT_TYPES,
    buildSchema(ctx = {}) {
      return buildSystemPostEditSchema({
        postCodeAutoCode,
        ...resolvePostEditSchemaOptions(ctx.dict),
      })
    },
    buildInitialValues(ctx = {}) {
      postCodeAutoCode.reset()
      const { userStore, parentFormData, config } = ctx
      const orgId = resolveUserFormOrgId(parentFormData)
      const fromForm = resolveDefaultsFromForm(config?.defaultsFromForm, parentFormData)
      return {
        postType: 2,
        postStatus: 1,
        sort: 0,
        tenantId: userStore?.userInfo?.tenantId ?? userStore?.tenantId,
        orgId: orgId ?? fromForm.orgId ?? null,
        ...fromForm,
      }
    },
    beforeOpen(ctx = {}) {
      const orgId = resolveUserFormOrgId(ctx.parentFormData)
      if (orgId === null || orgId === undefined || orgId === '') {
        return { ok: false, message: '请先选择所属组织' }
      }
      return { ok: true }
    },
    async submit(payload, ctx) {
      const { userStore } = ctx
      const body = {
        postType: 2,
        postStatus: 1,
        sort: 0,
        tenantId: userStore?.userInfo?.tenantId ?? userStore?.tenantId,
        ...payload,
      }
      await submitQuickCreateApi('post@/system/post/add', body)
      const record = await resolveCreatedPostRecord(body, body.tenantId)
      return { submitted: body, record }
    },
  }
}

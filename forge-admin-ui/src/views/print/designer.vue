<script setup>
import { NEmpty, NModal, NSpin, useThemeVars } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { businessApplicationDetail } from '@/api/business-application'
import { loadPrintFile } from '@/api/print'
import PrintDesigner from '@/components/print/designer/PrintDesigner.vue'
import PrintBindingPanel from '@/components/print/management/PrintBindingPanel.vue'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import { leavePrintDesigner } from '@/components/print/management/printRouteContext'
import PrintTemplateVersions from '@/components/print/management/PrintTemplateVersions.vue'
import PrintPreview from '@/components/print/runtime/PrintPreview.vue'
import { useUserStore } from '@/store'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const route = useRoute()
const router = useRouter()
const store = usePrintTemplateStore()
const canvas = usePrintDesignerStore()
const user = useUserStore()
const theme = useThemeVars()
const themeStyle = computed(() => ({ background: theme.value.bodyColor, color: theme.value.textColor1 }))
const designer = ref(null)
const allowed = permission => hasPrintPermission(user, permission)
const canManage = computed(() => allowed('print:template:manage'))
const canPublish = computed(() => allowed('print:template:publish'))
const canLeave = () => designer.value?.canLeave() ?? true
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
onBeforeUnmount(() => store.clear())
watch(() => route.query.templateId, (id) => {
  if (/^[1-9]\d*$/.test(String(id || ''))) {
    store.open(id)
  }
  else {
    store.clear()
    store.error = '请从打印模板列表选择模板'
  }
}, { immediate: true })
async function panel(name) {
  try {
    if (name === 'versions')
      await store.loadVersions()
    store.panel = name
  }
  catch (error) {
    store.error = error.message || '读取失败'
  }
}
async function restore(id) {
  if (!canManage.value || !await canLeave())
    return
  try {
    const before = canvas.serialize()
    const nameBefore = store.name
    const document = await store.versionDocument(id)
    if (!document)
      return
    if ((before !== canvas.serialize() || nameBefore !== store.name) && !await canLeave())
      return
    canvas.execute((draft) => {
      for (const key of Object.keys(draft)) delete draft[key]
      Object.assign(draft, document)
    })
    store.panel = null
  }
  catch (error) {
    store.error = error.message || '载入版本失败'
  }
}
async function goBack() {
  let applicationCode = String(route.query.applicationCode || '').trim()
  if (!applicationCode && store.row?.source?.applicationId) {
    try {
      const { data } = await businessApplicationDetail(store.row.source.applicationId)
      applicationCode = String(data?.applicationCode || '').trim()
    }
    catch {
      applicationCode = ''
    }
  }
  leavePrintDesigner({
    router,
    route,
    source: store.row?.source,
    applicationCode,
  })
}
</script>

<template>
  <div class="print-designer-page" :style="themeStyle">
    <NSpin v-if="store.loading" show style="padding: 40px" />
    <div v-else-if="store.document" class="page-editor">
      <PrintDesigner
        v-if="canManage"
        ref="designer"
        :key="store.row.id"
        v-model:template-name="store.name"
        :template="store.document"
        :catalog="store.catalog"
        :save-draft="store.save"
        :external-dirty="store.nameDirty"
        :external-error="store.error"
        :external-notice="store.notice"
        :resolve-file="loadPrintFile"
        page-mode
        :design-status="store.row.designStatus"
        :draft-revision="store.row.draftRevision"
        :can-manage="canManage"
        :can-publish="canPublish"
        :publishing="store.saving"
        :name-dirty="store.nameDirty"
        @back="goBack"
        @bindings="panel('bindings')"
        @versions="panel('versions')"
        @publish="store.publish()"
        @clear-external-error="store.error = ''"
        @clear-external-notice="store.notice = ''"
      />
      <PrintPreview v-else :template="store.document" :template-name="store.name" :context="{}" :catalog="store.catalog" :resolve-file="loadPrintFile" data-label="模板预览" :allow-print="false" />
    </div>
    <NEmpty v-else :description="store.error || '未载入打印模板'" />
    <NModal
      :show="!!store.panel"
      preset="card"
      :title="store.panel === 'versions' ? '发布版本' : '场景绑定'"
      :style="store.panel === 'bindings' ? 'width: min(560px, 94vw)' : 'width: min(720px, 94vw)'"
      @update:show="value => { if (!value) store.panel = null }"
    >
      <PrintTemplateVersions v-if="store.panel === 'versions'" :can-restore="canManage && !store.saving" @restore="restore" />
      <PrintBindingPanel v-if="store.panel === 'bindings'" :can-manage="canManage" />
    </NModal>
  </div>
</template>

<style scoped>
.print-designer-page {
  height: 100vh;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #fff);
}
.page-editor {
  flex: 1;
  min-height: 0;
}
</style>

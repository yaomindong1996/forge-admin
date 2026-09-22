<script setup>
import { NEmpty } from 'naive-ui'
import { computed, onBeforeUnmount, watch } from 'vue'
import PrintSourceSelector from '@/components/print/designer/PrintSourceSelector.vue'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import PrintTemplateList from '@/components/print/management/PrintTemplateList.vue'
import { useUserStore } from '@/store'
import { usePrintWorkspaceStore } from '@/stores/print/printWorkspaceStore'

const props = defineProps({
  application: { type: Object, required: true },
  applicationObjects: { type: Array, default: () => [] },
  pageId: { type: String, default: '' },
})
const store = usePrintWorkspaceStore()
const user = useUserStore()
const canView = computed(() => hasPrintPermission(user, 'print:template:view'))
const lockSource = computed(() => Boolean(String(props.pageId || '').trim()))
watch(() => [props.application, props.applicationObjects, props.pageId], () => {
  store.sync(props.application, props.applicationObjects, props.pageId)
}, { immediate: true, deep: true })
onBeforeUnmount(() => store.clear())
</script>

<template>
  <PrintTemplateList
    v-if="canView"
    :application-id="store.applicationId"
    :application-code="application.applicationCode"
    :source="store.source"
    :sources="store.sources"
    :lock-source="lockSource"
  >
    <template v-if="!lockSource" #source>
      <PrintSourceSelector />
    </template>
  </PrintTemplateList>
  <NEmpty v-else description="暂无查看打印模板的权限" />
</template>

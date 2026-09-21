<script setup>
import { NEmpty } from 'naive-ui'
import { computed, onBeforeUnmount, watch } from 'vue'
import PrintSourceSelector from '@/components/print/designer/PrintSourceSelector.vue'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import PrintTemplateList from '@/components/print/management/PrintTemplateList.vue'
import { useUserStore } from '@/store'
import { usePrintWorkspaceStore } from '@/stores/print/printWorkspaceStore'

const props = defineProps({ application: { type: Object, required: true }, applicationObjects: { type: Array, default: () => [] } })
const store = usePrintWorkspaceStore()
const user = useUserStore()
const canView = computed(() => hasPrintPermission(user, 'print:template:view'))
watch(() => [props.application, props.applicationObjects], () => store.sync(props.application, props.applicationObjects), { immediate: true, deep: true })
onBeforeUnmount(() => store.clear())
</script>

<template>
  <PrintTemplateList v-if="canView" :application-id="store.applicationId" :source="store.source" :sources="store.sources">
    <template #source>
      <PrintSourceSelector />
    </template>
  </PrintTemplateList>
  <NEmpty v-else description="暂无查看打印模板的权限" />
</template>

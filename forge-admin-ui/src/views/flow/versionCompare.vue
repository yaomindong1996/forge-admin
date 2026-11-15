<template>
  <NModal
    v-model:show="visible"
    preset="card"
    title="版本对比"
    style="width: 1000px"
    @update:show="handleVisibleUpdate"
  >
    <NSpace vertical>
      <NSelect v-model:value="version1" :options="versionOptions" placeholder="选择版本1" />
      <NSelect v-model:value="version2" :options="versionOptions" placeholder="选择版本2" />
      <NButton type="primary" @click="handleCompare">
        对比
      </NButton>
      <NDivider />
      <div v-if="diffResult">
        <NH3>差异结果</NH3>
        <NSpace vertical>
          <NEmpty v-if="isEmptyDiff" description="两个版本未检测到节点或连线差异" />
          <div v-if="diffResult.addedNodes?.length">
            <NText strong>
              新增节点：
            </NText>
            <NList>
              <NListItem v-for="node in diffResult.addedNodes" :key="node.id">
                {{ node.name }} ({{ node.id }})
              </NListItem>
            </NList>
          </div>
          <div v-if="diffResult.modifiedNodes?.length">
            <NText strong>
              修改节点：
            </NText>
            <NList>
              <NListItem v-for="node in diffResult.modifiedNodes" :key="node.id">
                {{ node.oldName }} → {{ node.newName }}
              </NListItem>
            </NList>
          </div>
          <div v-if="diffResult.deletedNodes?.length">
            <NText strong>
              删除节点：
            </NText>
            <NList>
              <NListItem v-for="node in diffResult.deletedNodes" :key="node.id">
                {{ node.name }} ({{ node.id }})
              </NListItem>
            </NList>
          </div>
          <div v-if="diffResult.addedFlows?.length">
            <NText strong>
              新增连线：
            </NText>
            <NList>
              <NListItem v-for="flow in diffResult.addedFlows" :key="flow.id">
                {{ flow.id }}：{{ flow.source }} → {{ flow.target }}
              </NListItem>
            </NList>
          </div>
          <div v-if="diffResult.modifiedFlows?.length">
            <NText strong>
              修改连线：
            </NText>
            <NList>
              <NListItem v-for="flow in diffResult.modifiedFlows" :key="flow.id">
                {{ flow.id }}：{{ flow.oldSource }} → {{ flow.oldTarget }} 改为 {{ flow.newSource }} → {{ flow.newTarget }}
              </NListItem>
            </NList>
          </div>
          <div v-if="diffResult.deletedFlows?.length">
            <NText strong>
              删除连线：
            </NText>
            <NList>
              <NListItem v-for="flow in diffResult.deletedFlows" :key="flow.id">
                {{ flow.id }}：{{ flow.source }} → {{ flow.target }}
              </NListItem>
            </NList>
          </div>
        </NSpace>
      </div>
    </NSpace>
  </NModal>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import versionApi from '@/api/version'

const props = defineProps({
  modelId: {
    type: [String, Number],
    required: true,
  },
})
const emit = defineEmits(['close'])

const visible = ref(true)
const version1 = ref(null)
const version2 = ref(null)
const versionOptions = ref([])
const diffResult = ref(null)
const isEmptyDiff = computed(() => {
  if (!diffResult.value)
    return false
  return !diffResult.value.addedNodes?.length
    && !diffResult.value.modifiedNodes?.length
    && !diffResult.value.deletedNodes?.length
    && !diffResult.value.addedFlows?.length
    && !diffResult.value.modifiedFlows?.length
    && !diffResult.value.deletedFlows?.length
})

function handleVisibleUpdate(show) {
  if (!show)
    emit('close')
}

async function loadVersionOptions() {
  try {
    const res = await versionApi.getVersionList(props.modelId, 1, 100)
    const records = res.data?.records || []
    versionOptions.value = records.map(v => ({
      label: `${v.versionName} (${v.publishTime})`,
      value: v.version,
    }))
    // 默认预选最新两个版本，减少空选再点对比的步骤
    if (!version1.value && records.length >= 1)
      version1.value = records[0].version
    if (!version2.value && records.length >= 2)
      version2.value = records[1].version
  }
  catch (error) {
    console.error('加载版本列表失败', error)
  }
}

async function handleCompare() {
  if (!version1.value || !version2.value) {
    window.$message?.warning('请选择两个版本')
    return
  }
  if (version1.value === version2.value) {
    window.$message?.warning('请选择两个不同版本')
    return
  }
  try {
    const res = await versionApi.compareVersions({
      modelId: props.modelId,
      version1: version1.value,
      version2: version2.value,
    })
    diffResult.value = res.data
  }
  catch (error) {
    console.error('版本对比失败', error)
  }
}

onMounted(() => {
  loadVersionOptions()
})
</script>

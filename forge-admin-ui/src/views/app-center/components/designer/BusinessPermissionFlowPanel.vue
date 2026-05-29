<template>
  <div class="business-permission-flow-panel">
    <div class="permission-flow-head">
      <div>
        <h3>权限流程</h3>
        <p>展示权限、审批和自动化挂接摘要，底层配置仍复用现有能力。</p>
      </div>
      <n-button size="small" secondary :loading="loading" @click="loadSummary">
        刷新
      </n-button>
    </div>

    <div class="permission-flow-body">
      <main class="permission-summary-pane">
        <section class="summary-card">
          <div class="summary-card-head">
            <div>
              <h4>对象权限</h4>
              <p>{{ permissionSummary.message || '权限摘要待加载' }}</p>
            </div>
            <n-tag :type="statusType(permissionSummary.status)" :bordered="false">
              {{ permissionSummary.statusLabel || permissionSummary.status || '未知' }}
            </n-tag>
          </div>
          <n-space v-if="permissionSummary.nextActionLabel" size="small">
            <n-button size="small" secondary type="primary" @click="openAction(permissionSummary)">
              {{ permissionSummary.nextActionLabel }}
            </n-button>
          </n-space>
        </section>

        <section class="summary-card">
          <div class="summary-card-head">
            <div>
              <h4>审批流程</h4>
              <p>审批能力通过对象能力挂接维护，发布检查只读取挂接状态。</p>
            </div>
            <n-tag type="info" :bordered="false">
              摘要
            </n-tag>
          </div>
          <BusinessBindingPanel
            target-type="OBJECT"
            :target-id="objectId"
            :target-code="objectCode"
          />
        </section>
      </main>

      <aside class="permission-flow-tips">
        <section>
          <h4>权限范围</h4>
          <div class="permission-facts">
            <div>
              <span>可见</span>
              <strong>菜单权限</strong>
            </div>
            <div>
              <span>新增</span>
              <strong>接口权限</strong>
            </div>
            <div>
              <span>编辑</span>
              <strong>按钮权限</strong>
            </div>
            <div>
              <span>删除</span>
              <strong>危险操作</strong>
            </div>
          </div>
        </section>
        <section>
          <h4>发布关注</h4>
          <p>权限和流程未配置不会阻断基础发布，但会在就绪度和发布检查中提示后续配置入口。</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { businessObjectPermissionSummary } from '@/api/business-app'
import BusinessBindingPanel from '../BusinessBindingPanel.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  objectCode: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const permissionSummary = ref({})

watch(() => props.objectId, loadSummary)
onMounted(loadSummary)

async function loadSummary() {
  if (!props.objectId) {
    permissionSummary.value = {}
    return
  }
  loading.value = true
  try {
    const res = await businessObjectPermissionSummary(props.objectId)
    permissionSummary.value = res.data || {}
  }
  finally {
    loading.value = false
  }
}

function openAction(item) {
  if (!item?.nextActionUrl) {
    message.info(item?.message || '暂无可打开的配置入口')
    return
  }
  if (/^https?:\/\//i.test(item.nextActionUrl)) {
    window.open(item.nextActionUrl, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(item.nextActionUrl)
}

function statusType(status) {
  if (['RUNNABLE', 'CONFIGURED', 'REGISTERED'].includes(status))
    return 'success'
  if (status === 'MISSING')
    return 'warning'
  if (status === 'ERROR')
    return 'error'
  return 'default'
}

defineExpose({
  loadSummary,
})
</script>

<style scoped>
.business-permission-flow-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
}

.permission-flow-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.permission-flow-head h3,
.summary-card h4,
.permission-flow-tips h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.permission-flow-head p,
.summary-card p,
.permission-flow-tips p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.permission-flow-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  min-height: 0;
}

.permission-summary-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.summary-card,
.permission-flow-tips section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.summary-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.permission-flow-tips {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.permission-facts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.permission-facts div {
  border-radius: 6px;
  background: #f1f5f9;
  padding: 10px;
}

.permission-facts span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.permission-facts strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .permission-flow-body {
    grid-template-columns: 1fr;
  }

  .permission-flow-tips {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>

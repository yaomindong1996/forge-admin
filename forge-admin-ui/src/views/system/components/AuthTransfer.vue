<!--
  权限穿梭框组件
  用于角色授权时选择菜单、API、按钮等权限
-->

<template>
  <div class="auth-transfer">
    <n-spin :show="loading">
      <n-space vertical :size="16">
        <!-- 树形穿梭框 -->
        <div class="transfer-wrapper">
          <!-- 左侧：已授权列表 -->
          <div class="transfer-panel left-panel">
            <div class="panel-header">
              <span>已授权</span>
              <span class="count">{{ leftTree.length > 0 ? authorizedKeys.length : 0 }}</span>
            </div>
            <div class="panel-body">
              <n-tree
                v-if="leftTree.length > 0"
                :data="leftTree"
                :checked-keys="authorizedKeys"
                :expanded-keys="authorizedExpandedKeys"
                checkable
                cascade
                selectable
                @update:checked-keys="handleAuthorizedChange"
                @update:expanded-keys="handleAuthorizedExpandedChange"
              />
              <n-empty v-else description="暂无已授权数据" />
            </div>
          </div>

          <div class="transfer-actions">
            <n-space vertical :size="8">
              <n-button
                type="primary"
                size="small"
                :disabled="rightCheckedKeys.length === 0"
                @click="handleAuthorize"
              >
                <template #icon>
                  <n-icon><ChevronBackOutline /></n-icon>
                </template>
                授权
              </n-button>
              <n-button
                type="error"
                size="small"
                :disabled="authorizedKeys.length === 0"
                @click="handleRevoke"
              >
                解除
                <template #icon>
                  <n-icon><ChevronForwardOutline /></n-icon>
                </template>
              </n-button>
            </n-space>
          </div>

          <!-- 右侧：未授权列表 -->
          <div class="transfer-panel right-panel">
            <div class="panel-header">
              <span>未授权</span>
              <span class="count">{{ rightTree.length > 0 ? rightCheckedKeys.length : 0 }}</span>
            </div>
            <div class="panel-body">
              <n-tree
                v-if="rightTree.length > 0"
                :data="rightTree"
                :checked-keys="rightCheckedKeys"
                :expanded-keys="rightExpandedKeys"
                checkable
                cascade
                selectable
                @update:checked-keys="handleRightCheckedChange"
                @update:expanded-keys="handleRightExpandedChange"
              />
              <n-empty v-else description="暂无未授权数据" />
            </div>
          </div>
        </div>
      </n-space>
    </n-spin>
  </div>
</template>

<script setup>
import { ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  // 角色ID
  roleId: {
    type: [String, Number],
    required: true,
  },
  // 权限类型: 1-菜单权限, 2-API权限
  kind: {
    type: String,
    default: '1',
  },
})

const message = window.$message

const loading = ref(false)
const leftTree = ref([]) // 已授权
const rightTree = ref([]) // 未授权
const authorizedKeys = ref([]) // 左侧选中的keys
const rightCheckedKeys = ref([]) // 右侧选中的keys
const authorizedExpandedKeys = ref([])
const rightExpandedKeys = ref([])

/**
 * 获取所有叶子节点的原始 ID
 * 只提取叶子节点(没有 children 的节点)的 _rawId
 */
function getAllLeafIds(tree = []) {
  const results = []
  const traverse = (items) => {
    items.forEach((item) => {
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
      else {
        // 提取原始 ID
        results.push(item._rawId || item.key)
      }
    })
  }
  traverse(tree)
  return results
}

/**
 * 从选中的组合 keys 中提取叶子节点的原始 ID
 * @param {Array} keys - 选中的组合 keys
 * @param {Array} tree - 树形数据
 */
function extractLeafRawIds(keys, tree) {
  const results = []
  const keySet = new Set(keys)

  const traverse = (items) => {
    items.forEach((item) => {
      if (item.children && item.children.length > 0) {
        // 递归遍历子节点
        traverse(item.children)
      }
      else {
        // 只处理叶子节点
        if (keySet.has(item.key)) {
          results.push(item._rawId || item.key)
        }
      }
    })
  }

  traverse(tree)
  return results
}

/**
 * 右侧选中变化
 */
function handleRightCheckedChange(keys) {
  rightCheckedKeys.value = keys
}

/**
 * 右侧展开变化
 */
function handleRightExpandedChange(keys) {
  rightExpandedKeys.value = keys
}

/**
 * 已授权树选中变化
 */
function handleAuthorizedChange(keys) {
  authorizedKeys.value = keys
}

/**
 * 已授权树展开变化
 */
function handleAuthorizedExpandedChange(keys) {
  authorizedExpandedKeys.value = keys
}

/**
 * 授权(从右侧移到左侧)
 */
async function handleAuthorize() {
  if (rightCheckedKeys.value.length === 0) {
    message.warning('请选择要授权的内容')
    return
  }

  try {
    loading.value = true

    // 获取左侧所有已授权的叶子节点原始 ID
    const allAuthIds = getAllLeafIds(leftTree.value)

    // 从右侧选中的组合 keys 中提取叶子节点的原始 ID
    const rightSelectedRawIds = extractLeafRawIds(rightCheckedKeys.value, rightTree.value)

    console.log('授权 - 左侧已授权:', allAuthIds)
    console.log('授权 - 右侧选中:', rightSelectedRawIds)

    // 合并原始 ID(去重)
    const authIds = Array.from(new Set([...allAuthIds, ...rightSelectedRawIds]))

    console.log('授权 - 最终提交:', authIds)

    // 调用授权接口: POST /role/auth
    await request.post('/role/auth', {
      authIds,
      roleId: props.roleId,
      kind: props.kind,
    })

    message.success('授权成功')

    // 清空选中状态
    rightCheckedKeys.value = []
    authorizedKeys.value = []

    // 刷新数据
    await loadData()
  }
  catch (error) {
    console.error('授权失败:', error)
    message.error('授权失败')
  }
  finally {
    loading.value = false
  }
}

/**
 * 解除授权(从左侧移除)
 */
async function handleRevoke() {
  if (authorizedKeys.value.length === 0) {
    message.warning('请选择要解除的内容')
    return
  }

  try {
    loading.value = true

    // 获取左侧所有已授权的叶子节点原始 ID
    const allAuthIds = getAllLeafIds(leftTree.value)

    // 从左侧选中的组合 keys 中提取叶子节点的原始 ID
    const leftSelectedRawIds = extractLeafRawIds(authorizedKeys.value, leftTree.value)

    console.log('解除 - 全部已授权:', allAuthIds)
    console.log('解除 - 左侧选中:', leftSelectedRawIds)

    // 过滤掉选中要解除的原始 ID,剩下的就是保留的授权
    const authIds = allAuthIds.filter(id => !leftSelectedRawIds.includes(id))

    console.log('解除 - 最终提交:', authIds)

    // 调用授权接口: POST /role/auth(传入剩余的授权ID)
    await request.post('/role/auth', {
      authIds,
      roleId: props.roleId,
      kind: props.kind,
    })

    message.success('解除授权成功')

    // 清空选中状态
    rightCheckedKeys.value = []
    authorizedKeys.value = []

    // 刷新数据
    await loadData()
  }
  catch (error) {
    console.error('解除授权失败:', error)
    message.error('解除授权失败')
  }
  finally {
    loading.value = false
  }
}

/**
 * 加载数据
 */
async function loadData() {
  try {
    loading.value = true

    // 并行加载已授权和未授权数据
    // GET /menu/authList?kind=1&roleId=xxx - 查询已授权列表 (左侧)
    // GET /menu/list?kind=1&roleId=xxx - 查询未授权列表 (右侧)
    const [authorizedRes, unauthorizedRes] = await Promise.all([
      request.get('/menu/authList', {
        params: {
          kind: props.kind,
          roleId: props.roleId,
        },
      }),
      request.get('/menu/list', {
        params: {
          kind: props.kind,
          roleId: props.roleId,
        },
      }),
    ])

    // 转换为树形数据
    leftTree.value = convertToTreeData(authorizedRes.data || [])
    rightTree.value = convertToTreeData(unauthorizedRes.data || [])

    // 自动展开第一级
    authorizedExpandedKeys.value = getFirstLevelKeys(leftTree.value)
    rightExpandedKeys.value = getFirstLevelKeys(rightTree.value)

    // 清空选中状态
    authorizedKeys.value = []
    rightCheckedKeys.value = []
  }
  catch (error) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败')
  }
  finally {
    loading.value = false
  }
}

/**
 * 转换为树形数据
 * 使用组合 key 避免 ID 重复问题
 * @param {Array} data - 原始数据
 * @param {string} parentKey - 父节点 key
 */
function convertToTreeData(data, parentKey = '') {
  if (!Array.isArray(data))
    return []

  return data.map((item) => {
    // 使用组合 key: parentKey-id,避免不同父节点下相同 ID 的冲突
    const uniqueKey = parentKey ? `${parentKey}-${item.id}` : `${item.id}`

    return {
      key: uniqueKey,
      label: item.name || item.label || item.title,
      _rawId: item.id, // 保留原始 ID 用于提交
      children: item.children ? convertToTreeData(item.children, uniqueKey) : undefined,
    }
  })
}

/**
 * 获取第一级节点的keys
 */
function getFirstLevelKeys(nodes) {
  return nodes.map(item => item.key)
}

// 监听角色ID变化,自动加载数据
watch(() => props.roleId, () => {
  if (props.roleId) {
    loadData()
  }
}, { immediate: true })

// 注意: 不需要在 onMounted 中重复调用 loadData,因为 watch 已设置 immediate: true
</script>

<style scoped>
.auth-transfer {
  padding: 16px 0;
}

.transfer-wrapper {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.transfer-panel {
  flex: 1;
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--n-border-color);
  font-weight: 600;
}

.count {
  color: var(--n-text-color-disabled);
  font-size: 13px;
}

.panel-body {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  max-height: 500px;
}

.transfer-actions {
  display: flex;
  align-items: center;
  padding: 0 8px;
}

.left-panel {
}

.right-panel {
  background: rgba(24, 160, 88, 0.05);
}
</style>

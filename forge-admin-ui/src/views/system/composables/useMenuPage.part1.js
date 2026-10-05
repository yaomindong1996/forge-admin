/** menu.vue setup part 1. */

import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import api from '@/api'
import { useDict } from '@/composables'
import { usePermissionStore, useUserStore } from '@/store'
import { request } from '@/utils'
import { toNumberDictOptions } from '@/utils/dict-options'
import { getMenuRouteOptions } from '@/utils/menu-route-options'
import {
  flattenResourceTree,
  resolveFreshResourceRow,
  resolveResourceContextRows,
} from '../menu-interaction-utils'
import { buildNavigationTree, getResourceTypeConfig, renderNavigationLabel } from '../menu-tree-presentation'

export function applyMenuPagePart1() {
  const __impl = {}
  const mut = {}

  const { dict } = useDict(
    'sys_resource_type',
    'sys_show_hide',
    'sys_req_method',
    'sys_link_open_target',
    'sys_user_type',
    'sys_yes_no',
  )

  const resourceTypeOptions = computed(() => toNumberDictOptions(dict.value.sys_resource_type))
  const visibleOptions = computed(() => toNumberDictOptions(dict.value.sys_show_hide))
  const apiMethodOptions = computed(() => dict.value.sys_req_method || [])
  const openTargetOptions = computed(() => dict.value.sys_link_open_target || [])
  const minUserTypeOptions = computed(() => toNumberDictOptions(dict.value.sys_user_type))
  const yesNoOptions = computed(() => toNumberDictOptions(dict.value.sys_yes_no))

  const permissionStore = usePermissionStore()
  const userStore = useUserStore()

  const currentUserClientCode = computed(() => userStore.userInfo?.userClient || 'pc')
  const routeOptions = getMenuRouteOptions()

  const pageRef = ref(null)
  const formRef = ref(null)
  const clientList = ref([])
  const currentClientCode = ref(currentUserClientCode.value)
  const loading = ref(false)
  const submitLoading = ref(false)
  const batchActionLoading = ref(false)
  const allResources = ref([])
  const selectedResourceId = ref(0)
  const selectedRow = ref(null)
  const checkedResourceIds = ref([])
  const navigationExpandedKeys = ref([0])
  const treeKeyword = ref('')
  const resourceKeyword = ref('')
  const resourceTypeFilter = ref(null)
  const visibleFilter = ref(null)
  const parentResourceOptions = ref([{ label: '顶级资源', value: 0, key: 0 }])
  const pendingParentId = ref(null)
  const pendingClientCode = ref(null)
  const drawerVisible = ref(false)
  const drawerMode = ref('add')
  const formData = ref({})
  const batchMigrateVisible = ref(false)
  const batchMigrateParentId = ref(0)
  const formIconTab = ref('font')
  const tableIconSelectorRef = ref(null)
  const tableIconEditRow = ref(null)
  const tableIconValue = ref('')

  const publicParams = computed(() => {
    if (currentClientCode.value)
      return { clientCode: currentClientCode.value }
    return {}
  })

  const drawerTitle = computed(() => drawerMode.value === 'edit' ? '编辑资源' : '新增资源')
  const drawerWidth = computed(() => Math.min(860, Math.max(640, Math.floor(window.innerWidth * 0.58))))

  const clientCodeOptions = computed(() => {
    return clientList.value.map(client => ({
      label: client.clientName,
      value: client.clientCode,
    }))
  })

  const resourceTypeFilterOptions = computed(() => resourceTypeOptions.value)
  const visibleFilterOptions = computed(() => visibleOptions.value)

  const flatResources = computed(() => flattenResourceTree(allResources.value))

  const navigationSelectedKeys = computed(() => {
    if (!selectedResourceId.value)
      return []
    const exists = flatResources.value.some(item => item.id === selectedResourceId.value)
    return exists ? [selectedResourceId.value] : []
  })

  const currentNode = computed(() => {
    if (!selectedResourceId.value)
      return null
    return flatResources.value.find(item => item.id === selectedResourceId.value) || null
  })

  const activeResource = computed(() => selectedRow.value || currentNode.value)

  const currentContextTitle = computed(() => {
    if (resourceKeyword.value || resourceTypeFilter.value !== null || visibleFilter.value !== null)
      return currentNode.value ? `${currentNode.value.resourceName} 下的匹配资源` : '全部匹配资源'
    return currentNode.value ? `${currentNode.value.resourceName} 下级资源` : '顶级资源'
  })

  const activeChildSummary = computed(() => {
    const children = activeResource.value?.children || []
    return {
      menu: children.filter(item => Number(item.resourceType) === 1 || Number(item.resourceType) === 2).length,
      button: children.filter(item => Number(item.resourceType) === 3).length,
      api: children.filter(item => Number(item.resourceType) === 4).length,
    }
  })

  const navigationTreeData = computed(() => {
    const keyword = treeKeyword.value.trim().toLowerCase()
    const children = buildNavigationTree(allResources.value, keyword)
    return [
      {
        key: 0,
        id: 0,
        label: '全部资源',
        resourceName: '全部资源',
        resourceType: 0,
        children,
      },
    ]
  })

  const displayRows = computed(() => {
    const hasFilter = !!resourceKeyword.value.trim() || resourceTypeFilter.value !== null || visibleFilter.value !== null
    const baseRows = getContextRows({ includeDescendants: hasFilter })

    if (!hasFilter)
      return baseRows

    return baseRows.filter(row => matchesResourceFilter(row))
  })

  const checkedResourceIdSet = computed(() => new Set(checkedResourceIds.value))

  const checkedResourceRows = computed(() => {
    const checkedIds = checkedResourceIdSet.value
    return flatResources.value.filter(row => checkedIds.has(row.id))
  })

  const allDisplayRowsChecked = computed(() => {
    return displayRows.value.length > 0 && displayRows.value.every(row => checkedResourceIdSet.value.has(row.id))
  })

  const displayRowsCheckIndeterminate = computed(() => {
    if (displayRows.value.length === 0)
      return false
    const checkedCount = displayRows.value.filter(row => checkedResourceIdSet.value.has(row.id)).length
    return checkedCount > 0 && checkedCount < displayRows.value.length
  })

  const batchMigrateRootRows = computed(() => {
    const checkedIds = checkedResourceIdSet.value
    return checkedResourceRows.value.filter(row => !hasCheckedAncestor(row, checkedIds))
  })

  const batchMigrateDisabledParentIds = computed(() => {
    const disabledIds = new Set(checkedResourceIds.value)
    checkedResourceRows.value.forEach((row) => {
      collectRowDescendantIds(row).forEach(id => disabledIds.add(id))
    })
    return disabledIds
  })

  const batchMigrateParentOptions = computed(() => [
    { label: '顶级资源', value: 0, key: 0 },
    ...buildBatchMigrateParentOptions(allResources.value, batchMigrateDisabledParentIds.value),
  ])

  watch(currentClientCode, async () => {
    selectedResourceId.value = 0
    selectedRow.value = null
    checkedResourceIds.value = []
    pendingParentId.value = null
    pendingClientCode.value = null
    await loadResourceTree({ expandAll: false })
  })

  onMounted(async () => {
    setupMenuPageLayout()
    await loadClientList()
    await loadResourceTree({ expandAll: false })
  })

  onBeforeUnmount(() => {
    pageRef.value?.closest?.('.nexus-page')?.classList.remove('menu-page-host-no-scroll')
  })

  function setupMenuPageLayout() {
    nextTick(() => {
      const pageEl = pageRef.value
      if (!pageEl)
        return

      pageEl.closest?.('.nexus-page')?.classList.add('menu-page-host-no-scroll')
    })
  }

  async function loadClientList() {
    try {
      const res = await request.get('/system/client/list')
      if (res.code === 200) {
        clientList.value = res.data || []

        if (!currentClientCode.value) {
          const userClient = currentUserClientCode.value
          const matchedClient = clientList.value.find(client => client.clientCode === userClient)
          if (matchedClient)
            currentClientCode.value = userClient
        }
      }
    }
    catch (error) {
      console.error('加载客户端列表失败:', error)
    }
  }

  async function loadResourceTree(options = {}) {
    loading.value = true
    try {
      const res = await request.get('/system/resource/tree', { params: publicParams.value })
      const list = normalizeListResponse(res)
      allResources.value = Array.isArray(list) ? list : []
      syncParentResourceOptions(allResources.value)
      if (options.expandAll)
        expandNavigationTree()
      else
        reconcileNavigationExpandedKeys()
      keepSelectionAvailable()
      reconcileCheckedResourceIds()
      await nextTick()
    }
    catch (error) {
      console.error('加载资源树失败:', error)
      window.$message?.error('加载资源树失败')
    }
    finally {
      loading.value = false
    }
  }

  function normalizeListResponse(res) {
    if (Array.isArray(res))
      return res
    if (Array.isArray(res?.data))
      return res.data
    if (Array.isArray(res?.data?.records))
      return res.data.records
    if (Array.isArray(res?.data?.list))
      return res.data.list
    return []
  }

  function keepSelectionAvailable() {
    selectedRow.value = resolveFreshResourceRow(flatResources.value, selectedRow.value)
    if (!selectedResourceId.value)
      return
    const exists = flatResources.value.some(item => item.id === selectedResourceId.value)
    if (!exists) {
      selectedResourceId.value = 0
      selectedRow.value = null
    }
  }

  function reconcileCheckedResourceIds() {
    if (checkedResourceIds.value.length === 0)
      return
    const existingIds = new Set(flatResources.value.map(item => item.id))
    checkedResourceIds.value = checkedResourceIds.value.filter(id => existingIds.has(id))
  }

  function handleResourceCheckedChange(row, checked) {
    if (!row?.id)
      return

    const nextIds = new Set(checkedResourceIds.value)
    if (checked)
      nextIds.add(row.id)
    else
      nextIds.delete(row.id)
    checkedResourceIds.value = [...nextIds]
  }

  function handleDisplayRowsCheckedChange(checked) {
    const nextIds = new Set(checkedResourceIds.value)
    displayRows.value.forEach((row) => {
      if (checked)
        nextIds.add(row.id)
      else
        nextIds.delete(row.id)
    })
    checkedResourceIds.value = [...nextIds]
  }

  function clearCheckedResources() {
    checkedResourceIds.value = []
  }

  function handleClientTabChange(clientCode) {
    currentClientCode.value = clientCode
  }

  function expandNavigationTree() {
    navigationExpandedKeys.value = getExpandableNavigationKeys()
  }

  function collapseNavigationTree() {
    navigationExpandedKeys.value = navigationTreeData.value[0]?.children?.length ? [0] : []
  }

  function handleNavigationExpandedKeys(keys) {
    navigationExpandedKeys.value = Array.isArray(keys) ? [...keys] : []
  }

  function reconcileNavigationExpandedKeys() {
    const validKeys = new Set(getAllNavigationKeys())
    navigationExpandedKeys.value = navigationExpandedKeys.value.filter(key => validKeys.has(key))
  }

  function getAllNavigationKeys() {
    const keys = []
    const walk = (items = []) => {
      items.forEach((item) => {
        keys.push(item.key ?? item.id)
        if (item.children?.length)
          walk(item.children)
      })
    }
    walk(navigationTreeData.value)
    return keys
  }

  function getExpandableNavigationKeys() {
    const keys = []
    const walk = (items = []) => {
      items.forEach((item) => {
        if (item.children?.length) {
          keys.push(item.key ?? item.id)
          walk(item.children)
        }
      })
    }
    walk(navigationTreeData.value)
    return keys
  }

  function handleNavigationSelect(keys) {
    selectedResourceId.value = keys?.[0] ?? 0
    selectedRow.value = selectedResourceId.value
      ? flatResources.value.find(item => item.id === selectedResourceId.value) || null
      : null
  }

  function getContextRows(options = {}) {
    return resolveResourceContextRows(allResources.value, currentNode.value, options)
  }

  function matchesResourceFilter(row) {
    const keyword = resourceKeyword.value.trim().toLowerCase()
    const typeMatched = resourceTypeFilter.value === null || Number(row.resourceType) === Number(resourceTypeFilter.value)
    const visibleMatched = visibleFilter.value === null || Number(row.visible) === Number(visibleFilter.value)
    const keywordMatched = !keyword || [
      row.resourceName,
      row.path,
      row.component,
      row.perms,
      row.apiUrl,
      row.remark,
    ].some(value => String(value || '').toLowerCase().includes(keyword))

    return typeMatched && visibleMatched && keywordMatched
  }

  function getDisplayLevel(row) {
    const level = Number(row.level || 0)
    if (!Number.isFinite(level) || level < 0)
      return 0
    return Math.min(level, 5)
  }

  function getRenderableIcon(row) {
    const icon = String(row?.icon || '').trim()
    if (!icon)
      return ''

    if (isImageIconValue(icon) || icon.startsWith('i-') || icon.startsWith('ionicons5:') || icon.startsWith('local:') || icon.startsWith('local-image:'))
      return icon

    if (icon === 'ProfileOutlined')
      return 'i-material-symbols:person-outline'

    return ''
  }

  async function handleSortCommit(row, value) {
    row._editingSort = false
    await handleInlineUpdate(row, 'sort', value ?? 0)
  }

  function getMoreActionOptions(row) {
    return [
      { label: '新增子项', key: 'add' },
      { label: '更换图标', key: 'icon', disabled: Number(row.resourceType) > 2 },
      { label: '复制路由', key: 'copyPath', disabled: !row.path },
      { label: '复制权限', key: 'copyPerms', disabled: !row.perms },
      { label: '删除', key: 'delete' },
    ]
  }

  function getResourceSubtitle(row) {
    const parts = []
    const parent = row.parent || currentNode.value
    if (parent?.resourceName)
      parts.push(`上级：${parent.resourceName}`)
    else
      parts.push('顶级资源')

    const childCount = row.children?.length || 0
    if (childCount)
      parts.push(`子项 ${childCount}`)

    return parts.join(' · ')
  }

  function getPrimaryRouteText(row) {
    if (Number(row.resourceType) === 4)
      return row.apiUrl || '-'
    if (Number(row.resourceType) === 3)
      return row.perms || '-'
    return row.path || '-'
  }

  function getSecondaryRouteText(row) {
    if (Number(row.resourceType) === 4)
      return row.apiMethod || ''
    if (Number(row.resourceType) === 3)
      return ''
    return row.component || ''
  }

  function getResourceTypeText(type) {
    return resourceTypeOptions.value.find(option => option.value === Number(type))?.label || '未知'
  }

  function getClientDisplayName(clientCode) {
    return clientList.value.find(item => item.clientCode === clientCode)?.clientName || clientCode || '-'
  }

  function getSsoTargetClientOptions(formValue) {
    const currentClient = formValue?.clientCode || pendingClientCode.value || currentClientCode.value
    return clientList.value
      .filter(client => client.clientCode && client.clientCode !== currentClient)
      .map(client => ({
        label: client.clientName,
        value: client.clientCode,
      }))
  }

  function handleMoreAction(key, row) {
    if (key === 'add')
      handleAdd(row)
    else if (key === 'icon')
      openTableIconSelector(row)
    else if (key === 'delete')
      handleDelete(row)
    else if (key === 'copyPath')
      copyText(row.path, '路由已复制')
    else if (key === 'copyPerms')
      copyText(row.perms, '权限标识已复制')
  }

  async function copyText(text, message) {
    if (!text)
      return
    try {
      await navigator.clipboard?.writeText(text)
      window.$message?.success(message)
    }
    catch {
      window.$message?.warning('当前浏览器不支持自动复制')
    }
  }

  function syncParentResourceOptions(list = allResources.value) {
    const convertToTreeSelect = (items = []) => {
      return items.map(item => ({
        label: item.resourceName,
        value: item.id,
        key: item.id,
        children: item.children && item.children.length > 0
          ? convertToTreeSelect(item.children)
          : undefined,
      }))
    }
    parentResourceOptions.value = [
      { label: '顶级资源', value: 0, key: 0 },
      ...convertToTreeSelect(Array.isArray(list) ? list : []),
    ]
  }

  function buildBatchMigrateParentOptions(list = [], disabledIds = new Set()) {
    return (Array.isArray(list) ? list : [])
      .filter(item => !disabledIds.has(item.id))
      .map((item) => {
        const children = buildBatchMigrateParentOptions(item.children || [], disabledIds)
        return {
          label: item.resourceName,
          value: item.id,
          key: item.id,
          children: children.length > 0 ? children : undefined,
        }
      })
  }

  function hasCheckedAncestor(row, checkedIds = checkedResourceIdSet.value) {
    let parent = row?.parent
    while (parent) {
      if (checkedIds.has(parent.id))
        return true
      parent = parent.parent
    }
    return false
  }

  function collectRowDescendantIds(row) {
    const ids = []
    const walk = (children = []) => {
      children.forEach((child) => {
        if (child?.id)
          ids.push(child.id)
        if (child?.children?.length)
          walk(child.children)
      })
    }
    walk(row?.children || [])
    return ids
  }

  function handleAddRoot() {
    // “顶级”不能继承当前选中的父级，明确传 0 保留接口原有根节点协议。
    handleAdd({ id: 0, clientCode: currentClientCode.value })
  }

  function handleAdd(row) {
    syncParentResourceOptions()
    pendingParentId.value = row?.id ?? selectedResourceId.value ?? 0
    pendingClientCode.value = row?.clientCode || currentNode.value?.clientCode || currentClientCode.value || null
    drawerMode.value = 'add'
    formData.value = beforeRenderForm(null)
    drawerVisible.value = true
    nextTick(() => formRef.value?.restoreValidation?.())
  }

  async function handleEdit(row) {
    syncParentResourceOptions()
    drawerMode.value = 'edit'
    const detail = await loadResourceDetail(row)
    formData.value = beforeRenderForm({ ...row, ...detail })
    drawerVisible.value = true
    selectedRow.value = row
    nextTick(() => formRef.value?.restoreValidation?.())
  }

  async function loadResourceDetail(row) {
    try {
      const res = await request.post('/system/resource/getById', null, { params: { id: row.id } })
      return res?.data || {}
    }
    catch (error) {
      console.warn('加载资源详情失败，使用列表数据:', error)
      return {}
    }
  }

  function handleDelete(row) {
    const childCount = getChildResourceCount(row)
    if (childCount > 0) {
      window.$message?.warning(`请先删除「${row.resourceName}」下的 ${childCount} 个子资源`)
      return
    }

    window.$dialog.warning({
      title: '确认删除',
      content: `确定要删除「${row.resourceName}」吗？删除后将无法恢复。`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          loading.value = true
          const res = await request.post('/system/resource/remove', null, { params: { id: row.id } })
          if (res.code === 200) {
            window.$message.success('删除成功')
            // 删除成功后，立即清空选中状态，避免树组件引用已删除的节点
            if (selectedRow.value?.id === row.id) {
              selectedRow.value = null
            }
            if (selectedResourceId.value === row.id) {
              selectedResourceId.value = 0
            }
            // 从展开状态中移除已删除节点的键
            navigationExpandedKeys.value = navigationExpandedKeys.value.filter(key => key !== row.id)
            await refreshSystemMenu()
            await loadResourceTree({ expandAll: false })
          }
          else {
            window.$message.error(res.msg || '删除失败')
          }
        }
        catch (error) {
          console.error('删除资源失败:', error)
          window.$message.error('删除失败')
        }
        finally {
          loading.value = false
        }
      },
    })
  }

  function handleBatchDelete() {
    if (checkedResourceIds.value.length === 0) {
      window.$message?.warning('请先选择要删除的资源')
      return
    }

    const blockers = getBatchDeleteBlockers()
    if (blockers.length > 0) {
      const names = blockers.slice(0, 3).map(row => row.resourceName).join('、')
      window.$message?.warning(`「${names}」下还有未选择的子资源，请勾选整棵子树后再删除`)
      return
    }

    const deletingIds = [...checkedResourceIds.value]
    window.$dialog.warning({
      title: '批量删除',
      content: `确定要删除选中的 ${deletingIds.length} 项资源吗？删除后将无法恢复。`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          batchActionLoading.value = true
          const res = await request.post('/system/resource/removeBatch', deletingIds)
          if (res.code === 200) {
            window.$message.success('批量删除成功')
            resetSelectionAfterDelete(deletingIds)
            await refreshSystemMenu()
            await loadResourceTree({ expandAll: false })
          }
          else {
            window.$message.error(res.msg || '批量删除失败')
          }
        }
        catch (error) {
          console.error('批量删除资源失败:', error)
          window.$message.error('批量删除失败')
        }
        finally {
          batchActionLoading.value = false
        }
      },
    })
  }

  function getBatchDeleteBlockers() {
    const checkedIds = checkedResourceIdSet.value
    return checkedResourceRows.value.filter(row =>
      collectRowDescendantIds(row).some(id => !checkedIds.has(id)),
    )
  }

  function resetSelectionAfterDelete(deletedIds) {
    const deletedIdSet = new Set(deletedIds)
    if (selectedRow.value?.id && deletedIdSet.has(selectedRow.value.id))
      selectedRow.value = null
    if (selectedResourceId.value && deletedIdSet.has(selectedResourceId.value))
      selectedResourceId.value = 0
    navigationExpandedKeys.value = navigationExpandedKeys.value.filter(key => !deletedIdSet.has(key))
    clearCheckedResources()
  }

  function openBatchMigrate() {
    if (checkedResourceIds.value.length === 0) {
      window.$message?.warning('请先选择要迁移的资源')
      return
    }
    batchMigrateParentId.value = resolveDefaultBatchMigrateParentId()
    batchMigrateVisible.value = true
  }

  function resolveDefaultBatchMigrateParentId() {
    const contextParentId = currentNode.value?.id || 0
    return isValidBatchMigrateParent(contextParentId) ? contextParentId : 0
  }

  function isValidBatchMigrateParent(parentId) {
    const normalizedParentId = Number(parentId || 0)
    if (normalizedParentId === 0)
      return true
    return !batchMigrateDisabledParentIds.value.has(normalizedParentId)
      && flatResources.value.some(row => row.id === normalizedParentId)
  }

  async function handleBatchMigrateSubmit() {
    if (checkedResourceIds.value.length === 0) {
      window.$message?.warning('请先选择要迁移的资源')
      return
    }

    const targetParentId = Number(batchMigrateParentId.value || 0)
    if (!isValidBatchMigrateParent(targetParentId)) {
      window.$message?.warning('目标上级不能是选中资源或其下级资源')
      return
    }

    const migratingIds = [...checkedResourceIds.value]
    try {
      batchActionLoading.value = true
      const res = await request.post('/system/resource/migrateBatch', {
        ids: migratingIds,
        parentId: targetParentId,
      })
      if (res.code === 200) {
        window.$message.success('批量迁移成功')
        batchMigrateVisible.value = false
        clearCheckedResources()
        await refreshSystemMenu()
        await loadResourceTree({ expandAll: false })
        focusMigratedParent(targetParentId)
      }
      else {
        window.$message.error(res.msg || '批量迁移失败')
      }
    }
    catch (error) {
      console.error('批量迁移资源失败:', error)
      window.$message.error('批量迁移失败')
    }
    finally {
      batchActionLoading.value = false
    }
  }

  function focusMigratedParent(parentId) {
    if (!parentId) {
      selectedResourceId.value = 0
      selectedRow.value = null
      return
    }

    const parent = flatResources.value.find(row => row.id === parentId)
    if (!parent)
      return
    expandResourcePath(parent)
    navigationExpandedKeys.value = [...new Set([...navigationExpandedKeys.value, parent.id])]
    selectedResourceId.value = parent.id
    selectedRow.value = parent
  }

  function getChildResourceCount(row) {
    if (!row?.children?.length)
      return 0

    let count = 0
    const walk = (children = []) => {
      children.forEach((child) => {
        count += 1
        if (child.children?.length)
          walk(child.children)
      })
    }
    walk(row.children)
    return count
  }

  async function handleDrawerSubmit() {
    try {
      await formRef.value?.validate()
      const normalized = beforeSubmit(formRef.value?.getFormData?.() || formData.value)
      if (normalized === false)
        return

      submitLoading.value = true
      const apiUrl = drawerMode.value === 'edit' ? '/system/resource/edit' : '/system/resource/add'
      const res = await request.post(apiUrl, normalized)
      if (res.code === 200) {
        window.$message.success('保存成功')
        drawerVisible.value = false
        await refreshSystemMenu()
        await loadResourceTree()
        selectSavedResource(normalized)
      }
      else {
        window.$message.error(res.msg || '保存失败')
      }
    }
    catch (error) {
      console.error('保存资源失败:', error)
      window.$message.error('保存失败')
    }
    finally {
      submitLoading.value = false
    }
  }

  function selectSavedResource(resource) {
    if (!resource?.id)
      return
    const saved = flatResources.value.find(item => item.id === resource.id)
    if (saved) {
      expandResourcePath(saved)
      selectedResourceId.value = saved.parentId || 0
      selectedRow.value = saved
    }
  }

  function expandResourcePath(resource) {
    const keys = new Set(navigationExpandedKeys.value)
    keys.add(0)
    let current = resource?.parent
    while (current) {
      keys.add(current.id)
      current = current.parent
    }
    navigationExpandedKeys.value = [...keys]
  }

  async function handleInlineUpdate(row, field, value) {
    const previousValue = row[field]
    try {
      row[field] = value
      const res = await request.post('/system/resource/edit', {
        id: row.id,
        [field]: value,
      })
      if (res.code === 200) {
        window.$message.success('更新成功')
        await refreshSystemMenu()
        await loadResourceTree()
      }
      else {
        row[field] = previousValue
        window.$message.error(res.msg || '更新失败')
      }
    }
    catch (error) {
      row[field] = previousValue
      console.error('内联更新失败:', error)
      window.$message.error('更新失败')
    }
  }

  async function refreshSystemMenu() {
    try {
      const res = await api.getMenu()
      if (res.code === 200 && res.data)
        permissionStore.setMenuData(res.data)
    }
    catch (error) {
      console.error('刷新系统菜单失败:', error)
    }
  }

  function beforeRenderForm(data) {
    formIconTab.value = isImageIconValue(data?.icon) ? 'image' : 'font'

    if (!data) {
      const parentId = pendingParentId.value !== null ? pendingParentId.value : 0
      const clientCode = pendingClientCode.value || currentClientCode.value || 'pc'
      pendingParentId.value = null
      pendingClientCode.value = null
      return { parentId, clientCode, resourceType: 2, sort: 0, visible: 1, menuStatus: 1, minUserType: 2, ssoEnabled: 0, ssoTargetClient: '', openTarget: '_self' }
    }

    pendingParentId.value = null
    pendingClientCode.value = null
    return {
      ...data,
      minUserType: data.minUserType ?? 2,
      ssoEnabled: data.ssoEnabled ?? 0,
      ssoTargetClient: data.ssoTargetClient || '',
      openTarget: data.openTarget || '_self',
    }
  }

  function beforeSubmit(sourceData) {
    const resourceType = sourceData.resourceType !== undefined ? Number(sourceData.resourceType) : sourceData.resourceType
    const ssoEnabled = resourceType === 2 ? Number(sourceData.ssoEnabled || 0) : 0
    const ssoTargetClient = ssoEnabled === 1 ? (sourceData.ssoTargetClient || '').trim() : ''
    const openTarget = resourceType === 2 && ssoEnabled === 1 ? (sourceData.openTarget || '_self') : '_self'

    if (resourceType === 2 && ssoEnabled === 1) {
      if (!ssoTargetClient) {
        window.$message.error('请选择目标子系统')
        return false
      }
      if (ssoTargetClient === sourceData.clientCode) {
        window.$message.error('目标子系统不能与当前客户端相同')
        return false
      }
    }

    return {
      ...sourceData,
      path: normalizeRouteInput(sourceData.path),
      component: normalizeComponentValue(sourceData.component),
      resourceType,
      minUserType: sourceData.minUserType !== undefined && sourceData.minUserType !== null ? Number(sourceData.minUserType) : 2,
      ssoEnabled,
      ssoTargetClient,
      openTarget,
    }
  }

  function isImageIconValue(value) {
    if (!value || typeof value !== 'string')
      return false

    const iconValue = value.trim()
    if (!iconValue)
      return false
    if (iconValue.startsWith('local-image:'))
      return false

    return iconValue.startsWith('http://')
      || iconValue.startsWith('https://')
      || iconValue.startsWith('data:')
      || iconValue.startsWith('blob:')
      || iconValue.startsWith('/api/file/')
      || iconValue.startsWith('forge-file://')
      || /^[a-f0-9]{32}$/i.test(iconValue)
      || /\.(?:png|jpe?g|webp|gif|svg|avif)(?:\?.*)?$/i.test(iconValue)
  }

  function getFontIconValue(value) {
    return isImageIconValue(value) ? '' : (value || '')
  }

  function getImageIconValue(value) {
    return isImageIconValue(value) ? value : ''
  }

  function handleFormIconTabChange(tab) {
    formIconTab.value = tab
  }

  async function openTableIconSelector(row) {
    tableIconEditRow.value = row
    tableIconValue.value = row.icon || ''
    await nextTick()
    tableIconSelectorRef.value?.open?.()
  }

  async function handleTableIconSelected(value) {
    const row = tableIconEditRow.value
    if (!row)
      return

    tableIconValue.value = value
    tableIconEditRow.value = null
    await handleInlineUpdate(row, 'icon', value)
  }

  function getUsedRoutePathSet(currentFormData) {
    return new Set(
      flatResources.value
        .filter(row => row.id !== currentFormData?.id)
        .map(row => row.path)
        .filter(Boolean),
    )
  }

  function matchesRouteKeyword(option, keyword) {
    const value = String(keyword || '').trim().toLowerCase()
    if (!value)
      return true
    return option.path.toLowerCase().includes(value)
      || option.component.toLowerCase().includes(value)
  }

  function normalizeRouteInput(routePath) {
    const value = String(routePath || '').trim()
    if (!value)
      return ''
    return value.startsWith('/') ? value.replace(/\/+/g, '/') : `/${value.replace(/\/+/g, '/')}`
  }

  function getAvailableRouteOptions(currentFormData, keyword = '') {
    const usedPathSet = getUsedRoutePathSet(currentFormData)
    const currentPath = currentFormData?.path
    return routeOptions
      .filter(option => option.path === currentPath || !usedPathSet.has(option.path))
      .filter(option => matchesRouteKeyword(option, keyword))
      .map(option => ({
        ...option,
        disabled: option.path !== currentPath && usedPathSet.has(option.path),
        label: option.path,
        value: option.path,
      }))
  }

  function renderRouteOptionLabel(option) {
    return h('div', { class: 'route-option-label' }, [
      h('span', { class: 'route-option-path' }, option.path),
    ])
  }

  function normalizeComponentValue(componentPath) {
    const value = String(componentPath || '').trim()
    if (!value)
      return ''
    return value.replace(/^\/+/, '').replace(/\/$/, '')
  }

  function handleRoutePathChange(routePath, updateValue, currentFormData) {
    const normalizedPath = normalizeRouteInput(routePath)
    if (!currentFormData) {
      updateValue(normalizedPath)
      return
    }

    currentFormData.path = normalizedPath
    autoFillComponentFromRoute(currentFormData)
    updateValue(normalizedPath)
  }

  function autoFillComponentFromRoute(currentFormData) {
    if (!currentFormData || !currentFormData.path || currentFormData.component)
      return

    const selected = routeOptions.find(option => option.path === currentFormData.path)
    if (selected?.component)
      currentFormData.component = selected.component
  }

  function getAvailableComponentOptions(_currentFormData, keyword = '') {
    return routeOptions
      .filter(option => matchesRouteKeyword(option, keyword))
      .map(option => ({
        ...option,
        label: option.component,
        value: option.component,
      }))
  }

  function renderComponentOptionLabel(option) {
    return h('div', { class: 'route-option-label' }, [
      h('span', { class: 'route-option-path' }, option.component),
      h('span', { class: 'route-option-component' }, `页面路由：${option.path}`),
    ])
  }

  // 本 part 的方法直接随返回值导出；__impl 只保留跨 part 的延迟实现。

  return {
    __impl,
    mut,
    autoFillComponentFromRoute,
    beforeRenderForm,
    beforeSubmit,
    buildBatchMigrateParentOptions,
    buildNavigationTree,
    clearCheckedResources,
    collapseNavigationTree,
    collectRowDescendantIds,
    copyText,
    expandNavigationTree,
    expandResourcePath,
    focusMigratedParent,
    getAllNavigationKeys,
    getAvailableComponentOptions,
    getAvailableRouteOptions,
    getBatchDeleteBlockers,
    getChildResourceCount,
    getClientDisplayName,
    getContextRows,
    getDisplayLevel,
    getExpandableNavigationKeys,
    getFontIconValue,
    getImageIconValue,
    getMoreActionOptions,
    getPrimaryRouteText,
    getRenderableIcon,
    getResourceSubtitle,
    getResourceTypeConfig,
    getResourceTypeText,
    getSecondaryRouteText,
    getSsoTargetClientOptions,
    getUsedRoutePathSet,
    handleAdd,
    handleAddRoot,
    handleBatchDelete,
    handleBatchMigrateSubmit,
    handleClientTabChange,
    handleDelete,
    handleDisplayRowsCheckedChange,
    handleDrawerSubmit,
    handleEdit,
    handleFormIconTabChange,
    handleInlineUpdate,
    handleMoreAction,
    handleNavigationExpandedKeys,
    handleNavigationSelect,
    handleResourceCheckedChange,
    handleRoutePathChange,
    handleSortCommit,
    handleTableIconSelected,
    hasCheckedAncestor,
    isImageIconValue,
    isValidBatchMigrateParent,
    keepSelectionAvailable,
    loadClientList,
    loadResourceDetail,
    loadResourceTree,
    matchesResourceFilter,
    matchesRouteKeyword,
    normalizeComponentValue,
    normalizeListResponse,
    normalizeRouteInput,
    openBatchMigrate,
    openTableIconSelector,
    reconcileCheckedResourceIds,
    reconcileNavigationExpandedKeys,
    refreshSystemMenu,
    renderComponentOptionLabel,
    renderNavigationLabel,
    renderRouteOptionLabel,
    resetSelectionAfterDelete,
    resolveDefaultBatchMigrateParentId,
    selectSavedResource,
    setupMenuPageLayout,
    syncParentResourceOptions,
    resourceTypeOptions,
    visibleOptions,
    apiMethodOptions,
    openTargetOptions,
    minUserTypeOptions,
    yesNoOptions,
    permissionStore,
    userStore,
    currentUserClientCode,
    routeOptions,
    pageRef,
    formRef,
    clientList,
    currentClientCode,
    loading,
    submitLoading,
    batchActionLoading,
    allResources,
    selectedResourceId,
    selectedRow,
    checkedResourceIds,
    navigationExpandedKeys,
    treeKeyword,
    resourceKeyword,
    resourceTypeFilter,
    visibleFilter,
    parentResourceOptions,
    pendingParentId,
    pendingClientCode,
    drawerVisible,
    drawerMode,
    formData,
    batchMigrateVisible,
    batchMigrateParentId,
    formIconTab,
    tableIconSelectorRef,
    tableIconEditRow,
    tableIconValue,
    publicParams,
    drawerTitle,
    drawerWidth,
    clientCodeOptions,
    resourceTypeFilterOptions,
    visibleFilterOptions,
    flatResources,
    navigationSelectedKeys,
    currentNode,
    activeResource,
    currentContextTitle,
    activeChildSummary,
    navigationTreeData,
    displayRows,
    checkedResourceIdSet,
    checkedResourceRows,
    allDisplayRowsChecked,
    displayRowsCheckIndeterminate,
    batchMigrateRootRows,
    batchMigrateDisabledParentIds,
    batchMigrateParentOptions,
  }
}

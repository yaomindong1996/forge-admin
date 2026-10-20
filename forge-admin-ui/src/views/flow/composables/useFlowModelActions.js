import { CopyOutline, CreateOutline, PauseCircleOutline, PlayCircleOutline, TimeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { h, ref } from 'vue'
import { getModelSecondaryAction } from '../utils/modelCatalog'

/** 卡片操作与请求锁同域，避免模型编排壳继续堆积菜单和状态逻辑。 */
export function useFlowModelActions({ handleEdit, handleStartTest, handleCopy, handleVersionHistory, handleSuspend, handleActivate, handleDelete, handleViewInstances }) {
  const modelActionLocks = ref(new Set())

  function modelActionKey(row, action) {
    return `${String(row?.id || row?.modelKey || '')}:${action}`
  }

  function isModelActionLocked(row, action) {
    return modelActionLocks.value.has(modelActionKey(row, action))
  }

  function isModelActionBusy(row) {
    const modelId = String(row?.id || row?.modelKey || '')
    return [...modelActionLocks.value].some(key => key.startsWith(`${modelId}:`))
  }

  function lockModelAction(row, action) {
    const key = modelActionKey(row, action)
    if (modelActionLocks.value.has(key))
      return null
    modelActionLocks.value = new Set(modelActionLocks.value).add(key)
    return key
  }

  function unlockModelAction(key) {
    if (!key)
      return
    const next = new Set(modelActionLocks.value)
    next.delete(key)
    modelActionLocks.value = next
  }

  function getActionOptions(row) {
    const renderIcon = (icon) => {
      return () => h(NIcon, null, { default: () => h(icon) })
    }
    const opts = [
      { label: '编辑信息', key: 'edit', icon: renderIcon(CreateOutline), disabled: isModelActionBusy(row) },
      { label: '版本历史', key: 'versionHistory', icon: renderIcon(TimeOutline), disabled: isModelActionBusy(row) },
      { label: '复制模型', key: 'copy', icon: renderIcon(CopyOutline), disabled: isModelActionLocked(row, 'copy') },
    ]
    if ([1, 2].includes(row.status) && getModelSecondaryAction(row)?.key !== 'instances')
      opts.push({ label: '运行记录', key: 'instances', icon: renderIcon(TimeOutline), disabled: isModelActionBusy(row) })
    if (row.status === 1) {
      opts.splice(1, 0, { label: '发起测试', key: 'startTest', icon: renderIcon(PlayCircleOutline), disabled: isModelActionBusy(row) })
      opts.push({ label: '挂起', key: 'suspend', icon: renderIcon(PauseCircleOutline), disabled: isModelActionLocked(row, 'suspend') })
    }
    if (row.status === 2) {
      opts.push({ label: '激活', key: 'activate', icon: renderIcon(PlayCircleOutline), disabled: isModelActionLocked(row, 'activate') })
    }
    opts.push({ type: 'divider', key: 'd1' })
    opts.push({ label: '删除', key: 'delete', icon: renderIcon(TrashOutline), disabled: isModelActionLocked(row, 'delete'), props: { style: 'color: #d03050' } })
    return opts
  }

  function handleActionSelect(key, row) {
    const map = { instances: handleViewInstances, edit: handleEdit, startTest: handleStartTest, copy: handleCopy, versionHistory: handleVersionHistory, suspend: handleSuspend, activate: handleActivate, delete: handleDelete }
    map[key]?.(row)
  }
  return { modelActionLocks, modelActionKey, isModelActionLocked, isModelActionBusy, lockModelAction, unlockModelAction, getActionOptions, handleActionSelect }
}

<template>
  <div class="dict-config-panel">
    <div v-if="dictItems.length === 0" class="empty-tip">
      <div>暂未检测到字典配置</div>
      <div class="empty-sub">
        在搜索/表格/编辑配置中，为字段添加 <code>dictType</code> 属性后，此处会自动提取并展示。
      </div>
    </div>
    <template v-else>
      <div class="panel-header">
        <span>检测到 {{ dictItems.length }} 个字典类型</span>
        <n-button
          type="primary"
          size="small"
          :loading="saving"
          @click="saveAllDicts"
        >
          一键保存到字典表
        </n-button>
      </div>
      <div class="dict-list">
        <div v-for="(item, idx) in dictItems" :key="item.dictType" class="dict-item">
          <div class="dict-item-header">
            <div class="dict-type-row">
              <span class="dict-type-label">字典类型:</span>
              <n-input
                v-model:value="item.dictType"
                size="small"
                style="width: 180px;"
                placeholder="dictType"
                :disabled="item.isExisting"
                @blur="checkExistingDict(item)"
              />
              <span class="dict-type-label" style="margin-left: 12px;">字典名称:</span>
              <n-input
                v-model:value="item.dictName"
                size="small"
                style="width: 150px;"
                placeholder="字典中文名称"
                :disabled="item.isExisting"
              />
              <n-tag v-if="item.saved" type="success" size="small" style="margin-left: 8px;">
                已保存
              </n-tag>
              <n-tag v-else-if="item.isExisting" type="warning" size="small" style="margin-left: 8px;">
                已存在
              </n-tag>
              <n-tag v-else type="info" size="small" style="margin-left: 8px;">
                新建
              </n-tag>
            </div>
            <div class="dict-options-header">
              <span class="dict-type-label">字典数据项（可选，留空则只创建字典类型）:</span>
              <n-button size="tiny" dashed @click="addOption(idx)">
                + 添加选项
              </n-button>
            </div>
          </div>
          <div v-if="item.isExisting" class="existing-dict-tip">
            <n-icon style="vertical-align: middle; margin-right: 4px;">
              <InformationCircleOutline />
            </n-icon>
            该字典类型已在系统中存在，保存时将同步数据项到字典表。
          </div>
          <div class="dict-options">
            <div v-for="(opt, oi) in item.options" :key="oi" class="dict-option-row">
              <n-input v-model:value="opt.dictLabel" size="small" placeholder="标签（如：正常）" style="flex: 1" />
              <n-input v-model:value="opt.dictValue" size="small" placeholder="值（如：1）" style="width: 90px" />
              <n-select
                v-model:value="opt.listClass"
                size="small"
                :options="tagTypeOptions"
                placeholder="颜色"
                style="width: 90px"
              />
              <n-button size="tiny" text type="error" @click="removeOption(idx, oi)">
                <n-icon><CloseOutline /></n-icon>
              </n-button>
            </div>
            <div v-if="item.options.length === 0" class="no-options-tip">
              暂无数据项
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { CloseOutline, InformationCircleOutline } from '@vicons/ionicons5'
import { ref, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  searchSchema: { type: String, default: '' },
  columnsSchema: { type: String, default: '' },
  editSchema: { type: String, default: '' },
  dictConfig: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const saving = ref(false)
const existingDictTypes = ref(new Set()) // 缓存已存在的字典类型
const lastEmitted = ref('') // 防止循环emit

const tagTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '成功(绿)', value: 'success' },
  { label: '信息(蓝)', value: 'info' },
  { label: '警告(橙)', value: 'warning' },
  { label: '错误(红)', value: 'error' },
]

// 从各 schema 中提取 dictType
function extractDictTypes(schemas) {
  const set = new Map()
  for (const s of schemas) {
    if (!s)
      continue
    try {
      const parsed = JSON.parse(s)
      if (!Array.isArray(parsed))
        continue
      for (const item of parsed) {
        const dt = item.dictType || item.render?.dictType
        if (dt && !set.has(dt)) {
          // 尝试从字段注释推断字典名称
          const name = item.label || item.title || dt
          set.set(dt, name)
        }
      }
    }
    catch (e) {}
  }
  return set
}

const dictItems = ref([])

/**
 * 解析 AI 生成的 dictConfig，提取字典类型和选项
 */
function parseDictConfig() {
  if (!props.dictConfig)
    return new Map()
  try {
    const parsed = JSON.parse(props.dictConfig)
    if (!Array.isArray(parsed))
      return new Map()
    const map = new Map()
    for (const item of parsed) {
      if (!item.dictType)
        continue
      map.set(item.dictType, {
        dictName: item.dictName || item.dictType,
        options: (item.items || []).map((opt, idx) => ({
          dictLabel: opt.dictLabel || '',
          dictValue: opt.dictValue !== undefined ? String(opt.dictValue) : '',
          listClass: opt.listClass || 'default',
          dictSort: opt.dictSort !== undefined ? opt.dictSort : idx + 1,
          dictStatus: opt.dictStatus !== undefined ? opt.dictStatus : 1,
        })),
      })
    }
    return map
  }
  catch (e) {
    return new Map()
  }
}

function rebuildDictItems() {
  const dtMap = extractDictTypes([
    props.searchSchema,
    props.columnsSchema,
    props.editSchema,
  ])

  // AI 生成的字典配置
  const aiDictMap = parseDictConfig()

  // 保留已有的编辑内容，只新增
  const existingMap = new Map(dictItems.value.map(d => [d.dictType, d]))
  const newItems = []
  for (const [dt, name] of dtMap) {
    if (existingMap.has(dt)) {
      const existing = existingMap.get(dt)
      const aiDict = aiDictMap.get(dt)
      // 如果已有条目 options 为空，且 AI 生成了选项，则合并 AI 数据
      if ((!existing.options || existing.options.length === 0) && aiDict?.options?.length > 0) {
        newItems.push({
          ...existing,
          dictName: aiDict.dictName || existing.dictName || name,
          options: aiDict.options,
        })
      }
      else {
        // 确保 existing 一定有 options 数组
        if (!existing.options)
          existing.options = []
        newItems.push(existing)
      }
    }
    else {
      // 优先使用 AI 生成的字典名称和选项
      const aiDict = aiDictMap.get(dt)
      newItems.push({
        dictType: dt,
        dictName: aiDict?.dictName || name,
        options: aiDict?.options || [],
        saved: false,
      })
    }
  }
  // 标记为内部更新，避免触发emit
  lastEmitted.value = props.dictConfig || ''
  dictItems.value = newItems

  // 延迟检查字典存在性并加载现有数据
  setTimeout(() => {
    newItems.forEach((item) => {
      if (item.dictType && !item.isExisting) {
        checkExistingDict(item)
      }
    })
  }, 0)
}

watch(
  [() => props.searchSchema, () => props.columnsSchema, () => props.editSchema, () => props.dictConfig],
  rebuildDictItems,
  { immediate: true },
)

// 序列化当前字典配置
function serializeDictItems() {
  const arr = dictItems.value.map((item) => {
    const obj = {
      dictType: item.dictType,
      dictName: item.dictName || item.dictType,
      items: (item.options || []).map((opt) => {
        const o = {}
        if (opt.dictLabel !== undefined && opt.dictLabel !== '')
          o.dictLabel = opt.dictLabel
        if (opt.dictValue !== undefined && opt.dictValue !== '')
          o.dictValue = opt.dictValue
        if (opt.listClass && opt.listClass !== 'default')
          o.listClass = opt.listClass
        if (opt.dictSort !== undefined)
          o.dictSort = opt.dictSort
        if (opt.dictStatus !== undefined)
          o.dictStatus = opt.dictStatus
        return o
      }).filter(o => Object.keys(o).length > 0),
    }
    return obj
  })
  return arr.length > 0 ? JSON.stringify(arr, null, 2) : ''
}

// dictItems 变化时自动emit（排除 rebuild 导致的变更）
watch(dictItems, () => {
  const serialized = serializeDictItems()
  if (serialized === lastEmitted.value)
    return
  lastEmitted.value = serialized
  emit('update:value', serialized)
}, { deep: true })

function addOption(idx) {
  const items = [...dictItems.value]
  const item = { ...items[idx] }
  if (!item.options)
    item.options = []
  item.options = [
    ...item.options,
    {
      dictLabel: '',
      dictValue: '',
      listClass: 'default',
      dictSort: item.options.length + 1,
      dictStatus: 1,
    },
  ]
  items[idx] = item
  dictItems.value = items
  console.log('[DictConfigPanel] addOption', idx, 'options count:', item.options.length)
}

function removeOption(idx, oi) {
  const items = [...dictItems.value]
  const item = { ...items[idx] }
  if (item.options) {
    item.options = item.options.filter((_, i) => i !== oi)
  }
  items[idx] = item
  dictItems.value = items
  console.log('[DictConfigPanel] removeOption', idx, oi, 'options count:', item.options?.length || 0)
}

/**
 * 检查字典类型是否存在，如存在则加载现有数据项合并到 options
 * 注意：通过 dictType 查找当前数组中的项，避免引用过期
 */
async function checkExistingDict(itemOrType) {
  const dictType = typeof itemOrType === 'string' ? itemOrType : itemOrType.dictType
  if (!dictType)
    return
  try {
    const res = await request.get('/system/dict/type/list', {
      params: { dictType },
    })
    const list = res.data || []
    const exists = list.some(d => d.dictType === dictType)

    // 查找当前 dictItems 中的对应项（避免引用过期）
    const currentItem = dictItems.value.find(d => d.dictType === dictType)
    if (currentItem) {
      currentItem.isExisting = exists
      if (exists) {
        existingDictTypes.value.add(dictType)
        // 加载该类型下的字典数据并合并
        await mergeExistingDictData(currentItem)
      }
    }
  }
  catch (e) {
    console.warn('检查字典类型失败:', e)
  }
}

/**
 * 加载数据库中已有的字典数据，合并到当前 options（保留用户编辑，补充 dictCode）
 */
async function mergeExistingDictData(item) {
  try {
    const dataRes = await request.get('/system/dict/data/list', {
      params: { dictType: item.dictType },
    })
    const dataList = dataRes.data || []
    if (dataList.length === 0)
      return

    const dbValueMap = new Map(dataList.map(d => [String(d.dictValue), d]))
    const currentOptions = item.options || []

    // 为已有 options 补充 dictCode
    const merged = currentOptions.map((opt) => {
      const dbItem = dbValueMap.get(opt.dictValue)
      if (dbItem) {
        return { ...opt, dictCode: dbItem.dictCode }
      }
      return opt
    })

    // 添加数据库中有但 options 中没有的项
    for (const d of dataList) {
      if (!merged.some(opt => opt.dictValue === String(d.dictValue))) {
        merged.push({
          dictLabel: d.dictLabel || '',
          dictValue: d.dictValue !== undefined ? String(d.dictValue) : '',
          listClass: d.listClass || 'default',
          dictSort: d.dictSort !== undefined ? d.dictSort : merged.length + 1,
          dictStatus: d.dictStatus !== undefined ? d.dictStatus : 1,
          dictCode: d.dictCode,
        })
      }
    }

    item.options = merged.sort((a, b) => (a.dictSort || 0) - (b.dictSort || 0))
  }
  catch (e) {
    console.warn('加载字典数据失败:', e)
  }
}

/**
 * 同步字典数据到字典表：新增/更新/删除
 */
async function syncDictData(item) {
  const dataRes = await request.get('/system/dict/data/list', {
    params: { dictType: item.dictType },
  })
  const existingData = dataRes.data || []
  const existingMap = new Map(existingData.map(d => [String(d.dictValue), d]))
  const optionValueSet = new Set()

  // 1. 遍历当前 options：更新或新增
  for (let i = 0; i < item.options.length; i++) {
    const opt = item.options[i]
    if (!opt.dictLabel && !opt.dictValue)
      continue

    const dictValue = opt.dictValue || String(i + 1)
    optionValueSet.add(dictValue)

    const existing = existingMap.get(dictValue)
    if (existing) {
      // 更新已有数据（标签、样式、排序变化时才请求）
      const needUpdate = existing.dictLabel !== opt.dictLabel
        || existing.listClass !== (opt.listClass || 'default')
        || (existing.dictSort || 0) !== (opt.dictSort || i + 1)
      if (needUpdate) {
        await request.post('/system/dict/data/edit', {
          dictCode: existing.dictCode,
          dictType: item.dictType,
          dictLabel: opt.dictLabel,
          dictValue,
          listClass: opt.listClass || 'default',
          dictSort: opt.dictSort || i + 1,
          dictStatus: opt.dictStatus !== undefined ? opt.dictStatus : 1,
        })
      }
    }
    else {
      // 新增数据
      await request.post('/system/dict/data/add', {
        dictType: item.dictType,
        dictLabel: opt.dictLabel,
        dictValue,
        listClass: opt.listClass || 'default',
        dictSort: opt.dictSort || i + 1,
        dictStatus: opt.dictStatus !== undefined ? opt.dictStatus : 1,
      })
    }
  }

  // 2. 删除数据库中有但 options 中没有的
  for (const d of existingData) {
    if (!optionValueSet.has(String(d.dictValue))) {
      await request.post('/system/dict/data/remove', null, {
        params: { dictCode: d.dictCode },
      })
    }
  }
}

async function saveAllDicts() {
  if (dictItems.value.length === 0)
    return
  saving.value = true
  let createCount = 0
  let syncCount = 0
  const errorMsgs = []

  for (const item of dictItems.value) {
    try {
      if (!item.isExisting) {
        // 1. 创建字典类型（仅当不存在时）
        await request.post('/system/dict/type/add', {
          dictType: item.dictType,
          dictName: item.dictName || item.dictType,
          dictStatus: 1,
          remark: 'AI CRUD 生成器自动创建',
        })
        item.isExisting = true
        existingDictTypes.value.add(item.dictType)
        createCount++
      }

      // 2. 同步字典数据（新建/已存在/已保存 都需要）
      await syncDictData(item)

      item.saved = true
      syncCount++
    }
    catch (e) {
      const msg = e?.response?.data?.msg || e?.message || '未知错误'
      errorMsgs.push(`${item.dictType}: ${msg}`)
    }
  }

  saving.value = false
  const msgParts = []
  if (createCount > 0)
    msgParts.push(`${createCount} 个类型新建成功`)
  if (syncCount > 0)
    msgParts.push(`${syncCount} 个数据同步成功`)
  if (msgParts.length > 0) {
    window.$message.success(msgParts.join('，'))
  }
  if (errorMsgs.length > 0) {
    window.$message.error(`部分保存失败: ${errorMsgs.join('；')}`)
  }
}
</script>

<style scoped>
.dict-config-panel {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
}

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.empty-sub {
  margin-top: 8px;
  font-size: 12px;
  color: #aaa;
}

.empty-sub code {
  background: #f0f0f0;
  padding: 1px 4px;
  border-radius: 3px;
  color: #e06c75;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #555;
}

.dict-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dict-item {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
}

.dict-item-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.dict-type-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.dict-type-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.dict-options-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 6px;
  border-top: 1px dashed #eee;
}

.dict-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dict-option-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.no-options-tip {
  font-size: 12px;
  color: #aaa;
  padding: 4px 0;
}

.existing-dict-tip {
  font-size: 12px;
  color: #f0a020;
  padding: 8px 0;
}
</style>

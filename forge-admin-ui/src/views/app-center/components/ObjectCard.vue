<template>
  <article class="object-card" @click="emit('open', object)">
    <div class="object-icon">
      <IconRenderer v-if="object.icon" :icon="object.icon" :size="22" />
      <n-icon v-else :component="CubeOutline" />
    </div>
    <div class="object-main">
      <div class="object-title-line">
        <h3>{{ object.objectName || object.objectCode }}</h3>
        <DictTag dict-type="sys_enable_disable" :value="object.status" :bordered="false" />
      </div>
      <p>{{ object.description || '业务单元设计和运行入口' }}</p>
      <div class="object-function-row">
        <DictTag dict-type="ai_business_object_type" :value="object.objectType" :bordered="false" />
        <span>{{ object.relationCount || 0 }} 关系</span>
        <span>{{ object.bindingCount || 0 }} 能力</span>
        <span>{{ object.appCount || 0 }} 入口</span>
      </div>
    </div>
    <div class="object-actions">
      <n-button secondary size="small" @click.stop="emit('design', object, primaryDesignPanel)">
        <template #icon>
          <n-icon><BuildOutline /></n-icon>
        </template>
        {{ primaryDesignText }}
      </n-button>
      <n-button quaternary circle size="small" aria-label="打开业务单元" @click.stop="emit('open', object)">
        <template #icon>
          <n-icon><OpenOutline /></n-icon>
        </template>
      </n-button>
      <n-dropdown trigger="click" :options="moreOptions" @select="handleMoreSelect">
        <n-button class="object-more" quaternary circle size="small" aria-label="更多操作" @click.stop>
          <template #icon>
            <n-icon><EllipsisVertical /></n-icon>
          </template>
        </n-button>
      </n-dropdown>
    </div>
  </article>
</template>

<script setup>
import { BuildOutline, CubeOutline, EllipsisVertical, OpenOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

const props = defineProps({
  object: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open', 'edit', 'design', 'stats', 'toggle', 'delete'])

const codeAppObject = computed(() => isCodeAppMeta(props.object))
const primaryDesignPanel = computed(() => codeAppObject.value ? 'flow-app' : 'form')
const primaryDesignText = computed(() => codeAppObject.value ? '业务流程配置' : '设计对象')
const moreOptions = computed(() => [
  {
    label: '编辑业务单元',
    key: 'edit',
  },
  {
    label: '业务流程配置',
    key: 'flow-app',
  },
  {
    label: '报表看板',
    key: 'stats',
  },
  {
    type: 'divider',
    key: 'divider-capability',
  },
  {
    label: props.object.status === 1 ? '停用对象' : '启用对象',
    key: 'toggle',
  },
  {
    type: 'divider',
    key: 'divider',
  },
  {
    label: '删除对象',
    key: 'delete',
  },
])

function handleMoreSelect(key) {
  if (key === 'edit') {
    emit('edit', props.object)
    return
  }
  if (key === 'flow-app') {
    emit('design', props.object, key)
    return
  }
  if (key === 'stats') {
    emit('stats', props.object)
    return
  }
  if (key === 'toggle') {
    emit('toggle', props.object)
    return
  }
  if (key === 'delete')
    emit('delete', props.object)
}

function isCodeAppMeta(object) {
  return hasCodeAppFlag(object?.options) || hasCodeAppFlag(object?.designerOptions)
}

function hasCodeAppFlag(value) {
  if (!value)
    return false
  if (typeof value === 'object')
    return value.codeApp === true
  if (typeof value !== 'string')
    return false
  const compactValue = value.replace(/\s/g, '')
  if (compactValue.includes('"codeApp":true'))
    return true
  try {
    return JSON.parse(value)?.codeApp === true
  }
  catch {
    return false
  }
}
</script>

<style scoped>
.object-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px 14px;
  align-items: start;
  min-height: 156px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.object-card:hover {
  border-color: #2f6feb;
  box-shadow: 0 10px 24px rgb(15 23 42 / 8%);
  transform: translateY(-1px);
}

.object-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #eef6ff;
  color: #2563eb;
  font-size: 22px;
}

.object-main {
  min-width: 0;
}

.object-title-line {
  display: flex;
  min-width: 0;
  gap: 10px;
  align-items: center;
}

.object-title-line h3 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-main p {
  display: -webkit-box;
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.object-function-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.object-function-row span {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  background: var(--bg-tertiary);
  line-height: 22px;
  padding: 0 8px;
}

.object-more {
  justify-self: end;
}

.object-actions {
  display: flex;
  grid-column: 2;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  border-top: 1px solid #eef2f7;
  padding-top: 10px;
}

@media (max-width: 520px) {
  .object-card {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .object-actions {
    grid-column: 2;
    justify-content: space-between;
  }
}
</style>

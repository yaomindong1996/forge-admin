<template>
  <article class="object-card" @click="emit('open', object)">
    <div class="object-icon">
      <n-icon :component="iconComponent" />
    </div>
    <div class="object-main">
      <div class="object-title-line">
        <h3>{{ object.objectName || object.objectCode }}</h3>
        <DictTag dict-type="sys_enable_disable" :value="object.status" :bordered="false" />
      </div>
      <p>{{ object.description || '业务对象设计和运行入口' }}</p>
      <div class="object-function-row">
        <DictTag dict-type="ai_business_object_type" :value="object.objectType" :bordered="false" />
        <span>{{ object.relationCount || 0 }} 关系</span>
        <span>{{ object.bindingCount || 0 }} 能力</span>
        <span>{{ object.appCount || 0 }} 入口</span>
      </div>
    </div>
    <n-button secondary size="small" @click.stop="emit('design', object)">
      <template #icon>
        <n-icon><BuildOutline /></n-icon>
      </template>
      设计对象
    </n-button>
    <n-button quaternary circle size="small" @click.stop="emit('open', object)">
      <template #icon>
        <n-icon><OpenOutline /></n-icon>
      </template>
    </n-button>
    <n-dropdown trigger="click" :options="moreOptions" @select="handleMoreSelect">
      <n-button class="object-more" quaternary circle size="small" @click.stop>
        <template #icon>
          <n-icon><EllipsisVertical /></n-icon>
        </template>
      </n-button>
    </n-dropdown>
  </article>
</template>

<script setup>
import { BuildOutline, CubeOutline, EllipsisVertical, OpenOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  object: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open', 'design', 'toggle', 'delete'])

const iconComponent = computed(() => CubeOutline)
const moreOptions = computed(() => [
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
  if (key === 'toggle') {
    emit('toggle', props.object)
    return
  }
  if (key === 'delete')
    emit('delete', props.object)
}
</script>

<style scoped>
.object-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto 34px 34px;
  gap: 12px;
  align-items: center;
  min-height: 104px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
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
  color: #111827;
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
  color: #6b7280;
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
  color: #6b7280;
  font-size: 12px;
}

.object-function-row span {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  background: #f3f4f6;
  line-height: 22px;
  padding: 0 8px;
}

.object-more {
  justify-self: end;
}

@media (max-width: 520px) {
  .object-card {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .object-card > .n-button,
  .object-card > .n-dropdown {
    grid-column: 2;
    justify-self: end;
  }
}
</style>

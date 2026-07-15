<template>
  <div class="capability-panel">
    <header>
      <div>
        <h2>{{ title }}</h2>
        <p>{{ description }}</p>
      </div>
      <span>{{ assetCount }} 项资产</span>
    </header>
    <div class="capability-body">
      <div v-for="item in items" :key="item.title" class="capability-row">
        <span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.description }}</small>
        </span>
        <n-button v-if="item.action" size="small" secondary @click="emit('action', item.action)">
          {{ item.actionLabel || '打开' }}
        </n-button>
        <span v-else class="planned">按对象能力汇总</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  assetCount: {
    type: [Number, String],
    default: 0,
  },
  items: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['action'])
</script>

<style scoped>
.capability-panel {
  display: grid;
  gap: 16px;
}

.capability-panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.capability-panel h2 {
  margin: 0;
  font-size: 18px;
}

.capability-panel p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
}

.capability-panel header > span,
.planned {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.capability-body {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.capability-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 64px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.capability-row:last-child {
  border-bottom: 0;
}

.capability-row > span:first-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.capability-row strong {
  font-size: 13px;
}

.capability-row small {
  color: var(--text-tertiary, #86909c);
}
</style>

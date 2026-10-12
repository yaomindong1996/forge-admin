<template>
  <section
    class="master-detail-workspace"
    :class="{
      'is-collapsed': collapsed,
      'is-separated': !attached,
    }"
    :style="workspaceStyle"
  >
    <aside class="master-detail-workspace__aside">
      <slot name="aside" :collapsed="collapsed" />
    </aside>
    <div v-if="attached" class="master-detail-workspace__divider" aria-hidden="true" />
    <main class="master-detail-workspace__main">
      <slot />
    </main>
  </section>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'MasterDetailWorkspace' })

const props = defineProps({
  /** 左侧主对象区域宽度。 */
  asideWidth: {
    type: [Number, String],
    default: 240,
  },
  /** 收起后左侧主对象区域宽度。 */
  collapsedAsideWidth: {
    type: [Number, String],
    default: 64,
  },
  /** 右侧工作区宽度，默认占用剩余空间。 */
  mainWidth: {
    type: [Number, String],
    default: 'minmax(0, 1fr)',
  },
  /** 是否收起左侧主对象区域。 */
  collapsed: {
    type: Boolean,
    default: false,
  },
  /** 是否以同一张工作台承载两侧内容。 */
  attached: {
    type: Boolean,
    default: true,
  },
})

const workspaceStyle = computed(() => ({
  '--master-detail-aside-width': normalizeSize(props.asideWidth),
  '--master-detail-collapsed-aside-width': normalizeSize(props.collapsedAsideWidth),
  '--master-detail-main-width': normalizeSize(props.mainWidth),
}))

function normalizeSize(value) {
  return typeof value === 'number' ? `${value}px` : value
}
</script>

<style scoped>
.master-detail-workspace {
  display: grid;
  grid-template-columns: var(--master-detail-aside-width) 1px var(--master-detail-main-width);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.master-detail-workspace.is-collapsed {
  grid-template-columns: var(--master-detail-collapsed-aside-width) 1px var(--master-detail-main-width);
}

.master-detail-workspace__aside,
.master-detail-workspace__main {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.master-detail-workspace__aside {
  background: var(--bg-primary, #fff);
}

.master-detail-workspace__divider {
  min-height: 0;
  background: var(--border-light, #e5e7eb);
}

.master-detail-workspace__main {
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #fff);
}

.master-detail-workspace.is-separated {
  grid-template-columns: var(--master-detail-aside-width) var(--master-detail-main-width);
  gap: 8px;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.master-detail-workspace.is-separated.is-collapsed {
  grid-template-columns: var(--master-detail-collapsed-aside-width) var(--master-detail-main-width);
}

:global(.dark) .master-detail-workspace {
  border-color: var(--border-light);
  background: #0f172a;
}

:global(.dark) .master-detail-workspace__aside,
:global(.dark) .master-detail-workspace__main {
  background: #0f172a;
}

:global(.dark) .master-detail-workspace__divider {
  background: #334155;
}

@media (max-width: 960px) {
  .master-detail-workspace,
  .master-detail-workspace.is-collapsed,
  .master-detail-workspace.is-separated,
  .master-detail-workspace.is-separated.is-collapsed {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(220px, 36dvh) minmax(0, 1fr);
    overflow-y: auto;
  }

  .master-detail-workspace__divider {
    display: none;
  }

  .master-detail-workspace__aside {
    border-bottom: 1px solid var(--border-light, #e5e7eb);
  }

  :global(.dark) .master-detail-workspace__aside {
    border-bottom-color: var(--border-light);
  }
}
</style>

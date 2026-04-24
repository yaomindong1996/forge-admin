<template>
  <div>
    <n-tooltip trigger="hover" placement="left">
      <template #trigger>
        <div id="layout-setting" class="f-c-c rounded-4 bg-primary p-8" @click="modalRef.open()">
          <i class="ai-icon:settings text-20 cursor-pointer bg-white" />
        </div>
      </template>
      布局设置
    </n-tooltip>

    <AiModal ref="modalRef" title="布局设置" :show-footer="false" width="700px">
      <div class="layout-grid">
        <div class="layout-item" @click="appStore.setLayout('simple')">
          <div class="layout-preview">
            <n-skeleton :width="20" :height="60" />
            <div class="ml-4">
              <n-skeleton :width="80" :height="60" />
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'simple' ? 'primary' : ''"
            ghost
            block
          >
            简约
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('normal')">
          <div class="layout-preview">
            <n-skeleton :width="20" :height="60" />
            <div class="ml-4">
              <n-skeleton :width="80" :height="10" />
              <n-skeleton class="mt-4" :width="80" :height="46" />
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'normal' ? 'primary' : ''"
            ghost
            block
          >
            通用
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('top-menu')">
          <div class="layout-preview">
            <div class="w-full">
              <n-skeleton :width="104" :height="10" />
              <div class="mt-2 flex">
                <n-skeleton :width="104" :height="46" />
              </div>
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'top-menu' ? 'primary' : ''"
            ghost
            block
          >
            顶部菜单
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('top-side-menu')">
          <div class="layout-preview">
            <div class="w-full">
              <n-skeleton :width="104" :height="10" />
              <div class="mt-2 flex">
                <n-skeleton :width="20" :height="46" />
                <div class="ml-2 flex-1">
                  <n-skeleton :width="80" :height="46" />
                </div>
              </div>
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'top-side-menu' ? 'primary' : ''"
            ghost
            block
          >
            顶部加侧面菜单
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('full')">
          <div class="layout-preview">
            <n-skeleton :width="20" :height="60" />
            <div class="ml-4">
              <n-skeleton :width="80" :height="6" />
              <n-skeleton class="mt-4" :width="80" :height="4" />
              <n-skeleton class="mt-4" :width="80" :height="42" />
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'full' ? 'primary' : ''"
            ghost
            block
          >
            全面
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('bento')">
          <div class="layout-preview">
            <div class="flex">
              <n-skeleton :width="12" :height="60" />
              <div class="ml-4 w-full">
                <n-skeleton :width="104" :height="6" />
                <n-skeleton class="mt-4" :width="104" :height="44" />
              </div>
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'bento' ? 'primary' : ''"
            ghost
            block
          >
            便当盒
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('immersive')">
          <div class="layout-preview">
            <div class="w-full">
              <n-skeleton :width="104" :height="8" />
              <n-skeleton class="mt-2" :width="104" :height="4" />
              <n-skeleton class="mt-4" :width="104" :height="42" />
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'immersive' ? 'primary' : ''"
            ghost
            block
          >
            沉浸式
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('nexus')">
          <div class="layout-preview">
            <div class="flex">
              <n-skeleton :width="20" :height="60" />
              <div class="ml-4 w-full">
                <n-skeleton :width="104" :height="8" />
                <n-skeleton class="mt-2" :width="104" :height="4" />
                <n-skeleton class="mt-4" :width="104" :height="38" />
              </div>
            </div>
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'nexus' ? 'primary' : ''"
            ghost
            block
          >
            Nexus 浮岛
          </n-button>
        </div>

        <div class="layout-item" @click="appStore.setLayout('empty')">
          <div class="layout-preview">
            <n-skeleton :width="104" :height="60" />
          </div>
          <n-button
            class="mt-12"
            size="small"
            :type="appStore.layout === 'empty' ? 'primary' : ''"
            ghost
            block
          >
            空白
          </n-button>
        </div>
      </div>

      <p class="mt-16 opacity-50">
        注: 此设置仅对未设置layout或者设置成跟随系统的页面有效，菜单设置的layout优先级最高
      </p>
    </AiModal>
  </div>
</template>

<script setup>
import { useModal } from '@/composables'
import { useAppStore } from '@/store'
import AiModal from '../ai-modal/index.vue'

const appStore = useAppStore()
const [modalRef] = useModal()
</script>

<style scoped>
.layout-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.layout-item {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  align-items: center;
}

.layout-item:hover {
  background-color: rgba(0, 0, 0, 0.02);
  border-color: rgba(0, 0, 0, 0.06);
}

.layout-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60px;
}
</style>

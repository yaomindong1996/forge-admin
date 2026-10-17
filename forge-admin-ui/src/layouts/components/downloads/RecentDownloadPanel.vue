<template>
  <section
    class="recent-downloads-panel" role="dialog" aria-label="最近下载" aria-modal="false"
    @keydown.esc.stop.prevent="$emit('close')"
  >
    <!-- 标题及低频清理操作 -->
    <header class="downloads-panel-header">
      <h2>最近下载</h2>
      <div class="downloads-panel-tools">
        <NPopconfirm
          v-if="store.canClear" positive-text="清空记录" negative-text="取消"
          @positive-click="store.clear"
        >
          <template #trigger>
            <NButton text size="tiny" class="downloads-clear" title="清空当前浏览器已加载的记录">
              清空记录
            </NButton>
          </template>
          <div class="downloads-clear-hint">
            只隐藏本浏览器已加载的记录。<br>
            不删除后台审计和本地文件，进行中的下载会保留。
          </div>
        </NPopconfirm>
        <NButton
          quaternary size="small" class="downloads-icon-button" aria-label="刷新下载记录"
          title="刷新" :disabled="store.loading" @click="store.refresh()"
        >
          <i class="i-lucide:refresh-cw" aria-hidden="true" />
        </NButton>
        <NButton
          quaternary size="small" class="downloads-icon-button" aria-label="关闭最近下载"
          title="关闭" @click="$emit('close')"
        >
          <i class="i-lucide:x" aria-hidden="true" />
        </NButton>
      </div>
    </header>
    <div v-if="store.activeCount" class="downloads-summary" role="status">
      <i class="downloads-summary-dot" /><span>{{ store.activeCount }} 个文件接收中</span>
      <span class="downloads-summary-note">可以继续操作其他页面</span>
    </div>
    <div v-else class="downloads-scope">
      {{ store.seeAll ? '本租户的下载记录' : '我的下载记录' }}
    </div>

    <!-- 独立滚动列表，不用全局 Loading 覆盖当前工作区。 -->
    <div class="downloads-panel-body" :aria-busy="store.loading">
      <div
        v-if="store.loading && !store.groups.length" class="downloads-skeleton"
        aria-label="正在加载下载记录"
      >
        <div v-for="index in 4" :key="index" class="downloads-skeleton-row">
          <NSkeleton :width="32" :height="38" :sharp="false" />
          <div><NSkeleton text width="80%" /><NSkeleton text width="55%" /></div>
        </div>
      </div>
      <div v-else-if="!store.groups.length && !store.error" class="downloads-empty">
        <div class="downloads-empty-icon">
          <i class="i-lucide:download" aria-hidden="true" />
        </div>
        <h3>暂无下载记录</h3><p>导出的报表和下载的附件<br>都可以在这里找到</p>
      </div>
      <template v-else>
        <section v-for="group in store.groups" :key="group.label" class="downloads-day-group" :aria-label="group.label">
          <h3>{{ group.label }}</h3>
          <RecentDownloadRow v-for="row in group.rows" :key="row.key" :row="row" />
        </section>
      </template>
      <div v-if="store.error" class="downloads-error" role="alert">
        <i class="i-lucide:circle-alert" aria-hidden="true" /><span>{{ store.error }}</span>
        <NButton text type="primary" size="small" @click="store.refresh()">
          重试
        </NButton>
      </div>
      <div v-if="store.hasMore" class="downloads-load-more">
        <NButton text size="small" :loading="store.loading" @click="store.refresh(true)">
          加载更多
        </NButton>
      </div>
      <p v-else-if="store.limitReached" class="downloads-history-limit">
        仅展示最近 500 条记录
      </p>
    </div>
    <footer class="downloads-panel-footer">
      <i class="i-lucide:info" aria-hidden="true" /><span>保存位置请查看浏览器的下载列表</span>
    </footer>
  </section>
</template>

<script setup>
import { NButton, NPopconfirm, NSkeleton } from 'naive-ui'
import { useRecentDownloadsStore } from '@/stores/system/recentDownloadsStore'
import RecentDownloadRow from './RecentDownloadRow.vue'

defineEmits(['close'])
const store = useRecentDownloadsStore()
</script>

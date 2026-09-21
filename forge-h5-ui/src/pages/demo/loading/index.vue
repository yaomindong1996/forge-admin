<template>
  <AiLayoutPage title="组件演示" subtitle="按需查看 Forge 移动端基础组件">
    <AiFeedbackHost />
    <template #navRight>
      <AiTag type="primary" round>{{ visibleComponents.length }} 个</AiTag>
    </template>

    <view class="demo-page">
      <view class="summary-card">
        <view class="summary-main">
          <text class="summary-title">组件库预览</text>
          <text class="summary-desc">外层只保留目录，点击组件后在弹出层查看示例。</text>
        </view>
        <AiIcon name="layers" color="#1f5fbf" size="lg" tile />
      </view>

      <scroll-view class="category-nav" scroll-x :show-scrollbar="false">
        <view class="category-track">
          <button
            v-for="item in categories"
            :key="item.key"
            class="category-button"
            :class="{ active: activeCategory === item.key }"
            @click="activeCategory = item.key"
          >
            {{ item.label }}
          </button>
        </view>
      </scroll-view>

      <view class="component-list">
        <button
          v-for="item in visibleComponents"
          :key="item.key"
          class="component-card"
          @click="openPreview(item)"
        >
          <view class="component-icon" :style="{ background: item.bg }">
            <AiIcon :name="item.icon" :color="item.color" size="md" />
          </view>
          <view class="component-copy">
            <view class="component-title-row">
              <text class="component-title">{{ item.title }}</text>
              <AiTag :type="item.tagType" variant="soft" size="sm">{{ item.categoryLabel }}</AiTag>
            </view>
            <text class="component-desc">{{ item.desc }}</text>
          </view>
          <text class="component-arrow">›</text>
        </button>
      </view>
    </view>

    <AiPopupSheet
      v-model="previewVisible"
      :title="activeComponent.title"
      :description="activeComponent.desc"
    >
      <view v-if="activeComponent.key === 'button'" class="button-stack">
        <AiButton block>主要按钮</AiButton>
        <AiButton block variant="secondary">次要按钮</AiButton>
        <AiButton block variant="outline">描边按钮</AiButton>
        <AiButton block variant="danger">危险按钮</AiButton>
        <AiButton block loading>提交中</AiButton>
      </view>

      <view v-else-if="activeComponent.key === 'tag'" class="tag-cloud">
        <AiTag type="primary">主要</AiTag>
        <AiTag type="success">成功</AiTag>
        <AiTag type="warning">提醒</AiTag>
        <AiTag type="danger">失败</AiTag>
        <AiTag type="default" variant="outline">默认</AiTag>
        <AiTag type="primary" round closable @close="handleTagClose">可关闭</AiTag>
      </view>

      <view v-else-if="activeComponent.key === 'field'" class="field-stack">
        <AiField v-model="form.nickname" label="昵称" clearable placeholder="请输入昵称">
          <template #leftIcon>
            <view class="icon-mask field-icon" :style="iconMask('/static/icons/ai-icon/user.svg', '#94a3b8')" />
          </template>
        </AiField>
        <AiField v-model="form.password" label="密码" type="password" placeholder="请输入密码">
          <template #leftIcon>
            <view class="icon-mask field-icon" :style="iconMask('/static/icons/ai-icon/lock.svg', '#94a3b8')" />
          </template>
        </AiField>
        <AiField
          v-model="form.code"
          label="验证码"
          layout="horizontal"
          placeholder="四位验证码"
          error="验证码错误或已过期"
        />
      </view>

      <view v-else-if="activeComponent.key === 'search'" class="field-stack">
        <AiSearchBar
          v-model="searchKeyword"
          placeholder="搜索菜单、消息、服务"
          @search="handleSearch"
          @clear="handleSearchClear"
          @cancel="handleSearchCancel"
        />
        <AiSearchBar
          v-model="searchKeyword"
          placeholder="常驻取消按钮"
          show-cancel
          @search="handleSearch"
          @cancel="handleSearchCancel"
        />
      </view>

      <view v-else-if="activeComponent.key === 'result'" class="result-preview">
        <AiResult
          type="success"
          title="提交成功"
          description="结果页适合提交、异常、无权限、网络错误等完整状态反馈。"
          primary-text="完成"
          secondary-text="返回"
          @primary="toast('点击了完成', { type: 'success' })"
          @secondary="toast('点击了返回')"
        />
      </view>

      <view v-else-if="activeComponent.key === 'skeleton'" class="skeleton-preview">
        <AiSkeleton type="profile" />
        <AiSkeleton class="skeleton-card" type="card" :rows="3" />
      </view>

      <view v-else-if="activeComponent.key === 'pull-list'" class="pull-list-preview">
        <AiPullList
          :list="demoPullItems"
          :loading="demoPullLoading"
          :refreshing="demoPullRefreshing"
          :finished="demoPullFinished"
          height="520rpx"
          @refresh="refreshDemoList"
          @load="loadDemoList"
          @retry="refreshDemoList"
        >
          <template #default="{ item }">
            <view class="demo-list-item">
              <view class="demo-list-icon">
                <AiIcon :name="item.icon" :color="item.color" size="sm" />
              </view>
              <view class="demo-list-copy">
                <text class="demo-list-title">{{ item.title }}</text>
                <text class="demo-list-desc">{{ item.desc }}</text>
              </view>
            </view>
          </template>
        </AiPullList>
      </view>

      <view v-else-if="activeComponent.key === 'cell'" class="cell-preview">
        <AiCellGroup>
          <AiCell title="个人信息" label="头像、昵称、联系方式" is-link clickable icon="/static/icons/ai-icon/user.svg" />
          <AiCell title="安全中心" label="登录密码、设备管理" is-link clickable icon="/static/icons/ai-icon/shield.svg" />
          <AiCell title="通知设置" value="已开启" is-link clickable icon="/static/icons/ai-icon/bell.svg" />
        </AiCellGroup>
      </view>

      <view v-else-if="activeComponent.key === 'layout'" class="layout-preview">
        <AiSection title="页面与分区" desc="统一背景、内容分区、头像、统计和空状态">
          <view class="profile-line">
            <AiAvatar size="lg" shape="square" />
            <view class="profile-copy">
              <text class="profile-title">Forge 移动端</text>
              <text class="profile-desc">用户端模板统一组件体系</text>
            </view>
          </view>
          <AiStats class="layout-stats" :items="statsItems" />
        </AiSection>
      </view>

      <view v-else-if="activeComponent.key === 'icon'" class="icon-grid">
        <view v-for="item in iconItems" :key="item.name" class="icon-tile">
          <AiIcon :name="item.name" :color="item.color" size="lg" />
          <text>{{ item.label }}</text>
        </view>
      </view>

      <view v-else-if="activeComponent.key === 'empty'" class="empty-preview">
        <AiEmpty title="暂无内容" description="空状态用于列表、消息、卡包等无数据场景。">
          <AiButton size="sm" variant="secondary">刷新</AiButton>
        </AiEmpty>
      </view>

      <view v-else-if="activeComponent.key === 'feedback'" class="button-stack">
        <AiButton block @click="showToast">轻提示 Toast</AiButton>
        <AiButton block variant="secondary" @click="showNotify">消息提示 Notify</AiButton>
        <AiButton block variant="outline" @click="showAlert">提示弹窗 Alert</AiButton>
        <AiButton block variant="danger" @click="showConfirm">确认弹窗 Confirm</AiButton>
      </view>

      <view v-else-if="activeComponent.key === 'loading'" class="loading-panel">
        <view class="loading-preview">
          <Loading visible :text="previewText" :type="currentType" theme="brand" size="md" />
        </view>
        <view class="segmented">
          <button
            v-for="item in typeOptions"
            :key="item.value"
            class="segment-button"
            :class="{ active: currentType === item.value }"
            @click="currentType = item.value"
          >
            {{ item.label }}
          </button>
        </view>
        <view class="button-stack">
          <AiButton block @click="showFullscreenLoading">全屏加载</AiButton>
          <AiButton block variant="secondary" @click="showServiceLoading">服务调用</AiButton>
        </view>
      </view>

      <view v-else-if="activeComponent.key === 'composable'" class="composable-preview">
        <AiSection title="usePageLoading" desc="统一页面首屏、刷新、失败和空状态。">
          <view class="state-line">
            <text class="state-label">当前状态</text>
            <AiTag type="primary" round>{{ pageLoading.status.value }}</AiTag>
          </view>
          <view class="button-stack compact">
            <AiButton block variant="secondary" @click="runPageLoadingDemo">模拟加载</AiButton>
            <AiButton block variant="outline" @click="pageLoading.setEmpty()">切换空状态</AiButton>
          </view>
        </AiSection>
        <AiSection title="useRequest" desc="请求状态、错误、成功提示和重复点击锁。">
          <view class="state-line">
            <text class="state-label">请求次数</text>
            <AiTag type="success" round>{{ requestCount }}</AiTag>
          </view>
          <AiButton block :loading="requestDemo.loading.value" @click="runRequestDemo">模拟请求</AiButton>
        </AiSection>
      </view>
    </AiPopupSheet>

    <Loading
      :visible="fullscreenVisible"
      text="正在加载..."
      :type="currentType"
      full-screen
      blur
      theme="brand"
      size="md"
      :z-index="12000"
    />
  </AiLayoutPage>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import AiAvatar from '@/components/AiAvatar.vue'
import AiButton from '@/components/AiButton.vue'
import AiCell from '@/components/AiCell.vue'
import AiCellGroup from '@/components/AiCellGroup.vue'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiField from '@/components/AiField.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiLayoutPage from '@/components/AiLayoutPage.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiPullList from '@/components/AiPullList.vue'
import AiResult from '@/components/AiResult.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import AiSection from '@/components/AiSection.vue'
import AiSkeleton from '@/components/AiSkeleton.vue'
import AiStats from '@/components/AiStats.vue'
import AiTag from '@/components/AiTag.vue'
import { usePageLoading, useRequest } from '@/composables'
import Loading from '@/components/Loading.vue'
import { loadingService } from '@/directives'
import { resolveStaticUrl } from '@/utils/assets'
import { showAlertDialog, showConfirmDialog } from '@/utils/dialog'
import { notify, toast } from '@/utils/notify'

const activeCategory = ref('all')
const currentType = ref('brand')
const fullscreenVisible = ref(false)
const previewVisible = ref(false)
const activeKey = ref('button')
const searchKeyword = ref('')
const demoPullLoading = ref(false)
const demoPullRefreshing = ref(false)
const demoPullFinished = ref(false)
const requestCount = ref(0)

const form = reactive({
  nickname: 'Forge 用户',
  password: '123456',
  code: 'vtra',
})

const categories = [
  { label: '全部', key: 'all' },
  { label: '基础', key: 'basic' },
  { label: '布局', key: 'layout' },
  { label: '表单', key: 'form' },
  { label: '列表', key: 'list' },
  { label: '反馈', key: 'feedback' },
  { label: '方法', key: 'composable' },
]

const componentItems = [
  {
    title: 'Button 按钮',
    key: 'button',
    category: 'basic',
    categoryLabel: '基础',
    desc: '主操作、次操作、危险操作与加载态。',
    icon: 'zap',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Tag 标签',
    key: 'tag',
    category: 'basic',
    categoryLabel: '基础',
    desc: '状态标签、描边标签、圆角标签和关闭交互。',
    icon: 'award',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Field 输入框',
    key: 'field',
    category: 'form',
    categoryLabel: '表单',
    desc: '普通输入、密码输入、错误态和横向表单。',
    icon: 'edit',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'success',
  },
  {
    title: 'SearchBar 搜索栏',
    key: 'search',
    category: 'form',
    categoryLabel: '表单',
    desc: '移动端搜索、清空、取消和搜索确认。',
    icon: 'search',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Result 结果页',
    key: 'result',
    category: 'layout',
    categoryLabel: '布局',
    desc: '提交成功、异常、无权限、404、网络错误等状态反馈。',
    icon: 'check-circle',
    color: '#10b981',
    bg: '#edf8f4',
    tagType: 'success',
  },
  {
    title: 'Skeleton 骨架屏',
    key: 'skeleton',
    category: 'layout',
    categoryLabel: '布局',
    desc: '首屏、列表和卡片加载占位，减少空白等待。',
    icon: 'loader',
    color: '#64748b',
    bg: '#f1f3f5',
    tagType: 'default',
  },
  {
    title: 'PullList 下拉列表',
    key: 'pull-list',
    category: 'list',
    categoryLabel: '列表',
    desc: '下拉刷新、上拉加载、空状态、错误重试和底部状态。',
    icon: 'list',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'success',
  },
  {
    title: 'Cell 单元格',
    key: 'cell',
    category: 'list',
    categoryLabel: '列表',
    desc: '适合设置项、个人信息和业务菜单入口。',
    icon: 'list',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Layout 布局组合',
    key: 'layout',
    category: 'layout',
    categoryLabel: '布局',
    desc: '页面背景、内容分区、头像和统计信息组合。',
    icon: 'layers',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Icon 图标',
    key: 'icon',
    category: 'layout',
    categoryLabel: '布局',
    desc: '统一 SVG mask 图标颜色、尺寸和底块样式。',
    icon: 'grid',
    color: '#d97706',
    bg: '#fff8eb',
    tagType: 'warning',
  },
  {
    title: 'Empty 空状态',
    key: 'empty',
    category: 'layout',
    categoryLabel: '布局',
    desc: '用于列表、消息、卡包等无数据场景。',
    icon: 'inbox',
    color: '#64748b',
    bg: '#f1f3f5',
    tagType: 'default',
  },
  {
    title: 'Feedback 反馈',
    key: 'feedback',
    category: 'feedback',
    categoryLabel: '反馈',
    desc: '轻提示、消息提示、Alert 和 Confirm 弹窗。',
    icon: 'bell',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
  {
    title: 'Loading 加载',
    key: 'loading',
    category: 'feedback',
    categoryLabel: '反馈',
    desc: '品牌、圆环、圆点和全屏加载状态。',
    icon: 'refresh-cw',
    color: '#059669',
    bg: '#edf8f4',
    tagType: 'success',
  },
  {
    title: 'Composables 公共方法',
    key: 'composable',
    category: 'composable',
    categoryLabel: '方法',
    desc: 'useRequest、usePageLoading 和 route 工具方法。',
    icon: 'code',
    color: '#1f5fbf',
    bg: '#edf4ff',
    tagType: 'primary',
  },
]

const statsItems = [
  { label: '菜单', value: 8 },
  { label: '权限', value: 24 },
  { label: '消息', value: 3 },
]

const iconItems = [
  { name: 'user', label: '用户', color: '#1f5fbf' },
  { name: 'shield', label: '安全', color: '#1f5fbf' },
  { name: 'bell', label: '通知', color: '#526071' },
  { name: 'zap', label: '快捷', color: '#d97706' },
]

const typeOptions = [
  { label: '品牌', value: 'brand' },
  { label: '圆环', value: 'spinner' },
  { label: '圆点', value: 'dots' },
]

const demoPullItems = ref([
  { id: 1, title: '账户通知', desc: '展示普通列表项和菜单入口。', icon: 'bell', color: '#1f5fbf' },
  { id: 2, title: '服务进度', desc: '支持下拉刷新和上拉加载。', icon: 'activity', color: '#10b981' },
  { id: 3, title: '安全提醒', desc: '内置空状态、错误状态和骨架屏。', icon: 'shield', color: '#526071' },
])

const pageLoading = usePageLoading('success')
const requestDemo = useRequest(() => new Promise((resolve) => {
  setTimeout(() => {
    requestCount.value += 1
    resolve({ ok: true, count: requestCount.value })
  }, 700)
}), {
  showSuccess: true,
  successMessage: '模拟请求完成',
  throwOnError: false,
})

const visibleComponents = computed(() => {
  if (activeCategory.value === 'all') return componentItems
  return componentItems.filter(item => item.category === activeCategory.value)
})

const activeComponent = computed(() => {
  return componentItems.find(item => item.key === activeKey.value) || componentItems[0]
})

const previewText = computed(() => {
  const typeLabel = typeOptions.find(item => item.value === currentType.value)?.label || ''
  return `${typeLabel}加载中...`
})

function openPreview(item) {
  activeKey.value = item.key
  previewVisible.value = true
}

function iconMask(icon, color) {
  const url = resolveStaticUrl(icon)
  return {
    backgroundColor: color,
    WebkitMask: `url(${url}) center / contain no-repeat`,
    mask: `url(${url}) center / contain no-repeat`,
  }
}

function handleTagClose() {
  toast('标签已关闭', { type: 'success' })
}

function handleSearch(value) {
  toast(value ? `搜索：${value}` : '请输入搜索内容')
}

function handleSearchClear() {
  toast('已清空搜索')
}

function handleSearchCancel() {
  toast('已取消搜索')
}

function refreshDemoList() {
  demoPullRefreshing.value = true
  demoPullFinished.value = false
  setTimeout(() => {
    demoPullItems.value = demoPullItems.value.slice(0, 3)
    demoPullRefreshing.value = false
    toast('列表已刷新', { type: 'success' })
  }, 800)
}

function loadDemoList() {
  demoPullLoading.value = true
  setTimeout(() => {
    const nextId = demoPullItems.value.length + 1
    demoPullItems.value = [
      ...demoPullItems.value,
      {
        id: nextId,
        title: `扩展项 ${nextId}`,
        desc: '这是上拉加载追加的列表项。',
        icon: 'plus-circle',
        color: '#d97706',
      },
    ]
    demoPullLoading.value = false
    demoPullFinished.value = demoPullItems.value.length >= 5
  }, 800)
}

function runPageLoadingDemo() {
  pageLoading.run(() => new Promise(resolve => setTimeout(() => resolve(['ok']), 700)))
    .then(() => toast('页面状态已更新', { type: 'success' }))
}

function runRequestDemo() {
  requestDemo.execute()
}

function showToast() {
  toast('这是一条轻提示', { type: 'success' })
}

function showNotify() {
  notify({
    title: '消息提示',
    description: '用于展示更完整的系统反馈信息。',
    type: 'info',
  })
}

function showAlert() {
  showAlertDialog({
    title: '提示弹窗',
    description: '这是一个用于信息确认的 Alert 弹窗。',
    icon: 'info',
    buttonText: '知道了',
  })
}

function showConfirm() {
  showConfirmDialog({
    title: '确认操作',
    description: '确认继续执行当前操作？',
    icon: 'warning',
    confirmText: '确认',
    cancelText: '取消',
  })
}

function showFullscreenLoading() {
  fullscreenVisible.value = true
  setTimeout(() => {
    fullscreenVisible.value = false
    toast('全屏加载演示完成', { type: 'success' })
  }, 1600)
}

function showServiceLoading() {
  const loading = loadingService.show({
    text: '服务加载中...',
    type: currentType.value,
    theme: 'brand',
    size: 'md',
  })
  setTimeout(() => {
    loading?.close?.()
    toast('服务调用演示完成', { type: 'success' })
  }, 1600)
}
</script>

<style lang="scss" scoped src="../../styles/loading-demo.scss"></style>

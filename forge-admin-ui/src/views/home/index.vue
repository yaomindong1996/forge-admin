<template>
  <div class="home-page">
    <!-- 顶部数据统计卡片 -->
    <div class="stats-grid">
      <div v-for="stat in statsData" :key="stat.title" class="stat-card" @click="handleStatClick(stat)">
        <div class="stat-card-inner">
          <div class="stat-header">
            <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
              <i :class="stat.icon" class="stat-icon-svg" />
            </div>
            <div class="stat-trend" :class="stat.trend === 'up' ? 'trend-up' : 'trend-down'">
              <i :class="stat.trend === 'up' ? 'ai-icon:trending-up' : 'ai-icon:trending-down'" />
              <span>{{ stat.percent }}</span>
            </div>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
            <div class="stat-desc">{{ stat.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间内容区域 -->
    <div class="content-grid">
      <!-- 左侧：访问量趋势图 -->
      <div class="chart-card chart-card-large">
        <div class="card-header">
          <div class="card-title">
            <i class="ai-icon:bar-chart-2" />
            <span>访问量趋势</span>
          </div>
          <n-button text size="small" @click="refreshVisitChart">
            <i class="ai-icon:refresh-cw" />
          </n-button>
        </div>
        <div class="card-body">
          <div ref="visitChartRef" class="chart-container"></div>
        </div>
      </div>

      <!-- 右侧：通知公告 -->
      <div class="notice-card">
        <div class="card-header">
          <div class="card-title">
            <i class="ai-icon:bell" />
            <span>通知公告</span>
            <n-badge v-if="unreadCount > 0" :value="unreadCount" :max="99" />
          </div>
          <n-button text size="small" @click="goToNoticePage">
            更多 <i class="ai-icon:arrow-right ml-4" />
          </n-button>
        </div>
        <div class="card-body">
          <n-scrollbar style="max-height: 340px">
            <div v-if="noticeList.length === 0" class="empty-state">
              <i class="ai-icon:inbox empty-icon" />
              <div class="empty-text">暂无公告</div>
            </div>
            <div v-else class="notice-list">
              <div
                v-for="notice in noticeList"
                :key="notice.noticeId"
                class="notice-item"
                @click="handleViewNotice(notice)"
              >
                <div class="notice-header">
                  <span v-if="notice.isRead === 0" class="unread-badge"></span>
                  <n-ellipsis :line-clamp="1" class="notice-title" :class="{ 'unread': notice.isRead === 0 }">
                    {{ notice.noticeTitle }}
                  </n-ellipsis>
                </div>
                <div class="notice-meta">
                  <span class="notice-time">{{ formatTime(notice.publishTime) }}</span>
                </div>
              </div>
            </div>
          </n-scrollbar>
        </div>
      </div>
    </div>

    <!-- 底部图表区域 -->
    <div class="charts-grid">
      <!-- 销售统计饼图 -->
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">
            <i class="ai-icon:pie-chart" />
            <span>销售分类统计</span>
          </div>
          <n-button text size="small" @click="refreshSalesChart">
            <i class="ai-icon:refresh-cw" />
          </n-button>
        </div>
        <div class="card-body">
          <div ref="salesChartRef" class="chart-container"></div>
        </div>
      </div>

      <!-- 用户增长柱状图 -->
      <div class="chart-card">
        <div class="card-header">
          <div class="card-title">
            <i class="ai-icon:trending-up" />
            <span>用户增长统计</span>
          </div>
          <n-button text size="small" @click="refreshUserChart">
            <i class="ai-icon:refresh-cw" />
          </n-button>
        </div>
        <div class="card-body">
          <div ref="userChartRef" class="chart-container"></div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-links-card">
      <div class="card-header">
        <div class="card-title">
          <i class="ai-icon:grid" />
          <span>快捷入口</span>
        </div>
      </div>
      <div class="card-body">
        <div class="quick-links-grid">
          <div
            v-for="item in quickLinks"
            :key="item.title"
            class="quick-link-item"
            @click="handleQuickLink(item)"
          >
            <div class="quick-link-icon" :style="{ background: item.gradient }">
              <i :class="item.icon" />
            </div>
            <div class="quick-link-title">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知详情弹窗 -->
    <n-modal
      v-model:show="showNoticeModal"
      preset="card"
      title="公告详情"
      style="width: 800px"
      :segmented="{ content: 'soft' }"
    >
      <div v-if="currentNotice" class="notice-detail">
        <div class="detail-header">
          <h3>{{ currentNotice.noticeTitle }}</h3>
          <n-space class="mt-8">
            <n-tag :type="getNoticeTypeColor(currentNotice.noticeType)" size="small">
              {{ getNoticeTypeText(currentNotice.noticeType) }}
            </n-tag>
            <span class="text-gray-400">发布人：{{ currentNotice.publisherName }}</span>
            <span class="text-gray-400">发布时间：{{ currentNotice.publishTime }}</span>
          </n-space>
        </div>
        <n-divider />
        <div class="detail-content" v-html="currentNotice.noticeContent"></div>

        <!-- 附件下载 -->
        <div v-if="currentNotice.attachments && currentNotice.attachments.length > 0" class="detail-attachments">
          <n-divider />
          <div class="attachment-title">附件</div>
          <n-space vertical>
            <div
              v-for="file in currentNotice.attachments"
              :key="file.fileId"
              class="attachment-item"
              @click="handleDownloadAttachment(file)"
            >
              <i class="i-material-symbols:attach-file" />
              <span>{{ file.fileName }}</span>
              <span class="text-gray-400">{{ formatFileSize(file.fileSize) }}</span>
              <i class="i-material-symbols:download" style="margin-left: auto" />
            </div>
          </n-space>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { request } from '@/utils'

const router = useRouter()

// 统计数据
const statsData = ref([
  {
    title: '总用户数',
    value: '8,846',
    percent: '12.5%',
    desc: '较昨日增长',
    trend: 'up',
    icon: 'ai-icon:users',
    gradient: 'linear-gradient(135deg, #4242F7 0%, #6366F1 100%)'
  },
  {
    title: '总订单数',
    value: '12,258',
    percent: '8.3%',
    desc: '较昨日增长',
    trend: 'up',
    icon: 'ai-icon:shopping-cart',
    gradient: 'linear-gradient(135deg, #5AC8FA 0%, #38BDF8 100%)'
  },
  {
    title: '总收入',
    value: '¥128,560',
    percent: '5.2%',
    desc: '较昨日下降',
    trend: 'down',
    icon: 'ai-icon:dollar-sign',
    gradient: 'linear-gradient(135deg, #6EE7B7 0%, #34D399 100%)'
  },
  {
    title: '增长率',
    value: '25.8%',
    percent: '2.1%',
    desc: '较上月增长',
    trend: 'up',
    icon: 'ai-icon:trending-up',
    gradient: 'linear-gradient(135deg, #A78BFA 0%, #8B5CF6 100%)'
  }
])

// 快捷入口
const quickLinks = ref([
  { title: '用户管理', icon: 'ai-icon:users', gradient: 'linear-gradient(135deg, #4242F7 0%, #6366F1 100%)', path: '/system/user' },
  { title: '角色管理', icon: 'ai-icon:shield', gradient: 'linear-gradient(135deg, #F472B6 0%, #EC4899 100%)', path: '/system/role' },
  { title: '菜单管理', icon: 'ai-icon:menu', gradient: 'linear-gradient(135deg, #5AC8FA 0%, #38BDF8 100%)', path: '/system/menu' },
  { title: '组织管理', icon: 'ai-icon:layers', gradient: 'linear-gradient(135deg, #6EE7B7 0%, #34D399 100%)', path: '/system/org' },
  { title: '通知公告', icon: 'ai-icon:bell', gradient: 'linear-gradient(135deg, #FBBF24 0%, #F59E0B 100%)', path: '/system/notice' },
  { title: '系统配置', icon: 'ai-icon:settings', gradient: 'linear-gradient(135deg, #A78BFA 0%, #8B5CF6 100%)', path: '/system/config' }
])

// 通知公告相关
const noticeList = ref([])
const unreadCount = ref(0)
const showNoticeModal = ref(false)
const currentNotice = ref(null)

// 图表引用
const visitChartRef = ref(null)
const salesChartRef = ref(null)
const userChartRef = ref(null)

// 加载通知公告列表
async function loadNoticeList() {
  try {
    const res = await request.get('/system/notice/user/page', {
      params: { pageNum: 1, pageSize: 5 }
    })
    if (res.code === 200) {
      noticeList.value = res.data.records || []
    }
  } catch (error) {
    console.error('加载通知公告失败:', error)
  }
}

// 加载未读数量
async function loadUnreadCount() {
  try {
    const res = await request.get('/system/notice/user/unread-count')
    if (res.code === 200) {
      unreadCount.value = res.data
    }
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 查看通知详情
async function handleViewNotice(notice) {
  try {
    const res = await request.post('/system/notice/getById', null, {
      params: { noticeId: notice.noticeId }
    })
    if (res.code === 200) {
      currentNotice.value = res.data
      showNoticeModal.value = true

      // 标记为已读
      if (notice.isRead === 0) {
        await markAsRead(notice.noticeId)
      }
    }
  } catch (error) {
    window.$message.error('获取详情失败')
  }
}

// 标记为已读
async function markAsRead(noticeId) {
  try {
    await request.post('/system/notice/markAsRead', null, {
      params: { noticeId }
    })
    loadNoticeList()
    loadUnreadCount()
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 跳转到通知公告页面
function goToNoticePage() {
  router.push('/system/notice-list')
}

// 快捷入口点击
function handleQuickLink(item) {
  if (item.path) {
    router.push(item.path)
  }
}

// 统计卡片点击
function handleStatClick(stat) {
  console.log('Stat clicked:', stat.title)
}

// 刷新图表
function refreshVisitChart() {
  initVisitChart()
}

function refreshSalesChart() {
  initSalesChart()
}

function refreshUserChart() {
  initUserChart()
}

// 下载附件
function handleDownloadAttachment(file) {
  try {
    const downloadUrl = `/api/system/file/download?fileId=${file.fileId}`
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = file.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.$message.success('开始下载')
  } catch (error) {
    window.$message.error('下载失败')
  }
}

// 格式化文件大小
function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化时间
function formatTime(time) {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'

  return time.split(' ')[0]
}

// 获取公告类型文本
function getNoticeTypeText(type) {
  const typeMap = {
    'NOTICE': '通知公告',
    'ANNOUNCEMENT': '系统公告',
    'NEWS': '新闻动态'
  }
  return typeMap[type] || type
}

// 获取公告类型颜色
function getNoticeTypeColor(type) {
  const colorMap = {
    'NOTICE': 'info',
    'ANNOUNCEMENT': 'warning',
    'NEWS': 'success'
  }
  return colorMap[type] || 'default'
}

// 初始化访问量趋势图
function initVisitChart() {
  const chart = echarts.init(visitChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B'
      },
      padding: [12, 16],
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(59, 130, 246, 0.1)'
        }
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisTick: {
        show: false
      },
      axisLine: {
        lineStyle: {
          color: '#E2E8F0'
        }
      },
      axisLabel: {
        color: '#64748B',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#F1F5F9',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#64748B',
        fontSize: 12
      }
    },
    series: [
      {
        name: '访问量',
        type: 'bar',
        barWidth: '50%',
        data: [320, 502, 301, 434, 590, 530, 420],
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#4242F7' },
            { offset: 1, color: '#6366F1' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#5A5AF7' },
              { offset: 1, color: '#4242F7' }
            ])
          }
        }
      }
    ]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 初始化销售统计饼图
function initSalesChart() {
  const chart = echarts.init(salesChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B'
      },
      padding: [12, 16],
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '5%',
      left: 'center',
      textStyle: {
        color: '#64748B',
        fontSize: 12
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16
    },
    series: [
      {
        name: '销售额',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
            color: '#1E293B'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { 
            value: 1048, 
            name: '电子产品',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#4242F7' },
                { offset: 1, color: '#6366F1' }
              ])
            }
          },
          { 
            value: 735, 
            name: '服装鞋帽',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#5AC8FA' },
                { offset: 1, color: '#38BDF8' }
              ])
            }
          },
          { 
            value: 580, 
            name: '食品饮料',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#6EE7B7' },
                { offset: 1, color: '#34D399' }
              ])
            }
          },
          { 
            value: 484, 
            name: '图书文具',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#A78BFA' },
                { offset: 1, color: '#8B5CF6' }
              ])
            }
          },
          { 
            value: 300, 
            name: '其他',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#FBBF24' },
                { offset: 1, color: '#F59E0B' }
              ])
            }
          }
        ]
      }
    ]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 初始化用户增长柱状图
function initUserChart() {
  const chart = echarts.init(userChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B'
      },
      padding: [12, 16],
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: '#CBD5E1',
          type: 'dashed'
        }
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisTick: {
        show: false
      },
      axisLine: {
        lineStyle: {
          color: '#E2E8F0'
        }
      },
      axisLabel: {
        color: '#64748B',
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: {
          color: '#F1F5F9',
          type: 'dashed'
        }
      },
      axisLabel: {
        color: '#64748B',
        fontSize: 12
      }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: [820, 932, 901, 934, 1290, 1330],
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#6EE7B7' },
            { offset: 1, color: '#34D399' }
          ])
        },
        itemStyle: {
          color: '#34D399',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(110, 231, 183, 0.25)' },
            { offset: 1, color: 'rgba(52, 211, 153, 0.02)' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: '#34D399',
            borderColor: '#fff',
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: 'rgba(52, 211, 153, 0.4)'
          }
        }
      }
    ]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => {
  loadNoticeList()
  loadUnreadCount()

  nextTick(() => {
    initVisitChart()
    initSalesChart()
    initUserChart()
  })
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Fira+Sans:wght@400;500;600;700&display=swap');

/* ===== 首页布局 ===== */
.home-page {
  padding: 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  min-height: 100%;
  overflow-y: auto;
  font-family: 'Inter', sans-serif;
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--stat-gradient);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
  border-color: rgba(203, 213, 225, 0.6);
}

.stat-card:hover::before {
  opacity: 1;
}

.stat-card-inner {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 26px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover .stat-icon-wrapper {
  transform: scale(1.08) rotate(3deg);
}

.stat-icon-svg {
  font-size: 26px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
  transition: transform 0.2s ease;
}

.stat-card:hover .stat-trend {
  transform: scale(1.05);
}

.trend-up {
  color: #059669;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.trend-down {
  color: #dc2626;
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  font-family: 'Fira Sans', sans-serif;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.stat-title {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.stat-desc {
  font-size: 12px;
  color: #94a3b8;
}

/* ===== 内容网格 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

/* ===== 图表网格 ===== */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

/* ===== 通用卡片 ===== */
.chart-card,
.notice-card,
.quick-links-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.chart-card:hover,
.notice-card:hover,
.quick-links-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  border-color: rgba(203, 213, 225, 0.6);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, transparent 100%);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-title i {
  font-size: 20px;
  color: #4242F7;
}

.card-body {
  padding: 24px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

/* ===== 通知列表 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
  opacity: 0.25;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notice-item {
  padding: 14px 18px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  position: relative;
}

.notice-item:hover {
  background: linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 100%);
  border-color: rgba(99, 102, 241, 0.2);
  transform: translateX(4px);
}

.notice-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 6px;
}

.unread-badge {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4242F7, #6366F1);
  flex-shrink: 0;
  margin-top: 5px;
  animation: pulse 2s ease-in-out infinite;
  box-shadow: 0 0 10px rgba(66, 66, 247, 0.5);
}

.notice-title {
  flex: 1;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

.notice-title.unread {
  color: #0f172a;
  font-weight: 600;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 18px;
}

.notice-time {
  font-size: 12px;
  color: #94a3b8;
}

/* ===== 公告详情 ===== */
.notice-detail {
  padding: 8px 0;
}

.detail-header h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #0f172a;
  font-weight: 700;
  font-family: 'Inter', sans-serif;
}

.detail-content {
  line-height: 1.8;
  color: #475569;
  min-height: 80px;
  font-size: 14px;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  margin: 12px 0;
}

.detail-attachments {
  margin-top: 24px;
}

.attachment-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 14px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f8fafc;
  font-size: 14px;
  color: #64748b;
}

.attachment-item:hover {
  background: linear-gradient(135deg, #EEF2FF 0%, #C7D2FE 100%);
  border-color: #A5B4FC;
  color: #4242F7;
  transform: translateX(4px);
}

/* ===== 快捷入口 ===== */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 22px 14px;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6) 0%, transparent 100%);
}

.quick-link-item:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(203, 213, 225, 0.5);
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.05);
}

.quick-link-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.quick-link-item:hover .quick-link-icon {
  transform: scale(1.12) rotate(-3deg);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.quick-link-title {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  text-align: center;
}

/* ===== 响应式 ===== */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .quick-links-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .stat-card {
    padding: 18px;
  }

  .stat-value {
    font-size: 24px;
  }
  
  .stat-icon-wrapper {
    width: 48px;
    height: 48px;
    font-size: 22px;
  }

  .quick-links-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .quick-link-item {
    padding: 16px 10px;
  }

  .quick-link-icon {
    width: 48px;
    height: 48px;
    font-size: 22px;
  }

  .chart-container {
    height: 260px;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .quick-links-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ===== 动画 ===== */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

/* ===== 减少动画（无障碍支持）===== */
@media (prefers-reduced-motion: reduce) {
  .stat-card:hover,
  .notice-item:hover,
  .quick-link-item:hover,
  .attachment-item:hover {
    transform: none;
  }

  .unread-badge {
    animation: none;
  }
  
  .stat-icon-wrapper,
  .quick-link-icon,
  .stat-trend {
    transition: none;
  }
}

/* ===== 暗色模式 ===== */
.dark .home-page {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
}

.dark .stat-card,
.dark .chart-card,
.dark .notice-card,
.dark .quick-links-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .stat-card:hover,
.dark .chart-card:hover,
.dark .notice-card:hover,
.dark .quick-links-card:hover {
  border-color: rgba(71, 85, 105, 0.6);
}

.dark .stat-value {
  color: #f1f5f9;
}

.dark .stat-title,
.dark .card-title {
  color: #e2e8f0;
}

.dark .stat-desc,
.dark .notice-time,
.dark .quick-link-title {
  color: #94a3b8;
}

.dark .notice-item:hover {
  background: rgba(67, 56, 202, 0.2);
  border-color: rgba(99, 102, 241, 0.3);
}

.dark .quick-link-item {
  background: rgba(30, 41, 59, 0.3);
  border-color: rgba(51, 65, 85, 0.3);
}

.dark .quick-link-item:hover {
  background: rgba(30, 41, 59, 0.6);
}

.dark .card-header {
  background: transparent;
  border-bottom-color: rgba(51, 65, 85, 0.4);
}

.dark .card-title i {
  color: #6366F1;
}

.dark .attachment-item {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .attachment-item:hover {
  background: rgba(67, 56, 202, 0.2);
  border-color: rgba(99, 102, 241, 0.3);
}

.dark .notice-title.unread {
  color: #f1f5f9;
}

.dark .notice-title {
  color: #94a3b8;
}

.dark .unread-badge {
  background: linear-gradient(135deg, #6366F1, #818CF8);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.4);
}
</style>

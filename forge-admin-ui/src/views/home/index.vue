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
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  },
  {
    title: '总订单数',
    value: '12,258',
    percent: '8.3%',
    desc: '较昨日增长',
    trend: 'up',
    icon: 'ai-icon:shopping-cart',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    title: '总收入',
    value: '¥128,560',
    percent: '5.2%',
    desc: '较昨日下降',
    trend: 'down',
    icon: 'ai-icon:dollar-sign',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    title: '增长率',
    value: '25.8%',
    percent: '2.1%',
    desc: '较上月增长',
    trend: 'up',
    icon: 'ai-icon:trending-up',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

// 快捷入口
const quickLinks = ref([
  { title: '用户管理', icon: 'ai-icon:users', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', path: '/system/user' },
  { title: '角色管理', icon: 'ai-icon:shield', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', path: '/system/role' },
  { title: '菜单管理', icon: 'ai-icon:menu', gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', path: '/system/menu' },
  { title: '组织管理', icon: 'ai-icon:layers', gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', path: '/system/org' },
  { title: '通知公告', icon: 'ai-icon:bell', gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', path: '/system/notice' },
  { title: '系统配置', icon: 'ai-icon:settings', gradient: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)', path: '/system/config' }
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
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#7c8ff0' },
              { offset: 1, color: '#8b5ab8' }
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
                { offset: 0, color: '#667eea' },
                { offset: 1, color: '#764ba2' }
              ])
            }
          },
          { 
            value: 735, 
            name: '服装鞋帽',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#f093fb' },
                { offset: 1, color: '#f5576c' }
              ])
            }
          },
          { 
            value: 580, 
            name: '食品饮料',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#4facfe' },
                { offset: 1, color: '#00f2fe' }
              ])
            }
          },
          { 
            value: 484, 
            name: '图书文具',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#43e97b' },
                { offset: 1, color: '#38f9d7' }
              ])
            }
          },
          { 
            value: 300, 
            name: '其他',
            itemStyle: { 
              color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [
                { offset: 0, color: '#fa709a' },
                { offset: 1, color: '#fee140' }
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
            { offset: 0, color: '#43e97b' },
            { offset: 1, color: '#38f9d7' }
          ])
        },
        itemStyle: {
          color: '#43e97b',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(67, 233, 123, 0.3)' },
            { offset: 1, color: 'rgba(56, 249, 215, 0.05)' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: '#43e97b',
            borderColor: '#fff',
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: 'rgba(67, 233, 123, 0.5)'
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
@import url('https://fonts.googleapis.com/css2?family=Fira+Code:wght@400;500;600;700&family=Fira+Sans:wght@300;400;500;600;700&display=swap');

.home-page {
  padding: 24px;
  background: #F8FAFC;
  min-height: 100vh;
  font-family: 'Fira Sans', sans-serif;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #E2E8F0;
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
  background: var(--gradient);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: transparent;
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
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.stat-icon-svg {
  font-size: 28px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 20px;
}

.trend-up {
  color: #10b981;
  background: #d1fae5;
}

.trend-down {
  color: #ef4444;
  background: #fee2e2;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1E293B;
  font-family: 'Fira Code', monospace;
  line-height: 1;
}

.stat-title {
  font-size: 14px;
  color: #64748B;
  font-weight: 500;
}

.stat-desc {
  font-size: 13px;
  color: #94A3B8;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

/* 卡片样式 */
.chart-card,
.notice-card,
.quick-links-card {
  background: white;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  overflow: hidden;
  transition: all 0.3s ease;
}

.chart-card:hover,
.notice-card:hover,
.quick-links-card:hover {
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border-color: #CBD5E1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #F1F5F9;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.card-title i {
  font-size: 20px;
  color: #3B82F6;
}

.card-body {
  padding: 24px;
}

.chart-container {
  height: 320px;
  width: 100%;
}

/* 通知列表 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94A3B8;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  background: #F8FAFC;
}

.notice-item:hover {
  background: #F1F5F9;
  border-color: #E2E8F0;
  transform: translateX(4px);
}

.notice-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.unread-badge {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3B82F6;
  flex-shrink: 0;
  margin-top: 6px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.notice-title {
  flex: 1;
  font-size: 14px;
  color: #475569;
  line-height: 1.5;
}

.notice-title.unread {
  color: #1E293B;
  font-weight: 600;
}

.notice-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}

.notice-time {
  color: #94A3B8;
}

/* 公告详情 */
.notice-detail {
  padding: 16px 0;
}

.detail-header h3 {
  margin: 0 0 12px 0;
  font-size: 20px;
  color: #1E293B;
}

.detail-content {
  line-height: 1.8;
  color: #475569;
  min-height: 100px;
  font-size: 15px;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 12px 0;
}

.detail-attachments {
  margin-top: 24px;
}

.attachment-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #1E293B;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #F8FAFC;
}

.attachment-item:hover {
  background: #F1F5F9;
  border-color: #3B82F6;
  transform: translateX(4px);
}

/* 快捷入口 */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #E2E8F0;
  background: #F8FAFC;
}

.quick-link-item:hover {
  background: white;
  border-color: transparent;
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.quick-link-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.quick-link-item:hover .quick-link-icon {
  transform: scale(1.1);
}

.quick-link-title {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
  text-align: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .stat-card {
    padding: 20px;
  }

  .stat-value {
    font-size: 28px;
  }

  .quick-links-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;
  }

  .quick-link-item {
    padding: 20px 12px;
  }

  .quick-link-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .chart-container {
    height: 260px;
  }
}

/* 动画优化 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .stat-card:hover,
  .notice-item:hover,
  .quick-link-item:hover {
    transform: none;
  }

  .unread-badge {
    animation: none;
  }
}
</style>

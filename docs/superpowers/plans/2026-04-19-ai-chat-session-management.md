# AI 会话管理页面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 开发管理员使用的 AI 会话管理页面，支持查看所有用户会话、聊天记录（类IM风格）和核心统计图表。

**Architecture:** 后端新增管理端 Controller + Mapper XML 自定义分页 SQL（JOIN sys_user），前端单页面集成统计卡片 + 趋势图 + 左右分栏会话管理。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus（自定义XML分页） + Sa-Token | Vue 3 + Naive UI + ECharts + Markdown渲染

---

## Task 1: 后端 DTO/VO 类

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/dto/AiSessionPageQuery.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/vo/AiSessionVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/vo/AiSessionStatisticsVO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/vo/DailyTrendItem.java`

- [ ] **Step 1: 创建 AiSessionPageQuery**

```java
package com.mdframe.forge.plugin.ai.session.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiSessionPageQuery extends PageQuery {

    private String keyword;

    private String startTime;

    private String endTime;

    private String status;
}
```

- [ ] **Step 2: 创建 AiSessionVO**

```java
package com.mdframe.forge.plugin.ai.session.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiSessionVO {

    private String id;

    private Long userId;

    private String nickName;

    private String avatar;

    private String sessionName;

    private String agentCode;

    private String status;

    private Integer messageCount;

    private Long tokenUsage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建 DailyTrendItem**

```java
package com.mdframe.forge.plugin.ai.session.vo;

import lombok.Data;

@Data
public class DailyTrendItem {

    private String date;

    private Long sessionCount;

    private Long messageCount;
}
```

- [ ] **Step 4: 创建 AiSessionStatisticsVO**

```java
package com.mdframe.forge.plugin.ai.session.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiSessionStatisticsVO {

    private Long totalSessions;

    private Long totalMessages;

    private Long todaySessions;

    private Long totalTokenUsage;

    private List<DailyTrendItem> dailyTrend;
}
```

- [ ] **Step 5: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/dto/
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/vo/
git commit -m "[ai-session] 新增会话管理DTO/VO类"
```

---

## Task 2: Mapper XML + Mapper 接口扩展

**Files:**
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/mapper/AiChatSessionMapper.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiChatSessionMapper.xml`

- [ ] **Step 1: 扩展 AiChatSessionMapper 接口**

在现有接口中添加 3 个自定义方法：

```java
package com.mdframe.forge.plugin.ai.session.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    Page<AiSessionVO> selectSessionPage(Page<?> page, @Param("query") AiSessionPageQuery query);

    AiSessionStatisticsVO selectStatistics();

    List<DailyTrendItem> selectDailyTrend();
}
```

- [ ] **Step 2: 创建 Mapper XML**

文件路径：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiChatSessionMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mdframe.forge.plugin.ai.session.mapper.AiChatSessionMapper">

    <resultMap id="sessionVOResultMap" type="com.mdframe.forge.plugin.ai.session.vo.AiSessionVO">
        <id property="id" column="id"/>
        <result property="userId" column="user_id"/>
        <result property="nickName" column="nick_name"/>
        <result property="avatar" column="avatar"/>
        <result property="sessionName" column="session_name"/>
        <result property="agentCode" column="agent_code"/>
        <result property="status" column="status"/>
        <result property="messageCount" column="message_count"/>
        <result property="tokenUsage" column="token_usage"/>
        <result property="createTime" column="create_time"/>
        <result property="updateTime" column="update_time"/>
    </resultMap>

    <select id="selectSessionPage" resultMap="sessionVOResultMap">
        SELECT s.id, s.user_id, u.real_name AS nick_name, u.avatar,
               s.session_name, s.agent_code, s.status,
               (SELECT COUNT(*) FROM ai_chat_record r WHERE r.session_id = s.id) AS message_count,
               (SELECT COALESCE(SUM(r.token_usage), 0) FROM ai_chat_record r WHERE r.session_id = s.id) AS token_usage,
               s.create_time, s.update_time
        FROM ai_chat_session s
        LEFT JOIN sys_user u ON s.user_id = u.id
        <where>
            <if test="query.keyword != null and query.keyword != ''">
                AND (u.real_name LIKE CONCAT('%', #{query.keyword}, '%') OR s.session_name LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.startTime != null and query.startTime != ''">
                AND s.create_time &gt;= #{query.startTime}
            </if>
            <if test="query.endTime != null and query.endTime != ''">
                AND s.create_time &lt;= CONCAT(#{query.endTime}, ' 23:59:59')
            </if>
            <if test="query.status != null and query.status != ''">
                AND s.status = #{query.status}
            </if>
        </where>
        ORDER BY s.update_time DESC
    </select>

    <select id="selectStatistics" resultType="com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO">
        SELECT
            (SELECT COUNT(*) FROM ai_chat_session WHERE status = '0') AS totalSessions,
            (SELECT COUNT(*) FROM ai_chat_record) AS totalMessages,
            (SELECT COUNT(*) FROM ai_chat_session WHERE status = '0' AND DATE(create_time) = CURDATE()) AS todaySessions,
            (SELECT COALESCE(SUM(token_usage), 0) FROM ai_chat_record) AS totalTokenUsage
    </select>

    <select id="selectDailyTrend" resultType="com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem">
        SELECT
            d.date,
            COALESCE(s.cnt, 0) AS sessionCount,
            COALESCE(r.cnt, 0) AS messageCount
        FROM (
            SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n DAY), '%Y-%m-%d') AS date
            FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                  UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
                  UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
                  UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
                  UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
                  UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29) nums
        ) d
        LEFT JOIN (
            SELECT DATE(create_time) AS dt, COUNT(*) AS cnt
            FROM ai_chat_session
            WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
            GROUP BY DATE(create_time)
        ) s ON d.date = s.dt
        LEFT JOIN (
            SELECT DATE(create_time) AS dt, COUNT(*) AS cnt
            FROM ai_chat_record
            WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
            GROUP BY DATE(create_time)
        ) r ON d.date = r.dt
        ORDER BY d.date
    </select>

</mapper>
```

- [ ] **Step 3: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/mapper/AiChatSessionMapper.java
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/mapper/AiChatSessionMapper.xml
git commit -m "[ai-session] 新增Mapper自定义分页SQL和统计SQL"
```

---

## Task 3: Service 层扩展

**Files:**
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/service/AiChatSessionService.java`

- [ ] **Step 1: 在 AiChatSessionService 中新增管理端方法**

在现有类中追加以下方法（保留现有方法不变）：

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.plugin.ai.session.vo.DailyTrendItem;

// 新增以下方法到 AiChatSessionService 类中

public Page<AiSessionVO> adminPage(AiSessionPageQuery query) {
    return baseMapper.selectSessionPage(query.toPage(), query);
}

public AiSessionStatisticsVO getStatistics() {
    AiSessionStatisticsVO stats = baseMapper.selectStatistics();
    if (stats == null) {
        stats = new AiSessionStatisticsVO();
        stats.setTotalSessions(0L);
        stats.setTotalMessages(0L);
        stats.setTodaySessions(0L);
        stats.setTotalTokenUsage(0L);
    }
    List<DailyTrendItem> trend = baseMapper.selectDailyTrend();
    stats.setDailyTrend(trend);
    return stats;
}
```

- [ ] **Step 2: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/session/service/AiChatSessionService.java
git commit -m "[ai-session] Service新增管理端分页和统计方法"
```

---

## Task 4: 管理端 Controller

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/admin/controller/AiSessionAdminController.java`

- [ ] **Step 1: 创建 AiSessionAdminController**

```java
package com.mdframe.forge.plugin.ai.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.dto.AiSessionPageQuery;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionStatisticsVO;
import com.mdframe.forge.plugin.ai.session.vo.AiSessionVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/admin/session")
@RequiredArgsConstructor
public class AiSessionAdminController {

    private final AiChatSessionService sessionService;
    private final AiChatRecordService recordService;

    @GetMapping("/page")
    public RespInfo<Page<AiSessionVO>> page(AiSessionPageQuery query) {
        return RespInfo.success(sessionService.adminPage(query));
    }

    @GetMapping("/{sessionId}/messages")
    public RespInfo<List<AiChatRecord>> messages(@PathVariable String sessionId) {
        return RespInfo.success(recordService.listBySession(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public RespInfo<Void> delete(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return RespInfo.success();
    }

    @GetMapping("/statistics")
    public RespInfo<AiSessionStatisticsVO> statistics() {
        return RespInfo.success(sessionService.getStatistics());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/admin/controller/AiSessionAdminController.java
git commit -m "[ai-session] 新增管理端Controller"
```

---

## Task 5: 菜单权限 SQL

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/sql/ai_session_admin.sql`

- [ ] **Step 1: 创建菜单权限 SQL**

```sql
-- =============================================
-- AI 会话管理菜单及权限
-- =============================================

-- 会话管理菜单（挂在 AI管理 目录下）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
SELECT 0, '会话管理', id, 2, 3, '/ai/session', '/ai/session', 0, 0, 1, 1, 'mdi:chat-processing', 0, 0, 'AI会话管理', NOW(), NOW()
FROM sys_resource WHERE tenant_id = 0 AND path = '/ai' AND resource_type = 1
LIMIT 1;

SET @ai_session_menu_id = LAST_INSERT_ID();

-- 会话管理按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES
(0, '会话查看', @ai_session_menu_id, 3, 1, 'ai:session:query', 0, 0, 1, 1, '查看AI会话', NOW(), NOW()),
(0, '会话删除', @ai_session_menu_id, 3, 2, 'ai:session:delete', 0, 0, 1, 1, '删除AI会话', NOW(), NOW());

-- 给超级管理员角色分配权限
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource WHERE tenant_id = 0 AND id >= @ai_session_menu_id;
```

- [ ] **Step 2: Commit**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/resources/sql/ai_session_admin.sql
git commit -m "[ai-session] 新增会话管理菜单权限SQL"
```

---

## Task 6: 前端 API 接口

**Files:**
- Modify: `forge-admin-ui/src/api/ai.js`

- [ ] **Step 1: 在 ai.js 末尾追加会话管理 API**

```js
// ========== 会话管理 ==========

export function sessionPage(params) {
  return request.get('/ai/admin/session/page', { params })
}

export function sessionMessages(sessionId) {
  return request.get(`/ai/admin/session/${sessionId}/messages`)
}

export function sessionDelete(sessionId) {
  return request.delete(`/ai/admin/session/${sessionId}`)
}

export function sessionStatistics() {
  return request.get('/ai/admin/session/statistics')
}
```

- [ ] **Step 2: Commit**

```bash
git add forge-admin-ui/src/api/ai.js
git commit -m "[ai-session] 前端新增会话管理API"
```

---

## Task 7: 前端会话管理页面

**Files:**
- Create: `forge-admin-ui/src/views/ai/session.vue`

这是核心前端页面，包含统计卡片、趋势图、左右分栏会话管理。

- [ ] **Step 1: 创建 session.vue 完整页面**

```vue
<template>
  <div class="ai-session-page">
    <!-- 统计卡片区 -->
    <div class="stats-grid">
      <div v-for="stat in statsCards" :key="stat.label" class="stat-card">
        <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
          <i :class="stat.icon" class="stat-icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 趋势图 -->
    <n-card class="trend-card" :bordered="false">
      <div class="card-header">
        <span class="card-title">近 30 天趋势</span>
        <n-button text size="small" @click="loadStatistics">
          <i class="i-material-symbols:refresh" />
        </n-button>
      </div>
      <div ref="trendChartRef" class="trend-chart" />
    </n-card>

    <!-- 左右分栏 -->
    <div class="session-container">
      <!-- 左侧：会话列表 -->
      <div class="session-list-panel">
        <div class="list-header">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索用户/会话标题"
            clearable
            size="small"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <i class="i-material-symbols:search" />
            </template>
          </n-input>
          <n-date-picker
            v-model:value="dateRange"
            type="daterange"
            size="small"
            clearable
            :style="{ width: '100%', marginTop: '8px' }"
            @update:value="handleSearch"
          />
        </div>

        <n-scrollbar class="session-list-scroll">
          <div v-if="sessionList.length === 0" class="empty-session">
            <n-empty description="暂无会话" />
          </div>
          <div
            v-for="item in sessionList"
            :key="item.id"
            class="session-item"
            :class="{ active: currentSession?.id === item.id }"
            @click="selectSession(item)"
          >
            <AuthImage
              :src="item.avatar || ''"
              :fallback="''"
              img-class="session-avatar"
              :img-style="{ width: '36px', height: '36px', borderRadius: '50%', objectFit: 'cover' }"
            />
            <div v-if="!item.avatar" class="session-avatar-placeholder">
              {{ (item.nickName || '?').charAt(0) }}
            </div>
            <div class="session-info">
              <div class="session-top-row">
                <span class="session-nick">{{ item.nickName || '未知用户' }}</span>
                <span class="session-time">{{ formatTime(item.updateTime) }}</span>
              </div>
              <div class="session-bottom-row">
                <n-ellipsis class="session-title" :line-clamp="1">{{ item.sessionName || '新对话' }}</n-ellipsis>
                <span class="session-msg-count">{{ item.messageCount || 0 }}条</span>
              </div>
            </div>
          </div>
        </n-scrollbar>

        <div class="list-footer">
          <n-pagination
            v-model:page="queryParams.pageNum"
            :page-size="queryParams.pageSize"
            :item-count="total"
            size="small"
            @update:page="loadSessions"
          />
        </div>
      </div>

      <!-- 右侧：聊天记录 -->
      <div class="chat-panel">
        <div v-if="!currentSession" class="chat-empty">
          <i class="i-material-symbols:forum" style="font-size: 48px; opacity: 0.3" />
          <p>选择左侧会话查看聊天记录</p>
        </div>
        <template v-else>
          <div class="chat-header">
            <div class="chat-header-info">
              <span class="chat-header-title">{{ currentSession.sessionName || '新对话' }}</span>
              <span class="chat-header-user">{{ currentSession.nickName }}</span>
              <span class="chat-header-count">{{ currentSession.messageCount || 0 }} 条消息</span>
            </div>
            <n-popconfirm @positive-click="handleDeleteSession">
              <template #trigger>
                <n-button text type="error" size="small">
                  <i class="i-material-symbols:delete-outline" /> 删除
                </n-button>
              </template>
              确定删除该会话吗？
            </n-popconfirm>
          </div>
          <n-scrollbar ref="chatScrollbarRef" class="chat-scroll">
            <div class="chat-messages">
              <div
                v-for="msg in messageList"
                :key="msg.id"
                class="chat-message"
                :class="msg.role"
              >
                <div v-if="msg.role === 'user'" class="msg-avatar user-avatar">
                  {{ (currentSession.nickName || '我').charAt(0) }}
                </div>
                <div v-else class="msg-avatar ai-avatar">
                  AI
                </div>
                <div class="msg-body">
                  <div class="msg-meta">
                    <span class="msg-role">{{ msg.role === 'user' ? currentSession.nickName : 'AI 助手' }}</span>
                    <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                  </div>
                  <div class="msg-bubble" :class="msg.role">
                    <div v-if="msg.role === 'assistant'" class="msg-content markdown-body" v-html="renderMarkdown(msg.content)" />
                    <div v-else class="msg-content">{{ msg.content }}</div>
                  </div>
                </div>
              </div>
            </div>
          </n-scrollbar>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { sessionPage, sessionMessages, sessionDelete, sessionStatistics } from '@/api/ai'
import { getEChartsTheme } from '@/utils/echarts-theme'
import AuthImage from '@/components/common/AuthImage.vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

defineOptions({ name: 'AiSession' })

const searchKeyword = ref('')
const dateRange = ref(null)
const sessionList = ref([])
const currentSession = ref(null)
const messageList = ref([])
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 15 })

const stats = ref({
  totalSessions: 0,
  totalMessages: 0,
  todaySessions: 0,
  totalTokenUsage: 0,
})

const trendChartRef = ref(null)
const chatScrollbarRef = ref(null)
let trendChart = null

const statsCards = computed(() => [
  {
    label: '会话总数',
    value: stats.value.totalSessions,
    icon: 'i-material-symbols:chat-bubble-outline',
    gradient: 'linear-gradient(135deg, #4242F7 0%, #6366F1 100%)',
  },
  {
    label: '消息总数',
    value: stats.value.totalMessages,
    icon: 'i-material-symbols:forum-outline',
    gradient: 'linear-gradient(135deg, #5AC8FA 0%, #38BDF8 100%)',
  },
  {
    label: '今日会话',
    value: stats.value.todaySessions,
    icon: 'i-material-symbols:today-outline',
    gradient: 'linear-gradient(135deg, #6EE7B7 0%, #34D399 100%)',
  },
  {
    label: 'Token 消耗',
    value: stats.value.totalTokenUsage,
    icon: 'i-material-symbols:token-outline',
    gradient: 'linear-gradient(135deg, #A78BFA 0%, #8B5CF6 100%)',
  },
])

marked.setOptions({
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
})

function renderMarkdown(content) {
  if (!content) return ''
  return marked.parse(content)
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

async function loadStatistics() {
  try {
    const res = await sessionStatistics()
    if (res.code === 200) {
      stats.value = {
        totalSessions: res.data.totalSessions || 0,
        totalMessages: res.data.totalMessages || 0,
        todaySessions: res.data.todaySessions || 0,
        totalTokenUsage: res.data.totalTokenUsage || 0,
      }
      renderTrendChart(res.data.dailyTrend || [])
    }
  }
  catch (e) {
    console.error('加载统计失败', e)
  }
}

async function loadSessions() {
  try {
    const params = {
      pageNum: queryParams.value.pageNum,
      pageSize: queryParams.value.pageSize,
      keyword: searchKeyword.value || undefined,
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = formatDate(dateRange.value[0])
      params.endTime = formatDate(dateRange.value[1])
    }
    const res = await sessionPage(params)
    if (res.code === 200) {
      sessionList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  }
  catch (e) {
    console.error('加载会话列表失败', e)
  }
}

function formatDate(ts) {
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function handleSearch() {
  queryParams.value.pageNum = 1
  loadSessions()
}

async function selectSession(item) {
  currentSession.value = item
  try {
    const res = await sessionMessages(item.id)
    if (res.code === 200) {
      messageList.value = res.data || []
      await nextTick()
      chatScrollbarRef.value?.scrollTo({ top: 99999, behavior: 'smooth' })
    }
  }
  catch (e) {
    console.error('加载消息失败', e)
  }
}

async function handleDeleteSession() {
  if (!currentSession.value) return
  try {
    const res = await sessionDelete(currentSession.value.id)
    if (res.code === 200) {
      window.$message.success('删除成功')
      currentSession.value = null
      messageList.value = []
      loadSessions()
      loadStatistics()
    }
  }
  catch (e) {
    window.$message.error('删除失败')
  }
}

function renderTrendChart(dailyTrend) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
    window.addEventListener('resize', () => trendChart?.resize())
  }
  const theme = getEChartsTheme()
  const dates = dailyTrend.map(d => d.date.substring(5))
  const sessionCounts = dailyTrend.map(d => d.sessionCount)
  const messageCounts = dailyTrend.map(d => d.messageCount)

  trendChart.setOption({
    ...theme,
    tooltip: {
      ...theme.tooltip,
      trigger: 'axis',
    },
    legend: {
      ...theme.legend,
      data: ['会话数', '消息数'],
    },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: {
      ...theme.categoryAxis,
      type: 'category',
      data: dates,
    },
    yAxis: [
      {
        ...theme.valueAxis,
        type: 'value',
        name: '会话数',
      },
      {
        ...theme.valueAxis,
        type: 'value',
        name: '消息数',
      },
    ],
    series: [
      {
        name: '会话数',
        type: 'line',
        smooth: true,
        data: sessionCounts,
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2, color: theme.color[0] },
        itemStyle: { color: theme.color[0] },
      },
      {
        name: '消息数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: messageCounts,
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 2, color: theme.color[2] },
        itemStyle: { color: theme.color[2] },
      },
    ],
  })
}

onMounted(() => {
  loadStatistics()
  loadSessions()
})

onUnmounted(() => {
  trendChart?.dispose()
  trendChart = null
})
</script>

<style scoped>
.ai-session-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 16px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-primary, #fff);
  border-radius: 12px;
  border: 1px solid var(--border-default, #e2e8f0);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon {
  font-size: 24px;
  color: #fff;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary, #64748b);
  margin-top: 4px;
}

/* 趋势图 */
.trend-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
}

.trend-chart {
  height: 220px;
}

/* 左右分栏 */
.session-container {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  background: var(--bg-primary, #fff);
  border-radius: 12px;
  border: 1px solid var(--border-default, #e2e8f0);
  overflow: hidden;
}

.session-list-panel {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-default, #e2e8f0);
}

.list-header {
  padding: 12px;
  border-bottom: 1px solid var(--border-default, #e2e8f0);
}

.session-list-scroll {
  flex: 1;
  min-height: 0;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid var(--border-default, rgba(226, 232, 240, 0.5));
}

.session-item:hover {
  background: var(--bg-secondary, #f8fafc);
}

.session-item.active {
  background: rgba(66, 66, 247, 0.08);
  border-right: 3px solid #4242F7;
}

.session-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4242F7 0%, #6366F1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-nick {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
}

.session-time {
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
}

.session-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.session-title {
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  flex: 1;
  min-width: 0;
}

.session-msg-count {
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
  margin-left: 8px;
  flex-shrink: 0;
}

.list-footer {
  padding: 8px 12px;
  border-top: 1px solid var(--border-default, #e2e8f0);
  display: flex;
  justify-content: center;
}

.empty-session {
  padding: 40px 0;
}

/* 聊天面板 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary, #94a3b8);
  gap: 12px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid var(--border-default, #e2e8f0);
}

.chat-header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
}

.chat-header-user {
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  margin-left: 12px;
}

.chat-header-count {
  font-size: 12px;
  color: var(--text-tertiary, #94a3b8);
  margin-left: 12px;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
}

.chat-messages {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chat-message {
  display: flex;
  gap: 10px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #4242F7 0%, #6366F1 100%);
  color: #fff;
}

.ai-avatar {
  background: linear-gradient(135deg, #6EE7B7 0%, #34D399 100%);
  color: #fff;
}

.msg-body {
  max-width: 70%;
  min-width: 0;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.chat-message.user .msg-meta {
  flex-direction: row-reverse;
}

.msg-role {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #64748b);
}

.msg-time {
  font-size: 11px;
  color: var(--text-tertiary, #94a3b8);
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.msg-bubble.user {
  background: linear-gradient(135deg, #4242F7 0%, #6366F1 100%);
  color: #fff;
  border-top-right-radius: 4px;
}

.msg-bubble.assistant {
  background: var(--bg-secondary, #f1f5f9);
  color: var(--text-primary, #0f172a);
  border-top-left-radius: 4px;
}

.msg-content {
  min-width: 0;
}

/* Markdown 样式 */
.msg-bubble.assistant :deep(.markdown-body) {
  font-size: 14px;
  line-height: 1.6;
}

.msg-bubble.assistant :deep(.markdown-body pre) {
  background: var(--bg-primary, #1e293b);
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.msg-bubble.assistant :deep(.markdown-body code) {
  font-size: 13px;
  font-family: 'Fira Code', Consolas, monospace;
}

.msg-bubble.assistant :deep(.markdown-body p) {
  margin: 4px 0;
}

.msg-bubble.assistant :deep(.markdown-body ul),
.msg-bubble.assistant :deep(.markdown-body ol) {
  padding-left: 20px;
  margin: 4px 0;
}

/* 暗色模式 */
.dark .stat-card {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .stat-value {
  color: #f1f5f9;
}

.dark .stat-label {
  color: #94a3b8;
}

.dark .session-container {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(51, 65, 85, 0.5);
}

.dark .session-item.active {
  background: rgba(66, 66, 247, 0.15);
}

.dark .msg-bubble.assistant {
  background: rgba(51, 65, 85, 0.5);
  color: #f1f5f9;
}

.dark .msg-bubble.assistant :deep(.markdown-body pre) {
  background: rgba(15, 23, 42, 0.8);
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .session-list-panel {
    width: 260px;
  }
}
</style>
```

- [ ] **Step 2: 检查 marked 和 highlight.js 是否已安装**

Run: `cd forge-admin-ui && cat package.json | grep -E '"marked"|"highlight.js"'`

如果没有安装，运行：
```bash
cd forge-admin-ui && pnpm add marked highlight.js
```

- [ ] **Step 3: Commit**

```bash
git add forge-admin-ui/src/views/ai/session.vue
git add forge-admin-ui/package.json forge-admin-ui/pnpm-lock.yaml
git commit -m "[ai-session] 新增AI会话管理页面"
```

---

## Task 8: 路由注册与最终验证

**Files:**
- 检查路由是否自动注册（基于菜单配置）
- 运行前端 lint

- [ ] **Step 1: 确认路由自动注册机制**

项目的菜单路由通常由后端 `sys_resource` 的 `path` 和 `component` 字段驱动。Task 5 的 SQL 中已配置 `path=/ai/session`，`component=/ai/session`，对应 `views/ai/session.vue`。

确认方式：启动后端后，在菜单管理中能看到"AI管理 > 会话管理"菜单，前端能自动加载路由。

- [ ] **Step 2: 执行 SQL 插入菜单权限**

连接 MySQL 数据库，执行 Task 5 中创建的 `ai_session_admin.sql`。

- [ ] **Step 3: 运行前端 lint**

```bash
cd forge-admin-ui && pnpm lint:fix -- src/views/ai/session.vue src/api/ai.js
```

修复所有 lint 错误后重新 lint 确认通过。

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "[ai-session] AI会话管理功能完成"
```

---

## 自查清单

| Spec 要求 | 对应 Task |
|-----------|-----------|
| 会话列表查询（分页、搜索、时间范围、状态） | Task 2 (Mapper XML) + Task 4 (Controller) + Task 7 (前端列表) |
| 会话下的消息记录-类IM聊天记录 | Task 4 (messages API) + Task 7 (聊天气泡) |
| 核心统计图表 | Task 2 (统计SQL) + Task 4 (statistics API) + Task 7 (卡片+趋势图) |
| 关联sys_user显示用户昵称等 | Task 2 (LEFT JOIN sys_user) + Task 7 (nickName/avatar渲染) |
| 前端页面美观易操作 | Task 7 (完整UI：统计卡片+趋势图+左右分栏+聊天气泡+Markdown渲染) |
| 菜单权限 | Task 5 (SQL) |

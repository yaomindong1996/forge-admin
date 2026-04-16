import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "Forge Admin",
  description: "基于 Vue3 + Spring Boot 的企业级后台管理框架",
  base: "/",
  themeConfig: {
    logo: '/logo.png',
    siteTitle: 'Forge Admin',
    nav: [
      { text: '开发指南', link: '/guide/sdd-workflow' },
      { text: '前端文档', link: '/frontend/components/overview' },
      { text: '后端文档', link: '/backend/modules/overview' },
    ],
    sidebar: {
      '/guide/': [
        {
          text: '开发指南',
          items: [
            { text: '渐进式 SDD 开发流程', link: '/guide/sdd-workflow' },
            { text: '项目编码规范', link: '/guide/conventions' },
          ]
        }
      ],
      '/frontend/': [
        {
          text: '前端组件',
          items: [
            { text: '概览', link: '/frontend/components/overview' },
            { text: 'AiForm 表单', link: '/frontend/components/ai-form' },
            { text: 'AiTable 表格', link: '/frontend/components/ai-table' },
            { text: 'AiCrudPage 页面', link: '/frontend/components/ai-crud-page' },
            { text: 'DictSelect 字典选择', link: '/frontend/components/dict-select' },
            { text: 'DictTag 字典标签', link: '/frontend/components/dict-tag' },
            { text: 'FileUpload 文件上传', link: '/frontend/components/file-upload' },
            { text: 'ImageUpload 图片上传', link: '/frontend/components/image-upload' },
            { text: 'IconSelector 图标选择', link: '/frontend/components/icon-selector' },
            { text: 'DemoBanner 演示横幅', link: '/frontend/components/demo-banner' },
            { text: 'SliderCaptchaModal 滑块验证', link: '/frontend/components/slider-captcha-modal' },
            { text: 'AiModal AI 模态框', link: '/frontend/components/ai-modal' },
            { text: 'BPMN 流程设计器', link: '/frontend/components/bpmn' },
            { text: 'FormDesigner 表单设计器', link: '/frontend/components/form-designer' },
          ]
        },
        {
          text: '工具函数',
          items: [
            { text: '概览', link: '/frontend/utils/overview' },
            { text: '通用工具', link: '/frontend/utils/common' },
            { text: 'HTTP 请求', link: '/frontend/utils/http' },
            { text: '加密请求', link: '/frontend/utils/encrypt-request' },
            { text: '文件处理', link: '/frontend/utils/file' },
            { text: '本地存储', link: '/frontend/utils/storage' },
            { text: '加密工具', link: '/frontend/utils/crypto' },
            { text: '字典管理', link: '/frontend/utils/dict' },
            { text: '新增工具', link: '/frontend/utils/new-utils' },
          ]
        }
      ],
      '/backend/': [
        {
          text: '后端模块',
          items: [
            { text: '概览', link: '/backend/modules/overview' },
            { text: '核心工具', link: '/backend/modules/core' },
            { text: '动态配置', link: '/backend/modules/config' },
            { text: '分布式幂等', link: '/backend/modules/idempotent' },
            { text: 'MyBatis-Plus', link: '/backend/modules/orm' },
            { text: '社交登录', link: '/backend/modules/social' },
            { text: 'Web 层', link: '/backend/modules/web' },
            { text: 'API 配置管理', link: '/backend/modules/api-config' },
            { text: '流程引擎客户端', link: '/backend/modules/flow-client' },
            { text: 'AI 插件', link: '/backend/modules/ai' },
            { text: 'Excel 导出', link: '/backend/modules/excel' },
            { text: '文件存储', link: '/backend/modules/file' },
            { text: '字典翻译', link: '/backend/modules/trans' },
            { text: '分布式 ID', link: '/backend/modules/id' },
            { text: '数据权限', link: '/backend/modules/datascope' },
            { text: '多租户', link: '/backend/modules/tenant' },
            { text: '消息推送', link: '/backend/modules/message' },
            { text: 'WebSocket', link: '/backend/modules/websocket' },
            { text: '定时任务', link: '/backend/modules/job' },
            { text: '操作日志', link: '/backend/modules/log' },
            { text: '缓存', link: '/backend/modules/cache' },
            { text: '认证授权', link: '/backend/modules/auth' },
            { text: '加解密', link: '/backend/modules/crypto' },
          ]
        }
      ]
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com' }
    ]
  }
})
# Fusion AI Design 文档

基于 VitePress 的 Fusion AI Design 组件库和框架使用文档。

## 项目概述

Fusion AI Design 文档站点提供了完整的组件使用指南、API参考、最佳实践和示例代码，帮助开发者快速上手和使用 Fusion AI Design 框架。

## 文档内容

### 项目介绍
- [项目概述](guide/introduction.md) - 项目背景、核心特性和设计理念
- [技术栈](guide/tech-stack.md) - 前端和后端技术栈详解
- [目录结构](guide/directory-structure.md) - 项目目录结构说明
- [环境配置](guide/environment.md) - 开发环境配置指南

### 组件文档
- [AI Form 组件库](components/ai-form/overview.md) - 智能表单组件库概述
  - [AiForm 表单组件](components/ai-form/ai-form.md) - 表单组件详细文档
  - [AiSearch 搜索组件](components/ai-form/ai-search.md) - 搜索组件详细文档
  - [AiTable 表格组件](components/ai-form/ai-table.md) - 表格组件详细文档
  - [AiCrudPage CRUD页面](components/ai-form/ai-crud-page.md) - 完整CRUD页面组件文档
- [IconSelector 图标选择器](components/icon-selector.md) - 图标选择组件文档
- [Loading 加载组件](components/loading.md) - 加载提示组件文档

### 布局配置
- [布局配置](layout/configuration.md) - 多种布局模式配置指南
- [主题定制](layout/theme.md) - 主题颜色和样式定制
- [菜单系统](layout/menu.md) - 动态菜单和权限控制

### 工具函数
- [工具函数概述](utils/overview.md) - 工具函数使用指南
- [HTTP请求](utils/http.md) - 基于Axios的HTTP工具
- [加密工具](utils/encrypt.md) - RSA/AES/DES加密解密工具
- [存储工具](utils/storage.md) - LocalStorage和SessionStorage封装
- [通用工具](utils/common.md) - 日期处理、防抖节流等通用工具

## 开发指南

### 本地开发

```bash
# 安装依赖
pnpm install

# 启动开发服务器
pnpm docs:dev

# 构建文档
pnpm docs:build

# 预览构建结果
pnpm docs:preview
```

### 文档编写规范

1. **Markdown格式** - 使用标准Markdown语法编写文档
2. **代码示例** - 提供完整的代码示例和使用场景
3. **API文档** - 详细说明每个属性、方法和事件
4. **最佳实践** - 提供实际开发中的最佳实践建议
5. **注意事项** - 标注使用时需要注意的问题

### 目录结构

```
fusion_design_docs/
├── .vitepress/          # VitePress配置
├── guide/               # 项目介绍文档
├── components/          # 组件文档
├── layout/              # 布局配置文档
├── utils/               # 工具函数文档
├── public/              # 静态资源
├── index.md             # 首页
└── README.md            # 本文档
```

## 贡献指南

欢迎提交Issue和Pull Request来帮助我们改进文档：

1. Fork 项目仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。
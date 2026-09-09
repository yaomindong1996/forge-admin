# 数据权限配置优化 Implementation Plan

**Goal:** 让管理员直接管理规则启停，并用业务文案理解权限刷新。

**Architecture:** AiCrudPage 保持主列表，页面专用开关和技术配置组件负责局部交互；专用状态 DTO 与 Mapper 更新保护并发，提交后刷新及 Redis 通知更新运行快照。

**Tech Stack:** Vue 3、Naive UI、Spring Boot、MyBatis XML、Redisson、Vitest、JUnit、Playwright。

- [x] 后端：配置/状态 DTO、XML 查询与状态更新、管理员 API、提交后刷新。
- [x] 权限运行态：加载显式禁用配置、跨实例通知与重订阅补载，针对性回归测试。
- [x] 前端：列表业务列、状态开关、刷新反馈、技术配置折叠与编辑文案。
- [x] 验证：状态交互单测、后端单测/编译、lint/build、浏览器亮暗色及窄屏检查。
- [x] 记录结果并按项目规范提交本轮文件，不包含用户已有 .DS_Store 改动，不推送。

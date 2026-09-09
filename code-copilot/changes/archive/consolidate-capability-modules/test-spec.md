# 测试规格 — Capability 插件模块收敛

> 本轮是模块结构重构。用户明确由其自行测试，因此不启动服务、不执行真实接口或端到端联调。

## 1. P0 静态验证

- [x] 新父模块包含且仅包含四个子模块。
- [x] 所有新 POM XML 可解析，artifactId 与模块路径一致。
- [x] 全仓无旧 Capability artifactId 依赖。
- [x] 全仓无旧公共执行契约 package import。
- [x] `platform` 不依赖 `actions / generator / flow-client`。
- [x] 自动配置导入项完整且无重复。
- [x] `git diff --check` 与新目录尾随空白扫描无本轮引入的问题。

## 2. 用户环境复验

- [ ] 编译 `forge-plugin-capability-parent` 四模块。
- [ ] 编译 `forge-admin-server` 聚合依赖。
- [ ] 启动 Admin 后确认能力目录、客户端、授权、外围用户映射、OAuth Token 与开放网关 Bean 正常装配。
- [ ] 回归业务动作、流程动作、系统服务和高风险审批开关。

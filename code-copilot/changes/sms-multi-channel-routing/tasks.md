# Tasks: sms-multi-channel-routing

## P0 配置多例
- [x] Flyway：channel_role、scene_templates、route_policy、ping_log
- [x] 实体 / Mapper / Service 列表与删除、设主
- [x] Provider 返回多配置；兼容 getEnabledConfig
- [x] 前端改为列表 + 抽屉编辑

## P1 路由
- [x] SmsRouter（WRR + PRIMARY_ONLY + captcha 锁主）
- [x] ReadConfig 多 Blend；发送按 configId
- [x] MessageSmsCaptchaSender 带 scene=LOGIN_CAPTCHA
- [x] 配置保存后刷新 Blend

## P2 Failover
- [x] 失败切换有限次数；SendResult 带实际 configId（日志）

## P3 Ping
- [x] route-policy Ping 字段 + 立即测试 API
- [x] @ScheduledJob 扫启用租户探活
- [x] ping_log 查询

# Collaboration Notify Pitfalls Index

Related titles live mainly under message/outbound/security capability notes. Open when matching symptoms:

- 出站白名单 / `COLLABORATION_PROVIDER` 未放行导致企微 API 失败
- 凭据/Token 日志明文（secret masking）
- 消息模板 Flyway `${` 未转义
- 流程待办重复推送（自定义动作 + 平台 listener 双发）
- 回调验签窗口/加解密失败却降级放行
- 租户上下文缺失导致连接/用户绑定查空

Also cross-check `forge-message-integration` pitfalls for template/idempotency issues before blaming the IM provider.

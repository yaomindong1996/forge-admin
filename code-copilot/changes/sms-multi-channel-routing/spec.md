# 短信多通道：并存配置、权重轮询、Failover、Ping

> 变更名：`sms-multi-channel-routing`  
> 状态：`implemented`（待本地编译与联调验收）  
> 创建日期：2026-10-29  
> 依据：多短信平台同时配置、主/备、权重轮询、运维 Ping

## 1. 背景

当前 `sys_sms_config` 虽有 `weight`，运行时只取 `ORDER BY weight DESC LIMIT 1`，`SmsFactory.getSmsBlend()` 无 configId，管理端也是单表单。无法同时启用腾讯云 + 网易云并按比例分流。

## 2. 目标

1. 租户可同时维护多条启用中的短信通道（多厂商）。  
2. 通道角色：`PRIMARY` / `SECONDARY`（租户内启用态最多一个 PRIMARY）。  
3. 路由模式：`WEIGHTED_RR`（默认）或 `PRIMARY_ONLY`。  
4. 权重轮询跨实例用 Redis 计数；主发 2 备发 1 = weight 2:1。  
5. **验证码场景默认锁定 PRIMARY**（`LOGIN_CAPTCHA`），不参与轮询。  
6. 发送失败可 Failover 到其它启用通道（有限次数）。  
7. 每通道可配置场景模板映射（`sceneTemplates` JSON）。  
8. 可选 Ping：运维手机号 + Cron + 逐通道探活日志。

## 3. 非目标

- 不新接短信 SDK（继续 sms4j）。  
- 不改邮件通道。  
- 不做按业务对象精细路由。

## 4. 数据模型

### 4.1 `sys_sms_config` 增量

| 列 | 说明 |
|----|------|
| `channel_role` | `PRIMARY` / `SECONDARY`，默认 `SECONDARY` |
| `scene_templates` | JSON：`{"LOGIN_CAPTCHA":"模板ID","SMS_PING":"..."}` |

存量启用行迁移：权重最高且启用的一条标为 `PRIMARY`，其余启用为 `SECONDARY`。

### 4.2 `sys_sms_route_policy`（每租户一行）

| 列 | 默认 | 说明 |
|----|------|------|
| `route_mode` | `WEIGHTED_RR` | 或 `PRIMARY_ONLY` |
| `captcha_lock_primary` | 1 | 验证码锁主通道 |
| `failover_enabled` | 1 | 失败切换 |
| `failover_max_attempts` | 2 | 含首次 |
| `ping_enabled` | 0 | |
| `ping_phones` | | 逗号分隔 |
| `ping_cron` | `0 0 */6 * * ?` | |
| `ping_mode` | `PER_CHANNEL` | 逐通道 Ping |

### 4.3 `sys_sms_ping_log`

记录每次探活：`tenant_id, config_id, supplier, phone, status, error_msg, send_time`。

## 5. 运行时

```
SendRequest(+scene)
  → SmsRouter.choose(tenantId, scene)
       LOGIN_CAPTCHA + captchaLock → PRIMARY
       否则 WEIGHTED_RR / PRIMARY_ONLY
  → 解析模板：sceneTemplates[scene] ?? templateId
  → SmsFactory.getSmsBlend(configId)
  → 失败且 failover → 下一通道（跳过已试）
```

Redis key：`forge:sms:wrr:{tenantId}`（INCR，按权重区间取模）。

配置变更后重建 sms4j Blend 列表。

## 6. API（超管）

- `GET /system/message-config/sms/list`
- `GET /system/message-config/sms/{id}`
- `POST /system/message-config/sms/save`（兼容）
- `DELETE /system/message-config/sms/{id}`
- `POST /system/message-config/sms/updateStatus`
- `POST /system/message-config/sms/set-primary?id=`
- `GET/PUT /system/message-config/sms/route-policy`
- `POST /system/message-config/sms/ping/test`（立即 Ping）
- `GET /system/message-config/sms/ping/logs`

兼容：原 `GET /sms` 仍返回 PRIMARY 或权重最高启用项。

## 7. 验收

- [ ] 可同时启用腾讯 + 网易；设主通道；权重 2:1 分流（多实例大致符合）  
- [ ] 验证码始终走 PRIMARY（锁开启时）  
- [ ] 主失败可切到备（failover 开）  
- [ ] Ping 可关可开；逐通道写日志  
- [ ] Secret 掩码与「至少一条启用」保护仍有效  

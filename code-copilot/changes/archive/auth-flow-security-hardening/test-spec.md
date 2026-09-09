# 认证与流程身份安全加固 - 测试计划

## P0
1. 短信/邮件通道未启用时，发码与重置均拒绝，不走图形验证码。
2. 账号不存在时发码仍返回成功，且不调用发送通道。
3. 流程待办/签收使用 Session userId；伪造 userId 返回 MISMATCH；pageSize 上限 100。
4. 匿名注册默认关闭。

## 命令
```bash
cd forge-server
mvn test -P enable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-plugin-parent/forge-plugin-system,forge-flow/forge-flow-server -am \
  -Dtest=CaptchaServiceImplTest,RecoveryChannelSupportTest,SystemAuthServiceImplPasswordRecoveryTest,SystemAuthServiceImplClientCredentialTest,ClientCredentialSurfaceContractTest,MessageEmailCaptchaSenderTest,MessageSmsCaptchaSenderTest,FlowDelegatedIdentityControllerTest
```

## P1 续 2
12. 配置/消息/组织/字典/公告/区划无类级豁免；选择器与收件箱方法级豁免。
13. 短信邮件通道配置需超级管理员。

## P1 续 3
14. 通用文件接口无类级豁免；上传/下载/删除方法级豁免；存储桶无豁免。
15. 文件元数据 `byFileId`/`rename` 方法级豁免；分组与 Excel 无类级豁免。
16. FileManager 删除在 `canModify=false` 时返回 403，不删除元数据。
17. Sa-Token 配置不含 `/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`、`/webjars/**`。

## P1 续
9. 高危管理 Controller 无类级 `@ApiPermissionIgnore`；用户 profile 保留方法级豁免。
10. 流程实例 terminate/delete 使用 Session。
11. 报表数据过滤器拒绝 window/document/fetch。

## P1 增量
5. 公告/消息 HTML 消毒；菜单搜索高亮转义。
6. Sa-Token 仅匿名 `/actuator/health`。
7. 租户管理类级权限豁免已去掉。
8. 登录页仅在通道启用时出现「忘记密码」。

## 跳过
- 不启动 Admin/Flow 真实服务，不做浏览器 E2E。

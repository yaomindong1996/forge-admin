# 认证与流程身份安全加固 - Tasks
> status: apply

## T1 找回密码通道
- [x] 全局短信/邮件配置未启用时拒绝该通道，不降级图形验证码
- [x] 双通道都未启用时返回「未启用找回密码通道」
- [x] 发码不暴露账号是否存在

## T2 流程身份
- [x] 待办/已办/发起/候选/签收/抄送/转办/终结/撤回使用 Session userId
- [x] 流程分页 pageSize 上限 100

## T3 注册与调试
- [x] 匿名注册默认关闭
- [x] 删除无鉴权调试接口

## T4 测试
- [x] 补单测并跑通相关模块

## T5 P1 XSS
- [x] 公告/消息/首页公告 v-html 消毒
- [x] 菜单搜索高亮先转义

## T6 P1 Actuator / 租户权限
- [x] 仅匿名 /actuator/health，只暴露 health
- [x] SysTenantController 去掉类级权限豁免

## T7 登录页找回密码
- [x] 按 resetPasswordChannels 展示；新密码 RSA
- [x] 增量测试

## T8 P1 管理接口权限
- [x] 角色/用户/资源/客户端/数据权限/监控/密文迁移去掉类级豁免
- [x] 用户资料接口保留方法级豁免

## T9 流程实例身份
- [x] 终止/删除使用 Session userId

## T10 报表过滤器
- [x] new Function 拒绝浏览器全局对象与网络 API

## T11 P1 配置与消息权限
- [x] 配置/缓存/存储/短信邮件通道去掉类级豁免
- [x] 组织树、字典按类型、公告前台、消息收件箱保留方法级豁免

## T12 P1 文件归属与报表 URL
- [x] FileController 去掉类级豁免；存储桶仅管理员；删除走上传者或管理员
- [x] 文件分组/元数据/Excel 去掉类级豁免；byFileId 与 rename 保留方法级豁免
- [x] 报表 URL 模板拦截危险全局对象

## T13 去掉未使用的 Swagger 匿名白名单
- [x] Sa-Token 不再放行 doc.html / webjars / swagger-ui / v3/api-docs

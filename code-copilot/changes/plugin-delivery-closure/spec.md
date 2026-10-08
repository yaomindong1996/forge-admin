# 插件交付闭环补充

## 背景与范围

补充用户指定的缺口 1、2；保留 plugin-foundation / plugin-center / plugin-runtime-license 既有协议。
用户已确认构建制品使用腾讯 COS，目标部署首先支持 Docker Compose。
本轮只编码、本地隔离验证；不上传真实制品、不部署服务器、不执行共享数据库迁移、不推送代码。
原工作区未提交的候选制品登记改造保持不动，不纳入本变更提交。

## D1：客户工程在线获取源码

- 复用 website 客户 SSO：S256 PKCE、随机 state、一次性 code，不使用平台管理员会话。
- 独立 CLI 客户端 forge-cli；回调固定 http://127.0.0.1:37329/callback，由网站显式加入 allowlist。
- 回调只监听 127.0.0.1，检查 Host、请求方法、路径、重复参数及 state；5 分钟超时，退出即关闭。
- apiBaseUrl / authorizeUrl 仅允许 HTTPS；HTTP 仅允许 127.0.0.1 / localhost 本地验证。
- Token、deviceId 只存在内存，禁止参数传 token、落盘、打印服务端异常原文；重定向禁止跟随。
- 查看已购列表，按明确 pluginId + version 选取已发布版本，禁止自动 latest、自动升级。
- 请求已有 mine / versions / access / download 接口，检查权益及核心兼容性。
- 下载上限 8 MiB；元数据、X-Source-SHA256、真实 ZIP SHA256 必须一致；包内描述必须与版本记录一致。
- 源码以 SHA256 命名，缓存仅用户可访问且不可覆盖；缓存成功不代表安装或授权成功。
- check 只预检；add 必须 --reviewed，调用已有 inspectPluginInstall / addPlugin 并保留定制保护及恢复备份。
- 企业源码下载不签发许可证，不改变 FeatureGate；社区模板禁止把付费源码加入 Git 索引。
- 工程生成后工具保持原始字节；不另造生成工程安装器。

## D2：制品分发与 Compose 部署

- 源码 ZIP 与构建制品使用独立 COS 前缀、独立 CAM 权限；不得公开 URL 或复用源码 8 MiB 内存接口。
- COS SDK 使用官方发布包；新增依赖须先确认。本轮等待 SDK 选择期间先完成 D1。
- 只发布现有封存、实际摘要核验的 community Admin JAR/UI 制品；不放开已有 ee 构建限制。
- 制品按 releaseId 内容寻址，不覆盖既有对象；实际读回验证与上传回执分离。
- Compose 包按固定镜像 digest、只读制品目录及外部私有配置生成，不接受浏览器任意 shell。
- 打包成功、上传成功、部署成功、运行健康必须分别记录；所有离线输出保持 deployed=false。
- 实际部署还需独立目标权限、当前审批核验、数据库备份确认及恢复验证；不得复用构建 token 直接部署。
- 回滚只切换已核验旧制品，不回滚 Flyway 历史及业务数据；数据库备份由操作者单独维护。

## 安全 / 人工审查

用户已授权交付功能开发，未授权操作远端环境。D1 复用现有权益判定，不新增收费/授予权限接口。
D2 的真实发布、部署及状态推进须独立审查与测试，不将模拟 SDK / Compose 测试标为真实 E2E。
无数据库结构变更。未完成阶段保持 tasks 待办，不宣称全流程已闭环。

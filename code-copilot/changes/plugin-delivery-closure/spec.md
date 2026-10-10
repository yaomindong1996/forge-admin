# 插件交付闭环补充

## 背景与范围

补充用户指定的缺口 1、2；保留 plugin-foundation / plugin-center / plugin-runtime-license 既有协议。
用户已确认构建制品使用腾讯 COS，目标部署首先支持 Docker Compose。
本轮只编码、本地隔离验证；不上传真实制品、不部署服务器、不执行共享数据库迁移、不推送代码。
原工作区未提交的候选制品登记改造保留，作为部署候选输入；不覆盖无关改动。

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
- COS SDK 使用腾讯官方 cos-nodejs-sdk-v5 3.0.0，已通过 npm 元数据确认版本及完整性。
  仅 scripts/forge-plugin-release 独立包依赖，锁定版本，不加入业务前端。
- 只发布现有封存、实际摘要核验的 community Admin JAR/UI 制品；不放开已有 ee 构建限制。
- 制品按 releaseId 内容寻址，不覆盖既有对象；实际读回验证与上传回执分离。
- Compose 包按固定镜像 digest、只读制品目录及外部私有配置生成，不接受浏览器任意 shell。
- 本阶段 Compose 仅生成 rootless Docker 目标包，容器 UID 0 映射为宿主普通用户；
  实际执行器必须核验 rootless，不得在 rootful Docker 使用该包。外部配置仅检查权限、不复制。
- 新增 check / prepare --reviewed / verify 离线入口；验真覆盖制品字节、配置、回执和只读权限。
- 打包成功、上传成功、部署成功、运行健康必须分别记录；所有离线输出保持 deployed=false。
- 实际部署还需独立目标权限、当前审批核验、数据库备份确认及恢复验证；不得复用构建 token 直接部署。
- 回滚只切换已核验旧制品，不回滚 Flyway 历史及业务数据；数据库备份由操作者单独维护。

## 安全 / 人工审查

用户已授权交付功能开发，未授权操作远端环境。D1 复用现有权益判定，不新增收费/授予权限接口。
D2 的真实发布、部署及状态推进须独立审查与测试，不将模拟 SDK / Compose 测试标为真实 E2E。
新增 V1.0.216 部署任务、目标状态、字典与独立按钮/API权限；不自动授予角色。
用户本轮明确要求完成全部未完成开发（真实验证除外），包含此状态流转与权限控制面。
只生成迁移，不执行共享库；回退停用执行器/入口，审计保留，不删除迁移历史。

## D2.3 执行契约（本轮）

- 目标与仓库由部署配置 allowlist 提供，浏览器只能选择 targetId，不提交路径、URL、命令、凭证。
- 独立 forge.plugin-delivery.worker 凭证绑定 tenantId、workerId、有效期，不能使用构建凭证。
- 机器接口经专用过滤器认证后以服务端请求属性选择固定 JSON 协议，数据库 API 加密配置不能改变此协议；
  未认证请求不设属性。保留全局防重放头，不靠 X-Inner-Call 跳过认证或防重放。
- 可视化动作 publish/deploy/restore 分离；每个目标一条活动任务，requestId 幂等，租约 CAS。
- queued → running → succeeded/failed/uncertain；超时不自动接管，人工核查后解除目标占用。
  uncertain 只表示无法证明最终状态；操作者不能把人工解除写成成功或更新当前版本。
- claim 和 authorize 均重新验证候选原审批；authorize 在每次远端写或 Compose 切换前调用。
- 发布使用私有 COS、禁覆盖头、桶/对象 ACL 和公共策略检查，流式 PUT 后逐文件读回 SHA256。
  manifest 最后写入，缺少 manifest 的半包不得安装；取回逐文件验证后封存本地 vault。
- deploy/restore 须确认数据库备份、迁移审查；restore 额外确认数据库向后兼容。
- 执行器只运行固定 Docker argv（无 shell），校验本地 owned rootless socket、cgroup v2、镜像 digest。
- Admin 应用 JAR 随包携带 db/migration（非过滤资源），独立容器配置选择 classpath 迁移；
  不改现有源码启动的 filesystem 默认配置。旧部署现场须匹配准备包摘要、只读挂载及相同单/双端组合。
- 切换前比较运行项目与控制面 previousReleaseId；外部配置不进入 COS；不执行数据库回滚。
- 后端运行探针独立认证，nonce 绑定请求，读取实际 classpath 插件目录与当前运行 JAR SHA256；
  UI 从实际 HTTP 逐文件读取摘要，禁止只靠容器标签/HTTP200证明运行成功。
- 成功回执来自认证执行器，明确不是平台自行读取远端字节；失败/超时有固定错误码，无原始日志或密钥。
- 完成恢复需重新核验旧制品和运行状态；保持未知状态锁，不自动 down/rm 用户项目。
- 容器实际内存、swap、CPU、PID限制及只读挂载必须和固定部署包匹配；
  uncertain 即使服务端已收到回执也保留本地锁，不能把收到回执误作允许再次部署。
- 人工关闭未知部署保留 unverifiedReleaseId 和核查人/说明/时间；此时只允许恢复最后确认的 currentReleaseId。
  执行器核对现场标签只能是确认版或该次未知版，不能覆盖无关项目；恢复成功才清除此未核验标记。
  recover-lock --reviewed 须经独立执行器接口确认原任务已人工关闭，只归档本进程所属目标锁，不删除历史。
- 真实 COS、SSO、MySQL迁移、LinuxCompose验收由用户执行，D2.4 不计入开发完成。

## 阶段结果

D1 本地源码获取与确认安装已实现，提交 14419e6a；真实网站客户登录需部署后验收。
D2.1/D2.2/D2.3 全部完成开发：锁定官方 COS SDK 3.0.0，候选登记、独立授权、实际发布与部署执行器、
运行身份和文件核验、可视化审计、未知结果治理和确认版本恢复已贯通。
新增部署身份仅来自部署配置，不允许数据库配置改写；机器 JSON 协议仍保留全局防重放。
本地隔离测试、生成工程回归、Java/UI构建及真实组件页面检查通过；不以夹具代替真实环境验收。
按用户明确范围，唯一保留 D2.4 的真实客户 SSO、私有 COS、MySQL迁移及LinuxCompose验收。
独立 Pro 工程、付费源码构建开放、自动支付、动态 JAR 热安装不在本轮已确认范围。

# T4 执行记录

## 2026-10-12：开始

- 分支 codex/spring-boot4-t0-compatibility，保留已有 T0–T3 和无关改动；没有切换 main。
- 读取根 AGENTS、code-copilot 规则、测试标准、偏好和相关踩坑，复用 T3 Spec/Tasks/Test/Log/Report。
- 当前日常版本 1.2.0；本轮不正式发版、不 Git 提交/push、不部署、不连接真实环境。
- 核对现有图像 @Async 自调用与裸下载、ReactAgent 单机 Map、Token 校验/消费分离。
- 额外发现 TokenRequiredStrategyHandler 的请求上下文错误转换及 Token 正文日志，纳入本轮修复。
- 核对 Redis 官方 Lua 原子执行与 Spring TaskScheduler API；不新增外部依赖、默认出站授权。

实际命令、失败、修复及证据在执行后追加，不预填通过。

## 实现与增量修复

- 图像入口只重建允许的请求字段与可信身份，pending 记录入库后提交独立池；不自调用 @Async。
  工作线程重读模型/供应商，原子比较期望状态；重复调度不重发请求，队列拒绝落 failed。
- 复用现有出站客户端与 EXTERNAL_CONNECTOR，PNG/JPEG 类型/完整解码/像素/字节限制；上传指定私有与已知大小。
  模型/存储异常只记录类型，errorMsg 不保存异常正文；空解密密钥不能进入 SDK 默认凭据兜底。
- 会话 Redis lease 在建行/历史/确认消费之前取得，心跳与所有权核对失败关闭；旧 owner 条件释放，跨节点可信取消。
  心跳调度器通过独立生命周期包装，不暴露为通用 TaskScheduler Bean，防止平台 @Scheduled 扫描抢占续租池。
- 模型/工具/事件边界验证执行许可，租约丢失不发布旧终止事件覆盖新运行；准备/提交失败（含 Error）释放本次占用。
- Token 原子生成/领取/TTL；修正请求头错误类型转换，默认前缀 alias 一致；移除 Token 和原始幂等键日志。
  未使用/已消费协议保留，业务失败不退还领取；缺失/过期/未知状态不创建 key。

## 命令与失败历史

以下日志均在 `/private/tmp/forge-boot4-t4-` 下，无真实环境凭据或调用：

| 日志 | 实际结果 |
| --- | --- |
| target1.log | AI 296 项，2 失败/1 错误；幂等未执行，不能记通过 |
| target2.log | 新竞争测试对 ChatModel.stream 使用无类型 any 导致重载歧义，测试编译中止 |
| target3.log | AI 301 项通过；Token 测试 stub 的 proceed 声明 Throwable、测试方法只声明 Exception，编译中止 |
| target4.log | 29 模块通过；AI 303 / 幂等 53，全量目标依赖同时执行；12:41:27 完成，耗时 2:16 |
| admin-ui1.log | 316 文件 / 2,279 项全通过；12:39:11 开始，117.74 秒 |
| clean1.log | 53 模块 clean package 全通过；12:52:21 完成，耗时 3:58 |

target1 根因不是工作线程泄漏：清理探针提交时再次捕获了尚未清理的请求租户。
先清理提交线程再检查池线程，保留全部无残留断言；另有 AiSecretCrypto 非空即密文的约定，
夹具未 stub decrypt 导致 null 未命中 adapter stub。补密文解密夹具，不改生产加密规则或放宽断言。
其它两处仅修复测试 API 类型与异常声明，不删除测试、改变业务断言或排除源码。

## 最终核验

- 逐类 Surefire XML 汇总 789 份报告：3,456 项，0 失败/错误/跳过；相比 T3 增加 59 项。
- AI 303、幂等 53、出站 48；其余模块与 T3 数量一致（generator 1,269、流程 189/43/17、系统 300 等）。
- 四服务 JAR：Admin 284M、App 151M、Flow 284M、Report 261M，均在 12:52 生成于各模块 target。
  仅本地验证产物，不是干净发布提交、不部署、不伪造上线状态。
- Node 20.19.0；发行版本检查保持 1.2.0。36 个相关 Java 文件单类/单行尺寸无超限；diff --check 通过。
- 主业务权限和 MCP 客户端委托保持；图像结果须已有查询权限，未自动赋权。
- 所有本轮 Maven/Node 测试会话完成；隔离 fixture/Bean 的 HTTP、请求线程池、图像池和心跳生命周期在测试结束清理。
  没有启动真实 Admin/Flow/DB/Redis 服务、创建部署服务 PID 或连接外部模型/COS。
- H5/Report/源码交付工具复用 T3 验证，不把它们计入 T4 重新执行结果；两处历史 JS lint 债未改、未关闭规则。
- 当前分支与 HEAD 保持 codex/spring-boot4-t0-compatibility / 719a2fc0b28d929edef62769dc2adc5e75ef557e。
  main、ForgePro、历史迁移和无关用户改动不动；没有 Git 提交、push、标签、发版或部署。

## 可复跑命令

从仓库根目录，使用已有 Java 17、Maven 3.9.11 和 Byte Buddy agent：

```bash
JAVA_TOOL_OPTIONS='-javaagent:<本机 byte-buddy-agent-1.18.11.jar 绝对路径>' \
mvn -B -ntp -f forge-server/pom.xml -Pdev,enable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-ai,forge-framework/forge-starter-parent/forge-starter-idempotent \
  -am test -Dstyle.color=never

JAVA_TOOL_OPTIONS='-javaagent:<本机 byte-buddy-agent-1.18.11.jar 绝对路径>' \
mvn -B -ntp -f forge-server/pom.xml -Pdev,enable-tests clean package -Dstyle.color=never

node scripts/forge-shared/version-cli.mjs --check
git diff --check
```

PC 测试从 forge-admin-ui 目录：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
node node_modules/vitest/vitest.mjs run --maxWorkers=4 --minWorkers=1
```

发版判定：日常开发；版本保持 1.2.0；CHANGELOG 归属 Unreleased。
真实环境、生产发布/部署均待用户另行安排，限制和配置要求见 compatibility-report。

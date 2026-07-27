# 掘金技术系列规划：《给 AI Agent 修城墙》

> 定位：源码级技术深度文，与已发的产品视角文（`forge-admin-juejin-article.md`）和横评系列错位互补。
> 主线：Forge Admin 的 AI 能力中枢（Capability Control Plane）——企业后台接入 AI Agent 时，认证、授权、审批、审计怎么做。
> 差异化理由：市面上开源后台框架几乎没有对标物；MCP / AI Agent 治理话题新、热度高；源码全部可查，可信度高。

## 系列文章

| 期 | 选题 | 核心卖点 | 关键源码 |
|----|------|---------|---------|
| 1 | 一行配置，把 Spring Boot 后台变成 AI Agent 的工具箱：MCP Server 插件源码拆解 | 协议守卫 fail-fast、传输层认证过滤器、工具贡献 SPI、上下文穿透 | `forge-plugin-mcp` |
| 2 | AI 调用也要过 OAuth：fdu_ 短期 Token 与三 Pepper 的凭证体系 | 机器客户端凭证签发、Token TTL 10 分钟、HMAC Pepper 分离、Origin 白名单 | `forge-plugin-capability-identity` |
| 3 | 只暴露三个元工具：search / describe / invoke 的受控能力网关 | 不把业务接口直接暴露为工具；字段白名单、幂等键、人工确认在能力层收口 | `forge-plugin-capability-secure-actions` |
| 4 | AI 的高危操作必须过人：挂 Flowable 审批的 HIGH 风险动作 | 高危动作默认关闭、KEK 加密、审批回调后才放行执行 | `forge-plugin-capability-high-risk-approval` |
| 5 | del_flag 写主键墓碑：逻辑删除与唯一索引的和解 | 行业通病踩坑叙事（0/1 方案同 code 只能删一次），传播性最强 | `V1.0.51__replace_logic_delete_generated_columns.sql`、56 处 `@TableLogic(delval="id")` |
| 6 | AI 流式生成 CRUD 的七阶段 SSE 协议 | 接已发《协议驱动 vs 代码生成》的续篇；Reactor Flux.concat 编排、产物与手工配置同构 | `CrudGeneratorStreamService`、`AiCodegenStrategy` |

## 发布节奏建议

- 每周 1 篇，先发第 1 篇（MCP，话题最新）打开系列认知。
- 第 5 篇（del_flag 墓碑）叙事门槛最低、最适合引流，可视前几篇数据提前。
- 每篇结尾挂系列目录 + 项目地址，形成互链。

## 选题雷区（已发文章，勿重复）

多租户字段级拦截、数据权限 SQL 改写、接口加解密原理、AiCrudPage 实操、Flowable 注解接入、低代码×工作流整合、协议驱动 vs 代码生成（理念文）、公式引擎、定时任务重构、低代码 JS 沙箱（功能级）。

## 写作基调

- 掘金读者画像：一线 Java/前端工程师，喜欢"问题 → 方案 → 源码 → 踩坑"结构。
- 每篇 3500-5000 字，代码片段取自真实源码（注明模块路径），架构图用文字/ASCII 或 Mermaid。
- 开头 200 字内必须抛出真实痛点（裸奔的 MCP Server、AI 误删数据等），不要先介绍项目。

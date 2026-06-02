# Forge Admin vs JeecgBoot：低代码方向完全不同，别选错了

选低代码平台时，90%的人只看star数。但50K star的JeecgBoot和Forge Admin，低代码方向完全不同，选错了后面全是坑。

我一个朋友去年用JeecgBoot接了个SaaS项目，上线三个月后发现多租户数据隔离不够深，接口安全也没做好，最后硬着头皮重构了权限层。不是JeecgBoot不行，是用错了地方。

今天这篇文章，把两个框架掰开揉碎了对比。看完你就知道该选谁。

（配图建议：两个框架Logo并列对比图，中间加vs字样）

---

## 一、低代码理念——BPM流程驱动 vs 协议驱动

这是两个框架最底层的分水岭。

JeecgBoot的低代码是**BPM流程驱动的**。核心路径是：在线配置表单→代码生成器生成前后端代码→手工Merge到项目里。它的低代码最终产物是什么？是代码文件。所以它的口号叫"AI生成→OnlineCoding→代码生成→手工Merge"。

Forge Admin走的是**协议驱动**路线。核心是一套JSON Schema协议——modelSchema定义数据模型，pageSchema定义页面编排。低代码的最终产物是什么？是JSON协议，不是代码。页面由运行时引擎按协议动态渲染。

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| 驱动方式 | BPM工作流驱动 | JSON Schema协议驱动 |
| 核心路径 | 在线配置→代码生成→手工Merge | AI生成→向导搭建→协议渲染 |
| 最终产物 | 代码文件（需手工合并） | JSON协议（运行时直接渲染） |
| 建表方式 | Online表单在线配置 | ModelDesigner浏览器端设计 |
| 页面模板 | 单表/树表/主子表 | SimpleCrud/TreeCrud/MasterDetail三模板 |

这里有个关键差异：JeecgBoot生成的代码你要自己合并到项目里，改完再生成就要处理冲突；Forge的协议是纯数据，改了配置页面直接变，页面和代码彻底解耦。

做短期项目，代码生成快；做长期产品，协议驱动可维护性强。

（配图建议：两个框架的低代码流程图对比，JeecgBoot侧标注"产出：代码文件"，Forge侧标注"产出：JSON协议"）

---

## 二、代码生成——一键生成全部代码 vs AI生成配置+模板渲染

代码生成器是JeecgBoot的看家本领。

JeecgBoot的代码生成器是真的强：在线配置好表结构，一键生成Controller、Service、Mapper、Entity、Vue页面，全套前后端代码直接可用。然后你手工Merge到自己的项目里。

Forge Admin的路线完全不同。它的代码生成是**AI生成配置+Velocity模板渲染**：AI生成的是一个AiCrudConfig配置JSON（不是代码），然后用28个Velocity模板把配置渲染成规范化的代码包，你下载下来直接用。

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| 生成方式 | 在线配置→一键生成 | AI生成配置→模板渲染→下载 |
| 生成内容 | Controller/Service/Mapper/Entity/Vue全套 | 规范代码包（模板统一结构） |
| AI角色 | AI辅助描述需求 | AI生成配置JSON，不生成代码 |
| 代码结构 | 生成即最终代码 | 模板渲染保证统一规范 |
| 安全注解 | 需手动添加 | 4类安全注解自动注入 |

核心区别：JeecgBoot生成的是最终代码，你要手工Merge，代码风格取决于生成器版本；Forge生成的是模板渲染的规范化代码+AI生成的配置JSON，代码结构统一可控。

换句话说，JeecgBoot是"AI帮你把代码写好"，Forge是"AI帮你把配置写好，模板帮你把代码写好"。

---

## 三、数据权限——颗粒度与扩展性

数据权限是企业级项目的硬需求。两个框架都做了，但深度完全不同。

JeecgBoot基于RBAC模型，支持行级、列级、字段级数据权限。常规的后台管理项目完全够用。

Forge Admin在这方面下了狠功夫：定义了7大DataScopeType——ALL（全部）、SELF（本人）、ORG（本组织）、ORG_AND_CHILD（组织及子组织）、CUSTOM（自定义）、TENANT_ALL（租户全部）、REGION（行政区划）。

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| 权限模型 | RBAC，行级/列级/字段级 | 7大DataScopeType |
| 行政区划权限 | 不支持 | 支持（REGION），省级=全部放行 |
| 实现层 | 业务层过滤 | JSqlParser在Mapper层SQL改写 |
| 跳过机制 | — | DataScopeContextHolder.isSkip() |
| 配置化 | — | SysDataScopeConfig表存储规则 |

Forge最狠的一招是**在Mapper层用JSqlParser改写SQL**。不是在Service层加过滤条件，而是直接在你的SQL的WHERE子句里动态拼接权限条件。这意味着无论你怎么写SQL，权限都绕不过去。同时兼容MyBatis-Plus分页的_mpCount查询，不会漏掉。

如果你的业务涉及行政区划数据权限（政府项目很常见），JeecgBoot需要自己扩展，Forge原生支持。

---

## 四、多租户——插件 vs 深度集成

多租户是SaaS产品的命门。

JeecgBoot支持SAAS多租户，作为平台特性存在。

Forge Admin的做法是**深度集成到SQL层**：MyBatis-Plus多租户插件+JSqlParser自动在SQL中注入tenant_id条件。但这还不是最关键的，最关键的是**五级忽略策略**：

| 优先级 | 策略 | 说明 |
|--------|------|------|
| 1 | 上下文标记 | TenantContextHolder运行时动态标记 |
| 2 | 配置白名单 | 17张系统表默认忽略 |
| 3 | 手动注册 | 代码级动态添加忽略表 |
| 4 | 自动扫描 | 检测表有无tenant_id列，无则自动跳过 |
| 5 | 注解驱动 | @IgnoreTenant注解+切面 |

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| 多租户实现 | 平台特性级 | SQL层注入+五级忽略策略 |
| 线程传递 | — | TransmittableThreadLocal |
| 严格模式 | — | 支持（无法确定租户时抛异常） |
| 动态忽略 | — | executeIgnore()函数式编程 |
| 自动检测 | — | autoDetectTenantColumn自动跳过无租户列的表 |

第五级自动扫描特别实用：你引个第三方表，表里没有tenant_id字段，Forge自动跳过注入，不会报错。JeecgBoot需要手动配置。

做SaaS产品的话，Forge的多租户方案是经过深思熟虑的——不是"支持多租户"，而是"多租户渗透到每一行SQL"。

（配图建议：五级忽略策略的金字塔层级图，从上到下排列）

---

## 五、安全能力——各有侧重

安全这块，两个框架的思路差异很大。

JeecgBoot用的是Shiro/Spring Security+JWT，颗粒化权限控制，菜单权限、按钮权限、数据权限都到位了，满足常规企业级需求。

Forge Admin在API安全层面做了大量工作：

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| 认证框架 | Shiro/Spring Security | Sa-Token |
| 接口加解密 | — | RSA密钥协商+SM4/AES+RequestBodyAdvice |
| 防重放攻击 | — | ReplayTokenCache+nonce机制 |
| 文件鉴权 | — | 时效URL+私有文件权限校验 |
| 分布式幂等 | — | 3策略+SpEL键生成+Redisson锁 |

Forge的接口加解密流程设计得很完整：客户端先拿RSA公钥→生成本地SM4/AES会话密钥→RSA加密后传给服务端→后续请求全部用会话密钥加解密。再加上防重放的nonce机制，API安全性直接拉满。

分布式幂等也有三套策略：STRICT（严格拒绝重复，适合支付）、RETURN_CACHE（返回缓存结果，适合查询）、TOKEN_REQUIRED（先拿Token再提交，适合表单）。

如果你做的是一般企业后台，JeecgBoot的安全能力够了。但如果是金融、政务、支付类场景，Forge在API安全上的投入是实打实的。

---

## 六、AI集成——广度 vs 深度

AI这块最容易被误解。很多人一看JeecgBoot的AI功能列表就觉得很全，但你要看AI是"贴上去的"还是"长进去的"。

JeecgBoot的AI覆盖很广：AI对话助手、知识库、流程编排、AI建表、AI写作，Skills矩阵很丰富。但本质上是功能层面的叠加——AI是一个独立模块，帮你做一些事情。

Forge Admin的AI是**深度嵌入核心流程**的：

| 对比维度 | JeecgBoot | Forge Admin |
|---------|-----------|-------------|
| AI定位 | 功能模块叠加 | 核心流程嵌入 |
| AI供应商 | DeepSeek/通义千问/ChatGPT等 | OpenAI/DeepSeek/Ollama等多供应商统一接入 |
| AI+低代码 | AI辅助描述需求 | AI直接生成crudConfig JSON驱动页面 |
| AI+大屏 | AI辅助设计 | AI生成大屏配置，独立子项目 |
| 工程化 | — | 熔断器+客户端缓存+流式输出 |

Forge的AI不做"聊天助手"这种外围功能。它做的是：你用自然语言描述需求→AI生成低代码配置JSON→运行时引擎直接渲染页面。AI不是帮你"加速开发"，AI本身就是开发流程的一环。

另外Forge的AI集成有完整的工程化考虑：多供应商统一接入接口、熔断器防止AI服务挂了拖垮系统、客户端缓存减少重复调用、流式输出优化体验。

简单说：JeecgBoot的AI是全（覆盖面广），Forge的AI是深（嵌入核心链路）。

---

## 总结：到底选谁？

别和稀泥，直接给结论：

**选JeecgBoot，如果你：**

- 需要BPM工作流+表单设计器做OA/审批类系统
- 项目周期短，独立开发快速出活
- 业务以流程驱动为主，对多租户和安全没有极致要求
- 想要一个功能全面的"全家桶"，什么都有

**选Forge Admin，如果你：**

- 做SaaS产品，多租户+数据权限+接口安全需要深度集成
- 团队协作开发长期产品，代码结构统一可控比"一键生成"更重要
- 想用AI深度驱动低代码，而不是把AI当聊天助手
- 做金融/政务/支付等高安全场景
- 需要行政区划级数据权限

一句话总结：**JeecgBoot是流程驱动的全能选手，Forge Admin是协议驱动的深度玩家。一个做广度，一个做深度。**

---

你正在用什么后台框架？评论区聊聊你的选型经历，踩过哪些坑。

**项目地址：**

**Forge Admin：**
- 后台管理演示：[http://81.70.22.48:8084/forge/login](http://81.70.22.48:8084/forge/login)
- 在线文档：[http://81.70.22.48:8084/forge-docs/](http://81.70.22.48:8084/forge-docs/)
- 大屏演示：[http://81.70.22.48:8084/forge-report/](http://81.70.22.48:8084/forge-report/)
- Gitee：[https://gitee.com/ForgeLab/forge-admin](https://gitee.com/ForgeLab/forge-admin)
- GitHub：[https://github.com/yaomindong1996/forge-admin](https://github.com/yaomindong1996/forge-admin)

**JeecgBoot：**
- 官网：[http://jeecg.com](http://jeecg.com)
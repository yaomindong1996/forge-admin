# Forge 踩坑索引

> 这里只放目录和写入规则。**不要把整本踩坑当每次会话必读材料。**
> 先读本索引，再按当前任务打开对应分类文件。

## 怎么用

- 新会话：读本文件标题列表，定位相关分类即可，不要通读分类全文。
- 改前端 / 路由 / Vite：`pitfalls/frontend.md`
- 改低代码 / 设计器 / 业务对象：`pitfalls/lowcode.md`
- 改流程 / BPMN / 待办：`pitfalls/flow.md`
- 改安全 / 加密 / 租户：`pitfalls/security.md`
- 改 Flyway / 索引 / 唯一键：`pitfalls/db-flyway.md`
- 改能力开放 / MCP：`pitfalls/capability.md`
- 其它后端框架问题：`pitfalls/backend.md`

## 写入规则

只记录真实故障、根因和规避方式。编码规范不要写到这里，写到 `AGENTS.md` 第 5 章或 `code-copilot/rules/coding-style.md`。

格式：

```markdown
## 问题标题

**发现日期**: YYYY-MM-DD

**问题描述**:
现象。

**解决方案**:
怎么避免、怎么修。
```

标题不要再手工编号。历史条目里的 `## 102.` 只是旧编号，检索时用标题关键词。

## 分类目录

### [前端 / 构建 / 路由](pitfalls/frontend.md)（18）

- Naive UI 表格居中不能只设置 `text-align`
- Naive Dialog 取消回调不能意外返回 false
- 8. 异步弹窗使用 v-if 首次打开无响应
- 7. SSE 流式对话前端解析不完整导致非实时输出
- 7. Blob 下载响应被统一错误拦截器误判为未知异常
- 10. SSE 流式响应解析不能只按 `\n\n` 分割
- 14. forge-report-ui 图标必须先注册到统一 icon 插件
- 17. Vite 懒加载依赖二次预构建导致菜单点击后整页刷新
- 20. 前端生产构建默认 Node 堆内存不足
- 49. 应用入口套件目录父级不能回填为实际菜单 ID
- 61. window.$message.loading 不返回 Naive 原生销毁句柄
- 62. Vite dev server 启动报 EMFILE: too many open files
- 105. 路由关联查询转运行时实体时不能丢 tenantId
- 106. AI 治理核心组件不能可选注入，非模型故障不能污染健康状态
- 117. Naive 组件内部 CSS 变量不能作为自定义页面主题 Token
- 162. 租户切换后的会话刷新不能从稳定用户 ID 退回用户名查询
- 169. CSS 尺寸变量在独立挂载场景需要兜底
- 182. Vue 客户端组件模板不能直接承载运行时 style 标签

### [低代码 / 设计器 / 业务对象](pitfalls/lowcode.md)（83）

- 表单发布检查必须展平 row/col 子组件
- 预览/发布前的派生运行配置不能传播应用设计变更
- 存量对象型应用不能只按新版页面树判空
- 对象设计版本不能复用 CRUD 发布版本
- 移动低代码字段事件不能只实现一次查询和回填
- 低代码动作种子只有 designer_options 没有发布版本时运行必然失败
- 1. AiCrudPage组件占位符格式错误
- 2. 分页参数名不一致导致分页失效
- 6. Vite Outdated Optimize Dep 导致动态路由模块加载失败
- 7. 业务记录选择器空 DTO 导致“选择器缺少业务对象编码”
- 7. 动态菜单路径高亮必须支持路由参数匹配
- 8. form-create 默认字段名不能沉淀为低代码业务字段编码
- 9. 移除前端依赖后必须同步清理 Vite optimizeDeps
- 9. 前端全量构建 Node 默认堆内存不足
- 10. fcDesigner 不能在窄栅格里被动压缩
- 11. fcDesigner 布局组件不能当业务字段处理
- 7. 树形低代码模板 beforeRenderForm 丢失行主键导致详情 URL 拼出 undefined
- 7. Naive UI 当前版本不导出 NSegmented
- 6. 外部接口代理返回二层加密壳导致图表数据异常
- 6. AiCrudPage 表格列配置不能直接使用 Naive UI title/key
- 11. Naive Select/TreeSelect 回显 Long ID 必须统一字符串类型
- 12. AiCrudPage 编辑嵌套明细不回显
- 18. AiForm 树选择必填校验误判已选 ID 为空
- 19. 旧技能示例与 AGENTS.md 规则冲突
- 22. 目录式动态路由生成空父记录导致 Component 为 null
- 24. ai_crud_config 表 status 字段类型为 char(1) 导致 Flyway 迁移失败
- 27. Vue 属性面板 watcher immediate 必须处理空选中项
- 28. 前端路由 query 中的雪花 ID 禁止转 Number
- 29. 业务对象设计器同步页面 Schema 必须保留 modelRefs
- 30. 业务对象发布前必须重新合并关系到 pageSchema
- 31. 运行态字段组件变更必须覆盖主表表单和子表明细
- 32. 设计器部分保存 DTO 禁止默认空集合
- 33. 表单优先设计保存必须同步运行态 fieldSettings
- 34. 组织树 optionSource 为空时必须回退默认数据源
- 35. 表单设计新增字段需要触发受控 DDL 同步
- 36. 跳转桥接路由不能登记顶部 Tab
- 37. fcDesigner 画布列数必须写入 rule.col.span
- 38. fcDesigner 删除组件不会自动删除字段资产
- 39. fcDesigner 布局组件的 ref_ 临时值不能进入 Forge Schema
- 40. 业务对象编码推理只应自动作用于新建
- 41. Flyway 会扫描注释和字符串中的占位符
- 50. form-create 随机字段 ID 未同步导致低代码页面引用不存在字段
- 51. 对象设计保存后必须同步关联运行态入口菜单
- 52. 字段资产全局保存不能强制要求当前选中字段
- 53. 表单优先 viewSchema 的 fieldCode 也是字段改名/删除引用点
- 54. flow server 直接引入 generator 插件会暴露管理端桥接依赖
- 56. AiCrudPage 详情态字典字段不能依赖禁用控件回显
- 58. 手写/隐藏业务路由必须同步 sys_resource 和角色授权
- 69. 低代码字段编码和数据库列名不能共用 camelCase
- 70. 应用中心新建业务对象必须选择低代码运行数据源
- 81. 应用中心对象设计器 URL 统一使用 object/:objectCode/designer
- 93. 低代码设计器 zone props 保存成功不代表运行态可见
- 94. 低代码自动编号不能只依赖配置迁移
- 95. 业务对象设计器重建字段必须保留运行态元数据
- 96. 表单设计器 schema 归一化不能丢弃校验预设字段
- 97. 前端默认加密时后端漏 @ApiDecrypt 会表现为 DTO 字段全空
- 113. 表格拖拽滚动不能抢占单元格文本选择
- 115. CREATE TABLE IF NOT EXISTS 不会升级存量表结构
- 116. 业务编码作为 LIKE 前缀时下划线会扩大匹配范围
- 118. 业务变量不能与低代码字段映射强制等同
- 119. 号段数据库水位不是最后实际使用值
- 120. 编码规则的 ruleCode 和 SEQ segmentKey 都是计数器永久身份
- 121. 高基数号段缓存必须有界且乐观重试不能运行在 RR 旧快照中
- 122. AiForm 数字字段类型必须统一归一化
- 125. AiCrudPage 的父容器必须提供明确高度
- 128. 批次迁移必须先全域预检并让异常逃逸事务回调
- 130. 列表设计器的字段目录与稳定模型分离时会出现“字段点不动”
- 131. 页面表单与 CRUD 不能停在“有字段、无数据存储”的半绑定状态
- 132. 草稿渲染接口放行不代表页面 CRUD 已进入设计预览
- 133. 组合编码长度必须按最终落库字段校验
- 134. 元数据准备完成不等于物理数据表已经存在
- 135. 自动托管表的发布状态不能依赖上一次保存时的同步快照
- 136. Vue 监听器返回新数组会把无关对象替换误判为请求条件变化
- 137. 页面查询字段与对象查询白名单分裂会造成静默失效
- 141. 整体草稿版本不能作为数据库结构是否同步的证据
- 145. 页面运行期间加密开关变化会让显式敏感请求绑定为空 DTO
- 170. 显式空 Schema 不能与缺失 Schema 使用同一个空集合分支
- 172. 表单设计器旧状态联动不能只保存 `props.__events`
- 173. MONEY 组件和数据库金额单位必须形成完整运行时协议
- 175. JSON 快照补丁不能靠肉眼猜字段数组下标
- 176. 数据区块的空 `fieldRefs` 应回退到运行时字段目录
- 177. 业务对象发布不能重置应用入口配置
- 180. 运行字段基线会掩盖表单组件的结构变更

### [流程 / Flowable / BPMN](pitfalls/flow.md)（51）

- 门户外层 deep 样式不能覆盖嵌套加载容器
- 动态 CRUD 事件不能把运行配置对象码当作流程标准对象码
- Flowable 固定审批人不能保存为用户变量表达式
- 业务流程任务表单选项不能只读 value/code
- try-with-resources 外的失败审计不能依赖上层租户上下文
- 3. BPMN XML属性值带前导空格导致匹配失败
- 4. SPEL表达式执行日志缺失导致排查困难
- 7. 代码应用已有业务对象时设计器误走低代码空模型
- 7. 示例流程初始化覆盖用户 BPMN 节点配置
- 7. form-create 设计器默认锁定字段 ID
- 5. Flowable 管理员转派后待办消失
- 7. Flowable 委派态任务不能直接完成
- 7. BPMN 设计器 XML 回传会清空撤销栈
- 8. AI 生成 BPMN 的 BPMNPlane 指向错误导致画布导入失败
- 42. Flowable 节点表达式变量必须由低代码映射提供
- 44. sys_flow_task.assignee 必须存用户 ID
- 45. 单据详情运行态也要归一 objectCode/configKey
- 46. 低代码 START_FLOW 不能同时走 custom-action 和内置发起
- 47. Flow 服务 sys_flow_business 也必须做 businessKey 幂等
- 48. 同一流程实例重复待办优先检查 BPMN 重复 sequenceFlow
- 60. Flowable 7 流程取消事件不能强转 FlowableEntityEvent
- 63. 流程统一表单字段目录为空时条件分支无法选择表单字段
- 64. 流程条件分支标签点击必须保留 edgeId
- 65. Flowable 默认分支不能导出 conditionExpression
- 66. 条件网关不能把分支数量固定死为 2
- 67. 条件分支画布标签不要直接展示 SpEL 原文
- 68. BPMN 只保留 conditionExpression 时需要反解析表单规则
- 75. 流程待办消息完成后必须自动置已读
- 76. 外部审批表单按钮 loading 必须绑定父级提交状态
- 77. 流程完成事件必须携带完整变量快照
- 78. 前端禁止把雪花 Long ID 转成 Number
- 79. 代码表单 Provider 不能依赖低代码运行配置 configKey
- 80. AiForm 字段权限必须由组件和调用端共同接入
- 82. 流程字段权限新旧键名必须新键优先并双写
- 83. 流程设计器全局保存前必须提交打开中的节点抽屉草稿
- 84. 运行时字段权限必须复用共享归一化
- 85. 节点表单资产选择不能清空字段权限
- 86. 代码应用配置不能替代 Provider 当前字段基准
- 87. 代码表单资产只改设计器不改运行时会导致审批仍走写死 Provider 配置
- 88. Flowable 流程定义标识不能直接字符串比较
- 89. 自定义业务表单不要重复请求父级已加载的待办上下文
- 90. 驳回到修改节点的业务状态不能只依赖 TASK_COMPLETED 变量
- 98. 审批运行态表单不能重建简化字段配置
- 99. 低代码审批详情不能只渲染主表 AiForm
- 100. 流程表单资产不能只读取业务对象设计草稿
- 101. 低代码业务表单空字段权限不能当成全只读
- 161. 设计态权限校验不能把当前用户授权当成权限资源目录
- 165. 多结果出口共享同一后继不能按边数直接展开布局
- 166. 条件分支端口是有序语义，不能作为集合排序
- 167. DAG 分支路由必须同时处理跨层穿卡和三种顺序一致性
- 183. 捕获参与当前事务的下游异常不能清除 rollback-only

### [后端框架 / Spring / Maven](pitfalls/backend.md)（33）

- Redisson 接口存在不代表社区版可以运行
- 受管 JSON 缓存不能只恢复容器外层类型
- 消息“批量接口”仍需检查数据库是否逐条更新
- 6. 本地文件存储返回相对访问地址导致图片渲染失败
- N. 问题标题
- 13. sys_file_metadata 不是标准业务审计表
- 16. 动态查询 SQL 注入检测误判字段名
- 25. 后端 Maven 编译必须使用 JDK 17
- 57. 运行时异常返回给前端前必须去掉异常类名前缀，前端错误提示不能只剩一句 message
- 59. forge-create minimal-admin 保留 generator 时必须提供 AI 降级适配器并补齐依赖
- 74. Spring Boot 3.5 与 Redisson 3.34.1 会触发登录 Redis 适配死循环
- 92. 根 POM 固定 skip 会让定向测试看起来通过但实际未执行
- 102. 超级管理员全量组织兜底不能当成真实绑定组织
- 109. Mapper XML 跨模块迁移后必须 clean install 清除旧资源
- 110. 新增跨模块 API 后单模块测试可能解析到本地仓库旧构件
- 123. 远程异步启动的 5xx 和响应解析失败不是确定性失败
- 124. Spring Bean 有多个构造器时必须标明生产注入入口
- 129. 直接执行 Maven surefire:test 不会重新编译测试源码
- 139. 已验签手机号不等于可以用 LIMIT 1 任意映射用户
- 143. Spring Service 增加测试便利构造器后必须显式选择注入构造器
- 147. 带 `@Transactional` 的无接口 Bean 不能声明为 final
- 分页复杂查询的 COUNT(*) 可能触发 JSqlParser 解析失败
- 148. MyBatis InnerInterceptor 创建期不能直接注入依赖 Mapper 的 Service
- 156. 幂等模板不能把受保护 action 的业务异常归类为基础设施故障
- 160. 画布 JSON 能解析不等于流程样例合法
- 164. JSqlParser 4.9 会重排 ORDER BY ... FOR UPDATE 导致 SQL 语法错误
- Pitfall: BaseMapper 子类无 saveBatch 能力
- Pitfall: LEFT JOIN 直接 GROUP BY 导致结果集膨胀
- Pitfall: FIND_IN_SET 替代 LIKE 匹配逗号分隔列表
- 168. 刷新树数据后必须重绑选中对象
- 174. MySQL 派生表外层引用的每个计算列都必须显式起别名
- 178. Generator 新增强制适配器时必须检查所有聚合服务
- 181. Mockito 匹配重载方法时必须指定参数类型

### [安全 / 加密 / 租户 / 鉴权](pitfalls/security.md)（19）

- 多租户拦截器会破坏 LIMIT 1 FOR UPDATE 顺序
- 登录密码 RSA 不能复用通用 API 传输加密开关
- 登录前、Token 事件和定时任务访问租户表必须显式建立上下文
- 租户拦截 SQL 不要使用 MySQL NULL 安全等号
- 5. 报表项目保存/读取接口缺少加解密注解导致配置不生效
- 8. 顶部菜单目录不应直接按自身 path 跳转
- 6. SSO 接口缺少 `@ApiDecrypt` 会导致请求参数为空
- 7. X-Inner-Call 只能由可信内部系统配置触发
- 9. 跨系统 SSO 首跳前的 `/crypto/exchange` 必须匿名放行
- 21. 菜单路径与文件自动路由不一致导致 404
- 71. 异步日志补全用户信息必须忽略租户条件
- 72. 租户业务数据源切换后数据权限不能访问业务库里的平台表
- 73. 超级管理员区划树被登录用户 regionCode 误裁剪
- 104. DashScope Core、Starter 和 Compatible 地址不能混为一体
- 114. 加解密总开关关闭时不能在 Bean 构造阶段校验密钥
- 127. 外部化 Secret 必须同时封堵高优先级数据库配置源
- 159. REQUIRES_NEW 建单后外层 REPEATABLE_READ 可能仍看不到新记录
- 171. 动作路径 `record.*` 的单测必须构造服务端权威记录上下文
- 超级管理员不能全局忽略租户隔离

### [数据库 / Flyway / 索引](pitfalls/db-flyway.md)（11）

- 15. Flyway 已执行版本脚本不能二次修改
- 23. 菜单活跃项函数签名不一致导致选中状态停留
- 26. Flyway 已执行版本禁止复用或改写
- 43. Flyway 迁移 tenant_id 0 归一化前必须先处理唯一键重复
- 55. Flyway 低版本补脚本会被默认校验拦截
- 91. MySQL 唯一索引遇到 NULL 不能作为幂等防线
- 126. Flyway 替换旧索引时不能假设历史索引仍存在
- 146. 字典请求失败结果不能写入全局缓存
- 163. 删除业务应用后保留的停用入口不能永久阻断业务域删除
- 179. 逻辑删除业务键回填去重必须与最终唯一索引使用相同维度
- 企业协同连接根的 client_id/client_secret 不能继续 NOT NULL

### [能力开放 / MCP / 动作发布](pitfalls/capability.md)（21）

- 低代码增强被选中不代表运行时拿到了启用版本正文
- Capability 短期 Token 不能交给 Sa-Token 解析
- 6. 关联功能使用不同条件注解会产生“Bean 存在但路由 404”
- 103. AI/MCP 机器调用的数据权限必须按执行器 fail-closed
- 107. MCP 配置声明和游标查询绑定不能替代启动硬闸门与签名
- 108. 凭据认证不能用完整实体回写，也不能依赖认证前租户上下文
- 111. 能力授权版本不能混用能力主表的当前 source binding
- 112. 流程办理 DTO 的 userId 和 taskId 都不能作为授权依据
- 138. 管理控制面与运行时共用特性开关会产生无法解释的 404
- 140. Capability 开关开启但 Pepper 空值会在身份模块启动期失败
- 142. Maven Reactor 未包含已修改 Starter 时会误用本地仓库旧版类路径
- 144. 依赖型特性开关必须在自动配置中编码联动关系
- 149. 公开 OAuth/OpenAPI 入口跳过登录租户后必须自行建立可信租户上下文
- 150. FLOW_ACTION START 的 recordId 不是任意外部业务号
- 151. 跨运行数据源建单不能假装与本地幂等日志原子提交
- 152. 业务动作已启用不等于它已可执行或可开放
- 153. 名为“新增”的 OPEN_PAGE 动作不是服务端创建记录能力
- 154. 受控发布器不能生成能力内核未实现的 Schema 关键字
- 155. Flyway 新增字典后 SPA 全局缓存可能一直保留旧列表
- 157. 桌面常驻属性面板不能用移动端抽屉显隐状态判断是否保存
- 158. 低代码业务字段编码不能被当作同名物理列

合计 232 条。

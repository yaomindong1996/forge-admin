
---
alwaysApply: true
---
# 编码规范
## 1. 命名
- 类名：大驼峰，见名知意
- 方法名：小驼峰，动词开头
- 常量：全大写下划线分隔
- 抽象类以 Abstract 或 Base 开头
- 测试类以被测类名开头，Test 结尾
- 禁止拼音、中英混拼命名
## 2. 异常处理
- 业务异常使用自定义 BusinessException，携带错误码
- 系统异常向上抛出，由统一异常处理器兜底
- 禁止吞掉异常（空 catch）
- catch 中必须记录日志
## 3. 日志
- Controller 入口打 INFO，含请求关键参数
- 异常打 ERROR，含完整堆栈
- 禁止在日志中打印用户敏感信息
## 4. 其他
- 所有 if/else/for/while 必须加大括号，禁止 `if (x) return;`
- 写接口必须考虑幂等
- 涉及并发场景必须说明同步策略
- 魔法值必须定义为常量
- 外部接口调用必须设置超时（默认 3s）并做降级
- 状态变更必须走状态机，禁止业务代码直接乱改状态字段
- 禁止提交包含用户个人信息的测试数据
## 5. git提交规范
- 禁止 master 分支变更：编码前检查当前分支，master 上立即停止
- 自动 Commit：每个 task/fix 完成后自动 commit，保持一个 task 一个 commit
- Commit 必须可编译：commit 前执行编译检查
- 禁止自动 Push：push 由用户主动触发，保留审查机会
- Message 格式：[<变更名>] <中文简述>

## 6. 数据库 SQL 规范
### 6.1 租户 ID 规则（重要）
- **业务数据**（字典 `sys_dict_type`/`sys_dict_data`、配置等需被租户查询到的数据）的 `tenant_id` **必须设为 `1`**（默认租户），**禁止设为 `0`**
- **原因**：项目的 `TenantLineInnerInterceptor` 会自动在所有 SELECT 查询中追加 `WHERE tenant_id = <当前登录用户租户ID>`，`tenant_id=0` 的数据对非零租户用户不可见，导致前端字典加载为空
- **例外**：`sys_resource`（菜单/权限）表不在租户拦截范围内，其 `tenant_id` 保持 `1` 即可
- **排查**：如果前端字典组件（DictSelect、DictTag、useDict）加载为空，首先检查数据库中对应字典数据的 tenant_id 是否为 1

### 6.2 其他 SQL 规范
- 所有业务表必须包含基础字段：`id`, `tenant_id`, `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
- 使用 `utf8mb4` 字符集，`InnoDB` 引擎
- 为高频查询字段创建索引，组合索引遵循最左前缀原则
- 禁止将数据库凭据提交到仓库
- Flyway 迁移脚本中禁止直接写业务 `${...}` 模板；确需入库时使用 `CONCAT('$', '{token}')` 拼接，并用 `rg -n '\$\{[^}]+\}' forge-server/db/migration` 确认无残留

## 7. 前端规范
### 7.1 字典使用
- 下拉选项、状态标签等**禁止硬编码**，必须使用字典组件
- 加载字典：`const { dict } = useDict('dict_type_1', 'dict_type_2')`
- 表单下拉：使用 `DictSelect` 组件，或在 editSchema 中通过 `computed` 引用 `dict.value['xxx']` 作为 `props.options`
- 表格标签渲染：使用 `DictTag` 组件，配合字典的 `listClass` 字段自动映射颜色
- Schema 必须定义为 `computed`，确保字典数据异步加载后选项能响应式更新

### 7.2 图片/文件字段渲染
- `imageUpload` 组件存储的值是 **fileId**，不是完整 URL
- 表格列渲染图片时，**必须使用 `getFileUrl()` 将 fileId 转换为下载链接**：
  ```js
  import { getFileUrl } from '@/utils/file'
  // 正确
  h(NAvatar, { src: getFileUrl(row.logo), size: 32, round: true })
  // 错误 - fileId 无法直接作为图片 src
  h(NAvatar, { src: row.logo, size: 32, round: true })
  ```
- **鉴权图片必须使用 `AuthImage` 组件**：
  - 文件下载接口 `/api/file/download/{fileId}` 需要鉴权（Bearer Token）
  - 直接用 URL 作为 `<img>` 或 `<NAvatar>` 的 `src` 属性**不会携带 Authorization header**，导致 token 无效错误
  - **解决方案**：使用 `AuthImage` 组件，它会通过 fetch 带 token 获取图片并转为 blob URL：
  ```js
  import AuthImage from '@/components/common/AuthImage.vue'
  // 正确 - AuthImage 自动处理鉴权
  h(AuthImage, {
    src: row.logo,
    imgStyle: { width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' }
  })
  // 错误 - NAvatar src 不会带 token，接口返回"未能读取到有效 token"
  h(NAvatar, { src: getFileUrl(row.logo), size: 32, round: true })
  ```

### 7.3 操作列按钮
- 操作列按钮超过 2 个时，`AiCrudPage` 会自动将多余的折叠到"更多"下拉菜单
- 按钮顺序安排：最常用的操作放前面（会被内联显示），低频操作放后面（收入"更多"）
- 操作列宽度需根据按钮数量合理设置：2个内联按钮约 150-180px

### 7.4 Pinia 状态管理（强制）

> 与 AGENTS.md 5.14 同源，此为编码层落地细则，适用于所有 AI 生成/修改的前端代码。

- **能用 Pinia 的场景必须用 Pinia**：跨组件、跨面板、跨层级的共享状态与通信（设计器 schema、选中 ID、面板 UI 状态、多组件联动状态等）必须沉淀到 Pinia store；项目已引入 Pinia 3，禁止因为"嫌麻烦"继续用 props/emit 层层透传。
- **透传超过 2 层即违规**：同一个状态经 props + emit 转手超过 2 层必须改 store，子组件直接读写 store，父组件不再充当数据中转站。
- **存量巨型组件渐进式改造模式**：入口组件接收 props 后 `syncFromProps` 同步进 store，同时 watch store 变化对外 emit 兼容存量父组件；拆出的内部面板子组件全部读写 store，不再接收中间 props。
- **store 文件组织**：`src/stores/<domain>/xxxStore.js`，导出 `useXxxStore`；一个 store 聚焦一个领域（如 `stores/designer/formDesignerStore.js`），禁止大杂烩全量 store。
- store 内用组合式 API（`defineStore('xxx', () => { ... })`）写法，与项目 `<script setup>` 风格一致。

### 7.5 组件拆分与单文件规模（强制）

- **Vue SFC 超过 800 行（模板+script+style 合计）必须拆分；超过 2000 行禁止提交**，必须先重构再提交。
- 拆分优先级：属性面板按分区拆（一个 collapse-item / tab-pane 一个子组件文件）→ 可复用逻辑抽 composable（`useXxx.js`）→ 纯函数下沉模块级 `utils.js`。
- 拆出的子组件与父组件同域放置：如 `forge-form-designer/panels/FooPanel.vue`，禁止散落在无关目录。
- 新增功能禁止继续往巨型 SFC 里追加模板/逻辑；发现所在文件已超限时，先拆分再实现。

## 8. 后端架构规范
### 8.1 循环依赖
- Service 之间**禁止相互注入**导致循环依赖
- 如需跨 Service 协调，将协调逻辑上提到 Controller 层

### 8.2 API Key 等敏感字段
- 接口返回中包含 API Key、Secret 等敏感字段时，**必须脱敏处理**
- 脱敏方式：保留前4后4位，中间用 `****` 替代

### 8.3 请求体禁止 Map
- Controller 写接口必须用明确的 DTO/VO 接收 `@RequestBody`，禁止 `@RequestBody Map<String, Object>` 再 `params.get("xxx")`
- 正确：`public RespInfo<?> approve(@RequestBody FlowTaskApproveDTO dto)`
- 错误：`public RespInfo<?> approve(@RequestBody Map<String, Object> params)`
- 允许例外：低代码动态记录体、流程变量字段 `variables`、真正动态键值的元数据
- 固定字段（如 `taskId`、`comment`、`userId`、`action`）必须进 DTO，不能整份请求都用 Map
- 新增写接口先建 DTO/VO 再写 Controller，禁止用 Map 占位

### 8.4 业务状态必须用枚举
- Java 业务代码禁止魔法数字/字符串状态（如 `setStatus(2)`、`getStatus() == 1`、`setEnabled(1)`、`"terminated"`），必须使用枚举
- 通用启用/停用（`enabled`、`visible`、`userStatus`、`isEnabled` 及同类 0/1 开关）用 `com.mdframe.forge.starter.core.enums.EnableStatus`
  - 写入：`entity.setEnabled(EnableStatus.ENABLED.getCode())`
  - 比较：`EnableStatus.ENABLED.matches(entity.getEnabled())`
- 多状态业务字段必须建本模块枚举，提供 `getCode()` / `matches()`
- 实体字段可继续用 `Integer`/`String` 与数据库列对齐，不要把实体字段改成枚举类型
- Mapper XML / SQL 允许字面量，禁止在 XML 中绑定 Java 类全名
- 前端展示走字典，`dict_value` 必须与后端枚举码一致
- 不要套 `EnableStatus`：Boolean 开关、编码规则段配置、`readonly` / `isDefault` / `validationPassed` 等非启用语义
- 测试可用字面量 0/1 作为存储码，生产代码不行


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
- 写接口必须考虑幂等
- 涉及并发场景必须说明同步策略
- 魔法值必须定义为常量
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

## 8. 后端架构规范
### 8.1 循环依赖
- Service 之间**禁止相互注入**导致循环依赖
- 如需跨 Service 协调，将协调逻辑上提到 Controller 层

### 8.2 API Key 等敏感字段
- 接口返回中包含 API Key、Secret 等敏感字段时，**必须脱敏处理**
- 脱敏方式：保留前4后4位，中间用 `****` 替代

# 移动端低代码运行时联调验收清单

> 适用分支：`codex/mobile-lowcode-runtime-refactor`
> 原则：后端协议保持不变；H5、微信小程序和管理端消费同一发布态配置与运行时接口。

## 1. 联调准备

- 准备一个已发布的低代码 CRUD 配置，至少包含文本、数字、字典、日期、上传、人员、组织、区域、关联记录和一组明细字段。
- 准备一个包含 `list/form/detail` 页面区块、移动导航和底部操作栏的独立挂载页。
- 准备一条真实待办及一条已办记录；BPMN 节点需配置可见、只读、可编辑、必填字段以及主子表权限。
- H5 网关需将 `/api/flow/**`、`/ai/business/flow/**` 转发到 Flow 服务，其余 `/ai/**`、`/system/**` 转发到 App 服务。
- 微信小程序的 `VITE_MP_API_BASE_URL` 必须是 HTTPS 绝对地址，并加入 `request`、`uploadFile` 合法域名。
- 全程使用测试账号和测试数据，不在日志或截图中保留个人敏感信息。

## 2. 协议基线

| 场景 | 请求 | 预期 |
|---|---|---|
| 发布态配置 | `GET /ai/crud-config/render/{configKey}` | 返回与管理端一致的 schema、childrenConfig、options 和页面配置 |
| 列表 | `GET /ai/crud/{configKey}/page?pageNum=1&pageSize=10` | 分页字段正确；记录 ID 不发生精度损失 |
| 详情 | `GET /ai/crud/{configKey}/{id}` | 主表、子表和字典回显完整 |
| 新建 | `POST /ai/crud/{configKey}` | 使用发布态字段协议写入，不出现移动端专用字段 |
| 修改 | `PUT /ai/crud/{configKey}` | Long/Snowflake ID 以字符串从页面传递，后端正常绑定 |
| 删除 | `DELETE /ai/crud/{configKey}/{id}` | 复用动态 CRUD 的逻辑删除/审计行为 |
| 查询源 | `POST /ai/lowcode/query-source/execute` | 只执行后端登记的受管查询源 |
| 关联记录 | `POST /ai/business/selector/query` | 搜索、分页、多选、显示字段和字段映射正确 |

## 3. 普通运行页

- 列表查询、展开/收起筛选、重置、上一页/下一页工作正常。
- 新建、编辑、详情在同一运行内核切换；返回列表后查询条件和分页符合预期。
- 文本、数字、金额、单选、复选、开关、日期时间、范围、上传、图片、签名、扫码均可操作。
- 人员、组织、区域、树、级联、关联记录和自定义选择器使用移动底部面板；搜索与加载更多无重复记录。
- 主子表新增、删除、必填校验、保存与只读回显正确。
- 未登记组件显示明确的“不支持”占位，不得回退为可编辑文本框。
- 动态 Vue/脚本不执行；iframe 在小程序中阻断，在 H5 未配置白名单时保持安全降级。

## 4. 独立挂载页

- 从应用门户进入以及直接打开 `app-entry`，最终都进入统一 `lowcode-runtime`。
- `applicationId/appId/pageId/pageCode/configKey` 参数完整透传。
- 页面标题、副标题、返回地址、内容边距、底部安全区和移动导航按发布配置生效。
- 连续打开两个不同页面时，前一页的列表、搜索、主表、子表和字典状态不串页。
- 表格、树表、主从详情在窄屏以卡片、折叠区或全屏选择器呈现，不出现桌面横向布局硬搬。

## 5. 审批动态表单

| 场景 | 请求/动作 | 验收点 |
|---|---|---|
| 待办上下文 | `GET /ai/business/flow/task-form-context` | 返回业务记录、发布态表单、节点字段权限和动作权限 |
| 暂存 | `PUT /ai/business/flow/task-form-context` | 只提交可编辑字段；必填和主子表校验正确 |
| 同意/驳回 | `POST /ai/business/flow/task-action` | 配置型业务表单由同一业务动作完成保存与流程办理 |
| 通用流程动作 | `/api/flow/task/approve|reject|return|delegate` | 非配置型任务保持原流程接口行为 |
| 已办/历史 | `GET /ai/business/flow/task-form-context/readonly` | 全表单强制只读，附件、签名、明细和流程轨迹可查看 |

- 验证字段权限优先级：节点 v3 动态数组权限 > v2 主子表权限 > 旧字段权限。
- 验证隐藏字段不渲染、不校验、不提交；只读字段不可通过页面操作修改。
- 验证必填审批意见、签名、必审要点、退回目标节点和委派人员选择。
- 验证同意、驳回、退回、委派成功后任务离开待办；重复点击不产生重复办理。

## 6. 文件、机器码与多端

- 附件、图片、签名统一上传到 `/api/file/upload`，表单保存 fileId，不保存临时 URL。
- H5 使用 Fetch/FormData，小程序使用 `uni.uploadFile`；失败时保留明确错误且不提交空 fileId。
- 二维码、CODE128、CODE39、EAN13、UPC 在 H5 和微信开发者工具中均可扫码识别。
- 微信小程序请求不依赖 Vite 代理；无绝对网关地址时应快速失败并给出配置提示。

## 7. 回归与证据

执行 `test-spec.md` 中的定向测试、H5 构建、小程序构建和 `git diff --check`。真实联调需保留以下证据：

- 使用的配置键、页面编码、任务 ID（脱敏）与测试环境版本；
- 每个失败请求的路径、状态码、响应错误码和可复现步骤；
- H5 与小程序关键页面截图；
- 审批前后业务状态、任务状态和流程历史的核对结果。

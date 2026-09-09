# H5 低代码运行时多区域单页改造任务

> status: complete

## 阶段 A：协议与基础组件

- [x] A1 增加 pageSections 纯函数协议，覆盖模式过滤、字段覆盖、子表定位和底栏动作显隐。
- [x] A2 新增 CardSection、PillSelect、BottomSheet 三个移动端基础组件。
- [x] A3 扩展 LowcodeField/LowcodeForm，支持 pillSelect 字典选项、只读回显和紧凑布局。

## 阶段 B：Section 运行时

- [x] B1 新增 PageSectionRenderer，支持 card、inline_grid、card_list、bottom_sheet。
- [x] B2 透传主表/子表字段事件、表单引用、明细增删和子表行动作。
- [x] B3 增加固定底栏，复用受控 displayCondition 解析并支持 loading/disabled 状态。

## 阶段 C：页面动作闭环

- [x] C1 lowcode-runtime.vue 增加 pageSections 分支，旧应用继续走原渲染路径。
- [x] C2 主表单引用改为多 section 管理，保存时校验所有可见主表单和子表单。
- [x] C3 支持 save/reset/action/cancel；新建态 action 先保存并取得主键，再执行业务动作。
- [x] C4 动态 CRUD 新建接口返回已创建记录，主子表/关联表模式同步回填主键，并保持现有忽略响应数据的调用方兼容。

## 阶段 D：预售配置迁移

- [x] D1 新增 V1.0.109 Flyway 脚本，为预售表单写入 pageSections 和 bottomBar。
- [x] D2 同步 ai_crud_config、ai_crud_config_version、ai_business_object 和对象设计发布快照。
- [x] D3 扩展预售迁移合同测试，约束分区、pill、底栏动作和快照同步。

## 阶段 E：验证

- [x] E1 运行 H5 纯函数测试与生产构建。
- [x] E2 运行 DynamicCrudController 与预售迁移合同定向测试。
- [x] E3 执行 Flyway 占位符、租户、危险配置和 git diff 静态检查。
- [x] E4 回填 execution-log.md 与 Spec 状态，记录 mock 浏览器验证及跳过的真实数据库/真机 E2E。

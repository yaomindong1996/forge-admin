# 低代码子表行动作与父子可信上下文任务

## 1. SDD 与协议

- [x] 盘点 ChildTableEditor、主子表运行配置、BusinessAction、发布快照和动态 CRUD 父子读取能力。
- [x] 固定 CHILD_ROW、relationKey、父子最小请求和可信上下文语义。
- [x] 建立 spec.md、tasks.md、test-spec.md、execution-log.md。

## 2. 后端执行与发布

- [x] DTO 和执行上下文增加 parentRecordId、childRecordId、relationKey、parentRecordData。
- [x] 发布态从 relationSnapshot 校验 CHILD_ROW 动作关系。
- [x] 按父详情重新读取父子记录并验证子行归属。
- [x] 扩展 parentRecord/parent 路径、系统父子字段和幂等摘要。
- [x] 动作位置、发布检查和运行态投影支持 CHILD_ROW。

## 3. 设计器与前端运行时

- [x] 自动化动作支持选择“子表行按钮”和目标明细关系。
- [x] masterDetailConfig.children 投影 relationKey 和 rowActions。
- [x] ChildTableEditor 渲染通用行按钮并禁用未保存行。
- [x] AiCrudPage 复用 COMMAND 输入、确认、权限、幂等与最小载荷。

## 4. 验证与收尾

- [x] 后端执行/发布/运行配置定向测试通过。
- [x] 前端运行时/设计器 Vitest 和目标 ESLint 通过。
- [x] generator 聚合编译、前端生产构建和 git diff 检查通过。
- [x] 回填执行日志、决策、踩坑并将 Spec 标记 completed。

# 全表检索查询收敛 - Tasks
> status: apply

## T1 用户列表 SQL
- [x] `SysUserMapper.xml` 去掉非相关 GROUP_CONCAT derived table
- [x] 补 Mapper XML 契约测试

## T2 候选任务分页
- [x] `FlowTaskMapper` 新增 `selectCandidateTasks`
- [x] `FlowTaskServiceImpl.candidateTasks` 不再 Flowable `.list()`

## T3 超时扫描
- [x] `FlowTimeoutServiceImpl` 用 `listPage` 分批

## T4 索引
- [x] Flyway 为 user_org / user_post / flow_task 补查询索引

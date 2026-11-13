-- 低代码发起的流程，待办 process_def_key 被写成了流程定义 UUID，列表无法按模型编码关联出流程名称。
-- 影响范围：仅把仍指向 ACT_RE_PROCDEF 且与 KEY_ 不一致的待办编码改回模型编码。
-- 回滚方式：无自动回滚；如需恢复，可按 process_def_id 把 process_def_key 改回对应 ACT_RE_PROCDEF.ID_。

SET @repair_flow_task_process_def_key = IF(
    (SELECT COUNT(*)
     FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name IN ('sys_flow_task', 'ACT_RE_PROCDEF')) = 2,
    'UPDATE sys_flow_task t JOIN ACT_RE_PROCDEF pd ON pd.ID_ = t.process_def_id SET t.process_def_key = pd.KEY_ WHERE pd.KEY_ IS NOT NULL AND pd.KEY_ <> '''' AND t.process_def_key <> pd.KEY_',
    'SELECT 1'
);
PREPARE repair_flow_task_process_def_key_stmt FROM @repair_flow_task_process_def_key;
EXECUTE repair_flow_task_process_def_key_stmt;
DEALLOCATE PREPARE repair_flow_task_process_def_key_stmt;

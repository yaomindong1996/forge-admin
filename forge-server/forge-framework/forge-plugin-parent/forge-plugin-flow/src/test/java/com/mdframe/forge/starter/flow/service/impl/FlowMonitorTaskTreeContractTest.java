package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 监控实例详情任务树的查询边界合同。
 *
 * <p>任务树是管理员视图，必须复用租户限定的本地任务快照，并且有稳定的返回上限，
 * 避免把超长流程历史一次性加载到内存。</p>
 */
class FlowMonitorTaskTreeContractTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java");
    private static final Path MAPPER = Path.of("src/main/resources/mapper/FlowTaskMapper.xml");

    @Test
    void adminTaskDetailsMustBuildTreeAndExposeCurrentTaskIds() throws IOException {
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("selectAdminTaskTreeByProcessInstance"));
        assertTrue(source.contains("result.setTaskTree(buildAdminTaskTree(treeTasks))"));
        assertTrue(source.contains("result.setCurrentTaskIds(treeTasks.stream()"));
        assertTrue(source.contains("FlowTaskStatus.isActionable(task.getStatus())"));
        assertTrue(source.contains("selectAdminTaskTreeByProcessInstance(business.getProcessInstanceId(), tenantId, 500)"));
        assertTrue(source.contains("setTaskTreeTruncated"));
    }

    @Test
    void adminTaskTreeQueryMustBeTenantScopedAndBounded() throws IOException {
        String xml = Files.readString(MAPPER);
        int start = xml.indexOf("<select id=\"selectAdminTaskTreeByProcessInstance\"");
        int end = xml.indexOf("</select>", start);
        assertTrue(start >= 0 && end > start);
        String statement = xml.substring(start, end);
        assertTrue(statement.contains("t.process_instance_id = #{processInstanceId}"));
        assertTrue(statement.contains("t.tenant_id = #{tenantId}"));
        assertTrue(statement.contains("ORDER BY t.create_time ASC, t.id ASC"));
        assertTrue(statement.contains("LIMIT #{limit}"));
    }
}

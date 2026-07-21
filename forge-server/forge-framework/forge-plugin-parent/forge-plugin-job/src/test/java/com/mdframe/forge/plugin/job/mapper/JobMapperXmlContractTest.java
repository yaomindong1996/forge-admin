package com.mdframe.forge.plugin.job.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobMapperXmlContractTest {

    @Test
    void shouldDefineJobConfigQueriesInMapperXml() throws IOException {
        String xml = readMainResource("mapper/SysJobConfigMapper.xml");

        assertTrue(xml.contains("<select id=\"selectJobPage\""));
        assertTrue(xml.contains("com.mdframe.forge.plugin.job.vo.JobConfigVO"));
        assertTrue(xml.contains("job.del_flag = 0"));
        assertTrue(xml.contains("query.syncStatus"));
        assertTrue(xml.contains("<select id=\"selectJobDetail\""));
        assertTrue(xml.contains("<select id=\"selectRecoveryCandidates\""));
        assertTrue(xml.contains("job.status &lt;&gt; 2"));
        assertTrue(xml.contains("<select id=\"selectByJobKey\""));
        assertTrue(xml.contains("<update id=\"markOnceCompleted\""));
        assertTrue(xml.contains("<update id=\"markOnceMissedCompleted\""));
        assertTrue(xml.contains("<update id=\"logicalDeleteByVersion\""));
        assertTrue(xml.contains("AND version = #{expectedVersion}"));
        assertTrue(xml.contains("<update id=\"applyExecutionOutcome\""));
        assertTrue(xml.contains("last_completion_time"));
        assertTrue(xml.contains("execution.end_time &gt; job.last_completion_time"));
        assertTrue(xml.contains("schedule_type = 'ONCE'"));
        assertTrue(xml.contains("status = 2"));
    }

    @Test
    void shouldDefineJobLogFiltersInMapperXml() throws IOException {
        String xml = readMainResource("mapper/SysJobLogMapper.xml");

        assertTrue(xml.contains("<select id=\"selectLogPage\""));
        assertTrue(xml.contains("com.mdframe.forge.plugin.job.vo.JobLogVO"));
        assertTrue(xml.contains("job_log.del_flag = 0"));
        assertTrue(xml.contains("query.jobGroup"));
        assertTrue(xml.contains("query.status"));
        assertTrue(xml.contains("query.triggerType"));
        assertTrue(xml.contains("query.startTime"));
        assertTrue(xml.contains("query.endTime"));
        assertTrue(xml.contains("job_log.trigger_time &gt;= #{query.startTime}"));
        assertTrue(xml.contains("job_log.trigger_time &lt;= #{query.endTime}"));
        assertTrue(xml.contains("<select id=\"selectLogDetail\""));
        assertTrue(xml.contains("com.mdframe.forge.plugin.job.vo.JobLogDetailVO"));
        assertTrue(xml.contains("<select id=\"selectExportList\""));
        assertTrue(xml.contains("com.mdframe.forge.plugin.job.vo.JobLogExportVO"));
        assertTrue(xml.contains("<select id=\"selectMonitorSummary\""));
        assertTrue(xml.contains("AS accepted_count"));
        assertTrue(xml.contains("job_log.status = 4"));
        assertTrue(xml.contains("<select id=\"selectRecentExecutions\""));
        assertTrue(xml.contains("<update id=\"completeRunningExecution\""));
        assertTrue(xml.contains("<select id=\"selectFailureAlarmContext\""));
        assertTrue(xml.contains("WHERE id = #{id}"));
        assertTrue(xml.contains("AND status = 2"));
        assertTrue(xml.contains("status IN (0, 1, 3)"));
        assertTrue(xml.contains("FROM sys_job_api_idempotency idempotency"));
        assertTrue(xml.contains("idempotency.expires_at &gt; NOW()"));
        assertTrue(xml.contains("<update id=\"refreshHeartbeat\""));
        assertTrue(xml.contains("<update id=\"failStaleExecutions\""));
        assertTrue(xml.contains("COALESCE(heartbeat_time, start_time, trigger_time) &lt; #{cutoff}"));
    }

    @Test
    void shouldKeepSensitiveLogColumnsOutOfListAndExportQueries() throws IOException {
        String xml = readMainResource("mapper/SysJobLogMapper.xml");
        String safeColumns = sqlBlock(xml, "SafeLogColumns");
        String listQuery = selectBlock(xml, "selectLogPage");
        String exportQuery = selectBlock(xml, "selectExportList");
        String alarmQuery = selectBlock(xml, "selectFailureAlarmContext");

        assertFalse(safeColumns.contains("job_param"));
        assertFalse(safeColumns.contains("job_log.result"));
        assertFalse(safeColumns.contains("job_log.exception_msg"));
        assertFalse(listQuery.contains("job_param"));
        assertFalse(listQuery.contains("job_log.result"));
        assertFalse(listQuery.contains("job_log.exception_msg"));
        assertFalse(exportQuery.contains("job_param"));
        assertFalse(exportQuery.contains("job_log.result"));
        assertFalse(exportQuery.contains("job_log.exception_msg"));
        assertFalse(alarmQuery.contains("job_param"));
        assertFalse(alarmQuery.contains("job_log.result"));
        assertTrue(alarmQuery.contains("job_log.exception_msg AS exception_summary"));
    }

    @Test
    void shouldNotBuildPageQueriesInServices() throws IOException {
        String configService = readMainJava("service/impl/SysJobConfigServiceImpl.java");
        String logService = readMainJava("service/impl/SysJobLogServiceImpl.java");

        assertFalse(configService.contains("LambdaQueryWrapper"));
        assertFalse(logService.contains("LambdaQueryWrapper"));
    }

    private String readMainResource(String relativePath) throws IOException {
        return Files.readString(resolveModulePath("src/main/resources/" + relativePath));
    }

    private String readMainJava(String relativePath) throws IOException {
        return Files.readString(resolveModulePath(
                "src/main/java/com/mdframe/forge/plugin/job/" + relativePath));
    }

    private String selectBlock(String xml, String id) {
        int start = xml.indexOf("<select id=\"" + id + "\"");
        int end = xml.indexOf("</select>", start);
        return xml.substring(start, end);
    }

    private String sqlBlock(String xml, String id) {
        int start = xml.indexOf("<sql id=\"" + id + "\"");
        int end = xml.indexOf("</sql>", start);
        return xml.substring(start, end);
    }

    private Path resolveModulePath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path direct = current.resolve(relativePath);
            if (Files.exists(direct)) {
                return direct;
            }
            Path nested = current.resolve(
                    "forge-server/forge-framework/forge-plugin-parent/forge-plugin-job")
                    .resolve(relativePath);
            if (Files.exists(nested)) {
                return nested;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }
}

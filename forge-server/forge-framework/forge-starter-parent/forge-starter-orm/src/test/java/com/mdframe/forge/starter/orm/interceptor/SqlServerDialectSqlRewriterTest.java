package com.mdframe.forge.starter.orm.interceptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerDialectSqlRewriterTest {

    @Test
    void rewritesForUpdateWithAlias() {
        String sql = "SELECT * FROM sys_user u WHERE u.id = ? FOR UPDATE";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).contains("FROM sys_user u WITH (UPDLOCK, ROWLOCK)");
        assertThat(out).doesNotContain("FOR UPDATE");
    }

    @Test
    void rewritesForUpdateWithoutAlias() {
        String sql = "SELECT id FROM biz_order WHERE tenant_id = ? AND id = ? FOR UPDATE";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).contains("FROM biz_order WITH (UPDLOCK, ROWLOCK)");
        assertThat(out).doesNotContain("FOR UPDATE");
    }

    @Test
    void rewritesBracketQuotedTable() {
        String sql = "SELECT * FROM [sys_flow_task] WHERE id = ? FOR UPDATE";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).contains("FROM [sys_flow_task] WITH (UPDLOCK, ROWLOCK)");
    }

    @Test
    void rewritesLimitOnlyAsTop() {
        String sql = "SELECT id FROM t WHERE a = 1 LIMIT ?";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).startsWith("SELECT TOP (?) id FROM t WHERE a = 1");
        assertThat(out).doesNotContain("LIMIT");
        assertThat(out).doesNotContain("OFFSET");
    }

    @Test
    void rewritesLimitOffsetKeywordKeepingOrderByAndFlagsParamSwap() {
        String sql = "SELECT id FROM t ORDER BY id DESC LIMIT ? OFFSET ?";
        var result = SqlServerDialectSqlRewriter.rewriteDetailed(sql);
        assertThat(result.sql()).contains("ORDER BY id DESC");
        assertThat(result.sql()).endsWith("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        assertThat(result.swapLastTwoLimitOffsetParams()).isTrue();
    }

    @Test
    void rewritesMysqlCommaLimit() {
        String sql = "SELECT id FROM t ORDER BY id LIMIT ?, ?";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).endsWith("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
    }

    @Test
    void leavesPlainSelectUntouched() {
        String sql = "SELECT id FROM t WHERE a = 1 ORDER BY id";
        assertThat(SqlServerDialectSqlRewriter.rewrite(sql)).isEqualTo(sql);
    }

    @Test
    void rewritesLimitInsideSubqueriesAndOuterAsTop() {
        String sql = "SELECT job.id, "
                + "(SELECT log.status FROM sys_job_log log WHERE log.job_config_id = job.id "
                + "ORDER BY log.start_time DESC, log.id DESC LIMIT 1) AS last_execution_status, "
                + "(SELECT log.start_time FROM sys_job_log log WHERE log.job_config_id = job.id "
                + "ORDER BY log.start_time DESC, log.id DESC LIMIT 1) AS last_execution_time "
                + "FROM sys_job_config job WHERE job.job_name = ? AND job.del_flag = 0 LIMIT 1";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).doesNotContain("LIMIT");
        assertThat(out).doesNotContain("OFFSET");
        assertThat(out).contains("SELECT TOP (1) job.id");
        assertThat(out).contains("(SELECT TOP (1) log.status FROM sys_job_log log");
        assertThat(out).contains("(SELECT TOP (1) log.start_time FROM sys_job_log log");
    }
}

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
    void rewritesLimitOnlyAndInjectsOrderBy() {
        String sql = "SELECT id FROM t WHERE a = 1 LIMIT ?";
        String out = SqlServerDialectSqlRewriter.rewrite(sql);
        assertThat(out).contains("ORDER BY (SELECT NULL)");
        assertThat(out).endsWith("OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY");
        assertThat(out).doesNotContain("LIMIT");
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
}

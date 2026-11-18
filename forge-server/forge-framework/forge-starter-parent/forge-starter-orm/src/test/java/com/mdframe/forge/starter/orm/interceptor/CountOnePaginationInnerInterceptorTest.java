package com.mdframe.forge.starter.orm.interceptor;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CountOnePaginationInnerInterceptorTest {

    @Test
    void shouldUseParserCompatibleCountExpressionForNestedFlowQuery() throws Exception {
        String sql = """
                SELECT t.id, t.title
                FROM sys_flow_task t
                WHERE t.tenant_id = ?
                  AND (t.assignee = ?
                    OR (t.candidate_groups IS NOT NULL AND t.candidate_groups != ''
                      AND (EXISTS (
                        SELECT 1
                        FROM sys_user_org_role current_user_role
                        INNER JOIN sys_role candidate_role
                                ON candidate_role.id = current_user_role.role_id
                               AND candidate_role.tenant_id = current_user_role.tenant_id
                        LEFT JOIN sys_role_org candidate_role_org
                               ON candidate_role_org.role_id = current_user_role.role_id
                        WHERE current_user_role.tenant_id = t.tenant_id
                          AND current_user_role.user_id = CAST(NULLIF(?, '') AS UNSIGNED)
                          AND (candidate_role.org_scope_type = 1 OR candidate_role_org.id IS NOT NULL)
                          AND (FIND_IN_SET(
                                  CAST(current_user_role.role_id AS varchar(64)),
                                  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(t.candidate_groups, ';', ','), '；', ','), '，', ','), '、', ','), ' ', '')
                              ) > 0
                              OR FIND_IN_SET(
                                  candidate_role.role_key,
                                  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(t.candidate_groups, ';', ','), '；', ','), '，', ','), '、', ','), ' ', '')
                              ) > 0)
                      ) OR EXISTS (
                        SELECT 1
                        FROM sys_user_org current_user_org
                        WHERE current_user_org.tenant_id = t.tenant_id
                          AND current_user_org.user_id = CAST(NULLIF(?, '') AS UNSIGNED)
                          AND FIND_IN_SET(
                              CAST(current_user_org.org_id AS varchar(64)),
                              REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(t.candidate_groups, ';', ','), '；', ','), '，', ','), '、', ','), ' ', '')
                          ) > 0
                      )))
                  )
                ORDER BY t.create_time DESC
                """;

        CountOnePaginationInnerInterceptor interceptor = new CountOnePaginationInnerInterceptor();
        String countSql = interceptor.autoCountSql(new Page<>(1, 10), sql);

        assertTrue(countSql.regionMatches(true, 0, "SELECT COUNT(1)", 0, "SELECT COUNT(1)".length()));
        CCJSqlParserUtil.parse(countSql);
    }
}

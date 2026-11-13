package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysUserMapperSqlContractTest {

    @Test
    void userListDisplayNamesMustUsePortableDetailQueries() throws IOException {
        String xml = Files.readString(Path.of("src/main/resources/mapper/SysUserMapper.xml"));
        String page = statement(xml, "selectUserPage");
        String export = statement(xml, "selectExportList");

        assertFalse(page.contains("GROUP_CONCAT"), "selectUserPage must not use MySQL GROUP_CONCAT");
        assertFalse(export.contains("GROUP_CONCAT"), "selectExportList must not use MySQL GROUP_CONCAT");
        assertFalse(xml.contains("GROUP_CONCAT"), "SysUserMapper must not use GROUP_CONCAT");

        assertTrue(xml.contains("selectTenantNameLabels"));
        assertTrue(xml.contains("selectOrgNameLabels"));
        assertTrue(xml.contains("selectPostNameLabels"));
        assertFalse(page.contains("GROUP BY sut.user_id"), "selectUserPage must not aggregate the whole user-tenant table");
        assertFalse(page.contains("GROUP BY uo.user_id"), "selectUserPage must not aggregate the whole user-org table");
        assertFalse(page.contains("GROUP BY up.user_id"), "selectUserPage must not aggregate the whole user-post table");
    }

    @Test
    void userListMustSortAdminUserTypesFirst() throws IOException {
        String xml = Files.readString(Path.of("src/main/resources/mapper/SysUserMapper.xml"));
        String page = statement(xml, "selectUserPage");
        String export = statement(xml, "selectExportList");

        // 非租户视图：用户类型升序优先（0 系统管理员、1 租户管理员在前），再按创建时间倒序
        for (String query : new String[] { page, export }) {
            assertTrue(query.contains("ORDER BY u.user_type, u.create_time DESC"),
                    "user list must sort by user_type (admin types first) before create_time");
            // 租户视图：排序口径必须与 user_type 显示列一致（租户管理员来自 sut_current.member_type）
            assertTrue(query.contains(
                    "ORDER BY CASE WHEN u.user_type = 0 THEN 0 WHEN sut_current.member_type = 1 THEN 1 ELSE 2 END"),
                    "tenant view must sort with the same user_type expression as displayed");
        }
    }

    private String statement(String xml, String id) {
        String startToken = "<select id=\"" + id + "\"";
        int start = xml.indexOf(startToken);
        assertTrue(start >= 0, () -> "Missing mapper statement: " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, () -> "Unclosed mapper statement: " + id);
        return xml.substring(start, end);
    }
}

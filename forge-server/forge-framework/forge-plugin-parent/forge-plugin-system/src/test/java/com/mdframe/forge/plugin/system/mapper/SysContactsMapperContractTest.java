package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SysContactsMapperContractTest {

    private static final Path MAPPER = Path.of("src/main/resources/mapper/SysContactsMapper.xml");

    @Test
    void activeMemberFilterRequiresCurrentTenantMembershipAndEnabledUser() throws IOException {
        String mapper = Files.readString(MAPPER);

        assertThat(statement(mapper, "activeMember", "sql"))
                .contains("u.del_flag = 0", "u.user_status = 1",
                        "sut.tenant_id = #{tenantId}", "sut.status = 1");
    }

    @Test
    void everyMemberQueryAppliesActiveMemberFilter() throws IOException {
        String mapper = Files.readString(MAPPER);

        for (String id : new String[]{"countMembers", "selectMemberPage", "selectMember", "selectChildOrgs"}) {
            assertThat(statement(mapper, id, "select")).as(id).contains("refid=\"activeMember\"");
        }
        assertThat(statement(mapper, "selectMember", "select")).contains("u.id = #{userId}");
    }

    @Test
    void orgAndLabelQueriesStayInsideCurrentTenant() throws IOException {
        String mapper = Files.readString(MAPPER);

        for (String id : new String[]{"selectMainOrg", "selectChildOrgs", "selectOrgNames"}) {
            assertThat(statement(mapper, id, "select")).as(id)
                    .contains("o.tenant_id = #{tenantId}", "o.del_flag = 0", "o.org_status = 1");
        }
        assertThat(statement(mapper, "selectPostNames", "select"))
                .contains("p.tenant_id = #{tenantId}", "p.del_flag = 0", "p.post_status = 1");
    }

    @Test
    void contactsMapperNeverUsesStringSubstitution() throws IOException {
        assertThat(Files.readString(MAPPER)).doesNotContain("${");
    }

    private String statement(String mapper, String id, String element) {
        int start = mapper.indexOf("id=\"" + id + "\"");
        int end = mapper.indexOf("</" + element + ">", start);
        assertThat(start).as(id).isGreaterThanOrEqualTo(0);
        assertThat(end).as(id).isGreaterThan(start);
        return mapper.substring(start, end);
    }
}

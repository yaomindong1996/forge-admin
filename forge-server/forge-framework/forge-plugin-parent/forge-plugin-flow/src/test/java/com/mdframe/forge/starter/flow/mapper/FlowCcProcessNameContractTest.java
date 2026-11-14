package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowCcProcessNameContractTest {

    @Test
    void ccListsMustResolveProcessNameFromTenantModel() throws IOException {
        String xml = Files.readString(Path.of("src/main/resources/mapper/FlowCcMapper.xml"));
        int start = xml.indexOf("<sql id=\"CcProcessNameColumn\">");
        String fragment = xml.substring(start, xml.indexOf("</sql>", start));
        assertTrue(fragment.contains("m.tenant_id = c.tenant_id"), "process name lookup must stay tenant scoped");
        assertTrue(fragment.contains("m.model_key = c.process_def_key"));
        assertTrue(fragment.contains("m.del_flag = 0"));
        assertTrue(fragment.contains("LIMIT 1"), "scalar lookup must never fan out cc rows");
        for (String id : new String[] {"selectMyPage", "selectSentPage"}) {
            int s = xml.indexOf("<select id=\"" + id + "\"");
            String statement = xml.substring(s, xml.indexOf("</select>", s));
            assertTrue(statement.contains("<include refid=\"CcProcessNameColumn\" />"), id + " must expose process_name");
        }
    }
}

package com.mdframe.forge.plugin.capability.flowaction.source;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlowActionSourceMapperContractTest {

    @Test
    void shouldAnchorSourceVersionToExactPublishedDesignVersionFact() throws Exception {
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("mapper/FlowActionSourceMapper.xml")) {
            assertThat(stream).isNotNull();
            String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(xml)
                    .contains("INNER JOIN ai_business_object_design_version v")
                    .contains("b.target_id = o.id")
                    .contains("v.object_id = o.id")
                    .contains("v.suite_code = o.suite_code")
                    .contains("v.object_code = o.object_code")
                    .contains("v.publish_version = o.last_publish_version")
                    .contains("v.publish_status = 'PUBLISHED'")
                    .contains("v.publish_version AS published_object_version");
        }
    }
}

package com.mdframe.forge.plugin.collaboration.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SocialCallbackEventMapperXmlContractTest {

    @Test
    void claimUsesTwoPhaseSelectThenUpdateByIds() throws Exception {
        Path xml = Path.of("src/main/resources/mapper/collaboration/SocialCallbackEventMapper.xml");
        String content = Files.readString(xml, StandardCharsets.UTF_8);
        assertThat(content).contains("selectClaimCandidateIds");
        assertThat(content).contains("claimEventsByIds");
        assertThat(content).doesNotContain("id=\"claimPendingEvents\"");

        int updateIdx = content.indexOf("<update id=\"claimEventsByIds\">");
        assertThat(updateIdx).isPositive();
        String updateBlock = content.substring(updateIdx, content.indexOf("</update>", updateIdx));
        assertThat(updateBlock).doesNotContain("ORDER BY");
        assertThat(updateBlock).doesNotContain("LIMIT");
        assertThat(updateBlock).contains("id IN");
    }
}

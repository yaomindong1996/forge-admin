package com.mdframe.forge.plugin.ai.multimodal.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.multimodal.image.dto.AiImageGenerateDTO;
import com.mdframe.forge.plugin.ai.multimodal.image.enums.AiImageGenerateStatus;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiImageContractTest {
    @Test
    void should_preserveGenerationInput_but_excludeInjectedEntityFieldsFromWireBinding() throws Exception {
        var request = new ObjectMapper().readValue("""
                {"modelId":89,"prompt":"fixture","size":"1024x1024","userId":99,"tenantId":99,
                 "id":999,"providerId":999,"status":"success","resultFileId":999,"delFlag":999}
                """, AiImageGenerateDTO.class).toRecord();
        assertThat(request.getModelId()).isEqualTo(89L);
        assertThat(request.getPrompt()).isEqualTo("fixture");
        assertThat(request.getId()).isNull();
        assertThat(request.getTenantId()).isNull();
        assertThat(request.getUserId()).isNull();
        assertThat(request.getProviderId()).isNull();
        assertThat(request.getStatus()).isNull();
        assertThat(request.getResultFileId()).isNull();
        assertThat(request.getDelFlag()).isNull();
    }

    @Test
    void should_parseActualMapper_and_matchIdentityExpectedStateAndActiveRecordInOneUpdate() throws Exception {
        var configuration = new Configuration();
        String resource = "mapper/AiImageGenerateRecordMapper.xml";
        try (var stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).isNotNull();
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var request = new AiImageGenerateDTO();
        request.setModelId(89L);
        request.setPrompt("fixture");
        var statement = configuration.getMappedStatement(
                "com.mdframe.forge.plugin.ai.multimodal.image.mapper.AiImageGenerateRecordMapper.transition");
        var bound = statement.getBoundSql(Map.of("record", request.toRecord(), "expectedStatus", "pending"));
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains("WHERE id = ? AND tenant_id = ? AND user_id = ?", "AND del_flag = 0 AND status = ?");
        assertThat(bound.getParameterMappings()).extracting(mapping -> mapping.getProperty())
                .contains("record.tenantId", "record.userId", "expectedStatus");
        assertThat(sql).doesNotContain("${");
    }

    @Test
    void should_allowOnlyForwardStateTransitions_without_resettingTerminalResults() {
        assertThat(AiImageGenerateStatus.PENDING.canTransitionTo(AiImageGenerateStatus.GENERATING)).isTrue();
        assertThat(AiImageGenerateStatus.PENDING.canTransitionTo(AiImageGenerateStatus.FAILED)).isTrue();
        assertThat(AiImageGenerateStatus.PENDING.canTransitionTo(AiImageGenerateStatus.SUCCESS)).isFalse();
        for (var status : AiImageGenerateStatus.values()) {
            assertThat(AiImageGenerateStatus.SUCCESS.canTransitionTo(status)).isFalse();
            assertThat(AiImageGenerateStatus.FAILED.canTransitionTo(status)).isFalse();
        }
    }
}

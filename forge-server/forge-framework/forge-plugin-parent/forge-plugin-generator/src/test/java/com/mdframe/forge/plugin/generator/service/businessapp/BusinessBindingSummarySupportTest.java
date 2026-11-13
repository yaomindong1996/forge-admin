package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessBindingSummarySupportTest {

    @Test
    void retainAndEnrichUsesJavaJsonNotSqlDialect() {
        BusinessBindingSummaryVO byKey = new BusinessBindingSummaryVO();
        byKey.setBindingKey("leave");
        byKey.setObjectCode("LEAVE");
        byKey.setOptionsJson("{\"codeApp\":true}");

        BusinessBindingSummaryVO byConfig = new BusinessBindingSummaryVO();
        byConfig.setBindingConfigJson("{\"flowModelKey\":\"leave\"}");
        byConfig.setObjectCode("LEAVE2");
        byConfig.setDesignerOptionsJson("{\"codeApp\":\"true\"}");

        BusinessBindingSummaryVO noise = new BusinessBindingSummaryVO();
        noise.setBindingConfigJson("{\"flowModelKey\":\"other\"}");
        noise.setObjectCode("X");

        List<BusinessBindingSummaryVO> matched = BusinessBindingSummarySupport.retainMatchingModelKey(
                List.of(byKey, byConfig, noise), "leave");
        assertThat(matched).hasSize(2);
        BusinessBindingSummarySupport.enrichDisplay(matched);
        assertThat(matched.get(0).getCodeApp()).isTrue();
        assertThat(matched.get(0).getEntryRoute()).contains("codeApp=1");
        assertThat(matched.get(0).getOptionsJson()).isNull();
        assertThat(matched.get(1).getCodeApp()).isTrue();
        assertThat(matched.get(1).getEntryRoute()).endsWith("&codeApp=1");
    }
}

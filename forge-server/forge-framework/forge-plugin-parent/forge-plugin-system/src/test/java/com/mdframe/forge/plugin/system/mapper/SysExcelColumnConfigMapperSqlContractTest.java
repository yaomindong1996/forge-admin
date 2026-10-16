package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防止 Excel 图片列字段从 Mapper XML 再次漏映射。
 */
class SysExcelColumnConfigMapperSqlContractTest {

    @Test
    void selectByConfigKeyIncludesImageColumnFields() throws Exception {
        String xml;
        try (InputStream input = SysExcelColumnConfigMapperSqlContractTest.class.getClassLoader()
                .getResourceAsStream("mapper/SysExcelColumnConfigMapper.xml")) {
            assertThat(input).as("SysExcelColumnConfigMapper.xml must be on test classpath").isNotNull();
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(xml).contains("column_type");
        assertThat(xml).contains("image_width");
        assertThat(xml).contains("image_height");
        assertThat(xml).contains("image_max_count");
        assertThat(xml).contains("property=\"columnType\"");
        assertThat(xml).contains("property=\"imageWidth\"");
        assertThat(xml).contains("property=\"imageHeight\"");
        assertThat(xml).contains("property=\"imageMaxCount\"");
    }
}

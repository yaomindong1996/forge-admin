package com.mdframe.forge.plugin.system.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SysContentInstalledAssetMapperContractTest {

    @Test
    void queriesMustFilterTenantKindAndLogicDelete() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/SysContentInstalledAssetMapper.xml"));

        assertThat(statement(mapper, "KindKeywordFilter", "sql"))
                .contains("tenant_id = #{tenantId}", "kind = #{kind}", "del_flag = 0")
                .doesNotContain("${");
        assertThat(statement(mapper, "countByKind", "select"))
                .contains("KindKeywordFilter")
                .doesNotContain("${");
        assertThat(statement(mapper, "selectPageByKind", "select"))
                .contains("KindKeywordFilter", "LIMIT #{offset}, #{limit}", "AssetSummaryColumns")
                .doesNotContain("${");
        assertThat(statement(mapper, "AssetSummaryColumns", "sql"))
                .doesNotContain("package_json");
        assertThat(statement(mapper, "selectByKindAndKey", "select"))
                .contains("tenant_id = #{tenantId}", "kind = #{kind}", "asset_key = #{assetKey}", "del_flag = 0")
                .doesNotContain("${");
        assertThat(statement(mapper, "selectActiveById", "select"))
                .contains("tenant_id = #{tenantId}", "id = #{id}", "del_flag = 0")
                .doesNotContain("${");
    }

    private String statement(String mapper, String id, String element) {
        int start = mapper.indexOf("id=\"" + id + "\"");
        int end = mapper.indexOf("</" + element + ">", start);
        assertThat(start).as(id).isGreaterThanOrEqualTo(0);
        assertThat(end).as(id).isGreaterThan(start);
        return mapper.substring(start, end);
    }
}

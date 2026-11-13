package com.mdframe.forge.plugin.generator.mapper;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessFlowModelBindingsMapperTest {
    @Test
    void batchQueryUsesRequestedKeysAndPreservesTenantDeletionAndStatusBoundaries() throws Exception {
        Configuration configuration = new Configuration();
        try (var input = getClass().getClassLoader().getResourceAsStream("mapper/BusinessBindingMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/BusinessBindingMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement(
                BusinessBindingMapper.class.getName() + ".selectFlowBindingsByModelKeys");
        var bound = statement.getBoundSql(Map.of(
                "tenantId", 1L,
                "modelKeys", List.of("A", "B"),
                "modelKeyLikeNeedles", List.of("%\"flowModelKey\"%A%", "%\"flowModelKey\"%B%")));
        String sql = bound.getSql();
        assertThat(CCJSqlParserUtil.parse(sql)).isNotNull();
        assertThat(sql).contains("b.tenant_id = ?",
                "o.tenant_id = b.tenant_id", "o.del_flag = '0'", "b.status = 1", "b.target_type = 'OBJECT'",
                "a.del_flag = 0", "ao.del_flag = 0", "b.binding_key IN", "b.binding_config LIKE");
        assertThat(sql).doesNotContain("JSON_VALID", "JSON_EXTRACT", "regexp_match", "_databaseId");
        assertThat(statement.getBoundSql(Map.of(
                "tenantId", 1L,
                "modelKeys", List.of(),
                "modelKeyLikeNeedles", List.of())).getSql())
                .contains("1 = 0");
        String single = configuration.getMappedStatement(BusinessBindingMapper.class.getName()
                + ".selectFlowBindingsByModelKey").getBoundSql(Map.of(
                "tenantId", 1L,
                "modelKey", "A",
                "modelKeyLikeNeedle", "%\"flowModelKey\"%A%")).getSql();
        assertThat(CCJSqlParserUtil.parse(single)).isNotNull();
        assertThat(single).contains("b.tenant_id = ?", "b.binding_config LIKE");
        assertThat(single).doesNotContain("JSON_VALID", "JSON_EXTRACT", "regexp_match");
    }
}

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
        var bound = statement.getBoundSql(Map.of("tenantId", 1L, "modelKeys", List.of("A", "B")));
        String sql = bound.getSql();
        assertThat(CCJSqlParserUtil.parse(sql)).isNotNull();
        assertThat(sql).contains("requested.model_key AS flowModelKey", "UNION ALL", "b.tenant_id = ?",
                "o.tenant_id = b.tenant_id", "o.del_flag = '0'", "b.status = 1", "b.target_type = 'OBJECT'",
                "a.del_flag = 0", "ao.del_flag = 0", "CASE WHEN JSON_VALID(b.binding_config)");
        assertThat(bound.getParameterMappings()).hasSize(4);
        assertThat(statement.getBoundSql(Map.of("tenantId", 1L, "modelKeys", List.of())).getSql())
                .contains("SELECT NULL AS model_key WHERE 1 = 0");
        String single = configuration.getMappedStatement(BusinessBindingMapper.class.getName()
                + ".selectFlowBindingsByModelKey").getBoundSql(Map.of("tenantId", 1L, "modelKey", "A")).getSql();
        assertThat(CCJSqlParserUtil.parse(single)).isNotNull();
        assertThat(single).contains("b.tenant_id = ?");
    }
}

package com.mdframe.forge.starter.flow.mapper;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowModelPublicationMapperTest {
    @Test
    void publicationQueryIsBoundedTenantScopedAndPrefersExactDeployment() throws Exception {
        Configuration configuration = new Configuration();
        try (var input = getClass().getClassLoader().getResourceAsStream("mapper/FlowModelVersionMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/FlowModelVersionMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement(
                FlowModelVersionMapper.class.getName() + ".selectPublicationSnapshots");
        String sql = statement.getBoundSql(Map.of("modelIds", List.of("A", "B"), "tenantId", 1L)).getSql();
        assertThat(CCJSqlParserUtil.parse(sql)).isNotNull();
        assertThat(sql).contains("candidate.tenant_id = ?", "v.tenant_id = ?", "m.tenant_id = ?",
                "candidate.del_flag = 0", "v.del_flag = 0", "m.del_flag = 0", "m.id IN",
                "candidate.deployment_id = m.deployment_id", "candidate.process_definition_id = m.process_definition_id",
                "candidate.version = m.version", "LIMIT 1");
        String empty = statement.getBoundSql(Map.of("modelIds", List.of(), "tenantId", 1L)).getSql();
        assertThat(empty).contains("AND 1 = 0");
    }
}

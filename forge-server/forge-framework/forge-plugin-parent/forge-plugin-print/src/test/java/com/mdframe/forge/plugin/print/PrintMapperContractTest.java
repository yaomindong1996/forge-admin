package com.mdframe.forge.plugin.print;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class PrintMapperContractTest {

    static Configuration configuration() throws Exception {
        var config = new Configuration();
        for (String name : List.of("PrintTemplate", "PrintTemplateVersion", "PrintBinding", "PrintExecution")) {
            String resource = "mapper/" + name + "Mapper.xml";
            try (var in = PrintMapperContractTest.class.getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
            }
        }
        return config;
    }

    @Test
    void parsesEveryMapperAndRequiresScopedParametersAndCas() throws Exception {
        var config = configuration();
        for (String id : config.getMappedStatementNames()) {
            if (!id.contains(".")) {
                continue;
            }
            var statement = config.getMappedStatement(id);
            var bound = statement.getBoundSql(Map.of());
            String sql = bound.getSql().replaceAll("\\s+", " ");
            assertThat(statement.getSqlCommandType()).isNotEqualTo(SqlCommandType.DELETE);
            assertThat(sql).contains("tenant_id").doesNotContain("${");
            assertThat(bound.getParameterMappings()).anySatisfy(parameter -> assertThat(parameter.getProperty()).matches("(?:row\\.)?tenantId"));
            if (statement.getSqlCommandType() != SqlCommandType.INSERT && !id.endsWith("selectMaxVersionNo")) {
                assertThat(sql).contains("del_flag = 0");
            }
            if (id.endsWith("updateDraft") || id.endsWith("publish") || id.endsWith("changeStatus") || id.endsWith("softDelete") || id.endsWith("updateBinding")) {
                assertThat(bound.getParameterMappings()).anySatisfy(parameter -> assertThat(parameter.getProperty()).isEqualTo("expectedRevision"));
            }
            if (id.contains("PrintTemplateVersionMapper")) {
                assertThat(statement.getSqlCommandType()).isIn(SqlCommandType.INSERT, SqlCommandType.SELECT);
            }
        }
        String lock = config.getMappedStatement("com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper.lockScoped").getBoundSql(Map.of()).getSql();
        assertThat(lock).contains("FOR UPDATE");
        String bindingLock = config.getMappedStatement("com.mdframe.forge.plugin.print.mapper.PrintBindingMapper.selectApplication").getBoundSql(Map.of()).getSql();
        assertThat(bindingLock).contains("FOR UPDATE");
        assertThat(bindingLock.toUpperCase(java.util.Locale.ROOT)).doesNotContain("ORDER BY");
        String event = config.getMappedStatement("com.mdframe.forge.plugin.print.mapper.PrintExecutionMapper.recordEvent").getBoundSql(Map.of()).getSql();
        assertThat(event).contains("actor = ?", "result = 'PREPARED'", "BETWEEN 1 AND 50");
    }
}

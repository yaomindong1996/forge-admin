package com.mdframe.forge.plugin.generator.mapper;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 发起审批目录引用版本 Mapper 的公共片段，无论 XML 加载先后都必须解析出完整 SQL。 */
class BusinessStartableProcessSqlTest {

    @Test
    void sharedPublishedProcessFragmentResolvesInEitherLoadOrder() throws Exception {
        String startable = "mapper/BusinessStartableObjectMapper.xml";
        String version = "mapper/BusinessProcessVersionMapper.xml";
        String process = "mapper/BusinessProcessMapper.xml";
        for (List<String> order : List.of(
                List.of(startable, version, process),
                List.of(process, version, startable),
                List.of(version, startable, process))) {
            Configuration configuration = new Configuration();
            for (String resource : order) {
                try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                    new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
                }
            }
            String sql = configuration.getMappedStatement(
                    BusinessStartableObjectMapper.class.getName() + ".selectStartableProcesses")
                    .getBoundSql(Map.of("tenantId", 1L)).getSql();

            assertThat(CCJSqlParserUtil.parse(sql)).isNotNull();
            assertThat(sql).contains("INNER JOIN ai_business_process_version v",
                    "v.version_no = p.published_version", "INNER JOIN ai_business_object o",
                    "INNER JOIN ai_business_application_object ao", "a.status = 1",
                    "o.del_flag = 0", "p.tenant_id = ?", "o.config_key IS NOT NULL");
            String byApplication = configuration.getMappedStatement(
                    BusinessProcessVersionMapper.class.getName() + ".selectCurrentPublishedByApplication")
                    .getBoundSql(Map.of("tenantId", 1L, "applicationId", 2L)).getSql();
            assertThat(byApplication).contains("INNER JOIN ai_business_application_object ao", "p.application_id = ?");
        }
    }
}

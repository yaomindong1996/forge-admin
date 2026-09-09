package com.mdframe.forge.starter.datascope.handler;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.context.DataScopeContext;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.enums.DataScopeType;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DataScopeFlowRelatedVisibilityTest {

    @Test
    void shouldOrFlowRelatedRecordsOnSelfScope() throws Exception {
        String where = rewriteSelfScope(1, "sample_purchase_order");
        assertTrue(where.contains("t.create_by = 9"));
        assertTrue(where.contains("sys_flow_record_participant"));
        assertTrue(where.contains("sample_purchase_order"));
        assertTrue(where.contains("t.id"));
        assertTrue(where.toUpperCase().contains("CAST"));
    }

    @Test
    void shouldSkipFlowRelatedWhenNotEnabled() throws Exception {
        String where = rewriteSelfScope(0, "sample_purchase_order");
        assertTrue(where.contains("t.create_by = 9"));
        assertFalse(where.contains("sys_flow_record_participant"));
    }

    @Test
    void shouldResolveBusinessTypeFromLowcodeResourceCode() throws Exception {
        SysDataScopeConfig config = baseConfig(1, null);
        config.setResourceCode("ai:business:ORDER");
        String where = rewrite(config);
        assertTrue(where.contains("ORDER"));
        assertTrue(where.contains("sys_flow_record_participant"));
    }

    private String rewriteSelfScope(int flowRelatedVisible, String businessType) throws Exception {
        SysDataScopeConfig config = baseConfig(flowRelatedVisible, businessType);
        return rewrite(config);
    }

    private SysDataScopeConfig baseConfig(int flowRelatedVisible, String businessType) {
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setTableAlias("t");
        config.setUserIdColumn("create_by");
        config.setFlowRelatedVisible(flowRelatedVisible);
        config.setFlowBusinessType(businessType);
        config.setRecordIdColumn("id");
        return config;
    }

    private String rewrite(SysDataScopeConfig config) throws Exception {
        DataScopeInterceptor interceptor = new DataScopeInterceptor(
                mock(com.mdframe.forge.starter.datascope.service.IDataScopeService.class),
                new DataScopeProperties());
        DataScopeContext context = new DataScopeContext();
        context.setUserId(9L);
        context.setTenantId(1L);
        Method method = DataScopeInterceptor.class.getDeclaredMethod("buildDataScopeSql",
                String.class, SysDataScopeConfig.class, DataScopeContext.class, DataScopeType.class);
        method.setAccessible(true);
        String modifiedSql = (String) method.invoke(interceptor, """
                SELECT t.id FROM biz_sample_purchase_order t WHERE t.del_flag = 0
                """, config, context, DataScopeType.SELF);
        return ((Select) CCJSqlParserUtil.parse(modifiedSql)).getPlainSelect().getWhere().toString();
    }
}

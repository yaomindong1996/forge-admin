package com.mdframe.forge.starter.datascope.handler;

import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataScopeFailClosedTest {

    @Test
    void shouldAllowExplicitlyDisabledRuleEvenInStrictPolicy() {
        IDataScopeService service = mock(IDataScopeService.class);
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setEnabled(0);
        when(service.getDataScopeConfig("com.example.UserMapper.selectPage")).thenReturn(config);
        DataScopeProperties properties = new DataScopeProperties();
        properties.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        QueryFixture fixture = queryFixture("com.example.UserMapper.selectPage");
        assertDoesNotThrow(() -> fixture.invoke(new DataScopeInterceptor(service, properties)));
    }

    @Test
    void shouldDenyUnconfiguredMapperInStrictPolicy() {
        IDataScopeService service = mock(IDataScopeService.class);
        DataScopeProperties properties = new DataScopeProperties();
        properties.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.DENY);
        DataScopeInterceptor interceptor = new DataScopeInterceptor(service, properties);
        QueryFixture fixture = queryFixture("com.example.UserMapper.selectPage");

        assertThrows(SQLException.class, () -> fixture.invoke(interceptor));
    }

    @Test
    void shouldWarnAndAllowUnconfiguredMapperInCompatibilityPolicy() {
        IDataScopeService service = mock(IDataScopeService.class);
        DataScopeProperties properties = new DataScopeProperties();
        properties.setUnconfiguredPolicy(DataScopeProperties.UnconfiguredPolicy.WARN);
        DataScopeInterceptor interceptor = new DataScopeInterceptor(service, properties);
        QueryFixture fixture = queryFixture("com.example.UserMapper.selectPage");

        assertDoesNotThrow(() -> fixture.invoke(interceptor));
    }

    @Test
    void shouldFailClosedWhenConfiguredMapperCannotLoadUserContext() {
        IDataScopeService service = mock(IDataScopeService.class);
        SysDataScopeConfig config = new SysDataScopeConfig();
        config.setEnabled(1);
        when(service.getDataScopeConfig("com.example.UserMapper.selectPage")).thenReturn(config);
        when(service.getCurrentUserDataScope()).thenThrow(new IllegalStateException("missing context"));
        DataScopeInterceptor interceptor = new DataScopeInterceptor(service, new DataScopeProperties());
        QueryFixture fixture = queryFixture("com.example.UserMapper.selectPage");

        assertThrows(SQLException.class, () -> fixture.invoke(interceptor));
    }

    private QueryFixture queryFixture(String mapperId) {
        Configuration configuration = new Configuration();
        SqlSource sqlSource = parameterObject -> new BoundSql(configuration,
                "SELECT id FROM sys_user", List.of(), parameterObject);
        MappedStatement statement = new MappedStatement.Builder(configuration, mapperId, sqlSource,
                SqlCommandType.SELECT).build();
        return new QueryFixture(statement, statement.getBoundSql(null));
    }

    private record QueryFixture(MappedStatement statement, BoundSql boundSql) {

        void invoke(DataScopeInterceptor interceptor) throws SQLException {
            interceptor.beforeQuery(mock(Executor.class), statement, null, RowBounds.DEFAULT,
                    mock(ResultHandler.class), boundSql);
        }
    }
}

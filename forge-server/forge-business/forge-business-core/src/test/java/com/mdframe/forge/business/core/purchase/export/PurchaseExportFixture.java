package com.mdframe.forge.business.core.purchase.export;

import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysTenantPackageService;
import com.mdframe.forge.plugin.system.service.impl.PermissionServiceImpl;
import com.mdframe.forge.plugin.system.service.impl.SysResourceServiceImpl;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.auth.config.SaTokenConfig;
import com.mdframe.forge.starter.auth.exception.SaTokenExceptionHandler;
import com.mdframe.forge.starter.auth.interceptor.ApiPermissionInterceptor;
import com.mdframe.forge.starter.auth.interceptor.ApiRateLimitInterceptor;
import com.mdframe.forge.starter.auth.service.ApiRateLimitManager;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.datascope.config.DataScopeProperties;
import com.mdframe.forge.starter.datascope.handler.DataScopeInterceptor;
import com.mdframe.forge.starter.datascope.mapper.DataScopeOrgMapper;
import com.mdframe.forge.starter.datascope.mapper.DataScopeRegionMapper;
import com.mdframe.forge.starter.datascope.mapper.DataScopeRoleMapper;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleDataScopeMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.impl.DataScopeServiceImpl;
import com.mdframe.forge.starter.orm.interceptor.CountOnePaginationInnerInterceptor;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

@Configuration(proxyBeanMethods = false)
@EnableWebMvc
@ImportAutoConfiguration(DispatcherServletAutoConfiguration.class)
@Import({SaTokenConfig.class, SaTokenExceptionHandler.class, PurchaseExportFixtureController.class})
class PurchaseExportFixture {

    static final String MIGRATION_LOCATION = "purchase-export/migration";
    private static final String[] MAPPER_FILES = {
        "business/SamplePurchaseOrderMapper", "SysUserOrgRoleMapper", "SysResourceMapper", "SysRoleResourceMapper",
        "datascope/SysDataScopeConfigMapper", "datascope/DataScopeRoleMapper", "datascope/DataScopeOrgMapper",
        "datascope/SysRoleDataScopeMapper", "datascope/SysRoleModuleDataScopeMapper", "datascope/DataScopeRegionMapper"
    };

    static EmbeddedDatabase emptyDatabase() {
        EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        new JdbcTemplate(database).execute("SET MODE MySQL");
        return database;
    }

    static FluentConfiguration migration(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:" + MIGRATION_LOCATION)
                .table("forge_schema_history");
    }

    @Bean(destroyMethod = "shutdown")
    EmbeddedDatabase dataSource() {
        EmbeddedDatabase database = emptyDatabase();
        migration(database).load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(database);
        // Deliberately partial assignments exercise each guard independently.
        addPartialGrant(jdbc, 102, PurchaseExportFixtureController.EXPORT_PERMISSION);
        addPartialGrant(jdbc, 103, PurchaseExportFixtureController.EXPORT_API_PERMISSION);
        return database;
    }

    private void addPartialGrant(JdbcTemplate jdbc, long roleId, String permission) {
        jdbc.update("""
                INSERT INTO sys_role_resource (tenant_id, role_id, resource_id)
                SELECT tenant_id, ?, id FROM sys_resource WHERE tenant_id = 1 AND perms = ?
                """, roleId, permission);
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    TomcatServletWebServerFactory webServerFactory() throws UnknownHostException {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.setAddress(InetAddress.getByName("127.0.0.1"));
        return factory;
    }

    @Bean
    SaTokenContextFilterForJakartaServlet saTokenContextFilter() {
        return new SaTokenContextFilterForJakartaServlet();
    }

    @Bean
    AuthProperties authProperties() {
        return new AuthProperties();
    }

    @Bean
    IApiConfigManager apiConfigManager() {
        // No custom endpoint settings. Authorization still goes through the production checks.
        return mock(IApiConfigManager.class);
    }

    @Bean
    SysResourceMapper resourceMapper(SqlSessionTemplate session) {
        return session.getMapper(SysResourceMapper.class);
    }

    @Bean
    PermissionServiceImpl permissionService(SysResourceMapper mapper) {
        return new PermissionServiceImpl(mapper);
    }

    @Bean
    SysResourceServiceImpl resources(SqlSessionTemplate session, SysResourceMapper mapper, PermissionServiceImpl permissions) {
        return new SysResourceServiceImpl(mapper, session.getMapper(SysUserRoleMapper.class),
                session.getMapper(SysRoleResourceMapper.class), permissions, new CommunityFeatureGate(),
                mock(ISysTenantPackageService.class));
    }

    @Bean
    ApiPermissionInterceptor apiPermission(PermissionServiceImpl permissions, AuthProperties properties,
                                            IApiConfigManager apiConfigs) {
        return new ApiPermissionInterceptor(permissions, properties, apiConfigs);
    }

    @Bean
    ApiRateLimitInterceptor apiRateLimit(IApiConfigManager apiConfigs, ObjectProvider<ApiRateLimitManager> manager) {
        return new ApiRateLimitInterceptor(apiConfigs, manager);
    }

    @Bean
    DataScopeProperties dataScopeProperties() {
        return new DataScopeProperties();
    }

    @Bean
    DataScopeServiceImpl dataScope(SqlSessionTemplate session, DataScopeProperties properties,
                                   ApplicationEventPublisher events) {
        return new DataScopeServiceImpl(session.getMapper(SysDataScopeConfigMapper.class),
                session.getMapper(DataScopeRoleMapper.class), session.getMapper(DataScopeOrgMapper.class),
                session.getMapper(SysRoleDataScopeMapper.class), session.getMapper(SysRoleModuleDataScopeMapper.class),
                session.getMapper(DataScopeRegionMapper.class), properties, events);
    }

    @Bean
    SqlSessionTemplate sqlSession(DataSource dataSource, ObjectProvider<DataScopeServiceImpl> scope,
                                   DataScopeProperties properties) throws Exception {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataScopeInterceptor(scope::getObject, properties));
        interceptor.addInnerInterceptor(new CountOnePaginationInnerInterceptor());
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(SysUserRoleMapper.class);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setPlugins(interceptor);
        factory.setMapperLocations(Stream.of(MAPPER_FILES)
                .map(name -> new ClassPathResource("mapper/" + name + ".xml")).toArray(Resource[]::new));
        return new SqlSessionTemplate(factory.getObject());
    }
}

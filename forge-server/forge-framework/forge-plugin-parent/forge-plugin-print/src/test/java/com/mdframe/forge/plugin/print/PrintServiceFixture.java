package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.junit.jupiter.api.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 测试专用数据源和适配器，绝不读取项目运行配置或连接真实业务库。
 */
abstract class PrintServiceFixture {

    final PrintSourceRequest source = new PrintSourceRequest(2L, PrintSourceType.LOWCODE, "page_purchase", null, "purchase");

    final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule()).disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    final PrintProtocolValidator protocol = new PrintProtocolValidator();

    final Set<String> permissions = new HashSet<>();

    PrintActor actor;

    Connection anchor;

    JdbcTemplate jdbc;

    DataSourceTransactionManager tx;

    jakarta.validation.ValidatorFactory validation;

    PrintIdentity identity;

    Adapter adapter;

    PrintProviderRegistry registry;

    PrintTemplateAccess access;

    PrintTemplateMapper templates;

    PrintTemplateVersionMapper versions;

    PrintBindingMapper bindingMapper;

    PrintExecutionMapper executionMapper;

    PrintTemplateService service;

    PrintTemplateVersionService publication;

    PrintBindingService bindings;

    PrintPrepareService runtime;

    PrintExecutionService events;

    String schema;

    @BeforeEach
    void setupServices() throws Exception {
        actor = new PrintActor(1L, 9L, 1L);
        permissions.addAll(Set.of("print:template:view", "print:template:manage", "print:template:publish", "print:execute"));
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:service_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=1000");
        anchor = ds.getConnection();
        jdbc = new JdbcTemplate(ds);
        tx = new DataSourceTransactionManager(ds);
        String sql = Files.readString(PrintResourceContractTest.migrationDirectory().resolve("V1.0.171__add_native_print_tables.sql")).replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", "");
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement);
            }
        }
        jdbc.execute("ALTER TABLE sys_print_template MODIFY page_id VARCHAR(128)");
        jdbc.execute("ALTER TABLE sys_print_binding MODIFY page_id VARCHAR(128)");
        jdbc.execute("CREATE TABLE synthetic_application (id BIGINT PRIMARY KEY,tenant_id BIGINT)");
        jdbc.update("INSERT INTO synthetic_application VALUES (2,1)");
        var config = PrintMapperContractTest.configuration();
        config.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), ds));
        var session = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config));
        templates = spy(session.getMapper(PrintTemplateMapper.class));
        versions = session.getMapper(PrintTemplateVersionMapper.class);
        bindingMapper = session.getMapper(PrintBindingMapper.class);
        executionMapper = session.getMapper(PrintExecutionMapper.class);
        validation = Validation.buildDefaultValidatorFactory();
        identity = new PrintIdentity(validation.getValidator()) {

            @Override
            public PrintActor require(String permission) {
                if (!permissions.contains(permission) || actor == null) {
                    throw PrintFailure.denied();
                }
                return actor;
            }
        };
        adapter = new Adapter();
        registry = new PrintProviderRegistry(List.of(adapter), List.of(adapter));
        access = new PrintTemplateAccess(templates, registry, identity);
        var documents = new PrintDocumentAccess(json);
        service = transactional(new PrintTemplateService(identity, templates, bindingMapper, access, registry, protocol, documents));
        publication = transactional(new PrintTemplateVersionService(identity, access, templates, versions, protocol, documents, json));
        bindings = transactional(new PrintBindingService(identity, access, bindingMapper, templates));
        events = transactional(new PrintExecutionService(identity, executionMapper));
        runtime = transactional(new PrintPrepareService(identity, registry, access, templates, versions, protocol, documents, new PrintDataProjector(json, documents), events));
        try (var input = getClass().getResourceAsStream("/print/valid-document.json")) {
            schema = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    <T> T transactional(T target) {
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }

    @AfterEach
    void closeServices() throws Exception {
        if (anchor != null) {
            anchor.close();
        }
        if (validation != null) {
            validation.close();
        }
    }

    PrintTemplateVO create(String code) {
        return service.create(new PrintTemplateCreateDTO(2L, code, "合成模板", PrintSourceType.LOWCODE, "page_purchase", null, "purchase", schema));
    }

    PrintTemplateVO publish(PrintTemplateVO row) {
        return publication.publish(row.id(), new PrintTemplatePublishDTO(row.draftRevision())).template();
    }

    PrintTemplateVO published() {
        var row = publish(create("fixture"));
        adapter.refs = List.of(new AuthorizedPrintContext.VersionRef(row.id(), row.publishedVersionId(), true, 0));
        return row;
    }

    PrintRecordRequest record() {
        return new PrintRecordRequest(source, "synthetic_record", PrintScene.DETAIL, null, null, null);
    }

    void fails(int code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getCode()).isEqualTo(code));
    }

    class Adapter implements PrintDataProvider, PrintApplicationAccess {

        boolean denied, designDenied, resourceDenied, referenced, spoofed;

        List<AuthorizedPrintContext.VersionRef> refs = List.of();

        PrintFieldCatalogVO fields = new PrintFieldCatalogVO(List.of(new PrintFieldCatalogVO.Field("main.number", "单号", "TEXT"), new PrintFieldCatalogVO.Field("main.remark", "备注", "TEXT"), new PrintFieldCatalogVO.Field("main.total", "合计", "MONEY"), new PrintFieldCatalogVO.Field("children.items", "明细", "COLLECTION"), new PrintFieldCatalogVO.Field("children.items.name", "名称", "TEXT"), new PrintFieldCatalogVO.Field("children.items.amount", "金额", "MONEY")));

        PrintData data = new PrintData(Map.of("number", "SYN-001", "remark", "合成备注", "total", 100, "secret", "MUST_NOT_LEAK"), Map.of("items", List.of(Map.of("name", "合成项目", "amount", 100, "secret", "MUST_NOT_LEAK"))), Map.of("secret", "MUST_NOT_LEAK"));

        @Override
        public void authorize(PrintActor current, Long app, PrintDesignAction action) {
            if (designDenied || !Long.valueOf(2).equals(app) || current.tenantId() != 1L) {
                throw PrintFailure.denied();
            }
        }

        @Override
        public void lockApplication(PrintActor current, Long app) {
            jdbc.queryForObject("SELECT id FROM synthetic_application WHERE tenant_id=? AND id=? FOR UPDATE", Long.class, current.tenantId(), app);
        }

        @Override
        public void assertTemplateUnreferenced(PrintActor current, Long app, Long template) {
            if (referenced) {
                throw PrintFailure.of(409, "PRINT_TEMPLATE_REFERENCED", "合成已发布应用引用");
            }
        }

        @Override
        public PrintSourceType sourceType() {
            return PrintSourceType.LOWCODE;
        }

        @Override
        public boolean supports(PrintSourceRequest candidate) {
            return true;
        }

        @Override
        public void authorizeDesignSource(PrintActor current, PrintSourceRequest candidate, PrintDesignAction action) {
            if (!source.equals(candidate) || designDenied) {
                throw PrintFailure.denied();
            }
        }

        @Override
        public PrintFieldCatalogVO catalog(AuthorizedPrintSource authorized) {
            return fields;
        }

        @Override
        public void validateDesignResources(AuthorizedPrintSource authorized, Set<String> files) {
            if (resourceDenied) {
                throw PrintFailure.denied();
            }
        }

        @Override
        public AuthorizedPrintContext authorize(PrintActor current, PrintRecordRequest request) {
            if (denied || current.tenantId() != 1L || !source.equals(request.source()) || !"synthetic_record".equals(request.recordId())) {
                throw PrintFailure.denied();
            }
            return new AuthorizedPrintContext(spoofed ? new PrintActor(1L, 8L, 1L) : current, request, 11L, refs, fields);
        }

        @Override
        public PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection) {
            return data;
        }

        @Override
        public void validateRuntimeResources(AuthorizedPrintContext context, Set<String> files) {
            if (resourceDenied) {
                throw PrintFailure.denied();
            }
        }
    }
}

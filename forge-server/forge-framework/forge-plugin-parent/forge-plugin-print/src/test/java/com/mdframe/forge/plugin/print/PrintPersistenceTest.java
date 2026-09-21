package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.entity.*;
import com.mdframe.forge.plugin.print.mapper.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/**
 * H2 仅验证 SQL 行为；MySQL 方言、租户拦截器与真实并发仍由集成验收覆盖。
 */
class PrintPersistenceTest {

    private SqlSession session;

    private PrintTemplateMapper templates;

    private PrintTemplateVersionMapper versions;

    @BeforeEach
    void setup() throws Exception {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:print_" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        var config = PrintMapperContractTest.configuration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(), ds));
        session = new SqlSessionFactoryBuilder().build(config).openSession(false);
        String migration = Files.readString(PrintResourceContractTest.migrationDirectory().resolve("V1.0.171__add_native_print_tables.sql"));
        migration = migration.replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci", "");
        try (var statement = session.getConnection().createStatement()) {
            for (String sql : migration.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("ALTER TABLE sys_print_template MODIFY page_id VARCHAR(128)");
            statement.execute("ALTER TABLE sys_print_binding MODIFY page_id VARCHAR(128)");
        }
        templates = session.getMapper(PrintTemplateMapper.class);
        versions = session.getMapper(PrintTemplateVersionMapper.class);
    }

    @AfterEach
    void close() {
        if (session != null) {
            session.close();
        }
    }

    private <T extends TenantEntity> T audit(T row) {
        row.setTenantId(1L);
        row.setCreateBy(9L);
        row.setUpdateBy(9L);
        row.setCreateDept(1L);
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(LocalDateTime.now());
        return row;
    }

    private PrintTemplate template() {
        var row = audit(new PrintTemplate());
        row.setApplicationId(2L);
        row.setTemplateCode("purchase");
        row.setTemplateName("采购单");
        row.setSourceType("LOWCODE");
        row.setSourceKey("page:3");
        row.setPageId("page_purchase");
        row.setObjectCode("purchase");
        row.setDraftSchema("{}");
        row.setDraftRevision(1L);
        row.setDesignStatus("DRAFT");
        row.setStatus(1);
        row.setDelFlag(0L);
        templates.insert(row);
        assertThat(templates.selectScoped(1L, row.getId()).getPageId()).isEqualTo("page_purchase");
        return row;
    }

    private PrintTemplateVersion version(Long templateId, int versionNo) {
        var row = audit(new PrintTemplateVersion());
        row.setTemplateId(templateId);
        row.setVersionNo(versionNo);
        row.setSchemaVersion(1);
        row.setSchemaJson("{}");
        row.setSchemaHash("a".repeat(64));
        row.setResourceManifest("[]");
        row.setPublishTime(LocalDateTime.now());
        row.setDelFlag(0);
        versions.insert(row);
        return row;
    }

    @Test
    void rejectsStaleEditsCrossTenantPublishAndPreservesPublishedVersion() {
        var row = template();
        var version = version(row.getId(), 1);
        assertThat(templates.publish(2L, row.getId(), 1L, version.getId(), 9L)).isZero();
        assertThat(templates.publish(1L, row.getId(), 1L, version.getId(), 9L)).isEqualTo(1);
        row.setDraftSchema("{\"changed\":true}");
        assertThat(templates.updateDraft(row, 1L)).isZero();
        assertThat(templates.updateDraft(row, 2L)).isEqualTo(1);
        assertThat(templates.selectScoped(1L, row.getId()).getDesignStatus()).isEqualTo("CHANGED");
        assertThat(versions.selectScoped(1L, row.getId(), version.getId()).getSchemaJson()).isEqualTo("{}");
        assertThat(templates.selectScoped(2L, row.getId())).isNull();
        assertThat(templates.changeStatus(1L, row.getId(), 3L, 0, 9L)).isEqualTo(1);
        assertThat(templates.publish(1L, row.getId(), 4L, version.getId(), 9L)).isZero();
    }

    @Test
    void softDeletionReleasesCodeRepeatedlyButVersionsNeverReuseNumbers() throws Exception {
        var first = template();
        var oldVersion = version(first.getId(), 1);
        assertThat(templates.softDelete(1L, first.getId(), 1L, 9L)).isEqualTo(1);
        var second = template();
        assertThat(templates.softDelete(1L, second.getId(), 1L, 9L)).isEqualTo(1);
        assertThat(template().getId()).isNotEqualTo(first.getId());
        assertThat(templates.selectScoped(1L, first.getId())).isNull();
        try (var statement = session.getConnection().prepareStatement("UPDATE sys_print_template_version SET del_flag=1 WHERE id=?")) {
            statement.setLong(1, oldVersion.getId());
            statement.executeUpdate();
        }
        assertThat(versions.selectMaxVersionNo(1L, first.getId())).isEqualTo(1);
        assertThatThrownBy(() -> version(first.getId(), 1)).isInstanceOf(org.apache.ibatis.exceptions.PersistenceException.class);
    }

    @Test
    void activeBindingProtectsTemplateAndExecutionEventsAreOwnedAndMonotonic() {
        var row = template();
        var binding = audit(new PrintBinding());
        binding.setApplicationId(2L);
        binding.setSourceType("LOWCODE");
        binding.setSourceKey("page:3");
        binding.setPageId("3");
        binding.setObjectCode("purchase");
        binding.setTemplateId(row.getId());
        binding.setScene("DETAIL");
        binding.setIsDefault(true);
        binding.setSortOrder(0);
        binding.setStatus(1);
        binding.setBindingRevision(1L);
        binding.setDelFlag(0L);
        session.getMapper(PrintBindingMapper.class).insert(binding);
        assertThat(templates.softDelete(1L, row.getId(), 1L, 9L)).isZero();
        var execution = audit(new PrintExecution());
        execution.setApplicationId(2L);
        execution.setTemplateId(row.getId());
        execution.setTemplateVersionId(1L);
        execution.setSourceKey("page:3");
        execution.setObjectCode("purchase");
        execution.setRecordId("synthetic_1");
        execution.setScene("DETAIL");
        execution.setActor(9L);
        execution.setDataMode("CURRENT");
        execution.setGeneratedAt(LocalDateTime.now());
        execution.setResult("PREPARED");
        execution.setDelFlag(0);
        var events = session.getMapper(PrintExecutionMapper.class);
        events.insert(execution);
        assertThat(events.recordEvent(1L, execution.getId(), 8L, "DIALOG_OPENED", 1, null)).isZero();
        assertThat(events.recordEvent(2L, execution.getId(), 9L, "DIALOG_OPENED", 1, null)).isZero();
        assertThat(events.recordEvent(1L, execution.getId(), 9L, "SUCCESS", 1, null)).isZero();
        assertThat(events.recordEvent(1L, execution.getId(), 9L, "DIALOG_OPENED", 51, null)).isZero();
        assertThat(events.recordEvent(1L, execution.getId(), 9L, "DIALOG_OPENED", 1, null)).isEqualTo(1);
        assertThat(events.recordEvent(1L, execution.getId(), 9L, "FAILED", 1, "FAILED")).isZero();
        assertThat(events.selectOwned(1L, execution.getId(), 8L)).isNull();
    }

    @Test
    void publishingAnotherTemplatesVersionIsRejectedAndRollbackLeavesNoPartialVersion() {
        var first = template();
        assertThat(templates.softDelete(1L, first.getId(), 1L, 9L)).isEqualTo(1);
        var current = template();
        var wrong = version(first.getId(), 1);
        assertThat(templates.publish(1L, current.getId(), 1L, wrong.getId(), 9L)).isZero();
        session.commit();
        var next = version(current.getId(), 1);
        assertThat(templates.publish(1L, current.getId(), 1L, next.getId(), 9L)).isEqualTo(1);
        session.rollback();
        assertThat(templates.selectScoped(1L, current.getId()).getPublishedVersionId()).isNull();
        assertThat(versions.selectVersions(1L, current.getId())).isEmpty();
    }

    @Test
    void defaultResetIsScopedAndInvalidatesStaleBindingEdits() {
        var row = template();
        var binding = audit(new PrintBinding());
        binding.setApplicationId(2L);
        binding.setSourceType("LOWCODE");
        binding.setSourceKey("page:3");
        binding.setPageId("3");
        binding.setObjectCode("purchase");
        binding.setTemplateId(row.getId());
        binding.setScene("DETAIL");
        binding.setIsDefault(true);
        binding.setSortOrder(0);
        binding.setStatus(1);
        binding.setBindingRevision(1L);
        binding.setDelFlag(0L);
        var bindings = session.getMapper(PrintBindingMapper.class);
        bindings.insert(binding);
        assertThat(bindings.clearDefault(2L, 2L, "page:3", "DETAIL", 9L)).isZero();
        assertThat(bindings.clearDefault(1L, 2L, "page:3", "LIST", 9L)).isZero();
        assertThat(bindings.clearDefault(1L, 2L, "page:3", "DETAIL", 9L)).isEqualTo(1);
        assertThat(bindings.updateBinding(binding, 1L)).isZero();
        assertThat(bindings.selectSource(1L, 2L, "page:3", "DETAIL").get(0).getIsDefault()).isFalse();
        assertThat(bindings.softDelete(1L, binding.getId(), 2L, 9L)).isEqualTo(1);
        assertThat(templates.softDelete(1L, row.getId(), 1L, 9L)).isEqualTo(1);
    }
}

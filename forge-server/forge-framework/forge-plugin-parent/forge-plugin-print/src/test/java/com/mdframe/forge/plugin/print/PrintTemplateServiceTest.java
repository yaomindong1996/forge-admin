package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintTemplateServiceTest extends PrintServiceFixture {

    @Test
    void staleEditDoesNotOverwriteAndCopyStartsIndependentDraft() {
        var row = publish(create("one"));
        var saved = service.update(row.id(), new PrintTemplateUpdateDTO(row.draftRevision(), "已更新", schema));
        fails(409, () -> service.update(row.id(), new PrintTemplateUpdateDTO(row.draftRevision(), "旧请求", schema)));
        assertThat(service.detail(row.id()).templateName()).isEqualTo("已更新");
        var copied = service.copy(saved.id(), new PrintTemplateCopyDTO(saved.draftRevision(), "copy", "副本"));
        assertThat(copied.publishedVersionId()).isNull();
        assertThat(copied.source()).isEqualTo(source);
        assertThat(copied.draftRevision()).isEqualTo(1);
    }

    @Test
    void metadataListOmitsSchemaAndIsScoped() {
        create("one");
        assertThat(service.page(2L, 1, 20).records()).singleElement().satisfies(row -> assertThat(row.schemaJson()).isNull());
        fails(403, () -> service.page(3L, 1, 20));
        fails(400, () -> service.page(2L, 1, 101));
    }

    @Test
    void tenantAndDesignPermissionCannotBeSpoofed() {
        var row = create("one");
        actor = new PrintActor(2L, 9L, 1L);
        fails(404, () -> service.detail(row.id()));
        actor = new PrintActor(1L, 9L, 1L);
        permissions.clear();
        permissions.add("print:execute");
        fails(403, () -> service.detail(row.id()));
    }

    @Test
    void deniesSourceFieldsAndFilesBeforeInserting() {
        adapter.fields = new PrintFieldCatalogVO(List.of());
        fails(400, () -> create("missing"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_print_template", Integer.class)).isZero();
    }

    @Test
    void duplicateCodeIsConflictAndDeletionAllowsReuse() {
        var row = create("one");
        fails(409, () -> create("one"));
        service.delete(row.id(), row.draftRevision());
        assertThat(create("one").id()).isNotEqualTo(row.id());
        assertThat(jdbc.queryForObject("SELECT del_flag FROM sys_print_template WHERE id=?", Long.class, row.id())).isEqualTo(row.id());
    }

    @Test
    void publishedApplicationReferencePreventsDeletion() {
        var row = create("one");
        adapter.referenced = true;
        fails(409, () -> service.delete(row.id(), row.draftRevision()));
        assertThat(service.detail(row.id()).id()).isEqualTo(row.id());
    }
}

package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintTemplateVersionServiceTest extends PrintServiceFixture {

    @Test
    void repeatedContentReusesImmutableVersionAndChangedContentCreatesNext() {
        var row = published();
        var repeated = publish(row);
        assertThat(repeated.publishedVersionId()).isEqualTo(row.publishedVersionId());
        assertThat(publication.list(row.id())).hasSize(1);
        assertThat(repeated.draftRevision()).isEqualTo(row.draftRevision());
        var changed = service.update(row.id(), new PrintTemplateUpdateDTO(row.draftRevision(), "修改", schema.replace("合成采购单", "合成新单据")));
        var next = publish(changed);
        assertThat(next.publishedVersionId()).isNotEqualTo(row.publishedVersionId());
        assertThat(publication.detail(row.id(), row.publishedVersionId()).schemaJson()).contains("合成采购单").doesNotContain("合成新单据");
    }

    @Test
    void failureAfterVersionInsertRollsBackEntirePublication() {
        var row = create("one");
        doReturn(0).when(templates).publish(1L, row.id(), row.draftRevision(), 1L, 9L);
        fails(409, () -> publish(row));
        assertThat(publication.list(row.id())).isEmpty();
        assertThat(service.detail(row.id()).publishedVersionId()).isNull();
    }

    @Test
    void removedFieldOrResourcePermissionDoesNotReplacePublishedVersion() {
        var row = published();
        var changed = service.update(row.id(), new PrintTemplateUpdateDTO(row.draftRevision(), "修改", schema.replace("合成采购单", "另一合成单")));
        adapter.resourceDenied = true;
        fails(403, () -> publish(changed));
        assertThat(service.detail(row.id()).publishedVersionId()).isEqualTo(row.publishedVersionId());
        assertThat(publication.list(row.id())).hasSize(1);
    }

    @Test
    void disabledTemplateCannotPublish() {
        var row = create("one");
        var disabled = service.status(row.id(), new PrintTemplateStatusDTO(row.draftRevision(), 0));
        fails(404, () -> publish(disabled));
    }
}

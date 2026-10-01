package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.service.PrintRuntimeBindingResolver;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrintRuntimeBindingResolverTest {

    private final PrintBusinessSourceMapper sources = mock(PrintBusinessSourceMapper.class);
    private final PrintBindingMapper bindings = mock(PrintBindingMapper.class);
    private final PrintTemplateMapper templates = mock(PrintTemplateMapper.class);
    private final PrintTemplateVersionMapper versions = mock(PrintTemplateVersionMapper.class);
    private final PrintRuntimeBindingResolver resolver = new PrintRuntimeBindingResolver(
            sources, bindings, templates, versions);

    private AuthorizedPrintContext context;
    private PrintSourceRequest source;

    @BeforeEach
    void setUp() {
        source = new PrintSourceRequest(
                7L, "purchase_order", null, PrintSourceType.SERVICE,
                null, null, "purchase_order");
        var record = new PrintRecordRequest(source, "PO-100", PrintScene.DETAIL,
                null, null, null);
        context = new AuthorizedPrintContext(
                new PrintActor(1L, 9L, 2L), record, null, List.of(),
                new PrintFieldCatalogVO(List.of()));
        when(sources.selectScoped(1L, 7L)).thenReturn(businessSource());
    }

    @Test
    void shouldUseOnlyVersionFixedByEnabledBinding() {
        var binding = binding(31L);
        when(bindings.selectStandaloneEnabled(1L, 7L, source.key(), "DETAIL"))
                .thenReturn(List.of(binding));
        when(templates.selectScoped(1L, 21L)).thenReturn(template());
        when(versions.selectScoped(1L, 21L, 31L)).thenReturn(version(31L));

        var resolved = resolver.resolve(context);

        assertThat(resolved.applicationVersionId()).isNull();
        assertThat(resolved.sourceRevision()).isEqualTo(5L);
        assertThat(resolved.versions()).containsExactly(
                new AuthorizedPrintContext.VersionRef(21L, 31L, true, 2));
    }

    @Test
    void shouldNotFallbackToTemplatesLatestPublishedVersion() {
        var binding = binding(31L);
        when(bindings.selectStandaloneEnabled(1L, 7L, source.key(), "DETAIL"))
                .thenReturn(List.of(binding));
        when(templates.selectScoped(1L, 21L)).thenReturn(template());
        when(versions.selectScoped(1L, 21L, 31L)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(context))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo(409));
    }

    private PrintBusinessSource businessSource() {
        var row = new PrintBusinessSource();
        row.setId(7L);
        row.setTenantId(1L);
        row.setSourceCode("purchase_order");
        row.setSourceType(PrintSourceType.SERVICE.getCode());
        row.setObjectCode("purchase_order");
        row.setSourceRevision(5L);
        row.setStatus(1);
        row.setDelFlag(0L);
        return row;
    }

    private PrintBinding binding(Long versionId) {
        var row = new PrintBinding();
        row.setTemplateId(21L);
        row.setTemplateVersionId(versionId);
        row.setIsDefault(true);
        row.setSortOrder(2);
        return row;
    }

    private PrintTemplate template() {
        var row = new PrintTemplate();
        row.setId(21L);
        row.setTenantId(1L);
        row.setBusinessSourceId(7L);
        row.setSourceCode("purchase_order");
        row.setSourceType(PrintSourceType.SERVICE.getCode());
        row.setSourceKey(source.key());
        row.setObjectCode("purchase_order");
        row.setPublishedVersionId(99L);
        row.setStatus(1);
        row.setDelFlag(0L);
        return row;
    }

    private PrintTemplateVersion version(Long id) {
        var row = new PrintTemplateVersion();
        row.setId(id);
        row.setTemplateId(21L);
        row.setVersionNo(3);
        return row;
    }
}

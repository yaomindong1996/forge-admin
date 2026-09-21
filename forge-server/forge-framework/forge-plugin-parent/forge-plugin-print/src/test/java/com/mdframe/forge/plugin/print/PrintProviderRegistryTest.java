package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.service.PrintProviderRegistry;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintProviderRegistryTest {

    private final PrintSourceRequest source = new PrintSourceRequest(1L, PrintSourceType.LOWCODE, "2", null, "purchase");

    @Test
    void missingProvidersFailClosedWithoutBreakingStartup() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext(PrintProviderRegistry.class)) {
            var registry = context.getBean(PrintProviderRegistry.class);
            assertThatThrownBy(() -> registry.provider(source)).hasMessageContaining("数据适配器");
            assertThatThrownBy(registry::application).hasMessageContaining("应用授权适配器");
        }
    }

    @Test
    void ambiguousProvidersAreRejectedAndBeanNamesAreNeverSelected() {
        var first = mock(PrintDataProvider.class);
        var second = mock(PrintDataProvider.class);
        for (var provider : List.of(first, second)) {
            when(provider.sourceType()).thenReturn(PrintSourceType.LOWCODE);
            when(provider.supports(source)).thenReturn(true);
        }
        assertThatThrownBy(() -> new PrintProviderRegistry(List.of(first, second), List.of()).provider(source)).hasMessageContaining("数据适配器");
        assertThat(new PrintProviderRegistry(List.of(first), List.of()).provider(source)).isSameAs(first);
    }
}

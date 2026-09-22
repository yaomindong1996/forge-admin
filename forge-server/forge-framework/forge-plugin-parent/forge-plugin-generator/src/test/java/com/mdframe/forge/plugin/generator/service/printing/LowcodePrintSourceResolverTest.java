package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.mdframe.forge.plugin.generator.service.printing.PrintLowcodeTestData.JSON;

class LowcodePrintSourceResolverTest {
    private final LowcodePrintSourceResolver resolver = new LowcodePrintSourceResolver();

    @Test
    void acceptsGridBlockPropsObjectRefWithoutConfigKey() throws Exception {
        var root = JSON.readTree("""
            {"application":{"options":{"inAppBuilder":{
              "nodes":[{"id":"page_custom","type":"page"}],
              "pages":{"page_custom":{"layout":{"gridLayout":{"items":[
                {"props":{"objectRef":{"objectId":"3","objectCode":"purchase"}}}
              ]}}}}
            }}},"objects":[{"objectId":"3","objectCode":"purchase","configKey":"purchase"}]}
            """);
        var source = new PrintSourceRequest(2L, PrintSourceType.LOWCODE, "page_custom", null, "purchase");
        assertThat(resolver.object(root, source, false).path("configKey").asText()).isEqualTo("purchase");
    }

    @Test
    void acceptsBusinessObjectRefOnBlockProps() throws Exception {
        var root = JSON.readTree("""
            {"application":{"options":{"inAppBuilder":{
              "nodes":[{"id":"page_custom","type":"page"}],
              "pages":{"page_custom":{"layout":{"items":[
                {"props":{"businessObjectRef":{"id":"3","objectCode":"purchase"}}}
              ]}}}
            }}},"objects":[{"objectId":"3","objectCode":"purchase","configKey":"purchase"}]}
            """);
        var source = new PrintSourceRequest(2L, PrintSourceType.LOWCODE, "page_custom", null, "purchase");
        assertThatCode(() -> resolver.object(root, source, false)).doesNotThrowAnyException();
    }

    @Test
    void rejectsPageWithoutMatchingObjectRef() throws Exception {
        var root = JSON.readTree("""
            {"application":{"options":{"inAppBuilder":{
              "nodes":[{"id":"page_custom","type":"page"}],
              "pages":{"page_custom":{"layout":{"gridLayout":{"items":[
                {"props":{"objectRef":{"objectId":"99","objectCode":"other"}}}
              ]}}}}
            }}},"objects":[{"objectId":"3","objectCode":"purchase","configKey":"purchase"}]}
            """);
        var source = new PrintSourceRequest(2L, PrintSourceType.LOWCODE, "page_custom", null, "purchase");
        assertThatThrownBy(() -> resolver.object(root, source, false)).isInstanceOf(BusinessException.class);
    }
}

package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.mdframe.forge.plugin.generator.service.printing.PrintApplicationTestData.*;
import static org.assertj.core.api.Assertions.*;

class PrintApplicationSnapshotCodecTest {
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final PrintApplicationSnapshotCodec codec = new PrintApplicationSnapshotCodec(factory.getValidator());
    @AfterEach void close() { factory.close(); }

    @Test void roundTripsFixedVersionsAndLegacyAbsence() {
        var expected = binding(10L, true);
        assertThat(codec.read(snapshot(expected), 2L)).containsExactly(expected);
        assertThat(codec.read("{\"application\":{\"id\":\"2\"}}", 2L)).isEmpty();
        assertThat(codec.read(snapshot(), 2L)).isEmpty();
        assertThatThrownBy(() -> codec.read(snapshot(expected), 3L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> codec.read(snapshot(expected), 2L).clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void rejectsDuplicateDefaultsReferencesUnknownVersionsAndMalformedSnapshots() {
        for (String json : List.of("null", "[]", "{} {}", "{\"printing\":null}",
                "{\"printing\":{\"schemaVersion\":2,\"bindings\":[]}}",
                "{\"printing\":{},\"printing\":{}}",
                snapshot(binding(10, true), binding(11, true)),
                snapshot(binding(10, false), binding(10, false)))) {
            assertThatThrownBy(() -> codec.read(json, 2L)).isInstanceOf(BusinessException.class);
        }
    }

    @Test void rejectsLossyIdsUnknownFieldsAndUntrustedSource() throws Exception {
        for (String field : List.of("templateId", "templateVersionId")) {
            for (String value : List.of("1.5", "true", "-1", "9223372036854775808", "\"01\"")) {
                ObjectNode root = root();
                ((ObjectNode) root.at("/printing/bindings/0")).set(field, new ObjectMapper().readTree(value));
                assertThatThrownBy(() -> codec.read(root.toString(), 2L)).isInstanceOf(BusinessException.class);
            }
        }
        ObjectNode root = root();
        ((ObjectNode) root.at("/printing/bindings/0/source")).put("pageId", "wrong/page");
        String wrongSource = root.toString();
        assertThatThrownBy(() -> codec.read(wrongSource, 2L)).isInstanceOf(BusinessException.class);
        root = root();
        ((ObjectNode) root.at("/printing/bindings/0")).put("provider", "clientBean");
        String invalid = root.toString();
        assertThatThrownBy(() -> codec.read(invalid, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsOverlargeListsAndPreservesLargeIntegralIds() throws Exception {
        ObjectNode root = root();
        ((ObjectNode) root.at("/printing/bindings/0")).put("templateId", "9007199254740993");
        assertThat(codec.read(root.toString(), 2L).get(0).templateId()).isEqualTo(9007199254740993L);
        var bindings = new PrintApplicationSnapshotCodec.Binding[101];
        for (int i = 0; i < bindings.length; i++) { bindings[i] = binding(i + 1, false); }
        assertThatThrownBy(() -> codec.read(snapshot(bindings), 2L)).isInstanceOf(BusinessException.class);
    }
    private ObjectNode root() throws Exception {
        return (ObjectNode) new ObjectMapper().readTree(snapshot(binding(10, true)));
    }
}

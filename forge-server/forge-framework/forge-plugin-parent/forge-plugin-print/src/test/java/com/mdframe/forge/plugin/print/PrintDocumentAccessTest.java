package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.service.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PrintDocumentAccessTest extends PrintServiceFixture {

    @Test
    void imageElementsCannotBypassFileAuthorizationThroughTextFields() throws Exception {
        var tree = json.readTree(schema);
        ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("body").get(0).path("elements").get(0)).put("type", "IMAGE");
        var document = protocol.validate(json.writeValueAsString(tree));
        var documents = new PrintDocumentAccess(json);
        fails(400, () -> documents.requirements(document, adapter.fields));
        var fields = new PrintFieldCatalogVO(adapter.fields.fields().stream().map(field -> field.path().equals("main.number") ? new PrintFieldCatalogVO.Field(field.path(), field.label(), "IMAGE") : field).toList());
        var needs = documents.requirements(document, fields);
        var data = new PrintData(Map.of("number", "synthetic_file_2", "secret", "unused_file"), Map.of(), Map.of());
        var projected = new PrintDataProjector(json, documents).project(data, needs, fields);
        assertThat(projected.fileIds()).containsExactlyInAnyOrder("synthetic_file_1", "synthetic_file_2");
    }

    @Test
    void totalPayloadLimitStopsSerializationEvenWhenEachCellIsAllowed() {
        var documents = new PrintDocumentAccess(json);
        var needs = documents.requirements(protocol.validate(schema), adapter.fields);
        var data = new PrintData(Map.of(), Map.of("items", Collections.nCopies(50, Map.of("name", "字".repeat(40000), "amount", 1))), Map.of());
        fails(400, () -> new PrintDataProjector(json, documents).project(data, needs, adapter.fields));
    }

    @Test
    void imageAliasesAuthorizeActualFileIdsAndUnselectedDataNeverSerializes() throws Exception {
        var tree = json.readTree(schema);
        var element = (com.fasterxml.jackson.databind.node.ObjectNode) tree.path("body").get(0).path("elements").get(0);
        element.put("type", "IMAGE");
        element.set("binding", json.readTree("{\"source\":\"CONSTANT\",\"value\":\"logo\"}"));
        var documents = new PrintDocumentAccess(json);
        var needs = documents.requirements(protocol.validate(json.writeValueAsString(tree)), adapter.fields);
        assertThat(needs.staticFileIds()).containsExactly("synthetic_file_1");
        var projected = new PrintDataProjector(json, documents).project(adapter.data, needs, adapter.fields);
        assertThat(json.writeValueAsString(projected.data())).doesNotContain("MUST_NOT_LEAK", "SYN-001");
    }

    @Test
    void nestedObjectValuesAndExternalImageReferencesFailClosed() {
        var documents = new PrintDocumentAccess(json);
        var needs = documents.requirements(protocol.validate(schema), adapter.fields);
        var projector = new PrintDataProjector(json, documents);
        fails(400, () -> projector.project(new PrintData(Map.of("number", Map.of("hidden", "secret")), Map.of(), Map.of()), needs, adapter.fields));
        fails(400, () -> documents.image("https://external.invalid/a.png", new HashSet<>()));
    }

    @Test
    void flowHistorySignatureColumnRequiresAndProjectsItsAuthorizedFile() throws Exception {
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(schema);
        var body = tree.withArray("body");
        body.removeAll();
        tree.withArray("resources").removeAll();
        body.add(json.readTree("""
                {"id":"history","kind":"TABLE","collectionPath":"flow.history","repeatHeader":true,
                 "columns":[{"id":"signature","field":"signature","title":"办理签名","widthMm":80}]}
                """));
        var fields = new PrintFieldCatalogVO(List.of(
                new PrintFieldCatalogVO.Field("flow.history", "审批记录", "COLLECTION"),
                new PrintFieldCatalogVO.Field("flow.history.signature", "办理签名", "IMAGE")));
        var documents = new PrintDocumentAccess(json);
        var needs = documents.requirements(protocol.validate(json.writeValueAsString(tree)), fields);
        var data = new PrintData(Map.of(), Map.of(), Map.of("history", List.of(Map.of("signature", "signature_1"))));
        var projected = new PrintDataProjector(json, documents).project(data, needs, fields);
        assertThat(projected.fileIds()).containsExactly("signature_1");
        assertThat(projected.data().flow()).containsEntry("history", List.of(Map.of("signature", "signature_1")));
    }
}

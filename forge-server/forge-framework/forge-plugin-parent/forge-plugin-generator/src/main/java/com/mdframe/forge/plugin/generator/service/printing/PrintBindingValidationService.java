package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PrintBindingValidationService {
    private final PrintMetadataResolver metadata;
    private final LowcodePrintCatalogBuilder catalogs;
    private final PrintProtocolValidator protocol;
    private final PrintDocumentAccess access;
    private final LowcodePrintResourceAccess resources;
    private final ObjectMapper json;

    public void validate(PrintActor actor, PrintApplicationSnapshotCodec.Binding binding,
                         String schemaJson, JsonNode snapshot, boolean published) {
        if (binding.scene() != PrintScene.LIST && binding.scene() != PrintScene.DETAIL) {
            throw PrintFailure.of(409, "PRINT_SCENE_UNSUPPORTED", "流程打印尚未接入，不能发布此场景绑定");
        }
        var model = published ? metadata.published(actor, binding.source(), snapshot)
                : metadata.candidate(actor, binding.source(), snapshot);
        var catalog = catalogs.build(model);
        var document = protocol.validate(schemaJson);
        var requirements = access.requirements(document, catalog);
        validateFormats(json.valueToTree(document.document()), access.catalog(catalog), null);
        resources.validate(actor, requirements.staticFileIds());
    }

    private void validateFormats(JsonNode node, Map<String, String> types, String collection) {
        if (node.isObject()) {
            if ("TABLE".equals(node.path("kind").asText())) {
                collection = node.path("collectionPath").asText();
            }
            String path = "FIELD".equals(node.path("binding").path("source").asText())
                    ? node.path("binding").path("path").asText()
                    : collection != null && node.has("field") ? collection + "." + node.path("field").asText() : null;
            String format = node.path("format").path("type").asText("TEXT");
            if (path != null && !"TEXT".equals(format) && !format.equals(types.get(path))) {
                throw PrintFailure.field(path, "打印字段类型与格式不兼容：" + path);
            }
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                validateFormats(child, types, collection);
            }
        }
    }
}

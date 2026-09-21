package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.print.protocol.PrintProtocolValidator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/**
 * Node 验证入口读取相同样例；显式标注服务端对未使用属性的更严格检查。
 */
class PrintProtocolCompatibilityTest {

    @TestFactory
    List<DynamicTest> sharedProtocolCases() throws Exception {
        var mapper = new ObjectMapper();
        JsonNode base;
        JsonNode cases;
        try (var input = getClass().getResourceAsStream("/print/valid-document.json")) {
            base = mapper.readTree(input);
        }
        try (var input = getClass().getResourceAsStream("/print/compatibility-cases.json")) {
            cases = mapper.readTree(input);
        }
        var tests = new ArrayList<DynamicTest>();
        for (JsonNode item : cases) {
            tests.add(DynamicTest.dynamicTest(item.get("name").asText(), () -> {
                JsonNode doc = base.deepCopy();
                for (JsonNode patch : item.get("patches")) {
                    String pointer = patch.get("pointer").asText();
                    int split = pointer.lastIndexOf('/');
                    ((ObjectNode) doc.at(pointer.substring(0, split))).set(pointer.substring(split + 1), patch.get("value"));
                }
                var validator = new PrintProtocolValidator();
                if (item.get("backendValid").booleanValue()) {
                    assertThatCode(() -> validator.validate(doc.toString())).doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> validator.validate(doc.toString())).isInstanceOf(PrintProtocolValidator.InvalidTemplateException.class);
                }
            }));
        }
        return tests;
    }
}

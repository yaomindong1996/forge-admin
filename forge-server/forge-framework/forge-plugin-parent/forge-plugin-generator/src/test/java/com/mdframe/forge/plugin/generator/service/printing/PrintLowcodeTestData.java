package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.*;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import java.util.*;

final class PrintLowcodeTestData {
    static final ObjectMapper JSON = new ObjectMapper();
    static final PrintActor ACTOR = new PrintActor(1L, 9L, 1L);
    static final String FIELDS = """
        [{"field":"id","columnName":"id","dataType":"bigint","primaryKey":true},
         {"field":"amount","columnName":"amount","dataType":"bigint","businessFieldType":"MONEY"},
         {"field":"status","columnName":"status","dataType":"varchar","dictType":"synthetic_status"},
         {"field":"phone","columnName":"phone","dataType":"varchar","sensitiveType":"PHONE"},
         {"field":"secret","columnName":"secret","dataType":"varchar","fieldStatus":"HIDDEN"},
         {"field":"password","columnName":"password","dataType":"varchar","sensitiveType":"PASSWORD"}]
        """;
    static String snapshot() {
        return """
            {"application":{"options":{"inAppBuilder":{"nodes":[
              {"id":"page_purchase","type":"page","objectRef":{"objectId":"3","objectCode":"purchase","configKey":"purchase"}}
            ],"pages":{"page_purchase":{}}}}},
             "objects":[{"objectId":"3","objectCode":"purchase","configKey":"purchase","publishedDesignVersionId":"30","tableName":"test_purchase"},
                        {"objectId":"4","objectCode":"item","configKey":"item","publishedDesignVersionId":"40","tableName":"test_item"}]}
            """;
    }
    static String model(String table) {
        return "{\"tableName\":\"" + table + "\",\"fields\":" + FIELDS + "}";
    }
    static String page(boolean children) {
        return children ? """
            {"primaryModelCode":"purchase","modelRefs":[
              {"modelCode":"purchase","primary":true,"tableName":"test_purchase","fields":[]},
              {"modelCode":"items","primary":false,"tableName":"test_item",
               "relations":[{"targetObjectCode":"purchase","sourceField":"purchase_id","targetField":"id"}],
               "fields":[{"field":"amount","formVisible":true},{"field":"phone","formVisible":true},
                         {"field":"status","formVisible":true},{"field":"secret","fieldStatus":"HIDDEN"}]}]}
            """ : "{\"modelRefs\":[]}";
    }
    static AiCrudConfigVersion version(long id, long configId, String key, boolean children) {
        var v = new AiCrudConfigVersion();
        v.setId(id); v.setTenantId(1L); v.setConfigId(configId); v.setConfigKey(key); v.setObjectCode(key);
        String model = model("test_" + key);
        if (key.equals("item")) {
            try {
                var root = JSON.readTree(model);
                ((com.fasterxml.jackson.databind.node.ArrayNode) root.path("fields")).addObject()
                        .put("field", "purchaseId").put("columnName", "purchase_id").put("dataType", "bigint");
                model = root.toString();
            } catch (Exception ex) { throw new AssertionError(ex); }
        }
        v.setModelSchema(model); v.setPageSchema(page(children));
        v.setPrimaryKeyField("id"); v.setPrimaryKeyColumn("id"); v.setPrimaryKeyType("LONG");
        v.setPublishSnapshot("{\"tableName\":\"test_" + key + "\",\"layoutType\":\"crud\","
                + "\"dictConfig\":null,\"transConfig\":{\"status\":{\"dictType\":\"synthetic_status\"}},"
                + "\"encryptConfig\":null,\"desensitizeConfig\":{\"phone\":{\"type\":\"PHONE\"}}}");
        return v;
    }
    static AiBusinessObjectDesignVersion design(long id, long objectId, long configId, long crudId, String key) {
        var d = new AiBusinessObjectDesignVersion();
        d.setId(id); d.setTenantId(1L); d.setObjectId(objectId); d.setConfigId(configId);
        d.setCrudConfigVersionId(crudId); d.setConfigKey(key); d.setObjectCode(key); d.setPublishStatus("PUBLISHED");
        return d;
    }
}

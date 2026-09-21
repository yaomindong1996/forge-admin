package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.dto.lowcode.*;
import com.mdframe.forge.plugin.generator.mapper.*;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/** 显式两条路径：设计目录读取草稿，运行目录只读取应用清单指定的不可变版本。 */
@Component
@RequiredArgsConstructor
public class PrintMetadataResolver {
    private final ObjectMapper json;
    private final LowcodePrintSourceResolver sources;
    private final BusinessApplicationMapper applications;
    private final BusinessApplicationObjectMapper applicationObjects;
    private final AiCrudConfigMapper configs;
    private final BusinessObjectDesignVersionMapper designs;
    private final AiCrudConfigVersionMapper versions;

    public record Model(AiCrudConfig config, LowcodeModelSchema schema, LowcodePageSchema page) { }
    public record Child(String key, Model model, String mainColumn, String childColumn,
                        List<Map<String, Object>> fields) { }
    public record Metadata(Model main, List<Child> children) { }

    public JsonNode parse(String text) {
        try {
            var node = json.readTree(text);
            if (node == null || !node.isObject()) {
                throw invalid("snapshot");
            }
            return node;
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw invalid("snapshot");
        }
    }

    public Metadata draft(PrintActor actor, PrintSourceRequest source) {
        var app = applications.selectEntityById(actor.tenantId(), source.applicationId());
        if (app == null) {
            throw PrintFailure.missing();
        }
        var root = json.createObjectNode();
        root.putObject("application").set("options", parse(app.getOptions()));
        root.set("objects", json.valueToTree(applicationObjects.selectByApplicationId(actor.tenantId(), source.applicationId())));
        return resolve(actor, source, root, false);
    }

    public Metadata candidate(PrintActor actor, PrintSourceRequest source, JsonNode snapshot) {
        return resolve(actor, source, snapshot, false);
    }

    public Metadata published(PrintActor actor, PrintSourceRequest source, JsonNode snapshot) {
        return resolve(actor, source, snapshot, true);
    }

    public void assertRuntimeEnabled(PrintActor actor, Metadata metadata) {
        List<Model> models = new ArrayList<>();
        models.add(metadata.main());
        metadata.children().forEach(child -> models.add(child.model()));
        for (Model model : models) {
            if (configs.countActiveRuntimeConfig(actor.tenantId(), model.config().getId(), model.config().getObjectCode()) != 1) {
                throw PrintFailure.of(409, "PRINT_OBJECT_DISABLED",
                        "业务对象未启用或运行配置不可用：" + model.config().getObjectCode());
            }
        }
    }

    private Metadata resolve(PrintActor actor, PrintSourceRequest source, JsonNode snapshot, boolean published) {
        var object = sources.object(snapshot, source);
        Model main = model(actor, object, published);
        List<Child> children = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (var ref : main.page().getModelRefs()) {
            if (Boolean.TRUE.equals(ref.getPrimary()) || Objects.equals(ref.getModelCode(), main.page().getPrimaryModelCode())) {
                continue;
            }
            if (ref.getModelCode() == null || !ref.getModelCode().matches("[A-Za-z_][A-Za-z0-9_]*") || !keys.add(ref.getModelCode())) {
                throw invalid("children");
            }
            List<Map<String, Object>> readableFields = childFields(main, ref);
            if (readableFields.isEmpty()) {
                continue;
            }
            // 子表必须拥有应用版本固定的完整 CRUD 配置，禁止用残缺的 modelRef 拼出脱敏/公式规则。
            List<Model> candidates = new ArrayList<>();
            for (JsonNode candidate : snapshot.path("objects")) {
                if (candidate == object || !Objects.equals(ref.getTableName(), candidate.path("tableName").asText())) {
                    continue;
                }
                Model child = model(actor, candidate, published);
                if (Objects.equals(child.config().getTableName(), ref.getTableName())) {
                    candidates.add(child);
                }
            }
            if (candidates.size() != 1) {
                throw invalid("children." + ref.getModelCode());
            }
            Model child = candidates.get(0);
            List<String[]> relations = new ArrayList<>();
            for (var relation : ref.getRelations()) {
                if (Objects.equals(relation.getTargetObjectCode(), main.page().getPrimaryModelCode())
                        || Objects.equals(relation.getTargetObjectCode(), source.objectCode())) {
                    relations.add(new String[]{relation.getTargetField(), relation.getSourceField()});
                }
            }
            if (relations.isEmpty()) {
                for (var relation : main.schema().getRelations()) {
                    if (Objects.equals(relation.getTargetObjectCode(), ref.getModelCode())
                            || Objects.equals(relation.getTargetObjectCode(), child.config().getObjectCode())) {
                        relations.add(new String[]{relation.getSourceField(), relation.getTargetField()});
                    }
                }
            }
            if (main.config().getOptions() != null && !main.config().getOptions().isBlank()) {
                for (JsonNode compiled : parse(main.config().getOptions()).path("masterDetailConfig").path("children")) {
                    if (ref.getModelCode().equals(compiled.path("modelCode").asText())) {
                        if (!Objects.equals(ref.getTableName(), compiled.path("tableName").asText())) {
                            throw invalid("children." + ref.getModelCode() + ".table");
                        }
                        relations.clear();
                        relations.add(new String[]{compiled.path("targetField").asText(), compiled.path("sourceField").asText()});
                    }
                }
            }
            if (relations.size() != 1) {
                throw invalid("children." + ref.getModelCode() + ".relation");
            }
            String mainColumn = column(main, relations.get(0)[0]);
            String childColumn = column(child, relations.get(0)[1]);
            if (childColumn.equals(child.config().getPrimaryKeyColumn())
                    || Set.of("tenant_id", "del_flag", "create_by", "update_by").contains(childColumn)) {
                throw invalid("children." + ref.getModelCode() + ".relation");
            }
            children.add(new Child(ref.getModelCode(), child, mainColumn, childColumn, readableFields));
        }
        return new Metadata(main, List.copyOf(children));
    }

    private List<Map<String, Object>> childFields(Model main, LowcodePageModelRef ref) {
        Set<String> selected = new HashSet<>();
        boolean constrained = false;
        if (main.config().getOptions() != null && !main.config().getOptions().isBlank()) {
            var compiled = parse(main.config().getOptions()).path("masterDetailConfig").path("children");
            if (compiled.isArray()) {
                constrained = true;
                for (JsonNode item : compiled) {
                    if (ref.getModelCode().equals(item.path("modelCode").asText())
                            && (item.path("showInDetail").asBoolean(true) || item.path("showInEdit").asBoolean(true))) {
                        item.path("fields").forEach(field -> selected.add(
                                field.path("sourceField").asText(field.path("field").asText())));
                    }
                }
            }
        }
        if (!constrained && ref.getProps() != null && ref.getProps().get("childFieldCodes") instanceof List<?> codes
                && !codes.isEmpty()) {
            constrained = true;
            codes.forEach(code -> selected.add(String.valueOf(code)));
        }
        if (!constrained) {
            Set<String> pageRefs = new HashSet<>();
            main.page().getZones().stream().filter(zone -> !Boolean.FALSE.equals(zone.getEnabled()))
                    .forEach(zone -> pageRefs.addAll(zone.getFieldRefs()));
            for (var field : ref.getFields()) {
                String name = Objects.toString(field.getOrDefault("sourceField", field.get("field")), "");
                String alias = Objects.toString(field.getOrDefault("fieldRef", ref.getModelCode() + "__" + name));
                if (pageRefs.contains(alias)) {
                    selected.add(name);
                }
            }
            constrained = !selected.isEmpty();
        }
        if (!constrained) {
            return List.copyOf(ref.getFields());
        }
        return ref.getFields().stream().filter(field -> selected.contains(
                Objects.toString(field.getOrDefault("sourceField", field.get("field")), ""))).toList();
    }

    private String column(Model model, String field) {
        if (field != null && field.equals(model.config().getPrimaryKeyField())) {
            return model.config().getPrimaryKeyColumn();
        }
        return model.schema().getFields().stream()
                .filter(item -> field != null && (field.equals(item.getField()) || field.equals(item.getColumnName())))
                .map(LowcodeFieldSchema::getColumnName).filter(Objects::nonNull).findFirst()
                .filter(value -> value.matches("[A-Za-z_][A-Za-z0-9_]*"))
                .orElseThrow(() -> invalid("relation." + field));
    }

    private Model model(PrintActor actor, JsonNode object, boolean published) {
        String key = object.path("configKey").asText();
        AiCrudConfig config;
        if (published) {
            long objectId = positiveId(object.path("objectId"));
            long designId = positiveId(object.path("publishedDesignVersionId"));
            var design = designs.selectVersionById(actor.tenantId(), objectId, designId);
            if (design == null || !BusinessObjectDesignStatus.PUBLISHED.matches(design.getPublishStatus())
                    || design.getCrudConfigVersionId() == null
                    || !key.equals(design.getConfigKey())
                    || !object.path("objectCode").asText().equals(design.getObjectCode())) {
                throw invalid(key);
            }
            var version = versions.selectVersionById(actor.tenantId(), design.getConfigId(), design.getCrudConfigVersionId());
            if (version == null || !key.equals(version.getConfigKey())
                    || !object.path("objectCode").asText().equals(version.getObjectCode())) {
                throw invalid(key);
            }
            config = new AiCrudConfig();
            BeanUtils.copyProperties(version, config);
            config.setId(version.getConfigId());
            config.setTenantId(actor.tenantId());
            config.setMode("CONFIG");
            config.setBuildMode("LOWCODE");
            var saved = parse(version.getPublishSnapshot());
            config.setTableName(required(saved, "tableName"));
            config.setLayoutType(required(saved, "layoutType"));
            config.setDictConfig(storedJson(saved, "dictConfig"));
            config.setTransConfig(storedJson(saved, "transConfig"));
            config.setEncryptConfig(storedJson(saved, "encryptConfig"));
            config.setDesensitizeConfig(storedJson(saved, "desensitizeConfig"));
        } else {
            config = configs.selectByConfigKey(actor.tenantId(), key);
            if (config == null || !"LOWCODE".equals(config.getBuildMode())) {
                throw invalid(key);
            }
        }
        try {
            var schema = json.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            var page = json.readValue(config.getPageSchema(), LowcodePageSchema.class);
            if (schema == null || page == null || schema.getFields() == null || page.getModelRefs() == null) {
                throw invalid(key);
            }
            if (config.getDesensitizeConfig() != null && !config.getDesensitizeConfig().isBlank()) {
                var masks = parse(config.getDesensitizeConfig());
                for (var field : schema.getFields()) {
                    if (masks.has(field.getField())) {
                        String type = masks.path(field.getField()).path("type").asText();
                        if (type.isBlank() || "NONE".equals(type)) {
                            throw invalid("mask." + field.getField());
                        }
                        field.setSensitiveType(type);
                    }
                }
            }
            return new Model(config, schema, page);
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw invalid(key);
        }
    }

    private String storedJson(JsonNode node, String key) {
        // 历史版本字段缺失不能静默使用当前草稿的解密或脱敏策略。
        if (!node.has(key)) {
            throw invalid(key);
        }
        JsonNode value = node.get(key);
        return value.isNull() ? null : value.isTextual() ? value.textValue() : value.toString();
    }

    private String required(JsonNode node, String key) {
        if (!node.path(key).isTextual() || node.path(key).asText().isBlank()) {
            throw invalid(key);
        }
        return node.get(key).textValue();
    }

    private long positiveId(JsonNode node) {
        try {
            long value = Long.parseLong(node.asText());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) { }
        throw invalid("versionId");
    }

    private RuntimeException invalid(String path) {
        return PrintFailure.field(path, "打印来源缺少完整的固定版本或关系配置：" + path);
    }
}

package com.mdframe.forge.plugin.data.printing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.service.DataDatasetRuntimeService;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetMetadataVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintParameterValidator;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintSource;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintBindingSelection;
import com.mdframe.forge.plugin.print.spi.PrintData;
import com.mdframe.forge.plugin.print.spi.PrintDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 已发布数据集到打印协议的受控适配器。
 * 表名、SQL、连接和租户身份均不接受客户端输入。
 */
@Component
@RequiredArgsConstructor
public class DatasetPrintDataProvider implements PrintDataProvider {

    private static final Set<String> MAPPING_KEYS = Set.of(
            "recordIdParam", "childrenKey", "maxRows");

    private final PrintBusinessSourceMapper sources;

    private final DataDatasetRuntimeService datasets;

    private final PrintParameterValidator parameters;

    private final ObjectMapper mapper;

    @Override
    public PrintSourceType sourceType() {
        return PrintSourceType.DATASET;
    }

    @Override
    public boolean supports(PrintSourceRequest source) {
        return source != null && source.sourceType() == PrintSourceType.DATASET
                && source.businessSourceId() != null;
    }

    @Override
    public void authorizeDesignSource(PrintActor actor, PrintSourceRequest request,
                                      PrintDesignAction action) {
        PrintBusinessSource source = requireSource(actor, request, false);
        DataDatasetMetadataVO metadata = datasets.metadata(source.getDatasetId());
        validateConfiguration(source, metadata);
    }

    @Override
    public PrintFieldCatalogVO catalog(AuthorizedPrintSource authorized) {
        if (authorized == null) {
            throw PrintFailure.denied();
        }
        PrintBusinessSource source = requireSource(
                authorized.actor(), authorized.source(), false);
        DataDatasetMetadataVO metadata = datasets.metadata(source.getDatasetId());
        SourceMapping mapping = validateConfiguration(source, metadata);
        return buildCatalog(metadata, mapping);
    }

    @Override
    public void validateDesignResources(AuthorizedPrintSource source, Set<String> fileIds) {
        if (fileIds != null && !fileIds.isEmpty()) {
            throw PrintFailure.of(400, "PRINT_RESOURCE_INVALID", "数据集打印来源不支持静态文件资源");
        }
    }

    @Override
    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        requireRuntimeScene(request.scene());
        PrintBusinessSource source = requireSource(actor, request.source(), true);
        PreparedQuery prepared = prepareQuery(source, request);
        DataDatasetQueryResultVO result = datasets.query(prepared.query());
        if (request.scene() == PrintScene.DETAIL && result.getSource().isEmpty()) {
            throw PrintFailure.missing();
        }
        return new AuthorizedPrintContext(
                actor, request, null, List.of(), prepared.catalog());
    }

    @Override
    public PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection) {
        PrintBusinessSource source = requireSource(
                context.actor(), context.record().source(), true);
        PreparedQuery prepared = prepareQuery(source, context.record());
        DataDatasetQueryResultVO result = datasets.query(prepared.query());
        return mapData(result, prepared.mapping());
    }

    @Override
    public void validateRuntimeResources(AuthorizedPrintContext context, Set<String> fileIds) {
        if (fileIds != null && !fileIds.isEmpty()) {
            throw PrintFailure.denied();
        }
    }

    private PrintBusinessSource requireSource(PrintActor actor, PrintSourceRequest requested,
                                              boolean enabled) {
        if (actor == null || !supports(requested)) {
            throw PrintFailure.denied();
        }
        PrintBusinessSource row = sources.selectScoped(
                actor.tenantId(), requested.businessSourceId());
        if (row == null || !PrintSourceType.DATASET.matches(row.getSourceType())
                || !requested.equals(PrintSourceRequest.from(row))) {
            throw PrintFailure.denied();
        }
        if (enabled && !EnableStatus.ENABLED.matches(row.getStatus())) {
            throw PrintFailure.missing();
        }
        return row;
    }

    private PreparedQuery prepareQuery(PrintBusinessSource source, PrintRecordRequest record) {
        DataDatasetMetadataVO metadata = datasets.metadata(source.getDatasetId());
        SourceMapping mapping = validateConfiguration(source, metadata);
        Map<String, Object> params = new LinkedHashMap<>(
                parameters.validate(source.getParameterSchemaJson(), record.params()));
        if (params.putIfAbsent(
                mapping.recordIdParam(), recordValue(metadata, mapping, record.recordId())) != null) {
            throw PrintFailure.of(400, "PRINT_PARAMETER_INVALID", "记录参数不能由调用方覆盖");
        }
        var query = new DataDatasetQueryDTO();
        query.setDatasetId(source.getDatasetId());
        query.setParams(params);
        query.setMaxRows(mapping.maxRows());
        query.setPageNum(1);
        query.setPageSize(mapping.maxRows());
        return new PreparedQuery(query, buildCatalog(metadata, mapping), mapping);
    }

    private SourceMapping validateConfiguration(PrintBusinessSource source,
                                                DataDatasetMetadataVO metadata) {
        SourceMapping mapping = mapping(source.getMappingJson());
        Map<String, String> datasetParams = datasetParams(metadata.getParamSchemaJson());
        if (!datasetParams.containsKey(mapping.recordIdParam())) {
            throw PrintFailure.of(
                    409, "PRINT_SOURCE_CONFIG_INVALID", "记录参数未在数据集参数协议中声明");
        }
        JsonNode sourceParams = object(source.getParameterSchemaJson(), "打印参数协议");
        sourceParams.fieldNames().forEachRemaining(name -> {
            if (!datasetParams.containsKey(name) || name.equals(mapping.recordIdParam())) {
                throw PrintFailure.of(
                        409, "PRINT_SOURCE_CONFIG_INVALID", "打印参数与数据集参数协议不一致：" + name);
            }
        });
        return mapping;
    }

    private SourceMapping mapping(String json) {
        JsonNode node = object(json, "数据映射");
        node.fieldNames().forEachRemaining(name -> {
            if (!MAPPING_KEYS.contains(name)) {
                throw PrintFailure.of(
                        409, "PRINT_SOURCE_CONFIG_INVALID", "数据映射包含未知配置：" + name);
            }
        });
        String recordIdParam = node.path("recordIdParam").asText("");
        String childrenKey = node.path("childrenKey").asText("");
        int maxRows = node.path("maxRows").asInt(1000);
        if (!recordIdParam.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                || !childrenKey.isEmpty()
                && !childrenKey.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                || maxRows < 1 || maxRows > 10000) {
            throw PrintFailure.of(409, "PRINT_SOURCE_CONFIG_INVALID", "数据映射配置无效");
        }
        return new SourceMapping(recordIdParam, childrenKey.isEmpty() ? null : childrenKey, maxRows);
    }

    private JsonNode object(String json, String label) {
        try {
            JsonNode node = json == null || json.isBlank()
                    ? mapper.createObjectNode()
                    : mapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw PrintFailure.of(409, "PRINT_SOURCE_CONFIG_INVALID", label + "必须是对象");
            }
            return node;
        } catch (java.io.IOException error) {
            throw PrintFailure.of(409, "PRINT_SOURCE_CONFIG_INVALID", label + "不是合法 JSON");
        }
    }

    private Map<String, String> datasetParams(String json) {
        try {
            JsonNode node = json == null || json.isBlank()
                    ? mapper.createArrayNode()
                    : mapper.readTree(json);
            if (!node.isArray()) {
                throw new IllegalArgumentException();
            }
            Map<String, String> result = new LinkedHashMap<>();
            for (JsonNode item : node) {
                String name = item.path("paramName").asText("");
                String type = item.path("dataType").asText("STRING");
                if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                        || result.putIfAbsent(name, type) != null) {
                    throw new IllegalArgumentException();
                }
            }
            return result;
        } catch (java.io.IOException | IllegalArgumentException error) {
            throw PrintFailure.of(
                    409, "PRINT_SOURCE_CONFIG_INVALID", "数据集参数协议无效，无法用于打印");
        }
    }

    private Object recordValue(DataDatasetMetadataVO metadata, SourceMapping mapping,
                               String recordId) {
        String type = datasetParams(metadata.getParamSchemaJson())
                .get(mapping.recordIdParam());
        try {
            return switch (type == null ? "STRING" : type.toUpperCase(Locale.ROOT)) {
                case "NUMBER" -> new BigDecimal(recordId);
                case "BOOLEAN" -> booleanRecordId(recordId);
                default -> recordId;
            };
        } catch (NumberFormatException error) {
            throw PrintFailure.of(400, "PRINT_PARAMETER_INVALID", "记录标识类型不匹配");
        }
    }

    private Boolean booleanRecordId(String recordId) {
        if (!"true".equalsIgnoreCase(recordId) && !"false".equalsIgnoreCase(recordId)) {
            throw PrintFailure.of(400, "PRINT_PARAMETER_INVALID", "记录标识类型不匹配");
        }
        return Boolean.valueOf(recordId);
    }

    private PrintFieldCatalogVO buildCatalog(DataDatasetMetadataVO metadata,
                                             SourceMapping mapping) {
        List<PrintFieldCatalogVO.Field> result = new ArrayList<>();
        List<DataDatasetFieldVO> fields = visibleFields(metadata);
        for (DataDatasetFieldVO field : fields) {
            result.add(new PrintFieldCatalogVO.Field(
                    "main." + field.getFieldName(), field.getFieldLabel(), fieldType(field)));
        }
        if (mapping.childrenKey() != null) {
            String root = "children." + mapping.childrenKey();
            result.add(new PrintFieldCatalogVO.Field(root, "明细", "COLLECTION"));
            for (DataDatasetFieldVO field : fields) {
                result.add(new PrintFieldCatalogVO.Field(
                        root + "." + field.getFieldName(), field.getFieldLabel(), fieldType(field)));
            }
        }
        return new PrintFieldCatalogVO(result);
    }

    private List<DataDatasetFieldVO> visibleFields(DataDatasetMetadataVO metadata) {
        if (metadata.getFields() == null) {
            return List.of();
        }
        return metadata.getFields().stream()
                .filter(field -> EnableStatus.ENABLED.matches(field.getDisplayEnabled()))
                .filter(field -> !"HIDDEN".equals(field.getSensitiveLevel()))
                .toList();
    }

    private String fieldType(DataDatasetFieldVO field) {
        return switch (String.valueOf(field.getDataType()).toUpperCase(Locale.ROOT)) {
            case "NUMBER" -> "NUMBER";
            case "DATE", "DATETIME" -> "DATE";
            case "BOOLEAN" -> "BOOLEAN";
            default -> "TEXT";
        };
    }

    private PrintData mapData(DataDatasetQueryResultVO result, SourceMapping mapping) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (result.getSource() != null) {
            result.getSource().forEach(row -> rows.add(scalarRow(row)));
        }
        Map<String, Object> main = rows.isEmpty() ? Map.of() : rows.get(0);
        Map<String, Object> children = mapping.childrenKey() == null
                ? Map.of()
                : Map.of(mapping.childrenKey(), rows);
        return new PrintData(main, children, Map.of());
    }

    /** 把 JDBC 时间等收敛成打印投影可接受的标量，避免 PRINT_DATA_LIMIT。 */
    private Map<String, Object> scalarRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (row == null) {
            return result;
        }
        row.forEach((key, value) -> result.put(key, scalarValue(value)));
        return result;
    }

    private Object scalarValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character character) {
            return String.valueOf(character);
        }
        if (value instanceof Enum<?> enumerated) {
            return enumerated.name();
        }
        if (value instanceof java.time.temporal.TemporalAccessor
                || value instanceof java.util.Date
                || value instanceof Calendar) {
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }

    private void requireRuntimeScene(PrintScene scene) {
        if (scene != PrintScene.LIST && scene != PrintScene.DETAIL) {
            throw PrintFailure.denied();
        }
    }

    private record SourceMapping(String recordIdParam, String childrenKey, int maxRows) {
    }

    private record PreparedQuery(DataDatasetQueryDTO query, PrintFieldCatalogVO catalog,
                                 SourceMapping mapping) {
    }
}

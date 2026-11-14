package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 业务绑定摘要跨库后处理：SQL 只做可移植粗过滤并选出 JSON 原文，
 * {@code codeApp} / 精确 modelKey / 入口路由在 Java 完成，后续换库不必再写 JSON 方言 SQL。
 */
public final class BusinessBindingSummarySupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BusinessBindingSummarySupport() {
    }

    /** LIKE 粗匹配针（含 flowModelKey 字样与 modelKey），精匹配见 {@link #retainMatchingModelKey}。 */
    public static String flowModelKeyLikeNeedle(String modelKey) {
        return "%\"flowModelKey\"%" + modelKey + "%";
    }

    public static List<BusinessBindingSummaryVO> retainMatchingModelKey(List<BusinessBindingSummaryVO> rows,
                                                                        String modelKey) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        String key = StringUtils.trimToNull(modelKey);
        if (key == null) {
            return List.of();
        }
        List<BusinessBindingSummaryVO> matched = new ArrayList<>(rows.size());
        for (BusinessBindingSummaryVO row : rows) {
            if (key.equals(StringUtils.trimToNull(row.getBindingKey()))
                    || key.equals(readText(row.getBindingConfigJson(), "flowModelKey"))) {
                if (StringUtils.isBlank(row.getFlowModelKey())) {
                    row.setFlowModelKey(key);
                }
                matched.add(row);
            }
        }
        return matched;
    }

    public static List<BusinessBindingSummaryVO> retainMatchingModelKeys(List<BusinessBindingSummaryVO> rows,
                                                                         List<String> modelKeys) {
        if (rows == null || rows.isEmpty() || modelKeys == null || modelKeys.isEmpty()) {
            return rows == null ? List.of() : List.of();
        }
        List<BusinessBindingSummaryVO> matched = new ArrayList<>();
        for (String modelKey : modelKeys) {
            String key = StringUtils.trimToNull(modelKey);
            if (key == null) {
                continue;
            }
            for (BusinessBindingSummaryVO row : rows) {
                if (key.equals(StringUtils.trimToNull(row.getBindingKey()))
                        || key.equals(readText(row.getBindingConfigJson(), "flowModelKey"))) {
                    BusinessBindingSummaryVO copy = copyRow(row);
                    copy.setFlowModelKey(key);
                    matched.add(copy);
                }
            }
        }
        return matched;
    }

    public static void enrichDisplay(BusinessBindingSummaryVO vo) {
        if (vo == null) {
            return;
        }
        boolean codeApp = isCodeAppTrue(vo.getOptionsJson()) || isCodeAppTrue(vo.getDesignerOptionsJson());
        vo.setCodeApp(codeApp);
        String objectCode = StringUtils.defaultString(vo.getObjectCode());
        String route = "/app-center/object/" + objectCode + "/designer?panel=flow-app";
        if (codeApp) {
            route = route + "&codeApp=1";
        }
        vo.setEntryRoute(route);
        vo.setBindingKey(null);
        vo.setBindingConfigJson(null);
        vo.setOptionsJson(null);
        vo.setDesignerOptionsJson(null);
    }

    public static void enrichDisplay(List<BusinessBindingSummaryVO> rows) {
        if (rows == null) {
            return;
        }
        rows.forEach(BusinessBindingSummarySupport::enrichDisplay);
    }

    private static BusinessBindingSummaryVO copyRow(BusinessBindingSummaryVO source) {
        BusinessBindingSummaryVO copy = new BusinessBindingSummaryVO();
        copy.setBindingId(source.getBindingId());
        copy.setFlowModelKey(source.getFlowModelKey());
        copy.setBindingName(source.getBindingName());
        copy.setObjectCode(source.getObjectCode());
        copy.setObjectName(source.getObjectName());
        copy.setApplicationId(source.getApplicationId());
        copy.setApplicationName(source.getApplicationName());
        copy.setSuiteName(source.getSuiteName());
        copy.setCodeApp(source.getCodeApp());
        copy.setEntryRoute(source.getEntryRoute());
        copy.setBindingKey(source.getBindingKey());
        copy.setBindingConfigJson(source.getBindingConfigJson());
        copy.setOptionsJson(source.getOptionsJson());
        copy.setDesignerOptionsJson(source.getDesignerOptionsJson());
        return copy;
    }

    private static boolean isCodeAppTrue(String json) {
        String value = readText(json, "codeApp");
        return "true".equalsIgnoreCase(value) || "1".equals(value) || Objects.equals(Boolean.TRUE.toString(), value);
    }

    private static String readText(String json, String field) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode node = root.get(field);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isBoolean()) {
                return Boolean.toString(node.booleanValue());
            }
            return StringUtils.trimToNull(node.asText());
        } catch (Exception ignored) {
            return null;
        }
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessRuntimeActionProjectionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRuntimeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将应用级运行交互叠加到对象级 CRUD 配置。
 *
 * <p>对象可以被多个应用复用，因而 flowInteraction 不能写回 AiCrudConfig 全局配置；
 * 只有携带业务应用入口上下文时，才从该应用的正式快照读取并叠加。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessApplicationRuntimeConfigOverlayService {

    private final BusinessAppMapper businessAppMapper;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessApplicationRuntimeService runtimeService;
    private final ObjectMapper objectMapper;
    private final BusinessProcessRuntimeActionProjectionService processActionProjectionService;

    public AiCrudConfigRenderVO overlay(String configKey, Long appId, AiCrudConfigRenderVO renderConfig) {
        return overlay(configKey, appId, null, renderConfig, false);
    }

    public AiCrudConfigRenderVO overlay(
            String configKey,
            Long appId,
            AiCrudConfigRenderVO renderConfig,
            boolean designPreview) {
        return overlay(configKey, appId, null, renderConfig, designPreview);
    }

    public AiCrudConfigRenderVO overlay(
            String configKey,
            Long appId,
            Long requestedApplicationId,
            AiCrudConfigRenderVO renderConfig,
            boolean designPreview) {
        if (renderConfig == null || StringUtils.isBlank(configKey)) {
            return renderConfig;
        }
        Long applicationId = resolveApplicationId(configKey, appId, requestedApplicationId);
        overlayFlowInteraction(configKey, appId, applicationId, renderConfig);
        overlayProcessActions(configKey, renderConfig, applicationId, designPreview);
        return renderConfig;
    }

    private Long resolveApplicationId(String configKey, Long appId, Long requestedApplicationId) {
        if (appId != null && appId > 0) {
            AiBusinessApp entry = businessAppMapper.selectEntityById(resolveTenantId(), appId);
            if (entry != null && StringUtils.equals(configKey, entry.getConfigKey())) {
                return entry.getApplicationId();
            }
        }
        if (requestedApplicationId == null || requestedApplicationId <= 0) {
            return null;
        }
        try {
            return applicationObjectService.containsConfigKey(requestedApplicationId, configKey)
                    ? requestedApplicationId : null;
        } catch (BusinessException ignored) {
            return null;
        }
    }

    private void overlayFlowInteraction(
            String configKey,
            Long appId,
            Long applicationId,
            AiCrudConfigRenderVO renderConfig) {
        if (applicationId == null) {
            return;
        }
        BusinessApplicationRuntimeVO runtime;
        try {
            runtime = runtimeService.runtimeById(applicationId);
        } catch (BusinessException ignored) {
            return;
        }
        if (runtime == null) {
            return;
        }
        if (appId != null && !containsEntry(runtime, appId)) {
            return;
        }
        Map<String, Object> applicationOptions = readMap(runtime.getApplication() == null
                ? null : runtime.getApplication().getOptions());
        Map<String, Object> builder = readMap(applicationOptions.get("inAppBuilder"));
        Object flowInteraction = builder.get("flowInteraction");
        if (flowInteraction == null) {
            return;
        }
        Map<String, Object> options = readMap(renderConfig.getOptions());
        options.put("flowInteraction", flowInteraction);
        renderConfig.setOptions(options);
    }

    private void overlayProcessActions(
            String configKey,
            AiCrudConfigRenderVO renderConfig,
            Long applicationId,
            boolean designPreview) {
        if (processActionProjectionService == null) {
            return;
        }
        List<Map<String, Object>> compiled;
        try {
            compiled = processActionProjectionService.compileForRender(
                    configKey,
                    renderConfig.getObjectCode(),
                    renderConfig.getModelSchema(),
                    applicationId,
                    designPreview);
        }
        catch (RuntimeException ex) {
            // 流程草稿投影失败时不能拖垮整个 designPreview render，否则前端会回落已发布操作列
            log.warn("[RuntimeConfigOverlay] 流程按钮投影失败, configKey={}, designPreview={}, err={}",
                    configKey, designPreview, ex.getMessage());
            return;
        }
        if (compiled.isEmpty()) {
            return;
        }
        List<Map<String, Object>> visible = compiled;
        Map<String, Object> options = readMap(renderConfig.getOptions());
        List<Map<String, Object>> rowActions = new ArrayList<>(readActionList(options.get("rowActions")));
        List<Map<String, Object>> toolbarActions = new ArrayList<>(readActionList(options.get("toolbarActions")));
        List<Map<String, Object>> detailActions = new ArrayList<>(readActionList(options.get("detailActions")));
        List<Map<String, Object>> formActions = new ArrayList<>(readActionList(options.get("formActions")));
        List<Map<String, Object>> runtimeActions = new ArrayList<>(readActionList(options.get("runtimeActions")));
        for (Map<String, Object> action : visible) {
            String position = StringUtils.defaultIfBlank(text(action.get("position")), "row");
            if ("toolbar".equals(position)) {
                mergeAction(toolbarActions, action);
            }
            else if ("detail".equals(position) || "form".equals(position)) {
                if ("form".equals(position)) {
                    mergeAction(formActions, action);
                }
                else {
                    mergeAction(detailActions, action);
                }
            }
            else {
                mergeAction(rowActions, action);
            }
            mergeAction(runtimeActions, action);
        }
        options.put("rowActions", rowActions);
        options.put("toolbarActions", toolbarActions);
        options.put("detailActions", detailActions);
        options.put("formActions", formActions);
        options.put("runtimeActions", runtimeActions);
        renderConfig.setOptions(options);
        mergeColumnActions(renderConfig, rowActions);
        if (StringUtils.isBlank(renderConfig.getObjectCode())) {
            Object objectCode = compiled.get(0).get("objectCode");
            if (objectCode != null) {
                renderConfig.setObjectCode(String.valueOf(objectCode));
            }
        }
    }

    private void mergeColumnActions(AiCrudConfigRenderVO renderConfig, List<Map<String, Object>> rowActions) {
        if (rowActions.isEmpty()) {
            return;
        }
        List<Map<String, Object>> columns = readActionList(renderConfig.getColumnsSchema());
        if (columns.isEmpty()) {
            return;
        }
        boolean merged = false;
        for (Map<String, Object> column : columns) {
            String key = firstText(column.get("key"), column.get("prop"), column.get("dataIndex"));
            if (!isActionColumn(key)) {
                continue;
            }
            List<Map<String, Object>> actions = new ArrayList<>(readActionList(column.get("actions")));
            for (Map<String, Object> action : rowActions) {
                mergeAction(actions, action);
            }
            column.put("actions", actions);
            column.put("width", Math.max(toInt(column.get("width"), 180), actions.size() * 58));
            merged = true;
        }
        if (merged) {
            renderConfig.setColumnsSchema(columns);
        }
    }

    private void mergeAction(List<Map<String, Object>> actions, Map<String, Object> action) {
        String key = text(action.get("key"));
        if (StringUtils.isBlank(key)) {
            actions.add(new LinkedHashMap<>(action));
            return;
        }
        for (int index = 0; index < actions.size(); index++) {
            if (key.equals(text(actions.get(index).get("key")))) {
                Map<String, Object> merged = new LinkedHashMap<>(actions.get(index));
                merged.putAll(action);
                actions.set(index, merged);
                return;
            }
        }
        actions.add(new LinkedHashMap<>(action));
    }

    private boolean containsEntry(BusinessApplicationRuntimeVO runtime, Long appId) {
        if (runtime == null || runtime.getEntries() == null) {
            return false;
        }
        return runtime.getEntries().stream()
                .anyMatch(entry -> entry != null && appId.equals(entry.getId()));
    }

    private Map<String, Object> readMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return objectMapper.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() { });
            } catch (Exception ignored) {
                return new LinkedHashMap<>();
            }
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readActionList(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> copied = new LinkedHashMap<>();
                    map.forEach((key, nested) -> copied.put(String.valueOf(key), nested));
                    result.add(copied);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private boolean isActionColumn(String key) {
        return List.of("actions", "action", "operations", "operation").contains(StringUtils.defaultString(key));
    }

    private String firstText(Object... values) {
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            String text = text(value);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        }
        return "";
    }

    private String text(Object value) {
        return value == null ? "" : StringUtils.trimToEmpty(String.valueOf(value));
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception ignored) {
            return 1L;
        }
    }
}

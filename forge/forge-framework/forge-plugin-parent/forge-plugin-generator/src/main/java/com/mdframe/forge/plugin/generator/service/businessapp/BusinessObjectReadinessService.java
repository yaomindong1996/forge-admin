package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessReadinessStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessReadinessItemVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务对象就绪度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessObjectReadinessService {

    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final AiCrudConfigMapper aiCrudConfigMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final BusinessTriggerMapper triggerMapper;

    /**
     * 查询业务对象就绪度
     *
     * @param objectId 业务对象 ID
     * @return 就绪度信息
     */
    public BusinessObjectReadinessVO readiness(Long objectId) {
        Long tenantId = resolveTenantId();

        // 查询业务对象
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        if (object == null) {
            throw new BusinessException("业务对象不存在");
        }

        BusinessObjectReadinessVO vo = new BusinessObjectReadinessVO();
        vo.setObjectId(object.getId());
        vo.setSuiteCode(object.getSuiteCode());
        vo.setObjectCode(object.getObjectCode());
        vo.setObjectName(object.getObjectName());

        List<BusinessReadinessItemVO> items = new ArrayList<>();
        int totalScore = 0;
        int maxScore = 0;

        // 1. 检查业务对象状态
        BusinessReadinessItemVO objectItem = checkObjectStatus(object, tenantId);
        items.add(objectItem);
        totalScore += getItemScore(objectItem.getStatus());
        maxScore += 100;

        // 2. 检查低代码模型
        BusinessReadinessItemVO modelItem = checkModelStatus(object, tenantId);
        items.add(modelItem);
        totalScore += getItemScore(modelItem.getStatus());
        maxScore += 100;

        // 3. 检查应用入口
        BusinessReadinessItemVO appItem = checkAppStatus(object, tenantId);
        items.add(appItem);
        totalScore += getItemScore(appItem.getStatus());
        maxScore += 100;

        // 4. 检查运行配置
        BusinessReadinessItemVO configItem = checkConfigStatus(object, tenantId);
        items.add(configItem);
        totalScore += getItemScore(configItem.getStatus());
        maxScore += 100;

        // 5. 检查导入导出
        BusinessReadinessItemVO importExportItem = checkImportExportStatus(object, tenantId);
        items.add(importExportItem);
        totalScore += getItemScore(importExportItem.getStatus());
        maxScore += 100;

        // 6. 检查对象关系
        BusinessReadinessItemVO relationItem = checkRelationStatus(object, tenantId);
        items.add(relationItem);
        totalScore += getItemScore(relationItem.getStatus());
        maxScore += 100;

        // 7. 检查能力挂接
        BusinessReadinessItemVO bindingItem = checkBindingStatus(object, tenantId);
        items.add(bindingItem);
        totalScore += getItemScore(bindingItem.getStatus());
        maxScore += 100;

        // 8. 检查单据闭环
        BusinessReadinessItemVO documentClosureItem = checkDocumentClosureStatus(object, tenantId);
        items.add(documentClosureItem);
        totalScore += getItemScore(documentClosureItem.getStatus());
        maxScore += 100;

        vo.setItems(items);
        vo.setScore(maxScore > 0 ? (totalScore * 100 / maxScore) : 0);

        // 计算整体状态
        String overallStatus = calculateOverallStatus(items);
        vo.setOverallStatus(overallStatus);

        // 设置下一步操作
        setNextAction(vo, items, overallStatus);

        return vo;
    }

    private BusinessReadinessItemVO checkObjectStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("OBJECT_STATUS");
        item.setItemName("业务对象状态");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在");
            item.setNextAction("CREATE_OBJECT");
            item.setNextActionLabel("创建业务对象");
        } else if (Integer.valueOf(0).equals(object.getStatus())) {
            item.setStatus(BusinessReadinessStatus.ERROR);
            item.setStatusLabel("已停用");
            item.setMessage("业务对象已停用");
            item.setNextAction("ENABLE_OBJECT");
            item.setNextActionLabel("启用业务对象");
        } else {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("正常");
            item.setMessage("业务对象已启用");
        }

        return item;
    }

    private BusinessReadinessItemVO checkModelStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("MODEL_STATUS");
        item.setItemName("低代码模型");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查模型");
            return item;
        }

        boolean hasModel = StringUtils.isNotBlank(object.getModelCode());
        if (hasModel) {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("已关联");
            item.setMessage("低代码模型已关联，可以支撑运行配置");
        } else {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未配置");
            item.setMessage("低代码模型未关联");
            item.setNextAction("CONFIGURE_MODEL");
            item.setNextActionLabel("配置模型");
        }

        return item;
    }

    private BusinessReadinessItemVO checkAppStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("APP_STATUS");
        item.setItemName("应用入口");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查应用入口");
            return item;
        }

        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        if (app == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("应用入口未创建");
            item.setNextAction("CREATE_APP_ENTRY");
            item.setNextActionLabel("生成应用入口");
        } else if (Integer.valueOf(0).equals(app.getStatus())) {
            item.setStatus(BusinessReadinessStatus.ERROR);
            item.setStatusLabel("已停用");
            item.setMessage("应用入口已停用");
            item.setNextAction("ENABLE_APP_ENTRY");
            item.setNextActionLabel("启用应用入口");
        } else {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("正常");
            item.setMessage("应用入口已启用");
            item.setNextActionUrl(StringUtils.defaultIfBlank(app.getEntryUrl(), "/ai/crud-page/" + app.getConfigKey()));
        }

        return item;
    }

    private BusinessReadinessItemVO checkConfigStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("CONFIG_STATUS");
        item.setItemName("运行配置");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查运行配置");
            return item;
        }

        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        if (app == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("应用入口不存在，无法检查运行配置");
            item.setNextAction("CREATE_APP_ENTRY");
            item.setNextActionLabel("生成应用入口");
            return item;
        }

        String configKey = app.getConfigKey();
        if (StringUtils.isBlank(configKey)) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未配置");
            item.setMessage("应用入口未配置 configKey");
            item.setNextAction("CONFIGURE_RUNTIME");
            item.setNextActionLabel("配置模型/布局/发布");
            return item;
        }

        AiCrudConfig config = aiCrudConfigMapper.selectByConfigKey(tenantId, configKey);
        if (config == null) {
            item.setStatus(BusinessReadinessStatus.ERROR);
            item.setStatusLabel("配置缺失");
            item.setMessage("运行配置不存在，请先发布应用");
            item.setNextAction("PUBLISH_APP");
            item.setNextActionLabel("发布应用");
        } else if ("1".equals(config.getStatus())) {
            item.setStatus(BusinessReadinessStatus.ERROR);
            item.setStatusLabel("已停用");
            item.setMessage("运行配置已停用，请先启用运行配置");
            item.setNextAction("ENABLE_RUNTIME");
            item.setNextActionLabel("启用运行配置");
        } else if (!"PUBLISHED".equals(config.getPublishStatus())) {
            item.setStatus(BusinessReadinessStatus.CONFIGURED);
            item.setStatusLabel("未发布");
            item.setMessage("运行配置未发布");
            item.setNextAction("PUBLISH_APP");
            item.setNextActionLabel("发布应用");
        } else {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("已发布");
            item.setMessage("运行配置已发布");
            item.setNextActionUrl("/ai/crud-page/" + configKey);
        }

        return item;
    }

    private BusinessReadinessItemVO checkImportExportStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("IMPORT_EXPORT_STATUS");
        item.setItemName("导入导出");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查导入导出");
            return item;
        }

        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        if (app == null || StringUtils.isBlank(app.getConfigKey())) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未配置");
            item.setMessage("运行配置不存在，无法启用导入导出");
            item.setNextAction("CONFIGURE_RUNTIME");
            item.setNextActionLabel("配置运行配置");
            return item;
        }

        AiCrudConfig config = aiCrudConfigMapper.selectByConfigKey(tenantId, app.getConfigKey());
        if (config == null || "1".equals(config.getStatus()) || !"PUBLISHED".equals(config.getPublishStatus())) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("不可用");
            item.setMessage("运行配置未发布或已停用，无法启用导入导出");
            item.setNextAction("PUBLISH_APP");
            item.setNextActionLabel("发布应用");
        } else {
            // 检查 options 中是否启用导入导出
            String options = config.getOptions();
            boolean importEnabled = optionEnabled(options, "importEnabled") || optionEnabled(options, "showImport");
            boolean exportEnabled = optionEnabled(options, "exportEnabled") || optionEnabled(options, "showExport");

            if (importEnabled || exportEnabled) {
                item.setStatus(BusinessReadinessStatus.RUNNABLE);
                item.setStatusLabel("已启用");
                item.setMessage("导入导出功能已启用");
            } else {
                item.setStatus(BusinessReadinessStatus.CONFIGURED);
                item.setStatusLabel("未启用");
                item.setMessage("导入导出功能未启用");
                item.setNextAction("ENABLE_IMPORT_EXPORT");
                item.setNextActionLabel("启用导入导出");
            }
        }

        return item;
    }

    private BusinessReadinessItemVO checkDocumentClosureStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("DOCUMENT_CLOSURE_STATUS");
        item.setItemName("单据闭环");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查单据闭环");
            return item;
        }

        BusinessDocumentConfigVO config = documentConfigService.getConfig(object.getId());
        if (!Boolean.TRUE.equals(config.getDocumentEnabled())) {
            item.setStatus(BusinessReadinessStatus.REGISTERED);
            item.setStatusLabel("未启用");
            item.setMessage("当前对象按普通 CRUD 运行，未启用单据闭环");
            item.setNextAction("CONFIGURE_DOCUMENT");
            item.setNextActionLabel("配置单据");
            item.setNextActionUrl("/app-center/object-designer/" + object.getObjectCode() + "?panel=document");
            return item;
        }
        if (StringUtils.isBlank(config.getStatusField())) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("缺状态字段");
            item.setMessage("单据模式已启用，但未配置状态字段");
            item.setNextAction("CONFIGURE_DOCUMENT");
            item.setNextActionLabel("配置单据状态");
            item.setNextActionUrl("/app-center/object-designer/" + object.getObjectCode() + "?panel=document");
            return item;
        }

        Map<String, Object> mainFlow = config.getMainFlowSummary();
        if (mainFlow == null || !Boolean.TRUE.equals(mainFlow.get("configured"))) {
            item.setStatus(BusinessReadinessStatus.CONFIGURED);
            item.setStatusLabel("缺主流程");
            item.setMessage("单据已配置，尚未绑定主流程");
            item.setNextAction("CONFIGURE_FLOW");
            item.setNextActionLabel("配置主流程");
            item.setNextActionUrl("/app-center/object-designer/" + object.getObjectCode() + "?panel=automation");
            return item;
        }
        if (!Boolean.TRUE.equals(mainFlow.get("complete"))) {
            item.setStatus(BusinessReadinessStatus.CONFIGURED);
            item.setStatusLabel("流程待完善");
            item.setMessage("主流程仍有缺口: " + summarizeGaps(mainFlow.get("gaps")));
            item.setNextAction("CONFIGURE_FLOW");
            item.setNextActionLabel("完善流程配置");
            item.setNextActionUrl("/app-center/object-designer/" + object.getObjectCode() + "?panel=automation");
            return item;
        }
        Object startModeValue = mainFlow.get("startMode");
        String startMode = StringUtils.defaultIfBlank(startModeValue == null ? null : String.valueOf(startModeValue), "MANUAL");
        if (requiresTrigger(startMode)) {
            Long triggerCount = triggerMapper.countActiveByObjectAndAction(tenantId, object.getObjectCode(), "START_FLOW");
            if (triggerCount == null || triggerCount <= 0) {
                item.setStatus(BusinessReadinessStatus.CONFIGURED);
                item.setStatusLabel("缺触发器");
                item.setMessage("发起方式包含触发器，但未配置启用的发起主流程触发器");
                item.setNextAction("CONFIGURE_TRIGGER");
                item.setNextActionLabel("配置触发器");
                item.setNextActionUrl("/app-center/trigger?objectCode=" + object.getObjectCode());
                return item;
            }
        }
        item.setStatus(BusinessReadinessStatus.RUNNABLE);
        item.setStatusLabel("已闭环");
        item.setMessage("单据设置、主流程、发起方式和触发器配置已具备闭环能力");
        return item;
    }

    private BusinessReadinessItemVO checkRelationStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("RELATION_STATUS");
        item.setItemName("对象关系");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查关系");
            return item;
        }

        // 查询对象关系数量
        Long relationCount = businessObjectMapper.countRelationsByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        if (relationCount != null && relationCount > 0) {
            item.setStatus(BusinessReadinessStatus.RUNNABLE);
            item.setStatusLabel("已配置");
            item.setMessage("已配置 " + relationCount + " 个对象关系");
        } else {
            item.setStatus(BusinessReadinessStatus.REGISTERED);
            item.setStatusLabel("未配置");
            item.setMessage("对象关系未配置");
            item.setNextAction("CONFIGURE_RELATIONS");
            item.setNextActionLabel("配置对象关系");
        }

        return item;
    }

    private BusinessReadinessItemVO checkBindingStatus(AiBusinessObject object, Long tenantId) {
        BusinessReadinessItemVO item = new BusinessReadinessItemVO();
        item.setItemCode("BINDING_STATUS");
        item.setItemName("能力挂接");

        if (object == null) {
            item.setStatus(BusinessReadinessStatus.MISSING);
            item.setStatusLabel("未创建");
            item.setMessage("业务对象不存在，无法检查能力挂接");
            return item;
        }

        // 查询能力挂接数量
        Long bindingCount = businessObjectMapper.countBindingsByObject(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        if (bindingCount != null && bindingCount > 0) {
            item.setStatus(BusinessReadinessStatus.CONFIGURED);
            item.setStatusLabel("已配置");
            item.setMessage("已配置 " + bindingCount + " 个能力挂接");
        } else {
            item.setStatus(BusinessReadinessStatus.REGISTERED);
            item.setStatusLabel("未配置");
            item.setMessage("能力挂接未配置");
            item.setNextAction("CONFIGURE_BINDINGS");
            item.setNextActionLabel("配置能力挂接");
        }

        return item;
    }

    private int getItemScore(String status) {
        if (BusinessReadinessStatus.RUNNABLE.equals(status)) {
            return 100;
        } else if (BusinessReadinessStatus.CONFIGURED.equals(status)) {
            return 75;
        } else if (BusinessReadinessStatus.REGISTERED.equals(status)) {
            return 50;
        } else if (BusinessReadinessStatus.ERROR.equals(status)) {
            return 25;
        } else {
            return 0;
        }
    }

    private String calculateOverallStatus(List<BusinessReadinessItemVO> items) {
        boolean hasMissing = false;
        boolean hasError = false;
        boolean hasConfigured = false;
        boolean hasRegistered = false;
        boolean allRunnable = true;

        for (BusinessReadinessItemVO item : items) {
            String status = item.getStatus();
            if (BusinessReadinessStatus.MISSING.equals(status)) {
                hasMissing = true;
                allRunnable = false;
            } else if (BusinessReadinessStatus.ERROR.equals(status)) {
                hasError = true;
                allRunnable = false;
            } else if (BusinessReadinessStatus.CONFIGURED.equals(status)) {
                hasConfigured = true;
                allRunnable = false;
            } else if (BusinessReadinessStatus.REGISTERED.equals(status)) {
                hasRegistered = true;
                allRunnable = false;
            }
        }

        if (runtimeItemsReady(items)) {
            return BusinessReadinessStatus.RUNNABLE;
        } else if (allRunnable) {
            return BusinessReadinessStatus.RUNNABLE;
        } else if (hasMissing) {
            return BusinessReadinessStatus.MISSING;
        } else if (hasError) {
            return BusinessReadinessStatus.ERROR;
        } else if (hasConfigured) {
            return BusinessReadinessStatus.CONFIGURED;
        } else if (hasRegistered) {
            return BusinessReadinessStatus.REGISTERED;
        } else {
            return BusinessReadinessStatus.MISSING;
        }
    }

    private boolean runtimeItemsReady(List<BusinessReadinessItemVO> items) {
        for (BusinessReadinessItemVO item : items) {
            if (!isRuntimeBlockingItem(item.getItemCode())) {
                continue;
            }
            String status = item.getStatus();
            if ("MODEL_STATUS".equals(item.getItemCode())) {
                if (!BusinessReadinessStatus.RUNNABLE.equals(status)
                        && !BusinessReadinessStatus.CONFIGURED.equals(status)) {
                    return false;
                }
                continue;
            }
            if (!BusinessReadinessStatus.RUNNABLE.equals(status)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRuntimeBlockingItem(String itemCode) {
        return "OBJECT_STATUS".equals(itemCode)
                || "MODEL_STATUS".equals(itemCode)
                || "APP_STATUS".equals(itemCode)
                || "CONFIG_STATUS".equals(itemCode)
                || "IMPORT_EXPORT_STATUS".equals(itemCode);
    }

    private boolean requiresTrigger(String startMode) {
        String normalized = StringUtils.defaultIfBlank(startMode, "MANUAL").trim().toUpperCase();
        return "TRIGGER".equals(normalized)
                || "BOTH".equals(normalized)
                || "MANUAL_AND_TRIGGER".equals(normalized)
                || "MANUAL_TRIGGER".equals(normalized);
    }

    private String summarizeGaps(Object gaps) {
        if (gaps instanceof List<?> list && !list.isEmpty()) {
            return String.join("、", list.stream()
                    .map(String::valueOf)
                    .filter(StringUtils::isNotBlank)
                    .toList());
        }
        return "变量映射、发起方式或状态回写未完善";
    }

    private boolean optionEnabled(String options, String key) {
        if (StringUtils.isBlank(options) || StringUtils.isBlank(key)) {
            return false;
        }
        return options.replace(" ", "").contains("\"" + key + "\":true");
    }

    private void setNextAction(BusinessObjectReadinessVO vo, List<BusinessReadinessItemVO> items, String overallStatus) {
        if (BusinessReadinessStatus.RUNNABLE.equals(overallStatus)) {
            vo.setNextAction("OPEN_RUNTIME");
            vo.setNextActionLabel("打开业务入口");
            vo.setNextActionUrl("/ai/crud-page/" + getConfigKey(items));
        } else {
            // 找到第一个需要处理的项
            for (BusinessReadinessItemVO item : items) {
                if (item.getNextAction() != null) {
                    vo.setNextAction(item.getNextAction());
                    vo.setNextActionLabel(item.getNextActionLabel());
                    vo.setNextActionUrl(item.getNextActionUrl());
                    break;
                }
            }
        }
    }

    private String getConfigKey(List<BusinessReadinessItemVO> items) {
        // 从配置状态项中获取 configKey
        for (BusinessReadinessItemVO item : items) {
            if ("CONFIG_STATUS".equals(item.getItemCode()) && 
                BusinessReadinessStatus.RUNNABLE.equals(item.getStatus())) {
                String url = item.getNextActionUrl();
                if (StringUtils.isNotBlank(url) && url.contains("/ai/crud-page/")) {
                    return StringUtils.substringAfter(url, "/ai/crud-page/");
                }
            }
        }
        return "";
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}

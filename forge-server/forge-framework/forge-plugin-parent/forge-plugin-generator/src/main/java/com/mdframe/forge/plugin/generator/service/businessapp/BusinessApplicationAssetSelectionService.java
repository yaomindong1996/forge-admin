package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析用户选择并自动补齐主对象、入口对象和扩展对象依赖。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationAssetSelectionService {

    private static final Set<String> DEFAULT_PUBLISHABLE_EXTENSION_STATUSES = Set.of(
            BusinessExtensionStatus.TESTED,
            BusinessExtensionStatus.ENABLED,
            BusinessExtensionStatus.DISABLED
    );

    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessAppMapper businessAppMapper;
    private final BusinessExtensionMapper extensionMapper;

    ResolvedSelection resolveContext(Long applicationId, BusinessApplicationPublishDTO dto) {
        List<BusinessApplicationObjectVO> objects = applicationObjectService.list(applicationId);
        List<AiBusinessApp> entries = businessAppMapper.selectByApplicationId(resolveTenantId(), applicationId);
        List<AiBusinessExtension> extensions = extensionMapper.selectByApplicationId(resolveTenantId(), applicationId);
        Map<Long, BusinessApplicationObjectVO> objectMap = objects.stream()
                .collect(Collectors.toMap(BusinessApplicationObjectVO::getObjectId, Function.identity()));
        Map<Long, AiBusinessApp> entryMap = entries.stream()
                .collect(Collectors.toMap(AiBusinessApp::getId, Function.identity()));
        Map<Long, AiBusinessExtension> extensionMap = extensions.stream()
                .collect(Collectors.toMap(AiBusinessExtension::getId, Function.identity()));

        BusinessApplicationAssetSelectionVO selection = new BusinessApplicationAssetSelectionVO();
        Set<Long> objectIds = initialSelection(dto == null ? null : dto.getSelectedObjectIds(), objectMap.keySet());
        List<Long> requestedEntryIds = dto == null ? null : dto.getSelectedEntryIds();
        Set<Long> entryIds = requestedEntryIds == null
                ? defaultPublishableEntryIds(entries)
                : requestedEntryIds.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> requestedExtensionIds = dto == null ? null : dto.getSelectedExtensionIds();
        Set<Long> extensionIds = requestedExtensionIds == null || requestedExtensionIds.isEmpty()
                ? defaultPublishableExtensionIds(extensions)
                : requestedExtensionIds.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validateOwned("业务对象", objectIds, objectMap.keySet());
        validateOwned("访问入口", entryIds, entryMap.keySet());
        validateOwned("业务扩展", extensionIds, extensionMap.keySet());
        long skippedDraftCount = extensions.stream()
                .filter(extension -> BusinessExtensionStatus.DRAFT.equals(extension.getStatus()))
                .filter(extension -> !extensionIds.contains(extension.getId()))
                .count();
        if (skippedDraftCount > 0L) {
            selection.getDependencyMessages().add(skippedDraftCount
                    + " 个未测试扩展仍保留为草稿，已自动跳过本次发布");
        }

        objects.stream()
                .filter(item -> BusinessApplicationObjectRole.PRIMARY.equals(item.getObjectRole()))
                .map(BusinessApplicationObjectVO::getObjectId)
                .findFirst()
                .ifPresent(primaryId -> autoIncludeObject(primaryId, objectIds, selection, "主对象是应用发布的必需依赖"));
        for (Long entryId : entryIds) {
            AiBusinessApp entry = entryMap.get(entryId);
            if (entry != null && entry.getObjectCode() != null) {
                objects.stream()
                        .filter(item -> entry.getObjectCode().equals(item.getObjectCode()))
                        .map(BusinessApplicationObjectVO::getObjectId)
                        .findFirst()
                        .ifPresent(objectId -> autoIncludeObject(objectId, objectIds, selection,
                                "访问入口“" + entry.getAppName() + "”依赖对应业务对象"));
            }
        }
        for (Long extensionId : extensionIds) {
            AiBusinessExtension extension = extensionMap.get(extensionId);
            if (extension != null && extension.getObjectId() != null) {
                autoIncludeObject(extension.getObjectId(), objectIds, selection,
                        "扩展“" + extension.getExtensionName() + "”依赖对应业务对象");
            }
            if (extension != null && extension.getEntryId() != null && !entryIds.contains(extension.getEntryId())) {
                entryIds.add(extension.getEntryId());
                selection.getDependencyMessages().add("扩展“" + extension.getExtensionName() + "”自动补齐页面入口");
            }
        }

        selection.setObjectIds(List.copyOf(objectIds));
        selection.setEntryIds(List.copyOf(entryIds));
        selection.setExtensionIds(List.copyOf(extensionIds));
        selection.setIncludeAutomation(dto == null || !Boolean.FALSE.equals(dto.getIncludeAutomation()));
        return new ResolvedSelection(selection, List.copyOf(objects), List.copyOf(entries), List.copyOf(extensions));
    }

    private Set<Long> initialSelection(List<Long> requested, Set<Long> allIds) {
        if (requested == null || requested.isEmpty()) {
            return new LinkedHashSet<>(allIds);
        }
        return requested.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static Set<Long> defaultPublishableExtensionIds(List<AiBusinessExtension> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return extensions.stream()
                .filter(extension -> DEFAULT_PUBLISHABLE_EXTENSION_STATUSES.contains(extension.getStatus()))
                .map(AiBusinessExtension::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static Set<Long> defaultPublishableEntryIds(List<AiBusinessApp> entries) {
        if (entries == null || entries.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return entries.stream()
                .filter(entry -> Integer.valueOf(1).equals(entry.getStatus()))
                .filter(entry -> !"RUNTIME".equalsIgnoreCase(entry.getEntryMode())
                        || org.apache.commons.lang3.StringUtils.isNotBlank(entry.getConfigKey()))
                .map(AiBusinessApp::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateOwned(String assetName, Set<Long> selected, Set<Long> owned) {
        if (!owned.containsAll(selected)) {
            throw new BusinessException("发布选择中包含不属于当前应用的" + assetName);
        }
    }

    private void autoIncludeObject(Long objectId, Set<Long> objectIds,
                                   BusinessApplicationAssetSelectionVO selection, String message) {
        if (objectId != null && objectIds.add(objectId)) {
            selection.getAutoIncludedObjectIds().add(objectId);
            selection.getDependencyMessages().add(message);
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    record ResolvedSelection(BusinessApplicationAssetSelectionVO selection,
                             List<BusinessApplicationObjectVO> objects,
                             List<AiBusinessApp> entries,
                             List<AiBusinessExtension> extensions) {
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 应用内业务对象编排服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationObjectService
        extends ServiceImpl<BusinessApplicationObjectMapper, AiBusinessApplicationObject> {

    private final BusinessApplicationService applicationService;
    private final BusinessObjectService objectService;

    public List<BusinessApplicationObjectVO> list(Long applicationId) {
        applicationService.requireEntity(applicationId);
        List<BusinessApplicationObjectVO> objects = baseMapper.selectByApplicationId(resolveTenantId(), applicationId);
        if (objects != null) {
            objects.forEach(this::enrichDatabaseSummary);
        }
        return objects;
    }

    /**
     * 页面从表单创建的业务对象跟着页面走。页面已从应用导航删除，且没有其它页面仍引用时，
     * 解除应用关联，并软删无人引用的页面托管对象，释放 object_code / model_code 以便同编码重建。
     * 不删除物理业务数据表（用户可自行 DROP；重建时会按新设计再建表）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void detachOrphanPageFormObjects(Long applicationId) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        JSONObject builder = readInAppBuilder(application.getOptions());
        if (builder == null) {
            return;
        }
        Set<String> pageIds = readPageIds(builder);
        Set<String> referencedObjectIds = new LinkedHashSet<>();
        collectReferencedObjectIds(builder, referencedObjectIds);
        List<BusinessApplicationObjectVO> current = list(applicationId);
        if (current == null || current.isEmpty()) {
            return;
        }
        List<BusinessApplicationObjectDTO> kept = new ArrayList<>();
        List<Long> orphanObjectIds = new ArrayList<>();
        boolean changed = false;
        for (BusinessApplicationObjectVO association : current) {
            if (association == null || association.getObjectId() == null) {
                continue;
            }
            if (isOrphanPageFormObject(association, pageIds, referencedObjectIds)) {
                changed = true;
                orphanObjectIds.add(association.getObjectId());
                continue;
            }
            BusinessApplicationObjectDTO item = new BusinessApplicationObjectDTO();
            item.setObjectId(association.getObjectId());
            item.setObjectRole(association.getObjectRole());
            item.setSortOrder(association.getSortOrder());
            item.setOptions(association.getOptions());
            kept.add(item);
        }
        if (changed) {
            replace(applicationId, kept);
            for (Long orphanObjectId : orphanObjectIds) {
                objectService.reclaimUnusedPageFormObjectById(orphanObjectId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void replace(Long applicationId, List<BusinessApplicationObjectDTO> objects) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        List<BusinessApplicationObjectDTO> normalized = objects == null ? List.of() : objects;
        List<AiBusinessApplicationObject> entities = validateAndConvert(application, normalized);
        baseMapper.logicDeleteByApplicationId(resolveTenantId(), applicationId);
        if (!entities.isEmpty()) {
            baseMapper.insertBatch(entities);
        }
        applicationService.markCompositionChanged(applicationId);
    }

    public Set<Long> listAffectedApplicationIds(Long objectId) {
        if (objectId == null) {
            throw new BusinessException("业务对象ID不能为空");
        }
        objectService.requireEntity(objectId);
        return new LinkedHashSet<>(baseMapper.selectApplicationIdsByObjectId(resolveTenantId(), objectId));
    }

    private List<AiBusinessApplicationObject> validateAndConvert(
            AiBusinessApplication application, List<BusinessApplicationObjectDTO> objects) {
        Set<Long> objectIds = new HashSet<>();
        int primaryCount = 0;
        List<AiBusinessApplicationObject> entities = new ArrayList<>(objects.size());
        for (BusinessApplicationObjectDTO dto : objects) {
            if (dto == null || dto.getObjectId() == null) {
                throw new BusinessException("应用关联的业务对象ID不能为空");
            }
            if (!objectIds.add(dto.getObjectId())) {
                throw new BusinessException("同一业务对象不能重复加入应用");
            }
            String role = objects.size() == 1
                    ? BusinessApplicationObjectRole.PRIMARY
                    : StringUtils.defaultIfBlank(dto.getObjectRole(), BusinessApplicationObjectRole.SHARED)
                            .toUpperCase(Locale.ROOT);
            if (!BusinessApplicationObjectRole.supportedRoles().contains(role)) {
                throw new BusinessException("应用对象角色不正确");
            }
            if (BusinessApplicationObjectRole.PRIMARY.equals(role) && ++primaryCount > 1) {
                throw new BusinessException("一个业务应用最多只能有一个主对象");
            }
            AiBusinessObject object = objectService.requireEntity(dto.getObjectId());
            if (!StringUtils.equals(application.getSuiteCode(), object.getSuiteCode())) {
                throw new BusinessException("业务对象与业务应用必须属于同一业务域");
            }
            AiBusinessApplicationObject entity = new AiBusinessApplicationObject();
            entity.setId(IdWorker.getId());
            entity.setTenantId(resolveTenantId());
            entity.setApplicationId(application.getId());
            entity.setObjectId(object.getId());
            entity.setObjectRole(role);
            entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
            entity.setOptions(normalizeOptions(dto.getOptions()));
            entities.add(entity);
        }
        return entities;
    }

    private String normalizeOptions(String options) {
        String value = StringUtils.trimToNull(options);
        if (value == null) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(value);
            return json.toJSONString();
        } catch (Exception e) {
            throw new BusinessException("应用内对象配置必须是合法 JSON 对象");
        }
    }

    private void enrichDatabaseSummary(BusinessApplicationObjectVO object) {
        if (object == null) {
            return;
        }
        object.setSyncStatus("UNKNOWN");
        try {
            JSONObject modelSchema = JSON.parseObject(object.getModelSchema());
            if (modelSchema != null) {
                object.setTableMode(modelSchema.getString("tableMode"));
            }
        } catch (Exception ignored) {
            object.setTableMode(null);
        }
        try {
            JSONObject datasource = JSON.parseObject(object.getRuntimeDatasourceSnapshot());
            if (datasource != null) {
                object.setDatasourceName(datasource.getString("datasourceName"));
                object.setAllowDdl(datasource.getBoolean("allowDdl"));
                object.setReadonly(datasource.getBoolean("readonly"));
                if (StringUtils.isBlank(object.getDatasourceCode())) {
                    object.setDatasourceCode(datasource.getString("datasourceCode"));
                }
            }
        } catch (Exception ignored) {
            object.setDatasourceName(null);
        }
        try {
            JSONObject designerOptions = JSON.parseObject(object.getDesignerOptions());
            JSONObject databaseSync = designerOptions == null ? null : designerOptions.getJSONObject("databaseSync");
            if (databaseSync != null && StringUtils.isNotBlank(databaseSync.getString("status"))) {
                object.setSyncStatus(databaseSync.getString("status"));
            }
        } catch (Exception ignored) {
            object.setSyncStatus("UNKNOWN");
        }
    }

    private JSONObject readInAppBuilder(String options) {
        if (StringUtils.isBlank(options)) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(options);
            return root == null ? null : root.getJSONObject("inAppBuilder");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> readPageIds(JSONObject builder) {
        Set<String> pageIds = new LinkedHashSet<>();
        JSONArray nodes = builder.getJSONArray("nodes");
        if (nodes != null) {
            for (int index = 0; index < nodes.size(); index++) {
                JSONObject node = nodes.getJSONObject(index);
                if (node != null && "page".equals(node.getString("type"))
                        && StringUtils.isNotBlank(node.getString("id"))) {
                    pageIds.add(node.getString("id"));
                }
            }
        }
        JSONObject pages = builder.getJSONObject("pages");
        if (pages != null) {
            pageIds.addAll(pages.keySet());
        }
        return pageIds;
    }

    private void collectReferencedObjectIds(Object value, Set<String> objectIds) {
        if (value instanceof JSONObject object) {
            JSONObject objectRef = object.getJSONObject("objectRef");
            if (objectRef != null && objectRef.get("objectId") != null) {
                objectIds.add(String.valueOf(objectRef.get("objectId")));
            }
            for (Object child : object.values()) {
                collectReferencedObjectIds(child, objectIds);
            }
            return;
        }
        if (value instanceof JSONArray array) {
            for (Object child : array) {
                collectReferencedObjectIds(child, objectIds);
            }
        }
    }

    private boolean isOrphanPageFormObject(BusinessApplicationObjectVO association,
                                           Set<String> pageIds,
                                           Set<String> referencedObjectIds) {
        JSONObject marker;
        try {
            marker = JSON.parseObject(association.getOptions());
        } catch (Exception ignored) {
            return false;
        }
        if (marker == null
                || !"PAGE_FORM".equals(marker.getString("managedBy"))
                || StringUtils.isBlank(marker.getString("sourcePageId"))) {
            return false;
        }
        if (pageIds.contains(marker.getString("sourcePageId"))) {
            return false;
        }
        return !referencedObjectIds.contains(String.valueOf(association.getObjectId()));
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

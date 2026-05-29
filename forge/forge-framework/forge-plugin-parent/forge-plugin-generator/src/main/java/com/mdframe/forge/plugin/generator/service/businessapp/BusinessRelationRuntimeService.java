package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessRelationRuntimeVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 对象关系运行解析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessRelationRuntimeService {

    private final BusinessObjectRelationMapper relationMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessAppMapper businessAppMapper;
    private final AiCrudConfigMapper aiCrudConfigMapper;

    /**
     * 查询对象关系运行入口
     *
     * @param objectId 业务对象 ID
     * @return 关系运行入口列表
     */
    public List<BusinessRelationRuntimeVO> relationRuntime(Long objectId) {
        Long tenantId = resolveTenantId();

        // 查询业务对象
        AiBusinessObject object = businessObjectMapper.selectById(objectId);
        if (object == null) {
            throw new BusinessException("业务对象不存在");
        }

        List<AiBusinessObjectRelation> relations = relationMapper.selectRuntimeRelationsBySource(
                tenantId, object.getSuiteCode(), object.getObjectCode());

        List<BusinessRelationRuntimeVO> result = new ArrayList<>();
        for (AiBusinessObjectRelation relation : relations) {
            BusinessRelationRuntimeVO vo = buildRelationRuntimeVO(relation, object, tenantId);
            result.add(vo);
        }

        return result;
    }

    private BusinessRelationRuntimeVO buildRelationRuntimeVO(
            AiBusinessObjectRelation relation, 
            AiBusinessObject sourceObject,
            Long tenantId) {
        BusinessRelationRuntimeVO vo = new BusinessRelationRuntimeVO();
        vo.setRelationId(relation.getId());
        vo.setRelationType(relation.getRelationType());
        vo.setSourceObjectCode(relation.getSourceObjectCode());
        vo.setSourceObjectName(sourceObject.getObjectName());
        vo.setTargetObjectCode(relation.getTargetObjectCode());
        vo.setSourceField(relation.getSourceFieldCode());
        vo.setTargetField(relation.getTargetFieldCode());

        vo.setRelationName(StringUtils.defaultIfBlank(
                relation.getRelationName(),
                buildRelationName(sourceObject.getObjectName(), relation.getTargetObjectCode())));

        // 查询目标对象
        AiBusinessObject targetObject = businessObjectMapper.selectByObjectCode(
                tenantId, relation.getSuiteCode(), relation.getTargetObjectCode());
        if (targetObject != null) {
            vo.setTargetObjectName(targetObject.getObjectName());
        } else {
            vo.setTargetObjectName(relation.getTargetObjectCode());
        }

        // 查询目标对象的应用入口
        AiBusinessApp targetApp = businessAppMapper.selectRuntimeAppByObject(
                tenantId, relation.getSuiteCode(), relation.getTargetObjectCode());

        if (targetApp == null) {
            vo.setCanOpen(false);
            vo.setMessage("目标对象未配置应用入口");
            vo.setNextAction("CREATE_APP_ENTRY");
            vo.setNextActionLabel("配置应用入口");
            return vo;
        }

        vo.setTargetAppId(targetApp.getId());
        vo.setTargetAppCode(targetApp.getAppCode());

        // 校验应用入口状态
        if (Integer.valueOf(0).equals(targetApp.getStatus())) {
            vo.setCanOpen(false);
            vo.setMessage("目标应用入口已停用");
            vo.setNextAction("ENABLE_APP_ENTRY");
            vo.setNextActionLabel("启用应用入口");
            return vo;
        }

        // 校验运行配置
        String configKey = StringUtils.trimToNull(targetApp.getConfigKey());
        vo.setTargetConfigKey(configKey);

        if (configKey == null) {
            vo.setCanOpen(false);
            vo.setMessage("目标应用未配置运行配置");
            vo.setNextAction("PUBLISH_APP");
            vo.setNextActionLabel("发布应用");
            return vo;
        }

        // 检查运行配置是否存在且已发布
        var config = aiCrudConfigMapper.selectByConfigKey(tenantId, configKey);
        if (config == null) {
            vo.setCanOpen(false);
            vo.setMessage("目标运行配置不存在");
            vo.setNextAction("PUBLISH_APP");
            vo.setNextActionLabel("发布应用");
            return vo;
        }
        if ("1".equals(config.getStatus())) {
            vo.setCanOpen(false);
            vo.setMessage("目标运行配置已停用");
            vo.setNextAction("ENABLE_RUNTIME");
            vo.setNextActionLabel("启用运行配置");
            return vo;
        }
        if (!"PUBLISHED".equals(config.getPublishStatus())) {
            vo.setCanOpen(false);
            vo.setMessage("目标运行配置未发布");
            vo.setNextAction("PUBLISH_APP");
            vo.setNextActionLabel("发布应用");
            return vo;
        }

        // 设置可打开状态
        vo.setCanOpen(true);
        vo.setOpenType("ROUTE");
        vo.setTargetUrl("/ai/crud-page/" + configKey);

        // 生成默认筛选参数
        vo.setDefaultFilter(buildDefaultFilter(relation));

        vo.setMessage("可进入" + vo.getTargetObjectName() + "列表");
        vo.setNextAction("OPEN_RUNTIME");
        vo.setNextActionLabel("查看" + vo.getTargetObjectName());

        return vo;
    }

    private String buildRelationName(String sourceName, String targetObjectCode) {
        // 将对象编码转换为业务名称
        String normalizedCode = StringUtils.lowerCase(targetObjectCode);
        String targetName = switch (normalizedCode) {
            case "contact" -> "联系人";
            case "opportunity" -> "商机";
            case "contract" -> "合同";
            case "payment" -> "回款";
            case "contract_item" -> "合同明细";
            case "follow_record" -> "跟进记录";
            default -> targetObjectCode;
        };
        return sourceName + "的" + targetName;
    }

    private String buildDefaultFilter(AiBusinessObjectRelation relation) {
        // 构建默认筛选参数，用于跳转时携带关联条件
        if (StringUtils.isBlank(relation.getSourceFieldCode()) || StringUtils.isBlank(relation.getTargetFieldCode())) {
            return null;
        }
        JSONObject filter = new JSONObject();
        filter.put("field", relation.getTargetFieldCode());
        filter.put("operator", "eq");
        filter.put("sourceField", relation.getSourceFieldCode());
        return JSON.toJSONString(filter);
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

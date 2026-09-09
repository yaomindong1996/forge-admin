package com.mdframe.forge.starter.flow.service;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 流程单据经手人索引。
 */
public interface FlowRecordParticipantService {

    void record(FlowBusiness business, String userId, String relationType);

    void record(Long tenantId, String businessType, String businessKey, String processInstanceId,
                String userId, String relationType);

    List<String> listBusinessIds(Long tenantId, String userId, String businessType);

    Map<String, List<String>> listRelationTypes(Long tenantId, String userId, String businessType,
                                                Collection<String> businessIds);
}

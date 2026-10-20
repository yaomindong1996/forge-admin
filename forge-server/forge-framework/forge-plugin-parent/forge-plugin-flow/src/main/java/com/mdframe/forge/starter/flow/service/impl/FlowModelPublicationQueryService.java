package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.mapper.FlowModelVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlowModelPublicationQueryService {
    private final FlowModelVersionMapper versionMapper;

    /** 仅补齐已经由模型分页授权筛选后的记录，一页只查询一次快照。 */
    public void enrich(List<FlowModel> models) {
        if (models == null || models.isEmpty()) {
            return;
        }
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0 || models.stream()
                .anyMatch(model -> !tenantId.equals(model.getTenantId()))) {
            throw new BusinessException("无法读取当前租户的流程发布信息");
        }
        List<String> ids = models.stream().map(FlowModel::getId).distinct().toList();
        Map<String, FlowModelVersion> snapshots = versionMapper.selectPublicationSnapshots(ids, tenantId).stream()
                .collect(Collectors.toMap(FlowModelVersion::getModelId, Function.identity()));
        models.forEach(model -> FlowModelPublicationSummary.apply(model, snapshots.get(model.getId())));
    }
}

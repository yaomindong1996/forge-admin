package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.enums.FlowModelPublicationState;
import com.mdframe.forge.starter.flow.enums.FlowModelStatus;

import java.util.Objects;

/** 只比较已版本化内容，不用更新时间推断发布状态，也不修改运行模型。 */
final class FlowModelPublicationSummary {
    private FlowModelPublicationSummary() {
    }

    static void apply(FlowModel model, FlowModelVersion snapshot) {
        model.setPublishedVersion(null);
        model.setPublishedTime(null);
        if (matchesDeployment(model, snapshot)) {
            model.setPublishedVersion(snapshot.getVersion());
            model.setPublishedTime(snapshot.getPublishTime());
            boolean unchanged = Objects.equals(content(model.getBpmnXml()), content(snapshot.getBpmnXml()))
                    && Objects.equals(content(model.getFormJson()), content(snapshot.getFormJson()));
            model.setPublicationState((unchanged ? FlowModelPublicationState.SYNCED
                    : FlowModelPublicationState.CHANGED).getCode());
            return;
        }
        boolean hasReleaseEvidence = snapshot != null || hasText(model.getDeploymentId())
                || hasText(model.getProcessDefinitionId()) || model.getDeployTime() != null
                || FlowModelStatus.PUBLISHED.matches(model.getStatus())
                || FlowModelStatus.SUSPENDED.matches(model.getStatus());
        model.setPublicationState((hasReleaseEvidence ? FlowModelPublicationState.UNKNOWN
                : FlowModelPublicationState.UNPUBLISHED).getCode());
    }

    private static boolean matchesDeployment(FlowModel model, FlowModelVersion snapshot) {
        return snapshot != null && hasText(snapshot.getBpmnXml())
                && hasText(model.getDeploymentId()) && hasText(model.getProcessDefinitionId())
                && snapshot.getVersion() != null && snapshot.getVersion() > 0
                && Objects.equals(model.getId(), snapshot.getModelId())
                && Objects.equals(model.getTenantId(), snapshot.getTenantId())
                && Objects.equals(model.getVersion(), snapshot.getVersion())
                && Objects.equals(model.getDeploymentId(), snapshot.getDeploymentId())
                && Objects.equals(model.getProcessDefinitionId(), snapshot.getProcessDefinitionId());
    }

    private static String content(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

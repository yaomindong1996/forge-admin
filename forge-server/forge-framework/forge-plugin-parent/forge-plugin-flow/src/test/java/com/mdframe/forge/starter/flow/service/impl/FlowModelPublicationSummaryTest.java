package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.enums.FlowModelPublicationState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlowModelPublicationSummaryTest {
    @Test
    void initialVersionIsNotEvidenceOfPublication() {
        FlowModel model = model();
        model.setStatus(0);
        model.setDeploymentId(null);
        model.setProcessDefinitionId(null);
        FlowModelPublicationSummary.apply(model, null);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.UNPUBLISHED.getCode());
        assertThat(model.getPublishedVersion()).isNull();
        assertThat(model.getVersion()).isEqualTo(3);
    }

    @Test
    void missingSnapshotDoesNotPretendPublishedModelIsUnpublished() {
        FlowModel model = model();
        FlowModelPublicationSummary.apply(model, null);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.UNKNOWN.getCode());
        assertThat(model.getPublishedVersion()).isNull();
        assertThat(model.getStatus()).isEqualTo(1);
    }

    @Test
    void matchingSnapshotReturnsTrueVersionAndContentRelationship() {
        FlowModel model = model();
        FlowModelVersion snapshot = snapshot(model);
        FlowModelPublicationSummary.apply(model, snapshot);
        assertThat(model.getPublishedVersion()).isEqualTo(3);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.SYNCED.getCode());
        model.setFormJson("{\"required\":true}");
        FlowModelPublicationSummary.apply(model, snapshot);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.CHANGED.getCode());
        assertThat(model.getStatus()).isEqualTo(1);
    }

    @Test
    void xmlComparisonIsCaseSensitiveAndDoesNotUseUpdateTime() {
        FlowModel model = model();
        FlowModelVersion snapshot = snapshot(model);
        model.setBpmnXml("<task name=\"ABC\"/>");
        FlowModelPublicationSummary.apply(model, snapshot);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.CHANGED.getCode());
        assertThat(model.getPublishedVersion()).isEqualTo(3);
    }

    @Test
    void mismatchedTenantModelVersionAndDeploymentNeverExposeWrongRelease() {
        FlowModel model = model();
        for (int field = 0; field < 5; field++) {
            FlowModelVersion snapshot = snapshot(model);
            switch (field) {
                case 0 -> snapshot.setTenantId(2L);
                case 1 -> snapshot.setModelId("other-model");
                case 2 -> snapshot.setVersion(9);
                case 3 -> snapshot.setDeploymentId("other-deployment");
                default -> snapshot.setProcessDefinitionId("other-definition");
            }
            FlowModelPublicationSummary.apply(model, snapshot);
            assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.UNKNOWN.getCode());
            assertThat(model.getPublishedVersion()).isNull();
        }
    }

    @Test
    void historicalEvidenceAndSuspendedModelsRemainConservative() {
        FlowModel model = model();
        FlowModelVersion snapshot = snapshot(model);
        model.setStatus(0);
        model.setDeploymentId(null);
        model.setProcessDefinitionId(null);
        FlowModelPublicationSummary.apply(model, snapshot);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.UNKNOWN.getCode());
        model.setStatus(2);
        FlowModelPublicationSummary.apply(model, null);
        assertThat(model.getPublicationState()).isEqualTo(FlowModelPublicationState.UNKNOWN.getCode());
    }

    private FlowModel model() {
        FlowModel model = new FlowModel();
        model.setId("model-A");
        model.setTenantId(1L);
        model.setVersion(3);
        model.setStatus(1);
        model.setDeploymentId("deployment-A");
        model.setProcessDefinitionId("definition-A");
        model.setBpmnXml("<task name=\"abc\"/>");
        return model;
    }

    private FlowModelVersion snapshot(FlowModel model) {
        FlowModelVersion snapshot = new FlowModelVersion();
        snapshot.setModelId(model.getId());
        snapshot.setTenantId(model.getTenantId());
        snapshot.setVersion(model.getVersion());
        snapshot.setDeploymentId(model.getDeploymentId());
        snapshot.setProcessDefinitionId(model.getProcessDefinitionId());
        snapshot.setBpmnXml(model.getBpmnXml());
        snapshot.setFormJson("");
        return snapshot;
    }
}

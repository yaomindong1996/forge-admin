package com.mdframe.forge.plugin.capability.flowaction.source;

import java.util.List;

public record FlowActionRegistrationSource(
        Long objectId,
        String suiteCode,
        String objectCode,
        String objectName,
        String flowModelKey,
        Integer publishedObjectVersion,
        boolean startSupported,
        boolean submissionSupported,
        String submissionUnavailableReason,
        List<FlowActionSubmissionField> submissionFields) {
}

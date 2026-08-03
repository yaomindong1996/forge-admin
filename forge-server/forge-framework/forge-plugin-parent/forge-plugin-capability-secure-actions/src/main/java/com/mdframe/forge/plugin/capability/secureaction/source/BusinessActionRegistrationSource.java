package com.mdframe.forge.plugin.capability.secureaction.source;

import java.util.List;

/**
 * 业务动作能力注册页的已发布快照数据。
 */
public record BusinessActionRegistrationSource(
        Long objectId,
        String suiteCode,
        String objectCode,
        String objectName,
        Integer publishedObjectVersion,
        List<BusinessActionRegistrationAction> actions,
        List<BusinessActionRegistrationField> writableFields) {
}

package com.mdframe.forge.starter.flow.support;

import com.mdframe.forge.starter.flow.entity.FlowRecordParticipant;
import com.mdframe.forge.starter.flow.entity.FlowTaskCandidate;
import com.mdframe.forge.starter.flow.mapper.FlowRecordParticipantMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskCandidateMapper;
import com.mdframe.forge.starter.flow.mapper.FlowUserGroupMapper;
import com.mdframe.forge.starter.orm.support.DuplicateKeyActions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 流程写路径的跨库封装：唯一键冲突用 Java 处理，Mapper 只保留标准 INSERT/UPDATE。
 */
@Component
@RequiredArgsConstructor
public class FlowDialectFreeWrites {

    private final FlowTaskCandidateMapper taskCandidateMapper;
    private final FlowRecordParticipantMapper participantMapper;
    private final FlowUserGroupMapper userGroupMapper;

    public int upsertTaskCandidate(FlowTaskCandidate candidate) {
        return DuplicateKeyActions.upsert(
                () -> taskCandidateMapper.updateUpsertByUniqueKey(candidate),
                () -> taskCandidateMapper.insertPlain(candidate));
    }

    public int insertParticipantIgnoreConflict(FlowRecordParticipant participant) {
        return DuplicateKeyActions.insertIgnoreConflict(
                () -> participantMapper.insertPlain(participant));
    }

    public int upsertUserGroupMember(Long groupId, Long userId, Long tenantId,
                                     Long operatorId, Long createDept) {
        return DuplicateKeyActions.upsert(
                () -> userGroupMapper.reactivateMember(groupId, userId, tenantId, operatorId),
                () -> userGroupMapper.insertMemberPlain(groupId, userId, tenantId, operatorId, createDept));
    }
}

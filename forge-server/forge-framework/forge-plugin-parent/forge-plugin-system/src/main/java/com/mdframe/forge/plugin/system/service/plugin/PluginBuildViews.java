package com.mdframe.forge.plugin.system.service.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import com.mdframe.forge.plugin.system.mapper.SysPluginBuildMapper;
import com.mdframe.forge.plugin.system.vo.PluginBuildExecutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PluginBuildViews {
    private final SysPluginBuildMapper mapper;
    private final ObjectMapper json;

    public PluginBuildExecutionVO summary(Long tenantId, String id) {
        var build = mapper.selectSummary(tenantId, id);
        if (build == null) {
            return null;
        }
        return new PluginBuildExecutionVO(build.getWorkerId(), build.getPhase(), build.getSourceCommit(),
                build.getImage(), build.getStartedTime(), build.getLeaseExpiresTime(), build.getDeadlineTime(),
                build.getFinishedTime(), expired(build), decode(build.getResultJson()), build.getResultSha256());
    }

    public String encode(PluginBuildResultDTO result) {
        try {
            return json.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("构建结果无法保存", exception);
        }
    }

    private boolean expired(SysPluginBuild build) {
        var now = LocalDateTime.now();
        return build.getFinishedTime() == null
                && (!build.getLeaseExpiresTime().isAfter(now) || !build.getDeadlineTime().isAfter(now));
    }

    private PluginBuildResultDTO decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return json.readValue(value, PluginBuildResultDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("构建结果损坏，请联系管理员", exception);
        }
    }
}

package com.mdframe.forge.plugin.capability.highrisk.publish;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/capability/high-risk-action")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.capability.high-risk", name = "enabled", havingValue = "true")
public class HighRiskActionController {
    private final HighRiskActionPublisher publisher;

    @PostMapping("/publish")
    @SaCheckPermission("ai:capability:high-risk:publish")
    public RespInfo<Long> publish(@Valid @RequestBody HighRiskActionPublishDTO dto) {
        return RespInfo.success(publisher.publish(SessionHelper.getTenantId(), dto));
    }
}

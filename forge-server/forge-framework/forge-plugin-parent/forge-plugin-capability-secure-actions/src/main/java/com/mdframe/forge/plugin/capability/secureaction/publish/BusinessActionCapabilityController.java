package com.mdframe.forge.plugin.capability.secureaction.publish;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
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
@RequestMapping("/ai/capability/business-action")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "forge.capability.secure-actions",
        name = "enabled",
        havingValue = "true")
public class BusinessActionCapabilityController {

    private final BusinessActionCapabilityPublisher publisher;

    @PostMapping("/publish")
    @SaCheckPermission("ai:capability:business-action:publish")
    @ApiDecrypt
    @OperationLog(module = "AI中枢受控动作", type = OperationType.ADD, desc = "发布受控业务动作能力")
    public RespInfo<Long> publish(@Valid @RequestBody BusinessActionCapabilityPublishDTO dto) {
        return RespInfo.success(publisher.publish(SessionHelper.getTenantId(), dto));
    }
}

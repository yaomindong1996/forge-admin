package com.mdframe.forge.flow.bridge;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.spi.FlowMonitorUserLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemFlowMonitorUserLookup implements FlowMonitorUserLookup {

    private final ISysUserService sysUserService;

    @Override
    public String findDisplayName(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            SysUser user = sysUserService.selectUserById(Long.parseLong(userId));
            return user == null ? null : user.getRealName();
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

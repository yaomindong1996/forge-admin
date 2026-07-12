package com.mdframe.forge.plugin.capability.identity.security;

import com.mdframe.forge.starter.core.session.LoginUser;

public record AuthenticatedCapabilityIdentity(
        CapabilitySecurityPrincipal principal,
        LoginUser loginUser) {
}

package com.mdframe.forge.starter.outbound.security;

import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;

public interface OutboundPolicyService {

    ValidatedOutboundTarget validate(OutboundRequestContext context);
}

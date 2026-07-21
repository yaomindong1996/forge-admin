package com.mdframe.forge.starter.outbound.client;

import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;

public interface SecureOutboundClient {

    OutboundResponse execute(OutboundRequest request);
}

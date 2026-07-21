package com.mdframe.forge.starter.outbound.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class SystemOutboundDnsResolver implements OutboundDnsResolver {

    @Override
    public List<InetAddress> resolveAll(String host) throws UnknownHostException {
        return Arrays.asList(InetAddress.getAllByName(host));
    }
}

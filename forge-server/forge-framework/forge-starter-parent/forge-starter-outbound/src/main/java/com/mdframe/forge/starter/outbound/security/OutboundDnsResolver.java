package com.mdframe.forge.starter.outbound.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public interface OutboundDnsResolver {

    List<InetAddress> resolveAll(String host) throws UnknownHostException;
}

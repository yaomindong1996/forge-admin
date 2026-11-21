package com.mdframe.forge.plugin.ai.resource.service;

import java.net.URI;

/**
 * 判断主机或 URL 是否属于内网私有地址（纯静态地址校验，不执行 DNS 探测）。
 */
public final class AiPrivateEndpoint {

    private AiPrivateEndpoint() {
    }

    public static boolean host(String host) {
        if (host == null) {
            return false;
        }
        if ("::1".equals(host) || "[::1]".equals(host)) {
            return true;
        }
        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        int[] n = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!octets[i].matches("0|[1-9][0-9]{0,2}")) {
                return false;
            }
            n[i] = Integer.parseInt(octets[i]);
            if (n[i] > 255) {
                return false;
            }
        }
        return n[0] == 127
                || n[0] == 10
                || (n[0] == 192 && n[1] == 168)
                || (n[0] == 172 && n[1] >= 16 && n[1] <= 31);
    }

    public static boolean url(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && host(uri.getHost())
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null
                    && uri.getQuery() == null
                    && uri.getPort() != 0
                    && uri.getPort() <= 65535;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

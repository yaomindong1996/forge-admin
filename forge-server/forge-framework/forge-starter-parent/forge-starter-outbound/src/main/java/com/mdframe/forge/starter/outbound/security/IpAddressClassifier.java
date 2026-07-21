package com.mdframe.forge.starter.outbound.security;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class IpAddressClassifier {

    public enum AddressType {
        PUBLIC,
        PRIVATE,
        BLOCKED
    }

    private final Set<InetAddress> localAddresses;

    public IpAddressClassifier() {
        this(discoverLocalAddresses());
    }

    public IpAddressClassifier(Set<InetAddress> localAddresses) {
        this.localAddresses = Set.copyOf(localAddresses);
    }

    public AddressType classify(InetAddress address) {
        if (address == null || localAddresses.contains(address)) {
            return AddressType.BLOCKED;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return classifyIpv4(bytes);
        }
        if (bytes.length == 16 && isIpv4Mapped(bytes)) {
            byte[] ipv4 = new byte[4];
            System.arraycopy(bytes, 12, ipv4, 0, ipv4.length);
            return classifyIpv4(ipv4);
        }
        return classifyIpv6(address, bytes);
    }

    private AddressType classifyIpv4(byte[] bytes) {
        int first = unsigned(bytes[0]);
        int second = unsigned(bytes[1]);
        int third = unsigned(bytes[2]);
        int fourth = unsigned(bytes[3]);

        if (first == 10 || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)) {
            return AddressType.PRIVATE;
        }

        boolean blocked = first == 0
                || first == 127
                || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254)
                || (first == 168 && second == 63 && third == 129 && fourth == 16)
                || (first == 192 && second == 0 && third == 0)
                || (first == 192 && second == 0 && third == 2)
                || (first == 192 && second == 88 && third == 99)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
        return blocked ? AddressType.BLOCKED : AddressType.PUBLIC;
    }

    private AddressType classifyIpv6(InetAddress address, byte[] bytes) {
        int first = unsigned(bytes[0]);
        int second = unsigned(bytes[1]);

        if (isAwsIpv6Metadata(bytes)) {
            return AddressType.BLOCKED;
        }
        if ((first & 0xfe) == 0xfc) {
            return AddressType.PRIVATE;
        }
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return AddressType.BLOCKED;
        }

        boolean globallyRoutablePrefix = (first & 0xe0) == 0x20;
        boolean ietfSpecial = first == 0x20 && second == 0x01
                && (unsigned(bytes[2]) == 0x00 || unsigned(bytes[2]) == 0x01);
        boolean documentation = first == 0x20 && second == 0x01
                && unsigned(bytes[2]) == 0x0d && unsigned(bytes[3]) == 0xb8;
        boolean orchidV2 = first == 0x20 && second == 0x01
                && unsigned(bytes[2]) >= 0x20 && unsigned(bytes[2]) <= 0x2f;
        boolean sixToFour = first == 0x20 && second == 0x02;
        boolean documentationV2 = first == 0x3f && second == 0xff
                && (unsigned(bytes[2]) & 0xf0) == 0;
        if (!globallyRoutablePrefix || ietfSpecial || documentation || orchidV2 || sixToFour
                || documentationV2) {
            return AddressType.BLOCKED;
        }
        return AddressType.PUBLIC;
    }

    private boolean isAwsIpv6Metadata(byte[] bytes) {
        byte[] metadata = {
                (byte) 0xfd, 0x00, 0x0e, (byte) 0xc2,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0x02, 0x54
        };
        return java.util.Arrays.equals(bytes, metadata);
    }

    private boolean isIpv4Mapped(byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return unsigned(bytes[10]) == 0xff && unsigned(bytes[11]) == 0xff;
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private static Set<InetAddress> discoverLocalAddresses() {
        Set<InetAddress> addresses = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return addresses;
            }
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                addresses.addAll(Collections.list(networkInterface.getInetAddresses()));
            }
        } catch (SocketException ignored) {
            addLoopbackFallbacks(addresses);
        }
        return addresses;
    }

    private static void addLoopbackFallbacks(Set<InetAddress> addresses) {
        try {
            addresses.add(Inet4Address.getByName("127.0.0.1"));
            addresses.add(InetAddress.getByName("::1"));
        } catch (UnknownHostException ignored) {
            // Literal loopback parsing is guaranteed by the JDK; range checks remain the fallback.
        }
    }
}

package com.mdframe.forge.starter.outbound.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.InetAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpAddressClassifierTest {

    private final IpAddressClassifier classifier = new IpAddressClassifier(Set.of());

    @ParameterizedTest
    @CsvSource({
            "8.8.8.8, PUBLIC",
            "1.1.1.1, PUBLIC",
            "2606:4700:4700::1111, PUBLIC",
            "10.0.0.1, PRIVATE",
            "172.16.0.1, PRIVATE",
            "192.168.255.255, PRIVATE",
            "fc00::1, PRIVATE",
            "fdff:ffff::1, PRIVATE",
            "127.0.0.1, BLOCKED",
            "169.254.169.254, BLOCKED",
            "168.63.129.16, BLOCKED",
            "100.64.0.1, BLOCKED",
            "192.0.2.1, BLOCKED",
            "198.18.0.1, BLOCKED",
            "224.0.0.1, BLOCKED",
            "240.0.0.1, BLOCKED",
            "0.0.0.0, BLOCKED",
            "::1, BLOCKED",
            "fe80::1, BLOCKED",
            "2001:db8::1, BLOCKED",
            "3fff::1, BLOCKED",
            "fd00:ec2::254, BLOCKED",
            "ff02::1, BLOCKED"
    })
    void shouldClassifySpecialAddressRanges(String address, IpAddressClassifier.AddressType expected)
            throws Exception {
        assertEquals(expected, classifier.classify(InetAddress.getByName(address)));
    }

    @ParameterizedTest
    @CsvSource({
            "::ffff:127.0.0.1, BLOCKED",
            "::ffff:10.0.0.1, PRIVATE",
            "::ffff:8.8.8.8, PUBLIC"
    })
    void shouldClassifyIpv4MappedIpv6ByEmbeddedAddress(String address,
                                                        IpAddressClassifier.AddressType expected)
            throws Exception {
        byte[] mapped = mappedAddress(address.substring(address.lastIndexOf(':') + 1));
        assertEquals(expected, classifier.classify(InetAddress.getByAddress(mapped)));
    }

    private byte[] mappedAddress(String ipv4) throws Exception {
        byte[] bytes = new byte[16];
        bytes[10] = (byte) 0xff;
        bytes[11] = (byte) 0xff;
        byte[] ipv4Bytes = InetAddress.getByName(ipv4).getAddress();
        System.arraycopy(ipv4Bytes, 0, bytes, 12, ipv4Bytes.length);
        return bytes;
    }
}

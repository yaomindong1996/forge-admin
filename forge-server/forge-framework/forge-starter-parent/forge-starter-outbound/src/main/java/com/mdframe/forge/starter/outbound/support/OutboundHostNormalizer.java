package com.mdframe.forge.starter.outbound.support;

import com.mdframe.forge.starter.core.exception.BusinessException;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class OutboundHostNormalizer {

    private static final int MAX_HOST_LENGTH = 253;

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("主机不能为空");
        }
        String host = value.trim();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.contains(":")) {
            return normalizeIpv6(host);
        }
        if (host.regionMatches(true, 0, "0x", 0, 2) || host.toLowerCase(Locale.ROOT).contains(".0x")) {
            throw new BusinessException("IPv4主机格式不合法");
        }
        if (isIpv4Candidate(host)) {
            return normalizeIpv4(host);
        }
        return normalizeDomain(host);
    }

    private String normalizeDomain(String value) {
        String host = removeTrailingDot(value);
        if (host.indexOf('*') >= 0 || host.indexOf('/') >= 0 || host.indexOf('\\') >= 0
                || host.indexOf('@') >= 0 || host.indexOf('%') >= 0) {
            throw new BusinessException("主机格式不合法");
        }
        try {
            String ascii = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            if (ascii.isBlank() || ascii.length() > MAX_HOST_LENGTH || ascii.startsWith(".")
                    || ascii.contains("..")) {
                throw new BusinessException("主机格式不合法");
            }
            return ascii;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("主机格式不合法");
        }
    }

    private String normalizeIpv6(String value) {
        if (value.indexOf('%') >= 0) {
            throw new BusinessException("IPv6主机不能包含区域标识");
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            return address.getHostAddress().toLowerCase(Locale.ROOT);
        } catch (UnknownHostException exception) {
            throw new BusinessException("IPv6主机格式不合法");
        }
    }

    private String normalizeIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            throw new BusinessException("IPv4主机格式不合法");
        }
        StringBuilder normalized = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)
                    || (part.length() > 1 && part.charAt(0) == '0')) {
                throw new BusinessException("IPv4主机格式不合法");
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                throw new BusinessException("IPv4主机格式不合法");
            }
            if (!normalized.isEmpty()) {
                normalized.append('.');
            }
            normalized.append(octet);
        }
        return normalized.toString();
    }

    private boolean isIpv4Candidate(String value) {
        return !value.isEmpty() && value.chars().allMatch(character -> Character.isDigit(character) || character == '.');
    }

    private String removeTrailingDot(String value) {
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }
}

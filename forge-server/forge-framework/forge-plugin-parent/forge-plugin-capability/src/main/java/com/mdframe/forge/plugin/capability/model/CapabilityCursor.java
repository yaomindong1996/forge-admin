package com.mdframe.forge.plugin.capability.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record CapabilityCursor(
        String snapshotVersion,
        String queryFingerprint,
        String lastSortKey,
        String signature) {

    private static final String DELIMITER = "\n";

    public String encode() {
        String value = snapshotVersion + DELIMITER
                + queryFingerprint + DELIMITER
                + lastSortKey + DELIMITER
                + signature;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static CapabilityCursor decode(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int firstDelimiter = decoded.indexOf(DELIMITER);
            int secondDelimiter = decoded.indexOf(DELIMITER, firstDelimiter + DELIMITER.length());
            int thirdDelimiter = decoded.indexOf(DELIMITER, secondDelimiter + DELIMITER.length());
            if (firstDelimiter <= 0
                    || secondDelimiter <= firstDelimiter + DELIMITER.length()
                    || thirdDelimiter <= secondDelimiter + DELIMITER.length()
                    || thirdDelimiter == decoded.length() - 1
                    || decoded.indexOf(DELIMITER, thirdDelimiter + DELIMITER.length()) >= 0) {
                throw new IllegalArgumentException("游标格式无效");
            }
            return new CapabilityCursor(
                    decoded.substring(0, firstDelimiter),
                    decoded.substring(firstDelimiter + DELIMITER.length(), secondDelimiter),
                    decoded.substring(secondDelimiter + DELIMITER.length(), thirdDelimiter),
                    decoded.substring(thirdDelimiter + DELIMITER.length()));
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("游标格式无效", exception);
        }
    }
}

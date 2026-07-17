package com.mdframe.forge.plugin.generator.manager.coderule;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

/**
 * 编码流水号固定宽度进制转换器。
 */
@Component
public class CodeRuleRadixCodec {

    private static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    private static final Map<String, String> ALPHABETS = Map.of(
            "DECIMAL", "0123456789",
            "HEX", "0123456789ABCDEF",
            "ALPHA_UPPER", "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "ALPHA_LOWER", "abcdefghijklmnopqrstuvwxyz",
            "ALPHANUMERIC", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    );

    public String encode(long value, String radixType, int length, boolean excludeAmbiguous) {
        if (value < 0) {
            throw new BusinessException("流水号不能小于0");
        }
        if (length < 1 || length > 32) {
            throw new BusinessException("流水号长度必须在1到32之间");
        }
        String alphabet = alphabet(radixType, excludeAmbiguous);
        BigInteger capacity = capacity(radixType, length, excludeAmbiguous);
        if (BigInteger.valueOf(value).compareTo(capacity) >= 0) {
            throw new BusinessException("流水号已超过当前进制和长度的容量，请增加流水号长度");
        }
        StringBuilder encoded = new StringBuilder();
        long remaining = value;
        do {
            int index = (int) (remaining % alphabet.length());
            encoded.append(alphabet.charAt(index));
            remaining /= alphabet.length();
        } while (remaining > 0);
        encoded.reverse();
        return StringUtils.leftPad(encoded.toString(), length, alphabet.charAt(0));
    }

    public int requiredLength(long value, String radixType, boolean excludeAmbiguous) {
        if (value < 0) {
            throw new BusinessException("流水号不能小于0");
        }
        BigInteger remaining = BigInteger.valueOf(value);
        BigInteger radix = BigInteger.valueOf(alphabet(radixType, excludeAmbiguous).length());
        int length = 1;
        while (remaining.compareTo(radix) >= 0) {
            remaining = remaining.divide(radix);
            length++;
        }
        return length;
    }

    public long maxValue(String radixType, int length, boolean excludeAmbiguous) {
        BigInteger maximum = capacity(radixType, length, excludeAmbiguous).subtract(BigInteger.ONE);
        return maximum.min(LONG_MAX_VALUE).longValue();
    }

    public int recommendedAllocationStep(String radixType,
                                         int length,
                                         boolean excludeAmbiguous) {
        BigInteger suggested = capacity(radixType, length, excludeAmbiguous)
                .divide(BigInteger.valueOf(1_000L));
        return suggested.max(BigInteger.ONE)
                .min(BigInteger.valueOf(1_000L))
                .intValue();
    }

    public String alphabet(String radixType, boolean excludeAmbiguous) {
        String normalized = StringUtils.defaultIfBlank(radixType, "DECIMAL").toUpperCase(Locale.ROOT);
        String alphabet = ALPHABETS.get(normalized);
        if (alphabet == null) {
            throw new BusinessException("不支持的流水号进制: " + radixType);
        }
        if (!excludeAmbiguous || "DECIMAL".equals(normalized) || "HEX".equals(normalized)) {
            return alphabet;
        }
        return alphabet.replace("I", "")
                .replace("O", "")
                .replace("Z", "")
                .replace("i", "")
                .replace("o", "")
                .replace("z", "");
    }

    private BigInteger capacity(String radixType, int length, boolean excludeAmbiguous) {
        if (length < 1 || length > 32) {
            throw new BusinessException("流水号长度必须在1到32之间");
        }
        return BigInteger.valueOf(alphabet(radixType, excludeAmbiguous).length()).pow(length);
    }
}

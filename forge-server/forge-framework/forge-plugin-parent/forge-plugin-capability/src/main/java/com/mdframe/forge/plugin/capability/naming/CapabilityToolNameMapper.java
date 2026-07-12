package com.mdframe.forge.plugin.capability.naming;

import com.mdframe.forge.plugin.capability.exception.CapabilityDefinitionException;
import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;

import java.util.regex.Pattern;

public final class CapabilityToolNameMapper {

    private static final int MAX_LENGTH = 128;
    private static final Pattern CAPABILITY_CODE_PATTERN = Pattern.compile(
            "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");

    public String toProtocolToolName(String capabilityCode) {
        if (capabilityCode == null || capabilityCode.isBlank()
                || capabilityCode.length() > MAX_LENGTH
                || !CAPABILITY_CODE_PATTERN.matcher(capabilityCode).matches()) {
            throw new CapabilityDefinitionException(CapabilityErrorCode.INVALID_ARGUMENT,
                    "能力编码必须是 1-128 位小写点分编码，每段以字母开头");
        }
        return capabilityCode;
    }
}

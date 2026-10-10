package com.mdframe.forge.starter.plugin.descriptor;

import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import com.mdframe.forge.starter.plugin.version.VersionRange;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** 安装器还需校验真实路径；运行时只接受明确、版本兼容的元数据。 */
public final class PluginDescriptorValidator {

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9-]{1,31}");
    private static final Pattern MODULE = Pattern.compile("[a-z][a-z0-9-]{1,63}");
    private static final Pattern FEATURE = Pattern.compile("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*");
    private static final Pattern UI_PATH = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_./-]*");
    private static final int MAX_FEATURE_LENGTH = 64;

    private PluginDescriptorValidator() {
    }

    public static void validate(PluginDescriptor descriptor, String coreVersion) {
        String context = context(descriptor, coreVersion);
        try {
            validateFields(descriptor);
            if (!VersionRange.parse(descriptor.requiresCore()).contains(coreVersion)) {
                throw new IllegalArgumentException("核心版本不兼容");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(context + "：" + exception.getMessage(), exception);
        }
    }

    public static String context(PluginDescriptor descriptor, String coreVersion) {
        if (descriptor == null) {
            return "插件 <unknown>，要求 <unknown>，当前核心 " + coreVersion;
        }
        return "插件 " + descriptor.id() + "，要求 " + descriptor.requiresCore() + "，当前核心 " + coreVersion;
    }

    private static void validateFields(PluginDescriptor descriptor) {
        require(descriptor != null, "描述不能为空");
        require(matches(ID, descriptor.id()), "id 必须为 2–32 位小写字母数字或短横线，且以字母开头");
        require(descriptor.name() != null && !descriptor.name().isBlank(), "name 不能为空");
        SemanticVersion.parse(descriptor.version());
        require(descriptor.edition() != null, "edition 不能为空");
        validateFeatures(descriptor);
        if (descriptor.server() != null) {
            require(matches(MODULE, descriptor.server().module()), "server.module 必须为安全的模块目录名");
        }
        if (descriptor.ui() != null) {
            String path = descriptor.ui().dir();
            require(matches(UI_PATH, path), "ui.dir 必须为相对目录");
            require(Arrays.stream(path.split("/", -1)).noneMatch(PluginDescriptorValidator::unsafeSegment),
                    "ui.dir 禁止空段、点段或路径穿越");
        }
    }

    private static void validateFeatures(PluginDescriptor descriptor) {
        require(descriptor.features() != null, "features 必须为数组");
        Set<String> unique = new HashSet<>();
        for (String feature : descriptor.features()) {
            require(matches(FEATURE, feature) && feature.length() <= MAX_FEATURE_LENGTH, "非法功能编码：" + feature);
            require(unique.add(feature), "功能编码重复：" + feature);
            boolean enterprise = descriptor.edition() == PluginEdition.ENTERPRISE;
            require(enterprise == feature.startsWith("ee."), "功能前缀与 edition 不一致：" + feature);
        }
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }

    private static boolean unsafeSegment(String segment) {
        return segment.isEmpty() || segment.equals(".") || segment.equals("..");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}

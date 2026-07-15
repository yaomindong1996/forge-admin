package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 白名单处理器的受限输入字段声明。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionInputField {

    private String type;

    private boolean required;

    public static ExtensionInputField required(String type) {
        return new ExtensionInputField(type, true);
    }

    public static ExtensionInputField optional(String type) {
        return new ExtensionInputField(type, false);
    }
}

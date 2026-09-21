package com.mdframe.forge.plugin.print.vo;

import java.util.List;

/**
 * 技术字段类型与前端打印字段目录一致，不是业务状态字典。
 */
public record PrintFieldCatalogVO(List<Field> fields) {

    public PrintFieldCatalogVO {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public record Field(String path, String label, String type) {
    }
}

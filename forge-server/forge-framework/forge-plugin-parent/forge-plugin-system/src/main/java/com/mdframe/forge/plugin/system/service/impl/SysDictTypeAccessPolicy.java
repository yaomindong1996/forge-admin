package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Locale;
import java.util.Objects;

/**
 * 字典类型保护：系统前缀 sys_ 禁删；已创建类型禁止改码。
 */
final class SysDictTypeAccessPolicy {

    private SysDictTypeAccessPolicy() {
    }

    static boolean isSystemDictType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            return false;
        }
        return dictType.trim().toLowerCase(Locale.ROOT).startsWith("sys_");
    }

    static boolean isSystemDictType(SysDictType type) {
        return type != null && isSystemDictType(type.getDictType());
    }

    static void assertDeletable(SysDictType existing) {
        if (isSystemDictType(existing)) {
            throw new BusinessException("系统内置字典不能删除：" + existing.getDictType());
        }
    }

    static void assertDictTypeCodeMutable(SysDictType existing, String nextDictType) {
        if (existing == null || StrUtil.isBlank(nextDictType)) {
            return;
        }
        if (!Objects.equals(existing.getDictType(), nextDictType.trim())) {
            throw new BusinessException("字典类型编码创建后不能修改，请新建类型");
        }
    }
}

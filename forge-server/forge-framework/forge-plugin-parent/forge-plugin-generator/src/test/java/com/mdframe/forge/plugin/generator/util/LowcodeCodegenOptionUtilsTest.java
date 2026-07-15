package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("低代码下载命名与路径设置")
class LowcodeCodegenOptionUtilsTest {

    @Test
    @DisplayName("实体前缀和表前缀统一派生业务类名")
    void buildsClassNameFromSharedNamingRule() {
        assertEquals("BizCustomer", LowcodeCodegenOptionUtils.buildClassName(
                "tf_customer", "biz", List.of("tf_", "t_")));
        assertEquals("TfCustomer", LowcodeCodegenOptionUtils.buildClassName(
                "tf_customer", "", List.of()));
    }

    @Test
    @DisplayName("空前缀列表与用户顺序会被保留")
    void preservesExplicitPrefixSelection() {
        assertEquals(List.of(), LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                List.of(), List.of("tf_"), LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES));
        assertEquals(List.of("tf_", "t_"), LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                null, List.of("tf_", "t_", "tf_"), LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES));
    }

    @Test
    @DisplayName("非法实体前缀和越界路径失败关闭")
    void rejectsInvalidNamingAndOutputPaths() {
        assertThrows(BusinessException.class,
                () -> LowcodeCodegenOptionUtils.normalizeEntityPrefix("biz-order"));
        assertThrows(BusinessException.class,
                () -> LowcodeCodegenOptionUtils.normalizeOutputPath(
                        "/tmp/source", LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径"));
        assertThrows(BusinessException.class,
                () -> LowcodeCodegenOptionUtils.normalizeOutputPath(
                        "backend/../source", LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径"));
    }
}

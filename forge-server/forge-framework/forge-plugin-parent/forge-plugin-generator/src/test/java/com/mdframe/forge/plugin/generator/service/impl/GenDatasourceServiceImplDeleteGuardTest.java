package com.mdframe.forge.plugin.generator.service.impl;

import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.mapper.GenDatasourceMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.vo.GenDatasourceReferenceHitVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenDatasourceServiceImpl 删除引用校验")
class GenDatasourceServiceImplDeleteGuardTest {

    @Mock
    private GenDatasourceMapper genDatasourceMapper;
    @Mock
    private RuntimeDatabaseDialectFactory dialectFactory;
    @Mock
    private LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;

    @InjectMocks
    private GenDatasourceServiceImpl service;

    @Test
    @DisplayName("存在运行配置引用时禁止删除")
    void blocksDeleteWhenReferenced() {
        GenDatasource datasource = datasource(183L, "main", "主库");
        GenDatasourceReferenceHitVO hit = new GenDatasourceReferenceHitVO();
        hit.setRefLabel("低代码运行配置");
        hit.setRefCount(12L);
        when(genDatasourceMapper.selectReferenceHits(eq(183L), eq("main"))).thenReturn(List.of(hit));

        BusinessException error = assertThrows(BusinessException.class, () -> service.assertNotReferenced(datasource));
        assertTrue(error.getMessage().contains("仍被引用"));
        assertTrue(error.getMessage().contains("低代码运行配置12 条"));
    }

    @Test
    @DisplayName("无引用时允许删除")
    void allowsDeleteWhenUnreferenced() {
        GenDatasource datasource = datasource(200L, "scratch", "临时库");
        GenDatasourceReferenceHitVO empty = new GenDatasourceReferenceHitVO();
        empty.setRefLabel("低代码运行配置");
        empty.setRefCount(0L);
        when(genDatasourceMapper.selectReferenceHits(eq(200L), eq("scratch"))).thenReturn(List.of(empty));

        assertDoesNotThrow(() -> service.assertNotReferenced(datasource));
    }

    @Test
    @DisplayName("编码为空时仍按 ID 检查")
    void checksByIdWhenCodeBlank() {
        GenDatasource datasource = datasource(9L, null, "未命名");
        when(genDatasourceMapper.selectReferenceHits(eq(9L), isNull())).thenReturn(List.of());

        assertDoesNotThrow(() -> service.assertNotReferenced(datasource));
    }

    private static GenDatasource datasource(Long id, String code, String name) {
        GenDatasource datasource = new GenDatasource();
        datasource.setDatasourceId(id);
        datasource.setDatasourceCode(code);
        datasource.setDatasourceName(name);
        return datasource;
    }
}

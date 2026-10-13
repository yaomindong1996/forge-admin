package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileGroupMapper;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SysFileMetadataControllerPermissionTest {

    private final ISysFileMetadataService fileMetadataService = mock(ISysFileMetadataService.class);
    private final SysFileGroupMapper fileGroupMapper = mock(SysFileGroupMapper.class);
    private final SysFileMetadataController controller =
            new SysFileMetadataController(fileMetadataService, fileGroupMapper);

    @Test
    void byFileIdShouldSurfaceForbiddenFromService() throws Exception {
        when(fileMetadataService.getByFileId("secret-file"))
                .thenThrow(new BusinessException(403, "无权访问该文件"));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestAdvice())
                .build();

        mvc.perform(get("/system/file/metadata/byFileId/secret-file"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权访问该文件"));
    }

    @Test
    void detailShouldReturnReadableMetadata() throws Exception {
        SysFileMetadata metadata = new SysFileMetadata();
        metadata.setId(12L);
        metadata.setFileId("ok-file");
        metadata.setOriginalName("a.png");
        when(fileMetadataService.getReadableById(12L)).thenReturn(metadata);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/system/file/metadata/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileId").value("ok-file"))
                .andExpect(jsonPath("$.data.originalName").value("a.png"));
    }

    @org.springframework.web.bind.annotation.RestControllerAdvice
    static class TestAdvice {
        @org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
        public com.mdframe.forge.starter.core.domain.RespInfo<?> handle(BusinessException ex) {
            return com.mdframe.forge.starter.core.domain.RespInfo.error(ex.getCode(), ex.getMessage());
        }
    }
}

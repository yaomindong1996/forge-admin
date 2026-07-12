package com.mdframe.forge.plugin.ai.model.service;

import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.model.capability.mapper.AiModelCapabilityMapper;
import com.mdframe.forge.plugin.ai.health.AiModelHealthRegistry;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelServiceTest {

    @Mock
    private AiModelMapper modelMapper;

    @Mock
    private AiModelCapabilityMapper capabilityMapper;

    @Mock
    private AiModelHealthRegistry healthRegistry;

    private AiModelService service;

    @BeforeEach
    void setUp() {
        service = new AiModelService(capabilityMapper, healthRegistry);
        ReflectionTestUtils.setField(service, "baseMapper", modelMapper);
    }

    @Test
    void requireEnabledDefaultModelIdShouldReturnAuthoritativeModel() {
        when(modelMapper.selectEnabledDefaultModelId(10L)).thenReturn("deepseek-v4-pro");

        String modelId = service.requireEnabledDefaultModelId(10L);

        assertEquals("deepseek-v4-pro", modelId);
    }

    @Test
    void requireEnabledDefaultModelIdShouldFailWhenMissing() {
        when(modelMapper.selectEnabledDefaultModelId(10L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.requireEnabledDefaultModelId(10L));

        assertEquals("请为供应商设置默认模型", exception.getMessage());
    }
}

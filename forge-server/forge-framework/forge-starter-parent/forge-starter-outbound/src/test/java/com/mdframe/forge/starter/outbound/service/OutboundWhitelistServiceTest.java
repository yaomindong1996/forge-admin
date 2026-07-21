package com.mdframe.forge.starter.outbound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistQuery;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistSaveRequest;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.service.impl.OutboundWhitelistServiceImpl;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboundWhitelistServiceTest {

    private SysOutboundWhitelistMapper mapper;
    private OutboundWhitelistServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysOutboundWhitelistMapper.class);
        service = new OutboundWhitelistServiceImpl(mapper, new OutboundHostNormalizer());
    }

    @Test
    void shouldNormalizeAndCreateFlowPrivateRuleInPlatformTenant() {
        when(mapper.countOverlappingRules(anyLong(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), isNull())).thenReturn(0L);
        when(mapper.insert(any(SysOutboundWhitelist.class))).thenReturn(1);

        SysOutboundWhitelist created = service.create(request(
                " flow_api ", "HTTPS", "Example.COM.", 443, 443, 1));

        ArgumentCaptor<SysOutboundWhitelist> captor = ArgumentCaptor.forClass(SysOutboundWhitelist.class);
        verify(mapper).insert(captor.capture());
        SysOutboundWhitelist entity = captor.getValue();
        assertEquals(1L, entity.getTenantId());
        assertEquals(OutboundScenes.FLOW_API, entity.getScene());
        assertEquals("https", entity.getProtocol());
        assertEquals("example.com", entity.getHost());
        assertEquals(1, entity.getAllowPrivate());
        assertEquals(1, entity.getStatus());
        assertEquals(entity, created);
    }

    @Test
    void shouldRejectPrivateExceptionForJobWebhook() {
        OutboundWhitelistSaveRequest request = request(
                OutboundScenes.JOB_WEBHOOK, "https", "hooks.example.com", 443, 443, 1);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("JOB_WEBHOOK场景禁止私网例外", exception.getMessage());
        verify(mapper, never()).insert(any(SysOutboundWhitelist.class));
    }

    @Test
    void shouldRejectInvalidOrOverlappingPortRange() {
        assertThrows(BusinessException.class, () -> service.create(request(
                OutboundScenes.FLOW_API, "https", "api.example.com", 8443, 443, 0)));

        when(mapper.countOverlappingRules(anyLong(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), isNull())).thenReturn(1L);
        BusinessException overlap = assertThrows(BusinessException.class, () -> service.create(request(
                OutboundScenes.FLOW_API, "https", "api.example.com", 443, 8443, 0)));

        assertEquals("相同场景、协议和主机已存在重叠端口规则", overlap.getMessage());
    }

    @Test
    void shouldRejectUpdateWhenRuleDoesNotExist() {
        OutboundWhitelistSaveRequest request = request(
                OutboundScenes.FLOW_API, "https", "api.example.com", 443, 443, 0);
        request.setId(99L);
        when(mapper.selectByIdForTenant(1L, 99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(request));

        assertEquals("出站白名单不存在", exception.getMessage());
        verify(mapper, never()).updateRule(any());
    }

    @Test
    void shouldUpdateRuleAndExcludeCurrentIdFromOverlapCheck() {
        OutboundWhitelistSaveRequest request = request(
                OutboundScenes.FLOW_API, "HTTPS", "API.Example.COM.", 443, 8443, 0);
        request.setId(99L);
        when(mapper.selectByIdForTenant(1L, 99L)).thenReturn(new SysOutboundWhitelist());
        when(mapper.countOverlappingRules(1L, OutboundScenes.FLOW_API, "https", "api.example.com",
                443, 8443, 99L)).thenReturn(0L);
        when(mapper.updateRule(any())).thenReturn(1);

        SysOutboundWhitelist updated = service.update(request);

        ArgumentCaptor<SysOutboundWhitelist> captor = ArgumentCaptor.forClass(SysOutboundWhitelist.class);
        verify(mapper).updateRule(captor.capture());
        assertEquals(99L, captor.getValue().getId());
        assertEquals("api.example.com", captor.getValue().getHost());
        assertEquals(captor.getValue(), updated);
    }

    @Test
    void shouldUsePlatformTenantForPageAndLogicalDelete() {
        OutboundWhitelistQuery query = new OutboundWhitelistQuery();
        query.setPageNum(2);
        query.setPageSize(20);
        Page<SysOutboundWhitelist> expected = new Page<>(2, 20);
        when(mapper.selectWhitelistPage(any(), anyLong(), any())).thenReturn(expected);
        when(mapper.selectByIdForTenant(1L, 7L)).thenReturn(new SysOutboundWhitelist());
        when(mapper.logicDelete(1L, 7L)).thenReturn(1);

        assertEquals(expected, service.page(query));
        service.delete(7L);

        ArgumentCaptor<Page<SysOutboundWhitelist>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(mapper).selectWhitelistPage(pageCaptor.capture(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(query));
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(20L, pageCaptor.getValue().getSize());
        verify(mapper).logicDelete(1L, 7L);
    }

    private OutboundWhitelistSaveRequest request(String scene, String protocol, String host,
                                                   int portStart, int portEnd, int allowPrivate) {
        OutboundWhitelistSaveRequest request = new OutboundWhitelistSaveRequest();
        request.setScene(scene);
        request.setProtocol(protocol);
        request.setHost(host);
        request.setPortStart(portStart);
        request.setPortEnd(portEnd);
        request.setAllowPrivate(allowPrivate);
        request.setStatus(1);
        request.setRemark("test");
        return request;
    }
}

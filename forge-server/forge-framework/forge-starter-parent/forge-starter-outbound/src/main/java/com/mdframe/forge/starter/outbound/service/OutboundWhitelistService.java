package com.mdframe.forge.starter.outbound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistQuery;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistSaveRequest;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;

public interface OutboundWhitelistService {

    Page<SysOutboundWhitelist> page(OutboundWhitelistQuery query);

    SysOutboundWhitelist getById(Long id);

    SysOutboundWhitelist create(OutboundWhitelistSaveRequest request);

    SysOutboundWhitelist update(OutboundWhitelistSaveRequest request);

    void delete(Long id);
}

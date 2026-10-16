package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.starter.flow.dto.FlowEntryDTO;
import com.mdframe.forge.starter.flow.dto.FlowEntryQueryDTO;
import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.vo.FlowEntryRuntimeVO;

/**
 * 流程入口服务。
 */
public interface FlowEntryService extends IService<FlowEntry> {

    IPage<FlowEntry> pageEntries(FlowEntryQueryDTO query, Integer pageNum, Integer pageSize);

    FlowEntry getEntryDetail(Long id);

    FlowEntry getByEntryCode(String entryCode);

    FlowEntryRuntimeVO getRuntimeEntry(String entryCode);

    void saveEntry(FlowEntryDTO dto);

    void deleteEntry(Long id);
}

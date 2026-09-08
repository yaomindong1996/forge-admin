package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseCreateDTO;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseQuery;
import com.mdframe.forge.starter.flow.dto.FlowCommentPhraseUpdateDTO;
import com.mdframe.forge.starter.flow.vo.FlowCommentPhraseVO;

import java.util.List;

/** 常用审批意见。 */
public interface FlowCommentPhraseService {

    List<FlowCommentPhraseVO> listUsable(String scene);

    List<FlowCommentPhraseVO> listMine();

    IPage<FlowCommentPhraseVO> page(FlowCommentPhraseQuery query);

    FlowCommentPhraseVO getById(Long id);

    FlowCommentPhraseVO create(FlowCommentPhraseCreateDTO request);

    FlowCommentPhraseVO update(FlowCommentPhraseUpdateDTO request);

    void delete(Long id);
}

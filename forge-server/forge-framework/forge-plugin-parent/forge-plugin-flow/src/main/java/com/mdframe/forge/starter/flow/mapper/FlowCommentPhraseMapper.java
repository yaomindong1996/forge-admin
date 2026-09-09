package com.mdframe.forge.starter.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.entity.FlowCommentPhrase;
import com.mdframe.forge.starter.flow.vo.FlowCommentPhraseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 常用审批意见 Mapper。 */
@Mapper
public interface FlowCommentPhraseMapper extends BaseMapper<FlowCommentPhrase> {

    IPage<FlowCommentPhraseVO> selectPageByOwner(Page<FlowCommentPhraseVO> page,
                                                 @Param("tenantId") Long tenantId,
                                                 @Param("ownerType") Integer ownerType,
                                                 @Param("userId") Long userId,
                                                 @Param("keyword") String keyword,
                                                 @Param("scene") String scene,
                                                 @Param("status") Integer status);

    List<FlowCommentPhraseVO> selectUsable(@Param("tenantId") Long tenantId,
                                           @Param("userId") Long userId,
                                           @Param("scene") String scene);

    List<FlowCommentPhraseVO> selectMine(@Param("tenantId") Long tenantId,
                                         @Param("userId") Long userId);

    FlowCommentPhraseVO selectVoByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    FlowCommentPhrase selectByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

    int countByContent(@Param("tenantId") Long tenantId,
                       @Param("ownerType") Integer ownerType,
                       @Param("userId") Long userId,
                       @Param("scene") String scene,
                       @Param("content") String content,
                       @Param("excludeId") Long excludeId);

    int countByOwner(@Param("tenantId") Long tenantId,
                     @Param("ownerType") Integer ownerType,
                     @Param("userId") Long userId);

    int updateByIdAndTenant(@Param("phrase") FlowCommentPhrase phrase, @Param("tenantId") Long tenantId);

    int logicallyDeleteByIdAndTenant(@Param("id") Long id,
                                     @Param("tenantId") Long tenantId,
                                     @Param("operatorId") Long operatorId);
}

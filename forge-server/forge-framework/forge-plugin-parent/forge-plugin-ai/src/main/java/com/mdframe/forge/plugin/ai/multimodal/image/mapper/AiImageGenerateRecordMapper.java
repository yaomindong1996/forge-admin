package com.mdframe.forge.plugin.ai.multimodal.image.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI图片生成记录 Mapper
 */
@Mapper
public interface AiImageGenerateRecordMapper extends BaseMapper<AiImageGenerateRecord> {

    int transition(@Param("record") AiImageGenerateRecord record, @Param("expectedStatus") String expectedStatus);

    Page<AiImageGenerateRecord> selectRecordPage(
            Page<AiImageGenerateRecord> page,
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("status") String status);
}

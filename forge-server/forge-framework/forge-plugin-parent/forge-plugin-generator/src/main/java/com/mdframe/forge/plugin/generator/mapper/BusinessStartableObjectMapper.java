package com.mdframe.forge.plugin.generator.mapper;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessStartableObjectMapper {

    List<BusinessStartableObjectVO> selectStartableObjects(@Param("tenantId") Long tenantId);
}

package com.mdframe.forge.plugin.generator.mapper;

import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableProcessRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessStartableObjectMapper {

    List<BusinessStartableObjectVO> selectStartableObjects(@Param("tenantId") Long tenantId);

    List<BusinessStartableProcessRow> selectStartableProcesses(@Param("tenantId") Long tenantId);
}

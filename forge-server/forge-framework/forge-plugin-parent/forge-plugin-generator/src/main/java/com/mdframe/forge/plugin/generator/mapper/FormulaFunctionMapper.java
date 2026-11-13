package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunction;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionInstall;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionVersion;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketQueryDTO;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FormulaFunctionMapper extends BaseMapper<AiFormulaFunction> {

    Page<AiFormulaFunction> selectFormulaFunctionPage(Page<AiFormulaFunction> page,
                                                      @Param("tenantId") Long tenantId,
                                                      @Param("keyword") String keyword,
                                                      @Param("category") String category,
                                                      @Param("sourceType") String sourceType,
                                                      @Param("status") String status);

    AiFormulaFunction selectFormulaFunctionByCode(@Param("tenantId") Long tenantId,
                                                  @Param("functionCode") String functionCode);

    List<AiFormulaFunctionVersion> selectFunctionVersions(@Param("tenantId") Long tenantId,
                                                          @Param("functionCode") String functionCode);

    AiFormulaFunctionInstall selectFunctionInstall(@Param("tenantId") Long tenantId,
                                                   @Param("functionCode") String functionCode);

    Page<FormulaFunctionMarketResponse> selectFormulaFunctionMarketPage(
            Page<FormulaFunctionMarketResponse> page,
            @Param("tenantId") Long tenantId,
            @Param("query") FormulaFunctionMarketQueryDTO query);

    FormulaFunctionMarketResponse selectFormulaFunctionMarketDetail(@Param("tenantId") Long tenantId,
                                                                    @Param("functionCode") String functionCode);

    List<FormulaFunctionMarketResponse> selectEnabledInstalledFunctions(@Param("tenantId") Long tenantId);

    List<FormulaFunctionMarketResponse> selectInstalledFunctionDefinitions(@Param("tenantId") Long tenantId);

    int insertFormulaFunction(AiFormulaFunction function);

    int updateFormulaFunctionByUk(AiFormulaFunction function);

    int insertFunctionVersion(AiFormulaFunctionVersion version);

    int updateFunctionVersionByUk(AiFormulaFunctionVersion version);

    int insertFunctionInstall(AiFormulaFunctionInstall install);

    int updateFunctionInstallByUk(AiFormulaFunctionInstall install);
}

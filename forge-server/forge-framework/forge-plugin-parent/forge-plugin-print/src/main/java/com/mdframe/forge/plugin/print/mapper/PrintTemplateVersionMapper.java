package com.mdframe.forge.plugin.print.mapper;

import com.mdframe.forge.plugin.print.entity.PrintTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 所有调用须先做来源授权；多语句写入由服务事务及应用/模板行锁串行化。
 */
@Mapper
public interface PrintTemplateVersionMapper {

    int insert(PrintTemplateVersion row);

    PrintTemplateVersion selectScoped(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId, @Param("id") Long id);

    List<PrintTemplateVersion> selectVersions(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

    Integer selectMaxVersionNo(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);
}

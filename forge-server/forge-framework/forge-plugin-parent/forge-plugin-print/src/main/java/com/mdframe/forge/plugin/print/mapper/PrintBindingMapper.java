package com.mdframe.forge.plugin.print.mapper;

import com.mdframe.forge.plugin.print.entity.PrintBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 所有调用须先做来源授权；多语句写入由服务事务及应用/模板行锁串行化。
 */
@Mapper
public interface PrintBindingMapper {

    int insert(PrintBinding row);

    PrintBinding selectScoped(@Param("tenantId") Long tenantId, @Param("id") Long id);

    List<PrintBinding> selectSource(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId, @Param("sourceKey") String sourceKey, @Param("scene") String scene);

    /** 同一来源下全部场景绑定（管理面板汇总）。 */
    List<PrintBinding> selectBySource(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId, @Param("sourceKey") String sourceKey);

    /** 候选发布清单：应用行锁内使用当前读，避免读取外层事务的旧一致性快照。 */
    List<PrintBinding> selectApplication(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId);

    /** 设计预览/管理态读取启用中的绑定，不加行锁。 */
    List<PrintBinding> selectApplicationEnabled(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId);

    long countTemplateReferences(@Param("tenantId") Long tenantId, @Param("templateId") Long templateId);

    int clearDefault(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId, @Param("sourceKey") String sourceKey, @Param("scene") String scene, @Param("actor") Long actor);

    int updateBinding(@Param("row") PrintBinding row, @Param("expectedRevision") Long expectedRevision);

    int softDelete(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("expectedRevision") Long expectedRevision, @Param("actor") Long actor);
}

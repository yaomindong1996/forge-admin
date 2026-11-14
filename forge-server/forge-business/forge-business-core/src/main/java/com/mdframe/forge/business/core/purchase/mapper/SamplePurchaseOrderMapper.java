package com.mdframe.forge.business.core.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.business.core.purchase.domain.SamplePurchaseOrder;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购单审批测试 Mapper。
 */
@Mapper
public interface SamplePurchaseOrderMapper extends BaseMapper<SamplePurchaseOrder> {

    IPage<SamplePurchaseOrderVO> selectPage(Page<SamplePurchaseOrderVO> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("query") SamplePurchaseOrderQuery query);

    SamplePurchaseOrderVO selectDetail(@Param("tenantId") Long tenantId,
                                       @Param("id") Long id);

    List<SamplePurchaseOrderVO> selectDetailsByIds(@Param("tenantId") Long tenantId,
                                                   @Param("ids") Collection<Long> ids);

    SamplePurchaseOrder selectByBusinessKey(@Param("tenantId") Long tenantId,
                                            @Param("businessKey") String businessKey);

    List<Map<String, Object>> selectActiveTaskDefKeysByBusinessKeys(@Param("tenantId") Long tenantId,
                                                                    @Param("businessKeys") Collection<String> businessKeys);

    /** 批量查询用户展示名，供会签人姓名 Java 聚合。 */
    List<Map<String, Object>> selectUserDisplayNames(@Param("userIds") Collection<Long> userIds);
}

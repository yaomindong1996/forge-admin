package com.mdframe.forge.business.core.purchase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 采购单审批测试业务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sample_purchase_order")
public class SamplePurchaseOrder extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private String title;

    private String supplierName;

    private Long amountCent;

    private String purchaseItems;

    private LocalDate needDate;

    private String status;

    @TableLogic
    private String delFlag;

    private Long applicantId;

    private String applicantName;

    private Long applicantDeptId;

    private String applicantDeptName;

    private String businessKey;

    private String processInstanceId;

    private Long deptLeaderId;

    private Long engineeringManagerId;

    private String countersignUserIds;

    private String ccRoleKeys;

    private String arrivalListFileIds;

    private String applicantModifyRemark;

    private String deptLeaderRemark;

    private String engineeringManagerRemark;

    private String countersignRemark;

    private String rejectReason;

    private String remark;
}

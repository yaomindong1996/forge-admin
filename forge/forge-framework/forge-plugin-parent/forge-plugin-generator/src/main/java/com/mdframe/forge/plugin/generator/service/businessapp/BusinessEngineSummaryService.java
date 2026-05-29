package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessEngineSummaryVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 引擎运行状态汇总服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessEngineSummaryService {

    private final BusinessBindingMapper bindingMapper;

    /**
     * 查询引擎运行状态汇总
     *
     * @return 引擎汇总列表
     */
    public List<BusinessEngineSummaryVO> summary() {
        Long tenantId = resolveTenantId();

        List<BusinessEngineSummaryVO> result = new ArrayList<>();

        // 审批引擎
        result.add(buildEngineSummary(tenantId, "APPROVAL", "审批引擎", "ionicons5:CheckmarkDoneOutline"));

        // 报表引擎
        result.add(buildEngineSummary(tenantId, "REPORT", "报表引擎", "ionicons5:BarChartOutline"));

        // 消息引擎
        result.add(buildEngineSummary(tenantId, "MESSAGE", "消息引擎", "ionicons5:MailOutline"));

        // 权限引擎
        result.add(buildEngineSummary(tenantId, "PERMISSION", "权限引擎", "ionicons5:LockClosedOutline"));

        // 触发器引擎
        result.add(buildEngineSummary(tenantId, "TRIGGER", "触发器引擎", "ionicons5:FlashOutline"));

        // 导入导出引擎
        result.add(buildEngineSummary(tenantId, "IMPORT_EXPORT", "导入导出", "ionicons5:SwapHorizontalOutline"));

        return result;
    }

    private BusinessEngineSummaryVO buildEngineSummary(Long tenantId, String engineType, String engineName, String engineIcon) {
        BusinessEngineSummaryVO vo = new BusinessEngineSummaryVO();
        vo.setEngineType(engineType);
        vo.setEngineName(engineName);
        vo.setEngineIcon(engineIcon);

        // 查询该类型能力的挂接统计
        Map<String, Object> stats = bindingMapper.selectBindingStatsByCapabilityType(tenantId, engineType);

        int totalCount = stats != null && stats.get("totalCount") != null 
                ? ((Number) stats.get("totalCount")).intValue() : 0;
        int runnableCount = stats != null && stats.get("runnableCount") != null 
                ? ((Number) stats.get("runnableCount")).intValue() : 0;

        vo.setTotalCount(totalCount);
        vo.setRunnableCount(runnableCount);
        vo.setPendingCount(totalCount - runnableCount);
        vo.setErrorCount(0);

        // 计算状态
        if (totalCount == 0) {
            vo.setStatus("MISSING");
            vo.setStatusLabel("未接入");
            vo.setMessage(engineName + "暂无接入配置");
        } else if (runnableCount == totalCount) {
            vo.setStatus("RUNNABLE");
            vo.setStatusLabel("已就绪");
            vo.setMessage(engineName + "已全部就绪");
        } else if (runnableCount > 0) {
            vo.setStatus("PARTIAL");
            vo.setStatusLabel("部分就绪");
            vo.setMessage(engineName + " " + runnableCount + "/" + totalCount + " 已就绪");
        } else {
            vo.setStatus("MISSING");
            vo.setStatusLabel("待配置");
            vo.setMessage(engineName + "需要完成配置才能运行");
        }

        return vo;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}

package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessReadinessStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessSuiteMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteAcceptanceVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 业务套件验收服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessSuiteAcceptanceService {

    private final BusinessSuiteMapper businessSuiteMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final BusinessObjectReadinessService businessObjectReadinessService;

    /**
     * 查询业务套件验收状态
     *
     * @param suiteCode 业务套件编码
     * @return 验收状态信息
     */
    public BusinessSuiteAcceptanceVO acceptance(String suiteCode) {
        Long tenantId = resolveTenantId();

        // 查询业务套件
        AiBusinessSuite suite = businessSuiteMapper.selectBySuiteCode(tenantId, suiteCode);
        if (suite == null) {
            throw new BusinessException("业务套件不存在");
        }

        BusinessSuiteAcceptanceVO vo = new BusinessSuiteAcceptanceVO();
        vo.setSuiteCode(suite.getSuiteCode());
        vo.setSuiteName(suite.getSuiteName());

        // 查询套件下的所有对象
        List<AiBusinessObject> objects = businessObjectMapper.selectBySuiteCode(tenantId, suiteCode)
                .stream()
                .filter(object -> isAcceptanceObject(suiteCode, object.getObjectCode()))
                .toList();

        // 计算对象验收状态
        List<BusinessSuiteAcceptanceVO.ObjectAcceptanceVO> objectAcceptances = new ArrayList<>();
        int runnableCount = 0;
        int totalCount = objects.size();

        for (AiBusinessObject object : objects) {
            BusinessSuiteAcceptanceVO.ObjectAcceptanceVO objectAcceptance = new BusinessSuiteAcceptanceVO.ObjectAcceptanceVO();
            objectAcceptance.setObjectCode(object.getObjectCode());
            objectAcceptance.setObjectName(object.getObjectName());

            try {
                BusinessObjectReadinessVO readiness = businessObjectReadinessService.readiness(object.getId());
                objectAcceptance.setReadinessStatus(readiness.getOverallStatus());
                objectAcceptance.setRunnable(BusinessReadinessStatus.RUNNABLE.equals(readiness.getOverallStatus()));
                objectAcceptance.setMessage(getObjectStatusMessage(readiness.getOverallStatus()));
                objectAcceptance.setStatusLabel(getStatusLabel(readiness.getOverallStatus()));

                if (BusinessReadinessStatus.RUNNABLE.equals(readiness.getOverallStatus())) {
                    runnableCount++;
                }
            } catch (Exception e) {
                log.warn("检查对象就绪度失败: {}", object.getObjectCode(), e);
                objectAcceptance.setReadinessStatus(BusinessReadinessStatus.ERROR);
                objectAcceptance.setRunnable(false);
                objectAcceptance.setMessage("检查失败: " + e.getMessage());
                objectAcceptance.setStatusLabel("异常");
            }

            objectAcceptances.add(objectAcceptance);
        }

        vo.setObjects(objectAcceptances);

        // 计算引擎能力验收状态
        List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> engineAcceptances = calculateEngineAcceptance(tenantId, suiteCode);
        vo.setEngines(engineAcceptances);

        // 计算渠道验收状态
        List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> channelAcceptances = calculateChannelAcceptance(tenantId, suiteCode);
        vo.setChannels(channelAcceptances);

        // 计算整体验收状态
        String overallStatus = calculateOverallStatus(objectAcceptances, engineAcceptances, channelAcceptances);
        vo.setOverallStatus(overallStatus);

        // 计算评分
        int score = calculateScore(objectAcceptances, engineAcceptances, channelAcceptances);
        vo.setScore(score);

        // 设置下一步操作
        setNextAction(vo, objectAcceptances, engineAcceptances, channelAcceptances);

        return vo;
    }

    private List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> calculateEngineAcceptance(Long tenantId, String suiteCode) {
        List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> engines = new ArrayList<>();

        // 审批引擎
        BusinessSuiteAcceptanceVO.EngineAcceptanceVO approvalEngine = new BusinessSuiteAcceptanceVO.EngineAcceptanceVO();
        approvalEngine.setEngineType("APPROVAL");
        approvalEngine.setEngineName("审批引擎");
        approvalEngine.setTotalCount(0);
        approvalEngine.setRunnableCount(0);
        approvalEngine.setPendingCount(0);
        approvalEngine.setErrorCount(0);
        approvalEngine.setStatus(BusinessReadinessStatus.MISSING);
        engines.add(approvalEngine);

        // 报表引擎
        BusinessSuiteAcceptanceVO.EngineAcceptanceVO reportEngine = new BusinessSuiteAcceptanceVO.EngineAcceptanceVO();
        reportEngine.setEngineType("REPORT");
        reportEngine.setEngineName("报表引擎");
        reportEngine.setTotalCount(0);
        reportEngine.setRunnableCount(0);
        reportEngine.setPendingCount(0);
        reportEngine.setErrorCount(0);
        reportEngine.setStatus(BusinessReadinessStatus.MISSING);
        engines.add(reportEngine);

        // 消息引擎
        BusinessSuiteAcceptanceVO.EngineAcceptanceVO messageEngine = new BusinessSuiteAcceptanceVO.EngineAcceptanceVO();
        messageEngine.setEngineType("MESSAGE");
        messageEngine.setEngineName("消息引擎");
        messageEngine.setTotalCount(0);
        messageEngine.setRunnableCount(0);
        messageEngine.setPendingCount(0);
        messageEngine.setErrorCount(0);
        messageEngine.setStatus(BusinessReadinessStatus.MISSING);
        engines.add(messageEngine);

        // 权限引擎
        BusinessSuiteAcceptanceVO.EngineAcceptanceVO permissionEngine = new BusinessSuiteAcceptanceVO.EngineAcceptanceVO();
        permissionEngine.setEngineType("PERMISSION");
        permissionEngine.setEngineName("权限引擎");
        permissionEngine.setTotalCount(0);
        permissionEngine.setRunnableCount(0);
        permissionEngine.setPendingCount(0);
        permissionEngine.setErrorCount(0);
        permissionEngine.setStatus(BusinessReadinessStatus.MISSING);
        engines.add(permissionEngine);

        // 触发器引擎
        BusinessSuiteAcceptanceVO.EngineAcceptanceVO triggerEngine = new BusinessSuiteAcceptanceVO.EngineAcceptanceVO();
        triggerEngine.setEngineType("TRIGGER");
        triggerEngine.setEngineName("触发器引擎");
        triggerEngine.setTotalCount(0);
        triggerEngine.setRunnableCount(0);
        triggerEngine.setPendingCount(0);
        triggerEngine.setErrorCount(0);
        triggerEngine.setStatus(BusinessReadinessStatus.MISSING);
        engines.add(triggerEngine);

        return engines;
    }

    private List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> calculateChannelAcceptance(Long tenantId, String suiteCode) {
        List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> channels = new ArrayList<>();

        // 移动端渠道
        BusinessSuiteAcceptanceVO.ChannelAcceptanceVO mobileChannel = new BusinessSuiteAcceptanceVO.ChannelAcceptanceVO();
        mobileChannel.setChannelType("MOBILE");
        mobileChannel.setChannelName("移动端");
        mobileChannel.setTotalCount(0);
        mobileChannel.setAvailableCount(0);
        mobileChannel.setStatus(BusinessReadinessStatus.MISSING);
        channels.add(mobileChannel);

        // 嵌入应用渠道
        BusinessSuiteAcceptanceVO.ChannelAcceptanceVO embeddedChannel = new BusinessSuiteAcceptanceVO.ChannelAcceptanceVO();
        embeddedChannel.setChannelType("EMBEDDED");
        embeddedChannel.setChannelName("嵌入应用");
        embeddedChannel.setTotalCount(0);
        embeddedChannel.setAvailableCount(0);
        embeddedChannel.setStatus(BusinessReadinessStatus.MISSING);
        channels.add(embeddedChannel);

        // 集成渠道
        BusinessSuiteAcceptanceVO.ChannelAcceptanceVO integrationChannel = new BusinessSuiteAcceptanceVO.ChannelAcceptanceVO();
        integrationChannel.setChannelType("INTEGRATION");
        integrationChannel.setChannelName("第三方集成");
        integrationChannel.setTotalCount(0);
        integrationChannel.setAvailableCount(0);
        integrationChannel.setStatus(BusinessReadinessStatus.MISSING);
        channels.add(integrationChannel);

        return channels;
    }

    private boolean isAcceptanceObject(String suiteCode, String objectCode) {
        if (!"CRM".equalsIgnoreCase(suiteCode)) {
            return true;
        }
        Set<String> coreObjects = Set.of("CUSTOMER", "CONTACT", "OPPORTUNITY", "CONTRACT", "PAYMENT");
        return coreObjects.contains(String.valueOf(objectCode).toUpperCase());
    }

    private String calculateOverallStatus(
            List<BusinessSuiteAcceptanceVO.ObjectAcceptanceVO> objects,
            List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> engines,
            List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> channels) {
        
        boolean allObjectsRunnable = objects.stream()
                .allMatch(obj -> BusinessReadinessStatus.RUNNABLE.equals(obj.getReadinessStatus()));
        
        boolean anyObjectMissing = objects.stream()
                .anyMatch(obj -> BusinessReadinessStatus.MISSING.equals(obj.getReadinessStatus()));

        if (allObjectsRunnable) {
            return BusinessReadinessStatus.PASSED;
        } else if (anyObjectMissing) {
            return BusinessReadinessStatus.FAILED;
        } else {
            return BusinessReadinessStatus.PARTIAL;
        }
    }

    private int calculateScore(
            List<BusinessSuiteAcceptanceVO.ObjectAcceptanceVO> objects,
            List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> engines,
            List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> channels) {
        
        if (objects.isEmpty()) {
            return 0;
        }

        int totalScore = 0;
        int maxScore = objects.size() * 100;

        for (BusinessSuiteAcceptanceVO.ObjectAcceptanceVO obj : objects) {
            String status = obj.getReadinessStatus();
            if (BusinessReadinessStatus.RUNNABLE.equals(status)) {
                totalScore += 100;
            } else if (BusinessReadinessStatus.CONFIGURED.equals(status)) {
                totalScore += 75;
            } else if (BusinessReadinessStatus.REGISTERED.equals(status)) {
                totalScore += 50;
            } else if (BusinessReadinessStatus.ERROR.equals(status)) {
                totalScore += 25;
            }
        }

        return maxScore > 0 ? (totalScore * 100 / maxScore) : 0;
    }

    private void setNextAction(
            BusinessSuiteAcceptanceVO vo,
            List<BusinessSuiteAcceptanceVO.ObjectAcceptanceVO> objects,
            List<BusinessSuiteAcceptanceVO.EngineAcceptanceVO> engines,
            List<BusinessSuiteAcceptanceVO.ChannelAcceptanceVO> channels) {
        
        if (BusinessReadinessStatus.PASSED.equals(vo.getOverallStatus())) {
            vo.setNextAction("VIEW_ACCEPTANCE_REPORT");
            vo.setNextActionLabel("查看验收报告");
        } else {
            // 找到第一个未就绪的对象
            for (BusinessSuiteAcceptanceVO.ObjectAcceptanceVO obj : objects) {
                if (!BusinessReadinessStatus.RUNNABLE.equals(obj.getReadinessStatus())) {
                    vo.setNextAction("FIX_OBJECT");
                    vo.setNextActionLabel("修复 " + obj.getObjectName());
                    break;
                }
            }
        }
    }

    private String getObjectStatusMessage(String status) {
        switch (status) {
            case BusinessReadinessStatus.RUNNABLE:
                return "可运行";
            case BusinessReadinessStatus.CONFIGURED:
                return "已配置，待发布";
            case BusinessReadinessStatus.REGISTERED:
                return "已登记，待配置";
            case BusinessReadinessStatus.MISSING:
                return "缺少必要配置";
            case BusinessReadinessStatus.ERROR:
                return "配置异常";
            default:
                return "未知状态";
        }
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case BusinessReadinessStatus.RUNNABLE:
                return "可运行";
            case BusinessReadinessStatus.CONFIGURED:
                return "已配置";
            case BusinessReadinessStatus.REGISTERED:
                return "已登记";
            case BusinessReadinessStatus.MISSING:
                return "未配置";
            case BusinessReadinessStatus.ERROR:
                return "异常";
            default:
                return "未知";
        }
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

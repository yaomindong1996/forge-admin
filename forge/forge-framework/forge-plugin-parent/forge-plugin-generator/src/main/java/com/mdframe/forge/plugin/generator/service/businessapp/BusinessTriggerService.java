package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTriggerLog;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerLogMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTriggerScenarioTemplateVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 触发器管理服务。
 * <p>
 * 负责触发器的 CRUD、启停操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessTriggerService {

    private final BusinessTriggerMapper triggerMapper;
    private final BusinessTriggerLogMapper triggerLogMapper;

    /**
     * 分页查询触发器
     */
    public Page<AiBusinessTrigger> selectPage(String objectCode, PageQuery pageQuery) {
        return selectPage(objectCode, null, pageQuery);
    }

    /**
     * 分页查询触发器
     */
    public Page<AiBusinessTrigger> selectPage(String objectCode, String scenarioType, PageQuery pageQuery) {
        Long tenantId = resolveTenantId();
        Page<AiBusinessTrigger> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return triggerMapper.selectTriggerPage(page, tenantId, StringUtils.trimToNull(objectCode),
                StringUtils.trimToNull(scenarioType));
    }

    /**
     * 查询触发器详情
     */
    public AiBusinessTrigger selectById(Long id) {
        return triggerMapper.selectById(id);
    }

    /**
     * 新增触发器
     */
    @Transactional(rollbackFor = Exception.class)
    public void insert(AiBusinessTrigger trigger) {
        validateTrigger(trigger);
        trigger.setTenantId(resolveTenantId());
        fillDefaults(trigger);
        trigger.setActionConfig(normalizeActionConfig(trigger.getActionType(), trigger.getActionConfig()));
        trigger.setExecuteCount(0L);
        triggerMapper.insert(trigger);
    }

    /**
     * 修改触发器
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(AiBusinessTrigger trigger) {
        validateTrigger(trigger);
        fillDefaults(trigger);
        trigger.setActionConfig(normalizeActionConfig(trigger.getActionType(), trigger.getActionConfig()));
        triggerMapper.updateById(trigger);
    }

    /**
     * 删除触发器
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        triggerMapper.deleteById(id);
    }

    /**
     * 启停触发器
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AiBusinessTrigger trigger = new AiBusinessTrigger();
        trigger.setId(id);
        trigger.setStatus(status);
        triggerMapper.updateById(trigger);
    }

    /**
     * 查询某对象某事件下所有启用的触发器
     */
    public List<AiBusinessTrigger> selectActiveByObjectAndEvent(Long tenantId, String objectCode, String eventType) {
        return triggerMapper.selectActiveByObjectAndEvent(tenantId, objectCode, eventType);
    }

    /**
     * 记录触发器执行日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveExecutionLog(AiBusinessTriggerLog logEntry) {
        triggerLogMapper.insert(logEntry);
    }

    /**
     * 更新触发器执行统计
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementExecuteCount(Long triggerId) {
        AiBusinessTrigger trigger = triggerMapper.selectById(triggerId);
        if (trigger != null) {
            trigger.setExecuteCount((trigger.getExecuteCount() == null ? 0L : trigger.getExecuteCount()) + 1);
            trigger.setLastExecuteTime(LocalDateTime.now());
            triggerMapper.updateById(trigger);
        }
    }

    /**
     * 查询触发器执行日志
     */
    public Page<AiBusinessTriggerLog> selectLogPage(Long triggerId, PageQuery pageQuery) {
        Long tenantId = resolveTenantId();
        Page<AiBusinessTriggerLog> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return triggerLogMapper.selectTriggerLogPage(page, tenantId, triggerId);
    }

    public List<BusinessTriggerScenarioTemplateVO> scenarioTemplates() {
        List<BusinessTriggerScenarioTemplateVO> templates = new ArrayList<>();
        templates.add(template("RECORD_CREATED_START_FLOW", "新增记录后发起流程",
                "记录创建后按条件自动发起已绑定流程", BusinessEvent.RECORD_CREATED, "START_FLOW", null));
        templates.add(template("STATUS_CHANGED_SEND_MESSAGE", "状态变更后发送消息",
                "单据状态变化后发送站内消息", BusinessEvent.STATUS_CHANGED, "SEND_MESSAGE", "CREATOR"));
        templates.add(template("FLOW_APPROVED_CREATE_RECORD", "流程通过后创建记录",
                "流程通过后创建关联业务记录", BusinessEvent.FLOW_APPROVED, "CREATE_RECORD", null));
        templates.add(template("FIELD_CHANGED_UPDATE_FIELD", "字段变更后更新字段",
                "字段变化后更新当前或目标记录字段", BusinessEvent.FIELD_CHANGED, "UPDATE_FIELD", null));
        templates.add(template("DUE_DATE_REMINDER", "到期提醒",
                "按计划任务或到期字段生成提醒", "SCHEDULED_DUE", "SEND_MESSAGE", "OWNER"));
        return templates;
    }

    public String normalizeActionConfig(String actionType, String actionConfig) {
        JSONObject config = readJson(actionConfig, "动作配置");
        if ("START_FLOW".equals(actionType)) {
            JSONArray normalized = new JSONArray();
            JSONArray mappings = config.getJSONArray("variableMapping");
            if (mappings != null) {
                for (int i = 0; i < mappings.size(); i++) {
                    JSONObject item = mappings.getJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String formField = StringUtils.firstNonBlank(item.getString("formField"), item.getString("field"));
                    String flowVariable = StringUtils.firstNonBlank(item.getString("flowVariable"), item.getString("variable"));
                    if (StringUtils.isBlank(formField) || StringUtils.isBlank(flowVariable)) {
                        continue;
                    }
                    JSONObject mapping = new JSONObject();
                    mapping.put("formField", formField.trim());
                    mapping.put("flowVariable", flowVariable.trim());
                    mapping.put("label", StringUtils.trimToNull(item.getString("label")));
                    normalized.add(mapping);
                }
            }
            config.put("variableMapping", normalized);
            config.put("flowModelKey", StringUtils.trimToNull(config.getString("flowModelKey")));
            config.put("titleTemplate", StringUtils.trimToNull(config.getString("titleTemplate")));
        }
        return config.toJSONString();
    }

    private void validateTrigger(AiBusinessTrigger trigger) {
        if (trigger == null) {
            throw new BusinessException("触发器不能为空");
        }
        if (StringUtils.isBlank(trigger.getObjectCode())) {
            throw new BusinessException("业务对象编码不能为空");
        }
        if (StringUtils.isBlank(trigger.getTriggerName())) {
            throw new BusinessException("触发器名称不能为空");
        }
        if (StringUtils.isBlank(trigger.getActionType())) {
            throw new BusinessException("动作类型不能为空");
        }
        if (StringUtils.isNotBlank(trigger.getEventCondition())) {
            readJson(trigger.getEventCondition(), "触发条件");
        }
        readJson(trigger.getActionConfig(), "动作配置");
    }

    private void fillDefaults(AiBusinessTrigger trigger) {
        trigger.setTriggerType(StringUtils.defaultIfBlank(trigger.getTriggerType(), "EVENT"));
        trigger.setBlockingMode(StringUtils.defaultIfBlank(trigger.getBlockingMode(), "ASYNC"));
        if (trigger.getDeveloperMode() == null) {
            trigger.setDeveloperMode(0);
        }
        if (trigger.getStatus() == null) {
            trigger.setStatus(1);
        }
        if (trigger.getSortOrder() == null) {
            trigger.setSortOrder(0);
        }
    }

    private JSONObject readJson(String json, String label) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            throw new BusinessException(label + "不是有效JSON");
        }
    }

    private BusinessTriggerScenarioTemplateVO template(String scenarioType, String scenarioName, String description,
                                                       String eventType, String actionType, String receiverRule) {
        BusinessTriggerScenarioTemplateVO vo = new BusinessTriggerScenarioTemplateVO();
        vo.setScenarioType(scenarioType);
        vo.setScenarioName(scenarioName);
        vo.setDescription(description);
        vo.setEventType(eventType);
        vo.setActionType(actionType);
        vo.setReceiverRule(receiverRule);
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

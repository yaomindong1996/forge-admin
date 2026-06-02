package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 业务事件发布者。
 * <p>
 * 在 DynamicCrudController 的增删改操作后调用此服务发布业务事件，
 * 交由触发器引擎异步处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessEventPublisher {

    private final BusinessTriggerExecutor triggerExecutor;
    private final AiCrudConfigMapper crudConfigMapper;

    /**
     * 发布记录创建事件
     */
    public void publishRecordCreated(String configKey, Map<String, Object> data) {
        BusinessEvent event = buildEvent(configKey, BusinessEvent.RECORD_CREATED, data, null);
        publish(event);
    }

    /**
     * 发布记录更新事件
     */
    public void publishRecordUpdated(String configKey, Map<String, Object> data, Map<String, Object> previousData) {
        BusinessEvent event = buildEvent(configKey, BusinessEvent.RECORD_UPDATED, data, previousData);
        if (event != null) {
            publish(event);

            // 检查是否有状态变更
            if (previousData != null && data != null) {
                checkStatusChange(event, data, previousData);
            }
        }
    }

    /**
     * 发布记录删除事件
     */
    public void publishRecordDeleted(String configKey, String recordId) {
        BusinessEvent event = buildEvent(configKey, BusinessEvent.RECORD_DELETED, null, null);
        if (event != null) {
            event.setRecordId(recordId);
            publish(event);
        }
    }

    public void publishFlowApproved(String objectCode, String recordId, Map<String, Object> recordData) {
        publish(buildFlowResultEvent(objectCode, recordId, BusinessEvent.FLOW_APPROVED, recordData));
    }

    public void publishFlowRejected(String objectCode, String recordId, Map<String, Object> recordData) {
        publish(buildFlowResultEvent(objectCode, recordId, BusinessEvent.FLOW_REJECTED, recordData));
    }

    /**
     * 接收流程回调等内部运行态发布的业务事件。
     */
    @EventListener
    public void publishFlowEvent(BusinessEvent event) {
        if (event == null || StringUtils.isBlank(event.getEventType())) {
            return;
        }
        if (BusinessEvent.FLOW_APPROVED.equals(event.getEventType())
                || BusinessEvent.FLOW_REJECTED.equals(event.getEventType())
                || BusinessEvent.FLOW_CANCELED.equals(event.getEventType())) {
            publish(event);
        }
    }

    /**
     * 检查状态字段变更，如有变更则额外发布 STATUS_CHANGED 事件
     */
    private void checkStatusChange(BusinessEvent baseEvent, Map<String, Object> data, Map<String, Object> previousData) {
        // 常见状态字段名
        String[] statusFields = {"status", "state", "audit_status", "approval_status", "documentStatus", "document_status"};
        for (String field : statusFields) {
            Object newVal = data.get(field);
            Object oldVal = previousData.get(field);
            if (newVal != null && !newVal.equals(oldVal)) {
                BusinessEvent statusEvent = BusinessEvent.builder()
                        .eventType(BusinessEvent.STATUS_CHANGED)
                        .suiteCode(baseEvent.getSuiteCode())
                        .objectCode(baseEvent.getObjectCode())
                        .configKey(baseEvent.getConfigKey())
                        .recordId(baseEvent.getRecordId())
                        .recordData(data)
                        .previousData(previousData)
                        .operatorId(baseEvent.getOperatorId())
                        .operatorName(baseEvent.getOperatorName())
                        .tenantId(baseEvent.getTenantId())
                        .build();
                publish(statusEvent);
                break;
            }
        }
    }

    private void publish(BusinessEvent event) {
        if (event == null || StringUtils.isBlank(event.getObjectCode()) || StringUtils.isBlank(event.getEventType())) {
            return;
        }
        triggerExecutor.executeTriggersAsync(event);
    }

    private BusinessEvent buildFlowResultEvent(String objectCode, String recordId,
                                               String eventType, Map<String, Object> recordData) {
        try {
            return BusinessEvent.builder()
                    .eventType(eventType)
                    .objectCode(objectCode)
                    .recordId(recordId)
                    .recordData(recordData)
                    .operatorId(resolveUserId())
                    .operatorName(resolveUsername())
                    .tenantId(resolveTenantId())
                    .build();
        } catch (Exception e) {
            log.debug("构建流程业务事件失败, objectCode={}, recordId={}", objectCode, recordId);
            return null;
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

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername() {
        try {
            return SessionHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据 configKey 构建事件
     */
    private BusinessEvent buildEvent(String configKey, String eventType, Map<String, Object> data, Map<String, Object> previousData) {
        try {
            Long tenantId = SessionHelper.getTenantId();

            // 从运行配置中获取对象编码
            String objectCode = resolveObjectCode(configKey, tenantId);
            if (objectCode == null) {
                return null; // 非业务对象的动态CRUD，不触发
            }

            String recordId = null;
            if (data != null && data.get("id") != null) {
                recordId = String.valueOf(data.get("id"));
            }

            return BusinessEvent.builder()
                    .eventType(eventType)
                    .objectCode(objectCode)
                    .configKey(configKey)
                    .recordId(recordId)
                    .recordData(data)
                    .previousData(previousData)
                    .operatorId(SessionHelper.getUserId())
                    .operatorName(SessionHelper.getUsername())
                    .tenantId(tenantId)
                    .build();
        } catch (Exception e) {
            log.debug("构建业务事件失败, configKey={}: {}", configKey, e.getMessage());
            return null;
        }
    }

    /**
     * 通过 configKey 查询关联的业务对象编码
     */
    private String resolveObjectCode(String configKey, Long tenantId) {
        try {
            // 通过 ai_crud_config 的 object_code 字段获取
            AiCrudConfig config = crudConfigMapper.selectByConfigKey(tenantId, configKey);
            if (config != null && config.getObjectCode() != null && !config.getObjectCode().isBlank()) {
                return config.getObjectCode();
            }
        } catch (Exception e) {
            log.debug("resolveObjectCode 失败: configKey={}", configKey);
        }
        return null;
    }
}

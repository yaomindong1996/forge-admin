package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTriggerLog;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 触发器执行引擎。
 * <p>
 * 负责事件匹配、条件评估和动作分发。
 * 当 BusinessEventPublisher 发布事件后，此类查找匹配的触发器并异步执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessTriggerExecutor {

    private final BusinessTriggerService triggerService;
    private final BusinessFlowService flowService;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessMessageChannelService messageChannelService;

    /**
     * 异步执行触发器匹配和动作
     */
    @Async
    public void executeTriggersAsync(BusinessEvent event) {
        try {
            List<AiBusinessTrigger> triggers = triggerService.selectActiveByObjectAndEvent(
                    event.getTenantId(), event.getObjectCode(), event.getEventType());

            if (triggers == null || triggers.isEmpty()) {
                return;
            }

            for (AiBusinessTrigger trigger : triggers) {
                executeSingleTrigger(trigger, event);
            }
        } catch (Exception e) {
            log.error("触发器执行异常, objectCode={}, eventType={}", event.getObjectCode(), event.getEventType(), e);
        }
    }

    @Async
    public void executeTriggerAsync(AiBusinessTrigger trigger, BusinessEvent event) {
        executeSingleTrigger(trigger, event);
    }

    public void executeTrigger(AiBusinessTrigger trigger, BusinessEvent event) {
        executeSingleTrigger(trigger, event);
    }

    public boolean matchesCondition(AiBusinessTrigger trigger, BusinessEvent event) {
        return evaluateCondition(trigger, event);
    }

    /**
     * 执行单个触发器
     */
    private void executeSingleTrigger(AiBusinessTrigger trigger, BusinessEvent event) {
        long startTime = System.currentTimeMillis();
        AiBusinessTriggerLog logEntry = buildLogEntry(trigger, event);

        try {
            // 1. 评估条件
            if (!evaluateCondition(trigger, event)) {
                logEntry.setExecuteStatus("SKIPPED");
                logEntry.setErrorMessage("条件不满足");
                triggerService.saveExecutionLog(logEntry);
                return;
            }

            // 2. 执行动作
            JSONObject result = executeAction(trigger, event);

            // 3. 记录执行结果
            String resultStatus = result == null ? "SUCCESS" : result.getString("status");
            logEntry.setExecuteStatus("TODO".equals(resultStatus) ? "TODO" : "SUCCESS");
            if (result != null) {
                logEntry.setTodoCode(result.getString("todoCode"));
                logEntry.setCorrelationId(resolveCorrelationId(result));
            }
            logEntry.setActionResult(result != null ? result.toJSONString() : null);
            logEntry.setDurationMs(System.currentTimeMillis() - startTime);
            triggerService.saveExecutionLog(logEntry);
            triggerService.incrementExecuteCount(trigger.getId());

            log.info("触发器执行成功: trigger={}, object={}, record={}",
                    trigger.getTriggerName(), event.getObjectCode(), event.getRecordId());

        } catch (Exception e) {
            logEntry.setExecuteStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setDurationMs(System.currentTimeMillis() - startTime);
            triggerService.saveExecutionLog(logEntry);

            log.error("触发器执行失败: trigger={}, object={}", trigger.getTriggerName(), event.getObjectCode(), e);
        }
    }

    /**
     * 评估触发条件
     */
    private boolean evaluateCondition(AiBusinessTrigger trigger, BusinessEvent event) {
        String conditionJson = trigger.getEventCondition();
        if (conditionJson == null || conditionJson.isBlank()) {
            return true; // 无条件则直接满足
        }

        try {
            JSONObject condition = JSON.parseObject(conditionJson);
            return evaluateConditionNode(condition, event);
        } catch (Exception e) {
            log.warn("触发器条件解析失败: triggerId={}, condition={}", trigger.getId(), conditionJson, e);
            return false;
        }
    }

    /**
     * 递归评估条件节点
     * <p>
     * 支持格式：
     * {"field": "status", "op": "eq", "value": "submitted"}
     * {"field": "status", "op": "changed_to", "value": "submitted"}
     * {"and": [...conditions]}
     * {"or": [...conditions]}
     */
    private boolean evaluateConditionNode(JSONObject node, BusinessEvent event) {
        JSONArray rules = node.getJSONArray("rules");
        if (rules != null) {
            boolean orLogic = "OR".equalsIgnoreCase(node.getString("logic"));
            for (int i = 0; i < rules.size(); i++) {
                boolean matched = evaluateConditionNode(rules.getJSONObject(i), event);
                if (orLogic && matched) {
                    return true;
                }
                if (!orLogic && !matched) {
                    return false;
                }
            }
            return !orLogic;
        }

        // AND 逻辑
        JSONArray andConditions = node.getJSONArray("and");
        if (andConditions != null) {
            for (int i = 0; i < andConditions.size(); i++) {
                if (!evaluateConditionNode(andConditions.getJSONObject(i), event)) {
                    return false;
                }
            }
            return true;
        }

        // OR 逻辑
        JSONArray orConditions = node.getJSONArray("or");
        if (orConditions != null) {
            for (int i = 0; i < orConditions.size(); i++) {
                if (evaluateConditionNode(orConditions.getJSONObject(i), event)) {
                    return true;
                }
            }
            return false;
        }

        // 字段条件
        String field = node.getString("field");
        String op = StringUtils.firstNonBlank(node.getString("op"), node.getString("operator"));
        Object expectedValue = node.get("value");

        if (field == null || op == null) {
            return true;
        }
        op = op.trim().toLowerCase(Locale.ROOT);

        Map<String, Object> recordData = event.getRecordData();
        Map<String, Object> previousData = event.getPreviousData();
        Object actualValue = recordData != null ? recordData.get(field) : null;

        return switch (op) {
            case "eq" -> Objects.equals(String.valueOf(actualValue), String.valueOf(expectedValue));
            case "neq" -> !Objects.equals(String.valueOf(actualValue), String.valueOf(expectedValue));
            case "is_null" -> actualValue == null;
            case "not_null" -> actualValue != null;
            case "gt" -> compare(actualValue, expectedValue, "gt");
            case "gte", "ge" -> compare(actualValue, expectedValue, "gte");
            case "lt" -> compare(actualValue, expectedValue, "lt");
            case "lte", "le" -> compare(actualValue, expectedValue, "lte");
            case "in" -> matchesAny(actualValue, expectedValue);
            case "not_in" -> !matchesAny(actualValue, expectedValue);
            case "contains" -> actualValue != null && expectedValue != null
                    && String.valueOf(actualValue).contains(String.valueOf(expectedValue));
            case "changed" -> {
                Object prevValue = previousData != null ? previousData.get(field) : null;
                yield !Objects.equals(String.valueOf(prevValue), String.valueOf(actualValue));
            }
            case "changed_to" -> {
                Object prevValue = previousData != null ? previousData.get(field) : null;
                yield !Objects.equals(String.valueOf(prevValue), String.valueOf(expectedValue))
                        && Objects.equals(String.valueOf(actualValue), String.valueOf(expectedValue));
            }
            case "changed_from" -> {
                Object prevValue = previousData != null ? previousData.get(field) : null;
                yield Objects.equals(String.valueOf(prevValue), String.valueOf(expectedValue));
            }
            default -> true;
        };
    }

    private boolean compare(Object actualValue, Object expectedValue, String operator) {
        Integer result = compareValues(actualValue, expectedValue);
        if (result == null) {
            return false;
        }
        return switch (operator) {
            case "gt" -> result > 0;
            case "gte" -> result >= 0;
            case "lt" -> result < 0;
            case "lte" -> result <= 0;
            default -> false;
        };
    }

    private Integer compareValues(Object actualValue, Object expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return null;
        }
        BigDecimal actualNumber = toDecimal(actualValue);
        BigDecimal expectedNumber = toDecimal(expectedValue);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber);
        }
        LocalDateTime actualTime = toDateTime(actualValue);
        LocalDateTime expectedTime = toDateTime(expectedValue);
        if (actualTime != null && expectedTime != null) {
            return actualTime.compareTo(expectedTime);
        }
        return String.valueOf(actualValue).compareTo(String.valueOf(expectedValue));
    }

    private BigDecimal toDecimal(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (value instanceof Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        String text = StringUtils.trimToNull(String.valueOf(value));
        if (text == null) {
            return null;
        }
        try {
            if (text.length() == 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean matchesAny(Object actualValue, Object expectedValue) {
        if (actualValue == null || expectedValue == null) {
            return false;
        }
        if (expectedValue instanceof JSONArray array) {
            for (Object item : array) {
                if (Objects.equals(String.valueOf(actualValue), String.valueOf(item))) {
                    return true;
                }
            }
            return false;
        }
        if (expectedValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (Objects.equals(String.valueOf(actualValue), String.valueOf(item))) {
                    return true;
                }
            }
            return false;
        }
        String expectedText = String.valueOf(expectedValue);
        for (String item : expectedText.split(",")) {
            if (Objects.equals(String.valueOf(actualValue), item.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行触发器动作
     */
    private JSONObject executeAction(AiBusinessTrigger trigger, BusinessEvent event) {
        String actionType = trigger.getActionType();
        JSONObject actionConfig = JSON.parseObject(trigger.getActionConfig());
        if (actionConfig == null) {
            actionConfig = new JSONObject();
        }

        return switch (actionType) {
            case "START_FLOW" -> executeStartFlowAction(actionConfig, event);
            case "SEND_MESSAGE" -> executeSendMessageAction(actionConfig, event);
            case "CREATE_RECORD" -> executeCreateRecordAction(actionConfig, event);
            case "UPDATE_FIELD" -> executeUpdateFieldAction(actionConfig, event);
            case "WEBHOOK" -> executeWebhookAction(actionConfig, event);
            default -> {
                log.warn("未知动作类型: {}", actionType);
                yield null;
            }
        };
    }

    /**
     * 发起流程动作
     */
    private JSONObject executeStartFlowAction(JSONObject config, BusinessEvent event) {
        boolean useMainFlow = config.getBoolean("useMainFlow") == null || config.getBooleanValue("useMainFlow");
        String flowModelKey = useMainFlow ? null : StringUtils.trimToNull(config.getString("flowModelKey"));
        JSONArray variableMapping = config.getJSONArray("variableMapping");

        // 构建流程变量
        JSONObject flowVariables = new JSONObject();
        if (variableMapping != null && event.getRecordData() != null) {
            for (int i = 0; i < variableMapping.size(); i++) {
                JSONObject mapping = variableMapping.getJSONObject(i);
                if (mapping == null) {
                    continue;
                }
                String formField = StringUtils.defaultIfBlank(mapping.getString("formField"), mapping.getString("field"));
                String flowVariable = StringUtils.defaultIfBlank(mapping.getString("flowVariable"), mapping.getString("variable"));
                Object value = readRecordValue(event.getRecordData(), formField);
                if (value != null && StringUtils.isNotBlank(flowVariable)) {
                    flowVariables.put(flowVariable, value);
                }
            }
        }

        // 调用流程服务发起流程
        String businessKey = event.getObjectCode() + ":" + event.getRecordId();
        String title = StringUtils.firstNonBlank(config.getString("title"), config.getString("titleTemplate"));
        if (StringUtils.isNotBlank(title)) {
            title = renderTemplate(title, event.getRecordData());
        }

        BusinessFlowRuntimeVO runtime = flowService.startFlowFromTrigger(flowModelKey, businessKey, title,
                event.getOperatorId(), event.getOperatorName(), event.getTenantId(), flowVariables);

        JSONObject result = new JSONObject();
        result.put("useMainFlow", useMainFlow);
        result.put("flowModelKey", runtime.getFlowModelKey());
        result.put("businessKey", businessKey);
        result.put("processInstanceId", runtime.getProcessInstanceId());
        result.put("status", runtime.getFlowStatus());
        return result;
    }

    /**
     * 发送消息动作（复用现有消息中心）
     */
    private JSONObject executeSendMessageAction(JSONObject config, BusinessEvent event) {
        String templateCode = config.getString("templateCode");
        String receiverRule = config.getString("receiverRule");
        String channelCode = StringUtils.firstNonBlank(config.getString("channelCode"), config.getString("channel"));
        BusinessMessageChannelStatus channelStatus = messageChannelService.resolveChannel(channelCode);
        if (Boolean.TRUE.equals(channelStatus.getTodo())) {
            JSONObject result = messageChannelService.buildThirdPartyTodoResult(
                    channelStatus.getChannelType(), channelStatus.getChannelCode());
            result.put("templateCode", templateCode);
            result.put("receiverRule", receiverRule);
            return result;
        }

        // 构建消息发送请求
        MessageSendRequestDTO req = new MessageSendRequestDTO();
        req.setTemplateCode(templateCode);
        req.setChannel(StringUtils.defaultIfBlank(channelStatus.getSendChannel(), "WEB"));
        req.setType("SYSTEM");
        req.setBizType("TRIGGER");
        req.setBizKey(event.getObjectCode() + ":" + event.getRecordId());

        // 模板参数：将业务记录数据作为模板变量
        Map<String, Object> params = new HashMap<>();
        if (event.getRecordData() != null) {
            params.putAll(event.getRecordData());
            event.getRecordData().forEach((key, value) -> {
                if (StringUtils.isNotBlank(key)) {
                    params.putIfAbsent(DynamicQueryGenerator.snakeToCamel(key), value);
                    params.putIfAbsent(DynamicQueryGenerator.camelToSnake(key), value);
                }
            });
        }
        params.put("objectCode", event.getObjectCode());
        params.put("eventType", event.getEventType());
        params.put("recordId", event.getRecordId());
        params.put("operatorName", event.getOperatorName());
        req.setParams(params);

        // 解析接收人规则
        resolveReceivers(req, receiverRule, event);

        // 调用现有消息服务发送
        try {
            SysMessage message = messageChannelService.sendInternalMessage(req);
            JSONObject result = new JSONObject();
            result.put("status", "SENT");
            result.put("templateCode", templateCode);
            result.put("channel", channelStatus.getChannelCode());
            result.put("messageId", message == null ? null : message.getId());
            return result;
        } catch (Exception e) {
            log.error("消息发送失败: templateCode={}, error={}", templateCode, e.getMessage());
            throw new RuntimeException("消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析接收人规则
     * 支持格式：STARTER / OWNER / CREATOR / USERS:1,2,3 / ROLES:1,2 / DEPTS:1,2 / ALL
     */
    private void resolveReceivers(MessageSendRequestDTO req, String receiverRule, BusinessEvent event) {
        if (receiverRule == null || receiverRule.isBlank()) {
            // 默认发给操作人
            if (event.getOperatorId() != null) {
                req.setUserIds(Set.of(event.getOperatorId()));
                req.setSendScope("USERS");
            }
            return;
        }

        String normalizedRule = receiverRule.trim().toUpperCase(Locale.ROOT);
        if ("STARTER".equals(normalizedRule)) {
            setSingleReceiver(req, event.getOperatorId());
        } else if ("OWNER".equals(normalizedRule)) {
            Long ownerId = firstLongFromRecord(event.getRecordData(),
                    "ownerId", "owner_id", "responsibleId", "responsible_id", "assigneeId", "assignee_id");
            setSingleReceiver(req, ownerId != null ? ownerId : event.getOperatorId());
        } else if ("CREATOR".equals(normalizedRule)) {
            Long creatorId = firstLongFromRecord(event.getRecordData(), "createBy", "create_by");
            setSingleReceiver(req, creatorId != null ? creatorId : event.getOperatorId());
        } else if (normalizedRule.startsWith("USERS:")) {
            req.setUserIds(new LinkedHashSet<>(parseLongList(receiverRule.substring(receiverRule.indexOf(':') + 1))));
            req.setSendScope("USERS");
        } else if (normalizedRule.startsWith("ROLES:")) {
            List<Long> roleIds = parseLongList(receiverRule.substring(receiverRule.indexOf(':') + 1));
            req.setUserIds(messageChannelService.toUserIdSet(messageChannelService.selectUserIdsByRoleIds(roleIds)));
            req.setSendScope("USERS");
        } else if (normalizedRule.startsWith("DEPTS:")) {
            List<Long> orgIds = parseLongList(receiverRule.substring(receiverRule.indexOf(':') + 1));
            req.setOrgIds(new LinkedHashSet<>(orgIds));
            req.setSendScope("ORG");
        } else if ("ALL".equals(normalizedRule)) {
            req.setSendScope("ALL");
        } else {
            // 默认发给操作人
            if (event.getOperatorId() != null) {
                req.setUserIds(Set.of(event.getOperatorId()));
                req.setSendScope("USERS");
            }
        }
    }

    private void setSingleReceiver(MessageSendRequestDTO req, Long userId) {
        if (userId != null) {
            req.setUserIds(Set.of(userId));
        }
        req.setSendScope("USERS");
    }

    private Long firstLongFromRecord(Map<String, Object> recordData, String... fields) {
        if (recordData == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            Object value = readRecordValue(recordData, field);
            if (value == null) {
                continue;
            }
            try {
                return Long.valueOf(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // ignore invalid receiver id
            }
        }
        return null;
    }

    private List<Long> parseLongList(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String item : value.split(",")) {
            try {
                ids.add(Long.valueOf(item.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid receiver id
            }
        }
        return ids;
    }

    /**
     * 创建关联记录动作
     */
    private JSONObject executeCreateRecordAction(JSONObject config, BusinessEvent event) {
        String targetConfigKey = config.getString("targetConfigKey");
        if (StringUtils.isBlank(targetConfigKey)) {
            throw new RuntimeException("创建记录动作缺少 targetConfigKey");
        }
        Map<String, Object> targetData = buildTargetData(config, event);
        if (targetData.isEmpty()) {
            throw new RuntimeException("创建记录动作没有可写入字段");
        }
        Map<String, Object> created = dynamicCrudService.insertInternal(targetConfigKey, targetData);
        JSONObject result = new JSONObject();
        result.put("status", "CREATED");
        result.put("targetConfigKey", targetConfigKey);
        result.put("createdRecordId", created == null ? null : created.get("id"));
        return result;
    }

    /**
     * 更新字段动作
     */
    private JSONObject executeUpdateFieldAction(JSONObject config, BusinessEvent event) {
        String targetConfigKey = StringUtils.defaultIfBlank(config.getString("targetConfigKey"), event.getConfigKey());
        if (StringUtils.isBlank(targetConfigKey)) {
            throw new RuntimeException("更新字段动作缺少 targetConfigKey");
        }
        Long targetRecordId = resolveTargetRecordId(config, event);
        Map<String, Object> updateFields = buildUpdateFields(config, event);
        if (updateFields.isEmpty()) {
            throw new RuntimeException("更新字段动作没有可更新字段");
        }
        dynamicCrudService.updateFieldsInternal(targetConfigKey, targetRecordId, updateFields);
        JSONObject result = new JSONObject();
        result.put("status", "UPDATED");
        result.put("targetConfigKey", targetConfigKey);
        result.put("targetRecordId", targetRecordId);
        return result;
    }

    /**
     * Webhook 调用动作（TODO预留）
     */
    private JSONObject executeWebhookAction(JSONObject config, BusinessEvent event) {
        log.info("Webhook动作待实现: objectCode={}, recordId={}", event.getObjectCode(), event.getRecordId());

        JSONObject result = new JSONObject();
        result.put("status", "TODO");
        result.put("todoCode", "WEBHOOK_NOT_IMPLEMENTED");
        result.put("message", "Webhook动作待实现");
        return result;
    }

    private Map<String, Object> buildTargetData(JSONObject config, BusinessEvent event) {
        Map<String, Object> targetData = new LinkedHashMap<>();
        JSONArray fieldMapping = firstArray(config, "fieldMapping", "fieldMappings");
        if (fieldMapping != null) {
            for (int i = 0; i < fieldMapping.size(); i++) {
                JSONObject mapping = fieldMapping.getJSONObject(i);
                if (mapping == null) {
                    continue;
                }
                String sourceField = StringUtils.firstNonBlank(
                        mapping.getString("sourceField"), mapping.getString("source"),
                        mapping.getString("formField"), mapping.getString("field"));
                String targetField = StringUtils.firstNonBlank(
                        mapping.getString("targetField"), mapping.getString("target"));
                if (StringUtils.isBlank(sourceField) || StringUtils.isBlank(targetField)) {
                    continue;
                }
                if (!hasRecordField(event.getRecordData(), sourceField)) {
                    throw new RuntimeException("字段映射源字段不存在: " + sourceField);
                }
                targetData.put(targetField.trim(), readRecordValue(event.getRecordData(), sourceField));
            }
        }
        JSONObject staticValues = config.getJSONObject("staticValues");
        if (staticValues != null) {
            targetData.putAll(staticValues);
        }
        return targetData;
    }

    private Map<String, Object> buildUpdateFields(JSONObject config, BusinessEvent event) {
        Map<String, Object> updateFields = new LinkedHashMap<>();
        JSONArray fields = firstArray(config, "fields", "fieldMapping", "fieldMappings");
        if (fields != null) {
            for (int i = 0; i < fields.size(); i++) {
                JSONObject item = fields.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String targetField = StringUtils.firstNonBlank(
                        item.getString("targetField"), item.getString("target"),
                        item.getString("field"), item.getString("formField"));
                if (StringUtils.isBlank(targetField)) {
                    continue;
                }
                Object value;
                String sourceField = StringUtils.firstNonBlank(item.getString("sourceField"), item.getString("source"));
                if (StringUtils.isNotBlank(sourceField)) {
                    if (!hasRecordField(event.getRecordData(), sourceField)) {
                        throw new RuntimeException("更新字段源字段不存在: " + sourceField);
                    }
                    value = readRecordValue(event.getRecordData(), sourceField);
                } else if (item.containsKey("value")) {
                    value = item.get("value");
                } else {
                    continue;
                }
                updateFields.put(targetField.trim(), value);
            }
        }
        JSONObject staticValues = config.getJSONObject("staticValues");
        if (staticValues != null) {
            updateFields.putAll(staticValues);
        }
        return updateFields;
    }

    private Long resolveTargetRecordId(JSONObject config, BusinessEvent event) {
        Object recordId = StringUtils.firstNonBlank(config.getString("targetRecordId"), config.getString("recordId"));
        String recordIdField = config.getString("recordIdField");
        if (StringUtils.isNotBlank(recordIdField)) {
            if (!hasRecordField(event.getRecordData(), recordIdField)) {
                throw new RuntimeException("更新字段记录ID来源字段不存在: " + recordIdField);
            }
            recordId = readRecordValue(event.getRecordData(), recordIdField);
        }
        if (recordId == null) {
            recordId = event.getRecordId();
        }
        if (recordId == null) {
            throw new RuntimeException("更新字段动作缺少目标记录ID");
        }
        try {
            return Long.valueOf(String.valueOf(recordId));
        } catch (NumberFormatException e) {
            throw new RuntimeException("更新字段动作目标记录ID不是数字");
        }
    }

    private JSONArray firstArray(JSONObject config, String... keys) {
        for (String key : keys) {
            JSONArray value = config.getJSONArray(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean hasRecordField(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return false;
        }
        return recordData.containsKey(field)
                || recordData.containsKey(DynamicQueryGenerator.snakeToCamel(field))
                || recordData.containsKey(DynamicQueryGenerator.camelToSnake(field));
    }

    private Object readRecordValue(Map<String, Object> recordData, String field) {
        if (recordData == null || StringUtils.isBlank(field)) {
            return null;
        }
        if (recordData.containsKey(field)) {
            return recordData.get(field);
        }
        String camelField = DynamicQueryGenerator.snakeToCamel(field);
        if (recordData.containsKey(camelField)) {
            return recordData.get(camelField);
        }
        String snakeField = DynamicQueryGenerator.camelToSnake(field);
        if (recordData.containsKey(snakeField)) {
            return recordData.get(snakeField);
        }
        return null;
    }

    private String renderTemplate(String template, Map<String, Object> recordData) {
        if (StringUtils.isBlank(template) || recordData == null || recordData.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : recordData.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = replaceTemplateToken(result, key, value);
            result = replaceTemplateToken(result, DynamicQueryGenerator.snakeToCamel(key), value);
            result = replaceTemplateToken(result, DynamicQueryGenerator.camelToSnake(key), value);
        }
        return result;
    }

    private String replaceTemplateToken(String template, String key, String value) {
        if (StringUtils.isBlank(key)) {
            return template;
        }
        return template.replace("${" + key + "}", value)
                .replace("{" + key + "}", value);
    }

    private String resolveCorrelationId(JSONObject result) {
        if (result == null) {
            return null;
        }
        return StringUtils.firstNonBlank(
                result.getString("processInstanceId"),
                result.getString("createdRecordId"),
                result.getString("targetRecordId"),
                result.getString("messageId"));
    }

    private AiBusinessTriggerLog buildLogEntry(AiBusinessTrigger trigger, BusinessEvent event) {
        AiBusinessTriggerLog logEntry = new AiBusinessTriggerLog();
        logEntry.setTenantId(event.getTenantId());
        logEntry.setTriggerId(trigger.getId());
        logEntry.setTriggerName(trigger.getTriggerName());
        logEntry.setSuiteCode(trigger.getSuiteCode());
        logEntry.setObjectCode(trigger.getObjectCode());
        logEntry.setRecordId(event.getRecordId());
        logEntry.setEventType(event.getEventType());
        logEntry.setEventData(event.getRecordData() != null ? JSON.toJSONString(event.getRecordData()) : null);
        logEntry.setActionType(trigger.getActionType());
        logEntry.setExecuteTime(LocalDateTime.now());
        logEntry.setRetryCount(0);
        return logEntry;
    }
}

package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowConditionRule;
import com.mdframe.forge.starter.flow.entity.FlowConditionItem;

import java.util.List;
import java.util.Map;

/**
 * 流程条件规则服务接口
 *
 * @author forge
 */
public interface FlowConditionRuleService extends IService<FlowConditionRule> {

    /**
     * 根据模型ID获取所有条件规则
     * @param modelId 模型ID
     * @return 条件规则列表
     */
    List<FlowConditionRule> getByModelId(String modelId);

    /**
     * 根据序列流ID获取条件规则
     * @param sequenceFlowId 序列流ID
     * @return 条件规则
     */
    FlowConditionRule getBySequenceFlowId(String sequenceFlowId);

    /**
     * 保存条件规则（包含条件项）
     * @param rule 条件规则
     * @param items 条件项列表
     * @return 是否成功
     */
    boolean saveRule(FlowConditionRule rule, List<FlowConditionItem> items);

    /**
     * 删除条件规则
     * @param id 规则ID
     * @return 是否成功
     */
    boolean deleteRule(String id);

    /**
     * 获取条件规则的条件项列表
     * @param ruleId 规则ID
     * @return 条件项列表
     */
    List<FlowConditionItem> getConditionItems(String ruleId);

    /**
     * 保存条件项
     * @param ruleId 规则ID
     * @param items 条件项列表
     * @return 是否成功
     */
    boolean saveConditionItems(String ruleId, List<FlowConditionItem> items);

    /**
     * 评估条件规则
     * @param ruleId 规则ID
     * @param variables 流程变量
     * @return 是否满足条件
     */
    boolean evaluateRule(String ruleId, Map<String, Object> variables);

    /**
     * 将条件规则转换为BPMN表达式
     * @param ruleId 规则ID
     * @return BPMN条件表达式
     */
    String toBpmnExpression(String ruleId);

    /**
     * 从BPMN表达式解析条件规则
     * @param expression BPMN条件表达式
     * @param sequenceFlowId 序列流ID
     * @return 条件规则
     */
    FlowConditionRule parseFromBpmnExpression(String expression, String sequenceFlowId);

    /**
     * 验证条件规则配置
     * @param rule 条件规则
     * @param items 条件项列表
     * @return 验证结果消息，null表示验证通过
     */
    String validateRule(FlowConditionRule rule, List<FlowConditionItem> items);
}
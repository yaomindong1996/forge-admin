-- 流程审批结果/通过抄送企业协同卡片模板。
-- 通用模板默认启用，平台差异化模板默认停用；模板正文中的变量使用 CONCAT 构造，避免 Flyway 解析占位符。

INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template,
     default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_RESULT_CARD', '流程审批结果卡片（通用）', 'SYSTEM', '流程审批结果通知',
       CONCAT('<div class="gray">流程审批结果通知</div><div class="normal">流程：', '$', '{processName}</div>',
              '<div class="normal">结果：', '$', '{result}</div><div class="normal">发起人：', '$', '{applyUserName}</div>',
              '<div class="highlight">点击卡片查看详情 ›</div>'),
       'COLLABORATION', 1, '流程审批结果企业协同卡片通用模板，支持 processName/result/applyUserName 变量', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT template_code FROM sys_message_template
        WHERE tenant_id = 1 AND template_code = 'FLOW_RESULT_CARD' AND del_flag = 0
    ) t
);

INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template,
     default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_RESULT_CARD_WECOM', '流程审批结果卡片（企业微信）', 'SYSTEM', '流程审批结果通知',
       CONCAT('<div class="gray">流程审批结果通知</div><div class="normal">流程：', '$', '{processName}</div>',
              '<div class="normal">结果：', '$', '{result}</div><div class="normal">发起人：', '$', '{applyUserName}</div>',
              '<div class="highlight">点击卡片查看详情 ›</div>'),
       'COLLABORATION', 0, '企业微信平台差异化审批结果卡片模板，启用后覆盖 FLOW_RESULT_CARD', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT template_code FROM sys_message_template
        WHERE tenant_id = 1 AND template_code = 'FLOW_RESULT_CARD_WECOM' AND del_flag = 0
    ) t
);

INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template,
     default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_CC_CARD', '流程抄送卡片（通用）', 'SYSTEM', '流程抄送通知',
       CONCAT('<div class="gray">流程抄送通知</div><div class="normal">流程：', '$', '{processName}</div>',
              '<div class="highlight">点击卡片查看详情 ›</div>'),
       'COLLABORATION', 1, '流程通过抄送企业协同卡片通用模板，支持 processName 变量', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT template_code FROM sys_message_template
        WHERE tenant_id = 1 AND template_code = 'FLOW_CC_CARD' AND del_flag = 0
    ) t
);

INSERT INTO sys_message_template
    (tenant_id, template_code, template_name, type, title_template, content_template,
     default_channel, enabled, remark, create_by, del_flag)
SELECT 1, 'FLOW_CC_CARD_WECOM', '流程抄送卡片（企业微信）', 'SYSTEM', '流程抄送通知',
       CONCAT('<div class="gray">流程抄送通知</div><div class="normal">流程：', '$', '{processName}</div>',
              '<div class="highlight">点击卡片查看详情 ›</div>'),
       'COLLABORATION', 0, '企业微信平台差异化流程抄送卡片模板，启用后覆盖 FLOW_CC_CARD', NULL, 0
WHERE NOT EXISTS (
    SELECT 1 FROM (
        SELECT template_code FROM sys_message_template
        WHERE tenant_id = 1 AND template_code = 'FLOW_CC_CARD_WECOM' AND del_flag = 0
    ) t
);

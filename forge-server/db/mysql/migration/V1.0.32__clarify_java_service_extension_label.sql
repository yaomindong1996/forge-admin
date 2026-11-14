-- 将受控服务端能力明确呈现为 Java 服务增强；只更新系统原始文案，不覆盖租户自定义标签。

UPDATE sys_dict_data
SET dict_label = 'Java 服务增强',
    remark = '绑定已开发、部署并注册的 LowcodeExtensionHandler，不在线编译任意 Java',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_extension_type'
  AND dict_value = 'SERVER_BINDING'
  AND dict_label = '服务端能力绑定';

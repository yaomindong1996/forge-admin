-- 国际化配置分组：默认关闭，全站中文；开启后才允许语言切换。（PostgreSQL，与 MySQL V1.0.240 同语义）
INSERT INTO sys_config_group (
  group_code, group_name, group_icon, config_value, sort, status, remark, create_time, update_time
)
SELECT
  'i18n',
  '国际化配置',
  'language',
  '{"enableI18n":false,"defaultLocale":"zh-CN"}',
  20,
  1,
  '是否启用国际化及默认语言',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM sys_config_group WHERE group_code = 'i18n'
);

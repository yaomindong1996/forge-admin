-- 国际化配置分组：默认关闭，全站中文；开启后才允许语言切换。（SQL Server，与 MySQL V1.0.240 同语义）
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys_config_group WHERE group_code = N'i18n')
BEGIN
    INSERT INTO sys_config_group (
      group_code, group_name, group_icon, config_value, sort, status, remark, create_time, update_time
    )
    VALUES (
      N'i18n',
      N'国际化配置',
      N'language',
      N'{"enableI18n":false,"defaultLocale":"zh-CN"}',
      20,
      1,
      N'是否启用国际化及默认语言',
      SYSUTCDATETIME(),
      SYSUTCDATETIME()
    );
END
GO

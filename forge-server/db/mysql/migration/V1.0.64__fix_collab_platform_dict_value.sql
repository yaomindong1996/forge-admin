-- 修正企业协同平台字典值：WECOM -> WECHAT_ENTERPRISE
-- V1.0.59 误将企业微信字典值写成 WECOM，与后端 SocialPlatform.WECHAT_ENTERPRISE 枚举编码不一致，
-- 导致按平台匹配 AccessTokenProvider/Connector 失败（提示"平台 WECOM 暂未提供连通测试能力"）。

-- 1. 修正字典值（防重复：目标值已存在时跳过，避免产生重复字典项）
UPDATE sys_dict_data
SET dict_value = 'WECHAT_ENTERPRISE'
WHERE dict_type = 'sys_collab_platform'
  AND dict_value = 'WECOM'
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT 1 FROM sys_dict_data
      WHERE dict_type = 'sys_collab_platform' AND dict_value = 'WECHAT_ENTERPRISE'
    ) t
  );

-- 2. 修正已按错误字典值落库的连接数据
UPDATE sys_social_config
SET platform = 'WECHAT_ENTERPRISE'
WHERE platform = 'WECOM';

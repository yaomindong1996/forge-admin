-- 企业协同平台字典补齐纯 OAuth 登录平台
-- 背景：V1.0.59 只写入企业微信一条平台字典，导致「企业协同 - 连接管理」平台下拉无法选择
-- Gitee/GitHub 等仅登录平台，旧「三方登录配置」页面又已收敛为只读，形成配置入口缺口。
-- 本脚本将 sys_collab_platform 对齐 sys_social_platform 的平台覆盖，使纯登录平台可通过
-- 「连接 + 应用（LOGIN 能力）」统一模型配置。
-- 平台编码必须与后端 SocialPlatform 枚举 code 完全一致，否则 AuthRequest 构建会失败。

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, 'N', 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 10 dict_sort, '钉钉' dict_label, 'DINGTALK' dict_value,
         'sys_collab_platform' dict_type, 'info' list_class, '钉钉扫码登录' remark
  UNION ALL SELECT 1, 11, '飞书', 'FEISHU', 'sys_collab_platform', 'info', '飞书扫码登录'
  UNION ALL SELECT 1, 12, '钉钉企业内部', 'DINGTALK_ACCOUNT', 'sys_collab_platform', 'info', '钉钉企业内部应用登录'
  UNION ALL SELECT 1, 20, '微信开放平台', 'WECHAT_OPEN', 'sys_collab_platform', 'success', '微信开放平台扫码登录'
  UNION ALL SELECT 1, 21, '微信', 'WECHAT', 'sys_collab_platform', 'success', '微信登录'
  UNION ALL SELECT 1, 22, '微信小程序', 'WECHAT_MINI', 'sys_collab_platform', 'success', '微信小程序登录'
  UNION ALL SELECT 1, 30, 'Gitee', 'GITEE', 'sys_collab_platform', 'default', 'Gitee OAuth 登录'
  UNION ALL SELECT 1, 31, 'GitHub', 'GITHUB', 'sys_collab_platform', 'default', 'GitHub OAuth 登录'
  UNION ALL SELECT 1, 40, 'QQ', 'QQ', 'sys_collab_platform', 'default', 'QQ OAuth 登录'
  UNION ALL SELECT 1, 41, '微博', 'WEIBO', 'sys_collab_platform', 'default', '微博 OAuth 登录'
  UNION ALL SELECT 1, 42, '支付宝', 'ALIPAY', 'sys_collab_platform', 'default', '支付宝 OAuth 登录'
  UNION ALL SELECT 1, 43, '百度', 'BAIDU', 'sys_collab_platform', 'default', '百度 OAuth 登录'
  UNION ALL SELECT 1, 50, '谷歌', 'GOOGLE', 'sys_collab_platform', 'default', 'Google OAuth 登录'
  UNION ALL SELECT 1, 51, 'Facebook', 'FACEBOOK', 'sys_collab_platform', 'default', 'Facebook OAuth 登录'
  UNION ALL SELECT 1, 52, 'Twitter', 'TWITTER', 'sys_collab_platform', 'default', 'Twitter OAuth 登录'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

-- 企业协同 Provider 出站白名单：企业微信官方 API 域名
-- 场景 COLLABORATION_PROVIDER 仅允许访问公网官方端点，禁止私网

INSERT INTO sys_outbound_whitelist (
  tenant_id, scene, protocol, host, port_start, port_end,
  allow_private, status, create_by, create_time, create_dept,
  update_by, update_time, remark
)
SELECT 1, 'COLLABORATION_PROVIDER', 'https', 'qyapi.weixin.qq.com', 443, 443,
       0, 1, 1, NOW(), 1,
       1, NOW(), '企业微信官方API出站白名单'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_outbound_whitelist
  WHERE tenant_id = 1
    AND scene = 'COLLABORATION_PROVIDER'
    AND protocol = 'https'
    AND host = 'qyapi.weixin.qq.com'
    AND port_start = 443
    AND port_end = 443
    AND del_flag = 0
);

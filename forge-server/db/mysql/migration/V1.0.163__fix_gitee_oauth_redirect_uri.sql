-- =====================================================================
-- V1.0.163 : 修正 Gitee OAuth 回调地址占位值
-- 背景：V1.0.161 / V1.0.162 预置的 redirect_uri 为
--       http://localhost:8580/social/callback/GITEE，指向后端 POST 换票
--       接口；而 Gitee 授权回调是 GET 请求且后端无该路由，照抄会导致
--       回调 404/405，授权流程中断。
--       正确配置：回调地址指向前端登录回调页（如
--       http://<前端域名>/forge/login/callback），前端收到 code/state 后
--       POST /social/callback 换取登录票据；且必须与 Gitee 开放平台
--       应用登记的回调地址完全一致。
-- 说明：仅修正仍为旧占位值的行，管理员已配置的真实地址不受影响；
--       UPDATE 以旧占位值为匹配条件，可重复执行。
-- =====================================================================

UPDATE sys_social_config
SET redirect_uri = 'http://localhost:3000/login/callback',
    remark = '请替换 client_id / client_secret 为 Gitee 开放平台应用凭据；回调地址填前端登录回调页完整地址（http://<前端域名>/forge/login/callback），须与 Gitee 应用登记的回调地址完全一致'
WHERE platform = 'GITEE'
  AND redirect_uri = 'http://localhost:8580/social/callback/GITEE'
  AND del_flag = 0;

UPDATE sys_social_app_config
SET redirect_uri = 'http://localhost:3000/login/callback',
    remark = '请替换 clientId / secret 为 Gitee 开放平台应用凭据；回调地址填前端登录回调页完整地址（http://<前端域名>/forge/login/callback），须与 Gitee 应用登记的回调地址完全一致'
WHERE redirect_uri = 'http://localhost:8580/social/callback/GITEE'
  AND del_flag = 0;

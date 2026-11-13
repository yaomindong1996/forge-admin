-- 修正 sys_tenant.system_theme 脏数据：
-- 该字段被前端 applyTenantConfig 当作十六进制主色使用（合法值如 #4242F7），
-- 但列默认值为 'light'（主题模式值），V1.0.158 社区租户等未显式指定主色的租户
-- 落入默认值后，前端 colorPalette 解析 'light' 抛异常并中断登录后的菜单加载，
-- 导致该租户用户登录后侧边栏空白。
-- 处理：非十六进制颜色值一律置 NULL，让前端回退默认主色。以旧值为条件，可重复执行。

UPDATE sys_tenant
SET system_theme = NULL,
    update_by    = 1,
    update_time  = NOW()
WHERE system_theme IS NOT NULL
  AND system_theme NOT REGEXP '^#[0-9a-fA-F]{3}$|^#[0-9a-fA-F]{6}$|^#[0-9a-fA-F]{8}$';

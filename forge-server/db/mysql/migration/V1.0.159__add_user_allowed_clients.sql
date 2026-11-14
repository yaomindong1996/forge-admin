-- 用户允许登录客户端列表：逗号分隔的 clientCode，空值表示不限制（所有客户端均可登录）
ALTER TABLE sys_user
    ADD COLUMN allowed_clients VARCHAR(500) DEFAULT NULL
        COMMENT '允许登录的客户端编码，逗号分隔（如 pc,app），空表示不限制'
        AFTER user_client;

package com.mdframe.forge.plugin.system.constant;

/**
 * @date 2025/12/15
 */
public interface SystemConstants {

    interface UserType {

        /**
         * 系统管理员。
         */
        int SYSTEM_ADMIN = 0;

        /**
         * 租户管理员。
         */
        int TENANT_ADMIN = 1;

        /**
         * 普通用户。
         */
        int NORMAL_USER = 2;
    }

    interface RoleDataScope {

        /**
         * 全部数据。
         */
        int ALL = 1;

        /**
         * 本租户数据。
         */
        int TENANT = 2;

        /**
         * 本组织数据。
         */
        int ORG = 3;

        /**
         * 本组织及子组织。
         */
        int ORG_AND_CHILD = 4;

        /**
         * 个人数据。
         */
        int SELF = 5;

        /**
         * 本行政区划数据。
         */
        int REGION = 7;
    }

    interface RoleOrgScope {

        /**
         * 租户全局适用。
         */
        int GLOBAL = 1;

        /**
         * 指定组织适用。
         */
        int CUSTOM = 2;
    }

    interface UserStatus {

        String LOCKED = "2";

        String UNLOCKED = "1";
    }
}

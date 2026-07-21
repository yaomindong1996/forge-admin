package com.mdframe.forge.plugin.job.constant;

/**
 * 定时任务管理权限标识。
 */
public final class JobPermissions {

    public static final String CONFIG_LIST = "system:jobConfig:list";
    public static final String CONFIG_ADD = "system:jobConfig:add";
    public static final String CONFIG_EDIT = "system:jobConfig:edit";
    public static final String CONFIG_REMOVE = "system:jobConfig:remove";
    public static final String CONFIG_START = "system:jobConfig:start";
    public static final String CONFIG_STOP = "system:jobConfig:stop";
    public static final String CONFIG_TRIGGER = "system:jobConfig:trigger";
    public static final String CONFIG_SYNC = "system:jobConfig:sync";
    public static final String CONFIG_DANGEROUS = "system:jobConfig:dangerous";
    public static final String LOG_LIST = "system:jobLog:list";
    public static final String LOG_DETAIL = "system:jobLog:detail";
    public static final String LOG_EXPORT = "system:jobLog:export";
    public static final String LOG_CLEAN = "system:jobLog:clean";
    public static final String API_TOKEN_LIST = "system:jobApiToken:list";
    public static final String API_TOKEN_ADD = "system:jobApiToken:add";
    public static final String API_TOKEN_REVOKE = "system:jobApiToken:revoke";
    public static final String API_TOKEN_ROTATE = "system:jobApiToken:rotate";

    private JobPermissions() {
    }
}

package com.mdframe.forge.starter.plugin.migration;

import java.util.regex.Pattern;

/** 只允许受控的插件路径和历史表，不能从描述文件注入任意 SQL 标识符。 */
public record PluginMigrationPlan(String pluginId, String location, String historyTable) {

    private static final String MAIN_HISTORY_SUFFIX = "_schema_history";
    private static final String PLUGIN_HISTORY_MARKER = "_plugin_";
    private static final int MAX_IDENTIFIER_LENGTH = 64;
    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern PLUGIN_ID = Pattern.compile("[a-z][a-z0-9-]{1,31}");

    public static PluginMigrationPlan from(String mainTable, String pluginId) {
        if (pluginId == null || !PLUGIN_ID.matcher(pluginId).matches()) {
            throw new IllegalArgumentException("插件迁移 ID 非法：" + pluginId);
        }
        if (mainTable == null || !IDENTIFIER.matcher(mainTable).matches()
                || !mainTable.endsWith(MAIN_HISTORY_SUFFIX) || mainTable.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("插件 " + pluginId + " 的主历史表必须是合法的 *_schema_history 标识符");
        }
        String prefix = mainTable.substring(0, mainTable.length() - MAIN_HISTORY_SUFFIX.length());
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException("插件 " + pluginId + " 的主历史表缺少工程前缀");
        }
        String table = prefix + PLUGIN_HISTORY_MARKER + pluginId.replace('-', '_') + "_history";
        if (table.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("插件 " + pluginId + " 的迁移历史表超过 64 字符：" + table);
        }
        return new PluginMigrationPlan(pluginId, "classpath:db/plugin/" + pluginId, table);
    }
}

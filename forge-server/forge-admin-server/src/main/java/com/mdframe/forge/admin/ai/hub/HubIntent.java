package com.mdframe.forge.admin.ai.hub;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 社区版全局 AI 助手轻量意图分类（纯对话路由 + 我的待办，不包含商业版应用构建意图）。
 */
enum HubIntent {
    MY_TODOS,
    CHAT;

    private static final Pattern GREETING = Pattern.compile(
            "^(你好|您好|嗨|哈喽|早上好|上午好|下午好|晚上好|在吗|hi|hello|hey)[!！。.,，?？~～\\s]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TODO_QUERY = Pattern.compile(
            "(查(看|一下)?|列出|显示|有(哪些|什么|多少)?)?我的待办|待办(事项|任务|列表|审批)|待我审批|我待处理");

    static HubIntent route(String message) {
        if (message == null || message.isBlank()) {
            return CHAT;
        }
        String normalized = message.strip();
        if (normalized.length() <= 40 && TODO_QUERY.matcher(normalized).find()) {
            return MY_TODOS;
        }
        return CHAT;
    }

    static boolean greeting(String message) {
        if (message == null) {
            return false;
        }
        return GREETING.matcher(message.strip().toLowerCase(Locale.ROOT)).matches();
    }
}

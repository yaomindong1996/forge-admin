package com.mdframe.forge.admin.ai.hub;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基于智能体可见名称与职责描述的轻量语义匹配；分数不足或并列时回落到通用对话。
 */
final class HubAgentMatch {

    private static final Pattern TERMS = Pattern.compile("[\\p{IsHan}]+|[\\p{IsLatin}0-9]{3,}");
    private static final Set<String> COMMON = Set.of(
            "帮我", "一下", "请帮", "麻烦", "智能", "助手", "用户", "提供",
            "支持", "需要", "可以", "处理", "根据", "是否", "如何", "什么",
            "怎么", "怎样", "配置", "设置", "使用", "说明", "介绍", "查看",
            "请问", "这个", "那个", "系统", "功能", "页面", "问题",
            "help", "with", "please", "the");

    private HubAgentMatch() {
    }

    static HubAssistantRouting.Agent best(String message, List<HubAssistantRouting.Agent> agents) {
        Set<String> query = terms(message);
        HubAssistantRouting.Agent best = null;
        int top = 0;
        int second = 0;
        for (var agent : agents) {
            int score = score(message, query, agent);
            if (score > top) {
                second = top;
                top = score;
                best = agent;
            } else {
                second = Math.max(score, second);
            }
        }
        return top >= 6 && top >= second + 3 ? best : null;
    }

    private static int score(String message, Set<String> query, HubAssistantRouting.Agent agent) {
        String name = agent.name().strip().toLowerCase(Locale.ROOT);
        if (name.codePointCount(0, name.length()) >= 3 && message.toLowerCase(Locale.ROOT).contains(name)) {
            return 10000;
        }
        Set<String> nameTerms = terms(name);
        Set<String> descriptionTerms = terms(Objects.toString(agent.description(), ""));
        return query.stream().mapToInt(term -> {
            int weight = term.length() >= 3 ? 3 : 1;
            return nameTerms.contains(term) ? weight * 2 : descriptionTerms.contains(term) ? weight : 0;
        }).sum();
    }

    private static Set<String> terms(String source) {
        var tokens = TERMS.matcher(source.toLowerCase(Locale.ROOT));
        Set<String> result = new HashSet<>();
        while (tokens.find()) {
            String word = tokens.group();
            if (Character.UnicodeScript.of(word.codePointAt(0)) == Character.UnicodeScript.HAN) {
                addHanTerms(result, word);
            } else {
                result.add(word);
            }
        }
        result.removeAll(COMMON);
        return result;
    }

    private static void addHanTerms(Set<String> result, String word) {
        int[] points = word.codePoints().toArray();
        for (int start = 0; start < points.length - 1; start++) {
            result.add(new String(points, start, 2));
            if (start + 2 < points.length) {
                result.add(new String(points, start, 3));
            }
        }
    }
}

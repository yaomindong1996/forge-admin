package com.mdframe.forge.plugin.system.support;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判断一次响应是否应记入下载记录，并解析展示用文件名。
 * 文件预览同样走 /api/file/download，只有显式下载意图才记录，避免把缩略图刷进列表。
 */
public final class FileDownloadLogSupport {

    public static final String INTENT_HEADER = "X-Forge-Download-Intent";
    public static final String SOURCE_FILE = "file";
    public static final String SOURCE_EXPORT = "export";

    private static final Pattern UTF8_NAME = Pattern.compile("filename\\*=(?:UTF-8'')?([^;]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_NAME = Pattern.compile("filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_ID = Pattern.compile("/api/file/download/([^/?#]+)");

    private FileDownloadLogSupport() {
    }

    public static boolean shouldRecord(String uri, String disposition, String intentHeader, int status) {
        // 未显式 setStatus 时部分容器返回 0，仍视为成功响应。
        if (status >= 400 || (status > 0 && status < 200) || disposition == null) {
            return false;
        }
        if (!disposition.toLowerCase(Locale.ROOT).contains("attachment")) {
            return false;
        }
        boolean fileApi = uri != null && uri.contains("/api/file/download/");
        boolean explicitDownload = "1".equals(intentHeader);
        return explicitDownload || !fileApi;
    }

    /** 超级管理员、租户管理员看本租户全部；其他人只看自己。返回 null 表示不按用户过滤。 */
    public static Long restrictedUserId(boolean seeAll, Long currentUserId) {
        return seeAll ? null : currentUserId;
    }

    public static String sourceOf(String uri) {
        return uri != null && uri.contains("/api/file/download/") ? SOURCE_FILE : SOURCE_EXPORT;
    }

    public static String fileIdOf(String uri) {
        if (uri == null) {
            return null;
        }
        Matcher matcher = FILE_ID.matcher(uri);
        if (!matcher.find()) {
            return null;
        }
        return decode(matcher.group(1));
    }

    public static String fileNameOf(String disposition) {
        if (disposition == null || disposition.isBlank()) {
            return "未命名文件";
        }
        Matcher utf8 = UTF8_NAME.matcher(disposition);
        if (utf8.find()) {
            return limit(decode(stripQuotes(utf8.group(1))));
        }
        Matcher plain = PLAIN_NAME.matcher(disposition);
        if (plain.find()) {
            return limit(decode(plain.group(1)));
        }
        return "未命名文件";
    }

    private static String stripQuotes(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private static String limit(String value) {
        if (value == null || value.isBlank()) {
            return "未命名文件";
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}

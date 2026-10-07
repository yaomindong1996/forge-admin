package com.mdframe.forge.starter.plugin.delivery;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static com.mdframe.forge.starter.plugin.delivery.PackagePathRules.require;

/** 不落盘、不执行包；Web 限额比源码 CLI 更保守，两者使用同一交付格式。 */
public final class PluginZipReader {
    public static final int MAX_ARCHIVE = 8 * 1024 * 1024;
    static final int MAX_FILE = 4 * 1024 * 1024;
    private static final int MAX_TOTAL = 32 * 1024 * 1024;
    private static final int MAX_ENTRIES = 2000;

    private PluginZipReader() {
    }

    public static Map<String, byte[]> read(byte[] bytes) {
        require(bytes != null && bytes.length <= MAX_ARCHIVE, "ZIP 超过 8 MiB 限制");
        ZipStructure zip = new ZipStructure(bytes);
        int end = zip.end();
        int count = zip.u16(end + 10);
        require(count > 0 && count <= MAX_ENTRIES && zip.u16(end + 4) == 0 && zip.u16(end + 6) == 0
                && count == zip.u16(end + 8), "ZIP 条目超限或分卷");
        long central = zip.u32(end + 16);
        require(central + zip.u32(end + 12) == end, "ZIP 目录非法或 ZIP64");
        int cursor = Math.toIntExact(central);
        Map<String, byte[]> files = new TreeMap<>();
        List<ZipStructure.Entry> entries = new ArrayList<>();
        HashSet<String> names = new HashSet<>();
        long total = 0;
        for (int index = 0; index < count; index++) {
            ZipStructure.Entry entry = zip.entry(cursor, (int) central);
            total += entry.size();
            require(total <= MAX_TOTAL, "ZIP 展开超过 32 MiB");
            require(names.add(PackagePathRules.normalized(entry.name())), "ZIP 路径重复或大小写/NFC 冲突");
            byte[] content = content(bytes, entry);
            if (!entry.directory()) {
                files.put(entry.name(), content);
            }
            entries.add(entry);
            cursor = entry.next();
        }
        require(cursor == end, "ZIP 目录长度不匹配");
        validateRanges(entries);
        validateAncestors(files.keySet(), names);
        return files;
    }

    private static byte[] content(byte[] bytes, ZipStructure.Entry entry) {
        Inflater inflater = new Inflater(true);
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes, entry.begin(), entry.packed())) {
            byte[] result;
            if (entry.method() == 0) {
                result = input.readNBytes(entry.size() + 1);
            } else {
                try (InflaterInputStream stream = new InflaterInputStream(input, inflater)) {
                    result = stream.readNBytes(entry.size() + 1);
                    require(inflater.finished() && inflater.getRemaining() == 0, "ZIP 压缩流长度不符");
                }
            }
            CRC32 crc = new CRC32();
            crc.update(result);
            require(result.length == entry.size() && crc.getValue() == entry.crc(), "ZIP 长度或 CRC 校验失败");
            require(entry.method() != 0 || entry.size() == entry.packed(), "ZIP 存储长度不符");
            return result;
        } catch (IOException exception) {
            throw new IllegalArgumentException("ZIP 压缩数据损坏", exception);
        } finally {
            inflater.end();
        }
    }

    private static void validateRanges(List<ZipStructure.Entry> entries) {
        entries.sort(Comparator.comparingInt(ZipStructure.Entry::local));
        long previous = 0;
        for (ZipStructure.Entry entry : entries) {
            require(entry.local() >= previous, "ZIP 文件范围重叠");
            previous = (long) entry.begin() + entry.packed();
        }
    }

    private static void validateAncestors(Iterable<String> files, HashSet<String> names) {
        HashSet<String> normalizedFiles = new HashSet<>();
        files.forEach(value -> normalizedFiles.add(PackagePathRules.normalized(value)));
        for (String path : names) {
            int end = path.lastIndexOf('/');
            while (end > 0) {
                require(!normalizedFiles.contains(path.substring(0, end)), "ZIP 文件和目录冲突");
                end = path.lastIndexOf('/', end - 1);
            }
        }
    }
}

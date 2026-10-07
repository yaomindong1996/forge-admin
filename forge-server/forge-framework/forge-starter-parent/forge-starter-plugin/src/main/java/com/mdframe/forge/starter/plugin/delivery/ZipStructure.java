package com.mdframe.forge.starter.plugin.delivery;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.mdframe.forge.starter.plugin.delivery.PackagePathRules.require;

/** 显式核对 central/local 两套头，不能让解压器悄悄接受差异路径或符号链接。 */
final class ZipStructure {
    private final byte[] bytes;
    private final ByteBuffer buffer;
    private final int end;

    ZipStructure(byte[] bytes) {
        this.bytes = bytes;
        buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        end = locateEnd();
    }

    int end() {
        return end;
    }

    private int locateEnd() {
        for (int index = bytes.length - 22; index >= Math.max(0, bytes.length - 65557); index--) {
            if (u32(index) == 0x06054b50L && index + 22 + u16(index + 20) == bytes.length) {
                return index;
            }
        }
        throw new IllegalArgumentException("ZIP 缺少合法结束目录");
    }

    Entry entry(int cursor, int central) {
        require(cursor + 46 <= bytes.length && u32(cursor) == 0x02014b50L, "ZIP 条目损坏");
        int flags = u16(cursor + 8);
        int method = u16(cursor + 10);
        require((flags & ~0x080e) == 0 && (flags & 1) == 0 && (method == 0 || method == 8),
                "ZIP 加密或压缩格式不支持");
        require(u16(cursor + 6) < 45 && u16(cursor + 34) == 0, "不支持 ZIP64 或分卷");
        int nameSize = u16(cursor + 28);
        int extraSize = u16(cursor + 30);
        int next = cursor + 46 + nameSize + extraSize + u16(cursor + 32);
        require(next <= end(), "ZIP 条目越界");
        byte[] rawName = slice(cursor + 46, cursor + 46 + nameSize);
        String name = name(rawName, flags);
        boolean directory = name.endsWith("/");
        String relative = PackagePathRules.validate(directory ? name.substring(0, name.length() - 1) : name);
        long mode = u32(cursor + 38) >>> 16;
        require((mode & 0xf000) == 0 || (mode & 0xf000) == (directory ? 0x4000 : 0x8000),
                "ZIP 包含链接或特殊文件");
        extra(cursor + 46 + nameSize, cursor + 46 + nameSize + extraSize);
        long size = u32(cursor + 24);
        long packed = u32(cursor + 20);
        require(size <= PluginZipReader.MAX_FILE && packed <= bytes.length, "ZIP 文件大小超过限制");
        int local = Math.toIntExact(u32(cursor + 42));
        int begin = local(local, central, new LocalHeader(rawName, flags, method, (int) packed,
                (int) size, u32(cursor + 16)));
        require(!directory || size == 0, "ZIP 目录不能含数据");
        return new Entry(relative, directory, method, (int) size, (int) packed, u32(cursor + 16),
                local, begin, next);
    }

    private int local(int local, int central, LocalHeader header) {
        require(local >= 0 && (long) local + 30 <= central && u32(local) == 0x04034b50L, "ZIP 本地头损坏");
        int nameSize = u16(local + 26);
        int begin = local + 30 + nameSize + u16(local + 28);
        require((long) begin + header.packed() <= central && u16(local + 6) == header.flags()
                && u16(local + 8) == header.method() && u16(local + 4) < 45,
                "ZIP 本地头不匹配或越界");
        if ((header.flags() & 8) == 0) {
            require(u32(local + 14) == header.crc() && u32(local + 18) == header.packed()
                    && u32(local + 22) == header.size(), "ZIP 本地长度/CRC 与目录不一致");
        }
        require(Arrays.equals(slice(local + 30, local + 30 + nameSize), header.rawName()), "ZIP 文件名不一致");
        extra(local + 30 + nameSize, begin);
        return begin;
    }

    private void extra(int start, int end) {
        int cursor = start;
        while (cursor < end) {
            require(cursor + 4 <= end && u16(cursor) != 1, "ZIP 扩展字段损坏或 ZIP64");
            cursor += 4 + u16(cursor + 2);
            require(cursor <= end, "ZIP 扩展字段越界");
        }
    }

    private String name(byte[] raw, int flags) {
        if ((flags & 0x800) == 0) {
            for (byte value : raw) {
                require(value >= 0, "ZIP 路径必须为 UTF-8/ASCII");
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("ZIP 路径不是有效 UTF-8", exception);
        }
    }

    int u16(int position) {
        require(position >= 0 && position + 2 <= bytes.length, "ZIP 字段越界");
        return Short.toUnsignedInt(buffer.getShort(position));
    }

    long u32(int position) {
        require(position >= 0 && position + 4 <= bytes.length, "ZIP 字段越界");
        return Integer.toUnsignedLong(buffer.getInt(position));
    }

    private byte[] slice(int begin, int end) {
        require(begin >= 0 && end >= begin && end <= bytes.length, "ZIP 字段越界");
        return Arrays.copyOfRange(bytes, begin, end);
    }

    record Entry(String name, boolean directory, int method, int size, int packed, long crc,
                 int local, int begin, int next) {
    }

    private record LocalHeader(byte[] rawName, int flags, int method, int packed, int size, long crc) {
    }
}

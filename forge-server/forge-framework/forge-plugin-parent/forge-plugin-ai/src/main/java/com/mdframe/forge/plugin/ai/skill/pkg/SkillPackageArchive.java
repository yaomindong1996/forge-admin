package com.mdframe.forge.plugin.ai.skill.pkg;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * 技能资料包 ZIP 内存解析器（不落盘、不拉远端依赖、不执行脚本，带 zip 炸弹与路径穿越防护）。
 */
@Component
public class SkillPackageArchive {

    public static final int ZIP_LIMIT = 10 * 1024 * 1024;
    public static final int FILE_LIMIT = 8 * 1024 * 1024;
    public static final int TOTAL_LIMIT = 64 * 1024 * 1024;
    public static final int MAX_FILES = 4096;
    public static final int MAX_ENTRIES = 8192;
    public static final int MAX_INSTRUCTIONS = 500_000;
    public static final String CONTRACT = "1.2.0";
    public static final String FORMAT = "skill_package_v1";

    private static final Set<String> TEXT_EXT = Set.of(
            "md", "txt", "json", "js", "mjs", "cjs", "ts", "tsx", "jsx", "py", "sh", "bash", "zsh",
            "yaml", "yml", "toml", "html", "htm", "css", "scss", "less", "svg", "xml", "csv", "vue",
            "go", "rs", "java", "kt", "rb", "php", "sql", "env", "gitignore", "dockerignore",
            "editorconfig", "properties", "gradle", "ini", "cfg", "conf");
    private static final Pattern FRONT_MATTER = Pattern.compile("^---\\r?\\n([\\s\\S]*?)\\r?\\n---\\r?\\n?");

    public record Dependency(String code, String version, String sha256) {
    }

    public record NodeRequirement(String type, String version) {
    }

    public record Manifest(int schemaVersion, String code, String name, String description, String version,
                           String contractVersion, String instructionsPath, List<String> runtimeKinds,
                           List<Dependency> dependencies, List<NodeRequirement> nodes) {
    }

    public record FileContent(String path, String content, String sha256, int sizeBytes) {
    }

    public record PackageContent(Manifest manifest, String archiveHash, String packageHash, String manifestJson,
                                 List<FileContent> files, int totalBytes) {
        public String instructions() {
            return files.stream()
                    .filter(f -> f.path().equals("SKILL.md"))
                    .findFirst()
                    .orElseThrow()
                    .content();
        }
    }

    private final ObjectMapper strict;

    public SkillPackageArchive(ObjectMapper mapper) {
        this.strict = mapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    public PackageContent read(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > ZIP_LIMIT
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw invalid("请选择10MiB以内的ZIP技能包");
        }
        try (var input = file.getInputStream()) {
            return read(input.readNBytes(ZIP_LIMIT + 1));
        } catch (IOException ex) {
            throw invalid("读取技能包失败");
        }
    }

    public PackageContent read(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > ZIP_LIMIT) {
            throw invalid("技能压缩包超过限制");
        }
        var files = new TreeMap<String, FileContent>();
        var paths = new HashSet<String>();
        int total = 0;
        int entries = 0;
        try (var channel = new SeekableInMemoryByteChannel(bytes);
             var zip = ZipFile.builder().setSeekableByteChannel(channel).setCharset(StandardCharsets.UTF_8).get()) {
            var iterator = zip.getEntries();
            while (iterator.hasMoreElements()) {
                var entry = iterator.nextElement();
                int kind = entry.getUnixMode() & 0170000;
                if (++entries > MAX_ENTRIES
                        || (kind != 0 && kind != 0100000 && kind != 0040000)
                        || entry.isUnixSymlink()
                        || !zip.canReadEntryData(entry)
                        || entry.getGeneralPurposeBit().usesEncryption()) {
                    throw invalid("技能包条目、链接或加密格式不可接受");
                }
                String rawName = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(entry.getRawName()))
                        .toString();
                if (!rawName.equals(entry.getName())) {
                    throw invalid("技能文件路径编码或规范化不一致");
                }
                String path = path(rawName, entry.isDirectory());
                if (!paths.add(path.toLowerCase(Locale.ROOT))) {
                    throw invalid("技能包存在重复文件或路径大小写冲突");
                }
                if (entry.isDirectory()) {
                    continue;
                }
                if (files.size() >= MAX_FILES || entry.getSize() < 0 || entry.getSize() > FILE_LIMIT) {
                    throw invalid("技能包文件过多或单文件过大");
                }
                if (!textPath(path)) {
                    continue;
                }
                byte[] data;
                try (var input = zip.getInputStream(entry)) {
                    data = input.readNBytes(FILE_LIMIT + 1);
                }
                if (data.length > FILE_LIMIT || data.length != entry.getSize()
                        || (total += data.length) > TOTAL_LIMIT
                        || entry.getCompressedSize() < 0
                        || data.length > Math.max(1, entry.getCompressedSize()) * 100L) {
                    throw invalid("技能包展开大小或压缩比超限");
                }
                var crc = new CRC32();
                crc.update(data);
                if (crc.getValue() != entry.getCrc()) {
                    throw invalid("技能文件校验失败");
                }
                String content;
                try {
                    content = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(data))
                            .toString();
                } catch (CharacterCodingException ex) {
                    continue;
                }
                if (content.indexOf('\0') >= 0
                        || content.chars().anyMatch(c -> c < 32 && c != '\n' && c != '\r' && c != '\t')) {
                    continue;
                }
                files.put(path, new FileContent(path, content, sha256(data), data.length));
            }
            return assemble(unwrapRoot(files), sha256(bytes), total);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("技能ZIP格式、校验或UTF8编码无效");
        }
    }

    public PackageContent stored(String manifestJson, List<FileContent> source) {
        try {
            var files = new TreeMap<String, FileContent>();
            int total = 0;
            for (var f : source) {
                byte[] bytes = f.content().getBytes(StandardCharsets.UTF_8);
                if (!path(f.path(), false).equals(f.path()) || !textPath(f.path()) || bytes.length > FILE_LIMIT
                        || bytes.length != f.sizeBytes() || !sha256(bytes).equals(f.sha256())
                        || files.putIfAbsent(f.path(), f) != null) {
                    throw invalid("技能版本文件已失效");
                }
                total += bytes.length;
            }
            if (files.size() > MAX_FILES || total > TOTAL_LIMIT) {
                throw invalid("技能版本文件超限");
            }
            var result = assemble(files, "", total);
            if (!result.manifestJson().equals(manifestJson)) {
                throw invalid("技能版本清单已发生变化");
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("技能版本快照不可读");
        }
    }

    public Manifest manifest(String value) {
        try {
            var manifest = strict.readValue(value, Manifest.class);
            validate(manifest);
            return manifest;
        } catch (Exception ex) {
            throw invalid("技能清单不可读");
        }
    }

    private PackageContent assemble(SortedMap<String, FileContent> files, String archiveHash, int total)
            throws IOException {
        if (!files.containsKey("SKILL.md")) {
            throw invalid("ZIP根目录须包含SKILL.md");
        }
        FileContent manifestFile = files.get("skill.json");
        if (manifestFile == null) {
            String synthesized = synthesizeManifest(files.get("SKILL.md").content());
            byte[] bytes = synthesized.getBytes(StandardCharsets.UTF_8);
            manifestFile = new FileContent("skill.json", synthesized, sha256(bytes), bytes.length);
            files = new TreeMap<>(files);
            files.put("skill.json", manifestFile);
            total += bytes.length;
        }
        var manifest = strict.readValue(manifestFile.content(), Manifest.class);
        validate(manifest);
        if (files.get("SKILL.md").content().isBlank() || files.get("SKILL.md").content().length() > MAX_INSTRUCTIONS) {
            throw invalid("SKILL.md说明须为1到" + MAX_INSTRUCTIONS + "字符");
        }
        String canonical = strict.writeValueAsString(manifest);
        StringBuilder content = new StringBuilder(canonical);
        for (var file : files.values()) {
            content.append('\n').append(file.path()).append(':').append(file.sha256());
        }
        return new PackageContent(manifest, archiveHash, sha256(content.toString()),
                canonical, List.copyOf(files.values()), total);
    }

    private static SortedMap<String, FileContent> unwrapRoot(SortedMap<String, FileContent> files) {
        if (files.containsKey("SKILL.md") || files.containsKey("skill.json")) {
            return files;
        }
        String prefix = null;
        for (String path : files.keySet()) {
            int slash = path.indexOf('/');
            if (slash <= 0) {
                return files;
            }
            String root = path.substring(0, slash + 1);
            if (prefix == null) {
                prefix = root;
            } else if (!prefix.equals(root)) {
                return files;
            }
        }
        if (prefix == null) {
            return files;
        }
        var unwrapped = new TreeMap<String, FileContent>();
        for (var entry : files.entrySet()) {
            String next = entry.getKey().substring(prefix.length());
            if (next.isEmpty()) {
                continue;
            }
            unwrapped.put(next, new FileContent(next, entry.getValue().content(),
                    entry.getValue().sha256(), entry.getValue().sizeBytes()));
        }
        return unwrapped;
    }

    private String synthesizeManifest(String skillMd) throws IOException {
        Map<String, String> meta = frontMatter(skillMd);
        String name = firstNonBlank(meta.get("name"), "imported_skill");
        String code = normalizeCode(name);
        if (!code(code)) {
            code = "imported_skill";
        }
        String description = firstNonBlank(meta.get("description"), name);
        if (description.length() > 1000) {
            description = description.substring(0, 1000);
        }
        String version = firstNonBlank(meta.get("version"), meta.get("metadata.version"), "1.0.0");
        if (!version(version)) {
            version = "1.0.0";
        }
        String display = name.length() > 80 ? name.substring(0, 80) : name;
        return strict.writeValueAsString(new Manifest(1, code, display, description, version, CONTRACT, "SKILL.md",
                List.of("agent_harness"), List.of(), List.of()));
    }

    private static Map<String, String> frontMatter(String skillMd) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = FRONT_MATTER.matcher(skillMd == null ? "" : skillMd);
        if (!matcher.find()) {
            return values;
        }
        String section = null;
        for (String line : matcher.group(1).split("\\r?\\n")) {
            if (line.matches("[A-Za-z0-9_-]+:\\s*$")) {
                section = line.substring(0, line.indexOf(':')).trim();
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            if ((section != null && line.startsWith(" ")) || line.startsWith("\t")) {
                values.put(section + "." + key, value);
            } else {
                section = null;
                values.put(key, value);
            }
        }
        return values;
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            return "";
        }
        String code = value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_");
        if (code.startsWith("_")) {
            code = code.substring(1);
        }
        if (!code.isEmpty() && Character.isDigit(code.charAt(0))) {
            code = "skill_" + code;
        }
        return code.length() > 80 ? code.substring(0, 80) : code;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private void validate(Manifest m) {
        if (m.schemaVersion() != 1 || !code(m.code()) || m.name() == null || m.name().isBlank()
                || m.name().length() > 80
                || m.description() == null || m.description().length() > 1000
                || !version(m.version()) || m.contractVersion() == null || m.contractVersion().length() > 32
                || !"SKILL.md".equals(m.instructionsPath())
                || m.runtimeKinds() == null || m.runtimeKinds().isEmpty() || m.runtimeKinds().size() > 3
                || new HashSet<>(m.runtimeKinds()).size() != m.runtimeKinds().size()
                || !Set.of("simple_chat", "agent_harness", "graph_workflow").containsAll(m.runtimeKinds())
                || m.dependencies() == null || m.dependencies().size() > 16
                || m.nodes() == null || m.nodes().size() > 16) {
            throw invalid("技能清单字段、入口、版本或运行类型无效");
        }
        var dependencies = new HashSet<String>();
        for (var d : m.dependencies()) {
            if (d == null || !code(d.code()) || !version(d.version())
                    || d.sha256() == null || !d.sha256().matches("[a-f0-9]{64}")
                    || d.code().equals(m.code()) || !dependencies.add(d.code())) {
                throw invalid("技能依赖须为不重复的精确版本和摘要");
            }
        }
        var nodes = new HashSet<String>();
        for (var n : m.nodes()) {
            if (n == null || n.type() == null || !n.type().matches("[A-Z][A-Z0-9_]{0,63}")
                    || n.version() == null || !n.version().matches("[0-9]{1,6}") || !nodes.add(n.type())) {
                throw invalid("节点要求须为不重复的类型和精确版本");
            }
        }
    }

    private static boolean textPath(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        if ("SKILL.md".equals(name) || "skill.json".equals(name)
                || "Dockerfile".equals(name) || "Makefile".equals(name)) {
            return true;
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return false;
        }
        return TEXT_EXT.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String path(String name, boolean directory) {
        String value = directory && name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        if (value.isEmpty() || value.length() > 200 || value.contains("\\") || value.contains(":")
                || value.startsWith("/")
                || !Normalizer.isNormalized(value, Normalizer.Form.NFC)
                || value.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw invalid("技能文件路径无效");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.startsWith(".") || "__MACOSX".equals(part)) {
                throw invalid("技能包包含隐藏或越界路径");
            }
        }
        return value;
    }

    public static boolean code(String value) {
        return value != null && value.matches("[a-z][a-z0-9_]{0,79}");
    }

    public static boolean version(String value) {
        if (value == null || value.length() > 80
                || !value.matches("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})(-[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*)?")) {
            return false;
        }
        int dash = value.indexOf('-');
        if (dash >= 0) {
            for (String part : value.substring(dash + 1).split("\\.")) {
                if (part.matches("[0-9]+") && part.length() > 1 && part.startsWith("0")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static BusinessException invalid(String value) {
        return new BusinessException(400, value);
    }
}

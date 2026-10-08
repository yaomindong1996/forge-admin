package com.mdframe.forge.starter.plugin.license;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/** 小尺寸严格 JSON，先验原字节签名再反序列化，禁止重序列化后验签。 */
public final class LicenseCodec {
    public static final int MAX_DOCUMENT_BYTES = 32768;
    public static final int MAX_PAYLOAD_BYTES = 16384;
    private static final ObjectMapper JSON = new ObjectMapper()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    private LicenseCodec() {
    }

    public static byte[] payload(LicensePayload payload) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(payload);
        if (bytes.length > MAX_PAYLOAD_BYTES) {
            throw new IOException("许可证载荷过大");
        }
        return bytes;
    }

    public static byte[] document(LicenseEnvelope envelope) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(envelope);
        if (bytes.length > MAX_DOCUMENT_BYTES) {
            throw new IOException("许可证文档过大");
        }
        return bytes;
    }

    public static LicensePayload verify(byte[] document, Map<String, PublicKey> trustedKeys)
            throws IOException, GeneralSecurityException {
        if (document == null || document.length == 0 || document.length > MAX_DOCUMENT_BYTES) {
            throw new IOException("许可证文档大小非法");
        }
        LicenseEnvelope envelope = JSON.readValue(document, LicenseEnvelope.class);
        PublicKey key = trustedKeys.get(envelope.keyId());
        if (key == null) {
            throw new GeneralSecurityException("许可证公钥不受信任");
        }
        byte[] payload = decode(envelope.payload(), MAX_PAYLOAD_BYTES);
        byte[] signature = decode(envelope.signature(), 64);
        if (signature.length != 64) {
            throw new GeneralSecurityException("许可证签名长度非法");
        }
        Signature verifier = Signature.getInstance(LicenseEnvelope.ALGORITHM);
        verifier.initVerify(key);
        verifier.update(payload);
        if (!verifier.verify(signature)) {
            throw new GeneralSecurityException("许可证签名无效");
        }
        return JSON.readValue(payload, LicensePayload.class);
    }

    public static PublicKey publicKey(byte[] pem) throws GeneralSecurityException {
        String text = new String(pem, java.nio.charset.StandardCharsets.US_ASCII);
        String encoded = text.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance(LicenseEnvelope.ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }

    public static byte[] readFile(Path path, int maxBytes) throws IOException {
        if (path == null || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("许可证文件不存在或不是普通文件");
        }
        // 读流仍有上限，不能只相信读取前的 Files.size（文件可能同时替换）。
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length == 0 || bytes.length > maxBytes) {
                throw new IOException("许可证文件大小非法");
            }
            return bytes;
        }
    }

    public static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String value, int maxBytes) throws IOException {
        if (value.isEmpty() || value.length() > (maxBytes * 4 + 2) / 3 || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IOException("许可证编码非法");
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length > maxBytes || !encode(bytes).equals(value)) {
                throw new IOException("许可证编码或大小非法");
            }
            return bytes;
        } catch (IllegalArgumentException ex) {
            throw new IOException("许可证编码非法", ex);
        }
    }
}

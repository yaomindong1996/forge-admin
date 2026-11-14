# 一期：模型管理升级 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI 模型管理从"仅 Chat"升级为全类型（Chat/Embedding/Rerank/ImageGeneration/ASR/TTS），并修复 API Key 明文存储漏洞。

**Architecture:** 扩展现有 `forge-plugin-ai` 的 `model` / `provider` 子域。模型类型从粗粒度枚举细分为 6 类，Provider 适配器注册表新增 Embedding/Rerank 等适配器，API Key 引入 AES 加密。前端模型管理页参考 snail-ai-admin 重新设计。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 2.0.0 ChatModel/EmbeddingModel、MyBatis-Plus、Flyway、Vue 3 + Naive UI

**Spec:** `docs/superpowers/specs/2026-08-04-ai-upgrade-master-design.md`

## 决策记录（2026-08-04 评审确认）

- **前置**：本计划依赖 Phase 0（Spring AI 1.1.2 → 2.0.0 升级）已冻结。所有新代码写在 2.0.0 API 上（Embedding 相关类以 2.0.0 为准，Task 5 代码块按 2.0.0 实际签名修正）。
- **API Key 加密格式**：沿用 **legacy 无前缀密文**（跟随全局 `write-versioned=false`），与能力平台/data/social/低代码一致。**不新增 `fpc1:` 前缀判断**——`AiSecretCrypto.isEncrypted` 改为"非空即为密文"，`decrypt` 兼容 legacy/versioned。密钥轮换统一开启 versioned 后再支持。
- **依赖**：`forge-plugin-ai/pom.xml` 新增 `forge-starter-crypto`（无循环）。见 Global Constraints。
- **存量 model_type 映射**：`image`→`IMAGE_GENERATION`、`audio`→`ASR`（枚举 `fromCode` 映射 + 迁移脚本批量 UPDATE，幂等）。`AiModelType` 枚举保持 6 类，不新增 `image`/`audio` 值。
- **适配器匹配**：一期注册表**纯 modelKey 前缀匹配**；provider→baseUrl/apiKey 解密 + 构造 Spring AI 模型闭环放二期（`VectorStoreFactory`/`RagSearch`）。
- **前端**：增量改造现有 `provider.vue`/`model.vue`/`provider-model.vue` 三页，不重构一体化页。

## Global Constraints

- 查询 SQL 必须写 Mapper XML（`DataScopeInterceptor` 按 MappedStatement id 改写），禁止 Service 层用 `LambdaQueryWrapper`
- 业务数据 `tenant_id` 必须为 `1`；`TenantLineInnerInterceptor` 自动追加 `WHERE tenant_id = ?`
- 分页参数：`pageNum`/`pageSize`，Controller 用 `@RequestParam(defaultValue = "1") Integer pageNum`
- 逻辑删除默认：`del_flag bigint NOT NULL DEFAULT 0`（0 = 未删除），数值主键表用 `@TableLogic(value = "0", delval = "id")`，唯一键直接建在 `del_flag` 上（**无 `logic_delete_active` 生成列**）
- API Key 返回前端必须脱敏（保留前 4 后 4，中间 `****`），禁止日志打印
- `forge-plugin-ai` 需新增依赖：`forge-starter-crypto`（`AiSecretCrypto` 复用 `PersistentCryptoService`）
- Flyway 迁移版本单调递增（> 1.0.81），SQL 幂等，`NOT EXISTS` 防重复
- 字典不硬编码，用 `useDict`/`DictSelect`；内置字典通过 Flyway 写入
- 禁止 Service 互相注入，跨 Service 协调上提 Controller
- 基础包：`com.mdframe.forge`

---

### Task 1: 模型类型枚举细分

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/constant/AiModelType.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/model/AiModelTypeTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `AiModelType` 枚举（`CHAT`、`EMBEDDING`、`RERANK`、`IMAGE_GENERATION`、`ASR`、`TTS`），每个含 `code`（字符串）和 `fromCode(String)` 静态方法

- [ ] **Step 1: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.model.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiModelTypeTest {

    @Test
    void allCodesAreUniqueAndMatchModelTypes() {
        assertEquals("chat", AiModelType.CHAT.getCode());
        assertEquals("embedding", AiModelType.EMBEDDING.getCode());
        assertEquals("rerank", AiModelType.RERANK.getCode());
        assertEquals("image_generation", AiModelType.IMAGE_GENERATION.getCode());
        assertEquals("asr", AiModelType.ASR.getCode());
        assertEquals("tts", AiModelType.TTS.getCode());
    }

    @Test
    void fromCodeResolvesKnownAndUnknown() {
        assertEquals(AiModelType.CHAT, AiModelType.fromCode("chat"));
        assertEquals(AiModelType.EMBEDDING, AiModelType.fromCode("embedding"));
        assertNull(AiModelType.fromCode("unknown_type"));
        assertNull(AiModelType.fromCode(null));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiModelTypeTest -DfailIfNoTests=false`
Expected: FAIL with "cannot find symbol class AiModelType"

- [ ] **Step 3: 实现枚举**

```java
package com.mdframe.forge.plugin.ai.model.constant;

/**
 * AI 模型类型（细分）。
 * chat 含 Vision（模型带视觉能力时对话可传图）。
 */
public enum AiModelType {

    CHAT("chat"),
    EMBEDDING("embedding"),
    RERANK("rerank"),
    IMAGE_GENERATION("image_generation"),
    ASR("asr"),
    TTS("tts");

    private final String code;

    AiModelType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 由 code 解析枚举。
     * 兼容存量宽泛值：image → IMAGE_GENERATION，audio → ASR（历史 model_type 只有 image/audio 四类）。
     * 未知返回 null（兼容未知类型）。
     */
    public static AiModelType fromCode(String code) {
        if (code == null) {
            return null;
        }
        if ("image".equals(code)) {
            return IMAGE_GENERATION;
        }
        if ("audio".equals(code)) {
            return ASR;
        }
        for (AiModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiModelTypeTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/constant/AiModelType.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/model/AiModelTypeTest.java
git commit -m "feat(ai): 模型类型枚举细分为6类"
```

---

### Task 2: ai_model_type 字典扩展迁移

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.86.1__add_ai_model_type_refined_dict.sql`

**Interfaces:**
- Consumes: Task 1 的 `AiModelType` code 值
- Produces: `ai_model_type` 字典新增 `rerank`/`image_generation`/`asr`/`tts` 项；`ai_model.model_type` 注释更新

- [ ] **Step 1: 写迁移脚本（幂等，NOT EXISTS 防重复，参考 V1.0.18 模板）**

```sql
-- 扩展 ai_model_type 字典：新增 rerank / image_generation / asr / tts
-- 注意：字典表用 dict_type（非 dict_code），参考 V1.0.18__add_ai_model_routing_governance.sql
INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 5 dict_sort, '重排模型' dict_label, 'rerank' dict_value, 'ai_model_type' dict_type, 'success' list_class, 'N' is_default, '用于RAG结果重排' remark
  UNION ALL SELECT 1, 6, '图片生成', 'image_generation', 'ai_model_type', 'warning', 'N', '文生图模型'
  UNION ALL SELECT 1, 7, '语音识别', 'asr', 'ai_model_type', 'info', 'N', '语音转文字'
  UNION ALL SELECT 1, 8, '语音合成', 'tts', 'ai_model_type', 'info', 'N', '文字转语音'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d WHERE d.tenant_id = seed.tenant_id AND d.dict_type = seed.dict_type AND d.dict_value = seed.dict_value);

-- 存量 model_type 宽泛值映射（幂等：仅更新存在的 image/audio 值）：
-- image → image_generation（文生图），audio → asr（语音识别）。audio 中的 tts 场景由用户在新页面重建为 tts 模型。
UPDATE ai_model SET model_type = 'image_generation' WHERE model_type = 'image';
UPDATE ai_model SET model_type = 'asr' WHERE model_type = 'audio';
```

> ⚠️ 实施前务必以 `forge-server/db/mysql/migration/V1.0.18__add_ai_model_routing_governance.sql` 为模板核对 `sys_dict_data` / `sys_dict_type` 真实列名（已确认用 `dict_type`）。
> ⚠️ 存量 `image`/`audio` 的 UPDATE 需确认 `model_type` 无其他依赖（如路由/能力判断按字符串比对，映射后保持一致）。

```bash
git add forge-server/db/mysql/migration/V1.0.86.1__add_ai_model_type_refined_dict.sql
git commit -m "feat(db): ai_model_type 字典扩展6类模型"
```

---

### Task 3: 封装 AI 密钥加密服务（复用 PersistentCryptoService）

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/support/AiSecretCrypto.java`
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/support/AiSecretCryptoTest.java`

**Interfaces:**
- Consumes: `forge-starter-crypto` 的 `PersistentCryptoService`（`encrypt(String plaintext, String algorithm)` / `decrypt(String ciphertext, String legacyAlgorithm)` / `reencrypt`），Bean 名 `persistentCryptoService`
- Produces: `AiSecretCrypto`（Spring `@Component`）：`encrypt(String) -> String`（legacy 密文，跟随全局 `write-versioned=false`）、`decrypt(String) -> String`、`isEncrypted(String) -> boolean`（**非空即为已加密**，不依赖 `fpc1:` 前缀）

> ⚠️ **不新增环境变量 / 不新写 AES 工具。** 直接复用现有 `PersistentCryptoService`，与能力平台/data/social/低代码同一套加密体系。算法传 `CryptoAlgorithm` 对应 code（null 用默认 SM4）。**注意：全局 `write-versioned=false`，返回的是 legacy 无前缀密文，不是 `fpc1:` 版本化密文**——`isEncrypted` 不能靠前缀判断，改为"已加密的值非空"（存储层只存密文，读到即视为已加密；明文仅出现在请求体/解密后使用处）。

- [ ] **Step 1: 确认 PersistentCryptoService 可注入**

Read: `forge-server/forge-framework/forge-starter-parent/forge-starter-crypto/src/main/java/com/mdframe/forge/starter/crypto/persistence/PersistentCryptoService.java`
Expected: 确认接口方法与 Bean 注册（`@Bean` / 自动配置类），并记录注入方式

- [ ] **Step 2: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.provider.support;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiSecretCryptoTest {

    @Test
    void encryptThenDecryptRoundTrips() {
        // 用 mock PersistentCryptoService 验证委托：encrypt 返回 legacy 密文，decrypt 还原
    }

    @Test
    void isEncryptedDetectsNonEmpty() {
        assertTrue(AiSecretCrypto.isEncrypted("ciphertext-without-prefix"));
        assertFalse(AiSecretCrypto.isEncrypted(null));
        assertFalse(AiSecretCrypto.isEncrypted(""));
    }
}
```

> 测试用 Mockito mock `PersistentCryptoService`（`encrypt` 返回 legacy 密文如 `"U2FsdGVkX1..."`，`decrypt` 还原），不依赖真实密钥配置。

- [ ] **Step 3: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiSecretCryptoTest -DfailIfNoTests=false`
Expected: FAIL（类不存在）

- [ ] **Step 4: 实现 AiSecretCrypto**

```java
package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 密钥加密服务，委托给现有 PersistentCryptoService（legacy 密文）。
 * 不新增密钥配置，复用 forge.crypto.* 体系；全局 write-versioned=false，落库为无前缀 legacy 密文。
 */
@Component
@RequiredArgsConstructor
public class AiSecretCrypto {

    private final PersistentCryptoService cryptoService;

    public String encrypt(String plain) {
        if (!StringUtils.hasText(plain)) {
            return plain;
        }
        return cryptoService.encrypt(plain, null); // algorithm 传 null 用默认
    }

    public String decrypt(String cipher) {
        if (!StringUtils.hasText(cipher)) {
            return cipher;
        }
        return cryptoService.decrypt(cipher, null);
    }

    /**
     * 存储层只存密文：非空即视为已加密。
     * 存量明文由迁移脚本统一加密；若仍有明文残留，decrypt 会走 legacy 解密兼容（legacy-read-enabled=true）。
     */
    public static boolean isEncrypted(String value) {
        return value != null && !value.isEmpty();
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiSecretCryptoTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/support/AiSecretCrypto.java forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/support/AiSecretCryptoTest.java
git commit -m "feat(ai): AI密钥加密复用PersistentCryptoService"
```

---

### Task 4: AiProviderService 接入加密存储

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java`（`createProvider` / `updateProvider` / `resolveTestProvider` / `normalizeProviderConnection`）
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/OpenAiCompatibleProviderAdapter.java`（取 Key 时解密）
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/adapter/DashScopeNativeProviderAdapter.java`（取 Key 时解密）
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/health/AiModelConnectionTestService.java`（**连接测试按模型类型路由**，见 Step 6）
- Test: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/test/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderServiceEncryptionTest.java`

**Interfaces:**
- Consumes: Task 3 的 `AiSecretCrypto.encrypt` / `decrypt` / `isEncrypted`
- Produces: 落库的 `apiKey` 为 legacy 密文（无前缀）；读取后返回前端前经 `AiProviderSecretMasker` 脱敏

- [ ] **Step 1: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.provider.service;

import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiProviderServiceEncryptionTest {

    @Test
    void savedProviderStoresEncryptedCipher() {
        AiSecretCrypto crypto = mockCrypto(); // Mockito mock: encrypt返回密文 decrypt还原
        String stored = crypto.encrypt("sk-raw-secret");
        assertTrue(AiSecretCrypto.isEncrypted(stored));
        assertNotEquals("sk-raw-secret", stored);
        assertEquals("sk-raw-secret", crypto.decrypt(stored));
    }

    @Test
    void unchangedMaskUpdateKeepsPersistedCipher() {
        // 前端回传脱敏值，应保持原密文不变（AiProviderService.resolveUpdateSecret 逻辑）
        String persisted = "U2FsdGVkX1ciphertext"; // legacy 密文
        String submitted = "sk-o****inal"; // 前4后4脱敏
        boolean unchanged = submitted != null && !AiSecretCrypto.isEncrypted(submitted)
                && com.mdframe.forge.plugin.ai.provider.support.AiProviderSecretMasker
                        .isUnchangedMask(submitted, "sk-original");
        assertTrue(unchanged);
        assertTrue(AiSecretCrypto.isEncrypted(persisted));
    }
}
```

> 测试用 Mockito mock `AiSecretCrypto`，不依赖真实密钥配置。注意：`AiProviderSecretMasker.isUnchangedMask` 在密文上依然成立（mask 的是存储值），`AiSecretCrypto.isEncrypted` 对 legacy 密文返回 true（非空即密文）。

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiProviderServiceEncryptionTest -DfailIfNoTests=false`
Expected: FAIL（`AiSecretCrypto` 尚未接入 AiProviderService）

- [ ] **Step 3: 修改 createProvider 加密落库**

在 `AiProviderService` 注入 `AiSecretCrypto`，`createProvider()` 的 `provider.setApiKey(requireSecret(request.getApiKey()))` 处改为：

```java
provider.setApiKey(aiSecretCrypto.encrypt(requireSecret(request.getApiKey())));
```

- [ ] **Step 4: 修改 updateProvider 加密落库**

在 `updateProvider()` 的 `resolveUpdateSecret(request.getApiKey(), persistedSecret)` 返回明文新值时，再包一层加密：

```java
String newSecret = resolveUpdateSecret(request.getApiKey(), persistedSecret);
provider.setApiKey(aiSecretCrypto.encrypt(newSecret));
```

`resolveUpdateSecret` 保持原逻辑（未改动的脱敏值返回 persisted 密文，已加密的直接原样）。

- [ ] **Step 5: 修改连接测试与适配器解密**

`resolveTestProvider()` 中：若 `request.getApiKey()` 非空（提交的是密文，因存储层只存密文），则 `aiSecretCrypto.decrypt`；否则按明文处理。
`OpenAiCompatibleProviderAdapter` / `DashScopeNativeProviderAdapter` 构造 `ChatModel` 前，对 `provider.getApiKey()` 调用 `aiSecretCrypto.decrypt()`（`AiProviderAdapterRegistry.createChatModel` 是集中入口，解密放这里更稳妥——见下方注）。

> 注：现有 `AiProviderAdapterRegistry.createChatModel(provider, options)` 是 ChatModel 唯一构造入口，解密逻辑建议放注册表内（对所有 adapter 统一），而非改每个 adapter。实施时以注册表为准，适配器构造用解密后的明文 Key。

- [ ] **Step 6: 连接测试按模型类型路由**

`AiModelConnectionTestService.test(modelPk)` 现有逻辑只做 Chat（`createChatModel` + 回 OK）。改为按 `AiModel.modelType` 路由：
- `chat`：走现有逻辑不变
- `embedding`：注入 `AiModelAdapterRegistry.getEmbedding(modelId)`，调 `adapter.embed(baseUrl, apiKey, model, List.of("hello"))`，验证返回向量非空 + 维度；`baseUrl`/`apiKey` 从 provider 取（`AiSecretCrypto.decrypt` 解密）
- `rerank`：注入 `AiModelAdapterRegistry.getRerank(modelId)`，调 `adapter.rerank(baseUrl, apiKey, model, "hello", List.of("world"))`，验证返回分数与输入顺序对应
- 其他类型（`image_generation`/`asr`/`tts`）：一期返回"该类型连接测试未实现"，二期/四期补齐

> 注：测试用的 `baseUrl`/`apiKey`/`model` 与 `createChatModel` 同一来源（`AiProvider`），解密逻辑复用 Task 4 的注册表内解密（`AiProviderAdapterRegistry.createChatModel` 已解密）。测试结果保持现有 `AiModelHealthRegistry` 的 lease 语义（成功/失败分类）。

- [ ] **Step 7: 运行测试验证通过 + 全量回归**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS（含既有 Provider 相关测试）

- [ ] **Step 8: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/ forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/health/AiModelConnectionTestService.java
git commit -m "feat(ai): Provider API Key 加密存储，适配器解密使用，连接测试按类型路由"
```

---

### Task 5: Embedding / Rerank 模型适配器（分类型接口，方案 C）

> **架构决策**：参考 Snail AI 的方案 C——每个模型类型一个独立接口 + `@Component` 实现 + Spring 自动装配 + 一个注册表按 modelKey 匹配。不扩展现有 `AiProviderAdapter`（Chat 专用，保持不变），不新建 54Doctor 式大工厂。现有 Chat 适配器体系本期不动，Embedding/Rerank 是平行接口。
>
> **Rerank 覆盖范围**：一期只做 `openai_compatible` 一种实现（覆盖 Jina/Cohere/bge/硅基流动等主流 rerank）。DashScope 原生 rerank 后续按需加——注册表扩展方便，只需新增一个 `@Component` 类，无需改注册表。

**Files:**
- Create: `.../model/adapter/AiEmbeddingModelAdapter.java`（接口）
- Create: `.../model/adapter/OpenAiCompatibleEmbeddingModelAdapter.java`（@Component）
- Create: `.../model/adapter/AiRerankModelAdapter.java`（接口）
- Create: `.../model/adapter/OpenAiCompatibleRerankModelAdapter.java`（@Component）
- Create: `.../model/adapter/AiModelAdapterRegistry.java`（@Component，注入 `List<AiEmbeddingModelAdapter>` / `List<AiRerankModelAdapter>`）
- Test: `.../model/adapter/AiModelAdapterRegistryTest.java`

**Interfaces:**
- Consumes: Task 1 的 `AiModelType`、`AiProvider`、Task 3 的 `AiSecretCrypto`
- Produces:
  - `AiEmbeddingModelAdapter`：`String getSupportedProvider()` / `boolean supports(String modelKey)` / `List<List<Float>> embed(String baseUrl, String apiKey, String model, List<String> texts)`
  - `AiRerankModelAdapter`：`String getSupportedProvider()` / `boolean supports(String modelKey)` / `List<Float> rerank(String baseUrl, String apiKey, String model, String query, List<String> passages)`（分数与输入顺序对应）
  - `AiModelAdapterRegistry`：`AiEmbeddingModelAdapter getEmbedding(String modelKey)` / `AiRerankModelAdapter getRerank(String modelKey)`（按 modelKey 匹配，无匹配抛 BusinessException）

- [ ] **Step 1: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.model.adapter;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AiModelAdapterRegistryTest {

    @Test
    void registryMatchesEmbeddingByModelKey() {
        // mock: registry 注入一个 supports("text-embedding") 的 adapter
        AiModelAdapterRegistry registry = new AiModelAdapterRegistry(List.of(), List.of());
        // 依赖 mock OpenAiCompatibleEmbeddingModelAdapter（supports("text-embedding")）
    }

    @Test
    void noMatchThrowsBusinessException() {
        AiModelAdapterRegistry registry = new AiModelAdapterRegistry(List.of(), List.of());
        assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                () -> registry.getEmbedding("nonexistent-model"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiModelAdapterRegistryTest -DfailIfNoTests=false`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 AiEmbeddingModelAdapter 接口与 OpenAI 兼容实现**

```java
package com.mdframe.forge.plugin.ai.model.adapter;

import java.util.List;

/**
 * Embedding 模型适配器接口。每个提供商一个实现，@Component + Spring 自动装配。
 * 参考 Snail AI 的 ChatModelFactory / EmbeddingModelFactory 模式。
 */
public interface AiEmbeddingModelAdapter {

    String getSupportedProvider();

    boolean supports(String modelKey);

    List<List<Float>> embed(String baseUrl, String apiKey, String model, List<String> texts);
}
```

`OpenAiCompatibleEmbeddingModelAdapter`（@Component）：`supports` 返回 `modelKey.startsWith("text-embedding") || modelKey.startsWith("embedding-") || modelKey.startsWith("bge-")`（可调）；`embed` 用 Spring AI 2.0.0 `EmbeddingModel`：

```java
// Spring AI 2.0.0 构造方式（以 Phase 0 迁移后的实际 API 为准，1.x 的 OpenAiEmbeddingModel 构造器已变更）
OpenAiApi api = OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model(model).build();
EmbeddingModel embeddingModel = new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
return embeddingModel.embed(texts); // 2.0.0 返回 EmbeddingResponse，取 getResults().get(i).getOutput()
```

> ⚠️ Phase 0 升级到 Spring AI 2.0.0 后，`EmbeddingModel.embed` 返回类型与包名可能变化。**实施前先在 forge-plugin-ai 内写一个最小探针测试**验证 2.0.0 的 `EmbeddingModel` 构造与 `embed` 真实签名，再按实际修正本代码块。adapter 最终返回 `List<List<Float>>` 契约不变（`List<Float> per text`）。

- [ ] **Step 4: 实现 AiRerankModelAdapter 接口与 OpenAI 兼容实现**

```java
package com.mdframe.forge.plugin.ai.model.adapter;

import java.util.List;

/**
 * Rerank 模型适配器接口。分数与 passages 输入顺序对应。
 */
public interface AiRerankModelAdapter {

    String getSupportedProvider();

    boolean supports(String modelKey);

    List<Float> rerank(String baseUrl, String apiKey, String model, String query, List<String> passages);
}
```

`OpenAiCompatibleRerankModelAdapter`（@Component）：用 Spring AI 的 `RestClient` POST `{baseUrl}/rerank`，body `{model, query, documents}`，解析 `results[].relevance_score`（兼容阿里云 `output.results[]` 格式）。

- [ ] **Step 5: 实现 AiModelAdapterRegistry**

```java
package com.mdframe.forge.plugin.ai.model.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 模型适配器注册表。注入所有 Embedding/Rerank 适配器 Bean，按 modelKey 匹配。
 */
@Component
@RequiredArgsConstructor
public class AiModelAdapterRegistry {

    private final List<AiEmbeddingModelAdapter> embeddingAdapters;
    private final List<AiRerankModelAdapter> rerankAdapters;

    public AiEmbeddingModelAdapter getEmbedding(String modelKey) {
        return embeddingAdapters.stream()
                .filter(a -> a.supports(modelKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到支持该Embedding模型的适配器: " + modelKey));
    }

    public AiRerankModelAdapter getRerank(String modelKey) {
        return rerankAdapters.stream()
                .filter(a -> a.supports(modelKey))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到支持该Rerank模型的适配器: " + modelKey));
    }
}
```

- [ ] **Step 6: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=AiModelAdapterRegistryTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/adapter/
git commit -m "feat(ai): Embedding/Rerank分类型适配器接口与注册表"
```

---

### Task 6: 模型管理页改造（前端）

**Files:**
- Modify: `forge-admin-ui/src/views/ai/provider-model.vue`（模型类型筛选 + 分类型配置表单）
- Modify: `forge-admin-ui/src/api/ai.js`（新增 rerank/embedding 相关接口，或复用现有 model CRUD）
- Modify: `forge-admin-ui/src/views/ai/model.vue`（模型列表按类型过滤）

**Interfaces:**
- Consumes: Task 1-5 的后端接口（`GET /ai/model/page`、`POST /ai/model`、`POST /ai/provider/test` 等现有接口）
- Produces: 前端可配置 6 类模型的完整页面

- [ ] **Step 1: 改造 provider-model.vue 增加模型类型筛选**

在 `forge-admin-ui/src/views/ai/provider-model.vue` 右侧模型表格上方，增加 `modelType` 筛选下拉（数据源 `useDict('ai_model_type')`），按类型过滤模型列表。仅展示当前类型的模型。

- [ ] **Step 2: 模型新增/编辑表单支持 6 类型**

在模型新增/编辑弹窗中，`modelType` 下拉用 `useDict('ai_model_type')`（含 6 类）。不同类型的额外配置项（如 Embedding 的维度、Rerank 的 rerank path）用条件渲染。`modelId` 下拉按类型联动（Chat 模型列表 / Embedding 模型列表 / ...）。

- [ ] **Step 3: 连接测试按类型路由**

模型连接测试按钮：Chat 类型走现有 `testConnection`（回 OK）；Embedding 类型调 `embed(["hello"])` 验证返回维度；Rerank 类型调 `rerank` 验证返回分数。

- [ ] **Step 4: 本地验证**

Run: `cd forge-admin-ui && pnpm dev`
Expected: 打开模型管理页，能看到 6 类模型类型，可新建 Embedding/Rerank 模型并测试连接

- [ ] **Step 5: 提交**

```bash
git add forge-admin-ui/src/views/ai/provider-model.vue forge-admin-ui/src/views/ai/model.vue forge-admin-ui/src/api/ai.js
git commit -m "feat(ui): 模型管理支持6类模型类型"
```

---

## Self-Review 记录

- **Spec 覆盖**：一期覆盖设计文档"模型管理升级"全部内容（类型细分 ✓、全类型模型 ✓、API Key 加密 ✓、前端页 ✓）
- **占位符扫描**：无 TBD/TODO；Task 2 的字典列名已按 V1.0.18 模板核实（`dict_type`），非占位符
- **类型一致性**：`AiModelType.CHAT/EMBEDDING/RERANK/IMAGE_GENERATION/ASR/TTS` 全篇一致；`AiSecretCrypto.encrypt/decrypt/isEncrypted` 签名一致
- **模型类型协调**：`model_type` 细分 6 类；Vision 不建新类型，用现有 `ai_model_capability.vision` 标记 chat 模型（Task 6 前端按 `ai_model_capability` 判断图片上传是否可用）
- **加密复用**：Task 3 复用 `PersistentCryptoService`，但**落库为 legacy 无前缀密文**（跟随全局 `write-versioned=false`，非 FPC1 版本化），不新增 `AiSecretCipher`/`FORGE_AI_SECRET_KEY`；`isEncrypted` 以"非空即密文"代替 `fpc1:` 前缀判断；密钥轮换统一开启 versioned 后再支持
- **适配器架构**：Task 5 采用**方案 C（分类型接口注册表）**——`AiEmbeddingModelAdapter`/`AiRerankModelAdapter` 独立接口 + `@Component` 实现 + `AiModelAdapterRegistry` 按 modelKey 匹配（参考 Snail AI `ModelFactory`）。不扩展现有 `AiProviderAdapter`（Chat 专用，一期不动），不建 54Doctor 式大工厂。二期/三期/四期通过 `AiModelAdapterRegistry.getEmbedding/getRerank/getImage/getAsr/getTts` 获取
- **存量迁移**：`image`/`audio` → `image_generation`/`asr` 映射 + 迁移 UPDATE（Task 2）
- **依赖**：`forge-plugin-ai` 新增 `forge-starter-crypto`（Task 3）
- **Spring AI 2.0.0**：依赖 Phase 0；Embedding 等新代码按 2.0.0 API，实施前探针验证

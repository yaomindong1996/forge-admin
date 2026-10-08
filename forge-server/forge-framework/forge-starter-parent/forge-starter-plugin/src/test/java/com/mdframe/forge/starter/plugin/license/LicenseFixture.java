package com.mdframe.forge.starter.plugin.license;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.List;

final class LicenseFixture {
    static final String PROJECT = "cf12f2ca-9548-472d-b8ae-7e447d26c0c9";
    static final String ID = "c79d565f-4a44-4b65-bc4c-a71eea79de29";

    private LicenseFixture() {
    }

    static KeyPair keyPair() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    static LicensePayload payload() {
        return new LicensePayload(1, ID, new LicensePayload.Binding("42", PROJECT),
                new LicensePayload.Scope(List.of("test-plugin"), List.of("ee.test.view")),
                new LicensePayload.Terms(100, 100, null, 200L));
    }

    static byte[] sign(byte[] payload, KeyPair pair) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        signer.update(payload);
        return LicenseCodec.document(new LicenseEnvelope("Ed25519", "key-1",
                LicenseCodec.encode(payload), LicenseCodec.encode(signer.sign())));
    }

    static byte[] document(KeyPair pair) throws Exception {
        return sign(LicenseCodec.payload(payload()), pair);
    }
}

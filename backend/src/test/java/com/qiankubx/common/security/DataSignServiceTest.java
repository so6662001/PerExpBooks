package com.qiankubx.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class DataSignServiceTest {

    private DataSignService dataSignService;

    @BeforeEach
    void setUp() {
        String signKey = "test-sign-key-for-unit-testing-32";
        SecretKeySpec keySpec = new SecretKeySpec(
                signKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        KeyManager keyManager = new KeyManager(signKey);
        dataSignService = new DataSignService(keyManager);
    }

    @Test
    void sign_shouldReturnConsistentHash() {
        String payload = "1|100|99.00|1|2026-04-15T10:00:00";

        String sign1 = dataSignService.sign(payload);
        String sign2 = dataSignService.sign(payload);

        assertThat(sign1).isNotBlank();
        assertThat(sign1).isEqualTo(sign2);
        assertThat(sign1).matches("[0-9a-f]{64}");
    }

    @Test
    void verify_validSignature_shouldReturnTrue() {
        String payload = "1|100|99.00|1|2026-04-15T10:00:00";
        String signature = dataSignService.sign(payload);

        boolean result = dataSignService.verify(payload, signature);

        assertThat(result).isTrue();
    }

    @Test
    void verify_tamperedData_shouldReturnFalse() {
        String originalPayload = "1|100|99.00|1|2026-04-15T10:00:00";
        String signature = dataSignService.sign(originalPayload);

        String tamperedPayload = "1|100|199.00|1|2026-04-15T10:00:00";
        boolean result = dataSignService.verify(tamperedPayload, signature);

        assertThat(result).isFalse();
    }

    @Test
    void sign_differentPayloads_shouldReturnDifferentHashes() {
        String payload1 = "1|100|99.00";
        String payload2 = "1|100|199.00";

        String sign1 = dataSignService.sign(payload1);
        String sign2 = dataSignService.sign(payload2);

        assertThat(sign1).isNotEqualTo(sign2);
    }

    @Test
    void verify_caseInsensitive_shouldReturnTrue() {
        String payload = "test-payload";
        String signature = dataSignService.sign(payload);

        boolean result = dataSignService.verify(payload, signature.toUpperCase());

        assertThat(result).isTrue();
    }
}

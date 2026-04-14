package com.qiankubx.common.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
public class KeyManager {

    @Getter
    private final SecretKey signingKey;

    public KeyManager(@Value("${qianku.security.sign-key}") String signKey) {
        byte[] keyBytes = signKey.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}

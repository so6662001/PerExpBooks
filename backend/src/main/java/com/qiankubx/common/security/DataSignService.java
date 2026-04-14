package com.qiankubx.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSignService {

    private final KeyManager keyManager;

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyManager.getSigningKey());
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("数据签名失败", e);
            throw new RuntimeException("数据签名失败", e);
        }
    }

    public boolean verify(String payload, String signature) {
        String computed = sign(payload);
        return computed.equalsIgnoreCase(signature);
    }
}

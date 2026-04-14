package com.qiankubx.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChainHashService {

    private final DataSignService dataSignService;

    public String computeChainHash(String previousHash, String currentPayload) {
        String combined = (previousHash == null ? "GENESIS" : previousHash) + "|" + currentPayload;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public boolean verifyChain(String previousHash, String currentPayload, String expectedHash) {
        String computed = computeChainHash(previousHash, currentPayload);
        return computed.equalsIgnoreCase(expectedHash);
    }
}

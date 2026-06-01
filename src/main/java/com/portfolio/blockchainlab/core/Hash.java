package com.portfolio.blockchainlab.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hash {
    private Hash() {
    }

    public static String sha256(String value) {
        try {
            // SHA-256은 입력이 조금만 달라져도 완전히 다른 digest를 만든다.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String blockHeader(BlockHeader header) {
        // 블록 hash는 body가 아니라 header 필드들의 결정적 직렬화 결과로 계산한다.
        return sha256(
                header.index() + "|" +
                        header.previousHash() + "|" +
                        header.timestamp().toEpochMilli() + "|" +
                        header.dataHash() + "|" +
                        header.difficulty() + "|" +
                        header.nonce()
        );
    }
}

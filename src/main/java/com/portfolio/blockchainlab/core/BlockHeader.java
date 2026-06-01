package com.portfolio.blockchainlab.core;

import java.time.Instant;
import java.util.Objects;

public record BlockHeader(
        long index,
        String previousHash,
        Instant timestamp,
        String dataHash,
        int difficulty,
        long nonce
) {
    public BlockHeader {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        if (difficulty < 0 || difficulty > 8) {
            throw new IllegalArgumentException("difficulty must be between 0 and 8");
        }
        Objects.requireNonNull(previousHash, "previousHash must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(dataHash, "dataHash must not be null");
    }
}

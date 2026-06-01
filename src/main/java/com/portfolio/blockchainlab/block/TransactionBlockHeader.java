package com.portfolio.blockchainlab.block;

import java.time.Instant;
import java.util.Objects;

public record TransactionBlockHeader(
        long index,
        String previousHash,
        Instant timestamp,
        String merkleRoot,
        int difficulty,
        long nonce
) {
    public TransactionBlockHeader {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        if (difficulty < 0 || difficulty > 8) {
            throw new IllegalArgumentException("difficulty must be between 0 and 8");
        }
        Objects.requireNonNull(previousHash, "previousHash must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(merkleRoot, "merkleRoot must not be null");
    }
}

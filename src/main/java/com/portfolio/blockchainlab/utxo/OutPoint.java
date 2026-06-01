package com.portfolio.blockchainlab.utxo;

import java.util.Objects;

public record OutPoint(
        String transactionId,
        int outputIndex
) {
    public OutPoint {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be blank");
        }
        if (outputIndex < 0) {
            throw new IllegalArgumentException("outputIndex must not be negative");
        }
    }
}

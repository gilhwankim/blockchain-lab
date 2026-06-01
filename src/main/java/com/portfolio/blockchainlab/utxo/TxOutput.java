package com.portfolio.blockchainlab.utxo;

import java.util.Objects;

public record TxOutput(
        String ownerAddress,
        long amount
) {
    public TxOutput {
        Objects.requireNonNull(ownerAddress, "ownerAddress must not be null");
        if (ownerAddress.isBlank()) {
            throw new IllegalArgumentException("ownerAddress must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}

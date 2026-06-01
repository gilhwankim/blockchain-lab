package com.portfolio.blockchainlab.utxo;

import java.util.Objects;

public record TxInput(
        OutPoint previousOutput,
        String publicKey,
        String signature
) {
    public TxInput {
        Objects.requireNonNull(previousOutput, "previousOutput must not be null");
        Objects.requireNonNull(publicKey, "publicKey must not be null");
        Objects.requireNonNull(signature, "signature must not be null");
    }

    public static TxInput unsigned(OutPoint previousOutput) {
        return new TxInput(previousOutput, "", "");
    }
}

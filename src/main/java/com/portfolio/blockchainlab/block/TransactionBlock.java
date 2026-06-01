package com.portfolio.blockchainlab.block;

import java.util.Objects;

public record TransactionBlock(
        TransactionBlockHeader header,
        String hash,
        TransactionBlockBody body
) {
    public TransactionBlock {
        Objects.requireNonNull(header, "header must not be null");
        Objects.requireNonNull(hash, "hash must not be null");
        Objects.requireNonNull(body, "body must not be null");
    }
}

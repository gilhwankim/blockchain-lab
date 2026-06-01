package com.portfolio.blockchainlab.core;

import java.util.Objects;

public record Block(
        BlockHeader header,
        String hash,
        String data
) {
    public Block {
        Objects.requireNonNull(header, "header must not be null");
        Objects.requireNonNull(hash, "hash must not be null");
        Objects.requireNonNull(data, "data must not be null");
    }

    public Block withData(String changedData) {
        return new Block(header, hash, changedData);
    }

    public Block withPreviousHash(String changedPreviousHash) {
        BlockHeader changedHeader = new BlockHeader(
                header.index(),
                changedPreviousHash,
                header.timestamp(),
                header.dataHash(),
                header.difficulty(),
                header.nonce()
        );
        return new Block(changedHeader, hash, data);
    }
}

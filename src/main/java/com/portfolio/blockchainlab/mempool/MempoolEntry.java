package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.utxo.Transaction;

import java.time.Instant;

public record MempoolEntry(
        Transaction transaction,
        long fee,
        Instant submittedAt
) {
}

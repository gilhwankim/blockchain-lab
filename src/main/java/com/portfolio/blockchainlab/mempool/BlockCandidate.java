package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.utxo.Transaction;

import java.util.List;

public record BlockCandidate(
        List<Transaction> transactions,
        long totalFees
) {
    public BlockCandidate {
        transactions = List.copyOf(transactions);
    }
}

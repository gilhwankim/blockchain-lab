package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.utxo.Transaction;

import java.util.List;

public record ReorgMempoolResult(
        List<Transaction> removedConfirmedTransactions,
        List<Transaction> restoredTransactions,
        List<Transaction> rejectedTransactions
) {
    public ReorgMempoolResult {
        removedConfirmedTransactions = List.copyOf(removedConfirmedTransactions);
        restoredTransactions = List.copyOf(restoredTransactions);
        rejectedTransactions = List.copyOf(rejectedTransactions);
    }

    public static ReorgMempoolResult empty() {
        return new ReorgMempoolResult(List.of(), List.of(), List.of());
    }
}

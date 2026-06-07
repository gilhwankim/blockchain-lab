package com.portfolio.blockchainlab.confirmation;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.mempool.Mempool;
import com.portfolio.blockchainlab.utxo.Transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionConfirmationTracker {
    private final int finalityThreshold;

    public TransactionConfirmationTracker(int finalityThreshold) {
        if (finalityThreshold <= 0) {
            throw new IllegalArgumentException("finalityThreshold must be positive");
        }
        this.finalityThreshold = finalityThreshold;
    }

    public TransactionConfirmation statusOf(
            String transactionId,
            List<TransactionBlock> canonicalChain,
            Mempool mempool
    ) {
        if (canonicalChain.isEmpty()) {
            return pendingOrUnknown(transactionId, mempool);
        }

        long tipHeight = canonicalChain.get(canonicalChain.size() - 1).header().index();

        // canonical chain 안에서 transaction을 찾으면 tip까지의 거리로 confirmation 수를 계산한다.
        for (TransactionBlock block : canonicalChain) {
            boolean included = block.body().transactions().stream()
                    .anyMatch(transaction -> transaction.id().equals(transactionId));
            if (included) {
                int confirmations = Math.toIntExact(tipHeight - block.header().index() + 1);
                return TransactionConfirmation.included(
                        transactionId,
                        block.hash(),
                        block.header().index(),
                        confirmations,
                        finalityThreshold
                );
            }
        }

        return pendingOrUnknown(transactionId, mempool);
    }

    public List<Transaction> orphanedBy(ReorgPlan plan) {
        Set<String> addedTransactionIds = new HashSet<>();
        for (TransactionBlock block : plan.addedBlocks()) {
            for (Transaction transaction : block.body().transactions()) {
                addedTransactionIds.add(transaction.id());
            }
        }

        // removed block에서 빠졌고 새 branch에 다시 포함되지 않은 일반 transaction을 orphaned로 본다.
        return plan.removedBlocks().stream()
                .flatMap(block -> block.body().transactions().stream())
                .filter(transaction -> !transaction.coinbase())
                .filter(transaction -> !addedTransactionIds.contains(transaction.id()))
                .toList();
    }

    public int finalityThreshold() {
        return finalityThreshold;
    }

    private TransactionConfirmation pendingOrUnknown(String transactionId, Mempool mempool) {
        if (mempool.contains(transactionId)) {
            return TransactionConfirmation.pending(transactionId);
        }
        return TransactionConfirmation.unknown(transactionId);
    }
}

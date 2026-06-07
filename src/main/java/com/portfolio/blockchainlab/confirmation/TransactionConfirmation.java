package com.portfolio.blockchainlab.confirmation;

import java.util.Optional;

public record TransactionConfirmation(
        String transactionId,
        TransactionLifecycleStatus status,
        Optional<String> blockHash,
        Optional<Long> blockHeight,
        int confirmations,
        boolean finalized
) {
    public TransactionConfirmation {
        blockHash = blockHash == null ? Optional.empty() : blockHash;
        blockHeight = blockHeight == null ? Optional.empty() : blockHeight;
        if (confirmations < 0) {
            throw new IllegalArgumentException("confirmations must not be negative");
        }
    }

    public static TransactionConfirmation unknown(String transactionId) {
        return new TransactionConfirmation(
                transactionId,
                TransactionLifecycleStatus.UNKNOWN,
                Optional.empty(),
                Optional.empty(),
                0,
                false
        );
    }

    public static TransactionConfirmation pending(String transactionId) {
        return new TransactionConfirmation(
                transactionId,
                TransactionLifecycleStatus.PENDING,
                Optional.empty(),
                Optional.empty(),
                0,
                false
        );
    }

    public static TransactionConfirmation included(
            String transactionId,
            String blockHash,
            long blockHeight,
            int confirmations,
            int finalityThreshold
    ) {
        boolean finalized = confirmations >= finalityThreshold;
        return new TransactionConfirmation(
                transactionId,
                finalized ? TransactionLifecycleStatus.FINALIZED : TransactionLifecycleStatus.CONFIRMED,
                Optional.of(blockHash),
                Optional.of(blockHeight),
                confirmations,
                finalized
        );
    }
}

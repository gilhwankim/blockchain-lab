package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TransactionValidationException;
import com.portfolio.blockchainlab.utxo.TransactionValidator;
import com.portfolio.blockchainlab.utxo.UtxoSet;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Mempool {
    private final Clock clock;
    private final Map<String, MempoolEntry> entries = new LinkedHashMap<>();

    public Mempool() {
        this(Clock.systemUTC());
    }

    Mempool(Clock clock) {
        this.clock = clock;
    }

    public synchronized MempoolEntry submit(Transaction transaction, UtxoSet currentUtxoSet) {
        if (transaction.coinbase()) {
            throw new TransactionValidationException("coinbase transaction cannot enter mempool");
        }

        TransactionValidator.ValidationResult result = TransactionValidator.validate(transaction, currentUtxoSet);
        MempoolEntry entry = new MempoolEntry(transaction, result.fee(), Instant.now(clock));

        // 같은 transaction id가 다시 들어오면 기존 entry를 덮어쓰지 않고 그대로 반환한다.
        entries.putIfAbsent(transaction.id(), entry);
        return entries.get(transaction.id());
    }

    public synchronized ReorgMempoolResult reconcileAfterReorg(ReorgPlan plan, UtxoSet currentUtxoSet) {
        if (!plan.changed()) {
            return ReorgMempoolResult.empty();
        }

        List<Transaction> removedConfirmed = new ArrayList<>();
        List<Transaction> restored = new ArrayList<>();
        List<Transaction> rejected = new ArrayList<>();

        // 새 canonical chain에 포함된 transaction은 이미 확정된 것으로 보고 mempool에서 제거한다.
        for (TransactionBlock block : plan.addedBlocks()) {
            for (Transaction transaction : block.body().transactions()) {
                if (!transaction.coinbase() && entries.remove(transaction.id()) != null) {
                    removedConfirmed.add(transaction);
                }
            }
        }

        // canonical chain에서 빠진 block의 transaction은 현재 UTXO 기준으로 여전히 유효할 때만 다시 mempool에 넣는다.
        for (TransactionBlock block : plan.removedBlocks()) {
            for (Transaction transaction : block.body().transactions()) {
                if (transaction.coinbase()) {
                    continue;
                }

                try {
                    if (!entries.containsKey(transaction.id())) {
                        submit(transaction, currentUtxoSet);
                        restored.add(transaction);
                    }
                } catch (TransactionValidationException ignored) {
                    rejected.add(transaction);
                }
            }
        }

        return new ReorgMempoolResult(removedConfirmed, restored, rejected);
    }

    public synchronized boolean contains(String transactionId) {
        return entries.containsKey(transactionId);
    }

    public synchronized boolean remove(String transactionId) {
        return entries.remove(transactionId) != null;
    }

    public synchronized List<MempoolEntry> entriesByFeeDesc() {
        return entries.values().stream()
                .sorted(Comparator.comparingLong(MempoolEntry::fee).reversed()
                        .thenComparing(MempoolEntry::submittedAt))
                .toList();
    }

    public synchronized int size() {
        return entries.size();
    }
}

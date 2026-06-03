package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TransactionValidationException;
import com.portfolio.blockchainlab.utxo.TransactionValidator;
import com.portfolio.blockchainlab.utxo.UtxoSet;

import java.time.Clock;
import java.time.Instant;
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

package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TransactionValidationException;
import com.portfolio.blockchainlab.utxo.UtxoSet;

import java.util.ArrayList;
import java.util.List;

public class BlockCandidateBuilder {
    private final int maxTransactions;

    public BlockCandidateBuilder(int maxTransactions) {
        if (maxTransactions <= 0) {
            throw new IllegalArgumentException("maxTransactions must be positive");
        }
        this.maxTransactions = maxTransactions;
    }

    public BlockCandidate build(Mempool mempool, UtxoSet currentUtxoSet) {
        UtxoSet simulation = currentUtxoSet.copy();
        List<Transaction> selected = new ArrayList<>();
        long totalFees = 0;

        for (MempoolEntry entry : mempool.entriesByFeeDesc()) {
            if (selected.size() == maxTransactions) {
                break;
            }

            try {
                // 실제 UTXO set이 아니라 복사본에 적용해 block 안의 double spend를 미리 걸러낸다.
                simulation.apply(entry.transaction());
                selected.add(entry.transaction());
                totalFees += entry.fee();
            } catch (TransactionValidationException ignored) {
                // 같은 UTXO를 이미 다른 transaction이 소비했다면 이 transaction은 candidate에서 제외한다.
            }
        }

        return new BlockCandidate(selected, totalFees);
    }
}

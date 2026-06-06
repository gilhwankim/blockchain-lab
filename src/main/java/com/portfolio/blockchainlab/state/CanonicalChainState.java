package com.portfolio.blockchainlab.state;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.Utxo;
import com.portfolio.blockchainlab.utxo.UtxoSet;

import java.util.ArrayList;
import java.util.List;

public class CanonicalChainState {
    private List<TransactionBlock> canonicalChain;
    private UtxoSet utxoSet;

    public CanonicalChainState(List<TransactionBlock> canonicalChain) {
        this.canonicalChain = List.copyOf(canonicalChain);
        this.utxoSet = replay(this.canonicalChain);
    }

    public synchronized void apply(ReorgPlan plan) {
        if (!plan.changed()) {
            return;
        }

        // reorg가 발생하면 제거된 block을 하나씩 되돌리는 대신 canonical chain 전체를 다시 재생한다.
        // 학습 단계에서는 이 방식이 느리지만, 상태 전환 규칙을 가장 명확하게 보여준다.
        this.canonicalChain = plan.canonicalChain();
        this.utxoSet = replay(this.canonicalChain);
    }

    public synchronized long balanceOf(String ownerAddress) {
        return utxoSet.balanceOf(ownerAddress);
    }

    public synchronized List<Utxo> utxos() {
        return utxoSet.list();
    }

    public synchronized List<TransactionBlock> canonicalChain() {
        return canonicalChain;
    }

    public static UtxoSet replay(List<TransactionBlock> canonicalChain) {
        UtxoSet replayed = new UtxoSet();

        // genesis부터 tip까지 순서대로 transaction을 적용해야 이전 output 참조가 올바르게 해석된다.
        for (TransactionBlock block : canonicalChain) {
            for (Transaction transaction : block.body().transactions()) {
                replayed.apply(transaction);
            }
        }

        return replayed;
    }

    public static List<Transaction> transactionsOf(List<TransactionBlock> chain) {
        List<Transaction> transactions = new ArrayList<>();
        for (TransactionBlock block : chain) {
            transactions.addAll(block.body().transactions());
        }
        return List.copyOf(transactions);
    }
}

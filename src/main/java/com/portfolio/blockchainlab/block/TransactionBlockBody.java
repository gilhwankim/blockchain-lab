package com.portfolio.blockchainlab.block;

import com.portfolio.blockchainlab.merkle.MerkleTree;
import com.portfolio.blockchainlab.utxo.Transaction;

import java.util.List;

public record TransactionBlockBody(
        List<Transaction> transactions
) {
    public TransactionBlockBody {
        transactions = List.copyOf(transactions);
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("transactions must not be empty");
        }
    }

    public String merkleRoot() {
        // block body의 transaction id 목록을 Merkle root 하나로 요약한다.
        return MerkleTree.root(transactions.stream().map(Transaction::id).toList());
    }
}

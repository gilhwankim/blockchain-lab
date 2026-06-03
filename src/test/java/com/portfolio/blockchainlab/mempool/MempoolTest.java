package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.crypto.Wallet;
import com.portfolio.blockchainlab.utxo.OutPoint;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TransactionValidationException;
import com.portfolio.blockchainlab.utxo.TxOutput;
import com.portfolio.blockchainlab.utxo.UtxoSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MempoolTest {
    @Test
    void validTransactionEntersMempoolWithFee() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 100);
        utxoSet.apply(coinbase);
        Transaction payment = alice.createTransaction(
                List.of(new OutPoint(coinbase.id(), 0)),
                List.of(new TxOutput(bob.address(), 80))
        );
        Mempool mempool = new Mempool();

        MempoolEntry entry = mempool.submit(payment, utxoSet);

        assertThat(entry.fee()).isEqualTo(20);
        assertThat(mempool.size()).isEqualTo(1);
    }

    @Test
    void coinbaseTransactionCannotEnterMempool() {
        UtxoSet utxoSet = new UtxoSet();
        Mempool mempool = new Mempool();
        Transaction coinbase = Transaction.coinbase(1, "miner", 50);

        assertThatThrownBy(() -> mempool.submit(coinbase, utxoSet))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("coinbase transaction cannot enter mempool");
    }

    @Test
    void blockCandidateSelectsHigherFeeTransactionFirst() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction reward1 = Transaction.coinbase(1, alice.address(), 100);
        Transaction reward2 = Transaction.coinbase(2, alice.address(), 100);
        utxoSet.apply(reward1);
        utxoSet.apply(reward2);
        Transaction lowFee = alice.createTransaction(
                List.of(new OutPoint(reward1.id(), 0)),
                List.of(new TxOutput(bob.address(), 95))
        );
        Transaction highFee = alice.createTransaction(
                List.of(new OutPoint(reward2.id(), 0)),
                List.of(new TxOutput(bob.address(), 80))
        );
        Mempool mempool = new Mempool();
        mempool.submit(lowFee, utxoSet);
        mempool.submit(highFee, utxoSet);

        BlockCandidate candidate = new BlockCandidateBuilder(10).build(mempool, utxoSet);

        assertThat(candidate.transactions()).containsExactly(highFee, lowFee);
        assertThat(candidate.totalFees()).isEqualTo(25);
    }

    @Test
    void blockCandidateSkipsConflictingDoubleSpend() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Wallet carol = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction reward = Transaction.coinbase(1, alice.address(), 100);
        utxoSet.apply(reward);
        OutPoint rewardOutPoint = new OutPoint(reward.id(), 0);
        Transaction lowFee = alice.createTransaction(
                List.of(rewardOutPoint),
                List.of(new TxOutput(bob.address(), 95))
        );
        Transaction highFee = alice.createTransaction(
                List.of(rewardOutPoint),
                List.of(new TxOutput(carol.address(), 70))
        );
        Mempool mempool = new Mempool();
        mempool.submit(lowFee, utxoSet);
        mempool.submit(highFee, utxoSet);

        BlockCandidate candidate = new BlockCandidateBuilder(10).build(mempool, utxoSet);

        assertThat(candidate.transactions()).containsExactly(highFee);
        assertThat(candidate.totalFees()).isEqualTo(30);
        assertThat(utxoSet.balanceOf(alice.address())).isEqualTo(100);
    }

    @Test
    void blockCandidateHonorsMaxTransactionLimit() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction reward1 = Transaction.coinbase(1, alice.address(), 100);
        Transaction reward2 = Transaction.coinbase(2, alice.address(), 100);
        utxoSet.apply(reward1);
        utxoSet.apply(reward2);
        Transaction first = alice.createTransaction(
                List.of(new OutPoint(reward1.id(), 0)),
                List.of(new TxOutput(bob.address(), 80))
        );
        Transaction second = alice.createTransaction(
                List.of(new OutPoint(reward2.id(), 0)),
                List.of(new TxOutput(bob.address(), 90))
        );
        Mempool mempool = new Mempool();
        mempool.submit(first, utxoSet);
        mempool.submit(second, utxoSet);

        BlockCandidate candidate = new BlockCandidateBuilder(1).build(mempool, utxoSet);

        assertThat(candidate.transactions()).containsExactly(first);
        assertThat(candidate.totalFees()).isEqualTo(20);
    }
}

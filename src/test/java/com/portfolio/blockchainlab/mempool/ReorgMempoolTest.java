package com.portfolio.blockchainlab.mempool;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.block.TransactionBlockMiner;
import com.portfolio.blockchainlab.consensus.ForkChoice;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.crypto.Wallet;
import com.portfolio.blockchainlab.state.CanonicalChainState;
import com.portfolio.blockchainlab.utxo.OutPoint;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TxOutput;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReorgMempoolTest {
    private final TransactionBlockMiner miner = new TransactionBlockMiner(
            Clock.fixed(Instant.parse("2026-06-06T01:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void confirmedTransactionsAreRemovedFromMempool() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Transaction genesisReward = Transaction.coinbase(0, alice.address(), 100);
        TransactionBlock genesis = miner.mine(0, "0", List.of(genesisReward), 0);
        CanonicalChainState state = new CanonicalChainState(List.of(genesis));
        ForkChoice forkChoice = new ForkChoice(genesis);
        Mempool mempool = new Mempool();

        Transaction payment = alice.createTransaction(
                List.of(new OutPoint(genesisReward.id(), 0)),
                List.of(new TxOutput(bob.address(), 80))
        );
        mempool.submit(payment, state.utxoSet());
        TransactionBlock block = miner.mine(1, genesis.hash(), List.of(payment), 1);

        ReorgPlan plan = forkChoice.addBlock(block);
        state.apply(plan);
        ReorgMempoolResult result = mempool.reconcileAfterReorg(plan, state.utxoSet());

        assertThat(result.removedConfirmedTransactions()).containsExactly(payment);
        assertThat(mempool.contains(payment.id())).isFalse();
        assertThat(mempool.size()).isZero();
    }

    @Test
    void removedBlockTransactionsReturnOnlyWhenStillValidAfterReorg() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Wallet carol = Wallet.create();
        Wallet dave = Wallet.create();
        Wallet erin = Wallet.create();
        Wallet minerWallet = Wallet.create();

        Transaction aliceReward = Transaction.coinbase(0, alice.address(), 100);
        Transaction daveReward = Transaction.coinbase(0, dave.address(), 50);
        TransactionBlock genesis = miner.mine(0, "0", List.of(aliceReward, daveReward), 0);
        ForkChoice forkChoice = new ForkChoice(genesis);
        CanonicalChainState state = new CanonicalChainState(forkChoice.canonicalChain());
        Mempool mempool = new Mempool();

        Transaction payBob = alice.createTransaction(
                List.of(new OutPoint(aliceReward.id(), 0)),
                List.of(new TxOutput(bob.address(), 60), new TxOutput(alice.address(), 40))
        );
        Transaction payErin = dave.createTransaction(
                List.of(new OutPoint(daveReward.id(), 0)),
                List.of(new TxOutput(erin.address(), 45))
        );
        TransactionBlock a1 = miner.mine(1, genesis.hash(), List.of(payBob, payErin), 1);

        Transaction payCarol = alice.createTransaction(
                List.of(new OutPoint(aliceReward.id(), 0)),
                List.of(new TxOutput(carol.address(), 70), new TxOutput(alice.address(), 30))
        );
        TransactionBlock b1 = miner.mine(1, genesis.hash(), List.of(payCarol), 1);
        TransactionBlock b2 = miner.mine(2, b1.hash(), List.of(Transaction.coinbase(2, minerWallet.address(), 10)), 2);

        mempool.submit(payBob, state.utxoSet());
        mempool.submit(payErin, state.utxoSet());

        ReorgPlan aPlan = forkChoice.addBlock(a1);
        state.apply(aPlan);
        mempool.reconcileAfterReorg(aPlan, state.utxoSet());

        assertThat(mempool.size()).isZero();

        ReorgPlan ignoredFork = forkChoice.addBlock(b1);
        state.apply(ignoredFork);
        mempool.reconcileAfterReorg(ignoredFork, state.utxoSet());

        ReorgPlan reorg = forkChoice.addBlock(b2);
        state.apply(reorg);
        ReorgMempoolResult result = mempool.reconcileAfterReorg(reorg, state.utxoSet());

        assertThat(result.restoredTransactions()).containsExactly(payErin);
        assertThat(result.rejectedTransactions()).containsExactly(payBob);
        assertThat(mempool.contains(payErin.id())).isTrue();
        assertThat(mempool.contains(payBob.id())).isFalse();
        assertThat(mempool.entriesByFeeDesc()).extracting(MempoolEntry::transaction).containsExactly(payErin);
    }
}

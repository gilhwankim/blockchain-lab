package com.portfolio.blockchainlab.confirmation;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.block.TransactionBlockMiner;
import com.portfolio.blockchainlab.consensus.ForkChoice;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.crypto.Wallet;
import com.portfolio.blockchainlab.mempool.Mempool;
import com.portfolio.blockchainlab.mempool.ReorgMempoolResult;
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

class TransactionConfirmationTrackerTest {
    private final TransactionBlockMiner miner = new TransactionBlockMiner(
            Clock.fixed(Instant.parse("2026-06-07T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void transactionMovesFromPendingToConfirmedAndFinalized() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Transaction genesisReward = Transaction.coinbase(0, alice.address(), 100);
        TransactionBlock genesis = miner.mine(0, "0", List.of(genesisReward), 0);
        ForkChoice forkChoice = new ForkChoice(genesis);
        CanonicalChainState state = new CanonicalChainState(forkChoice.canonicalChain());
        Mempool mempool = new Mempool();
        TransactionConfirmationTracker tracker = new TransactionConfirmationTracker(3);

        Transaction payment = alice.createTransaction(
                List.of(new OutPoint(genesisReward.id(), 0)),
                List.of(new TxOutput(bob.address(), 90))
        );
        mempool.submit(payment, state.utxoSet());

        assertThat(tracker.statusOf(payment.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.PENDING);

        TransactionBlock block1 = miner.mine(1, genesis.hash(), List.of(payment), 1);
        ReorgPlan block1Plan = forkChoice.addBlock(block1);
        state.apply(block1Plan);
        mempool.reconcileAfterReorg(block1Plan, state.utxoSet());

        TransactionConfirmation confirmed = tracker.statusOf(payment.id(), forkChoice.canonicalChain(), mempool);

        assertThat(confirmed.status()).isEqualTo(TransactionLifecycleStatus.CONFIRMED);
        assertThat(confirmed.confirmations()).isEqualTo(1);
        assertThat(confirmed.finalized()).isFalse();

        TransactionBlock block2 = miner.mine(2, block1.hash(), List.of(Transaction.coinbase(2, "miner-2", 10)), 1);
        ReorgPlan block2Plan = forkChoice.addBlock(block2);
        state.apply(block2Plan);
        mempool.reconcileAfterReorg(block2Plan, state.utxoSet());

        TransactionBlock block3 = miner.mine(3, block2.hash(), List.of(Transaction.coinbase(3, "miner-3", 10)), 1);
        ReorgPlan block3Plan = forkChoice.addBlock(block3);
        state.apply(block3Plan);
        mempool.reconcileAfterReorg(block3Plan, state.utxoSet());

        TransactionConfirmation finalized = tracker.statusOf(payment.id(), forkChoice.canonicalChain(), mempool);

        assertThat(finalized.status()).isEqualTo(TransactionLifecycleStatus.FINALIZED);
        assertThat(finalized.confirmations()).isEqualTo(3);
        assertThat(finalized.finalized()).isTrue();
        assertThat(finalized.blockHeight()).contains(1L);
        assertThat(finalized.blockHash()).contains(block1.hash());
    }

    @Test
    void reorgCanMoveConfirmedTransactionsToPendingOrUnknown() {
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
        TransactionConfirmationTracker tracker = new TransactionConfirmationTracker(3);

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

        assertThat(tracker.statusOf(payBob.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.CONFIRMED);
        assertThat(tracker.statusOf(payErin.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.CONFIRMED);

        forkChoice.addBlock(b1);
        ReorgPlan reorg = forkChoice.addBlock(b2);
        state.apply(reorg);
        ReorgMempoolResult result = mempool.reconcileAfterReorg(reorg, state.utxoSet());

        assertThat(tracker.orphanedBy(reorg)).containsExactly(payBob, payErin);
        assertThat(result.restoredTransactions()).containsExactly(payErin);
        assertThat(result.rejectedTransactions()).containsExactly(payBob);
        assertThat(tracker.statusOf(payErin.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.PENDING);
        assertThat(tracker.statusOf(payBob.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.UNKNOWN);
        assertThat(tracker.statusOf(payCarol.id(), forkChoice.canonicalChain(), mempool).status())
                .isEqualTo(TransactionLifecycleStatus.CONFIRMED);
    }
}

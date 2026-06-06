package com.portfolio.blockchainlab.state;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.block.TransactionBlockMiner;
import com.portfolio.blockchainlab.consensus.ForkChoice;
import com.portfolio.blockchainlab.consensus.ReorgPlan;
import com.portfolio.blockchainlab.crypto.Wallet;
import com.portfolio.blockchainlab.utxo.OutPoint;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TxOutput;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalChainStateTest {
    private final TransactionBlockMiner miner = new TransactionBlockMiner(
            Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void reorgReplaysUtxoStateFromNewCanonicalChain() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Wallet carol = Wallet.create();
        Wallet minerWallet = Wallet.create();

        Transaction genesisReward = Transaction.coinbase(0, alice.address(), 100);
        TransactionBlock genesis = miner.mine(0, "0", List.of(genesisReward), 0);
        ForkChoice forkChoice = new ForkChoice(genesis);
        CanonicalChainState state = new CanonicalChainState(forkChoice.canonicalChain());
        OutPoint genesisOut = new OutPoint(genesisReward.id(), 0);

        Transaction payBob = alice.createTransaction(
                List.of(genesisOut),
                List.of(new TxOutput(bob.address(), 60), new TxOutput(alice.address(), 40))
        );
        TransactionBlock a1 = miner.mine(1, genesis.hash(), List.of(payBob), 1);

        Transaction payCarol = alice.createTransaction(
                List.of(genesisOut),
                List.of(new TxOutput(carol.address(), 70), new TxOutput(alice.address(), 30))
        );
        TransactionBlock b1 = miner.mine(1, genesis.hash(), List.of(payCarol), 1);
        TransactionBlock b2 = miner.mine(2, b1.hash(), List.of(Transaction.coinbase(2, minerWallet.address(), 10)), 2);

        state.apply(forkChoice.addBlock(a1));

        assertThat(state.balanceOf(bob.address())).isEqualTo(60);
        assertThat(state.balanceOf(carol.address())).isZero();
        assertThat(state.balanceOf(alice.address())).isEqualTo(40);

        ReorgPlan ignoredFork = forkChoice.addBlock(b1);
        state.apply(ignoredFork);

        assertThat(ignoredFork.changed()).isFalse();
        assertThat(state.balanceOf(bob.address())).isEqualTo(60);

        ReorgPlan reorg = forkChoice.addBlock(b2);
        state.apply(reorg);

        assertThat(reorg.changed()).isTrue();
        assertThat(reorg.removedBlocks()).containsExactly(a1);
        assertThat(reorg.addedBlocks()).containsExactly(b1, b2);
        assertThat(state.canonicalChain()).containsExactly(genesis, b1, b2);
        assertThat(state.balanceOf(bob.address())).isZero();
        assertThat(state.balanceOf(carol.address())).isEqualTo(70);
        assertThat(state.balanceOf(alice.address())).isEqualTo(30);
        assertThat(state.balanceOf(minerWallet.address())).isEqualTo(10);
    }
}

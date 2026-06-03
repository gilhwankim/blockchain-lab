package com.portfolio.blockchainlab.consensus;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.block.TransactionBlockMiner;
import com.portfolio.blockchainlab.utxo.Transaction;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ForkChoiceTest {
    private final TransactionBlockMiner miner = new TransactionBlockMiner(
            Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void startsWithGenesisAsCanonicalChain() {
        TransactionBlock genesis = block(0, "0", "genesis", 0);
        ForkChoice forkChoice = new ForkChoice(genesis);

        assertThat(forkChoice.canonicalTip().height()).isZero();
        assertThat(forkChoice.canonicalChain()).containsExactly(genesis);
    }

    @Test
    void storesBlockAsOrphanWhenParentIsMissing() {
        TransactionBlock genesis = block(0, "0", "genesis", 0);
        ForkChoice forkChoice = new ForkChoice(genesis);
        TransactionBlock orphan = block(2, "missing-parent", "orphan", 1);

        ReorgPlan plan = forkChoice.addBlock(orphan);

        assertThat(plan.changed()).isFalse();
        assertThat(forkChoice.orphansWaitingFor("missing-parent")).containsExactly(orphan);
        assertThat(forkChoice.canonicalChain()).containsExactly(genesis);
    }

    @Test
    void equalWorkForkKeepsCurrentCanonicalChain() {
        TransactionBlock genesis = block(0, "0", "genesis", 0);
        TransactionBlock left = block(1, genesis.hash(), "left", 1);
        TransactionBlock right = block(1, genesis.hash(), "right", 1);
        ForkChoice forkChoice = new ForkChoice(genesis);

        ReorgPlan firstPlan = forkChoice.addBlock(left);
        ReorgPlan secondPlan = forkChoice.addBlock(right);

        assertThat(firstPlan.changed()).isTrue();
        assertThat(secondPlan.changed()).isFalse();
        assertThat(forkChoice.canonicalChain()).containsExactly(genesis, left);
    }

    @Test
    void higherWorkBranchTriggersReorg() {
        TransactionBlock genesis = block(0, "0", "genesis", 0);
        TransactionBlock a1 = block(1, genesis.hash(), "a1", 1);
        TransactionBlock a2 = block(2, a1.hash(), "a2", 1);
        TransactionBlock b1 = block(1, genesis.hash(), "b1", 1);
        TransactionBlock b2 = block(2, b1.hash(), "b2", 2);
        ForkChoice forkChoice = new ForkChoice(genesis);

        forkChoice.addBlock(a1);
        forkChoice.addBlock(a2);
        ReorgPlan ignored = forkChoice.addBlock(b1);
        ReorgPlan reorg = forkChoice.addBlock(b2);

        assertThat(ignored.changed()).isFalse();
        assertThat(reorg.changed()).isTrue();
        assertThat(reorg.removedBlocks()).containsExactly(a2, a1);
        assertThat(reorg.addedBlocks()).containsExactly(b1, b2);
        assertThat(forkChoice.canonicalChain()).containsExactly(genesis, b1, b2);
    }

    @Test
    void longerButLowerWorkBranchDoesNotReplaceCanonicalChain() {
        TransactionBlock genesis = block(0, "0", "genesis", 0);
        TransactionBlock highWork = block(1, genesis.hash(), "high-work", 5);
        TransactionBlock low1 = block(1, genesis.hash(), "low-1", 1);
        TransactionBlock low2 = block(2, low1.hash(), "low-2", 1);
        TransactionBlock low3 = block(3, low2.hash(), "low-3", 1);
        ForkChoice forkChoice = new ForkChoice(genesis);

        forkChoice.addBlock(highWork);
        forkChoice.addBlock(low1);
        forkChoice.addBlock(low2);
        ReorgPlan plan = forkChoice.addBlock(low3);

        assertThat(plan.changed()).isFalse();
        assertThat(forkChoice.canonicalChain()).containsExactly(genesis, highWork);
    }

    private TransactionBlock block(long index, String previousHash, String label, int difficulty) {
        Transaction coinbase = Transaction.coinbase(index, "miner-" + label, 50);
        return miner.mine(index, previousHash, List.of(coinbase), difficulty);
    }
}

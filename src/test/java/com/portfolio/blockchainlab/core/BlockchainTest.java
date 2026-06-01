package com.portfolio.blockchainlab.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BlockchainTest {
    private final ProofOfWork proofOfWork = new ProofOfWork(
            Clock.fixed(Instant.parse("2026-05-29T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void newChainStartsWithGenesisBlock() {
        Blockchain blockchain = new Blockchain(proofOfWork);

        assertThat(blockchain.height()).isZero();
        assertThat(blockchain.blocks()).hasSize(1);
        assertThat(blockchain.isValid()).isTrue();
    }

    @Test
    void addingBlockIncreasesHeightAndKeepsChainValid() {
        Blockchain blockchain = new Blockchain(proofOfWork);

        Block block = blockchain.addBlock("first study note");

        assertThat(blockchain.height()).isEqualTo(1);
        assertThat(block.header().index()).isEqualTo(1);
        assertThat(blockchain.isValid()).isTrue();
    }

    @Test
    void minedBlockHashSatisfiesDifficulty() {
        Block block = proofOfWork.mine(1, "previous", "payload", 4);

        assertThat(block.hash()).startsWith("0000");
        assertThat(ProofOfWork.satisfiesDifficulty(block.hash(), block.header().difficulty())).isTrue();
    }

    @Test
    void changingBlockDataBreaksValidation() {
        Blockchain blockchain = new Blockchain(proofOfWork);
        blockchain.addBlock("original data");

        Block changed = blockchain.blocks().get(1).withData("changed data");
        blockchain.replaceBlockForStudyOnly(1, changed);

        assertThat(blockchain.isValid()).isFalse();
    }

    @Test
    void changingPreviousHashBreaksValidation() {
        Blockchain blockchain = new Blockchain(proofOfWork);
        blockchain.addBlock("original data");

        Block changed = blockchain.blocks().get(1).withPreviousHash("fake previous hash");
        blockchain.replaceBlockForStudyOnly(1, changed);

        assertThat(blockchain.isValid()).isFalse();
    }
}

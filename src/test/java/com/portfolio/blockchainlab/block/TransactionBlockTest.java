package com.portfolio.blockchainlab.block;

import com.portfolio.blockchainlab.utxo.Transaction;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionBlockTest {
    private final TransactionBlockMiner miner = new TransactionBlockMiner(
            Clock.fixed(Instant.parse("2026-05-30T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void transactionBlockHeaderContainsMerkleRootFromBody() {
        Transaction coinbase = Transaction.coinbase(1, "miner", 50);
        TransactionBlock block = miner.mine(1, "previous", List.of(coinbase), 3);

        assertThat(block.header().merkleRoot()).isEqualTo(block.body().merkleRoot());
        assertThat(block.hash()).startsWith("000");
        assertThat(TransactionBlockMiner.isValid(block)).isTrue();
    }

    @Test
    void changedBodyBreaksTransactionBlockValidation() {
        Transaction coinbase = Transaction.coinbase(1, "miner", 50);
        Transaction changedCoinbase = Transaction.coinbase(2, "miner", 50);
        TransactionBlock block = miner.mine(1, "previous", List.of(coinbase), 2);
        TransactionBlock changed = new TransactionBlock(
                block.header(),
                block.hash(),
                new TransactionBlockBody(List.of(changedCoinbase))
        );

        assertThat(TransactionBlockMiner.isValid(changed)).isFalse();
    }
}

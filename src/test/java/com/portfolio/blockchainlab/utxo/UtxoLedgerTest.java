package com.portfolio.blockchainlab.utxo;

import com.portfolio.blockchainlab.crypto.Wallet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtxoLedgerTest {
    @Test
    void coinbaseTransactionCreatesSpendableBalance() {
        UtxoLedger ledger = new UtxoLedger();

        Transaction coinbase = ledger.createCoinbase(1, "miner", 50);

        assertThat(coinbase.coinbase()).isTrue();
        assertThat(ledger.balanceOf("miner")).isEqualTo(50);
        assertThat(ledger.utxos()).hasSize(1);
    }

    @Test
    void regularTransactionConsumesOldUtxoAndCreatesNewUtxos() {
        UtxoLedger ledger = new UtxoLedger();
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Transaction coinbase = ledger.createCoinbase(1, alice.address(), 50);
        OutPoint aliceReward = new OutPoint(coinbase.id(), 0);

        Transaction payment = alice.createTransaction(
                List.of(aliceReward),
                List.of(new TxOutput(bob.address(), 30), new TxOutput(alice.address(), 20))
        );
        ledger.apply(payment);

        assertThat(ledger.balanceOf(alice.address())).isEqualTo(20);
        assertThat(ledger.balanceOf(bob.address())).isEqualTo(30);
        assertThat(ledger.utxos()).hasSize(2);
        assertThat(ledger.utxos()).extracting(Utxo::outPoint).doesNotContain(aliceReward);
    }

    @Test
    void spendingSameUtxoTwiceFails() {
        UtxoLedger ledger = new UtxoLedger();
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Wallet carol = Wallet.create();
        Transaction coinbase = ledger.createCoinbase(1, alice.address(), 50);
        OutPoint aliceReward = new OutPoint(coinbase.id(), 0);

        Transaction firstSpend = alice.createTransaction(List.of(aliceReward), List.of(new TxOutput(bob.address(), 30)));
        ledger.apply(firstSpend);

        assertThatThrownBy(() ->
                ledger.apply(alice.createTransaction(List.of(aliceReward), List.of(new TxOutput(carol.address(), 30))))
        )
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("missing UTXO");
    }

    @Test
    void transactionCannotCreateMoreOutputValueThanInputValue() {
        UtxoLedger ledger = new UtxoLedger();
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        Transaction coinbase = ledger.createCoinbase(1, alice.address(), 50);
        OutPoint aliceReward = new OutPoint(coinbase.id(), 0);

        assertThatThrownBy(() ->
                ledger.apply(alice.createTransaction(List.of(aliceReward), List.of(new TxOutput(bob.address(), 51))))
        )
                .isInstanceOf(TransactionValidationException.class)
                .hasMessage("output sum exceeds input sum");
    }

    @Test
    void transactionFeeIsInputSumMinusOutputSum() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 50);
        utxoSet.apply(coinbase);

        Transaction transaction = alice.createTransaction(
                List.of(new OutPoint(coinbase.id(), 0)),
                List.of(new TxOutput(bob.address(), 30), new TxOutput(alice.address(), 15))
        );

        TransactionValidator.ValidationResult result = TransactionValidator.validate(transaction, utxoSet);

        assertThat(result.inputSum()).isEqualTo(50);
        assertThat(result.outputSum()).isEqualTo(45);
        assertThat(result.fee()).isEqualTo(5);
    }
}

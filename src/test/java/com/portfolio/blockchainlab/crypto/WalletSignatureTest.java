package com.portfolio.blockchainlab.crypto;

import com.portfolio.blockchainlab.utxo.OutPoint;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TransactionValidationException;
import com.portfolio.blockchainlab.utxo.TxInput;
import com.portfolio.blockchainlab.utxo.TxOutput;
import com.portfolio.blockchainlab.utxo.UtxoSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletSignatureTest {
    @Test
    void walletAddressIsDerivedFromPublicKey() {
        Wallet wallet = Wallet.create();

        assertThat(wallet.address()).isEqualTo(CryptoKeys.addressFromPublicKey(wallet.publicKey()));
    }

    @Test
    void walletSignedTransactionPassesValidation() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 50);
        utxoSet.apply(coinbase);

        Transaction payment = alice.createTransaction(
                List.of(new OutPoint(coinbase.id(), 0)),
                List.of(new TxOutput(bob.address(), 30), new TxOutput(alice.address(), 20))
        );

        utxoSet.apply(payment);

        assertThat(utxoSet.balanceOf(alice.address())).isEqualTo(20);
        assertThat(utxoSet.balanceOf(bob.address())).isEqualTo(30);
    }

    @Test
    void differentWalletCannotSpendUtxoOwnedByAlice() {
        Wallet alice = Wallet.create();
        Wallet mallory = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 50);
        utxoSet.apply(coinbase);

        Transaction forged = mallory.createTransaction(
                List.of(new OutPoint(coinbase.id(), 0)),
                List.of(new TxOutput(mallory.address(), 50))
        );

        assertThatThrownBy(() -> utxoSet.apply(forged))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("input signer does not own UTXO");
    }

    @Test
    void changingOutputAfterSigningBreaksSignature() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 50);
        utxoSet.apply(coinbase);

        Transaction signed = alice.createTransaction(
                List.of(new OutPoint(coinbase.id(), 0)),
                List.of(new TxOutput(bob.address(), 30), new TxOutput(alice.address(), 20))
        );
        Transaction tampered = Transaction.regular(
                signed.inputs(),
                List.of(new TxOutput(bob.address(), 31), new TxOutput(alice.address(), 19))
        );

        assertThatThrownBy(() -> utxoSet.apply(tampered))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("invalid input signature");
    }

    @Test
    void missingSignatureFailsValidation() {
        Wallet alice = Wallet.create();
        Wallet bob = Wallet.create();
        UtxoSet utxoSet = new UtxoSet();
        Transaction coinbase = Transaction.coinbase(1, alice.address(), 50);
        utxoSet.apply(coinbase);
        OutPoint reward = new OutPoint(coinbase.id(), 0);

        Transaction unsigned = Transaction.regular(
                List.of(new TxInput(reward, alice.publicKey(), "")),
                List.of(new TxOutput(bob.address(), 50))
        );

        assertThatThrownBy(() -> utxoSet.apply(unsigned))
                .isInstanceOf(TransactionValidationException.class)
                .hasMessageContaining("invalid input signature");
    }
}

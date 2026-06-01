package com.portfolio.blockchainlab.crypto;

import com.portfolio.blockchainlab.utxo.OutPoint;
import com.portfolio.blockchainlab.utxo.Transaction;
import com.portfolio.blockchainlab.utxo.TxInput;
import com.portfolio.blockchainlab.utxo.TxOutput;

import java.security.KeyPair;
import java.util.List;

public class Wallet {
    private final KeyPair keyPair;
    private final String publicKey;
    private final String address;

    private Wallet(KeyPair keyPair) {
        this.keyPair = keyPair;
        this.publicKey = CryptoKeys.publicKey(keyPair.getPublic());
        this.address = CryptoKeys.addressFromPublicKey(publicKey);
    }

    public static Wallet create() {
        return new Wallet(CryptoKeys.generateKeyPair());
    }

    public Transaction createTransaction(List<OutPoint> inputs, List<TxOutput> outputs) {
        Transaction unsigned = Transaction.unsigned(inputs, outputs);

        // 각 input은 이 wallet이 해당 UTXO 소비를 승인했다는 증명을 담아야 한다.
        List<TxInput> signedInputs = inputs.stream()
                .map(outPoint -> new TxInput(outPoint, publicKey, CryptoKeys.sign(keyPair.getPrivate(), unsigned.signingPayload())))
                .toList();

        return Transaction.regular(signedInputs, outputs);
    }

    public String address() {
        return address;
    }

    public String publicKey() {
        return publicKey;
    }
}

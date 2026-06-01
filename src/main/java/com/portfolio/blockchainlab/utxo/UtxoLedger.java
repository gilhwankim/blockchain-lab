package com.portfolio.blockchainlab.utxo;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UtxoLedger {
    private final UtxoSet utxoSet = new UtxoSet();

    public synchronized Transaction createCoinbase(long blockHeight, String minerAddress, long reward) {
        Transaction coinbase = Transaction.coinbase(blockHeight, minerAddress, reward);

        // coinbase를 적용하면 새로 발행된 가치가 UTXO set에 추가된다.
        utxoSet.apply(coinbase);
        return coinbase;
    }

    public synchronized Transaction spend(List<TxInput> inputs, List<TxOutput> outputs) {
        Transaction transaction = Transaction.regular(inputs, outputs);

        // regular transaction을 적용하면 input 소비와 output 생성이 하나의 흐름으로 처리된다.
        utxoSet.apply(transaction);
        return transaction;
    }

    public synchronized void apply(Transaction transaction) {
        // 서명된 transaction은 ledger 밖에서 만들어진 뒤 검증 대상으로 제출될 수 있다.
        utxoSet.apply(transaction);
    }

    public synchronized long balanceOf(String ownerAddress) {
        return utxoSet.balanceOf(ownerAddress);
    }

    public synchronized List<Utxo> utxos() {
        return utxoSet.list();
    }
}

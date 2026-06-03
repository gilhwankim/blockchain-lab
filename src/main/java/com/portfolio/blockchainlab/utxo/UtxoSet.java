package com.portfolio.blockchainlab.utxo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UtxoSet {
    private final Map<OutPoint, TxOutput> outputs = new LinkedHashMap<>();

    public UtxoSet() {
    }

    private UtxoSet(Map<OutPoint, TxOutput> outputs) {
        this.outputs.putAll(outputs);
    }

    public synchronized void apply(Transaction transaction) {
        TransactionValidator.validate(transaction, this);

        if (!transaction.coinbase()) {
            // 소비된 output은 제거되어 이후에 다시 쓸 수 없게 된다.
            for (TxInput input : transaction.inputs()) {
                outputs.remove(input.previousOutput());
            }
        }

        // transaction이 만든 모든 output은 새롭게 소비 가능한 UTXO가 된다.
        for (int index = 0; index < transaction.outputs().size(); index++) {
            outputs.put(new OutPoint(transaction.id(), index), transaction.outputs().get(index));
        }
    }

    public synchronized Optional<TxOutput> get(OutPoint outPoint) {
        return Optional.ofNullable(outputs.get(outPoint));
    }

    public synchronized long balanceOf(String ownerAddress) {
        // balance는 account 상태로 저장하지 않고 unspent output들의 합으로 계산한다.
        return outputs.values().stream()
                .filter(output -> output.ownerAddress().equals(ownerAddress))
                .mapToLong(TxOutput::amount)
                .sum();
    }

    public synchronized List<Utxo> list() {
        return outputs.entrySet().stream()
                .map(entry -> new Utxo(entry.getKey(), entry.getValue()))
                .toList();
    }

    public synchronized UtxoSet copy() {
        // block candidate를 만들 때 실제 UTXO set을 건드리지 않고 검증 시뮬레이션하기 위한 복사본이다.
        return new UtxoSet(new LinkedHashMap<>(outputs));
    }
}

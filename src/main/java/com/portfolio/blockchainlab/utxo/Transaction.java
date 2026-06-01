package com.portfolio.blockchainlab.utxo;

import com.portfolio.blockchainlab.core.Hash;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record Transaction(
        String id,
        List<TxInput> inputs,
        List<TxOutput> outputs,
        boolean coinbase
) {
    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public static Transaction coinbase(long blockHeight, String minerAddress, long reward) {
        List<TxInput> inputs = List.of();
        List<TxOutput> outputs = List.of(new TxOutput(minerAddress, reward));

        // 같은 보상이라도 같은 id가 생기지 않도록 coinbase tx id에는 block height를 포함한다.
        String payload = "coinbase|" + blockHeight + "|" + serializeOutputs(outputs);
        return new Transaction(Hash.sha256(payload), inputs, outputs, true);
    }

    public static Transaction regular(List<TxInput> inputs, List<TxOutput> outputs) {
        String payload = signingPayload(inputs, outputs);
        return new Transaction(Hash.sha256(payload), inputs, outputs, false);
    }

    public static Transaction unsigned(List<OutPoint> inputs, List<TxOutput> outputs) {
        return regular(inputs.stream().map(TxInput::unsigned).toList(), outputs);
    }

    public long outputSum() {
        return outputs.stream().mapToLong(TxOutput::amount).sum();
    }

    public String signingPayload() {
        return signingPayload(inputs, outputs);
    }

    private static String signingPayload(List<TxInput> inputs, List<TxOutput> outputs) {
        return "regular|" + serializeInputs(inputs) + "|" + serializeOutputs(outputs);
    }

    private static String serializeInputs(List<TxInput> inputs) {
        return inputs.stream()
                .map(input -> input.previousOutput().transactionId() + ":" + input.previousOutput().outputIndex())
                .collect(Collectors.joining(","));
    }

    private static String serializeOutputs(List<TxOutput> outputs) {
        return outputs.stream()
                .map(output -> output.ownerAddress() + ":" + output.amount())
                .collect(Collectors.joining(","));
    }
}

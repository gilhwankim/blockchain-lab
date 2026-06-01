package com.portfolio.blockchainlab.utxo;

import com.portfolio.blockchainlab.crypto.CryptoKeys;

import java.util.HashSet;
import java.util.Set;

public final class TransactionValidator {
    private TransactionValidator() {
    }

    public static ValidationResult validate(Transaction transaction, UtxoSet utxoSet) {
        if (transaction.outputs().isEmpty()) {
            throw new TransactionValidationException("transaction must create at least one output");
        }
        if (transaction.outputs().stream().anyMatch(output -> output.amount() <= 0)) {
            throw new TransactionValidationException("output amount must be positive");
        }

        if (transaction.coinbase()) {
            return validateCoinbase(transaction);
        }
        return validateRegular(transaction, utxoSet);
    }

    private static ValidationResult validateCoinbase(Transaction transaction) {
        if (!transaction.inputs().isEmpty()) {
            throw new TransactionValidationException("coinbase transaction must not have inputs");
        }

        // Coinbase는 새 가치를 발행하므로 비교할 input 합계가 없다.
        return new ValidationResult(0, transaction.outputSum(), 0);
    }

    private static ValidationResult validateRegular(Transaction transaction, UtxoSet utxoSet) {
        if (transaction.inputs().isEmpty()) {
            throw new TransactionValidationException("regular transaction must have inputs");
        }

        long inputSum = 0;
        Set<OutPoint> seenInputs = new HashSet<>();

        for (TxInput input : transaction.inputs()) {
            OutPoint outPoint = input.previousOutput();

            // 하나의 transaction 안에서 같은 이전 output을 두 번 참조할 수 없다.
            if (!seenInputs.add(outPoint)) {
                throw new TransactionValidationException("duplicate input: " + outPoint);
            }

            // transaction은 현재 UTXO set에 남아 있는 output만 소비할 수 있다.
            TxOutput previousOutput = utxoSet.get(outPoint)
                    .orElseThrow(() -> new TransactionValidationException("missing UTXO: " + outPoint));

            // public key에서 파생한 address가 이전 output의 소유자 address와 같아야 한다.
            String signerAddress = CryptoKeys.addressFromPublicKey(input.publicKey());
            if (!signerAddress.equals(previousOutput.ownerAddress())) {
                throw new TransactionValidationException("input signer does not own UTXO: " + outPoint);
            }

            // signature는 바로 이 transaction payload를 승인한 값이어야 한다.
            if (!CryptoKeys.verify(input.publicKey(), transaction.signingPayload(), input.signature())) {
                throw new TransactionValidationException("invalid input signature: " + outPoint);
            }

            inputSum += previousOutput.amount();
        }

        long outputSum = transaction.outputSum();
        if (inputSum < outputSum) {
            throw new TransactionValidationException("output sum exceeds input sum");
        }

        // 새 output으로 배정되지 않은 금액은 transaction fee가 된다.
        return new ValidationResult(inputSum, outputSum, inputSum - outputSum);
    }

    public record ValidationResult(long inputSum, long outputSum, long fee) {
    }
}

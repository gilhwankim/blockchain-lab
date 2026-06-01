package com.portfolio.blockchainlab.utxo;

public record Utxo(
        OutPoint outPoint,
        TxOutput output
) {
}

package com.portfolio.blockchainlab.consensus;

import com.portfolio.blockchainlab.block.TransactionBlock;

public record BlockNode(
        TransactionBlock block,
        String parentHash,
        long height,
        long cumulativeWork
) {
}

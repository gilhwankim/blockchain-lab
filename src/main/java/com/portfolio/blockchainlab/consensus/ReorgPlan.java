package com.portfolio.blockchainlab.consensus;

import com.portfolio.blockchainlab.block.TransactionBlock;

import java.util.List;

public record ReorgPlan(
        List<TransactionBlock> removedBlocks,
        List<TransactionBlock> addedBlocks,
        List<TransactionBlock> canonicalChain
) {
    public ReorgPlan {
        removedBlocks = List.copyOf(removedBlocks);
        addedBlocks = List.copyOf(addedBlocks);
        canonicalChain = List.copyOf(canonicalChain);
    }

    public static ReorgPlan noChange(List<TransactionBlock> canonicalChain) {
        return new ReorgPlan(List.of(), List.of(), canonicalChain);
    }

    public boolean changed() {
        return !removedBlocks.isEmpty() || !addedBlocks.isEmpty();
    }
}

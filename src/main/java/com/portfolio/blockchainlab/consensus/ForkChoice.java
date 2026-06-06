package com.portfolio.blockchainlab.consensus;

import com.portfolio.blockchainlab.block.TransactionBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForkChoice {
    private final BlockStore blockStore;
    private BlockNode canonicalTip;

    public ForkChoice(TransactionBlock genesis) {
        this.blockStore = new BlockStore(genesis);
        this.canonicalTip = blockStore.find(genesis.hash()).orElseThrow();
    }

    public synchronized ReorgPlan addBlock(TransactionBlock block) {
        BlockStore.AddResult result = blockStore.add(block);
        if (result.status() != BlockStore.Status.CONNECTED) {
            return ReorgPlan.noChange(canonicalChain());
        }

        BlockNode candidateTip = result.node();
        if (candidateTip.cumulativeWork() <= canonicalTip.cumulativeWork()) {
            // 누적 work가 같거나 작으면 기존 canonical chain을 유지한다.
            return ReorgPlan.noChange(canonicalChain());
        }

        ReorgPlan plan = createReorgPlan(canonicalTip, candidateTip);
        canonicalTip = candidateTip;
        return plan;
    }

    public synchronized List<TransactionBlock> canonicalChain() {
        return blockStore.canonicalPathTo(canonicalTip);
    }

    public synchronized BlockNode canonicalTip() {
        return canonicalTip;
    }

    public synchronized List<TransactionBlock> orphansWaitingFor(String parentHash) {
        return blockStore.orphansWaitingFor(parentHash);
    }

    private ReorgPlan createReorgPlan(BlockNode oldTip, BlockNode newTip) {
        List<TransactionBlock> oldPath = blockStore.canonicalPathTo(oldTip);
        List<TransactionBlock> newPath = blockStore.canonicalPathTo(newTip);
        int commonPrefixLength = commonPrefixLength(oldPath, newPath);

        List<TransactionBlock> removed = new ArrayList<>(oldPath.subList(commonPrefixLength, oldPath.size()));
        List<TransactionBlock> added = new ArrayList<>(newPath.subList(commonPrefixLength, newPath.size()));

        // removed는 tip에서 parent 방향으로 rollback하는 순서가 더 자연스럽다.
        Collections.reverse(removed);
        return new ReorgPlan(removed, added, newPath);
    }

    private int commonPrefixLength(List<TransactionBlock> left, List<TransactionBlock> right) {
        int length = Math.min(left.size(), right.size());
        int index = 0;

        while (index < length && left.get(index).hash().equals(right.get(index).hash())) {
            index++;
        }
        return index;
    }
}

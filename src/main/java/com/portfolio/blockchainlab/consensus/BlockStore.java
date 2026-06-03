package com.portfolio.blockchainlab.consensus;

import com.portfolio.blockchainlab.block.TransactionBlock;
import com.portfolio.blockchainlab.block.TransactionBlockMiner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockStore {
    private final Map<String, BlockNode> nodes = new LinkedHashMap<>();
    private final Map<String, List<TransactionBlock>> orphansByParentHash = new HashMap<>();

    public BlockStore(TransactionBlock genesis) {
        if (!TransactionBlockMiner.isValid(genesis)) {
            throw new IllegalArgumentException("genesis block must be valid");
        }
        nodes.put(genesis.hash(), new BlockNode(genesis, genesis.header().previousHash(), 0, genesis.header().difficulty()));
    }

    public AddResult add(TransactionBlock block) {
        if (!TransactionBlockMiner.isValid(block)) {
            throw new IllegalArgumentException("block must be valid");
        }
        if (nodes.containsKey(block.hash())) {
            return AddResult.duplicate(block.hash());
        }

        Optional<BlockNode> parent = find(block.header().previousHash());
        if (parent.isEmpty()) {
            // parent를 아직 보지 못한 block은 orphan으로 보관한다.
            orphansByParentHash.computeIfAbsent(block.header().previousHash(), ignored -> new ArrayList<>()).add(block);
            return AddResult.orphan(block.hash(), block.header().previousHash());
        }

        BlockNode node = connect(block, parent.get());
        return AddResult.connected(node);
    }

    public Optional<BlockNode> find(String hash) {
        return Optional.ofNullable(nodes.get(hash));
    }

    public List<TransactionBlock> canonicalPathTo(BlockNode tip) {
        List<TransactionBlock> path = new ArrayList<>();
        BlockNode current = tip;

        while (current != null) {
            path.add(0, current.block());
            current = nodes.get(current.parentHash());
        }
        return path;
    }

    public List<TransactionBlock> orphansWaitingFor(String parentHash) {
        return List.copyOf(orphansByParentHash.getOrDefault(parentHash, List.of()));
    }

    private BlockNode connect(TransactionBlock block, BlockNode parent) {
        long cumulativeWork = parent.cumulativeWork() + block.header().difficulty();
        BlockNode node = new BlockNode(block, parent.block().hash(), parent.height() + 1, cumulativeWork);

        // parent와 연결된 block만 fork choice 후보가 될 수 있다.
        nodes.put(block.hash(), node);
        return node;
    }

    public record AddResult(
            Status status,
            String blockHash,
            String missingParentHash,
            BlockNode node
    ) {
        public static AddResult connected(BlockNode node) {
            return new AddResult(Status.CONNECTED, node.block().hash(), null, node);
        }

        public static AddResult orphan(String blockHash, String missingParentHash) {
            return new AddResult(Status.ORPHAN, blockHash, missingParentHash, null);
        }

        public static AddResult duplicate(String blockHash) {
            return new AddResult(Status.DUPLICATE, blockHash, null, null);
        }
    }

    public enum Status {
        CONNECTED,
        ORPHAN,
        DUPLICATE
    }
}

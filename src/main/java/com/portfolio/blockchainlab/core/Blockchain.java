package com.portfolio.blockchainlab.core;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class Blockchain {
    private static final int DEFAULT_DIFFICULTY = 4;

    private final ProofOfWork proofOfWork;
    private final List<Block> blocks = new ArrayList<>();

    public Blockchain(ProofOfWork proofOfWork) {
        this.proofOfWork = proofOfWork;
        this.blocks.add(genesisBlock());
    }

    public synchronized Block addBlock(String data) {
        Block previous = latestBlock();

        // 새 블록은 반드시 직전 블록의 hash를 참조해야 체인 연결성이 생긴다.
        Block mined = proofOfWork.mine(
                previous.header().index() + 1,
                previous.hash(),
                data,
                DEFAULT_DIFFICULTY
        );
        blocks.add(mined);
        return mined;
    }

    public synchronized boolean isValid() {
        // 전체 체인을 처음부터 다시 계산해 현재 저장된 블록들이 신뢰 가능한지 확인한다.
        return ChainValidator.isValid(blocks);
    }

    public synchronized long height() {
        return blocks.size() - 1L;
    }

    public synchronized Block latestBlock() {
        return blocks.get(blocks.size() - 1);
    }

    public synchronized List<Block> blocks() {
        return List.copyOf(blocks);
    }

    public synchronized void replaceBlockForStudyOnly(int index, Block block) {
        blocks.set(index, block);
    }

    private static Block genesisBlock() {
        String data = "genesis";
        String dataHash = Hash.sha256(data);

        // Genesis block은 이전 블록이 없으므로 previousHash를 고정값 "0"으로 둔다.
        BlockHeader header = new BlockHeader(
                0,
                "0",
                Instant.parse("2026-01-01T00:00:00Z"),
                dataHash,
                0,
                0
        );
        return new Block(header, Hash.blockHeader(header), data);
    }
}

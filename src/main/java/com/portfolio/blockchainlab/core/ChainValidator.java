package com.portfolio.blockchainlab.core;

import java.util.List;

public final class ChainValidator {
    private ChainValidator() {
    }

    public static boolean isValid(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }

        // Genesis block이 깨지면 이후 블록이 맞더라도 같은 체인이라고 볼 수 없다.
        if (!isGenesisValid(blocks.get(0))) {
            return false;
        }

        for (int i = 1; i < blocks.size(); i++) {
            Block previous = blocks.get(i - 1);
            Block current = blocks.get(i);

            // index는 블록 순서를 나타내므로 중간에 건너뛰거나 되돌아가면 invalid다.
            if (current.header().index() != previous.header().index() + 1) {
                return false;
            }

            // previousHash가 이전 블록 hash와 같아야 두 블록이 암호학적으로 연결된다.
            if (!current.header().previousHash().equals(previous.hash())) {
                return false;
            }

            // body data가 바뀌었는지 header의 dataHash와 다시 비교한다.
            if (!Hash.sha256(current.data()).equals(current.header().dataHash())) {
                return false;
            }

            // header 필드가 바뀌었는지 block hash를 다시 계산해 확인한다.
            if (!Hash.blockHeader(current.header()).equals(current.hash())) {
                return false;
            }

            // 채굴된 hash가 현재 difficulty 조건을 만족하는지 확인한다.
            if (!ProofOfWork.satisfiesDifficulty(current.hash(), current.header().difficulty())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGenesisValid(Block genesis) {
        // Genesis block은 체인의 출발점이므로 최소 규칙을 명시적으로 검증한다.
        return genesis.header().index() == 0
                && "0".equals(genesis.header().previousHash())
                && Hash.sha256(genesis.data()).equals(genesis.header().dataHash())
                && Hash.blockHeader(genesis.header()).equals(genesis.hash());
    }
}

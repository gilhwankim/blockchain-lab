package com.portfolio.blockchainlab.core;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ProofOfWork {
    private final Clock clock;

    public ProofOfWork() {
        this(Clock.systemUTC());
    }

    ProofOfWork(Clock clock) {
        this.clock = clock;
    }

    public Block mine(long index, String previousHash, String data, int difficulty) {
        String dataHash = Hash.sha256(data);
        Instant timestamp = Instant.now(clock);
        long nonce = 0;

        // nonce를 바꿔가며 difficulty 조건을 만족하는 header hash를 찾는다.
        while (true) {
            BlockHeader header = new BlockHeader(index, previousHash, timestamp, dataHash, difficulty, nonce);
            String hash = Hash.blockHeader(header);

            // 조건을 만족하는 순간 이 header는 해당 난이도만큼의 작업을 증명한다.
            if (satisfiesDifficulty(hash, difficulty)) {
                return new Block(header, hash, data);
            }
            nonce++;
        }
    }

    public static boolean satisfiesDifficulty(String hash, int difficulty) {
        // 현재 학습 단계에서는 leading zero 개수를 difficulty로 사용한다.
        return hash.startsWith("0".repeat(difficulty));
    }
}

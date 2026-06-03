package com.portfolio.blockchainlab.block;

import com.portfolio.blockchainlab.core.Hash;
import com.portfolio.blockchainlab.core.ProofOfWork;
import com.portfolio.blockchainlab.utxo.Transaction;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class TransactionBlockMiner {
    private final Clock clock;

    public TransactionBlockMiner() {
        this(Clock.systemUTC());
    }

    public TransactionBlockMiner(Clock clock) {
        this.clock = clock;
    }

    public TransactionBlock mine(long index, String previousHash, List<Transaction> transactions, int difficulty) {
        TransactionBlockBody body = new TransactionBlockBody(transactions);
        Instant timestamp = Instant.now(clock);
        long nonce = 0;

        // nonce를 증가시키며 header hash가 difficulty 조건을 만족할 때까지 반복한다.
        while (true) {
            TransactionBlockHeader header = new TransactionBlockHeader(
                    index,
                    previousHash,
                    timestamp,
                    body.merkleRoot(),
                    difficulty,
                    nonce
            );
            String hash = hashHeader(header);

            // header에는 transaction 전체가 아니라 Merkle root만 들어간다.
            if (ProofOfWork.satisfiesDifficulty(hash, difficulty)) {
                return new TransactionBlock(header, hash, body);
            }
            nonce++;
        }
    }

    public static boolean isValid(TransactionBlock block) {
        // body의 transaction 목록으로 Merkle root를 다시 계산해 header와 비교한다.
        if (!block.body().merkleRoot().equals(block.header().merkleRoot())) {
            return false;
        }

        // header field가 바뀌었는지 block hash를 다시 계산해 확인한다.
        if (!hashHeader(block.header()).equals(block.hash())) {
            return false;
        }

        // 채굴 결과가 현재 difficulty 조건을 만족하는지 확인한다.
        return ProofOfWork.satisfiesDifficulty(block.hash(), block.header().difficulty());
    }

    public static String hashHeader(TransactionBlockHeader header) {
        // transaction block hash 역시 body가 아니라 header의 결정적 직렬화 결과로 계산한다.
        return Hash.sha256(
                header.index() + "|" +
                        header.previousHash() + "|" +
                        header.timestamp().toEpochMilli() + "|" +
                        header.merkleRoot() + "|" +
                        header.difficulty() + "|" +
                        header.nonce()
        );
    }
}

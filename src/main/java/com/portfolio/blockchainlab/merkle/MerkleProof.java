package com.portfolio.blockchainlab.merkle;

import com.portfolio.blockchainlab.core.Hash;

import java.util.List;

public record MerkleProof(
        String leafHash,
        String rootHash,
        List<MerkleProofStep> steps
) {
    public MerkleProof {
        steps = List.copyOf(steps);
    }

    public boolean verify() {
        String current = leafHash;

        for (MerkleProofStep step : steps) {
            // sibling이 왼쪽인지 오른쪽인지에 따라 hash 결합 순서가 달라진다.
            current = switch (step.direction()) {
                case LEFT -> Hash.sha256(step.siblingHash() + current);
                case RIGHT -> Hash.sha256(current + step.siblingHash());
            };
        }

        // leaf에서 시작해 root까지 재계산한 값이 header의 Merkle root와 같아야 한다.
        return current.equals(rootHash);
    }
}

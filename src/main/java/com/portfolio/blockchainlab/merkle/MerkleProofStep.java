package com.portfolio.blockchainlab.merkle;

public record MerkleProofStep(
        String siblingHash,
        Direction direction
) {
    public enum Direction {
        LEFT,
        RIGHT
    }
}

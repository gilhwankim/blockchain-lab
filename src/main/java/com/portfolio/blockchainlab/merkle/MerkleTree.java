package com.portfolio.blockchainlab.merkle;

import com.portfolio.blockchainlab.core.Hash;

import java.util.ArrayList;
import java.util.List;

import static com.portfolio.blockchainlab.merkle.MerkleProofStep.Direction.LEFT;
import static com.portfolio.blockchainlab.merkle.MerkleProofStep.Direction.RIGHT;

public final class MerkleTree {
    private MerkleTree() {
    }

    public static String root(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            throw new IllegalArgumentException("leafHashes must not be empty");
        }

        List<String> level = List.copyOf(leafHashes);
        while (level.size() > 1) {
            level = nextLevel(level);
        }
        return level.get(0);
    }

    public static MerkleProof proof(List<String> leafHashes, int targetIndex) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            throw new IllegalArgumentException("leafHashes must not be empty");
        }
        if (targetIndex < 0 || targetIndex >= leafHashes.size()) {
            throw new IllegalArgumentException("targetIndex is out of range");
        }

        String leafHash = leafHashes.get(targetIndex);
        String rootHash = root(leafHashes);
        List<MerkleProofStep> steps = new ArrayList<>();
        List<String> level = List.copyOf(leafHashes);
        int index = targetIndex;

        while (level.size() > 1) {
            int siblingIndex = siblingIndex(level, index);
            String siblingHash = level.get(siblingIndex);
            MerkleProofStep.Direction direction = siblingIndex < index ? LEFT : RIGHT;
            steps.add(new MerkleProofStep(siblingHash, direction));

            // 부모 level로 올라가면 현재 leaf의 index는 절반으로 줄어든다.
            index = index / 2;
            level = nextLevel(level);
        }

        return new MerkleProof(leafHash, rootHash, steps);
    }

    private static List<String> nextLevel(List<String> currentLevel) {
        List<String> next = new ArrayList<>();

        for (int i = 0; i < currentLevel.size(); i += 2) {
            String left = currentLevel.get(i);

            // leaf 개수가 홀수면 마지막 hash를 복제해서 pair를 만든다.
            String right = i + 1 < currentLevel.size() ? currentLevel.get(i + 1) : left;
            next.add(Hash.sha256(left + right));
        }

        return next;
    }

    private static int siblingIndex(List<String> level, int index) {
        if (index % 2 == 0) {
            return index + 1 < level.size() ? index + 1 : index;
        }
        return index - 1;
    }
}

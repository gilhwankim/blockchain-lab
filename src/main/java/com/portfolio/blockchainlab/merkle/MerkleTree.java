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
        // Proof는 "targetIndex 위치의 leaf가 이 Merkle root에 포함된다"는 것을 증명하는 경로다.
        // 그래서 빈 tree나 존재하지 않는 targetIndex로는 proof를 만들 수 없다.
        if (leafHashes == null || leafHashes.isEmpty()) {
            throw new IllegalArgumentException("leafHashes must not be empty");
        }
        if (targetIndex < 0 || targetIndex >= leafHashes.size()) {
            throw new IllegalArgumentException("targetIndex is out of range");
        }

        // 검증은 target leaf hash에서 시작해서 root까지 다시 계산하는 방식으로 진행된다.
        String leafHash = leafHashes.get(targetIndex);

        // proof에 기대되는 최종 root도 함께 담아 둔다.
        // 나중에 MerkleProof.verify()가 재계산한 root와 이 값을 비교한다.
        String rootHash = root(leafHashes);

        // steps에는 target leaf가 root까지 올라가는 데 필요한 sibling hash와 방향을 순서대로 저장한다.
        // 예: target이 h3라면 [h4 RIGHT, h12 LEFT] 같은 형태가 된다.
        List<MerkleProofStep> steps = new ArrayList<>();

        // level은 현재 보고 있는 tree의 층이다.
        // 처음에는 leaf 층이고, 반복할 때마다 부모 층으로 바뀐다.
        List<String> level = List.copyOf(leafHashes);

        // index는 현재 level에서 target 노드가 몇 번째 위치인지 나타낸다.
        // 부모 level로 올라갈 때마다 index / 2가 된다.
        int index = targetIndex;

        while (level.size() > 1) {
            // 현재 target 노드와 pair를 이루는 sibling의 index를 찾는다.
            // 짝수 index면 오른쪽 sibling, 홀수 index면 왼쪽 sibling을 사용한다.
            // 단, 마지막 노드가 홀로 남은 경우에는 자기 자신을 sibling으로 사용한다.
            int siblingIndex = siblingIndex(level, index);

            // proof 검증 때 필요한 것은 전체 level이 아니라 sibling hash 하나뿐이다.
            String siblingHash = level.get(siblingIndex);

            // sibling이 target의 왼쪽인지 오른쪽인지 기록해야 한다.
            // Merkle parent는 hash(left + right)이므로 방향이 틀리면 root가 달라진다.
            MerkleProofStep.Direction direction = siblingIndex < index ? LEFT : RIGHT;
            steps.add(new MerkleProofStep(siblingHash, direction));

            // 부모 level로 올라가면 두 child가 하나의 parent가 되므로 index는 절반으로 줄어든다.
            // 예: leaf index 2와 3은 둘 다 parent level의 index 1이 된다.
            index = index / 2;

            // 현재 level의 모든 노드를 두 개씩 묶어 부모 level을 만든 뒤 다음 반복에서 사용한다.
            level = nextLevel(level);
        }

        // leafHash, rootHash, root까지 가는 sibling 경로를 하나의 proof 객체로 반환한다.
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

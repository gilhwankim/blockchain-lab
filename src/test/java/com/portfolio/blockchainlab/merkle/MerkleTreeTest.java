package com.portfolio.blockchainlab.merkle;

import com.portfolio.blockchainlab.core.Hash;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MerkleTreeTest {
    @Test
    void calculatesMerkleRootFromLeafHashes() {
        List<String> leaves = List.of(hash("tx1"), hash("tx2"), hash("tx3"), hash("tx4"));

        String root = MerkleTree.root(leaves);

        assertThat(root).hasSize(64);
    }

    @Test
    void changingLeafOrderChangesMerkleRoot() {
        List<String> original = List.of(hash("tx1"), hash("tx2"), hash("tx3"));
        List<String> reordered = List.of(hash("tx2"), hash("tx1"), hash("tx3"));

        assertThat(MerkleTree.root(original)).isNotEqualTo(MerkleTree.root(reordered));
    }

    @Test
    void changingLeafValueChangesMerkleRoot() {
        List<String> original = List.of(hash("tx1"), hash("tx2"), hash("tx3"));
        List<String> changed = List.of(hash("tx1"), hash("tx2-changed"), hash("tx3"));

        assertThat(MerkleTree.root(original)).isNotEqualTo(MerkleTree.root(changed));
    }

    @Test
    void createsAndVerifiesMerkleProof() {
        List<String> leaves = List.of(hash("tx1"), hash("tx2"), hash("tx3"), hash("tx4"));

        MerkleProof proof = MerkleTree.proof(leaves, 2);

        assertThat(proof.leafHash()).isEqualTo(hash("tx3"));
        assertThat(proof.verify()).isTrue();
    }

    @Test
    void merkleProofFailsWithWrongTargetHash() {
        List<String> leaves = List.of(hash("tx1"), hash("tx2"), hash("tx3"), hash("tx4"));
        MerkleProof proof = MerkleTree.proof(leaves, 2);
        MerkleProof wrongProof = new MerkleProof(hash("fake-tx"), proof.rootHash(), proof.steps());

        assertThat(wrongProof.verify()).isFalse();
    }

    private static String hash(String value) {
        return Hash.sha256(value);
    }
}

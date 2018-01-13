/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.util.ArrayList;
import java.util.List;

import org.semux.crypto.Hash;

/**
 * Simple implementation of the Merkle tree.
 */
public class MerkleTree {

    private Node root;
    private int size;
    private int levels = 0;

    /**
     * Construct a Merkle tree.
     * 
     * @param hashes
     */
    public MerkleTree(List<byte[]> hashes) {
        this.size = hashes.size();

        List<Node> nodes = new ArrayList<>();
        for (byte[] b : hashes) {
            nodes.add(new Node(b));
        }
        this.root = build(nodes);
    }

    /**
     * Get the root hash.
     * 
     * @return
     */
    public byte[] getRootHash() {
        return root.value;
    }

    /**
     * Get the size of elements.
     * 
     * @return
     */
    public int size() {
        return size;
    }

    /**
     * Returns the Merkle proof of the Nth element.
     * 
     * @param i
     *            the element index, starting from zero.
     * @return
     */
    public List<byte[]> getProof(int i) {
        List<byte[]> proof = new ArrayList<>();

        int half = 1 << (levels - 2);
        Node p = root;
        do {
            // shallow copy
            proof.add(p.value);

            if (i < half) {
                p = p.left;
            } else {
                p = p.right;
            }
            i -= half;
            half >>= 1;
        } while (p != null);

        return proof;
    }

    private Node build(List<Node> nodes) {
        if (nodes.isEmpty()) {
            return new Node(Bytes.EMPTY_HASH);
        }

        while (nodes.size() > 1) {
            List<Node> list = new ArrayList<>();

            for (int i = 0; i < nodes.size(); i += 2) {
                Node left = nodes.get(i);
                if (i + 1 < nodes.size()) {
                    Node right = nodes.get(i + 1);
                    list.add(new Node(Hash.h256(left.value, right.value), left, right));
                } else {
                    list.add(new Node(left.value, left, null));
                }
            }

            levels++;
            nodes = list;
        }

        levels++;
        return nodes.get(0);
    }

    private static class Node {
        byte[] value;
        Node left;
        Node right;

        public Node(byte[] value) {
            this.value = value;
        }

        public Node(byte[] value, Node left, Node right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }
    }
}

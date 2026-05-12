/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kademlia-style routing table over the full 160-bit node-ID space [0,
 * 2<sup>160</sup>).
 *
 * <h3>Rules</h3>
 * <ul>
 * <li>The ID space is partitioned into {@link RoutingBucket}s. Initially there
 * is a single bucket covering the entire space.</li>
 * <li>A node with ID {@code N} belongs to the unique bucket for which
 * {@code min <= N < max}.</li>
 * <li>Each bucket holds at most {@value RoutingBucket#CAPACITY} nodes.</li>
 * <li>When the target bucket is full and our own ID does <em>not</em> fall
 * inside its range the insertion is rejected.</li>
 * <li>When the target bucket is full and our own ID <em>does</em> fall inside
 * its range the bucket is split at its midpoint and insertion is retried — this
 * repeats until the node fits or the range cannot be subdivided further.</li>
 * </ul>
 */
public final class RoutingTable {
    /** 2^160 — the exclusive upper bound of the entire ID space. */
    static final BigInteger MAX_ID = BigInteger.ONE.shiftLeft(160);
    /** The own node ID used to decide whether a full bucket may be split. */
    private final BigInteger ownId;
    /**
     * Ordered list of buckets. The list is always sorted by {@code min} so that a
     * binary search can locate the correct bucket in O(log n).
     */
    private final List<RoutingBucket> buckets;
    private Map<ByteBuffer, Node> nodes;

    /**
     * Creates a new, empty routing table.
     *
     * @param ownId the 20-byte ID of the local node.
     */
    public RoutingTable(ByteBuffer ownId) {
        this.ownId = new BigInteger(1, ownId.array());
        this.buckets = new ArrayList<>();
        // Start with one bucket covering [0, 2^160).
        this.buckets.add(new RoutingBucket(BigInteger.ZERO, MAX_ID));
        this.nodes = new ConcurrentHashMap<>();
    }
    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Locates the index of the bucket whose range contains {@code nodeId} using
     * binary search on the sorted {@code buckets} list.
     */
    private int bucketIndexFor(BigInteger nodeId) {
        int lo = 0, hi = this.buckets.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            RoutingBucket b = this.buckets.get(mid);
            if (nodeId.compareTo(b.min) < 0) {
                hi = mid - 1;
            } else if (nodeId.compareTo(b.max) >= 0) {
                lo = mid + 1;
            } else
                return mid;
        }
        // Should never happen if the bucket list covers [0, MAX_ID) completely.
        throw new IllegalStateException("No bucket found for node ID " + nodeId.toString(16));
    }

    /**
     * Returns an unmodifiable view of all buckets, ordered by their lower bound.
     */
    public List<RoutingBucket> buckets() {
        return Collections.unmodifiableList(this.buckets);
    }

    /**
     * Returns up to {@value RoutingBucket#CAPACITY} nodes whose IDs are closest to
     * {@code targetId} according to the XOR metric:
     *
     * <pre>
     * distance(A, B) = |A XOR B|
     * </pre>
     *
     * <p>
     * The search starts at the bucket that would contain {@code targetId} and then
     * expands outward — first toward lower buckets, then toward higher ones — until
     * at least {@value RoutingBucket#CAPACITY} candidates have been collected or
     * all buckets have been visited. The candidates are then sorted by XOR distance
     * and the closest {@value RoutingBucket#CAPACITY} are returned.
     *
     * @param targetId a 20-byte (160-bit) node ID.
     * @return an unmodifiable list of at most {@value RoutingBucket#CAPACITY}
     *         nodes, sorted from closest to farthest.
     */
    public List<Node> closest(ByteBuffer targetId) {
        BigInteger target = new BigInteger(1, targetId.array());
        int startIdx = bucketIndexFor(target);
        // Collect candidates by expanding outward from the target bucket.
        // lo moves left, hi moves right; both start at the target bucket index.
        List<Node> candidates = new ArrayList<>(RoutingBucket.CAPACITY * 2);
        int lo = startIdx - 1;
        int hi = startIdx; // hi is the next right index to consume
        // First, always add the target bucket itself.
        candidates.addAll(this.buckets.get(startIdx).nodes());
        hi = startIdx + 1;
        // Expand outward until we have enough candidates or exhaust all buckets.
        while (candidates.size() < RoutingBucket.CAPACITY && (lo >= 0 || hi < this.buckets.size())) {
            if (lo >= 0) {
                candidates.addAll(this.buckets.get(lo).nodes());
                lo--;
            }
            if (hi < this.buckets.size()) {
                candidates.addAll(this.buckets.get(hi).nodes());
                hi++;
            }
        }
        // Sort all candidates by XOR distance to the target.
        candidates.sort(Comparator.comparing(node -> new BigInteger(1, node.id.array()).xor(target)));
        // Return at most CAPACITY results.
        int limit = Math.min(candidates.size(), RoutingBucket.CAPACITY);
        return Collections.unmodifiableList(candidates.subList(0, limit));
    }

    public Node getNode(ByteBuffer id) {
        return this.nodes.get(id);
    }

    /**
     * Attempts to insert {@code node} into the routing table.
     *
     * @return {@code true} if the node was inserted; {@code false} if it was
     *         rejected (target bucket full and cannot be split).
     * @throws IllegalArgumentException if the node ID is outside [0, 2^160).
     */
    private boolean insert(Node node) {
        BigInteger nodeId = new BigInteger(1, node.id.array());
        if (nodeId.signum() < 0 || nodeId.compareTo(MAX_ID) >= 0)
            throw new IllegalArgumentException("Node ID is outside the valid ID space.");
        while (true) {
            int idx = bucketIndexFor(nodeId);
            RoutingBucket bucket = this.buckets.get(idx);
            if (!bucket.isFull())
                return bucket.add(node);
            // Replace the full bucket with its two halves.
            RoutingBucket[] halves = bucket.split();
            this.buckets.set(idx, halves[0]);
            this.buckets.add(idx + 1, halves[1]);
            // Loop: re-evaluate which (now smaller) bucket the node belongs to.
        }
    }

    public void insertNode(Node node) {
        if (insert(node)) {
            this.nodes.put(node.id, node);
        }
    }

    public Collection<Node> nodes() {
        return Collections.unmodifiableCollection(this.nodes.values());
    }

    /**
     * Removes the node with the given {@code nodeId} from the routing table.
     *
     * @return {@code true} if a node was removed.
     */
    private boolean remove(ByteBuffer nodeId) {
        BigInteger id = new BigInteger(1, nodeId.array());
        int idx = bucketIndexFor(id);
        return this.buckets.get(idx).remove(nodeId);
    }

    public void removeAll(List<Node> expired) {
        for (Node node : expired) {
            removeNode(node.id);
        }
    }

    public boolean removeNode(ByteBuffer nodeId) {
        if (remove(nodeId)) {
            this.nodes.remove(nodeId);
            return true;
        }
        return false;
    }
    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Returns the total number of nodes held across all buckets. */
    public int size() {
        int total = 0;
        for (RoutingBucket b : this.buckets) {
            total += b.size();
        }
        return total;
    }
}

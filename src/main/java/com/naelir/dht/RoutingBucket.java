package com.naelir.dht;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single bucket in the {@link RoutingTable}.
 *
 * <p>
 * Covers the half-open node-ID range [{@code min}, {@code max}) in the absolute
 * 160-bit ID space. A bucket holds at most {@value #CAPACITY} nodes before it
 * is considered full.
 */
final class RoutingBucket {
    /** Maximum number of nodes a bucket may hold (k = 8 per the spec). */
    static final int CAPACITY = 8;
    /** Inclusive lower bound of the ID range covered by this bucket. */
    final BigInteger min;
    /** Exclusive upper bound of the ID range covered by this bucket. */
    final BigInteger max;
    private final List<Node> nodes;

    RoutingBucket(BigInteger min, BigInteger max) {
        this.min = min;
        this.max = max;
        this.nodes = new ArrayList<>(CAPACITY);
    }
    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Adds {@code node} to this bucket.
     *
     * @return {@code true} if the node was added; {@code false} if the bucket was
     *         already full or the node is already present.
     */
    boolean add(Node node) {
        if (isFull())
            return false;
        // Avoid duplicates — identity is the node ID.
        for (Node existing : this.nodes) {
            if (existing.id.equals(node.id))
                return false;
        }
        this.nodes.add(node);
        return true;
    }

    /**
     * Returns {@code true} when {@code nodeId} falls inside [{@code min},
     * {@code max}).
     */
    boolean contains(BigInteger nodeId) {
        return nodeId.compareTo(this.min) >= 0 && nodeId.compareTo(this.max) < 0;
    }

    boolean isFull() {
        return this.nodes.size() >= CAPACITY;
    }

    List<Node> nodes() {
        return Collections.unmodifiableList(this.nodes);
    }
    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    boolean remove(ByteBuffer nodeId) {
        return this.nodes.removeIf(n -> n.id.equals(nodeId));
    }

    int size() {
        return this.nodes.size();
    }
    // -------------------------------------------------------------------------
    // Split
    // -------------------------------------------------------------------------

    /**
     * Splits this bucket at its midpoint into two new buckets and redistributes the
     * existing nodes between them.
     *
     * <p>
     * The midpoint is {@code mid = (min + max) / 2}. The two resulting buckets
     * cover [{@code min}, {@code mid}) and [{@code mid}, {@code max}).
     *
     * @return a two-element array where index 0 is the lower half and index 1 is
     *         the upper half.
     */
    RoutingBucket[] split() {
        BigInteger mid = this.min.add(this.max).shiftRight(1); // (min + max) / 2
        RoutingBucket lower = new RoutingBucket(this.min, mid);
        RoutingBucket upper = new RoutingBucket(mid, this.max);
        for (Node node : this.nodes) {
            BigInteger nodeId = new BigInteger(1, node.id.array());
            if (nodeId.compareTo(mid) < 0) {
                lower.add(node);
            } else {
                upper.add(node);
            }
        }
        return new RoutingBucket[] { lower, upper };
    }

    @Override
    public String toString() {
        return "RoutingBucket[" + this.min.toString(16) + ".." + this.max.toString(16) + ", size=" + this.nodes.size()
                + "]";
    }
}

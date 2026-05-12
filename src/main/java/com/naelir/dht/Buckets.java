/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class Buckets {
    private List<Bucket> buckets;
    private ByteBuffer myself;
    private BigInteger myBits;

    public Buckets(ByteBuffer myId) {
        this.myself = myId;
        this.myBits = new BigInteger(1, myId.array());
        this.buckets = new ArrayList<>(4);
        this.buckets.add(new Bucket(0, Config.ID_SIZE - 1));
    }

    public boolean add(Node node) {
        Bucket bucket = getBucket(node.id);
        if (bucket == null)
            return false;
        if (bucket.getRangeBegin() == bucket.getRangeEnd() && bucket.isFull())
            return false;
        if (bucket.add(node)) {
            if (shouldSplit(bucket))
                return split(bucket.getRangeBegin(), node);
            return true;
        }
        return false;
    }

    public void clear() {
        for (Bucket bucket : this.buckets) {
            bucket.clear();
        }
        this.buckets.clear();
        this.buckets.add(new Bucket(0, Config.ID_SIZE - 1));
    }

    private Bucket getBucket(ByteBuffer id) {
        int bucketIdx = getBucketIndex(id);
        if (bucketIdx < 0)
            return null;
        return this.buckets.get(bucketIdx);
    }

    private int getBucketIndex(ByteBuffer id) {
        int range = getRange(id);
        if (range < 0)
            return -1;
        int bucketIdx = getBucketIndex(range);
        if (bucketIdx < 0)
            throw new IllegalStateException("ID does not fit in any bucket");
        return bucketIdx;
    }

    private int getBucketIndex(int range) {
        for (int i = this.buckets.size() - 1; i >= 0; i--) {
            Bucket b = this.buckets.get(i);
            if (range >= b.getRangeBegin() && range <= b.getRangeEnd())
                return i;
        }
        return -1;
    }

    public List<Bucket> getBuckets() {
        return Collections.unmodifiableList(this.buckets);
    }

    public List<ByteBuffer> getClosest(ByteBuffer id, int max) {
        if (id.equals(this.myself))
            return getClosest(max);
        List<ByteBuffer> closest = new ArrayList<>(max);
        int count = 0;
        int start = getBucketIndex(id);
        // start at closest bucket, then to the smaller (closer to us) buckets
        for (int i = start; i >= 0 && count < max; i--) {
            Set<Node> entries = this.buckets.get(i).getEntries();
            for (Node e : entries) {
                closest.add(e.id);
                count++;
            }
        }
        // then the farther from us buckets if necessary
        for (int i = start + 1; i < this.buckets.size() && count < max; i++) {
            Set<Node> entries = this.buckets.get(i).getEntries();
            for (Node e : entries) {
                closest.add(e.id);
                count++;
            }
        }
        XORComparator comp = new XORComparator(id);
        Collections.sort(closest, comp);
        int size = closest.size();
        for (int i = size - 1; i >= max; i--) {
            closest.remove(i);
        }
        return closest;
    }

    public List<ByteBuffer> getClosest(int max) {
        List<ByteBuffer> closest = new ArrayList<>(max);
        int count = 0;
        // start at first (closest) bucket
        for (int i = 0; i < this.buckets.size() && count < max; i++) {
            Set<Node> entries = this.buckets.get(i).getEntries();
            // add the whole bucket,
            // extras will be trimmed after sorting
            for (Node e : entries) {
                closest.add(e.id);
                count++;
            }
        }
        XORComparator comp = new XORComparator(this.myself);
        Collections.sort(closest, comp);
        int sz = closest.size();
        for (int i = sz - 1; i >= max; i--) {
            closest.remove(i);
        }
        return closest;
    }

    private int getRange(ByteBuffer id) {
        BigInteger xor = this.myBits.xor(new BigInteger(1, id.array()));
        return xor.bitLength() - 1;
    }

    public boolean remove(ByteBuffer id) {
        Bucket b = getBucket(id);
        return b != null && b.remove(id);
    }

    private boolean shouldSplit(Bucket b) {
        return b.getRangeBegin() != b.getRangeEnd() && b.size() > Config.BUCKET_SIZE;
    }

    public int size() {
        int count = 0;
        for (Bucket b : this.buckets) {
            count += b.size();
        }
        return count;
    }

    private boolean split(int range, Node added) {
        int bucketIdx = getBucketIndex(range);
        while (shouldSplit(this.buckets.get(bucketIdx))) {
            Bucket b = this.buckets.get(bucketIdx);
            Bucket b1 = new Bucket(b.getRangeBegin(), b.getRangeEnd() - 1);
            Bucket b2 = new Bucket(b.getRangeEnd(), b.getRangeEnd());
            for (Node node : b.getEntries()) {
                if (getRange(node.id) < b2.getRangeBegin()) {
                    b1.add(node);
                } else {
                    b2.add(node);
                }
            }
            this.buckets.set(bucketIdx, b1);
            this.buckets.add(bucketIdx + 1, b2);
            if (b2.size() > Config.BUCKET_SIZE) {
                b2.remove(added.id);
                if (b.isFull() == false) {
                    b2.add(added);
                } else
                    return false;
            }
        }
        return true;
    }
}

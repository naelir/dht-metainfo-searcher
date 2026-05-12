/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NodeList {
    public static final long CLEAN_INTERVAL = 60 * 1000; // 60 sec
    public static final long NODE_EXPIRE_TIME = 15 * 60 * 1000; // 15 min
    public static final long NODE_REPLACEABLE_TIME = 12 * 60 * 1000; // 12 min
    public static final long NODE_PING_TIME = 10 * 60 * 1000; // 10 min
    public static final long EXPLORE_INTERVAL = 60 * 1000; // 60 sec
    public static final long UPDATE_BUCKET_INTERVAL = 10 * 60 * 1000; // 10 min
    public static final int EXPLORE_AGGRESSIVE_MAX_NODES = 100;
    public static final int EXPLORE_MAX_NODES = 600;
    public static final int MAX_NODES = 800;
    private final HashMap<ByteBuffer, Node> nodeMap = new HashMap<>();
    private final Buckets buckets;
    private long lastCleanTime = 0;
    private long lastExploreTime = 0;
    private ByteBuffer myNodeID;

    public NodeList(ByteBuffer myNodeID) {
        this.lastCleanTime = System.currentTimeMillis();
        this.buckets = new Buckets(myNodeID);
        this.myNodeID = myNodeID;
    }

    public synchronized void clear() {
        this.nodeMap.clear();
        this.buckets.clear();
    }

    public synchronized Set<ByteBuffer> explore() {
        long now = System.currentTimeMillis();
        Set<ByteBuffer> list = new HashSet<>();
        if (this.nodeMap.size() <= EXPLORE_MAX_NODES) {
            for (Bucket b : this.buckets.getBuckets()) {
                // update old and not full buckets
                if (b.getLastChanged() < now - UPDATE_BUCKET_INTERVAL
                        && (b.size() < 6 || b.getRangeBegin() != b.getRangeEnd())) {
                    // check closest bucket
                    if (b.getRangeBegin() != b.getRangeEnd()) {
                        // explore my ID
//                        int depth = 2;
//                        if (this.nodeMap.size() < EXPLORE_AGGRESSIVE_MAX_NODES) {
//                            depth = 3;
//                        }
                        list.add(this.myNodeID);
                    } else {
                        // generate random ID
                        ByteBuffer randomID = Generator.generateRandomID(this.myNodeID, b.getRangeBegin());
                        list.add(randomID);
                    }
                }
            }
        }
        return list;
    }

    public synchronized List<Node> findClosest(ByteBuffer id, int max) {
        List<ByteBuffer> ids = this.buckets.getClosest(id, 2 * max);
        List<Node> closest = new ArrayList<>(ids.size());
        // Add non-questionable first
        for (ByteBuffer key : ids) {
            Node node = this.nodeMap.get(key);
            if (node != null && closest.size() < max) {
                closest.add(node);
            }
        }
        // Add questionable if there is room
        if (closest.size() >= max)
            return closest;
        for (ByteBuffer key : ids) {
            Node node = this.nodeMap.get(key);
            if (node != null && closest.size() < max) {
                closest.add(node);
            }
        }
        return closest;
    }

    public synchronized Node get(ByteBuffer nid) {
        return this.nodeMap.get(nid);
    }

    public synchronized int numOfBuckets() {
        return this.buckets.getBuckets().size();
    }

    public synchronized void put(Node node) {
        if (this.buckets.add(node)) {
            this.nodeMap.put(node.id, node);
        }
    }

    public synchronized void putIfAbsent(Node node) {
        if (this.nodeMap.size() < MAX_NODES && this.nodeMap.containsKey(node.id) == false) {
            if (this.buckets.add(node)) {
                this.nodeMap.put(node.id, node);
            }
        }
    }

    public synchronized void remove(ByteBuffer nodeId) {
        Node node = this.nodeMap.get(nodeId);
        if (node != null) {
            this.buckets.remove(nodeId);
            this.nodeMap.remove(nodeId);
        }
    }

    public synchronized int size() {
        return this.nodeMap.size();
    }
}

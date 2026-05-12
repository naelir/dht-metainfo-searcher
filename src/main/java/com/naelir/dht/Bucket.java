/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.naelir.dht;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Bucket {
    private int rangeBegin;
    private int rangeEnd;
    private Set<Node> nodes;
    private long lastChanged = 0;

    public Bucket(int begin, int end) {
        this.rangeBegin = begin;
        this.rangeEnd = end;
        this.nodes = new HashSet<>();
    }

    public boolean add(Node node) {
        if (this.nodes.size() >= 2 * Config.BUCKET_SIZE)
            return false;
        boolean added = this.nodes.add(node);
        this.lastChanged = System.currentTimeMillis();
        return added;
    }

    public void clear() {
        this.nodes.clear();
    }

    public Set<Node> getEntries() {
        return Collections.unmodifiableSet(this.nodes);
    }

    public long getLastChanged() {
        return this.lastChanged;
    }

    public int getRangeBegin() {
        return this.rangeBegin;
    }

    public int getRangeEnd() {
        return this.rangeEnd;
    }

    public boolean isFull() {
        return this.nodes.size() >= Config.BUCKET_SIZE;
    }

    public boolean remove(ByteBuffer node) {
        return this.nodes.remove(node);
    }

    public void setLastChanged(long now) {
        this.lastChanged = now;
    }

    public int size() {
        return this.nodes.size();
    }
}
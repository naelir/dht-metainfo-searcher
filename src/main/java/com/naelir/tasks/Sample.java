package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.naelir.bt.Torrent;
import com.naelir.dht.Converter;
import com.naelir.dht.Node;

public class Sample {
    Torrent torrent;
//    RoutingTable table;
    Set<Node> asked;
    Set<Node> peers;
    int checked;
    boolean skip;

    public Sample(Torrent torrent, List<Node> ask, boolean skip) {
        this.torrent = torrent;
//        this.table = new RoutingTable();
        this.peers = new HashSet<>();
        this.asked = new HashSet<>(4);
        this.skip = skip;
//        ask.forEach(e -> this.table.insert(e));
    }

    public synchronized void addAsked(Node n) {
        this.asked.add(n);
    }

    public synchronized void addPeer(Node list) {
        this.peers.add(list);
    }

    public synchronized boolean asked(Node node) {
        return this.asked.contains(node);
    }

    public ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(Converter.toArray(this.torrent.infoHash()));
    }

    public void check() {
        this.checked++;
    }

    public synchronized Set<Node> peers() {
        return new HashSet<>(this.peers);
    }

    public boolean skip() {
        return this.skip;
    }

    public void skip(boolean b) {
        this.skip = b;
    }
//    public RoutingTable table() {
//        return this.table;
//    }

    public Torrent torrent() {
        return this.torrent;
    }
}

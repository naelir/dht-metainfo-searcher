package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.naelir.bt.Torrent;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.RoutingTable;

public class Sample {
    Torrent torrent;
    RoutingTable table;
    Set<Node> peers;
    int checked;
    boolean skip;

    public Sample(Torrent torrent, List<Node> ask, boolean skip) {
        this.torrent = torrent;
        this.table = new RoutingTable();
        this.peers = new HashSet<>();
        this.skip = skip;
        ask.forEach(e -> this.table.insert(e));
    }
    
    public boolean skip() {
        return skip;
    }
    
    public ByteBuffer byteBuffer() {
        return ByteBuffer.wrap(Generator.toArray(torrent.infoHash()));
    }
    
    public synchronized void addPeer(Node list) {
        this.peers.add(list);
    }

    public synchronized Set<Node> peers() {
        return new HashSet<>(this.peers);
    }

    public Torrent torrent() {
        return this.torrent;
    }

    public void skip(boolean b) {
        this.skip = b;
        
    }

    public RoutingTable table() {
        return table;
    }
    
    public void check() {
        checked++;
    }
}

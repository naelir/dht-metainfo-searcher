package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.naelir.bt.Torrent;
import com.naelir.dht.Command;
import com.naelir.dht.Generator;
import com.naelir.dht.Node;
import com.naelir.dht.RoutingTable;

public class Sample {
    Torrent torrent;
    RoutingTable table;
    Set<Node> peers;
    int checked;
    public final Node from;

    boolean skip;

    public Sample(Torrent torrent, Node from, boolean skip) {
        this.torrent = torrent;
        this.from = from;
        this.table = new RoutingTable();
        this.table.insert(from);
        this.peers = new HashSet<>();
        this.skip = skip;
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
        return new HashSet<Node>(this.peers);
    }

    public Torrent torrent() {
        return this.torrent;
    }

    public synchronized void removePeer(Node node) {
        this.peers.remove(node);
    }
    
    public boolean isResolved() {
        return torrent.meta() != null;
    }
    
    public boolean isEmpty() {
        return (checked > 5 && peers.size() == 0);
    }
    
    
    public boolean isPing() {
        return peers.stream().allMatch(n -> n.have(Command.PING));
    }

    public void skip(boolean b) {
        this.skip = b;
        
    }

    public RoutingTable table() {
        return table;
    }
}

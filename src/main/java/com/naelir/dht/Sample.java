package com.naelir.dht;

import java.util.HashSet;
import java.util.Set;

import com.naelir.bt.Torrent;

public class Sample {
    Torrent torrent;
    RoutingTable table;
    Set<Node> peers;
    int checked;
    public final Node from;

    boolean isCrap;
    
    public Sample(Torrent torrent, Node from) {
        this.torrent = torrent;
        this.from = from;
        this.table = new RoutingTable();
        this.peers = new HashSet<>();
    }

    public synchronized void addPeer(Node list) {
        this.peers.add(list);
    }

    public Set<Node> getPeers() {
        return this.peers;
    }

    public Torrent getTorrent() {
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
}

package com.naelir.tasks;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.naelir.bt.Torrent;
import com.naelir.dht.Converter;
import com.naelir.dht.Node;

public class Sample {
    Torrent torrent;
    Set<Node> asked;
    Set<Node> peers;
    List<String> deniedLocations;
    int checked;

    public Sample(Torrent torrent, List<Node> ask, boolean skip) {
        this.torrent = torrent;
        this.peers = new HashSet<>();
        this.deniedLocations = new ArrayList<>(5);
        this.asked = new HashSet<>(4);
    }

    public synchronized void addAsked(Node n) {
        this.asked.add(n);
    }
    
    public synchronized void addPeer(Node list) {
        this.peers.add(list);
    }

    
    public synchronized void addDeniedLocations(String location) {
        this.deniedLocations.add(location);
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
    
    public synchronized List<String> deniedLocations() {
        return deniedLocations;
    }
    
    public Torrent torrent() {
        return this.torrent;
    }
}
